package com.cuboide.escriba

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.EscribaServices
import com.cuboide.escriba.core.ai.AiConfig
import com.cuboide.escriba.core.ai.AiEvent
import com.cuboide.escriba.core.ai.AiException
import com.cuboide.escriba.core.ai.AiProvider
import com.cuboide.escriba.core.ai.AiRef
import com.cuboide.escriba.core.ai.AiRequest
import com.cuboide.escriba.core.ai.ChatRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AssistantControllerTest {
    private val fs = InMemoryFileStore()
    private val clock = TestClock()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val requests = mutableListOf<AiRequest>()
    private var reply: List<String> = listOf("Resposta ", "da IA.")
    private var failWith: String? = null
    /** Quantas das próximas respostas param pelo limite de tamanho. */
    private var cutNext = 0
    private val fake = object : AiProvider {
        override fun stream(req: AiRequest): Flow<String> = events(req).mapNotNull { (it as? AiEvent.Text)?.text }

        override fun events(req: AiRequest): Flow<AiEvent> = flow {
            requests += req
            failWith?.let { throw AiException(it) }
            reply.forEach { emit(AiEvent.Text(it)) }
            emit(AiEvent.Stop(truncated = cutNext-- > 0))
        }
    }
    private val services = EscribaServices(fs, "/lib", "/data", { clock.now }, clock::id, { "2026-11-14" }, aiProvider = { fake })
    private val c = EscribaController(services, scope)
    private val a get() = c.assistant

    @AfterTest
    fun tearDown() = scope.cancel()

    private fun connect() = a.updateConfig { it.copy(apiKey = "k", consentGiven = true) }

    private fun waitIdle() {
        val end = System.currentTimeMillis() + 5000
        while (a.busy && System.currentTimeMillis() < end) Thread.sleep(10)
        assertFalse(a.busy, "o assistente não terminou")
    }

    private fun write(text: String) {
        c.chapter!!.type(TextFieldValue(text))
        c.flushSave()
    }

    @Test
    fun withoutConnectionNothingIsSent() {
        c.createBook("Livro", "")
        a.send("Oi", null)
        assertTrue(requests.isEmpty())
        assertNotNull(a.error)
    }

    @Test
    fun firstUseAsksConsentThenSends() {
        c.createBook("Livro", "")
        a.updateConfig { it.copy(apiKey = "k") }
        a.send("Oi", null)
        assertTrue(a.consentPending)
        assertTrue(requests.isEmpty())
        a.acceptConsent()
        waitIdle()
        assertEquals(1, requests.size)
        assertTrue(a.config.consentGiven)
    }

    @Test
    fun answerStreamsIntoTheConversationAndIsSavedInTheBook() {
        connect()
        c.createBook("Livro", "")
        write("Mirela chegou ao porto.")
        a.send("O que acontece depois?", null)
        waitIdle()
        val msgs = a.current!!.messages
        assertEquals(listOf(ChatRole.User, ChatRole.Assistant), msgs.map { it.role })
        assertEquals("Resposta da IA.", msgs.last().text)
        // O contexto do livro foi para o prompt de sistema.
        assertTrue("Título: Livro" in requests.single().system)
        // E a conversa ficou no livro.
        val saved = services.books.loadConversations(c.book!!.path).conversations.single()
        assertEquals("Resposta da IA.", saved.messages.last().text)
        assertEquals("O que acontece depois?", saved.title)
    }

    @Test
    fun providerErrorsAreShownAndLeaveNoEmptyAnswer() {
        connect()
        c.createBook("Livro", "")
        failWith = "Chave de API inválida."
        a.send("Oi", null)
        waitIdle()
        assertEquals("Chave de API inválida.", a.error)
        assertTrue(a.current!!.messages.none { it.role == ChatRole.Assistant })
    }

    @Test
    fun selectionGoesAsContextAndCanBeReplacedWithAVersionKept() {
        connect()
        c.createBook("Livro", "")
        val text = "Era uma noite escura. Mirela andou. O vento soprou."
        write(text)
        val start = text.indexOf("Mirela andou.")
        c.chapter!!.type(TextFieldValue(text, TextRange(start, start + "Mirela andou.".length)))
        reply = listOf("Mirela avançou em silêncio.")
        assertTrue(a.attachSelection())
        a.send("Melhore o trecho", null)
        waitIdle()
        val question = a.current!!.messages.first()
        assertTrue(question.refs.any { it is AiRef.Selection })
        assertTrue("Mirela andou." in requests.single().system)

        val before = c.snapshots.size
        assertTrue(a.replaceSelection(a.current!!.messages.last()))
        assertEquals("Era uma noite escura. Mirela avançou em silêncio. O vento soprou.", c.chapter!!.value.text)
        assertEquals(before + 1, c.snapshots.size) // versão guardada antes de trocar
        // E dá para desfazer.
        c.chapter!!.undo()
        assertEquals(text, c.chapter!!.value.text)
    }

    @Test
    fun replacementIsRefusedWhenTheTextWasEdited() {
        connect()
        c.createBook("Livro", "")
        val text = "Primeiro. Mirela andou. Último."
        write(text)
        val start = text.indexOf("Mirela andou.")
        c.chapter!!.type(TextFieldValue(text, TextRange(start, start + 13)))
        a.attachSelection()
        a.send("Melhore", null)
        waitIdle()
        write("Primeiro. Tudo mudou. Último.")
        assertFalse(a.replaceSelection(a.current!!.messages.last()))
        assertEquals("Primeiro. Tudo mudou. Último.", c.chapter!!.value.text)
    }

    @Test
    fun insertAtCursorPutsTheAnswerInItsOwnParagraph() {
        connect()
        c.createBook("Livro", "")
        write("Primeira linha.")
        reply = listOf("Segunda linha.\n\nTerceira linha.")
        a.send("Continue", null)
        waitIdle()
        c.chapter!!.type(TextFieldValue("Primeira linha.", TextRange(15)))
        a.insertAtCursor(a.current!!.messages.last())
        assertEquals("Primeira linha.\nSegunda linha.\nTerceira linha.", c.chapter!!.value.text)
    }

    @Test
    fun referencesAreOfferedAndFiltered() {
        c.createBook("Livro", "")
        c.saveEntry(c.newEntry(com.cuboide.escriba.core.model.EntryKind.Personagem).copy(name = "Mirela"))
        val all = a.refOptions("")
        assertTrue(all.any { it.ref == AiRef.Synopsis } && all.any { it.ref == AiRef.Outline })
        assertTrue(all.any { it.label == "Mirela" })
        assertEquals(listOf("Mirela"), a.refOptions("mire").map { it.label }) // sem diferenciar acento nem caixa
        a.addRef(AiRef.Synopsis)
        assertTrue(a.refOptions("").none { it.ref == AiRef.Synopsis }) // já escolhida
    }

    @Test
    fun conversationsReloadWhenTheBookIsReopened() {
        connect()
        c.createBook("Livro", "")
        a.send("Oi", null)
        waitIdle()
        val path = c.book!!.path
        c.closeBook()
        assertTrue(a.conversations.isEmpty())
        c.openBook(path)
        assertEquals(2, a.current!!.messages.size)
    }

    /** Seleções deixadas no editor (Busca, menções, localizar) não vão sozinhas; o capítulo aberto vai inteiro. */
    @Test
    fun openChapterGoesWholeAndAStaleSelectionDoesNot() {
        connect()
        c.createBook("Livro", "")
        val text = "Começo do capítulo. Meio do capítulo. Fim do capítulo."
        write(text)
        c.chapter!!.type(TextFieldValue(text, TextRange(0, 6))) // seleção que o autor não fez de propósito
        assertNotNull(a.selectionAvailable) // oferecida como botão…
        assertNotNull(a.autoChapterRef)
        a.send("O que acha?", null)
        waitIdle()
        val question = a.current!!.messages.first()
        assertTrue(question.refs.none { it is AiRef.Selection }) // …mas não anexada
        assertTrue(question.refs.contains(AiRef.Chapter(c.currentChapterId!!)))
        assertTrue("Fim do capítulo." in requests.single().system) // o capítulo inteiro foi
        assertTrue(question.usedContext.any { it.label.endsWith("(inteiro)") })
    }

    @Test
    fun theOpenChapterCanBeLeftOut() {
        connect()
        c.createBook("Livro", "")
        write("Texto secreto do capítulo.")
        a.dismissAutoChapter()
        assertEquals(null, a.autoChapterRef)
        a.send("Ideias soltas sobre dragões", null)
        waitIdle()
        assertTrue(a.current!!.messages.first().refs.none { it is AiRef.Chapter })
    }

    @Test
    fun improveTaskNeedsASelection() {
        connect()
        c.createBook("Livro", "")
        write("Um texto qualquer.")
        a.runTask(com.cuboide.escriba.core.ai.AiTask.Melhorar)
        assertTrue(requests.isEmpty())
        assertNotNull(a.error)
        c.chapter!!.type(TextFieldValue("Um texto qualquer.", TextRange(3, 8)))
        a.runTask(com.cuboide.escriba.core.ai.AiTask.Melhorar)
        waitIdle()
        assertTrue(a.current!!.messages.first().refs.any { it is AiRef.Selection && it.text == "texto" })
    }

    /** "Analise o capítulo 2" leva o capítulo 2 inteiro, mesmo com outro capítulo aberto. */
    @Test
    fun chapterCitedInTheRequestGoesWhole() {
        connect()
        c.createBook("Livro", "")
        write("Texto do primeiro capítulo.")
        c.addChapter()
        write("Lucas assentiu e saiu. Fim do segundo capítulo.")
        c.selectChapter(c.book!!.manifest.chapters.first().id)
        a.send("Analise o capítulo 2 por inteiro", null)
        waitIdle()
        val second = c.book!!.manifest.chapters[1].id
        assertTrue(a.current!!.messages.first().refs.contains(AiRef.Chapter(second)))
        assertTrue("Fim do segundo capítulo." in requests.single().system)
    }

    @Test
    fun fullBookTogglesAndSendsTheWholeBook() {
        connect()
        c.createBook("Livro", "")
        write("Primeiro capítulo inteiro aqui.")
        c.addChapter()
        write("Segundo capítulo aberto.")
        a.toggleFullBook()
        assertTrue(a.fullBook)
        assertTrue(a.fullBookTokens > 0)
        a.send("O que acha desse capítulo?", null)
        waitIdle()
        assertTrue("Primeiro capítulo inteiro aqui." in requests.single().system)
        assertEquals(a.config.maxOutputTokens, requests.single().maxTokens)
        assertTrue(a.fullBook) // continua ligado para a próxima pergunta
        a.toggleFullBook()
        assertFalse(a.fullBook)
    }

    @Test
    fun modeIsKeptPerConversationAndInstructionsReachThePrompt() {
        connect()
        a.updateConfig { it.copy(generalInstructions = "Seja direto.") }
        c.createBook("Livro", "")
        a.saveBookInstructions("Diálogos com travessão.")
        a.setMode("revisor") // ainda sem conversa: vale para a próxima
        a.send("Revise isto", null)
        waitIdle()
        val first = a.current!!
        assertEquals("revisor", first.modeId)
        val system = requests.last().system
        assertTrue("Seja direto." in system && "Diálogos com travessão." in system && "Seu papel nesta conversa: Revisor" in system)

        // Outra conversa com outro modo; a primeira continua com o dela.
        a.newConversation()
        a.setMode("coautor")
        a.send("Ideias?", null)
        waitIdle()
        assertEquals("coautor", a.current!!.modeId)
        a.openConversation(first.id)
        assertEquals("Revisor", a.mode!!.name)

        // Tudo fica guardado: instruções no livro, modo na conversa.
        val path = c.book!!.path
        c.closeBook()
        c.openBook(path)
        assertEquals("Diálogos com travessão.", a.bookInstructions)
        assertEquals(setOf("revisor", "coautor"), a.conversations.map { it.modeId }.toSet())
    }

    @Test
    fun deletedModeFallsBackToFree() {
        connect()
        c.createBook("Livro", "")
        a.setMode("editor")
        a.send("Oi", null)
        waitIdle()
        a.removeMode("editor")
        assertEquals(null, a.mode) // conversa passa a ser livre
        a.restoreDefaultModes()
        assertEquals("Editor", a.mode!!.name) // e volta ao restaurar
        val custom = a.addMode()
        a.updateMode(custom.copy(name = "Poeta", instructions = "Responda em versos."))
        assertTrue(a.config.modes.any { it.name == "Poeta" })
    }

    /** Resposta cortada pelo limite: fica marcada e "Continuar" completa a mesma mensagem. */
    @Test
    fun truncatedAnswerIsMarkedAndCanBeContinued() {
        connect()
        c.createBook("Livro", "")
        cutNext = 1
        reply = listOf("Primeira parte ")
        a.send("Analise o livro", null)
        waitIdle()
        val answer = a.current!!.messages.last()
        assertTrue(answer.truncated)
        assertEquals(16_000, requests.last().maxTokens)

        reply = listOf("e o final.")
        a.continueAnswer(answer)
        waitIdle()
        val msgs = a.current!!.messages
        assertEquals(2, msgs.size) // nada de mensagem nova: completa a mesma
        assertEquals("Primeira parte e o final.", msgs.last().text)
        assertFalse(msgs.last().truncated)
        // O pedido de continuação leva a resposta cortada e a instrução de seguir dela, com o mesmo contexto.
        val last = requests.last()
        assertEquals("Primeira parte ", last.messages[last.messages.size - 2].text)
        assertTrue("Continue exatamente" in last.messages.last().text)
        assertEquals(requests.first().system, last.system)
        // E fica guardado no livro.
        val saved = services.books.loadConversations(c.book!!.path).conversations.single().messages.last()
        assertEquals("Primeira parte e o final.", saved.text)
        assertFalse(saved.truncated)
    }
}
