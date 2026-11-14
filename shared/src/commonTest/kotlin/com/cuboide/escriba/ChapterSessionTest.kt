package com.cuboide.escriba

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.cuboide.escriba.app.ChapterSaved
import com.cuboide.escriba.app.ChapterSession
import com.cuboide.escriba.app.SaveState
import com.cuboide.escriba.core.text.DailyWordMark
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ChapterSessionTest {
    private val scope = TestScope()
    private val mark = DailyWordMark { "2026-11-14" }
    private var smartTypography = true

    private var disk: String? = null
    private var failWrites = 0
    private val saves = mutableListOf<ChapterSaved>()

    private fun session(text: String = "") = ChapterSession(
        chapterId = "cap",
        text = text,
        scope = scope,
        now = { scope.testScheduler.currentTime },
        mark = mark,
        smartTypography = { smartTypography },
        write = { body ->
            if (failWrites > 0) {
                failWrites--
                throw IllegalStateException("disco cheio")
            }
            disk = body
        },
        onSaved = { saves += it },
    )

    private val writtenToday get() = saves.sumOf { it.written }

    @AfterTest
    fun tearDown() = scope.cancel()

    private fun ChapterSession.typeAt(text: String) = type(TextFieldValue(text, TextRange(text.length)))

    @Test
    fun typingRulesApplyAndUndoTakesBackOnlyTheCorrection() {
        val s = session()
        s.typeAt("a -")
        scope.testScheduler.advanceTimeBy(5_000)
        s.typeAt("a --")
        assertEquals("a —", s.text)
        s.undo()
        assertEquals("a --", s.text)
    }

    @Test
    fun aKeystrokeThatBeatsTheCorrectionToTheFieldKeepsIt() {
        val s = session()
        s.typeAt("e")
        assertEquals("E", s.text)
        // O campo manda "el" (sobre o "e" que ele conhecia) antes de receber o "E".
        s.typeAt("el")
        assertEquals("El", s.text)
    }

    @Test
    fun typingInABurstIsOneUndoStepAndAPauseStartsAnother() {
        smartTypography = false
        val s = session()
        s.typeAt("um")
        s.typeAt("um dois")
        scope.testScheduler.advanceTimeBy(ChapterSession.UNDO_MERGE_MS + 1)
        s.typeAt("um dois três")
        s.undo()
        assertEquals("um dois", s.text)
        s.undo()
        assertEquals("", s.text)
        s.redo()
        assertEquals("um dois", s.text)
        assertEquals(true, s.canRedo)
    }

    @Test
    fun anAppEditIsItsOwnUndoStepEvenRightAfterTyping() {
        val s = session()
        s.typeAt("Era uma vez")
        s.edit(TextFieldValue("Era *uma* vez", TextRange(4, 9)))
        s.undo()
        assertEquals("Era uma vez", s.text)
        s.undo()
        assertEquals("", s.text)
    }

    @Test
    fun movingTheSelectionIsNotAnEdit() {
        val s = session("Era uma vez")
        s.select(TextRange(4, 7))
        assertEquals(TextRange(4, 7), s.value.selection)
        assertEquals(false, s.canUndo)
        s.edit(s.value.copy(selection = TextRange(0))) // o app pedindo só outro cursor
        assertEquals(TextRange(0), s.value.selection)
        assertEquals(false, s.canUndo)
    }

    @Test
    fun theChapterIsSavedAfterAPauseInTyping() {
        val s = session()
        s.typeAt("Era uma vez")
        assertEquals(SaveState.Pendente, s.saveState)
        scope.testScheduler.advanceTimeBy(ChapterSession.AUTOSAVE_DELAY_MS - 1)
        s.typeAt("Era uma vez um")
        scope.testScheduler.advanceTimeBy(ChapterSession.AUTOSAVE_DELAY_MS - 1)
        assertEquals(null, disk) // cada tecla adia a gravação
        scope.testScheduler.advanceTimeBy(2)
        assertEquals("Era uma vez um", disk)
        assertEquals(SaveState.Salvo, s.saveState)
        assertEquals(4, saves.single().words)
    }

    @Test
    fun flushWritesPendingTextAtOnce() {
        val s = session()
        s.typeAt("texto digitado antes de fechar")
        assertEquals(true, s.flush())
        assertEquals("texto digitado antes de fechar", disk)
        assertEquals(SaveState.Salvo, s.saveState)
    }

    @Test
    fun undoAndRedoDoNotCountWordsTwice() {
        smartTypography = false
        val s = session()
        s.typeAt("um dois três quatro"); s.flush()
        s.undo(); s.flush()
        s.redo(); s.flush()
        assertEquals(4, writtenToday)
        s.typeAt("um dois três quatro cinco"); s.flush()
        assertEquals(5, writtenToday)
    }

    @Test
    fun onlyKeyboardEditsCountAsWrittenWords() {
        smartTypography = false
        val s = session("já existia")
        s.typeAt("já existia um dois três")
        // O Assistente insere cinco palavras antes da gravação: as três digitadas continuam contando.
        s.edit(TextFieldValue("já existia um dois três a b c d e"))
        s.flush()
        assertEquals(3, writtenToday)
        s.edit(TextFieldValue("já existia um dois três a b c d e f g")) // substituir tudo, inserir texto…
        s.flush()
        assertEquals(3, writtenToday)
    }

    @Test
    fun aFailedSaveKeepsTheTextPendingAndTriesAgain() {
        val s = session()
        failWrites = 1
        s.typeAt("não pode se perder")
        assertEquals(false, s.flush())
        assertEquals(SaveState.Erro, s.saveState)
        assertEquals(null, disk)
        assertEquals(true, s.flush())
        assertEquals("não pode se perder", disk)
        assertEquals(SaveState.Salvo, s.saveState)
    }

    @Test
    fun aFailedAutosaveIsRetriedOnTheNextEdit() {
        val s = session()
        failWrites = 1
        s.typeAt("um")
        scope.testScheduler.advanceTimeBy(ChapterSession.AUTOSAVE_DELAY_MS + 1)
        assertEquals(SaveState.Erro, s.saveState)
        s.typeAt("um dois")
        scope.testScheduler.advanceTimeBy(ChapterSession.AUTOSAVE_DELAY_MS + 1)
        assertEquals("um dois", disk)
        assertEquals(SaveState.Salvo, s.saveState)
    }

    @Test
    fun activeTimeGoesOutWithTheSave() {
        val s = session()
        for (text in listOf("a", "a b", "a b c", "a b c d")) {
            s.typeAt(text)
            scope.testScheduler.advanceTimeBy(700) // menos que a espera do autosave
        }
        s.flush()
        assertEquals(2, saves.single().activeSeconds) // 3 × 700 ms
    }
}
