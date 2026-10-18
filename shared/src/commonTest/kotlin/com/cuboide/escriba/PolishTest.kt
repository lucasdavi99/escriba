package com.cuboide.escriba

import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.EscribaServices
import com.cuboide.escriba.core.model.ChapterMeta
import com.cuboide.escriba.core.storage.BookRepository
import com.cuboide.escriba.core.storage.SnapshotStore
import com.cuboide.escriba.core.storage.TrashStore
import com.cuboide.escriba.core.text.DayKey
import com.cuboide.escriba.core.text.heatmapColumns
import com.cuboide.escriba.core.text.searchBook
import com.cuboide.escriba.core.text.stripMarkup
import com.cuboide.escriba.core.text.visibleText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Acertos de acabamento: busca e versões sem marcação, lixeira, último livro, mapa de calor. */
class PolishTest {
    private val fs = InMemoryFileStore()
    private val clock = TestClock()
    private val books = BookRepository(fs, "/lib", { clock.now }, clock::id)
    private val trash = TrashStore(fs, "/data/lixeira", books, { clock.now }, clock::id)
    private val snapshots = SnapshotStore(fs, { clock.now }, clock::id)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @AfterTest
    fun tearDown() = scope.cancel()

    // ---------- Texto visível

    @Test
    fun visibleTextMatchesStripMarkupAndMapsBack() {
        val raw = "# Título\nEra um **rei** [bom]{.smallcaps}[^1].\n* * *\n![Mapa](assets/m.png)"
        val visible = visibleText(raw)
        assertEquals(stripMarkup(raw), visible.text)
        val at = visible.text.indexOf("rei")
        assertEquals("rei", raw.substring(visible.rawIndex[at], visible.rawEnd(at + 3)))
    }

    @Test
    fun bookSearchLooksAtWhatTheReaderSees() {
        val raw = "Era um rei **bom** e [justo]{.smallcaps}[^1]."
        val hits = searchBook(listOf(Triple("c1", "Um", raw)), "rei bom")
        assertEquals(1, hits.size)
        val hit = hits.single()
        // O trecho vai do primeiro ao último caractere visível; o "**" final fica de fora.
        assertEquals("rei **bom", raw.substring(hit.start, hit.end))
        assertFalse("**" in hit.snippet || "{.smallcaps}" in hit.snippet || "[^1]" in hit.snippet)
        assertEquals("rei bom", hit.snippet.substring(hit.snippetMatchStart, hit.snippetMatchEnd))
        // O marcador sozinho não é texto do livro.
        assertTrue(searchBook(listOf(Triple("c1", "Um", raw)), "**").isEmpty())
    }

    // ---------- Lixeira

    private fun bookWith(vararg titles: String): Pair<String, List<ChapterMeta>> {
        val book = books.createBook("Livro")
        val chapters = titles.map { ChapterMeta(clock.id(), it) }
        books.saveManifest(book.path, books.loadManifest(book.path).copy(chapters = chapters))
        chapters.forEach { books.writeChapter(book.path, it.id, "texto de ${it.title}") }
        return book.path to chapters
    }

    private fun trashAt(path: String, chapter: ChapterMeta) {
        val manifest = books.loadManifest(path)
        val index = manifest.chapters.indexOfFirst { it.id == chapter.id }
        clock.now += 1_000
        trash.trashChapter(path, "Livro", chapter, books.readChapter(path, chapter.id), index)
        books.deleteChapterFile(path, chapter.id)
        books.saveManifest(path, manifest.copy(chapters = manifest.chapters.filterNot { it.id == chapter.id }))
    }

    @Test
    fun restoredChapterGoesBackToItsPlace() {
        val (path, ch) = bookWith("A", "B", "C", "D")
        trashAt(path, ch[1])
        trash.restore(trash.list().single())
        assertEquals(listOf("A", "B", "C", "D"), books.loadManifest(path).chapters.map { it.title })
    }

    @Test
    fun restoreAllUndoesDeletionsInReverseOrder() {
        val (path, ch) = bookWith("A", "B", "C", "D")
        trashAt(path, ch[1]) // B saiu da posição 1
        trashAt(path, ch[3]) // D saiu da posição 2 (depois de B ter saído)
        assertEquals(2, trash.restoreAll(trash.list()))
        assertEquals(listOf("A", "B", "C", "D"), books.loadManifest(path).chapters.map { it.title })
        assertTrue(trash.list().isEmpty())
    }

    @Test
    fun deletingAChapterForeverAlsoRemovesItsVersions() {
        val (path, ch) = bookWith("A", "B")
        snapshots.create(path, ch[0].id, "versão antiga de A", "manual", 3)
        snapshots.create(path, ch[1].id, "versão antiga de B", "manual", 3)
        trashAt(path, ch[0])
        trash.deleteForever(trash.list().single())
        assertTrue(snapshots.list(path, ch[0].id).isEmpty())
        assertEquals(1, snapshots.list(path, ch[1].id).size)
    }

    @Test
    fun controllerEmptiesAndRestoresTheTrash() {
        val c = EscribaController(EscribaServices(fs, "/lib", "/data", { clock.now }, clock::id, { "2026-10-18" }), scope)
        c.createBook("Livro", "")
        c.addChapter()
        c.addChapter()
        val ids = c.book!!.manifest.chapters.map { it.id }
        c.trashChapter(ids[0])
        clock.now += 1_000
        c.trashChapter(ids[1])
        c.restoreAllFromTrash(c.trashItems)
        assertEquals(ids, c.book!!.manifest.chapters.map { it.id })
        c.trashChapter(ids[2])
        c.emptyTrash(c.trashItems)
        assertTrue(c.trashItems.isEmpty())
        assertEquals(ids.take(2), c.book!!.manifest.chapters.map { it.id })
    }

    // ---------- Último livro

    @Test
    fun reopensTheLastBookOnStart() {
        val services = EscribaServices(fs, "/lib", "/data", { clock.now }, clock::id, { "2026-10-18" })
        val first = EscribaController(services, scope)
        first.createBook("Meu livro", "")
        val path = first.book!!.path
        first.closeBook() // voltar para a biblioteca não esquece o livro

        val second = EscribaController(EscribaServices(fs, "/lib", "/data", { clock.now }, clock::id, { "2026-10-18" }), scope)
        assertEquals(path, second.book?.path)

        second.trashBook(second.library.single())
        val third = EscribaController(EscribaServices(fs, "/lib", "/data", { clock.now }, clock::id, { "2026-10-18" }), scope)
        assertNull(third.book)
        assertNull(third.settings.lastBookPath)
    }

    // ---------- Mapa de calor

    @Test
    fun weekdays() {
        assertEquals(4, DayKey.dayOfWeek("1970-01-01")) // quinta
        assertEquals(0, DayKey.dayOfWeek("2026-10-18")) // domingo
        assertEquals(4, DayKey.dayOfWeek("2026-10-01"))
        assertEquals("18/10", DayKey.shortLabel("2026-10-18"))
    }

    @Test
    fun heatmapWeeksStartOnSundayWithMonthLabels() {
        // 1º/10/2026 é quinta: a primeira semana tem 4 vazios antes.
        val days = (0L until 24L).map { DayKey.plusDays("2026-10-01", it) }
        val columns = heatmapColumns(days)
        assertEquals(listOf(null, null, null, null, "2026-10-01", "2026-10-02", "2026-10-03"), columns.first().days)
        assertTrue(columns.all { it.days.size == 7 })
        assertEquals("out", columns.first().monthLabel)
        assertEquals(listOf("out"), columns.mapNotNull { it.monthLabel })
        // Semana que começa em setembro e entra em outubro leva o rótulo de outubro.
        val crossing = heatmapColumns((0L until 14L).map { DayKey.plusDays("2026-09-27", it) })
        assertEquals("out", crossing.first().monthLabel)
    }
}
