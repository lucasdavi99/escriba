package com.cuboide.escriba.core.ai

import com.cuboide.escriba.platform.httpStreamLines
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.takeWhile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class ChatTurn(val role: ChatRole, val text: String)

data class AiRequest(
    val system: String,
    val messages: List<ChatTurn>,
    val model: String,
    val maxTokens: Int = 4096,
)

/** Falha com mensagem já pronta para mostrar ao usuário. */
class AiException(message: String) : Exception(message)

/** Resposta HTTP que não foi 2xx (lançada por [httpStreamLines]). */
class HttpStatusException(val status: Int, val body: String) : Exception("HTTP $status")

interface AiProvider {
    /** Pedaços de texto da resposta, à medida que chegam. Falhas viram [AiException] em português. */
    fun stream(req: AiRequest): Flow<String>
}

fun createAiProvider(config: AiConfig): AiProvider = when (config.provider) {
    AiProviderKind.Anthropic -> AnthropicProvider(config)
    AiProviderKind.OpenAi, AiProviderKind.Gemini, AiProviderKind.Custom -> OpenAiProvider(config)
}

private val json = Json { ignoreUnknownKeys = true }

/** API de mensagens da Anthropic, com SSE. */
class AnthropicProvider(private val config: AiConfig) : AiProvider {
    override fun stream(req: AiRequest): Flow<String> =
        httpStreamLines(
            url = config.effectiveBaseUrl + "/v1/messages",
            headers = mapOf(
                "x-api-key" to config.apiKey,
                "anthropic-version" to "2023-06-01",
                "content-type" to "application/json",
            ),
            body = anthropicBody(req),
        ).takeWhile { !(sseData(it)?.contains("\"message_stop\"") ?: false) } // fim da resposta: não espera a conexão fechar
            .mapNotNull { parseAnthropicLine(it) }.translateErrors()

    companion object {
        fun anthropicBody(req: AiRequest): String = buildJsonObject {
            put("model", req.model)
            put("max_tokens", req.maxTokens)
            put("stream", true)
            if (req.system.isNotBlank()) put("system", req.system)
            put("messages", turnsJson(req.messages))
        }.toString()

        /** Texto novo de uma linha SSE, ou `null` se a linha não traz texto. */
        fun parseAnthropicLine(line: String): String? {
            val data = sseData(line) ?: return null
            val obj = runCatching { json.parseToJsonElement(data).jsonObject }.getOrNull() ?: return null
            return when (obj["type"]?.jsonPrimitive?.contentOrNull) {
                "content_block_delta" -> obj["delta"]?.jsonObject?.takeIf { it["type"]?.jsonPrimitive?.contentOrNull == "text_delta" }
                    ?.get("text")?.jsonPrimitive?.contentOrNull
                "error" -> throw AiException(
                    "O provedor devolveu um erro: " + (obj["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull ?: "desconhecido"),
                )
                else -> null
            }
        }
    }
}

/** Chat Completions da OpenAI, com SSE. Serve também para Ollama, OpenRouter e semelhantes. */
class OpenAiProvider(private val config: AiConfig) : AiProvider {
    override fun stream(req: AiRequest): Flow<String> {
        val headers = buildMap {
            put("content-type", "application/json")
            if (config.apiKey.isNotBlank()) put("authorization", "Bearer ${config.apiKey}")
        }
        return httpStreamLines(openAiUrl(config.effectiveBaseUrl), headers, openAiBody(req))
            .takeWhile { sseData(it) != "[DONE]" } // fim da resposta: não espera a conexão fechar
            .mapNotNull { parseOpenAiLine(it) }.translateErrors()
    }

    companion object {
        /** Aceita o endereço com ou sem `/v1` no fim; o do Gemini (`…/v1beta/openai`) já tem o prefixo da versão. */
        fun openAiUrl(base: String): String {
            val b = base.trimEnd('/')
            return if (b.endsWith("/v1") || b.endsWith("/openai")) "$b/chat/completions" else "$b/v1/chat/completions"
        }

        fun openAiBody(req: AiRequest): String = buildJsonObject {
            put("model", req.model)
            put("stream", true)
            put("max_tokens", req.maxTokens)
            put("messages", buildJsonArray {
                if (req.system.isNotBlank()) add(buildJsonObject { put("role", "system"); put("content", req.system) })
                req.messages.forEach { add(buildJsonObject { put("role", it.role.apiName); put("content", it.text) }) }
            })
        }.toString()

        fun parseOpenAiLine(line: String): String? {
            val data = sseData(line) ?: return null
            if (data == "[DONE]") return null
            val obj = runCatching { json.parseToJsonElement(data).jsonObject }.getOrNull() ?: return null
            obj["error"]?.let { e ->
                val msg = (e as? JsonObject)?.get("message")?.jsonPrimitive?.contentOrNull ?: e.toString()
                throw AiException("O provedor devolveu um erro: $msg")
            }
            val choice = (obj["choices"] as? JsonArray)?.firstOrNull() as? JsonObject ?: return null
            return choice["delta"]?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() }
        }
    }
}

private val ChatRole.apiName: String get() = if (this == ChatRole.User) "user" else "assistant"

private fun turnsJson(turns: List<ChatTurn>): JsonElement = buildJsonArray {
    turns.forEach { add(buildJsonObject { put("role", it.role.apiName); put("content", it.text) }) }
}

/** Conteúdo de uma linha `data: …` do SSE; `null` para qualquer outra linha. */
internal fun sseData(line: String): String? =
    if (line.startsWith("data:")) line.removePrefix("data:").trim().takeIf { it.isNotEmpty() } else null

/** Traduz falhas de rede e HTTP em mensagens para o usuário. */
internal fun <T> Flow<T>.translateErrors(): Flow<T> = catch { e ->
    // Cancelar interrompe a leitura bloqueada, que chega como IOException: isso é cancelamento, não falha de rede.
    currentCoroutineContext().ensureActive()
    when (e) {
        is CancellationException -> throw e
        is AiException -> throw e
        is HttpStatusException -> throw AiException(friendlyHttpError(e.status, e.body))
        else -> throw AiException("Não foi possível falar com o provedor: ${e.message ?: e::class.simpleName}")
    }
}

fun friendlyHttpError(status: Int, body: String): String {
    val detail = runCatching {
        // O Gemini devolve os erros dentro de uma lista: `[{"error": {...}}]`.
        val parsed = json.parseToJsonElement(body)
        val root = if (parsed is JsonArray) parsed.first().jsonObject else parsed.jsonObject
        val err = root["error"]
        (err as? JsonObject)?.get("message")?.jsonPrimitive?.contentOrNull
            ?: (err as? JsonPrimitive)?.contentOrNull
            ?: root["message"]?.jsonPrimitive?.contentOrNull
    }.getOrNull()
    val base = when (status) {
        401, 403 -> "Chave de API inválida ou sem permissão."
        404 -> "Modelo ou endereço não encontrado. Confira o nome do modelo e o endereço nas Preferências."
        429 -> "Limite de uso do provedor atingido. Tente de novo em instantes."
        400 -> "O provedor recusou o pedido."
        in 500..599 -> "O provedor está com problemas (HTTP $status). Tente de novo em instantes."
        else -> "Erro do provedor (HTTP $status)."
    }
    return if (detail.isNullOrBlank()) base else "$base ($detail)"
}
