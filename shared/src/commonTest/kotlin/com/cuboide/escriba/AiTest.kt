package com.cuboide.escriba

import com.cuboide.escriba.core.ai.AiConfig
import com.cuboide.escriba.core.ai.AiProviderKind
import com.cuboide.escriba.core.ai.AiRef
import com.cuboide.escriba.core.ai.AiRequest
import com.cuboide.escriba.core.ai.AnthropicProvider
import com.cuboide.escriba.core.ai.BookContext
import com.cuboide.escriba.core.ai.BookConversations
import com.cuboide.escriba.core.ai.ChatMessage
import com.cuboide.escriba.core.ai.ChatRole
import com.cuboide.escriba.core.ai.ChatTurn
import com.cuboide.escriba.core.ai.ContextBuilder
import com.cuboide.escriba.core.ai.ContextPiece
import com.cuboide.escriba.core.ai.Conversation
import com.cuboide.escriba.core.ai.OpenAiProvider
import com.cuboide.escriba.core.ai.AiException
import com.cuboide.escriba.core.ai.friendlyHttpError
import com.cuboide.escriba.core.ai.scorePieces
import com.cuboide.escriba.core.ai.withUpgradedModes
import com.cuboide.escriba.core.model.Beat
import com.cuboide.escriba.core.model.BookManifest
import com.cuboide.escriba.core.model.ChapterMeta
import com.cuboide.escriba.core.model.EntryAttribute
import com.cuboide.escriba.core.model.EntryKind
import com.cuboide.escriba.core.model.Outline
import com.cuboide.escriba.core.model.Universe
import com.cuboide.escriba.core.model.UniverseEntry
import com.cuboide.escriba.core.storage.AiStore
import com.cuboide.escriba.core.storage.BookRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AiTest {
    private val request = AiRequest("Seja breve.", listOf(ChatTurn(ChatRole.User, "Oi"), ChatTurn(ChatRole.Assistant, "Olá")), "m-1", 100)

    // ---------- provedores ----------

    @Test
    fun anthropicBodyAndStreamParsing() {
        val body = AnthropicProvider.anthropicBody(request)
        assertTrue("\"system\":\"Seja breve.\"" in body)
        assertTrue("\"stream\":true" in body)
        assertTrue("\"role\":\"assistant\"" in body)
        val delta = """data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Olá"}}"""
        assertEquals("Olá", AnthropicProvider.parseAnthropicLine(delta))
        assertNull(AnthropicProvider.parseAnthropicLine("event: ping"))
        assertNull(AnthropicProvider.parseAnthropicLine("""data: {"type":"message_start"}"""))
        assertFailsWith<AiException> {
            AnthropicProvider.parseAnthropicLine("""data: {"type":"error","error":{"type":"overloaded_error","message":"Sobrecarregado"}}""")
        }
    }

    @Test
    fun openAiBodyUrlAndStreamParsing() {
        val body = OpenAiProvider.openAiBody(request)
        // O prompt de sistema vira a primeira mensagem.
        assertTrue(body.indexOf("\"role\":\"system\"") < body.indexOf("\"role\":\"user\""))
        assertEquals("https://api.openai.com/v1/chat/completions", OpenAiProvider.openAiUrl("https://api.openai.com"))
        assertEquals("http://localhost:11434/v1/chat/completions", OpenAiProvider.openAiUrl("http://localhost:11434/v1/"))
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
            OpenAiProvider.openAiUrl(AiProviderKind.Gemini.defaultBaseUrl),
        )
        assertEquals("a", OpenAiProvider.parseOpenAiLine("""data: {"choices":[{"delta":{"content":"a"}}]}"""))
        assertNull(OpenAiProvider.parseOpenAiLine("data: [DONE]"))
        assertNull(OpenAiProvider.parseOpenAiLine("""data: {"choices":[{"delta":{"role":"assistant"}}]}"""))
        assertFailsWith<AiException> { OpenAiProvider.parseOpenAiLine("""data: {"error":{"message":"cota"}}""") }
    }

    /** O motivo da parada diz se a resposta foi cortada pelo limite de tamanho. */
    @Test
    fun stopReasonTellsWhenTheAnswerWasCut() {
        val ev = com.cuboide.escriba.core.ai.AiEvent.Stop(truncated = true)
        assertEquals(
            listOf(com.cuboide.escriba.core.ai.AiEvent.Text("fim"), ev),
            OpenAiProvider.parseOpenAiEvents("""data: {"choices":[{"delta":{"content":"fim"},"finish_reason":"length"}]}"""),
        )
        assertEquals(
            listOf(com.cuboide.escriba.core.ai.AiEvent.Stop(truncated = false)),
            OpenAiProvider.parseOpenAiEvents("""data: {"choices":[{"delta":{},"finish_reason":"stop"}]}"""),
        )
        assertEquals(emptyList(), OpenAiProvider.parseOpenAiEvents("""data: {"choices":[{"delta":{"content":""},"finish_reason":null}]}"""))
        assertEquals(ev, AnthropicProvider.parseAnthropicEvent("""data: {"type":"message_delta","delta":{"stop_reason":"max_tokens"}}"""))
        assertEquals(
            com.cuboide.escriba.core.ai.AiEvent.Stop(truncated = false),
            AnthropicProvider.parseAnthropicEvent("""data: {"type":"message_delta","delta":{"stop_reason":"end_turn"}}"""),
        )
    }

    @Test
    fun httpErrorsAreExplainedInPortuguese() {
        assertTrue("Chave de API inválida" in friendlyHttpError(401, """{"error":{"message":"bad key"}}"""))
        assertTrue("bad key" in friendlyHttpError(401, """{"error":{"message":"bad key"}}"""))
        assertTrue("Limite de uso" in friendlyHttpError(429, ""))
        assertTrue("HTTP 502" in friendlyHttpError(502, "<html>"))
        // O Gemini devolve os erros dentro de uma lista.
        assertTrue("API key not valid" in friendlyHttpError(400, """[{"error":{"code":400,"message":"API key not valid."}}]"""))
    }

    @Test
    fun configDefaultsAndValidation() {
        val c = AiConfig()
        assertEquals("claude-sonnet-5-5", c.effectiveModel)
        assertFalse(c.isConfigured) // falta a chave
        assertTrue(c.copy(apiKey = "k").isConfigured)
        val custom = AiConfig(provider = AiProviderKind.Custom, model = "llama3.1")
        assertTrue(custom.isConfigured) // servidor local não exige chave
        assertEquals("http://localhost:11434", custom.effectiveBaseUrl)
        assertEquals("http://x:1", AiConfig(baseUrl = " http://x:1/ ").effectiveBaseUrl)
    }

    // ---------- armazenamento ----------

    @Test
    fun aiStorePersistsInItsOwnFile() {
        val fs = InMemoryFileStore()
        val store = AiStore(fs, "/data/ia.json")
        store.update { it.copy(apiKey = "segredo", model = "x") }
        val again = AiStore(fs, "/data/ia.json")
        assertEquals("segredo", again.current.apiKey)
        assertEquals("x", again.current.model)
    }

    @Test
    fun conversationsAreSavedInsideTheBook() {
        val fs = InMemoryFileStore()
        val clock = TestClock()
        val repo = BookRepository(fs, "/lib", { clock.now }, clock::id)
        val book = repo.createBook("Livro")
        assertTrue(repo.loadConversations(book.path).conversations.isEmpty())
        val conv = Conversation(
            "c1", "Ideias", listOf(
                ChatMessage("m1", ChatRole.User, "Oi", refs = listOf(AiRef.Chapter("x"), AiRef.Synopsis, AiRef.Selection("x", "trecho"))),
                ChatMessage("m2", ChatRole.Assistant, "Olá"),
            ),
        )
        repo.saveConversations(book.path, BookConversations(listOf(conv)))
        val back = repo.loadConversations(book.path).conversations.single()
        assertEquals(conv, back)
    }

    // ---------- busca de contexto ----------

    @Test
    fun relevantPiecesScoreHigher() {
        fun piece(text: String) = ContextPiece("t", text, null, "k")
        val pieces = listOf(
            piece("A cidade de Pedra Alta fica no alto da montanha e ninguém sobe lá."),
            piece("Mirela afiou a espada junto da fogueira antes da batalha."),
            piece("O mercado vendia peixe, sal e tecidos coloridos."),
        )
        val scores = scorePieces(pieces, "Como Mirela se prepara para a batalha?")
        assertTrue(scores[1] > scores[0] && scores[1] > scores[2])
        assertEquals(listOf(0.0, 0.0, 0.0), scorePieces(pieces, "para como com"))
    }

    private fun sampleBook(): BookContext {
        val ch1 = ChapterMeta("c1", "A chegada")
        val ch2 = ChapterMeta("c2", "A batalha")
        val mirela = UniverseEntry(
            "e1", EntryKind.Personagem, "Mirela", subtitle = "Capitã da guarda",
            attributes = listOf(EntryAttribute("a1", "eyes", "Olhos", "verdes", sealed = true)),
        )
        val outro = UniverseEntry("e2", EntryKind.Local, "Porto Cinzento", subtitle = "Cidade portuária")
        return BookContext(
            manifest = BookManifest(id = "b", title = "Crônicas", genre = "Fantasia", synopsis = "Uma guerra por um porto.", createdAt = 0, modifiedAt = 0, chapters = listOf(ch1, ch2)),
            chapterBodies = mapOf(
                "c1" to "Mirela chegou ao Porto Cinzento ao amanhecer.\nOs barcos já partiam.",
                "c2" to "A batalha começou quando o sino tocou. Mirela ergueu a espada.",
            ),
            universe = Universe(listOf(mirela, outro)),
            outline = Outline(listOf(Beat("b1", "Cerco ao porto", "A frota inimiga cerca o porto.", act = "Ato II"))),
            currentChapterId = "c2",
        )
    }

    @Test
    fun contextIncludesMentionedEntryWithSealedFactsAndOutline() {
        val built = ContextBuilder.build(sampleBook(), "Como Mirela reage ao cerco?", emptyList(), budgetTokens = 4000)
        assertTrue("Título: Crônicas" in built.system)
        assertTrue("Cerco ao porto" in built.system) // roteiro sempre vai
        assertTrue("Olhos: verdes (fato fixo, não pode mudar)" in built.system)
        assertTrue(built.used.any { it.ref == AiRef.Entry("e1") })
    }

    /** Modos salvos na versão 1: os de fábrica ganham o texto novo, os editados ficam, e os novos aparecem. */
    @Test
    fun savedModesAreUpgradedWithoutLosingEdits() {
        val v1Revisor = "Aja como revisor profissional de português do Brasil. Aponte erros de ortografia, acentuação, gramática, " +
            "concordância, regência e pontuação, além de palavras repetidas e vícios de linguagem. Para cada problema, " +
            "mostre o trecho original, a correção e uma explicação curta. Não mude o estilo nem a voz do autor."
        val old = AiConfig(
            modes = listOf(
                com.cuboide.escriba.core.ai.AiMode("revisor", "Revisor", v1Revisor),
                com.cuboide.escriba.core.ai.AiMode("editor", "Meu editor", "Minhas regras."),
                com.cuboide.escriba.core.ai.AiMode("x", "Poeta", "Em versos."),
            ),
        )
        val up = old.withUpgradedModes()
        val byId = up.modes.associateBy { it.id }
        assertTrue("Antes:" in byId.getValue("revisor").instructions) // texto novo
        assertEquals("Minhas regras.", byId.getValue("editor").instructions) // editado pelo autor: fica
        assertEquals("Em versos.", byId.getValue("x").instructions)
        assertTrue("painel" in byId && "continuidade" in byId) // novos
        assertTrue("leitor-beta" !in byId) // apagado pelo autor: não volta
        assertEquals(up, up.withUpgradedModes()) // idempotente
        // Quem começa agora já tem os seis modos novos.
        assertEquals(6, AiConfig().withUpgradedModes().modes.size)
    }

    @Test
    fun aiStoreUpgradesOldFilesOnLoad() {
        val fs = InMemoryFileStore()
        fs.writeText("/data/ia.json", """{"apiKey":"k","modes":[{"id":"editor","name":"Editor","instructions":"x"}]}""")
        val cfg = AiStore(fs, "/data/ia.json").current
        assertEquals("k", cfg.apiKey)
        assertEquals(listOf("editor", "painel", "continuidade"), cfg.modes.map { it.id })
    }

    /** As três camadas de instruções do autor entram no prompt, depois das regras fixas e antes do contexto. */
    @Test
    fun authorInstructionsGoIntoThePromptInLayers() {
        val mode = com.cuboide.escriba.core.ai.DEFAULT_AI_MODES.first { it.id == "revisor" }
        val built = ContextBuilder.build(
            sampleBook(), "oi", emptyList(), 4000,
            instructions = com.cuboide.escriba.core.ai.AuthorInstructions("Seja direto.", "Prólogo em 1ª pessoa.", mode),
        )
        val s = built.system
        assertTrue("## Como o autor quer ser ajudado\nSeja direto." in s)
        assertTrue("## Sobre este livro\nPrólogo em 1ª pessoa." in s)
        assertTrue("## Seu papel nesta conversa: Revisor" in s)
        assertTrue(s.indexOf("Regras:") < s.indexOf("# Instruções do autor"))
        assertTrue(s.indexOf("# Instruções do autor") < s.indexOf("# Contexto do livro"))
        // Sem instruções, a seção nem aparece.
        assertFalse("# Instruções do autor" in ContextBuilder.build(sampleBook(), "oi", emptyList(), 4000).system)
    }

    /** "Livro inteiro": todos os capítulos e fichas vão inteiros, com o capítulo em foco e as instruções de comparação. */
    @Test
    fun fullBookSendsEveryChapterAndEntry() {
        val built = ContextBuilder.build(sampleBook(), "o que acha desse capítulo?", listOf(AiRef.Chapter("c2")), 50_000, fullBook = true)
        assertEquals(0, built.cutChapters)
        assertTrue("Os barcos já partiam." in built.system) // capítulo 1, nem citado nem achado por busca
        assertTrue("Mirela ergueu a espada." in built.system)
        assertTrue("Cidade portuária" in built.system) // ficha que não aparece no pedido
        assertTrue("Capítulo em foco: Capítulo 2 — A batalha." in built.system)
        assertTrue("repetições" in built.system)
        assertTrue(built.used.none { it.label.contains("(passagem") })
        assertEquals(1, built.used.count { it.label.startsWith("Capítulo 2") }) // o capítulo em foco não se repete
    }

    @Test
    fun fullBookShortensChaptersFairlyWhenItDoesNotFit() {
        val long = (1..300).joinToString("\n") { "Linha longa número $it do primeiro capítulo, que não cabe." }
        val book = sampleBook().let { BookContext(it.manifest, it.chapterBodies + ("c1" to long), it.universe, it.outline, null) }
        val built = ContextBuilder.build(book, "analise", emptyList(), 2_000, fullBook = true)
        assertEquals(1, built.cutChapters)
        assertTrue(built.used.any { it.label == "Capítulo 1 — A chegada (encurtado)" })
        assertTrue(built.used.any { it.label == "Capítulo 2 — A batalha (inteiro)" }) // o curto vai inteiro
    }

    @Test
    fun fairSharesGiveShortTextsAllTheyNeed() {
        assertEquals(listOf(10, 45, 45), ContextBuilder.fairShares(listOf(10, 100, 200), 100))
        assertEquals(listOf(10, 20), ContextBuilder.fairShares(listOf(10, 20), 1000))
    }

    /** Um pedido genérico sobre o capítulo aberto não puxa passagens de outros capítulos por coincidência de palavras. */
    @Test
    fun genericRequestDoesNotPullPassagesFromOtherChapters() {
        val book = sampleBook().let {
            BookContext(
                it.manifest, it.chapterBodies + ("c2" to "Veja bem: a batalha foi analisada por todos. Ficou bom para eles."),
                it.universe, it.outline, currentChapterId = "c1",
            )
        }
        val built = ContextBuilder.build(book, "Analise o capitulo e veja se está bom", listOf(AiRef.Chapter("c1")), budgetTokens = 4000)
        assertTrue(built.used.none { it.label.contains("(passagem") }, built.used.toString())
    }

    /** Já um pedido com conteúdo de verdade acha a passagem certa em outro capítulo. */
    @Test
    fun specificRequestFindsThePassageInAnotherChapter() {
        val built = ContextBuilder.build(sampleBook(), "o que acontece quando o sino toca na batalha?", listOf(AiRef.Chapter("c1")), budgetTokens = 4000)
        assertTrue(built.used.any { it.label.startsWith("Capítulo 2") && it.label.contains("(passagem") }, built.used.toString())
    }

    @Test
    fun explicitReferencesComeFirstAndAreNotRepeated() {
        val built = ContextBuilder.build(sampleBook(), "resuma", listOf(AiRef.Chapter("c1")), budgetTokens = 4000)
        assertEquals(AiRef.Chapter("c1"), built.used.first().ref)
        // O capítulo apontado entra inteiro uma vez só, sem "trechos" repetidos dele.
        assertEquals(1, built.used.count { it.label.startsWith("Capítulo 1") })
        assertTrue("Os barcos já partiam." in built.system)
    }

    @Test
    fun contextStaysInsideTheBudget() {
        val long = (1..400).joinToString("\n") { "Mirela caminhou pela cidade número $it carregando a espada." }
        val book = sampleBook().let {
            BookContext(it.manifest, it.chapterBodies + ("c1" to long), it.universe, it.outline, it.currentChapterId)
        }
        val built = ContextBuilder.build(book, "Mirela espada cidade", listOf(AiRef.Chapter("c1")), budgetTokens = 1000)
        // 1000 tokens ≈ 4000 caracteres; as regras fixas do prompt somam pouco acima disso.
        assertTrue(built.system.length < 4000 + 1500, "tamanho ${built.system.length}")
        assertTrue("cortado por limite de contexto" in built.system)
    }

    @Test
    fun selectionAndOtherBooksAreAttached() {
        val other = sampleBook().let { BookContext(it.manifest.copy(title = "Segundo livro", synopsis = "A continuação."), emptyMap(), it.universe, Outline()) }
        val built = ContextBuilder.build(
            sampleBook(), "melhore", listOf(AiRef.Selection("c2", "A batalha começou."), AiRef.Book("/outro")),
            budgetTokens = 4000, others = mapOf("/outro" to other),
        )
        assertTrue("A batalha começou." in built.system)
        assertTrue("Outro livro: Segundo livro" in built.system)
        assertNotNull(built.used.firstOrNull { it.label == "Trecho selecionado" })
    }
}
