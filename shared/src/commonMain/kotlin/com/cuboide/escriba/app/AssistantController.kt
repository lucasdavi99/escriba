package com.cuboide.escriba.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import com.cuboide.escriba.core.ai.AiConfig
import com.cuboide.escriba.core.ai.AiException
import com.cuboide.escriba.core.ai.AiRef
import com.cuboide.escriba.core.ai.AiRequest
import com.cuboide.escriba.core.ai.AiTask
import com.cuboide.escriba.core.ai.AuthorInstructions
import com.cuboide.escriba.core.ai.BookContext
import com.cuboide.escriba.core.ai.BookConversations
import com.cuboide.escriba.core.ai.ChatMessage
import com.cuboide.escriba.core.ai.ChatRole
import com.cuboide.escriba.core.ai.ChatTurn
import com.cuboide.escriba.core.ai.ContextBuilder
import com.cuboide.escriba.core.ai.Conversation
import com.cuboide.escriba.core.model.chapterNumbers
import com.cuboide.escriba.core.universe.fold
import com.cuboide.escriba.platform.copyToClipboard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Uma opção da lista que abre ao digitar `@` no prompt. */
data class RefOption(val label: String, val detail: String, val ref: AiRef)

/**
 * O assistente de IA: conversas do livro aberto, o que vai de contexto em cada pergunta e a aplicação das respostas
 * no texto. A IA só sugere; o texto do livro só muda por ações do autor (inserir, substituir), sempre com uma versão
 * guardada antes.
 */
class AssistantController(
    private val c: EscribaController,
    private val services: EscribaServices,
    private val scope: CoroutineScope,
) {
    var config by mutableStateOf(services.ai.current); private set
    var conversations by mutableStateOf<List<Conversation>>(emptyList()); private set
    var currentId by mutableStateOf<String?>(null); private set
    var draft by mutableStateOf(TextFieldValue(""))
    var draftRefs by mutableStateOf<List<AiRef>>(emptyList()); private set
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    /** Pedido à espera do "ok" do aviso de privacidade. */
    var consentPending by mutableStateOf(false); private set
    var testing by mutableStateOf(false); private set
    var testResult by mutableStateOf<String?>(null); private set

    // Painel flutuante (posição e tamanho em dp; `null` = canto padrão).
    var panelVisible by mutableStateOf(false)
    var panelX by mutableStateOf<Float?>(null)
    var panelY by mutableStateOf<Float?>(null)
    var panelWidth by mutableStateOf(380f)
    var panelHeight by mutableStateOf(520f)

    private var job: Job? = null
    /** Livro a que as conversas pertencem (o gravador não usa o livro aberto, que pode já ter mudado). */
    private var bookPath: String? = null
    private var pendingSend: Pair<String, AiTask?>? = null
    /** Onde estava o trecho selecionado de cada pergunta, para substituí-lo depois (vale só nesta sessão). */
    private val selectionRanges = HashMap<String, IntRange>()
    /** Onde está, no capítulo, o trecho que o autor anexou ao rascunho. */
    private var draftSelectionRange: IntRange? = null
    /** Capítulo em que o autor tirou o "capítulo aberto" do contexto (volta a valer ao trocar de capítulo). */
    private var autoChapterOffFor by mutableStateOf<String?>(null)

    val current: Conversation? get() = conversations.firstOrNull { it.id == currentId }

    // ---------- Instruções do autor (gerais, do livro e modo) ----------

    /** Instruções do autor sobre o livro aberto (regras da história, tom, público), guardadas no livro. */
    var bookInstructions by mutableStateOf(""); private set
    /** Modo para a próxima conversa nova: o último escolhido. */
    private var draftModeId by mutableStateOf<String?>(null)

    /** Modo da conversa aberta (ou da próxima, se ainda não há conversa); `null` = livre. */
    val modeId: String? get() = current?.let { it.modeId } ?: if (current == null) draftModeId else null
    val mode: com.cuboide.escriba.core.ai.AiMode? get() = modeId?.let { id -> config.modes.firstOrNull { it.id == id } }

    /** Troca o modo da conversa aberta (fica guardado nela) e das próximas conversas novas. */
    fun setMode(id: String?) {
        draftModeId = id
        val conv = current ?: return
        conversations = conversations.map { if (it.id == conv.id) it.copy(modeId = id) else it }
        persist()
    }

    fun saveBookInstructions(text: String) {
        val path = bookPath ?: return
        bookInstructions = text
        services.books.saveBookInstructions(path, text)
    }

    fun addMode(): com.cuboide.escriba.core.ai.AiMode {
        val mode = com.cuboide.escriba.core.ai.AiMode(services.newId(), "Novo modo")
        updateConfig { it.copy(modes = it.modes + mode) }
        return mode
    }

    fun updateMode(mode: com.cuboide.escriba.core.ai.AiMode) =
        updateConfig { cfg -> cfg.copy(modes = cfg.modes.map { if (it.id == mode.id) mode else it }) }

    /** Apaga o modo; conversas que o usavam passam a ser livres. */
    fun removeMode(id: String) = updateConfig { cfg -> cfg.copy(modes = cfg.modes.filterNot { it.id == id }) }

    fun restoreDefaultModes() = updateConfig { it.copy(modes = com.cuboide.escriba.core.ai.DEFAULT_AI_MODES) }

    // ---------- Livro inteiro ----------

    /** "Livro inteiro": cada pergunta leva todos os capítulos e fichas, para comparar o capítulo com o livro todo. */
    var fullBook by mutableStateOf(false); private set
    /** Tamanho estimado do livro inteiro em tokens (calculado ao ligar), para o autor ter noção do custo. */
    var fullBookTokens by mutableStateOf(0); private set

    fun toggleFullBook() {
        fullBook = !fullBook
        if (fullBook) refreshFullBookEstimate()
    }

    private fun refreshFullBookEstimate() {
        if (c.book == null) return
        val ctx = currentBookContext()
        val chapters = ctx.chapterBodies.values.sumOf { com.cuboide.escriba.core.text.stripMarkup(it).length }
        val entries = ctx.universe.entries.sumOf { ContextBuilder.entryText(it).length + it.name.length }
        fullBookTokens = (chapters + entries + 2000) / 4
    }

    // ---------- Conexão ----------

    fun updateConfig(transform: (AiConfig) -> AiConfig) {
        services.ai.update(transform)
        config = services.ai.current
        testResult = null
    }

    fun togglePanel() {
        if (c.book == null) return
        panelVisible = !panelVisible
    }

    /** Pede uma resposta curta ao provedor para conferir chave, modelo e endereço. */
    fun testConnection() {
        if (testing) return
        val cfg = config
        if (!cfg.isConfigured) {
            testResult = "Preencha a chave e o modelo primeiro."
            return
        }
        testing = true
        testResult = null
        scope.launch {
            try {
                val done = kotlinx.coroutines.withTimeoutOrNull(TEST_TIMEOUT_MS) {
                    services.aiProvider(cfg).stream(
                        AiRequest("", listOf(ChatTurn(ChatRole.User, "Responda apenas: ok")), cfg.effectiveModel, maxTokens = 16),
                    ).collect { }
                    true
                }
                testResult = if (done == true) "Conexão funcionando." else "O provedor não respondeu em ${TEST_TIMEOUT_MS / 1000} s. Confira o endereço e a sua conexão."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                testResult = e.message ?: "Falha na conexão."
            } finally {
                testing = false
            }
        }
    }

    // ---------- Livro aberto ----------

    fun onBookOpened(path: String) {
        stop()
        bookPath = path
        conversations = services.books.loadConversations(path).conversations.sortedByDescending { it.modifiedAt }
        currentId = conversations.firstOrNull()?.id
        bookInstructions = services.books.loadBookInstructions(path)
        draft = TextFieldValue("")
        draftRefs = emptyList()
        error = null
        selectionRanges.clear()
    }

    fun onBookClosed() {
        stop()
        persist()
        bookPath = null
        fullBook = false
        bookInstructions = ""
        conversations = emptyList()
        currentId = null
        panelVisible = false
        draftRefs = emptyList()
        error = null
    }

    // ---------- Conversas ----------

    fun newConversation() {
        stop()
        currentId = null
        draftRefs = emptyList()
        error = null
    }

    fun openConversation(id: String) {
        if (id == currentId) return
        stop()
        currentId = id
        error = null
    }

    fun deleteConversation(id: String) {
        if (id == currentId) stop()
        conversations = conversations.filterNot { it.id == id }
        if (currentId == id) currentId = conversations.firstOrNull()?.id
        persist()
    }

    // ---------- Referências (@) ----------

    fun addRef(ref: AiRef) {
        if (ref !in draftRefs) draftRefs = draftRefs + ref
    }

    fun removeRef(ref: AiRef) {
        draftRefs = draftRefs - ref
        if (ref is AiRef.Selection) draftSelectionRange = null
    }

    /**
     * O capítulo aberto no Manuscrito vai inteiro como contexto, sem o autor precisar apontá-lo. Aparece como chip e
     * pode ser tirado ([dismissAutoChapter]).
     */
    val autoChapterRef: AiRef.Chapter?
        get() {
            val id = c.currentChapterId ?: return null
            if (c.section != Section.Manuscrito || autoChapterOffFor == id) return null
            val ref = AiRef.Chapter(id)
            return if (ref in draftRefs) null else ref
        }

    fun dismissAutoChapter() {
        autoChapterOffFor = c.currentChapterId
    }

    /** Texto selecionado no capítulo aberto que ainda não foi anexado; `null` se não há seleção. */
    val selectionAvailable: String?
        get() {
            if (c.currentChapterId == null || draftRefs.any { it is AiRef.Selection }) return null
            val sel = c.editor.selection
            if (sel.collapsed) return null
            return c.editor.text.substring(sel.min, sel.max).takeIf { it.isNotBlank() }
        }

    /** Anexa ao pedido o trecho selecionado no editor (só quando o autor pede: seleções antigas não vão sozinhas). */
    fun attachSelection(): Boolean {
        val id = c.currentChapterId ?: return false
        val text = selectionAvailable ?: return false
        val sel = c.editor.selection
        draftRefs = draftRefs + AiRef.Selection(id, text)
        draftSelectionRange = sel.min until sel.max
        return true
    }

    fun refLabel(ref: AiRef): String {
        val b = c.book ?: return "Referência"
        return when (ref) {
            is AiRef.Chapter -> b.manifest.chapters.firstOrNull { it.id == ref.id }?.let { chapterName(it.id) } ?: "Capítulo removido"
            is AiRef.Beat -> "Cena: " + (b.outline.beats.firstOrNull { it.id == ref.id }?.title ?: "removida")
            is AiRef.Entry -> b.universe.entries.firstOrNull { it.id == ref.id }?.name ?: "Ficha removida"
            is AiRef.Selection -> "Trecho selecionado (${com.cuboide.escriba.core.text.countWords(ref.text)} palavras)"
            AiRef.Synopsis -> "Sinopse"
            AiRef.Outline -> "Enredo inteiro"
            is AiRef.Book -> "Livro: " + (c.library.firstOrNull { it.path == ref.path }?.manifest?.title ?: "outro livro")
        }
    }

    /** Opções do `@`: capítulos, cenas, fichas, sinopse, enredo e outros livros, filtradas por [query]. */
    fun refOptions(query: String): List<RefOption> {
        val b = c.book ?: return emptyList()
        val q = fold(query.trim())
        val all = buildList {
            add(RefOption("Sinopse", "do livro", AiRef.Synopsis))
            add(RefOption("Enredo inteiro", "todas as cenas", AiRef.Outline))
            b.manifest.chapters.forEach { add(RefOption(chapterName(it.id), "${it.words} palavras", AiRef.Chapter(it.id))) }
            b.outline.beats.forEach { add(RefOption("Cena: ${it.title}", it.act.ifBlank { "Enredo" }, AiRef.Beat(it.id))) }
            b.universe.entries.forEach { add(RefOption(it.name.ifBlank { "(sem nome)" }, it.kind.label, AiRef.Entry(it.id))) }
            c.library.filter { it.path != b.path }.forEach {
                add(RefOption("Livro: ${it.manifest.title}", "outro livro", AiRef.Book(it.path)))
            }
        }
        val filtered = if (q.isEmpty()) all else all.filter { fold(it.label).contains(q) || fold(it.detail).contains(q) }
        return filtered.filter { it.ref !in draftRefs }.take(30)
    }

    private fun chapterName(id: String): String {
        val b = c.book ?: return "Capítulo"
        val meta = b.manifest.chapters.firstOrNull { it.id == id } ?: return "Capítulo"
        val n = chapterNumbers(b.manifest.chapters)[id]
        return when {
            n != null -> "Cap. $n — ${meta.title}"
            meta.title.isBlank() || meta.title.equals(meta.kind.label, ignoreCase = true) -> meta.kind.label
            else -> "${meta.kind.label} — ${meta.title}"
        }
    }

    // ---------- Enviar ----------

    /** O texto do rascunho vai como pedido. */
    fun sendDraft() = send(draft.text.trim(), null)

    /** Um pedido pronto ([AiTask]); o que o autor já digitou entra como complemento. */
    fun runTask(task: AiTask) {
        val extra = draft.text.trim()
        send(if (extra.isEmpty()) task.prompt else task.prompt + "\n\n" + extra, task)
    }

    fun acceptConsent() {
        updateConfig { it.copy(consentGiven = true) }
        consentPending = false
        pendingSend?.let { (text, task) -> pendingSend = null; send(text, task) }
    }

    fun declineConsent() {
        consentPending = false
        pendingSend = null
    }

    fun send(text: String, task: AiTask?) {
        val book = c.book ?: return
        if (busy || text.isBlank()) return
        val cfg = config
        if (!cfg.isConfigured) {
            error = "Conecte uma IA em Preferências (provedor, chave e modelo)."
            return
        }
        if (!cfg.consentGiven) {
            pendingSend = text to task
            consentPending = true
            return
        }
        error = null
        c.flushSave()

        // O que vai de contexto: o que o autor apontou (chips), o capítulo aberto inteiro e, nos pedidos prontos que
        // trabalham sobre um trecho, a seleção do editor. Uma seleção esquecida no editor não vai sozinha.
        if (task?.needsSelection == true && draftRefs.none { it is AiRef.Selection } && !attachSelection()) {
            error = "Selecione no capítulo o trecho para “${task.label}”."
            return
        }
        val refs = draftRefs.toMutableList()
        // Capítulos citados no pedido ("o capítulo 2", "o prólogo") vão inteiros, antes do capítulo aberto.
        com.cuboide.escriba.core.ai.chaptersNamedIn(text, book.manifest.chapters).take(MAX_NAMED_CHAPTERS).forEach { id ->
            val ref = AiRef.Chapter(id)
            if (ref !in refs) refs += ref
        }
        autoChapterRef?.let { if (it !in refs) refs += it }
        val chapterId = c.currentChapterId
        if (task != null && chapterId != null && refs.none { it is AiRef.Chapter || it is AiRef.Selection }) {
            refs += AiRef.Chapter(chapterId)
        }
        val userId = services.newId()
        draftSelectionRange?.let { r -> if (refs.any { it is AiRef.Selection }) selectionRanges[userId] = r }

        val others = refs.filterIsInstance<AiRef.Book>().mapNotNull { r ->
            runCatching { r.path to loadBookContext(r.path) }.getOrNull()
        }.toMap()
        val history = current?.messages.orEmpty()
        val previousQuestion = history.lastOrNull { it.role == ChatRole.User }?.text.orEmpty()
        val built = ContextBuilder.build(
            book = currentBookContext(),
            query = if (text.length < 60) "$previousQuestion $text" else text,
            refs = refs,
            budgetTokens = if (fullBook) cfg.fullBookBudgetTokens else cfg.contextBudgetTokens,
            others = others,
            fullBook = fullBook,
            instructions = AuthorInstructions(cfg.generalInstructions, bookInstructions, mode),
        )
        // Uma análise do livro todo costuma ser longa: deixa a resposta crescer mais.
        val maxTokens = if (fullBook) 8192 else 4096
        if (fullBook) {
            refreshFullBookEstimate()
            if (built.cutChapters > 0) {
                c.message = "O livro não coube inteiro: ${built.cutChapters} capítulo(s) foram encurtados. Aumente o limite em Preferências."
            }
        }

        val now = services.now()
        val userMsg = ChatMessage(userId, ChatRole.User, text, refs, built.used, now)
        val assistantId = services.newId()
        val conv = (current ?: Conversation(services.newId(), title = titleFrom(text), modifiedAt = now, modeId = draftModeId))
        val turns = (conv.messages.takeLast(MAX_HISTORY) + userMsg).map { ChatTurn(it.role, it.text) }
        putConversation(conv.copy(messages = conv.messages + userMsg + ChatMessage(assistantId, ChatRole.Assistant, "", createdAt = now), modifiedAt = now))
        currentId = conv.id
        draft = TextFieldValue("")
        draftRefs = emptyList()
        draftSelectionRange = null
        busy = true

        job = scope.launch {
            val out = StringBuilder()
            try {
                services.aiProvider(cfg).stream(AiRequest(built.system, turns, cfg.effectiveModel, maxTokens)).collect { chunk ->
                    out.append(chunk)
                    setAssistantText(conv.id, assistantId, out.toString())
                }
                if (out.isBlank()) error = "A IA não devolveu texto. Tente de novo ou confira o modelo."
            } catch (e: CancellationException) {
                throw e
            } catch (e: AiException) {
                error = e.message
            } catch (e: Exception) {
                error = "Não foi possível falar com a IA: ${e.message}"
            } finally {
                busy = false
                if (out.isBlank()) removeMessage(conv.id, assistantId) else persist()
            }
        }
    }

    /** Para a resposta em andamento, guardando o que já chegou. */
    fun stop() {
        job?.cancel()
        job = null
        busy = false
    }

    // ---------- Aplicar a resposta ----------

    /** Texto da IA no formato do manuscrito: um parágrafo por linha. */
    private fun asManuscript(text: String): String = com.cuboide.escriba.core.text.chatMarkdownToManuscript(text)

    fun copy(message: ChatMessage) {
        copyToClipboard(message.text)
        c.message = "Resposta copiada."
    }

    /** Insere a resposta no cursor do capítulo aberto (em parágrafo próprio). */
    fun insertAtCursor(message: ChatMessage) {
        val id = c.currentChapterId
        if (id == null) {
            c.message = "Abra um capítulo para inserir o texto."
            return
        }
        val text = c.editor.text
        val at = c.editor.selection.max.coerceIn(0, text.length)
        val before = if (at > 0 && text[at - 1] != '\n') "\n" else ""
        val after = if (at < text.length && text[at] != '\n') "\n" else ""
        c.replaceRange(at, at, before + asManuscript(message.text) + after, "Antes de inserir texto da IA")
        c.message = "Texto inserido. A versão anterior está em Versões."
    }

    /** O trecho que a pergunta usava, se ele ainda está no capítulo; senão `null`. */
    fun selectionTarget(question: ChatMessage): IntRange? {
        val ref = question.refs.filterIsInstance<AiRef.Selection>().firstOrNull() ?: return null
        if (ref.chapterId != c.currentChapterId) return null
        val text = c.editor.text
        selectionRanges[question.id]?.let { r ->
            if (r.last < text.length && text.substring(r.first, r.last + 1) == ref.text) return r
        }
        val first = text.indexOf(ref.text)
        if (first < 0 || text.indexOf(ref.text, first + 1) >= 0) return null
        return first until first + ref.text.length
    }

    /** Antes e depois da troca do trecho da pergunta pela resposta, para o autor conferir antes de aplicar. */
    fun replacementPreview(answer: ChatMessage): Pair<String, String>? {
        val question = questionOf(answer) ?: return null
        val ref = question.refs.filterIsInstance<AiRef.Selection>().firstOrNull() ?: return null
        return ref.text to asManuscript(answer.text)
    }

    fun replaceSelection(answer: ChatMessage): Boolean {
        val question = questionOf(answer) ?: return false
        val range = selectionTarget(question)
        if (range == null) {
            c.message = "Não encontrei mais o trecho original no capítulo aberto."
            return false
        }
        val ok = c.replaceRange(range.first, range.last + 1, asManuscript(answer.text), "Antes de trocar trecho pela IA")
        if (ok) c.message = "Trecho substituído. A versão anterior está em Versões."
        return ok
    }

    fun saveAsNote(message: ChatMessage) {
        c.addNoteBlockWith("Do assistente", message.text.trim())
        c.message = "Resposta guardada nas anotações do livro."
    }

    private fun questionOf(answer: ChatMessage): ChatMessage? {
        val msgs = current?.messages ?: return null
        val i = msgs.indexOfFirst { it.id == answer.id }
        return if (i > 0) msgs[i - 1].takeIf { it.role == ChatRole.User } else null
    }

    // ---------- Internos ----------

    private fun titleFrom(text: String): String = text.lineSequence().first().trim().take(48).ifEmpty { "Nova conversa" }

    private fun currentBookContext(): BookContext {
        val b = c.book!!
        val bodies = b.manifest.chapters.associate { meta ->
            meta.id to if (meta.id == c.currentChapterId) c.editor.text else services.books.readChapter(b.path, meta.id)
        }
        return BookContext(b.manifest, bodies, b.universe, b.outline, c.currentChapterId)
    }

    private fun loadBookContext(path: String): BookContext {
        val manifest = services.books.loadManifest(path)
        val bodies = services.books.readAllChapters(path, manifest).associate { (m, text) -> m.id to text }
        return BookContext(manifest, bodies, services.books.loadUniverse(path), services.books.loadOutline(path))
    }

    private fun putConversation(conv: Conversation) {
        conversations = (listOf(conv) + conversations.filterNot { it.id == conv.id })
    }

    private fun setAssistantText(convId: String, messageId: String, text: String) {
        conversations = conversations.map { cv ->
            if (cv.id != convId) cv else cv.copy(messages = cv.messages.map { if (it.id == messageId) it.copy(text = text) else it })
        }
    }

    private fun removeMessage(convId: String, messageId: String) {
        conversations = conversations.map { cv ->
            if (cv.id != convId) cv else cv.copy(messages = cv.messages.filterNot { it.id == messageId })
        }
        persist()
    }

    private fun persist() {
        val path = bookPath ?: return
        val clean = conversations.map { cv -> cv.copy(messages = cv.messages.filterNot { it.role == ChatRole.Assistant && it.text.isEmpty() }) }
        services.books.saveConversations(path, BookConversations(clean))
    }

    companion object {
        /** Mensagens anteriores reenviadas junto com a nova pergunta. */
        const val MAX_HISTORY = 12
        const val TEST_TIMEOUT_MS = 45_000L
        /** Capítulos citados no pedido que entram inteiros (o resto entra só por passagens relevantes). */
        const val MAX_NAMED_CHAPTERS = 3
    }
}
