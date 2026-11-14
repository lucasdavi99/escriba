package com.cuboide.escriba

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.EscribaServices
import com.cuboide.escriba.core.model.ChapterKind
import com.cuboide.escriba.core.model.EntryKind
import com.cuboide.escriba.core.text.BlockKind
import com.cuboide.escriba.core.text.MarkKind
import com.cuboide.escriba.core.universe.withAttr
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** O controller com a sessão do capítulo: o que acontece ao redor do texto (troca de capítulo, livro, chamadores). */
@OptIn(ExperimentalCoroutinesApi::class)
class ControllerTest {
    private val fs = InMemoryFileStore()
    private val clock = TestClock()
    private var day = "2026-10-17"
    /** Um relógio só: as esperas do controller e o `now` andam juntos, e só quando o teste manda. */
    private val scope = TestScope()
    private val services = EscribaServices(fs, "/lib", "/data", { clock.now + scope.testScheduler.currentTime }, clock::id, { day })
    private val c = EscribaController(services, scope)

    @AfterTest
    fun tearDown() = scope.cancel()

    private fun type(text: String) {
        c.chapter!!.type(TextFieldValue(text, TextRange(text.length)))
        c.flushSave()
    }

    private fun chapterOnDisk(id: String = c.currentChapterId!!): String = services.books.readChapter(c.book!!.path, id)

    private fun select(start: Int, end: Int) {
        c.chapter!!.select(TextRange(start, end))
    }

    @Test
    fun switchingChapterSavesThePendingText() {
        c.createBook("Livro", "")
        val first = c.currentChapterId!!
        c.chapter!!.type(TextFieldValue("ainda não gravado"))
        c.addChapter()
        assertEquals("ainda não gravado", chapterOnDisk(first))
    }

    @Test
    fun closingTheBookSavesThePendingText() {
        c.createBook("Livro", "")
        val path = c.book!!.path
        val id = c.currentChapterId!!
        c.chapter!!.type(TextFieldValue("antes de fechar"))
        assertTrue(c.closeBook())
        assertEquals("antes de fechar", services.books.readChapter(path, id))
    }

    @Test
    fun aChapterThatCannotBeSavedStaysOpen() {
        c.createBook("Livro", "")
        val first = c.currentChapterId!!
        c.addChapter()
        val second = c.currentChapterId!!
        c.selectChapter(first)
        c.chapter!!.type(TextFieldValue("não pode se perder"))
        fs.failWrites = 2 // a troca de capítulo e o fechar do livro

        assertFalse(c.selectChapter(second))
        assertEquals(first, c.currentChapterId)
        assertEquals("não pode se perder", c.chapter!!.text)
        assertNotNull(c.message)
        assertFalse(c.closeBook())
        assertNotNull(c.book)

        assertTrue(c.selectChapter(second)) // o disco voltou: grava e troca
        assertEquals("não pode se perder", chapterOnDisk(first))
    }

    @Test
    fun noNewChapterOrPartWhileTheOpenOneCannotBeSaved() {
        c.createBook("Livro", "")
        val first = c.currentChapterId!!
        c.chapter!!.type(TextFieldValue("não pode se perder"))
        fs.failWritesEndingWith = "/$first.md"
        val titleFocus = c.titleFocusRequests
        val editorFocus = c.editorFocusRequests

        c.addChapter()
        c.addPart(ChapterKind.Prologo)

        assertEquals(1, c.book!!.manifest.chapters.size)
        assertEquals(first, c.currentChapterId)
        assertEquals(titleFocus, c.titleFocusRequests) // o título do capítulo antigo não ganha foco para renomear
        assertEquals(editorFocus, c.editorFocusRequests)
    }

    @Test
    fun restoringAVersionThatCannotBeSavedKeepsTheWarning() {
        c.createBook("Livro", "")
        type("um dois três")
        c.snapshotCurrent()
        type("um")
        fs.failWritesEndingWith = "/${c.currentChapterId}.md"
        c.restoreSnapshot(c.snapshots.first { it.label == "Versão manual" })
        assertEquals("um dois três", c.chapter!!.text) // restaurado na tela, ainda pendente
        assertTrue("Não foi possível gravar" in c.message.orEmpty(), c.message)
    }

    @Test
    fun saveAllWritesTheChapterAndThePendingEntry() {
        c.createBook("Livro", "")
        val path = c.book!!.path
        val mara = c.createEntry(EntryKind.Personagem)
        c.chapter!!.type(TextFieldValue("Mara chegou."))
        c.updateEntry(mara.copy(name = "Mara")) // grava depois de uma pausa
        assertTrue(c.flushAll()) // Ctrl+S
        assertEquals("Mara chegou.", chapterOnDisk())
        assertEquals("Mara", services.books.loadUniverse(path).entries.single().name)
    }

    @Test
    fun exitingWithAChapterThatCannotBeSavedLeavesARecoveryCopy() {
        c.createBook("Livro", "")
        c.chapter!!.type(TextFieldValue("o texto que o disco recusou"))
        fs.failWrites = 1
        c.prepareToExit()

        val dir = "/data/recuperação"
        val copy = fs.list(dir).single()
        assertTrue("Livro" in copy && "Capítulo 1" in copy, copy)
        assertEquals("o texto que o disco recusou", fs.readText("$dir/$copy"))

        val next = EscribaController(services, scope) // a próxima abertura avisa, com livro e capítulo
        assertEquals(dir, next.recoveryDir)
        assertEquals(listOf("“Capítulo 1”, de Livro"), next.recoveredChapters)
        next.dismissRecovery()
        assertEquals(null, next.recoveryDir)
    }

    @Test
    fun exitingNormallyLeavesNoRecoveryCopy() {
        c.createBook("Livro", "")
        c.chapter!!.type(TextFieldValue("gravado"))
        c.prepareToExit()
        assertEquals("gravado", chapterOnDisk())
        assertEquals(null, EscribaController(services, scope).recoveryDir)
    }

    @Test
    fun undoBelongsToTheOpenChapter() {
        c.createBook("Livro", "")
        val first = c.currentChapterId!!
        type("primeiro capítulo")
        c.addChapter()
        assertFalse(c.chapter!!.canUndo)
        c.selectChapter(first)
        assertFalse(c.chapter!!.canUndo) // a sessão nova começa sem histórico
        assertEquals("primeiro capítulo", c.chapter!!.text)
    }

    @Test
    fun restoringAVersionIsNotWriting() {
        c.createBook("Livro", "")
        type("um dois três quatro cinco seis")
        c.snapshotCurrent()
        type("um")
        c.restoreSnapshot(c.snapshots.first { it.label == "Versão manual" })
        assertEquals("um dois três quatro cinco seis", chapterOnDisk())
        assertEquals(6, c.productivity.today.words)
    }

    @Test
    fun replacingInTheChapterIsNotWriting() {
        c.createBook("Livro", "")
        type("a b c")
        c.findQuery = "b"
        c.replaceText = "x y z w"
        c.replaceAllInChapter()
        c.flushSave()
        assertEquals("a x y z w c", chapterOnDisk())
        assertEquals(3, c.productivity.today.words)
    }

    @Test
    fun undoingAnEditRefreshesTheContradictionCheck() {
        c.createBook("Livro", "")
        val mara = c.createEntry(EntryKind.Personagem).copy(name = "Mara").withAttr("olhos", "verde", c::newId)
        c.saveEntry(mara)
        c.toggleSeal(mara.id, "olhos")
        type("Mara abriu os olhos verdes.")
        c.setCheckContradictions(true)
        assertTrue(c.currentIssues.isEmpty())

        c.chapter!!.edit(TextFieldValue("Mara abriu os olhos azuis."))
        scope.testScheduler.advanceTimeBy(EscribaController.CHECK_DELAY_MS + 1)
        assertEquals(1, c.currentIssues.size)

        c.chapter!!.undo()
        scope.testScheduler.advanceTimeBy(EscribaController.CHECK_DELAY_MS + 1)
        assertTrue(c.currentIssues.isEmpty())
    }

    @Test
    fun openFindAsksForKeyboardFocusEveryTime() {
        c.createBook("Livro", "")
        val before = c.findFocusRequests
        c.openFind()
        c.openFind() // Ctrl+F com a barra já aberta volta o foco para "Localizar"
        assertEquals(before + 2, c.findFocusRequests)
    }

    @Test
    fun formattingIsUndoableAndRedoable() {
        c.createBook("Livro", "")
        type("Era uma vez")
        select(4, 7)
        c.toggleMark(MarkKind.Italic)
        assertEquals("Era *uma* vez", c.chapter!!.text)
        c.toggleBlockStyle(BlockKind.Heading1)
        assertEquals("# Era *uma* vez", c.chapter!!.text)
        c.chapter!!.undo()
        assertEquals("Era *uma* vez", c.chapter!!.text)
        c.chapter!!.undo()
        assertEquals("Era uma vez", c.chapter!!.text)
        c.chapter!!.redo()
        assertEquals("Era *uma* vez", c.chapter!!.text)
        // Formatação não conta como palavras novas.
        assertEquals(3, c.chapter!!.words)
    }

    @Test
    fun footnotesLiveInTheChapter() {
        c.createBook("Livro", "")
        type("Era um rei")
        select(7, 10)
        c.addFootnote("Rei de quê?")
        assertEquals("Era um rei[^1]", c.chapter!!.text)
        assertEquals(listOf("1" to "Rei de quê?"), c.currentFootnotes)
        c.updateFootnote("1", "Rei de Portugal.")
        c.flushSave()
        val saved = services.books.loadManifest(c.book!!.path).chapters.single()
        assertEquals("Rei de Portugal.", saved.footnotes["1"])
        assertEquals(3, saved.words)
        c.removeFootnote("1")
        assertEquals("Era um rei", c.chapter!!.text)
    }

    @Test
    fun partsStayOutOfTheNumbering() {
        c.createBook("Livro", "")
        c.addChapter()
        c.addPart(ChapterKind.Prologo)
        c.addPart(ChapterKind.Epilogo)
        val chapters = c.book!!.manifest.chapters
        assertEquals(listOf(ChapterKind.Prologo, ChapterKind.Capitulo, ChapterKind.Capitulo, ChapterKind.Epilogo), chapters.map { it.kind })
        assertEquals(listOf(1, 2), chapters.mapNotNull { c.chapterNumbers[it.id] })
        c.addChapter()
        assertEquals("Capítulo 3", c.book!!.manifest.chapters.last().title)
    }

    @Test
    fun noteBlocksAndEntriesFromSelection() {
        c.createBook("Livro", "")
        c.addNoteBlock()
        val block = c.book!!.notes.blocks.single()
        c.updateNoteBlock(block.copy(title = "Ideias", body = "Mara tem um irmão."))
        assertEquals("Ideias", services.books.loadNotes(c.book!!.path).blocks.single().title)
        type("Mara chegou.")
        select(0, 4)
        c.createEntryFromSelection(EntryKind.Personagem)
        assertEquals(listOf("Mara"), c.book!!.universe.entries.map { it.name })
        c.createEntryFromSelection(EntryKind.Personagem) // repetida: não duplica
        assertEquals(1, c.book!!.universe.entries.size)
    }
}
