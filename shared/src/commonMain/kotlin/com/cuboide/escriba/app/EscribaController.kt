package com.cuboide.escriba.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.cuboide.escriba.core.export.ExportFormat
import com.cuboide.escriba.core.export.SCENE_BREAK_TEXT
import com.cuboide.escriba.core.export.buildExportDocument
import com.cuboide.escriba.core.export.exportHtml
import com.cuboide.escriba.core.export.exportMarkdown
import com.cuboide.escriba.core.export.exportPlainText
import com.cuboide.escriba.core.export.imagePaths
import com.cuboide.escriba.core.board.PlotBoards
import com.cuboide.escriba.core.board.ensureBoard
import com.cuboide.escriba.core.model.Beat
import com.cuboide.escriba.core.model.BookManifest
import com.cuboide.escriba.core.model.BookNotes
import com.cuboide.escriba.core.model.BookPartGroup
import com.cuboide.escriba.core.model.ChapterKind
import com.cuboide.escriba.core.model.NoteBlock
import com.cuboide.escriba.core.model.chapterNumbers
import com.cuboide.escriba.core.model.BookSummary
import com.cuboide.escriba.core.model.ChapterMeta
import com.cuboide.escriba.core.model.ChapterStatus
import com.cuboide.escriba.core.model.EntryKind
import com.cuboide.escriba.core.model.Outline
import com.cuboide.escriba.core.model.Universe
import com.cuboide.escriba.core.model.UniverseEntry
import com.cuboide.escriba.core.outline.StructureTemplate
import com.cuboide.escriba.core.outline.applyTemplate
import com.cuboide.escriba.core.outline.move
import com.cuboide.escriba.core.outline.remove
import com.cuboide.escriba.core.outline.upsert
import com.cuboide.escriba.core.storage.AppSettings
import com.cuboide.escriba.core.storage.BookRepository
import com.cuboide.escriba.core.storage.FileStore
import com.cuboide.escriba.core.storage.ProductivityStore
import com.cuboide.escriba.core.storage.ProductivitySummary
import com.cuboide.escriba.core.ai.createAiProvider
import com.cuboide.escriba.core.storage.RestoreOutcome
import com.cuboide.escriba.core.storage.SettingsStore
import com.cuboide.escriba.core.storage.Snapshot
import com.cuboide.escriba.core.storage.SnapshotStore
import com.cuboide.escriba.core.storage.TrashItem
import com.cuboide.escriba.core.storage.TrashStore
import com.cuboide.escriba.core.storage.TrashKind
import com.cuboide.escriba.core.board.ImageFrame
import com.cuboide.escriba.core.model.EntryImage
import com.cuboide.escriba.core.calendar.CalendarTemplate
import com.cuboide.escriba.core.calendar.CalendarTemplates
import com.cuboide.escriba.core.calendar.ParsedDate
import com.cuboide.escriba.core.calendar.WorldCalendar
import com.cuboide.escriba.core.calendar.WorldDate
import com.cuboide.escriba.core.calendar.parseDate
import com.cuboide.escriba.core.language.LangStyle
import com.cuboide.escriba.core.language.Script
import com.cuboide.escriba.core.language.fontBytes
import com.cuboide.escriba.platform.registerScriptFont
import com.cuboide.escriba.core.language.Language
import com.cuboide.escriba.core.language.Translation
import com.cuboide.escriba.core.language.WorldLanguage
import com.cuboide.escriba.core.language.fnv1a64
import com.cuboide.escriba.core.universe.BookCheck
import com.cuboide.escriba.core.universe.BookUniverse
import com.cuboide.escriba.core.universe.copyEntries
import com.cuboide.escriba.core.universe.imagePaths
import com.cuboide.escriba.core.universe.mergeSaga
import com.cuboide.escriba.core.storage.AcervoMeta
import com.cuboide.escriba.core.storage.AcervoRepository
import com.cuboide.escriba.core.storage.AcervoSummary
import com.cuboide.escriba.core.storage.EscribaJson
import com.cuboide.escriba.core.storage.safeFileName
import com.cuboide.escriba.core.universe.ChapterText
import com.cuboide.escriba.core.universe.FactKind
import com.cuboide.escriba.core.universe.SealStatus
import com.cuboide.escriba.core.universe.TextSpot
import com.cuboide.escriba.core.universe.checkBook
import com.cuboide.escriba.core.universe.schema
import com.cuboide.escriba.core.universe.sealSuggestions
import com.cuboide.escriba.core.universe.sealText
import com.cuboide.escriba.core.universe.withAttr
import com.cuboide.escriba.core.model.EntryAttribute
import com.cuboide.escriba.core.universe.EntryMentions
import com.cuboide.escriba.core.universe.MentionIndex
import com.cuboide.escriba.core.universe.UNIVERSE_SCHEMA_VERSION
import com.cuboide.escriba.core.universe.findMentions
import com.cuboide.escriba.core.universe.searchTerms
import com.cuboide.escriba.core.storage.fileName
import com.cuboide.escriba.core.storage.joinPath
import com.cuboide.escriba.core.text.BlockKind
import com.cuboide.escriba.core.text.Edit
import com.cuboide.escriba.core.text.MarkKind
import com.cuboide.escriba.core.text.applyTypingRules
import com.cuboide.escriba.core.text.clearFormatting
import com.cuboide.escriba.core.text.countManuscriptWords
import com.cuboide.escriba.core.text.footnoteIds
import com.cuboide.escriba.core.text.insertFootnoteRef
import com.cuboide.escriba.core.text.insertStandaloneLine
import com.cuboide.escriba.core.text.nextFootnoteId
import com.cuboide.escriba.core.text.rebaseFieldEdit
import com.cuboide.escriba.core.text.toggleBlock
import com.cuboide.escriba.core.text.toggleCenter
import com.cuboide.escriba.core.text.toggleInline
import com.cuboide.escriba.core.text.FindOptions
import com.cuboide.escriba.core.text.SearchHit
import com.cuboide.escriba.core.text.TextMatch
import com.cuboide.escriba.core.text.findAll
import com.cuboide.escriba.core.text.replaceAll
import com.cuboide.escriba.core.text.replaceOne
import com.cuboide.escriba.core.text.searchBook
import com.cuboide.escriba.platform.buildDocx
import com.cuboide.escriba.platform.chooseOpenFile
import com.cuboide.escriba.platform.copyToClipboard
import com.cuboide.escriba.platform.coverFacesPng
import com.cuboide.escriba.platform.chooseSaveFile
import com.cuboide.escriba.core.layout.CheckFix
import com.cuboide.escriba.core.layout.CheckItem
import com.cuboide.escriba.core.layout.CheckLevel
import com.cuboide.escriba.core.layout.COVER_SAFE_MM
import com.cuboide.escriba.core.layout.CoverExportRecord
import com.cuboide.escriba.core.layout.CoverLayout
import com.cuboide.escriba.core.layout.CoverOverlay
import com.cuboide.escriba.core.layout.CoverPanel
import com.cuboide.escriba.core.layout.CoverSnapshot
import com.cuboide.escriba.core.layout.loadCoverSnapshot
import com.cuboide.escriba.core.layout.saveCoverSnapshot
import com.cuboide.escriba.core.layout.snapshot
import com.cuboide.escriba.core.layout.EbookCoverSize
import com.cuboide.escriba.core.layout.coverGeometry
import com.cuboide.escriba.core.layout.ebookCoverPixels
import com.cuboide.escriba.core.layout.applyFix
import com.cuboide.escriba.core.layout.checkLayout
import com.cuboide.escriba.core.layout.printSpec
import com.cuboide.escriba.core.layout.CoverSettings
import com.cuboide.escriba.core.layout.ImageSizer
import com.cuboide.escriba.core.layout.LaidOutBook
import com.cuboide.escriba.core.layout.LayoutSettings
import com.cuboide.escriba.core.layout.LayoutTheme
import com.cuboide.escriba.core.layout.layoutBook
import com.cuboide.escriba.core.layout.formatMm
import com.cuboide.escriba.core.layout.layoutCover
import com.cuboide.escriba.core.layout.loadLayoutSettings
import com.cuboide.escriba.core.layout.saveLayoutSettings
import com.cuboide.escriba.platform.imagePixelSize
import com.cuboide.escriba.platform.layoutMeasurer
import com.cuboide.escriba.platform.writeCoverPdf
import com.cuboide.escriba.platform.writeEbookCover
import com.cuboide.escriba.platform.writeLayoutPdf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Dependências do app; nos testes usam um FileStore em memória e um relógio controlado. */
class EscribaServices(
    val fs: FileStore,
    libraryDir: String,
    dataDir: String,
    val now: () -> Long,
    val newId: () -> String,
    val dayKey: (Long) -> String,
    /** Cria o cliente da IA para a conexão do usuário; os testes trocam por um falso. */
    val aiProvider: (com.cuboide.escriba.core.ai.AiConfig) -> com.cuboide.escriba.core.ai.AiProvider = ::createAiProvider,
) {
    val dataDir: String = dataDir
    val books = BookRepository(fs, libraryDir, now, newId)
    val acervo = AcervoRepository(fs, books, now, newId)
    val snapshots = SnapshotStore(fs, now, newId)
    val trash = TrashStore(fs, joinPath(dataDir, "lixeira"), books, now, newId)
    val productivity = ProductivityStore(fs, joinPath(dataDir, "produtividade.json"))
    val settings = SettingsStore(fs, joinPath(dataDir, "settings.json"))
    /** Conexão com a IA (chave incluída), em arquivo próprio: não vai para o livro nem para exportações. */
    val ai = com.cuboide.escriba.core.storage.AiStore(fs, joinPath(dataDir, "ia.json"))
}

/** Seções do menu lateral. As que dependem de um livro aberto ficam desabilitadas na biblioteca. */
enum class Section(val label: String, val needsBook: Boolean) {
    Biblioteca("Biblioteca", false),
    Manuscrito("Manuscrito", true),
    Enredo("Enredo", true),
    Universo("Universo", true),
    Assistente("Assistente", true),
    Metas("Metas", true),
    Versoes("Versões", true),
    Busca("Busca", true),
    Exportar("Exportar", true),
    Diagramacao("Diagramação", true),
    Lixeira("Lixeira", false),
    Preferencias("Preferências", false),
}

enum class SaveState { Salvo, Pendente, Salvando }

/** O que o Universo está mostrando: as fichas ou uma ferramenta do mundo. */
enum class UniverseTool { Fichas, Calendario, Idiomas, Acervo }

/** Universo do Acervo aberto para edição. */
class OpenAcervo(val path: String, meta: AcervoMeta, universe: Universe) {
    var meta by mutableStateOf(meta)
    var universe by mutableStateOf(universe)
}

class OpenBook(
    val path: String,
    manifest: BookManifest,
    universe: Universe,
    outline: Outline,
    notes: BookNotes = BookNotes(),
    boards: PlotBoards = PlotBoards(),
    calendar: WorldCalendar? = null,
    language: WorldLanguage? = null,
) {
    var manifest by mutableStateOf(manifest)
    /** Calendário próprio do livro; `null` = usa o comum (ver [EscribaController.calendar]). */
    var calendar by mutableStateOf(calendar)
    /** Língua inventada do livro (semente e estilo); `null` = ainda não tem. */
    var language by mutableStateOf(language)
    var universe by mutableStateOf(universe)
    var outline by mutableStateOf(outline)
    var notes by mutableStateOf(notes)
    var boards by mutableStateOf(boards)
}

class EscribaController(private val services: EscribaServices, private val scope: CoroutineScope) {
    var settings by mutableStateOf(services.settings.current); private set
    var library by mutableStateOf<List<BookSummary>>(emptyList()); private set
    var book by mutableStateOf<OpenBook?>(null); private set
    var currentChapterId by mutableStateOf<String?>(null); private set
    var editor by mutableStateOf(TextFieldValue("")); private set
    var saveState by mutableStateOf(SaveState.Salvo); private set
    var section by mutableStateOf(Section.Biblioteca); private set
    var focusMode by mutableStateOf(false)
    var showChapters by mutableStateOf(true)
    var showInspector by mutableStateOf(true)
    var productivity by mutableStateOf(services.productivity.summary(today(), HISTORY_DAYS)); private set
    var trashItems by mutableStateOf<List<TrashItem>>(emptyList()); private set
    var snapshots by mutableStateOf<List<Snapshot>>(emptyList()); private set
    var message by mutableStateOf<String?>(null)
    /** Assistente de IA: conversas, contexto e o painel flutuante. */
    val assistant = AssistantController(this, services, scope)

    /** Mapa de enredo do livro aberto. */
    val map = PlotMapController(this, services, scope)
    var enredoView by mutableStateOf(EnredoView.Mapa)
    /** Ficha que o Universo deve abrir ao ser mostrado (vem do chip de um cartão do mapa). */
    var universeFocus by mutableStateOf<String?>(null); private set
    var universeTool by mutableStateOf(UniverseTool.Fichas)
    /** Universo do Acervo aberto (as fichas editadas são as dele); `null` = o universo do livro. */
    var acervo by mutableStateOf<OpenAcervo?>(null); private set

    // Localizar/substituir no capítulo atual
    var findVisible by mutableStateOf(false)
    var findQuery by mutableStateOf("")
    var replaceText by mutableStateOf("")
    var findOptions by mutableStateOf(FindOptions())
    var matches by mutableStateOf<List<TextMatch>>(emptyList()); private set
    var currentMatch by mutableStateOf(-1); private set
    /** Muda a cada [openFind]; a barra de busca usa isso para pegar o foco do teclado. */
    var findFocusRequests by mutableStateOf(0); private set

    /** O editor do capítulo tem o foco do teclado (Ctrl+Z vai para o desfazer do manuscrito). */
    var editorFocused by mutableStateOf(false)
    /** Mudam quando o editor ou o título do capítulo devem pegar o foco (depois de botões, capítulo novo etc.). */
    var editorFocusRequests by mutableStateOf(0); private set
    var titleFocusRequests by mutableStateOf(0); private set
    var canUndo by mutableStateOf(false); private set
    var canRedo by mutableStateOf(false); private set

    // Busca no livro inteiro
    var bookSearchQuery by mutableStateOf("")
    var bookSearchHits by mutableStateOf<List<SearchHit>>(emptyList()); private set

    val libraryDir: String get() = services.books.libraryDir

    // Diagramação
    var layoutSettings by mutableStateOf(LayoutSettings()); private set
    var layout by mutableStateOf<LaidOutBook?>(null); private set
    var layoutBusy by mutableStateOf(false); private set
    var layoutError by mutableStateOf<String?>(null); private set
    private var layoutJob: Job? = null

    private var autosaveJob: Job? = null
    /** Resultado da última conferência do livro (só existe com a conferência ligada ou depois do relatório). */
    var bookCheck by mutableStateOf<BookCheck?>(null); private set
    /** Fonte da escrita (para o editor, a aba Idiomas e a Diagramação). Declarada antes do `init`, que reabre o livro. */
    var scriptFont by mutableStateOf<ScriptFontInfo?>(null); private set
    private var scriptCache: Triple<Long, LangStyle, Pair<Script, ScriptFontInfo>>? = null
    private var languageCache: Pair<WorldLanguage, Language>? = null
    /** Conferência de contradições em espera (ver [scheduleCheck]). */
    private var checkJob: Job? = null
    /** Texto dos capítulos já lidos para a conferência, pela versão (modifiedAt). */
    private val checkTexts = HashMap<String, Pair<Long, String>>()
    /** Gravação da ficha em espera (ver [updateEntry]). */
    private var entrySaveJob: Job? = null
    private val mentionIndex = MentionIndex()
    /**
     * Maior contagem de palavras de cada capítulo já registrada hoje. Só o que passa dessa marca entra em
     * "palavras escritas": apagar e reescrever, ou desfazer e refazer, não conta o mesmo texto duas vezes.
     */
    private val wordsHighWater = mutableMapOf<String, Int>()
    private var highWaterDay = ""
    private var lastEditAt = 0L
    private var pendingActiveMs = 0L
    private val undoStack = ArrayDeque<TextFieldValue>()
    private val redoStack = ArrayDeque<TextFieldValue>()
    private var lastUndoAt = 0L
    private var lastUndoMergeable = false
    /** Texto que o campo conhece enquanto ainda não recebeu nossa versão corrigida (ver [rebaseFieldEdit]). */
    private var fieldBase: String? = null
    private var fieldBaseCursor = 0
    private var fieldBaseJob: Job? = null

    init {
        services.trash.retentionDays = settings.trashRetentionDays
        services.trash.purgeExpired()
        refreshLibrary()
        reopenLastBook()
    }

    /** Ao iniciar, volta para o livro em que se escrevia por último (se ele ainda existir). */
    private fun reopenLastBook() {
        val path = settings.lastBookPath ?: return
        if (library.any { it.path == path }) openBook(path) else updateSettings { it.copy(lastBookPath = null) }
    }

    // ---------- Biblioteca ----------

    fun refreshLibrary() {
        library = services.books.listBooks()
        trashItems = services.trash.list()
        productivity = services.productivity.summary(today(), HISTORY_DAYS)
    }

    fun createBook(title: String, author: String) {
        val created = services.books.createBook(title, author)
        refreshLibrary()
        openBook(created.path)
    }

    fun openBook(path: String) {
        closeBook()
        val manifest = runCatching { services.books.loadManifest(path) }.getOrElse {
            message = "Não foi possível abrir o livro: ${it.message}"
            return
        }
        book = OpenBook(
            path, manifest, services.books.loadUniverse(path), services.books.loadOutline(path), services.books.loadNotes(path),
            services.books.loadBoards(path).ensureBoard(services.newId, "Quadro 1"),
            services.books.loadCalendar(path),
            services.books.loadLanguage(path),
        )
        map.reset()
        layoutSettings = loadLayoutSettings(services.fs, path)
        layout = null
        services.productivity.record(today(), 0, 0, newSession = true)
        updateSettings { it.copy(lastBookPath = path) }
        manifest.chapters.firstOrNull()?.let { selectChapter(it.id) }
        refreshScriptFont()
        section = Section.Manuscrito
        checkTexts.clear()
        bookCheck = null
        scheduleCheck()
        assistant.onBookOpened(path)
    }

    fun goTo(target: Section) {
        if (target.needsBook && book == null) return
        if (section == Section.Manuscrito && target != Section.Manuscrito) flushSave()
        if (section == Section.Enredo && target != Section.Enredo) map.flush()
        if (section == Section.Universo && target != Section.Universo) {
            flushEntrySave()
            // O resto do app (inspetor, Enredo) mostra as fichas do livro, não as do Acervo.
            closeAcervoUniverse()
            if (universeTool == UniverseTool.Acervo) universeTool = UniverseTool.Fichas
        }
        if (target != Section.Manuscrito) focusMode = false
        section = target
        if (target == Section.Diagramacao) requestLayout(0)
    }

    fun closeBook() {
        assistant.onBookClosed()
        registerScriptFont(null)
        scriptFont = null
        flushSave()
        flushEntrySave()
        acervo = null
        mentionIndex.clear()
        map.flush()
        map.reset()
        universeFocus = null
        layoutJob?.cancel()
        layout = null
        layoutBusy = false
        book = null
        section = Section.Biblioteca
        focusMode = false
        currentChapterId = null
        editor = TextFieldValue("")
        resetUndo()
        findVisible = false
        bookSearchHits = emptyList()
        snapshots = emptyList()
        refreshLibrary()
    }

    fun trashBook(summary: BookSummary) {
        if (book?.path == summary.path) closeBook()
        if (settings.lastBookPath == summary.path) updateSettings { it.copy(lastBookPath = null) }
        services.trash.trashBook(summary.path, summary.manifest.title)
        refreshLibrary()
        message = "“${summary.manifest.title}” foi para a lixeira."
    }

    fun updateBookDetails(title: String, author: String, synopsis: String, genre: String, dailyGoal: Int, bookGoal: Int) {
        updateManifest {
            it.copy(
                title = title.trim().ifEmpty { it.title },
                author = author.trim(),
                synopsis = synopsis.trim(),
                genre = genre.trim(),
                dailyGoal = dailyGoal.coerceAtLeast(0),
                bookGoal = bookGoal.coerceAtLeast(0),
            )
        }
    }

    fun setGoals(dailyGoal: Int, bookGoal: Int) =
        updateManifest { it.copy(dailyGoal = dailyGoal.coerceAtLeast(0), bookGoal = bookGoal.coerceAtLeast(0)) }

    // ---------- Capítulos ----------

    val currentChapter: ChapterMeta?
        get() = book?.manifest?.chapters?.firstOrNull { it.id == currentChapterId }

    fun selectChapter(id: String) {
        val b = book ?: return
        if (id == currentChapterId) return
        flushSave()
        val body = services.books.readChapter(b.path, id)
        currentChapterId = id
        editor = TextFieldValue(body)
        resetUndo()
        highWater(id, countManuscriptWords(body))
        saveState = SaveState.Salvo
        refreshMatches()
        snapshots = services.snapshots.list(b.path, id)
        scheduleCheck()
    }

    fun addChapter() {
        val b = book ?: return
        val number = b.manifest.chapters.count { it.kind == ChapterKind.Capitulo } + 1
        val meta = ChapterMeta(services.newId(), "Capítulo $number", modifiedAt = services.now())
        services.books.writeChapter(b.path, meta.id, "")
        updateManifest { it.copy(chapters = it.chapters + meta) }
        selectChapter(meta.id)
        titleFocusRequests++ // o título já vem selecionado para renomear; Enter leva ao texto
    }

    /**
     * Cria uma parte do livro que não é capítulo numerado (prólogo, dedicatória, epílogo…).
     * As de antes da história entram depois das outras do começo; interlúdios, depois do capítulo aberto;
     * as de depois da história, no fim.
     */
    fun addPart(kind: ChapterKind) {
        val b = book ?: return
        flushSave()
        val chapters = b.manifest.chapters
        val index = when (kind.group) {
            BookPartGroup.Antes -> chapters.indexOfFirst { it.kind.group != BookPartGroup.Antes }.let { if (it < 0) chapters.size else it }
            BookPartGroup.Entre -> chapters.indexOfFirst { it.id == currentChapterId }.let { if (it < 0) chapters.size else it + 1 }
            BookPartGroup.Depois, null -> chapters.size
        }
        val meta = ChapterMeta(services.newId(), kind.label, modifiedAt = services.now(), kind = kind)
        services.books.writeChapter(b.path, meta.id, "")
        updateManifest { it.copy(chapters = it.chapters.toMutableList().apply { add(index.coerceIn(0, size), meta) }) }
        selectChapter(meta.id)
        requestEditorFocus()
    }

    fun setChapterKind(id: String, kind: ChapterKind) = updateChapterMeta(id) { it.copy(kind = kind) }

    /** Número de cada capítulo numerado; partes como prólogo e epílogo não aparecem. */
    val chapterNumbers: Map<String, Int> get() = book?.manifest?.chapters?.let(::chapterNumbers) ?: emptyMap()

    fun renameChapter(id: String, title: String) {
        if (title.isBlank()) return
        updateChapterMeta(id) { it.copy(title = title.trim()) }
    }

    fun setChapterStatus(id: String, status: ChapterStatus) = updateChapterMeta(id) { it.copy(status = status) }

    fun moveChapter(id: String, delta: Int) {
        updateManifest { m ->
            val list = m.chapters.toMutableList()
            val from = list.indexOfFirst { it.id == id }
            val to = from + delta
            if (from < 0 || to !in list.indices) return@updateManifest m
            list.add(to, list.removeAt(from))
            m.copy(chapters = list)
        }
    }

    fun trashChapter(id: String) {
        val b = book ?: return
        if (id == currentChapterId) flushSave()
        val index = b.manifest.chapters.indexOfFirst { it.id == id }
        val meta = b.manifest.chapters.getOrNull(index) ?: return
        val body = services.books.readChapter(b.path, id)
        services.trash.trashChapter(b.path, b.manifest.title, meta, body, index)
        services.books.deleteChapterFile(b.path, id)
        updateManifest { it.copy(chapters = it.chapters.filterNot { c -> c.id == id }) }
        if (id == currentChapterId) {
            currentChapterId = null
            editor = TextFieldValue("")
            resetUndo()
            b.manifest.chapters.firstOrNull()?.let { selectChapter(it.id) }
        }
        trashItems = services.trash.list()
        message = "Capítulo “${meta.title}” foi para a lixeira."
    }

    // ---------- Editor e autosave ----------

    /**
     * Mudança vinda do teclado (digitar, colar, apagar). Aplica as regras de digitação (travessão, aspas,
     * continuação de lista…) e agrupa a digitação seguida num só passo do desfazer.
     */
    fun onEditorChange(value: TextFieldValue) {
        if (value.text == editor.text) {
            editor = value
            return
        }
        var incoming = value
        val base = fieldBase
        if (base != null && !isInsertionInto(editor.text, value.text)) {
            // Tecla que chegou antes de o campo receber a correção anterior: aplica sobre o texto corrigido.
            rebaseFieldEdit(base, value.text, editor.text, fieldBaseCursor)?.let { (text, delta) ->
                incoming = TextFieldValue(text, TextRange(value.selection.start + delta, value.selection.end + delta))
            }
        }
        val ruled = if (incoming.selection.collapsed) {
            applyTypingRules(editor.text, incoming.text, incoming.selection.end, settings.smartTypography)
        } else null
        recordUndo(editor, mergeable = true)
        applyEdit(incoming)
        if (ruled != null) {
            // A versão sem a correção vira um passo próprio: Ctrl+Z desfaz só a correção, como num editor de texto.
            recordUndo(editor, mergeable = false)
            applyEdit(TextFieldValue(ruled.text, TextRange(ruled.selStart, ruled.selEnd)))
        }
        scheduleCheck()
        // Até o próximo quadro o campo ainda acha que o texto é value.text.
        if (editor.text != value.text) {
            fieldBase = value.text
            fieldBaseCursor = value.selection.end
            fieldBaseJob?.cancel()
            fieldBaseJob = scope.launch {
                delay(FIELD_SYNC_MS)
                fieldBase = null
            }
        } else {
            fieldBase = null
        }
    }

    private fun isInsertionInto(old: String, new: String): Boolean {
        if (new.length <= old.length) return false
        var prefix = 0
        while (prefix < old.length && old[prefix] == new[prefix]) prefix++
        var suffix = 0
        while (suffix < old.length - prefix && old[old.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++
        return prefix + suffix >= old.length
    }

    /** Mudança feita pelo app (formatação, substituição, inserção): sempre um passo próprio no desfazer. */
    private fun commitEdit(value: TextFieldValue) {
        if (value.text != editor.text) {
            recordUndo(editor, mergeable = false)
            applyEdit(value)
        } else {
            editor = value
        }
        requestEditorFocus()
    }

    private fun applyEdit(value: TextFieldValue) {
        editor = value
        val t = services.now()
        // Tempo ativo = soma dos intervalos curtos entre edições (pausas longas não contam).
        if (lastEditAt != 0L) pendingActiveMs += (t - lastEditAt).coerceIn(0, ACTIVE_GAP_MS)
        lastEditAt = t
        saveState = SaveState.Pendente
        if (findVisible) refreshMatches()
        autosaveJob?.cancel()
        autosaveJob = scope.launch {
            delay(AUTOSAVE_DELAY_MS)
            saveCurrentChapter()
        }
    }

    fun requestEditorFocus() {
        editorFocusRequests++
    }

    /**
     * Botão de mostrar/ocultar painel do Manuscrito. Com o painel recolhido por falta de espaço, o clique avisa
     * em vez de esconder de vez (a preferência só muda quando o painel está de fato na tela).
     */
    fun togglePanel(chapters: Boolean, visible: Boolean) {
        val wanted = if (chapters) showChapters else showInspector
        if (wanted && !visible) {
            message = "A janela está estreita demais para mostrar ${if (chapters) "os capítulos" else "o inspetor"} ao lado do texto. Aumente a janela."
            return
        }
        if (chapters) showChapters = !showChapters else showInspector = !showInspector
    }

    /** Clique na folha fora do texto: cursor no fim do capítulo. */
    fun focusEditorAtEnd() {
        editor = editor.copy(selection = TextRange(editor.text.length))
        requestEditorFocus()
    }

    // ---------- Desfazer / refazer ----------

    private fun recordUndo(previous: TextFieldValue, mergeable: Boolean) {
        val t = services.now()
        val merge = mergeable && lastUndoMergeable && undoStack.isNotEmpty() && t - lastUndoAt < UNDO_MERGE_MS
        if (!merge) {
            undoStack.addLast(previous)
            if (undoStack.size > MAX_UNDO) undoStack.removeFirst()
        }
        lastUndoAt = t
        lastUndoMergeable = mergeable
        redoStack.clear()
        updateUndoFlags()
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(editor)
        lastUndoMergeable = false
        applyEdit(previous)
        updateUndoFlags()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(editor)
        lastUndoMergeable = false
        applyEdit(next)
        updateUndoFlags()
    }

    private fun resetUndo() {
        undoStack.clear()
        redoStack.clear()
        lastUndoMergeable = false
        updateUndoFlags()
    }

    private fun updateUndoFlags() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    fun flushSave() {
        autosaveJob?.cancel()
        autosaveJob = null
        if (saveState != SaveState.Salvo) saveCurrentChapter()
    }

    private fun saveCurrentChapter() {
        val b = book ?: return
        val id = currentChapterId ?: return
        saveState = SaveState.Salvando
        val body = editor.text
        services.books.writeChapter(b.path, id, body)
        val words = countManuscriptWords(body)
        // Sem marca de hoje, parte da contagem gravada antes desta edição (não da atual).
        val lastSaved = b.manifest.chapters.firstOrNull { it.id == id }?.words ?: words
        val written = (words - highWater(id, lastSaved)).coerceAtLeast(0)
        if (written > 0) wordsHighWater[id] = words
        services.productivity.record(today(), written, pendingActiveMs / 1000)
        pendingActiveMs %= 1000
        updateChapterMeta(id) { it.copy(words = words, modifiedAt = services.now()) }
        productivity = services.productivity.summary(today(), HISTORY_DAYS)
        saveState = SaveState.Salvo
    }

    val currentWords: Int get() = countManuscriptWords(editor.text)

    /** Total do livro com o capítulo aberto contado ao vivo. */
    val bookWords: Int
        get() = book?.manifest?.chapters?.sumOf { if (it.id == currentChapterId) currentWords else it.words } ?: 0

    // ---------- Formatação ----------

    private fun applyFormatting(op: (String, Int, Int) -> Edit) {
        if (currentChapterId == null) return
        val sel = editor.selection
        val result = op(editor.text, sel.min, sel.max)
        commitEdit(TextFieldValue(result.text, TextRange(result.selStart, result.selEnd)))
    }

    /** Negrito, itálico, versalete, sobrescrito ou subscrito na seleção (ou para o que for digitado a seguir). */
    fun toggleMark(kind: MarkKind) = applyFormatting { t, s, e -> toggleInline(t, s, e, kind) }

    /** Título, subtítulo, chamada, lista ou citação nos parágrafos selecionados (de novo volta ao normal). */
    fun toggleBlockStyle(kind: BlockKind) = applyFormatting { t, s, e -> toggleBlock(t, s, e, kind) }

    fun toggleCentered() = applyFormatting(::toggleCenter)

    fun clearFormat() = applyFormatting(::clearFormatting)

    fun wrapQuotes() = applyFormatting { t, s, e -> Edit(t.substring(0, s) + "“" + t.substring(s, e) + "”" + t.substring(e), s + 1, e + 1) }

    /**
     * Troca o trecho [start, end) do capítulo aberto por [text] como um passo próprio do desfazer, depois de guardar uma
     * versão em Versões ([label]). `false` se o trecho não existe mais.
     */
    fun replaceRange(start: Int, end: Int, text: String, label: String): Boolean {
        if (currentChapterId == null || start < 0 || end < start || end > editor.text.length) return false
        snapshotCurrent(label)
        val old = editor.text
        commitEdit(TextFieldValue(old.substring(0, start) + text + old.substring(end), TextRange(start + text.length)))
        return true
    }

    fun insertText(insert: String) = applyFormatting { t, s, e -> Edit(t.substring(0, s) + insert + t.substring(e), s + insert.length) }

    /** Quebra de cena num parágrafo próprio (aparece centralizada e sai assim na exportação). */
    fun insertSceneBreak() = applyFormatting { t, s, e -> insertStandaloneLine(t, s, e, SCENE_BREAK_TEXT) }

    /** Escolhe uma imagem, copia para `assets/` do livro e insere `![legenda](assets/…)` num parágrafo próprio. */
    fun insertImage() {
        val b = book ?: return
        if (currentChapterId == null) return
        scope.launch {
            val source = chooseOpenFile("Inserir imagem", IMAGE_EXTENSIONS) ?: return@launch
            val bytes = services.fs.readBytes(source)
            if (bytes == null) {
                message = "Não foi possível ler a imagem."
                return@launch
            }
            val relative = services.books.importAsset(b.path, bytes, source.substringAfterLast('.', "png"))
            val caption = fileName(source).substringBeforeLast('.').replace(']', ')')
            applyFormatting { t, s, e -> insertStandaloneLine(t, s, e, "![$caption]($relative)") }
        }
    }

    // ---------- Notas de rodapé ----------

    /** Notas do capítulo aberto na ordem em que aparecem no texto: (id, texto). */
    val currentFootnotes: List<Pair<String, String>>
        get() {
            val chapter = currentChapter ?: return emptyList()
            return footnoteIds(editor.text).map { it to chapter.footnotes[it].orEmpty() }
        }

    /** Insere a chamada da nota depois da seleção e guarda o texto da nota no capítulo. */
    fun addFootnote(noteText: String) {
        val chapter = currentChapter ?: return
        val id = nextFootnoteId(editor.text, chapter.footnotes.keys)
        updateChapterMeta(chapter.id) { it.copy(footnotes = it.footnotes + (id to noteText.trim())) }
        applyFormatting { t, s, e -> insertFootnoteRef(t, s, e, id) }
    }

    fun updateFootnote(id: String, noteText: String) {
        val chapterId = currentChapterId ?: return
        updateChapterMeta(chapterId) { it.copy(footnotes = it.footnotes + (id to noteText)) }
    }

    /** Tira a chamada do texto. O texto da nota fica guardado, para o Ctrl+Z trazer tudo de volta. */
    fun removeFootnote(id: String) {
        val ref = "${MarkKind.Footnote.open}$id${MarkKind.Footnote.close}"
        val at = editor.text.indexOf(ref)
        if (at >= 0) commitEdit(TextFieldValue(editor.text.removeRange(at, at + ref.length), TextRange(at)))
    }

    // ---------- Blocos de notas do livro ----------

    fun addNoteBlock() {
        val b = book ?: return
        saveNotes(b, b.notes.copy(blocks = b.notes.blocks + NoteBlock(services.newId())))
    }

    /** Cria um bloco de notas já preenchido (ex.: uma resposta do assistente guardada como anotação). */
    fun addNoteBlockWith(title: String, body: String) {
        val b = book ?: return
        saveNotes(b, b.notes.copy(blocks = b.notes.blocks + NoteBlock(services.newId(), title, body)))
    }

    fun updateNoteBlock(block: NoteBlock) {
        val b = book ?: return
        saveNotes(b, b.notes.copy(blocks = b.notes.blocks.map { if (it.id == block.id) block else it }))
    }

    fun removeNoteBlock(id: String) {
        val b = book ?: return
        saveNotes(b, b.notes.copy(blocks = b.notes.blocks.filterNot { it.id == id }))
    }

    private fun saveNotes(b: OpenBook, notes: BookNotes) {
        b.notes = notes
        services.books.saveNotes(b.path, notes)
    }

    /** Cria no Universo uma ficha com o nome selecionado no texto. */
    fun createEntryFromSelection(kind: EntryKind) {
        val b = book ?: return
        val name = editor.text.substring(editor.selection.min, editor.selection.max).trim()
        if (name.isEmpty() || '\n' in name) {
            message = "Selecione um nome no texto para criar a ficha."
            return
        }
        if (b.universe.entries.any { it.kind == kind && it.name.equals(name, ignoreCase = true) }) {
            message = "Já existe uma ficha de ${kind.label.lowercase()} chamada “$name”."
            return
        }
        saveEntry(newEntry(kind).copy(name = name))
        message = "Ficha de ${kind.label.lowercase()} “$name” criada no Universo."
        requestEditorFocus()
    }

    // ---------- Localizar / substituir ----------

    fun openFind() {
        goTo(Section.Manuscrito)
        findVisible = true
        val selected = editor.text.substring(editor.selection.min, editor.selection.max)
        if (selected.isNotEmpty() && '\n' !in selected) findQuery = selected
        refreshMatches()
        findFocusRequests++
    }

    fun closeFind() {
        findVisible = false
        matches = emptyList()
        currentMatch = -1
    }

    fun refreshMatches() {
        matches = if (findVisible) findAll(editor.text, findQuery, findOptions) else emptyList()
        currentMatch = when {
            matches.isEmpty() -> -1
            else -> matches.indexOfFirst { it.start >= editor.selection.min }.takeIf { it >= 0 } ?: 0
        }
    }

    fun goToMatch(delta: Int) {
        if (matches.isEmpty()) return
        currentMatch = (currentMatch + delta).mod(matches.size)
        val m = matches[currentMatch]
        editor = editor.copy(selection = TextRange(m.start, m.end))
    }

    fun replaceCurrent() {
        val m = matches.getOrNull(currentMatch) ?: return
        val text = replaceOne(editor.text, m, findQuery, replaceText, findOptions)
        commitEdit(TextFieldValue(text, TextRange(m.start)))
        refreshMatches()
    }

    fun replaceAllInChapter() {
        val result = replaceAll(editor.text, findQuery, replaceText, findOptions)
        if (result.count == 0) return
        snapshotCurrent("Antes de substituir “$findQuery”")
        commitEdit(TextFieldValue(result.text, TextRange(0)))
        refreshMatches()
        message = "${result.count} substituição(ões) no capítulo."
    }

    /** Apaga todas as ocorrências no capítulo (guarda uma versão antes). */
    fun deleteAllInChapter() {
        val result = replaceAll(editor.text, findQuery, "", findOptions)
        if (result.count == 0) return
        snapshotCurrent("Antes de apagar “$findQuery”")
        commitEdit(TextFieldValue(result.text, TextRange(0)))
        refreshMatches()
        message = "${result.count} ocorrência(s) apagada(s) no capítulo."
    }

    fun replaceAllInBook() {
        val b = book ?: return
        flushSave()
        var total = 0
        for (meta in b.manifest.chapters) {
            val body = services.books.readChapter(b.path, meta.id)
            val result = replaceAll(body, findQuery, replaceText, findOptions)
            if (result.count == 0) continue
            services.snapshots.create(b.path, meta.id, body, "Antes de substituir “$findQuery” no livro", countManuscriptWords(body))
            services.books.writeChapter(b.path, meta.id, result.text)
            val words = countManuscriptWords(result.text)
            raiseHighWater(meta.id, words)
            updateChapterMeta(meta.id) { it.copy(words = words, modifiedAt = services.now()) }
            total += result.count
        }
        currentChapterId?.let { id ->
            editor = TextFieldValue(services.books.readChapter(b.path, id))
            // O desfazer do editor só alcançaria este capítulo; a volta completa fica nas versões salvas acima.
            resetUndo()
            snapshots = services.snapshots.list(b.path, id)
        }
        refreshMatches()
        message = "$total substituição(ões) no livro."
    }

    // ---------- Busca no livro ----------

    fun runBookSearch() {
        val b = book ?: return
        flushSave()
        val chapters = b.manifest.chapters.map { Triple(it.id, it.title, services.books.readChapter(b.path, it.id)) }
        bookSearchHits = searchBook(chapters, bookSearchQuery, findOptions)
    }

    fun openHit(hit: SearchHit) {
        selectChapter(hit.chapterId)
        val end = minOf(hit.end, editor.text.length)
        editor = editor.copy(selection = TextRange(minOf(hit.start, end), end))
    }

    // ---------- Versões ----------

    fun snapshotCurrent(label: String = "Versão manual") {
        val b = book ?: return
        val id = currentChapterId ?: return
        services.snapshots.create(b.path, id, editor.text, label, currentWords)
        snapshots = services.snapshots.list(b.path, id)
    }

    fun restoreSnapshot(snapshot: Snapshot) {
        snapshotCurrent("Antes de restaurar versão")
        commitEdit(TextFieldValue(snapshot.body))
        currentChapterId?.let { raiseHighWater(it, countManuscriptWords(snapshot.body)) } // restaurar não é "escrever"
        flushSave()
        message = "Versão restaurada. O texto anterior foi guardado em Versões."
    }

    fun deleteSnapshot(snapshot: Snapshot) {
        val b = book ?: return
        services.snapshots.remove(b.path, snapshot)
        snapshots = services.snapshots.list(b.path, snapshot.chapterId)
    }

    // ---------- Universo (do livro ou do Acervo) ----------

    /** Fichas do universo em uso: o do Acervo aberto ou o do livro. */
    val universeEntries: List<UniverseEntry> get() = hostUniverse?.entries.orEmpty()

    /** Pasta do universo em uso (livro ou Acervo). */
    val universeHostPath: String? get() = acervo?.path ?: book?.path

    private val hostTitle: String get() = acervo?.meta?.name ?: book?.manifest?.title.orEmpty()

    private var hostUniverse: Universe?
        get() = acervo?.universe ?: book?.universe
        set(value) {
            if (value == null) return
            val a = acervo
            if (a != null) a.universe = value else book?.universe = value
        }

    /** Grava o universo em uso e, se ele é de uma saga, a cópia de cada livro dela. */
    private fun persistUniverse() {
        val path = universeHostPath ?: return
        val universe = hostUniverse ?: return
        services.books.saveUniverse(path, universe)
        mirrorSaga(path, universe)
    }

    /** Grava a ficha na hora (criação, imagens, botões). */
    fun saveEntry(entry: UniverseEntry) {
        if (hostUniverse == null) return
        putEntry(entry)
        entrySaveJob?.cancel()
        entrySaveJob = null
        persistUniverse()
    }

    /** Atualiza a ficha na tela e grava daqui a pouco: digitação não reescreve o `universe.json` a cada tecla. */
    fun updateEntry(entry: UniverseEntry) {
        if (hostUniverse == null) return
        putEntry(entry)
        entrySaveJob?.cancel()
        entrySaveJob = scope.launch {
            delay(ENTRY_SAVE_DELAY_MS)
            entrySaveJob = null
            persistUniverse()
        }
    }

    /** Grava agora a ficha que estava esperando (antes de sair do Universo, fechar o livro ou ler do disco). */
    fun flushEntrySave() {
        val job = entrySaveJob ?: return
        job.cancel()
        entrySaveJob = null
        persistUniverse()
    }

    private fun putEntry(entry: UniverseEntry) {
        val universe = hostUniverse ?: return
        if (acervo == null) scheduleCheck()
        val stamped = entry.copy(modifiedAt = services.now())
        val entries = universe.entries
        val updated = if (entries.any { it.id == entry.id }) entries.map { if (it.id == entry.id) stamped else it } else entries + stamped
        hostUniverse = universe.copy(entries = updated, schemaVersion = UNIVERSE_SCHEMA_VERSION)
    }

    fun newEntry(kind: EntryKind): UniverseEntry = UniverseEntry(services.newId(), kind, "")

    /** Cria e grava uma ficha vazia, já pronta para edição. */
    fun createEntry(kind: EntryKind): UniverseEntry = newEntry(kind).also { saveEntry(it) }

    fun newId(): String = services.newId()

    /** Capítulos cujo texto cita a ficha (nome ou apelidos, palavra inteira, sem acentos). */
    fun chaptersMentioning(entry: UniverseEntry): List<ChapterMeta> {
        val b = book ?: return emptyList()
        val ids = entryMentions(entry).chapters.map { it.chapterId }.toSet()
        return b.manifest.chapters.filter { it.id in ids }
    }

    /** Painel "No manuscrito": em que capítulos a ficha é citada e quantas vezes. */
    fun entryMentions(entry: UniverseEntry): EntryMentions {
        val b = book ?: return EntryMentions(emptyList(), emptyList(), 0)
        val chapters = b.manifest.chapters.map { meta ->
            val version = if (meta.id == currentChapterId) editor.text.hashCode().toLong() shl 32 or 1L else meta.modifiedAt
            Triple(meta.id, meta.title, version)
        }
        return mentionIndex.mentions(entry, chapters) { id ->
            if (id == currentChapterId) editor.text else services.books.readChapter(b.path, id)
        }
    }

    /** Fichas citadas no capítulo aberto, com quantas vezes cada uma aparece (mais citadas primeiro). */
    fun entriesInCurrentChapter(): List<Pair<UniverseEntry, Int>> {
        val b = book ?: return emptyList()
        val text = editor.text
        return b.universe.entries.mapNotNull { e ->
            val n = findMentions(text, e.searchTerms()).size
            if (n > 0) e to n else null
        }.sortedByDescending { it.second }
    }

    /** Abre o capítulo no Manuscrito com a citação selecionada. */
    fun openMention(chapterId: String, match: TextMatch) {
        flushEntrySave()
        selectChapter(chapterId)
        goTo(Section.Manuscrito)
        val end = minOf(match.end, editor.text.length)
        editor = editor.copy(selection = TextRange(minOf(match.start, end), end))
        requestEditorFocus()
    }

    fun trashEntry(entry: UniverseEntry) {
        val path = universeHostPath ?: return
        val universe = hostUniverse ?: return
        flushEntrySave()
        services.trash.trashEntry(path, hostTitle, entry)
        hostUniverse = universe.copy(entries = universe.entries.filterNot { it.id == entry.id })
        persistUniverse()
        trashItems = services.trash.list()
        message = "Ficha “${entry.name.ifBlank { "sem nome" }}” foi para a lixeira."
    }

    /** Fichas do universo em uso que estão na lixeira. */
    val entryTrash: List<TrashItem>
        get() = universeHostPath?.let { path -> trashItems.filter { it.kind == TrashKind.Ficha && it.bookPath == path } }.orEmpty()

    /** Imagem do universo em uso (retratos e galerias). */
    fun readUniverseAsset(path: String): ByteArray? = universeHostPath?.let { services.books.readAsset(it, path) }

    /**
     * Escolhe uma imagem, copia para `assets/` do livro (ou do universo do Acervo aberto) e devolve o caminho
     * relativo e o tamanho em pixels (`null` se a pessoa desistir ou a imagem não puder ser lida).
     */
    suspend fun pickAndImportImage(title: String, extensions: List<String> = IMAGE_EXTENSIONS, intoUniverse: Boolean = false): Pair<String, Pair<Int, Int>>? {
        val target = (if (intoUniverse) universeHostPath else book?.path) ?: return null
        val source = chooseOpenFile(title, extensions) ?: return null
        val bytes = services.fs.readBytes(source)
        val size = bytes?.let { imagePixelSize(it) }
        if (bytes == null || size == null) {
            message = "Não foi possível ler a imagem."
            return null
        }
        return services.books.importAsset(target, bytes, source.substringAfterLast('.', "png")) to size
    }

    /** Escolhe uma imagem para a ficha: vira o retrato se ela ainda não tiver um, senão entra na galeria. */
    fun addEntryImage(entryId: String, asPortrait: Boolean) {
        scope.launch {
            val (path, _) = pickAndImportImage(if (asPortrait) "Retrato" else "Imagem da ficha", intoUniverse = true) ?: return@launch
            addEntryImagePath(entryId, path, asPortrait)
        }
    }

    /** Põe na ficha uma imagem que já está em `assets/` do universo em uso. */
    fun addEntryImagePath(entryId: String, path: String, asPortrait: Boolean) {
        val entry = universeEntries.firstOrNull { it.id == entryId } ?: return
        val image = EntryImage(path)
        val updated = when {
            asPortrait && entry.portrait != null -> entry.copy(portrait = image, gallery = listOf(entry.portrait) + entry.gallery)
            asPortrait || entry.portrait == null -> entry.copy(portrait = image)
            else -> entry.copy(gallery = entry.gallery + image)
        }
        saveEntry(updated)
    }

    /** Troca o retrato por uma imagem da galeria (o retrato antigo volta para a galeria). */
    fun makePortrait(entryId: String, path: String) {
        val entry = universeEntries.firstOrNull { it.id == entryId } ?: return
        val chosen = entry.gallery.firstOrNull { it.path == path } ?: return
        val rest = entry.gallery.filterNot { it.path == path }
        saveEntry(entry.copy(portrait = chosen, gallery = listOfNotNull(entry.portrait) + rest))
    }

    /** Tira a imagem da ficha (o arquivo fica no livro: pode estar em uso em outro lugar). */
    fun removeEntryImage(entryId: String, path: String) {
        val entry = universeEntries.firstOrNull { it.id == entryId } ?: return
        saveEntry(
            if (entry.portrait?.path == path) entry.copy(portrait = null)
            else entry.copy(gallery = entry.gallery.filterNot { it.path == path }),
        )
    }

    fun setPortraitFrame(entryId: String, frame: ImageFrame) {
        val entry = universeEntries.firstOrNull { it.id == entryId } ?: return
        val portrait = entry.portrait ?: return
        updateEntry(entry.copy(portrait = portrait.copy(frame = frame)))
    }

    // ---------- Acervo ----------

    /** Universos do Acervo (lidos quando a lista é mostrada). */
    var acervoList by mutableStateOf<List<AcervoSummary>>(emptyList()); private set

    fun refreshAcervo() {
        acervoList = services.acervo.list()
    }

    /** Mostra a lista de universos do Acervo. */
    fun showAcervo() {
        closeAcervoUniverse()
        refreshAcervo()
        universeTool = UniverseTool.Acervo
        goTo(Section.Universo)
    }

    fun createAcervoUniverse(name: String, description: String): AcervoSummary {
        val created = services.acervo.create(name, description)
        refreshAcervo()
        return created
    }

    /** Copia todas as fichas do livro aberto (com imagens) para um universo novo do Acervo. */
    fun importBookToAcervo(): AcervoSummary? {
        val b = book ?: return null
        flushEntrySave()
        val created = services.acervo.create("${b.manifest.title} — universo", sourceTitle = b.manifest.title)
        val copies = copyEntries(b.universe.entries, { services.books.readAsset(b.path, it) }, { bytes, ext -> services.books.importAsset(created.path, bytes, ext) }, services.newId)
        services.books.saveUniverse(created.path, Universe(copies, UNIVERSE_SCHEMA_VERSION))
        refreshAcervo()
        message = "Universo do livro copiado para o Acervo (${copies.size} ficha(s))."
        return created
    }

    /** Abre um universo do Acervo: as fichas mostradas e editadas passam a ser as dele. */
    fun openAcervoUniverse(path: String) {
        flushEntrySave()
        val meta = services.acervo.loadMeta(path) ?: return
        acervo = OpenAcervo(path, meta, services.books.loadUniverse(path))
        universeTool = UniverseTool.Fichas
        goTo(Section.Universo)
    }

    /** Volta para o universo do livro. */
    fun closeAcervoUniverse() {
        val a = acervo ?: return
        flushEntrySave()
        acervo = null
        // Editar a saga muda a cópia do livro aberto: relê.
        if (a.meta.saga) book?.let { it.universe = services.books.loadUniverse(it.path) }
    }

    /** Apaga um universo do Acervo (vai para a lixeira inteiro). */
    fun trashAcervoUniverse(summary: AcervoSummary) {
        if (acervo?.path == summary.path) closeAcervoUniverse()
        services.trash.trashFolder(summary.path, summary.meta.name, TrashKind.Universo)
        trashItems = services.trash.list()
        refreshAcervo()
        message = "Universo “${summary.meta.name}” foi para a lixeira."
    }

    /** De onde dá para trazer fichas: os outros livros da biblioteca e os universos do Acervo. */
    fun entrySources(): List<Pair<String, String>> {
        val here = universeHostPath
        val books = library.filter { it.path != here }.map { it.path to "Livro: ${it.manifest.title}" }
        val universes = services.acervo.list().filter { it.path != here }.map { it.path to "Acervo: ${it.meta.name}" }
        return books + universes
    }

    fun entriesAt(path: String): List<UniverseEntry> = services.books.loadUniverse(path).entries

    /** Copia as fichas [ids] de [fromPath] (livro ou Acervo) para o universo em uso, com as imagens. */
    fun bringEntries(fromPath: String, ids: Set<String>) {
        val to = universeHostPath ?: return
        val universe = hostUniverse ?: return
        flushEntrySave()
        val chosen = services.books.loadUniverse(fromPath).entries.filter { it.id in ids }
        val copies = copyEntries(chosen, { services.books.readAsset(fromPath, it) }, { bytes, ext -> services.books.importAsset(to, bytes, ext) }, services.newId)
        hostUniverse = universe.copy(entries = universe.entries + copies, schemaVersion = UNIVERSE_SCHEMA_VERSION)
        persistUniverse()
        message = "${copies.size} ficha(s) trazida(s)."
    }

    // ---------- Saga (livros unificados) ----------

    /** Livros da biblioteca que podem entrar na saga do livro aberto. */
    val sagaCandidates: List<BookSummary> get() = library.filter { it.path != book?.path }

    /**
     * Livros da mesma saga passam a usar as mesmas fichas. Antes de juntar, guarda uma cópia do `universe.json` de
     * cada livro em `<dados>/backups/saga-<data>/`. Fichas repetidas (mesmo tipo e nome) viram uma só; cenas e
     * cartões que apontavam para as fichas juntadas passam a apontar para a ficha que ficou.
     */
    fun unifyBooks(otherPaths: List<String>, sagaName: String) {
        val b = book ?: return
        flushEntrySave()
        flushSave()
        map.flush()
        val existingSaga = b.manifest.sagaId?.let { services.acervo.findSaga(it) }
        val oldMembers = existingSaga?.let { services.acervo.loadMeta(it)?.bookPaths }.orEmpty()
        val paths = (listOf(b.path) + oldMembers + otherPaths).distinct().filter { services.fs.exists(joinPath(it, BookRepository.MANIFEST)) }
        val members = paths.map { p ->
            val title = if (p == b.path) b.manifest.title else services.books.loadManifest(p).title
            BookUniverse(p, title, if (p == b.path) b.universe else services.books.loadUniverse(p))
        }
        // Cópia de segurança antes de mexer em qualquer livro.
        val backup = joinPath(services.dataDir, "backups", "saga-${services.now()}")
        members.forEach { m -> services.fs.writeText(joinPath(backup, safeFileName(m.title, "livro"), BookRepository.UNIVERSE), EscribaJson.encodeToString(Universe.serializer(), m.universe)) }

        val result = mergeSaga(members, services.newId)
        val name = sagaName.trim().ifBlank { "Saga de ${b.manifest.title}" }
        val sagaPath = existingSaga ?: services.acervo.create(name, saga = true, bookPaths = paths).path
        val meta = services.acervo.loadMeta(sagaPath)!!.copy(name = name, saga = true, bookPaths = paths)
        services.acervo.saveMeta(sagaPath, meta)
        copySagaAssets(result.universe, paths, sagaPath)
        services.books.saveUniverse(sagaPath, result.universe)
        for (p in paths) {
            services.books.saveUniverse(p, result.universe)
            val outline = services.books.loadOutline(p)
            services.books.saveOutline(p, outline.copy(beats = outline.beats.map { beat -> beat.copy(characterIds = beat.characterIds.map { result.idMap[it] ?: it }.distinct()) }))
            val boards = services.books.loadBoards(p)
            services.books.saveBoards(p, boards.copy(boards = boards.boards.map { bd -> bd.copy(nodes = bd.nodes.map { n -> n.entryId?.let { id -> n.copy(entryId = result.idMap[id] ?: id) } ?: n }) }))
            val manifest = services.books.loadManifest(p)
            services.books.saveManifest(p, manifest.copy(sagaId = meta.id, sagaName = name))
        }
        reloadOpenBook()
        b.boards = services.books.loadBoards(b.path)
        refreshLibrary()
        refreshAcervo()
        message = "Saga “$name”: ${paths.size} livro(s) com as mesmas fichas" + if (result.joined > 0) " (${result.joined} ficha(s) repetida(s) juntada(s))." else "."
    }

    /** Tira o livro aberto da saga: ele fica com uma cópia das fichas, que deixam de mudar junto. */
    fun leaveSaga() {
        val b = book ?: return
        val sagaId = b.manifest.sagaId ?: return
        flushEntrySave()
        services.acervo.findSaga(sagaId)?.let { path ->
            services.acervo.loadMeta(path)?.let { m -> services.acervo.saveMeta(path, m.copy(bookPaths = m.bookPaths - b.path)) }
        }
        updateManifest { it.copy(sagaId = null, sagaName = "") }
        message = "O livro saiu da saga. As fichas continuam nele."
    }

    /** Universo de saga em [path] (livro da saga ou o próprio universo da saga): grava nos outros livros e na saga. */
    private fun mirrorSaga(path: String, universe: Universe) {
        val sagaPath: String
        val members: List<String>
        val a = acervo
        if (a != null && a.path == path) {
            if (!a.meta.saga) return
            sagaPath = a.path
            members = a.meta.bookPaths
        } else {
            val sagaId = book?.takeIf { it.path == path }?.manifest?.sagaId ?: return
            sagaPath = services.acervo.findSaga(sagaId) ?: return
            members = services.acervo.loadMeta(sagaPath)?.bookPaths.orEmpty()
        }
        val targets = (members + sagaPath).filter { it != path && services.fs.exists(it) }
        copySagaAssets(universe, listOf(path) + targets, sagaPath)
        targets.forEach { services.books.saveUniverse(it, universe) }
    }

    /** Toda imagem usada pelas fichas passa a existir em todos os livros da saga e no universo da saga. */
    private fun copySagaAssets(universe: Universe, paths: List<String>, sagaPath: String) {
        val all = (paths + sagaPath).distinct()
        for (image in universe.imagePaths()) {
            val holder = all.firstOrNull { services.fs.exists(joinPath(it, image)) } ?: continue
            val bytes = services.fs.readBytes(joinPath(holder, image)) ?: continue
            all.filter { !services.fs.exists(joinPath(it, image)) }.forEach { services.fs.writeBytes(joinPath(it, image), bytes) }
        }
    }

    // ---------- Selo e contradições ----------

    /** Contradições do capítulo aberto, para sublinhar no editor. */
    val currentIssues: List<TextMatch>
        get() {
            if (!settings.checkContradictions) return emptyList()
            val id = currentChapterId ?: return emptyList()
            return bookCheck?.contradictions.orEmpty().filter { it.spot.chapterId == id }.map { TextMatch(it.spot.start, it.spot.end) }
        }

    fun setCheckContradictions(on: Boolean) {
        updateSettings { it.copy(checkContradictions = on) }
        if (on) runCheck() else bookCheck = null
    }

    /** Confere o livro agora (relatório), com ou sem a conferência ligada. */
    fun runCheck(): BookCheck? {
        val b = book ?: return null
        val chapters = b.manifest.chapters.map { meta ->
            val text = if (meta.id == currentChapterId) editor.text
            else checkTexts[meta.id]?.takeIf { it.first == meta.modifiedAt }?.second
                ?: services.books.readChapter(b.path, meta.id).also { checkTexts[meta.id] = meta.modifiedAt to it }
            ChapterText(meta.id, meta.title, text)
        }
        return checkBook(b.universe.entries, chapters).also { bookCheck = it }
    }

    /** Reconfere pouco depois da última mudança (texto ou fichas), se a conferência estiver ligada. */
    private fun scheduleCheck() {
        if (!settings.checkContradictions || book == null) return
        checkJob?.cancel()
        checkJob = scope.launch {
            delay(CHECK_DELAY_MS)
            runCheck()
        }
    }

    /** Liga ou desliga o selo de um atributo da ficha. */
    fun toggleSeal(entryId: String, key: String) {
        val entry = universeEntries.firstOrNull { it.id == entryId } ?: return
        val existing = entry.attributes.firstOrNull { it.key == key }
        val updated = if (existing != null) entry.copy(attributes = entry.attributes.map { if (it.key == key) it.copy(sealed = !it.sealed) else it })
        else entry.copy(attributes = entry.attributes + EntryAttribute(services.newId(), key, entry.schema.attribute(key)?.label.orEmpty(), "", sealed = true))
        saveEntry(updated)
    }

    /**
     * Copia o que a descrição física diz para os atributos com selo e sela. [replace]: categorias em conflito em
     * que o autor escolheu ficar com a descrição; as outras em conflito ficam como estão.
     */
    fun sealFromDescription(entryId: String, replace: Set<FactKind> = emptySet()) {
        val entry = universeEntries.firstOrNull { it.id == entryId } ?: return
        var updated = entry
        for (s in sealSuggestions(entry)) {
            if (s.status == SealStatus.Conflito && s.kind !in replace) continue
            val value = if (s.status == SealStatus.Igual) s.current else sealText(s.values)
            updated = updated.withAttr(s.kind.attrKey, value, services.newId)
            updated = updated.copy(attributes = updated.attributes.map { if (it.key == s.kind.attrKey) it.copy(sealed = true) else it })
        }
        saveEntry(updated)
    }

    /** Abre o trecho no Manuscrito, selecionado. */
    fun openSpot(spot: TextSpot) = openMention(spot.chapterId, TextMatch(spot.start, spot.end))

    // ---------- Idiomas ----------

    /** Língua do livro já gerada (a geração é guardada enquanto semente e estilo não mudam). */
    val bookLanguage: Language?
        get() {
            val wl = book?.language ?: return null
            languageCache?.takeIf { it.first.seed == wl.seed && it.first.style == wl.style }?.let { return it.second }
            return wl.build().also { languageCache = wl to it }
        }

    /** Cria a língua do livro: a semente vem do título, então o mesmo livro dá sempre a mesma língua. */
    fun createLanguage(style: LangStyle) {
        val b = book ?: return
        saveLanguage(WorldLanguage(fnv1a64(b.manifest.title), style))
    }

    /** Muda o estilo e mantém a semente (a língua muda de clima, não de sorteio). */
    fun setLanguageStyle(style: LangStyle) {
        book?.language?.let { saveLanguage(it.copy(style = style)) }
    }

    /** Sorteia outra língua; a atual fica guardada para "Voltar à anterior". */
    fun rerollLanguage() {
        val wl = book?.language ?: return
        saveLanguage(wl.copy(seed = kotlin.random.Random.nextLong(), previousSeeds = (wl.previousSeeds + wl.seed).takeLast(20), coined = emptyMap()))
    }

    fun previousLanguage() {
        val wl = book?.language ?: return
        val previous = wl.previousSeeds.lastOrNull() ?: return
        saveLanguage(wl.copy(seed = previous, previousSeeds = wl.previousSeeds.dropLast(1), coined = emptyMap()))
    }

    /** Guarda no dicionário do livro as palavras cunhadas de uma tradução que o autor usou (copiou). */
    fun keepCoined(translation: Translation) {
        val wl = book?.language ?: return
        val lang = bookLanguage ?: return
        val add = translation.newLemmas.filter { it !in wl.coined }.associateWith { lang.protoWord(it) }
        if (add.isNotEmpty()) saveLanguage(wl.copy(coined = wl.coined + add))
    }

    private fun saveLanguage(language: WorldLanguage) {
        val b = book ?: return
        b.language = language
        services.books.saveLanguage(b.path, language)
        refreshScriptFont()
    }

    // ---------- Escrita (fonte gerada) ----------

    /** Fonte da escrita do livro: id (muda com a fonte), bytes e o arquivo em `fontes/` do livro. */
    class ScriptFontInfo(val id: String, val bytes: ByteArray, val path: String)

    /** Alfabeto da língua do livro. */
    val bookScript: Script? get() = scriptState()?.first

    private fun scriptState(): Pair<Script, ScriptFontInfo>? {
        val b = book ?: return null
        val wl = b.language ?: return null
        scriptCache?.takeIf { it.first == wl.seed && it.second == wl.style }?.let { return it.third }
        val lang = bookLanguage ?: return null
        val script = Script(lang)
        val bytes = script.fontBytes()
        val id = "escrita-" + (fnv1a64(bytes.joinToString(",")) ushr 32).toString(16).padStart(8, '0')
        val path = joinPath(b.path, "fontes", "$id.ttf")
        if (!services.fs.exists(path)) services.fs.writeBytes(path, bytes)
        val info = ScriptFontInfo(id, bytes, path)
        scriptCache = Triple(wl.seed, wl.style, script to info)
        return script to info
    }

    /** Gera (se preciso) e registra a fonte da escrita da língua do livro aberto. */
    private fun refreshScriptFont() {
        val info = scriptState()?.second
        scriptFont = info
        registerScriptFont(info?.path)
        if (info != null && layout != null) requestLayout(0)
    }

    /** Texto na escrita da língua (a partir de letras nossas). */
    fun toScript(roman: String): String = bookScript?.transliterate(roman) ?: roman

    /** Volta a escrita para letras nossas (exportações sem a fonte). */
    private fun romanizeScript(text: String): String = bookScript?.romanize(text) ?: text

    // ---------- Calendário do mundo ----------

    /** Calendário em uso: o do livro ou, sem ele, o comum (datas como "3 de março, 12" continuam valendo). */
    val calendar: WorldCalendar get() = book?.calendar ?: CalendarTemplates.implicit

    /** Mostra a aba Calendário do Universo. */
    fun showCalendar() {
        universeTool = UniverseTool.Calendario
        goTo(Section.Universo)
    }

    /** Cria o calendário do livro a partir de um modelo. */
    fun createCalendar(template: CalendarTemplate) = saveCalendar(CalendarTemplates.build(template))

    /** Grava o calendário e relê as datas das cenas que ainda não tinham sido entendidas. */
    fun saveCalendar(calendar: WorldCalendar) {
        val b = book ?: return
        b.calendar = calendar
        services.books.saveCalendar(b.path, calendar)
        updateOutline { o ->
            o.copy(beats = o.beats.map { beat ->
                if (beat.date != null || beat.whenText.isBlank()) beat
                else (calendar.parseDate(beat.whenText) as? ParsedDate.Ok)?.let { beat.copy(date = it.date) } ?: beat
            })
        }
    }

    fun setPresent(date: WorldDate?) {
        book?.calendar?.let { saveCalendar(it.copy(present = date)) }
    }

    /** Dá luas e sol a um calendário que não tem (ver [CalendarTemplates.suggestedSky]). */
    fun addSuggestedSky() {
        book?.calendar?.let { saveCalendar(CalendarTemplates.suggestedSky(it)) }
    }

    /** "Quando" digitado numa cena: guarda o texto e a data lida (se deu para ler). */
    fun withWhen(beat: Beat, text: String): Beat {
        val parsed = calendar.parseDate(text)
        return beat.copy(whenText = text.trim(), date = (parsed as? ParsedDate.Ok)?.date)
    }

    // ---------- Enredo ----------

    /** Abre a ficha no Universo (pelo chip de um cartão do mapa). */
    fun openUniverseEntry(id: String) {
        // Dentro de um universo do Acervo, pular para outra ficha dele não volta para o livro.
        val inAcervo = acervo?.universe?.entries?.any { it.id == id } == true
        if (!inAcervo && book?.universe?.entries?.none { it.id == id } != false) {
            message = "A ficha vinculada não existe mais."
            return
        }
        if (!inAcervo) closeAcervoUniverse()
        universeFocus = id
        universeTool = UniverseTool.Fichas
        goTo(Section.Universo)
    }

    /** O Universo já mostrou a ficha pedida. */
    fun consumeUniverseFocus() {
        universeFocus = null
    }

    fun newBeat(): Beat = Beat(services.newId(), "")

    fun saveBeat(beat: Beat) = updateOutline { it.upsert(beat.copy(title = beat.title.trim(), modifiedAt = services.now())) }

    fun moveBeat(id: String, delta: Int) = updateOutline { it.move(id, delta) }

    fun trashBeat(id: String) {
        val b = book ?: return
        val index = b.outline.beats.indexOfFirst { it.id == id }
        if (index < 0) return
        val beat = b.outline.beats[index]
        services.trash.trashBeat(b.path, b.manifest.title, beat, index)
        updateOutline { it.remove(id) }
        trashItems = services.trash.list()
        message = "Cena “${beat.title.ifBlank { "sem título" }}” foi para a lixeira."
    }

    fun applyTemplate(template: StructureTemplate) {
        updateOutline { it.applyTemplate(template, services.newId, services.now()) }
        message = "Modelo “${template.name}” aplicado: ${template.steps.size} cenas adicionadas ao fim do roteiro."
    }

    /** Abre no editor o capítulo vinculado à cena. */
    fun openBeatChapter(beat: Beat) {
        val chapterId = beat.chapterId ?: return
        if (book?.manifest?.chapters?.none { it.id == chapterId } != false) return
        selectChapter(chapterId)
        section = Section.Manuscrito
    }

    /** Cria um capítulo com o título da cena e vincula os dois. */
    fun createChapterForBeat(beat: Beat) {
        val b = book ?: return
        val title = beat.title.ifBlank { "Capítulo ${b.manifest.chapters.size + 1}" }
        val meta = ChapterMeta(services.newId(), title, modifiedAt = services.now())
        services.books.writeChapter(b.path, meta.id, "")
        updateManifest { it.copy(chapters = it.chapters + meta) }
        saveBeat(beat.copy(chapterId = meta.id))
    }

    private fun updateOutline(transform: (Outline) -> Outline) {
        val b = book ?: return
        val updated = transform(b.outline)
        if (updated == b.outline) return
        b.outline = updated
        services.books.saveOutline(b.path, updated)
    }

    // ---------- Lixeira ----------

    fun restoreFromTrash(item: TrashItem) {
        flushEntrySave()
        when (val outcome = services.trash.restore(item)) {
            is RestoreOutcome.Restored -> {
                if (book?.path == outcome.bookPath) reloadOpenBook()
                message = "“${item.title}” restaurado."
            }
            is RestoreOutcome.OriginMissing ->
                message = "O livro de origem (${outcome.bookTitle ?: "desconhecido"}) não existe mais. Restaure o livro primeiro."
        }
        refreshLibrary()
    }

    fun deleteForever(item: TrashItem) {
        services.trash.deleteForever(item)
        trashItems = services.trash.list()
    }

    /** Restaura todos os [items] (os que estão visíveis no filtro da lixeira). */
    fun restoreAllFromTrash(items: List<TrashItem>) {
        flushSave()
        flushEntrySave()
        val restored = services.trash.restoreAll(items)
        reloadOpenBook()
        refreshLibrary()
        val missing = items.size - restored
        message = if (missing == 0) "$restored item(ns) restaurado(s)."
        else "$restored item(ns) restaurado(s). $missing ficaram na lixeira porque o livro de origem não existe mais."
    }

    /** Apaga de vez os [items] (os que estão visíveis no filtro da lixeira). */
    fun emptyTrash(items: List<TrashItem>) {
        items.forEach { services.trash.deleteForever(it) }
        trashItems = services.trash.list()
        message = "${items.size} item(ns) apagado(s) definitivamente."
    }

    /** Relê do disco o livro aberto (depois de restaurar coisas nele pela lixeira). */
    private fun reloadOpenBook() {
        val open = book ?: return
        open.manifest = services.books.loadManifest(open.path)
        open.universe = services.books.loadUniverse(open.path)
        open.outline = services.books.loadOutline(open.path)
    }

    fun trashDaysLeft(item: TrashItem) = services.trash.daysLeft(item)

    // ---------- Exportação ----------

    fun export(format: ExportFormat) {
        val b = book ?: return
        flushSave()
        scope.launch {
            val target = chooseSaveFile(b.manifest.title, format.extension) ?: return@launch
            runCatching {
                // DOCX, HTML, Markdown e texto não levam a fonte: a escrita inventada sai em letras nossas.
                val chapters = services.books.readAllChapters(b.path, b.manifest).map { (meta, body) -> meta.copy(title = romanizeScript(meta.title)) to romanizeScript(body) }
                val text = buildExportDocument(b.manifest, chapters)
                val doc = text.copy(images = imagePaths(text).mapNotNull { path -> services.books.readAsset(b.path, path)?.let { path to it } }.toMap())
                when (format) {
                    ExportFormat.Markdown -> services.fs.writeText(target, exportMarkdown(doc))
                    ExportFormat.Texto -> services.fs.writeText(target, exportPlainText(doc))
                    ExportFormat.Html -> services.fs.writeText(target, exportHtml(doc))
                    ExportFormat.Docx -> services.fs.writeBytes(target, buildDocx(doc))
                }
            }.onSuccess { message = "Exportado para $target" }
                .onFailure { message = "Falha ao exportar: ${it.message}" }
        }
    }

    // ---------- Diagramação ----------

    fun updateLayout(transform: (LayoutSettings) -> LayoutSettings) {
        val b = book ?: return
        val updated = transform(layoutSettings)
        if (updated == layoutSettings) return
        layoutSettings = updated
        saveLayoutSettings(services.fs, b.path, updated)
        requestLayout()
    }

    fun applyLayoutTheme(theme: LayoutTheme) = updateLayout { theme.apply(it) }

    /** Imagem do livro usada pela diagramação (prévia e PDF). */
    fun layoutImage(path: String): ByteArray? = book?.let { services.books.readAsset(it.path, path) }

    /** Refaz a diagramação em segundo plano; várias mudanças seguidas viram um único cálculo. */
    fun requestLayout(delayMs: Long = LAYOUT_DELAY_MS) {
        val b = book ?: return
        layoutJob?.cancel()
        layoutJob = scope.launch {
            if (delayMs > 0) delay(delayMs)
            flushSave()
            layoutBusy = true
            val settings = layoutSettings
            val manifest = b.manifest
            try {
                val chapters = services.books.readAllChapters(b.path, manifest)
                val result = withContext(Dispatchers.Default) {
                    layoutBook(manifest, chapters, settings, layoutMeasurer(), ImageSizer { p -> services.books.readAsset(b.path, p)?.let(::imagePixelSize) })
                }
                layout = result
                layoutError = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                layoutError = "Não foi possível diagramar: ${e.message}"
            } finally {
                layoutBusy = false
            }
        }
    }

    /** Pontos críticos da conferência que seguraram o PDF (a tela pergunta se gera assim mesmo). */
    var layoutGate by mutableStateOf<List<CheckItem>?>(null); private set

    fun applyLayoutFix(fix: CheckFix) {
        val pages = layout?.pages?.size ?: return
        updateLayout { applyFix(it, fix, pages) }
        message = "Conserto aplicado: ${fix.label.lowercase()}. A diagramação foi refeita — confira o sumário e a lombada."
    }

    fun dismissLayoutGate() { layoutGate = null }

    fun copyPrintSpec() {
        val laid = layout ?: return
        copyToClipboard(printSpec(laid))
        message = "Ficha técnica copiada: cole no e-mail ou na mensagem para a gráfica."
    }

    /** Gera o PDF do miolo. Sem [force], para antes se a conferência achar algo que sairia errado no impresso. */
    fun exportLayoutPdf(force: Boolean = false) {
        val b = book ?: return
        val laid = layout ?: return
        layoutGate = null
        if (!force) {
            val critical = checkLayout(laid).filter { it.level == CheckLevel.Critico }
            if (critical.isNotEmpty()) {
                layoutGate = critical
                return
            }
        }
        scope.launch {
            val target = chooseSaveFile("${b.manifest.title} - miolo", "pdf") ?: return@launch
            runCatching { withContext(Dispatchers.Default) { writeLayoutPdf(laid, target, ::layoutImage) } }
                .onSuccess { message = "PDF do miolo gerado: $target (${laid.pages.size} páginas)" }
                .onFailure { message = "Falha ao gerar o PDF: ${it.message}" }
        }
    }

    // ---------- Capa ----------

    /** Muda só a capa: grava `layout.json` sem refazer a diagramação do miolo. */
    fun updateCover(transform: (CoverSettings) -> CoverSettings) {
        val b = book ?: return
        val updated = layoutSettings.copy(cover = transform(layoutSettings.cover))
        if (updated == layoutSettings) return
        layoutSettings = updated
        saveLayoutSettings(services.fs, b.path, updated)
    }

    /** Escolhe a arte de fundo e copia para `assets/` do livro. */
    fun importCoverImage() {
        val b = book ?: return
        scope.launch {
            val source = chooseOpenFile("Arte da capa", COVER_IMAGE_EXTENSIONS) ?: return@launch
            val bytes = services.fs.readBytes(source)
            if (bytes == null || imagePixelSize(bytes) == null) {
                message = "Não foi possível ler a imagem."
                return@launch
            }
            val relative = services.books.importAsset(b.path, bytes, source.substringAfterLast('.', "png"))
            // Arte nova começa enquadrada do zero (cobrindo tudo, centralizada).
            updateCover { it.copy(image = relative, artZoom = 1f, artOffsetXMm = 0f, artOffsetYMm = 0f) }
        }
    }

    /** Importa uma imagem solta (logo, selo) e a põe no canto inferior esquerdo da contracapa, para arrastar. */
    fun addCoverOverlay() {
        val b = book ?: return
        val laid = layout ?: return
        scope.launch {
            val source = chooseOpenFile("Imagem sobre a capa (logo, selo)", COVER_IMAGE_EXTENSIONS) ?: return@launch
            val bytes = services.fs.readBytes(source)
            if (bytes == null || imagePixelSize(bytes) == null) {
                message = "Não foi possível ler a imagem."
                return@launch
            }
            val relative = services.books.importAsset(b.path, bytes, source.substringAfterLast('.', "png"))
            val g = coverGeometry(laid, layoutSettings.cover)
            val back = g.zone(CoverPanel.Contracapa)!!
            val overlay = CoverOverlay(services.newId(), relative, back.xMm + COVER_SAFE_MM, g.heightMm - COVER_SAFE_MM - 20f, 25f)
            updateCover { it.copy(overlays = it.overlays + overlay) }
        }
    }

    fun updateCoverOverlay(id: String, transform: (CoverOverlay) -> CoverOverlay) =
        updateCover { c -> c.copy(overlays = c.overlays.map { if (it.id == id) transform(it) else it }) }

    fun removeCoverOverlay(id: String) = updateCover { c -> c.copy(overlays = c.overlays.filterNot { it.id == id }) }

    /** Capa montada sobre o miolo [laid] (a lombada vem do número de páginas dele). Pode ler a arte do disco. */
    fun coverFor(laid: LaidOutBook, settings: LayoutSettings = layoutSettings): CoverLayout {
        val px = settings.cover.image.takeIf { it.isNotBlank() }?.let { layoutImage(it) }?.let(::imagePixelSize)
        return layoutCover(laid, settings.cover, settings.front.isbn, layoutMeasurer(), px) { path -> layoutImage(path)?.let(::imagePixelSize) }
    }

    fun exportCoverPdf() {
        val b = book ?: return
        val laid = layout ?: return
        val settings = layoutSettings
        scope.launch {
            val cover = withContext(Dispatchers.Default) { coverFor(laid, settings) }
            if (!cover.canExport) {
                message = "A capa tem pendências: " + cover.issues.filter { it.blocking }.joinToString(" ") { it.message }
                return@launch
            }
            val target = chooseSaveFile("${b.manifest.title} - capa", "pdf") ?: return@launch
            runCatching { withContext(Dispatchers.Default) { writeCoverPdf(cover, target, ::layoutImage) } }
                .onSuccess {
                    // Guarda a lombada desta capa: se o miolo mudar depois, a conferência avisa.
                    updateCover { c -> c.copy(lastExport = CoverExportRecord(laid.pages.size, cover.geometry.spineMm)) }
                    scope.launch { storeCoverSnapshot(cover, withContext(Dispatchers.Default) { coverSkinPng(cover) }) }
                    message = "PDF da capa gerado: $target (lombada de ${formatMm(cover.geometry.spineMm)} mm)"
                }
                .onFailure { message = "Falha ao gerar o PDF da capa: ${it.message}" }
        }
    }

    /** Muda quando uma vitrine é gravada, para a Biblioteca reler. */
    var coverSnapshotVersion by mutableStateOf(0); private set

    /** Pele do livro 3D desta capa, em PNG (pode demorar: rode fora da thread da tela). */
    fun coverSkinPng(cover: CoverLayout): ByteArray = coverFacesPng(cover, COVER_SKIN_WIDTH_PX, ::layoutImage)

    /** Grava a vitrine do livro aberto (a Biblioteca mostra o livro 3D com ela). */
    fun storeCoverSnapshot(cover: CoverLayout, png: ByteArray) {
        val b = book ?: return
        runCatching { saveCoverSnapshot(services.fs, b.path, cover.geometry.snapshot(), png) }
            .onSuccess { coverSnapshotVersion++ }
    }

    fun coverSnapshot(bookPath: String): Pair<CoverSnapshot, ByteArray>? = loadCoverSnapshot(services.fs, bookPath)

    /** Frente da capa em JPEG para lojas de e-book (não precisa de ISBN nem de miolo par). */
    fun exportEbookCover(size: EbookCoverSize) {
        val b = book ?: return
        val laid = layout ?: return
        val settings = layoutSettings
        if (settings.cover.image.isBlank() && settings.cover.gradient == null) {
            message = "Importe a arte ou escolha um degradê antes de gerar a capa do e-book."
            return
        }
        scope.launch {
            val cover = withContext(Dispatchers.Default) { coverFor(laid, settings) }
            val target = chooseSaveFile("${b.manifest.title} - capa ebook", "jpg") ?: return@launch
            runCatching { withContext(Dispatchers.Default) { writeEbookCover(cover, target, size, ::layoutImage) } }
                .onSuccess {
                    val (w, h) = ebookCoverPixels(size, cover.geometry)
                    message = "Capa do e-book gerada: $target ($w × $h px)"
                }
                .onFailure { message = "Falha ao gerar a capa do e-book: ${it.message}" }
        }
    }

    // ---------- Preferências ----------

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        services.settings.update(transform)
        settings = services.settings.current
        services.trash.retentionDays = settings.trashRetentionDays
    }

    // ---------- Internos ----------

    private fun updateChapterMeta(id: String, transform: (ChapterMeta) -> ChapterMeta) =
        updateManifest { m -> m.copy(chapters = m.chapters.map { if (it.id == id) transform(it) else it }) }

    private fun updateManifest(transform: (BookManifest) -> BookManifest) {
        val b = book ?: return
        val updated = transform(b.manifest)
        if (updated == b.manifest) return
        b.manifest = updated.copy(modifiedAt = services.now())
        services.books.saveManifest(b.path, b.manifest)
    }

    /** Marca do capítulo hoje; se ainda não há, começa em [current]. Muda de dia, recomeça do zero. */
    private fun highWater(chapterId: String, current: Int): Int {
        val day = today()
        if (day != highWaterDay) {
            wordsHighWater.clear()
            highWaterDay = day
        }
        return wordsHighWater.getOrPut(chapterId) { current }
    }

    /** Texto que entrou sem ser digitado (substituição, restauração) passa a fazer parte da marca. */
    private fun raiseHighWater(chapterId: String, words: Int) {
        wordsHighWater[chapterId] = maxOf(highWater(chapterId, words), words)
    }

    private fun today() = services.dayKey(services.now())

    companion object {
        const val AUTOSAVE_DELAY_MS = 800L
        const val ENTRY_SAVE_DELAY_MS = 300L
        const val CHECK_DELAY_MS = 600L
        const val LAYOUT_DELAY_MS = 350L
        const val HISTORY_DAYS = 84
        const val ACTIVE_GAP_MS = 30_000L
        const val UNDO_MERGE_MS = 1_500L
        const val FIELD_SYNC_MS = 120L
        const val MAX_UNDO = 300
        val IMAGE_EXTENSIONS = listOf("png", "jpg", "jpeg", "gif", "webp")
        /** Só formatos que o PDF da capa embute sem conversão. */
        val COVER_IMAGE_EXTENSIONS = listOf("png", "jpg", "jpeg")
        /** Largura da pele do livro 3D (contracapa + lombada + capa). */
        const val COVER_SKIN_WIDTH_PX = 2000
    }
}
