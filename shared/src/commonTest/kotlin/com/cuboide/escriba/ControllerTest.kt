package com.cuboide.escriba

import androidx.compose.ui.text.input.TextFieldValue
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.EscribaServices
import com.cuboide.escriba.app.SaveState
import androidx.compose.ui.text.TextRange
import com.cuboide.escriba.core.model.ChapterKind
import com.cuboide.escriba.core.model.EntryKind
import com.cuboide.escriba.core.text.BlockKind
import com.cuboide.escriba.core.text.MarkKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
        c.onEditorChange(TextFieldValue(text))
        c.flushSave()
    }

    private fun chapterOnDisk(): String {
        val book = c.book!!
        return services.books.readChapter(book.path, c.currentChapterId!!)
    }

    @Test
    fun flushSaveWritesPendingTextAtOnce() {
        c.createBook("Livro", "")
        c.onEditorChange(TextFieldValue("texto digitado antes de fechar"))
        assertEquals(SaveState.Pendente, c.saveState)
        assertEquals("", chapterOnDisk())

        c.flushSave() // o que a janela chama ao fechar

        assertEquals(SaveState.Salvo, c.saveState)
        assertEquals("texto digitado antes de fechar", chapterOnDisk())
    }

    @Test
    fun undoAndRedoDoNotCountWordsTwice() {
        c.createBook("Livro", "")
        type("um dois três quatro")
        type("")                     // desfazer
        type("um dois três quatro")  // refazer
        assertEquals(4, c.productivity.today.words)

        type("um dois três quatro cinco")
        assertEquals(5, c.productivity.today.words)
    }

    @Test
    fun deletingOldTextAndWritingCountsOnlyNewWords() {
        c.createBook("Livro", "")
        type("a b c d e f")
        day = "2026-10-18"
        type("a b c")       // apagou texto de ontem
        type("a b c d e f") // e reescreveu a mesma quantidade
        assertEquals(0, c.productivity.today.words)
        type("a b c d e f g h")
        assertEquals(2, c.productivity.today.words)
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
    fun openFindAsksForKeyboardFocusEveryTime() {
        c.createBook("Livro", "")
        val before = c.findFocusRequests
        c.openFind()
        c.openFind() // Ctrl+F com a barra já aberta volta o foco para "Localizar"
        assertEquals(before + 2, c.findFocusRequests)
    }

    private fun select(start: Int, end: Int) {
        c.onEditorChange(c.editor.copy(selection = TextRange(start, end)))
    }

    @Test
    fun formattingIsUndoableAndRedoable() {
        c.createBook("Livro", "")
        type("Era uma vez")
        select(4, 7)
        c.toggleMark(MarkKind.Italic)
        assertEquals("Era *uma* vez", c.editor.text)
        c.toggleBlockStyle(BlockKind.Heading1)
        assertEquals("# Era *uma* vez", c.editor.text)
        c.undo()
        assertEquals("Era *uma* vez", c.editor.text)
        c.undo()
        assertEquals("Era uma vez", c.editor.text)
        c.redo()
        assertEquals("Era *uma* vez", c.editor.text)
        // Formatação não conta como palavras novas.
        assertEquals(3, c.currentWords)
    }

    @Test
    fun typingRulesAndUndoOfTheCorrection() {
        c.createBook("Livro", "")
        c.onEditorChange(TextFieldValue("a -", TextRange(3)))
        scope.testScheduler.advanceTimeBy(5_000)
        c.onEditorChange(TextFieldValue("a --", TextRange(4)))
        assertEquals("a —", c.editor.text)
        c.undo() // desfaz só a correção
        assertEquals("a --", c.editor.text)
    }

    @Test
    fun fastKeystrokeAfterCorrectionKeepsIt() {
        c.createBook("Livro", "")
        c.onEditorChange(TextFieldValue("e", TextRange(1)))
        assertEquals("E", c.editor.text)
        // O campo manda "el" (sobre o "e" que ele conhecia) antes de receber o "E".
        c.onEditorChange(TextFieldValue("el", TextRange(2)))
        assertEquals("El", c.editor.text)
    }

    @Test
    fun footnotesLiveInTheChapter() {
        c.createBook("Livro", "")
        type("Era um rei")
        select(7, 10)
        c.addFootnote("Rei de quê?")
        assertEquals("Era um rei[^1]", c.editor.text)
        assertEquals(listOf("1" to "Rei de quê?"), c.currentFootnotes)
        c.updateFootnote("1", "Rei de Portugal.")
        c.flushSave()
        val saved = services.books.loadManifest(c.book!!.path).chapters.single()
        assertEquals("Rei de Portugal.", saved.footnotes["1"])
        assertEquals(3, saved.words)
        c.removeFootnote("1")
        assertEquals("Era um rei", c.editor.text)
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
