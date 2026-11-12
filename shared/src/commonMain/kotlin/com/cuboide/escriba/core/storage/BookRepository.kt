package com.cuboide.escriba.core.storage

import com.cuboide.escriba.core.board.PlotBoards
import com.cuboide.escriba.core.calendar.WorldCalendar
import com.cuboide.escriba.core.model.BOOK_EXTENSION
import com.cuboide.escriba.core.model.BookManifest
import com.cuboide.escriba.core.model.BookNotes
import com.cuboide.escriba.core.model.BookSummary
import com.cuboide.escriba.core.model.ChapterMeta
import com.cuboide.escriba.core.model.Outline
import com.cuboide.escriba.core.model.Universe
import com.cuboide.escriba.core.universe.migrated

/**
 * Formato de um livro no disco (pasta `Nome.escriba/`):
 *
 * ```
 * book.json            manifesto: metadados, metas e ordem dos capítulos
 * chapters/<id>.md     texto de cada capítulo
 * universe.json        fichas (personagens, locais, grupos, lore)
 * outline.json         roteiro: cartões de cena na ordem da história
 * enredo.json          mapas de enredo: quadros livres com nós, ligações e a legenda de cores
 * notes.json           blocos de anotações livres do livro
 * assets/…             imagens do texto (`![legenda](assets/x.png)`), da capa e dos mapas de enredo
 * snapshots/<id>/…     versões salvas de cada capítulo (ver SnapshotStore)
 * ```
 */
class BookRepository(
    private val fs: FileStore,
    val libraryDir: String,
    private val now: () -> Long,
    private val newId: () -> String,
) {
    fun listBooks(): List<BookSummary> {
        fs.mkdirs(libraryDir)
        return fs.list(libraryDir)
            .filter { it.endsWith(BOOK_EXTENSION) }
            .mapNotNull { name ->
                val path = joinPath(libraryDir, name)
                runCatching { BookSummary(path, loadManifest(path)) }.getOrNull()
            }
            .sortedByDescending { it.manifest.modifiedAt }
    }

    fun createBook(title: String, author: String = ""): BookSummary {
        val cleanTitle = title.trim().ifEmpty { "Livro sem título" }
        val path = uniqueBookPath(cleanTitle)
        fs.mkdirs(joinPath(path, CHAPTERS_DIR))
        val time = now()
        val firstChapter = ChapterMeta(id = newId(), title = "Capítulo 1", modifiedAt = time)
        val manifest = BookManifest(
            id = newId(),
            title = cleanTitle,
            author = author.trim(),
            createdAt = time,
            modifiedAt = time,
            chapters = listOf(firstChapter),
        )
        writeChapter(path, firstChapter.id, "")
        saveUniverse(path, Universe())
        saveOutline(path, Outline())
        saveManifest(path, manifest)
        return BookSummary(path, manifest)
    }

    fun loadManifest(bookPath: String): BookManifest {
        val text = fs.readText(joinPath(bookPath, MANIFEST)) ?: error("Livro sem $MANIFEST: $bookPath")
        return EscribaJson.decodeFromString(BookManifest.serializer(), text)
    }

    fun saveManifest(bookPath: String, manifest: BookManifest) {
        fs.writeText(joinPath(bookPath, MANIFEST), EscribaJson.encodeToString(BookManifest.serializer(), manifest))
    }

    fun readChapter(bookPath: String, chapterId: String): String =
        fs.readText(chapterPath(bookPath, chapterId)) ?: ""

    fun writeChapter(bookPath: String, chapterId: String, body: String) {
        fs.writeText(chapterPath(bookPath, chapterId), body)
    }

    fun deleteChapterFile(bookPath: String, chapterId: String) {
        fs.deleteRecursively(chapterPath(bookPath, chapterId))
    }

    fun loadUniverse(bookPath: String): Universe {
        val text = fs.readText(joinPath(bookPath, UNIVERSE)) ?: return Universe()
        // Fichas antigas viram blocos em memória; o arquivo só muda no próximo salvamento.
        return runCatching { EscribaJson.decodeFromString(Universe.serializer(), text).migrated() }.getOrDefault(Universe())
    }

    fun saveUniverse(bookPath: String, universe: Universe) {
        fs.writeText(joinPath(bookPath, UNIVERSE), EscribaJson.encodeToString(Universe.serializer(), universe))
    }

    /** Livros antigos não têm `outline.json`: o roteiro começa vazio. */
    fun loadOutline(bookPath: String): Outline {
        val text = fs.readText(joinPath(bookPath, OUTLINE)) ?: return Outline()
        return runCatching { EscribaJson.decodeFromString(Outline.serializer(), text) }.getOrDefault(Outline())
    }

    fun saveOutline(bookPath: String, outline: Outline) {
        fs.writeText(joinPath(bookPath, OUTLINE), EscribaJson.encodeToString(Outline.serializer(), outline))
    }

    /** Livros antigos não têm `enredo.json`: o mapa começa vazio (o controlador cria o primeiro quadro). */
    fun loadBoards(bookPath: String): PlotBoards {
        val text = fs.readText(joinPath(bookPath, ENREDO)) ?: return PlotBoards()
        return runCatching { EscribaJson.decodeFromString(PlotBoards.serializer(), text) }.getOrDefault(PlotBoards())
    }

    /** Calendário do mundo do livro, ou `null` se o livro ainda não tem um (ou o arquivo não pôde ser lido). */
    fun loadCalendar(bookPath: String): WorldCalendar? {
        val text = fs.readText(joinPath(bookPath, CALENDAR)) ?: return null
        return runCatching { EscribaJson.decodeFromString(WorldCalendar.serializer(), text) }.getOrNull()
    }

    fun saveCalendar(bookPath: String, calendar: WorldCalendar) {
        fs.writeText(joinPath(bookPath, CALENDAR), EscribaJson.encodeToString(WorldCalendar.serializer(), calendar))
    }

    fun saveBoards(bookPath: String, boards: PlotBoards) {
        fs.writeText(joinPath(bookPath, ENREDO), EscribaJson.encodeToString(PlotBoards.serializer(), boards))
    }

    fun loadNotes(bookPath: String): BookNotes {
        val text = fs.readText(joinPath(bookPath, NOTES)) ?: return BookNotes()
        return runCatching { EscribaJson.decodeFromString(BookNotes.serializer(), text) }.getOrDefault(BookNotes())
    }

    fun saveNotes(bookPath: String, notes: BookNotes) {
        fs.writeText(joinPath(bookPath, NOTES), EscribaJson.encodeToString(BookNotes.serializer(), notes))
    }

    /** Copia uma imagem para `assets/` do livro e devolve o caminho relativo usado no texto. */
    fun importAsset(bookPath: String, bytes: ByteArray, extension: String): String {
        val relative = joinPath(ASSETS_DIR, safeFileName(newId(), "imagem") + "." + extension.lowercase())
        fs.writeBytes(joinPath(bookPath, relative), bytes)
        return relative
    }

    /** Bytes de um arquivo do livro pelo caminho relativo (ex.: imagens), ou `null` se não existir. */
    fun readAsset(bookPath: String, relative: String): ByteArray? =
        if (relative.contains("..")) null else fs.readBytes(joinPath(bookPath, relative))

    /** Todos os capítulos com texto, na ordem do livro. */
    fun readAllChapters(bookPath: String, manifest: BookManifest): List<Pair<ChapterMeta, String>> =
        manifest.chapters.map { it to readChapter(bookPath, it.id) }

    fun uniqueBookPath(title: String): String {
        val base = safeFileName(title, "Livro")
        var candidate = joinPath(libraryDir, base + BOOK_EXTENSION)
        var n = 2
        while (fs.exists(candidate)) candidate = joinPath(libraryDir, "$base ($n)$BOOK_EXTENSION").also { n++ }
        return candidate
    }

    private fun chapterPath(bookPath: String, chapterId: String) =
        joinPath(bookPath, CHAPTERS_DIR, safeFileName(chapterId, "capitulo") + ".md")

    companion object {
        const val MANIFEST = "book.json"
        const val UNIVERSE = "universe.json"
        const val OUTLINE = "outline.json"
        const val ENREDO = "enredo.json"
        const val CALENDAR = "calendar.json"
        const val CHAPTERS_DIR = "chapters"
        const val SNAPSHOTS_DIR = "snapshots"
        const val NOTES = "notes.json"
        const val ASSETS_DIR = "assets"
    }
}
