package com.cuboide.escriba

import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.EscribaServices
import com.cuboide.escriba.core.ai.AiProvider
import com.cuboide.escriba.core.ai.AiRequest
import com.cuboide.escriba.core.ai.ChatRole
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Parar uma resposta e pedir outra logo em seguida, com o tempo e a ordem sob controle do teste. */
@OptIn(ExperimentalCoroutinesApi::class)
class AssistantStopTest {
    private val fs = InMemoryFileStore()
    private val clock = TestClock()
    private val scope = TestScope()
    /** A IA manda um pedaço e fica "pensando" até alguém parar. */
    private val slow = object : AiProvider {
        override fun stream(req: AiRequest): Flow<String> = flow {
            emit("parte")
            awaitCancellation()
        }
    }
    private val services = EscribaServices(
        fs, "/lib", "/data", { clock.now + scope.testScheduler.currentTime }, clock::id, { "2026-11-14" }, aiProvider = { slow },
    )
    private val c = EscribaController(services, scope)
    private val a get() = c.assistant

    @AfterTest
    fun tearDown() = scope.cancel()

    @Test
    fun aStoppedAnswerDoesNotUnlockTheNextOne() {
        c.createBook("Livro", "")
        a.updateConfig { it.copy(apiKey = "k", consentGiven = true) }
        a.send("Primeira pergunta", null)
        scope.testScheduler.runCurrent()

        a.stop()
        a.send("Segunda pergunta", null)
        scope.testScheduler.runCurrent() // o fim da primeira resposta (já parada) roda aqui

        assertTrue(a.busy, "a segunda resposta ainda está chegando")
        val answers = a.current!!.messages.filter { it.role == ChatRole.Assistant }.map { it.text }
        assertEquals(listOf("parte", "parte"), answers) // o que chegou da primeira ficou guardado
    }
}
