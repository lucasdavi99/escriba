package com.cuboide.escriba

import com.cuboide.escriba.core.export.buildExportDocument
import com.cuboide.escriba.core.export.exportMarkdown
import com.cuboide.escriba.core.model.EntryKind
import com.cuboide.escriba.core.model.UniverseEntry
import com.cuboide.escriba.core.storage.BookRepository
import com.cuboide.escriba.core.storage.ProductivityStore
import com.cuboide.escriba.core.storage.RestoreOutcome
import com.cuboide.escriba.core.storage.SnapshotStore
import com.cuboide.escriba.core.storage.TrashStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StorageTest {
    private val fs = InMemoryFileStore()
    private val clock = TestClock()
    private val books = BookRepository(fs, "/lib", { clock.now }, clock::id)
    private val trash = TrashStore(fs, "/data/lixeira", books, { clock.now }, clock::id)

    @Test
    fun createListAndReadBook() {
        val created = books.createBook("Meu: Livro?", "Ana")
        assertEquals("/lib/Meu_ Livro_.escriba", created.path)
        val chapterId = created.manifest.chapters.single().id
        books.writeChapter(created.path, chapterId, "Olá mundo")
        assertEquals("Olá mundo", books.readChapter(created.path, chapterId))
        assertEquals(listOf("Meu: Livro?"), books.listBooks().map { it.manifest.title })
        // Mesmo título não sobrescreve o livro existente.
        assertEquals("/lib/Meu_ Livro_ (2).escriba", books.createBook("Meu: Livro?").path)
    }

    @Test
    fun chapterGoesToTrashAndComesBack() {
        val book = books.createBook("Livro")
        val chapter = book.manifest.chapters.single()
        books.writeChapter(book.path, chapter.id, "texto precioso")
        trash.trashChapter(book.path, "Livro", chapter, "texto precioso")
        books.deleteChapterFile(book.path, chapter.id)
        books.saveManifest(book.path, book.manifest.copy(chapters = emptyList()))

        val item = trash.list().single()
        assertIs<RestoreOutcome.Restored>(trash.restore(item))
        val restored = books.loadManifest(book.path).chapters.single()
        assertEquals("texto precioso", books.readChapter(book.path, restored.id))
        assertTrue(trash.list().isEmpty())
    }

    @Test
    fun bookTrashRoundTripAndOriginMissing() {
        val book = books.createBook("Saga")
        books.saveUniverse(book.path, books.loadUniverse(book.path))
        trash.trashEntry(book.path, "Saga", UniverseEntry("e1", EntryKind.Personagem, "Lia"))
        trash.trashBook(book.path, "Saga")
        assertFalse(fs.exists(book.path))

        val entryItem = trash.list().first { it.entry != null }
        assertIs<RestoreOutcome.OriginMissing>(trash.restore(entryItem))

        val bookItem = trash.list().first { it.entry == null }
        val outcome = trash.restore(bookItem)
        assertIs<RestoreOutcome.Restored>(outcome)
        assertEquals("Saga", books.loadManifest(outcome.bookPath).title)
        assertIs<RestoreOutcome.Restored>(trash.restore(trash.list().single()))
        assertEquals(listOf("Lia"), books.loadUniverse(outcome.bookPath).entries.map { it.name })
    }

    @Test
    fun trashPurgesExpiredItems() {
        val book = books.createBook("Livro")
        trash.retentionDays = 30
        trash.trashChapter(book.path, "Livro", book.manifest.chapters.single(), "")
        clock.now += 29 * TrashStore.DAY_MS
        assertEquals(0, trash.purgeExpired())
        assertEquals(1, trash.daysLeft(trash.list().single()))
        clock.now += 2 * TrashStore.DAY_MS
        assertEquals(1, trash.purgeExpired())
        assertTrue(trash.list().isEmpty())
    }

    @Test
    fun snapshotsAreListedNewestFirstAndPruned() {
        val store = SnapshotStore(fs, { clock.now }, clock::id, maxPerChapter = 3)
        repeat(5) { i ->
            clock.now += 1000
            store.create("/lib/B.escriba", "c1", "v$i", "versão $i", i)
        }
        assertEquals(listOf("v4", "v3", "v2"), store.list("/lib/B.escriba", "c1").map { it.body })
    }

    @Test
    fun productivityStreakAndPersistence() {
        val p = ProductivityStore(fs, "/data/prod.json")
        p.record("2026-10-14", 300, 600)
        p.record("2026-10-15", 500, 900)
        p.record("2026-10-16", 200, 300)
        p.record("2026-10-16", -50, 0) // apagar texto não desconta palavras escritas
        val s = ProductivityStore(fs, "/data/prod.json").summary("2026-10-17", recentDays = 7)
        assertEquals(3, s.streakDays) // hoje ainda sem escrita: conta até ontem
        assertEquals("2026-10-15" to 500, s.bestDay)
        assertEquals(1000, s.totalWords)
        assertEquals(7, s.recent.size)
        assertEquals("2026-10-17", s.recent.last().first)
    }

    @Test
    fun exportsMarkdownInBookOrder() {
        val book = books.createBook("Livro", "Ana")
        val c1 = book.manifest.chapters.single()
        books.writeChapter(book.path, c1.id, "Primeira linha.\n\nSegunda *linha*.")
        val doc = buildExportDocument(book.manifest, books.readAllChapters(book.path, book.manifest))
        assertEquals(
            "# Livro\n\n*por Ana*\n\n## Capítulo 1\n\nPrimeira linha.\n\nSegunda *linha*.\n",
            exportMarkdown(doc),
        )
    }
}
