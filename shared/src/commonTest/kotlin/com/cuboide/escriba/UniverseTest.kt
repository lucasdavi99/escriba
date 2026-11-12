package com.cuboide.escriba

import androidx.compose.ui.text.input.TextFieldValue
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.EscribaServices
import com.cuboide.escriba.core.model.EntryKind
import com.cuboide.escriba.core.model.Universe
import com.cuboide.escriba.core.model.UniverseEntry
import com.cuboide.escriba.core.storage.EscribaJson
import com.cuboide.escriba.core.storage.joinPath
import com.cuboide.escriba.core.text.TextMatch
import com.cuboide.escriba.core.universe.EntrySchemas
import com.cuboide.escriba.core.universe.MentionIndex
import com.cuboide.escriba.core.universe.UNIVERSE_SCHEMA_VERSION
import com.cuboide.escriba.core.universe.aliases
import com.cuboide.escriba.core.universe.blockText
import com.cuboide.escriba.core.universe.digest
import com.cuboide.escriba.core.universe.findMentions
import com.cuboide.escriba.core.universe.fold
import com.cuboide.escriba.core.universe.matchesFilter
import com.cuboide.escriba.core.universe.migrated
import com.cuboide.escriba.core.universe.schema
import com.cuboide.escriba.core.universe.withAttr
import com.cuboide.escriba.core.universe.withBlock
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

class UniverseModelTest {
    private var n = 0
    private val id = { "id${++n}" }

    @Test
    fun oldEntriesBecomeBlocksAndMigrationIsIdempotent() {
        val old = Universe(
            listOf(
                UniverseEntry("p", EntryKind.Personagem, "Mara", summary = "Capitã", notes = "Tem medo do mar."),
                UniverseEntry("l", EntryKind.Local, "Porto Velho", summary = "Cidade de pedra", notes = "Chove muito."),
                UniverseEntry("o", EntryKind.Lore, "A Maré", summary = "Magia das águas"),
                UniverseEntry("g", EntryKind.Grupo, "Remadores"),
            ),
        )
        val once = old.migrated()
        assertEquals(UNIVERSE_SCHEMA_VERSION, once.schemaVersion)
        val (p, l, o, g) = once.entries
        assertEquals("Capitã\n\nTem medo do mar.", p.blockText(EntrySchemas.NOTES))
        assertEquals("Cidade de pedra", l.blockText(EntrySchemas.DESCRIPTION))
        assertEquals("Chove muito.", l.blockText(EntrySchemas.NOTES))
        assertEquals("Magia das águas", o.blockText("conteudo"))
        assertTrue(g.blocks.isEmpty())
        assertTrue(once.entries.all { it.summary.isEmpty() && it.notes.isEmpty() })
        assertEquals(once, once.migrated())
        // Ficha da versão 2 já gravada não volta a ser migrada mesmo com campos antigos preenchidos.
        val v2 = Universe(listOf(UniverseEntry("x", EntryKind.Local, "X", summary = "fica")), UNIVERSE_SCHEMA_VERSION)
        assertEquals(v2, v2.migrated())
    }

    @Test
    fun oldUniverseJsonStillDecodes() {
        val json = """{"entries":[{"id":"a","kind":"Personagem","name":"Ilda","summary":"Ferreira","tags":["forja"]}]}"""
        val u = EscribaJson.decodeFromString(Universe.serializer(), json).migrated()
        val e = u.entries.single()
        assertEquals("Ferreira", e.blockText(EntrySchemas.NOTES))
        assertEquals(listOf("forja"), e.tags)
        assertEquals("Ferreira", e.digest())
    }

    @Test
    fun attributesAndBlocksAreCreatedOnDemandAndDroppedWhenEmpty() {
        var e = UniverseEntry("e", EntryKind.Personagem, "Mara")
        e = e.withAttr(EntrySchemas.ALIASES, "Marinha; a Capitã, ", id)
        assertEquals(listOf("Marinha", "a Capitã"), e.aliases)
        assertEquals("Apelidos", e.attributes.single().label)
        e = e.withAttr(EntrySchemas.ALIASES, "", id)
        assertTrue(e.attributes.isEmpty())
        e = e.withBlock("aparencia", "Alta.", id)
        assertEquals("Descrição física", e.blocks.single().title)
        e = e.withBlock("aparencia", "", id)
        assertTrue(e.blocks.isEmpty())
        assertEquals("Papel na história", e.schema.subtitleLabel)
    }

    @Test
    fun foldKeepsPositionsAndDropsAccents() {
        val text = "Ângela e JOÃO, açúcar"
        assertEquals(text.length, fold(text).length)
        assertEquals("angela e joao, acucar", fold(text))
    }

    @Test
    fun mentionsUseWholeWordsAccentsAliasesAndLongestFirst() {
        val text = "Ana Clara viu Ana. Mariana não conta. Ângela, a Capitã, sorriu; ANGELA voltou."
        // "Ana" não pega dentro de "Mariana" nem conta de novo dentro de "Ana Clara".
        assertEquals(listOf(TextMatch(0, 9), TextMatch(14, 17)), findMentions(text, listOf("Ana", "Ana Clara")))
        val angela = findMentions(text, listOf("Angela", "a Capitã"))
        assertEquals(listOf("Ângela", "a Capitã", "ANGELA"), angela.map { text.substring(it.start, it.end) })
        assertTrue(findMentions(text, listOf("A")).isEmpty()) // termo de uma letra é ignorado
    }

    @Test
    fun mentionIndexRereadsOnlyChangedChapters() {
        val entry = UniverseEntry("e", EntryKind.Personagem, "Mara")
        val bodies = mutableMapOf("c1" to "Mara chegou. Mara saiu.", "c2" to "Ninguém.")
        val reads = mutableListOf<String>()
        val index = MentionIndex()
        fun run(v2: Long) = index.mentions(entry, listOf(Triple("c1", "Um", 1L), Triple("c2", "Dois", v2))) { reads += it; bodies.getValue(it) }

        val first = run(1L)
        assertEquals(1, first.citedIn)
        assertEquals(2, first.chapters.single().matches.size)
        assertEquals(2, first.totalChapters)
        assertEquals(listOf("Mara"), first.terms)
        reads.clear()
        bodies["c2"] = "Mara também."
        val second = run(2L)
        assertEquals(listOf("c2"), reads) // c1 veio do cache
        assertEquals(2, second.citedIn)
    }

    @Test
    fun filterLooksAtNameAliasesTagsAndText() {
        var n2 = 0
        val e = UniverseEntry("e", EntryKind.Personagem, "Mara", tags = listOf("mar"))
            .withAttr(EntrySchemas.ALIASES, "Capitã", { "a${++n2}" })
            .withAttr("cabelo", "ruivo", { "a${++n2}" })
            .withBlock("aparencia", "Olhos **cinzentos**.", { "b${++n2}" })
        assertTrue(e.matchesFilter("capita"))
        assertTrue(e.matchesFilter("RUIVO"))
        assertTrue(e.matchesFilter("cinzentos"))
        assertTrue(e.matchesFilter("mar"))
        assertTrue(e.matchesFilter(" "))
        assertFalse(e.matchesFilter("loiro"))
    }
}

class UniverseControllerTest {
    private val fs = InMemoryFileStore()
    private val clock = TestClock()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val services = EscribaServices(fs, "/lib", "/data", { clock.now }, clock::id, { "2026-11-11" })
    private val c = EscribaController(services, scope)

    @AfterTest
    fun tearDown() = scope.cancel()

    private fun onDisk(): Universe = services.books.loadUniverse(c.book!!.path)

    @Test
    fun typingIsSavedLaterAndFlushedWhenLeaving() {
        c.createBook("Livro", "")
        val e = c.createEntry(EntryKind.Personagem)
        c.updateEntry(e.copy(name = "Mara"))
        assertEquals("Mara", c.book!!.universe.entries.single().name) // a tela já mostra
        assertEquals("", onDisk().entries.single().name)               // o disco ainda não
        c.flushEntrySave()
        assertEquals("Mara", onDisk().entries.single().name)
        assertEquals(UNIVERSE_SCHEMA_VERSION, onDisk().schemaVersion)
    }

    @Test
    fun portraitAndGallery() {
        c.createBook("Livro", "")
        val path = c.book!!.path
        val e = c.createEntry(EntryKind.Personagem)
        val a = services.books.importAsset(path, byteArrayOf(1), "png")
        val b = services.books.importAsset(path, byteArrayOf(2), "png")
        c.addEntryImagePath(e.id, a, asPortrait = false) // sem retrato: vira o retrato
        c.addEntryImagePath(e.id, b, asPortrait = false)
        var saved = onDisk().entries.single()
        assertEquals(a, saved.portrait?.path)
        assertEquals(listOf(b), saved.gallery.map { it.path })
        c.makePortrait(e.id, b)
        saved = onDisk().entries.single()
        assertEquals(b, saved.portrait?.path)
        assertEquals(listOf(a), saved.gallery.map { it.path })
        c.removeEntryImage(e.id, b)
        assertNull(onDisk().entries.single().portrait)
    }

    @Test
    fun mentionsCountPerChapterAndOpenTheChapter() {
        c.createBook("Livro", "")
        c.onEditorChange(TextFieldValue("Mara viu o mar. A Capitã riu."))
        c.flushSave()
        val first = c.currentChapterId!!
        c.addChapter()
        c.onEditorChange(TextFieldValue("Sem ninguém."))
        c.flushSave()
        val entry = c.createEntry(EntryKind.Personagem).copy(name = "Mara").withAttr(EntrySchemas.ALIASES, "a Capitã", c::newId)
        c.saveEntry(entry)

        val m = c.entryMentions(c.book!!.universe.entries.single())
        assertEquals(1, m.citedIn)
        assertEquals(2, m.totalChapters)
        assertEquals(2, m.chapters.single().matches.size)
        assertEquals(listOf(first), c.chaptersMentioning(entry).map { it.id })

        c.openMention(first, m.chapters.single().matches[1])
        assertEquals(first, c.currentChapterId)
        assertEquals("A Capitã", c.editor.text.substring(c.editor.selection.min, c.editor.selection.max))
        assertEquals(listOf("Mara" to 2), c.entriesInCurrentChapter().map { it.first.name to it.second })
    }

    @Test
    fun universeTrashShowsOnlyThisBooksEntries() {
        c.createBook("Um", "")
        val one = c.book!!.path
        c.trashEntry(c.createEntry(EntryKind.Local).copy(name = "Vale"))
        c.createBook("Dois", "")
        c.trashEntry(c.createEntry(EntryKind.Local).copy(name = "Serra"))
        assertEquals(listOf("Serra"), c.entryTrash.map { it.title })
        c.restoreFromTrash(c.entryTrash.single())
        assertEquals(listOf("Serra"), c.book!!.universe.entries.map { it.name })
        assertTrue(c.entryTrash.isEmpty())
        c.openBook(one)
        assertEquals(listOf("Vale"), c.entryTrash.map { it.title })
    }

    @Test
    fun oldBookOnDiskOpensMigrated() {
        c.createBook("Antigo", "")
        val path = c.book!!.path
        c.closeBook()
        fs.writeText(joinPath(path, "universe.json"), """{"entries":[{"id":"a","kind":"Local","name":"Vale","summary":"Verde"}]}""")
        c.openBook(path)
        assertEquals("Verde", c.book!!.universe.entries.single().blockText(EntrySchemas.DESCRIPTION))
    }
}
