package com.cuboide.escriba

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.cuboide.escriba.app.ChapterSession
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

    private fun session(text: String = "") = ChapterSession(
        chapterId = "cap",
        text = text,
        scope = scope,
        now = { scope.testScheduler.currentTime },
        mark = mark,
        smartTypography = { smartTypography },
    )

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
}
