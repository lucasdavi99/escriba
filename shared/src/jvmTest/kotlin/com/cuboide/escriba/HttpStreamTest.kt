package com.cuboide.escriba

import com.cuboide.escriba.core.ai.AiConfig
import com.cuboide.escriba.core.ai.AiProviderKind
import com.cuboide.escriba.core.ai.AiRequest
import com.cuboide.escriba.core.ai.ChatRole
import com.cuboide.escriba.core.ai.ChatTurn
import com.cuboide.escriba.core.ai.createAiProvider
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A chamada HTTP real da IA contra um servidor local: streaming, fim da resposta e cancelamento. */
class HttpStreamTest {
    private val sse = "data: {\"choices\":[{\"delta\":{\"content\":\"ok\"}}]}\n\ndata: [DONE]\n\n"

    private fun <T> withServer(handler: (HttpExchange) -> Unit, block: (AiConfig) -> T): T {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { handler(it) }
        server.start()
        try {
            return block(AiConfig(provider = AiProviderKind.Custom, model = "m", baseUrl = "http://127.0.0.1:${server.address.port}"))
        } finally {
            server.stop(0)
        }
    }

    private fun request() = AiRequest("", listOf(ChatTurn(ChatRole.User, "oi")), "m", 16)

    @Test
    fun streamsChunksFromAnEventStream() = withServer({ ex ->
        ex.responseHeaders.add("Content-Type", "text/event-stream")
        ex.sendResponseHeaders(200, 0)
        ex.responseBody.use { it.write(sse.toByteArray()) }
    }) { cfg ->
        val out = runBlocking { withTimeoutOrNull(10_000) { createAiProvider(cfg).stream(request()).toList() } }
        assertEquals(listOf("ok"), out)
    }

    /** Alguns servidores não fecham a conexão depois do `[DONE]`: a resposta tem de terminar mesmo assim. */
    @Test
    fun endsAtDoneEvenIfTheServerKeepsTheConnectionOpen() = withServer({ ex ->
        ex.responseHeaders.add("Content-Type", "text/event-stream")
        ex.sendResponseHeaders(200, 0)
        ex.responseBody.write(sse.toByteArray())
        ex.responseBody.flush()
        Thread.sleep(20_000)
        ex.close()
    }) { cfg ->
        val started = System.currentTimeMillis()
        val out = runBlocking { withTimeoutOrNull(8_000) { createAiProvider(cfg).stream(request()).toList() } }
        assertEquals(listOf("ok"), out)
        assertTrue(System.currentTimeMillis() - started < 8_000)
    }

    /** Cancelar (Parar, tempo esgotado) tem de soltar a leitura bloqueada, sem esperar o servidor. */
    @Test
    fun cancellationReleasesABlockedRead() = withServer({ ex ->
        ex.responseHeaders.add("Content-Type", "text/event-stream")
        ex.sendResponseHeaders(200, 0)
        ex.responseBody.write("data: {\"choices\":[{\"delta\":{\"content\":\"a\"}}]}\n\n".toByteArray())
        ex.responseBody.flush()
        Thread.sleep(20_000) // nada mais chega
        ex.close()
    }) { cfg ->
        val started = System.currentTimeMillis()
        val result = runBlocking { withTimeoutOrNull(1_000) { createAiProvider(cfg).stream(request()).toList() } }
        assertNull(result)
        assertTrue(System.currentTimeMillis() - started < 5_000, "a coleta não soltou a leitura bloqueada")
    }

    @Test
    fun httpErrorsBecomeReadableMessages() = withServer({ ex ->
        val body = """[{"error":{"code":400,"message":"API key not valid."}}]""".toByteArray()
        ex.sendResponseHeaders(400, body.size.toLong())
        ex.responseBody.use { it.write(body) }
    }) { cfg ->
        val failure = runBlocking { runCatching { createAiProvider(cfg).stream(request()).toList() }.exceptionOrNull() }
        assertNotNull(failure)
        assertTrue("API key not valid" in failure.message.orEmpty(), failure.message)
    }
}
