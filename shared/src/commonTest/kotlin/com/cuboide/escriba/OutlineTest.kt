package com.cuboide.escriba

import com.cuboide.escriba.core.model.Beat
import com.cuboide.escriba.core.model.BeatStatus
import com.cuboide.escriba.core.model.Outline
import com.cuboide.escriba.core.outline.BeatFilter
import com.cuboide.escriba.core.outline.StructureTemplates
import com.cuboide.escriba.core.outline.applyTemplate
import com.cuboide.escriba.core.outline.groupByAct
import com.cuboide.escriba.core.outline.move
import com.cuboide.escriba.core.outline.moveTo
import com.cuboide.escriba.core.outline.remove
import com.cuboide.escriba.core.outline.upsert
import com.cuboide.escriba.core.storage.BookRepository
import com.cuboide.escriba.core.storage.RestoreOutcome
import com.cuboide.escriba.core.storage.TrashKind
import com.cuboide.escriba.core.storage.TrashStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class OutlineTest {
    private val fs = InMemoryFileStore()
    private val clock = TestClock()
    private val books = BookRepository(fs, "/lib", { clock.now }, clock::id)
    private val trash = TrashStore(fs, "/data/lixeira", books, { clock.now }, clock::id)

    private fun outlineOf(vararg ids: String) = Outline(ids.map { Beat(it, "Cena $it") })
    private fun Outline.ids() = beats.map { it.id }

    @Test
    fun reorderKeepsOtherCardsAndIgnoresOutOfBounds() {
        val o = outlineOf("a", "b", "c", "d")
        assertEquals(listOf("b", "a", "c", "d"), o.move("a", 1).ids())
        assertEquals(listOf("d", "a", "b", "c"), o.moveTo("d", 0).ids())
        assertEquals(listOf("a", "c", "d", "b"), o.moveTo("b", 3).ids())
        assertSame(o, o.move("a", -1))
        assertSame(o, o.moveTo("c", 4))
        assertSame(o, o.move("x", 1))
    }

    @Test
    fun upsertReplacesInPlaceOrAppends() {
        val o = outlineOf("a", "b")
        val edited = o.upsert(Beat("a", "Novo título", status = BeatStatus.Escrita))
        assertEquals(listOf("a", "b"), edited.ids())
        assertEquals("Novo título", edited.beats.first().title)
        assertEquals(listOf("a", "b", "c"), o.upsert(Beat("c", "C")).ids())
        assertEquals(listOf("b"), o.remove("a").ids())
    }

    @Test
    fun templatesAreAppendedWithFreshIdsAndActs() {
        val existing = outlineOf("x")
        val result = existing.applyTemplate(StructureTemplates.ThreeActs, clock::id, clock.now)
        val added = result.beats.drop(1)
        assertEquals("x", result.beats.first().id)
        assertEquals(StructureTemplates.ThreeActs.steps.size, added.size)
        assertEquals(added.size, added.map { it.id }.toSet().size)
        assertTrue(added.all { it.status == BeatStatus.Planejada && it.summary.isNotBlank() })

        val groups = groupByAct(added)
        assertEquals(listOf("Ato I · Apresentação", "Ato II · Confronto", "Ato III · Resolução"), groups.map { it.act })
        assertEquals(added.size, groups.sumOf { it.beats.size })

        val journey = Outline().applyTemplate(StructureTemplates.HeroJourney, clock::id, clock.now)
        assertEquals(12, journey.beats.size)
        assertEquals(listOf("Partida", "Iniciação", "Retorno"), groupByAct(journey.beats).map { it.act })
    }

    @Test
    fun groupingOnlyJoinsConsecutiveCards() {
        val beats = listOf(Beat("1", "a", act = "I"), Beat("2", "b", act = "II"), Beat("3", "c", act = "I"))
        assertEquals(listOf("I", "II", "I"), groupByAct(beats).map { it.act })
    }

    @Test
    fun filterByStatusCharacterAndText() {
        val beats = listOf(
            Beat("1", "Fuga", summary = "Lia foge", characterIds = listOf("lia")),
            Beat("2", "Duelo", status = BeatStatus.Escrita, characterIds = listOf("lia", "rui")),
            Beat("3", "Epílogo", status = BeatStatus.Escrita),
        )
        assertEquals(listOf("1", "2"), beats.filter(BeatFilter(characterId = "lia")::matches).map { it.id })
        assertEquals(listOf("2"), beats.filter(BeatFilter(BeatStatus.Escrita, "rui")::matches).map { it.id })
        assertEquals(listOf("1"), beats.filter(BeatFilter(query = "FOGE")::matches).map { it.id })
    }

    @Test
    fun outlineIsSavedInsideTheBookFolder() {
        val book = books.createBook("Roteiro")
        assertTrue(books.loadOutline(book.path).beats.isEmpty())
        val chapterId = book.manifest.chapters.single().id
        val outline = Outline(listOf(Beat("b1", "Abertura", chapterId = chapterId, characterIds = listOf("e1"))))
        books.saveOutline(book.path, outline)
        assertTrue(fs.exists("${book.path}/${BookRepository.OUTLINE}"))
        assertEquals(outline, books.loadOutline(book.path))

        // Livro antigo, sem outline.json, e arquivo corrompido não quebram a abertura.
        fs.deleteRecursively("${book.path}/${BookRepository.OUTLINE}")
        assertEquals(Outline(), books.loadOutline(book.path))
        fs.writeText("${book.path}/${BookRepository.OUTLINE}", "{ quebrado")
        assertEquals(Outline(), books.loadOutline(book.path))
    }

    @Test
    fun beatGoesToTrashAndReturnsToItsPosition() {
        val book = books.createBook("Livro")
        val outline = outlineOf("a", "b", "c")
        val removed = outline.beats[1]
        trash.trashBeat(book.path, "Livro", removed, 1)
        books.saveOutline(book.path, outline.remove("b"))

        val item = trash.list().single()
        assertEquals(TrashKind.Cena, item.kind)
        assertEquals("Cena b", item.title)
        assertIs<RestoreOutcome.Restored>(trash.restore(item))
        assertEquals(listOf("a", "b", "c"), books.loadOutline(book.path).ids())
        assertTrue(trash.list().isEmpty())
    }

    @Test
    fun restoredBeatGetsNewIdOnConflictAndNeedsItsBook() {
        val book = books.createBook("Livro")
        books.saveOutline(book.path, outlineOf("a"))
        trash.trashBeat(book.path, "Livro", Beat("a", "Duplicada"), 99)
        assertIs<RestoreOutcome.Restored>(trash.restore(trash.list().single()))
        val beats = books.loadOutline(book.path).beats
        assertEquals(2, beats.size)
        assertEquals("Duplicada", beats.last().title)
        assertNotEquals("a", beats.last().id)

        trash.trashBeat(book.path, "Livro", Beat("z", "Órfã"), 0)
        trash.trashBook(book.path, "Livro")
        val beatItem = trash.list().first { it.kind == TrashKind.Cena }
        assertIs<RestoreOutcome.OriginMissing>(trash.restore(beatItem))
    }
}
