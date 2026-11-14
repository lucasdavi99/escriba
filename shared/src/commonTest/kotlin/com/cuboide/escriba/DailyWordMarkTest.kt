package com.cuboide.escriba

import com.cuboide.escriba.core.text.DailyWordMark
import kotlin.test.Test
import kotlin.test.assertEquals

class DailyWordMarkTest {
    private var day = "2026-10-17"
    private val mark = DailyWordMark { day }

    @Test
    fun keyboardWordsAboveTheMarkCountOnce() {
        assertEquals(4, mark.wrote("cap", from = 0, to = 4))
        assertEquals(0, mark.wrote("cap", from = 4, to = 0))  // apagou
        assertEquals(0, mark.wrote("cap", from = 0, to = 4))  // e reescreveu o mesmo tanto
        assertEquals(1, mark.wrote("cap", from = 4, to = 5))
    }

    @Test
    fun aNewDayStartsTheMarkAtTheTextThatAlreadyExisted() {
        mark.wrote("cap", from = 0, to = 6)
        day = "2026-10-18"
        assertEquals(0, mark.wrote("cap", from = 6, to = 3)) // apagou texto de ontem
        assertEquals(0, mark.wrote("cap", from = 3, to = 6)) // e reescreveu a mesma quantidade
        assertEquals(2, mark.wrote("cap", from = 6, to = 8))
    }

    @Test
    fun anAppEditRaisesTheMarkWithoutCounting() {
        assertEquals(3, mark.wrote("cap", from = 0, to = 3))
        mark.appEdited("cap", from = 3, to = 103) // o Assistente inseriu cem palavras
        assertEquals(3, mark.wrote("cap", from = 103, to = 106))
    }

    @Test
    fun restoringOlderTextDoesNotLowerTheMark() {
        mark.wrote("cap", from = 0, to = 6)
        mark.appEdited("cap", from = 1, to = 6) // restaurou a versão de seis palavras
        assertEquals(0, mark.wrote("cap", from = 6, to = 6))
        assertEquals(1, mark.wrote("cap", from = 6, to = 7))
    }

    @Test
    fun activeTimeAddsShortGapsBetweenEditsAndKeepsTheRemainder() {
        mark.activity(at = 0)
        mark.activity(at = 10_000)
        mark.activity(at = 100_000) // pausa longa: conta só 30 s
        assertEquals(40, mark.takeActiveSeconds())
        mark.activity(at = 101_500)
        assertEquals(1, mark.takeActiveSeconds())
        mark.activity(at = 103_000) // os 500 ms que sobraram entram aqui
        assertEquals(2, mark.takeActiveSeconds())
    }

    @Test
    fun eachChapterHasItsOwnMark() {
        assertEquals(5, mark.wrote("um", from = 0, to = 5))
        assertEquals(5, mark.wrote("outro", from = 0, to = 5))
    }
}
