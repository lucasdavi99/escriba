package com.cuboide.escriba.core.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Quem fornece o modelo de IA. Cada um fala um protocolo diferente. */
@Serializable
enum class AiProviderKind(val label: String, val defaultModel: String, val defaultBaseUrl: String, val needsKey: Boolean) {
    Anthropic("Anthropic (Claude)", "claude-sonnet-5-5", "https://api.anthropic.com", true),
    OpenAi("OpenAI", "gpt-4o", "https://api.openai.com", true),
    /** Usa o endpoint compatível com OpenAI do Google; a chave vem do Google AI Studio. */
    Gemini("Google (Gemini)", "gemini-2.5-flash", "https://generativelanguage.googleapis.com/v1beta/openai", true),
    Custom("Compatível com OpenAI (Ollama, OpenRouter…)", "", "http://localhost:11434", false),
}

/**
 * Conexão do usuário com a IA. Fica em `ia.json` na pasta de dados do app, **nunca dentro do livro**
 * nem em exportações.
 */
@Serializable
data class AiConfig(
    val provider: AiProviderKind = AiProviderKind.Anthropic,
    val apiKey: String = "",
    /** Vazio usa o modelo padrão do provedor. */
    val model: String = "",
    /** Vazio usa o endereço padrão do provedor; no "Compatível" é obrigatório. */
    val baseUrl: String = "",
    /** Quanto contexto do livro enviar por pergunta, em tokens (estimados). */
    val contextBudgetTokens: Int = DEFAULT_CONTEXT_BUDGET,
    /** Limite quando "Livro inteiro" está ligado (o livro todo vai em cada pergunta, então é bem maior). */
    val fullBookBudgetTokens: Int = DEFAULT_FULL_BOOK_BUDGET,
    /** O usuário já viu o aviso de que o texto do livro vai para o provedor. */
    val consentGiven: Boolean = false,
    /**
     * Tamanho máximo de cada resposta, em tokens. Nos modelos que "pensam" antes de responder (Gemini 2.5+, por
     * exemplo), o raciocínio conta dentro desse limite, então ele precisa de folga.
     */
    val maxOutputTokens: Int = DEFAULT_MAX_OUTPUT,
    /** Instruções do autor para todas as conversas, de todos os livros (tom, jeito de ser ajudado). */
    val generalInstructions: String = "",
    /** Papéis que o autor escolhe no chat (Revisor, Leitor beta…); editáveis nas Preferências. */
    val modes: List<AiMode> = DEFAULT_AI_MODES,
    /** Versão dos modos de fábrica já aplicada (ver `withUpgradedModes`); 0 em arquivos antigos. */
    val modesVersion: Int = 0,
) {
    val effectiveModel: String get() = model.trim().ifEmpty { provider.defaultModel }
    val effectiveBaseUrl: String get() = baseUrl.trim().ifEmpty { provider.defaultBaseUrl }.trimEnd('/')
    val isConfigured: Boolean
        get() = effectiveModel.isNotEmpty() && effectiveBaseUrl.isNotEmpty() && (!provider.needsKey || apiKey.isNotBlank())

    companion object {
        const val DEFAULT_CONTEXT_BUDGET = 24_000
        const val MIN_CONTEXT_BUDGET = 2_000
        const val MAX_CONTEXT_BUDGET = 200_000
        const val DEFAULT_FULL_BOOK_BUDGET = 120_000
        const val MAX_FULL_BOOK_BUDGET = 1_000_000
        const val DEFAULT_MAX_OUTPUT = 16_000
        const val MIN_MAX_OUTPUT = 2_000
        const val MAX_MAX_OUTPUT = 64_000
    }
}

@Serializable
enum class ChatRole { User, Assistant }

/** Algo do livro que o autor aponta no prompt (`@`) ou que o app anexa sozinho. */
@Serializable
sealed interface AiRef {
    @Serializable @SerialName("chapter") data class Chapter(val id: String) : AiRef
    @Serializable @SerialName("beat") data class Beat(val id: String) : AiRef
    @Serializable @SerialName("entry") data class Entry(val id: String) : AiRef
    @Serializable @SerialName("selection") data class Selection(val chapterId: String, val text: String) : AiRef
    @Serializable @SerialName("synopsis") data object Synopsis : AiRef
    @Serializable @SerialName("outline") data object Outline : AiRef
    /** Outro livro da biblioteca (ou da mesma saga): entra com sinopse, roteiro e fichas, não o texto todo. */
    @Serializable @SerialName("book") data class Book(val path: String) : AiRef
}

/** Um item de contexto efetivamente enviado, para mostrar ao autor "o que a IA leu". */
@Serializable
data class ContextItem(val label: String, val ref: AiRef? = null)

@Serializable
data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val text: String,
    val refs: List<AiRef> = emptyList(),
    val usedContext: List<ContextItem> = emptyList(),
    val createdAt: Long = 0,
    /** A resposta parou pelo limite de tamanho (o autor pode pedir para continuar). */
    val truncated: Boolean = false,
)

@Serializable
data class Conversation(
    val id: String,
    val title: String = "Nova conversa",
    val messages: List<ChatMessage> = emptyList(),
    val modifiedAt: Long = 0,
    /** Modo escolhido para esta conversa ([AiMode.id]); `null` = livre, sem papel. */
    val modeId: String? = null,
)

/** Conversas de um livro, em `<livro>/ia/conversas.json`. */
@Serializable
data class BookConversations(val conversations: List<Conversation> = emptyList())
