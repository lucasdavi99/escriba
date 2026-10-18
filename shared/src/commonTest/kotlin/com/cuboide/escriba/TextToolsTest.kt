package com.cuboide.escriba

import com.cuboide.escriba.core.export.InlineSpan
import com.cuboide.escriba.core.export.parseInline
import com.cuboide.escriba.core.text.DayKey
import com.cuboide.escriba.core.text.DiffKind
import com.cuboide.escriba.core.text.FindOptions
import com.cuboide.escriba.core.text.countWords
import com.cuboide.escriba.core.text.diffStats
import com.cuboide.escriba.core.text.diffWords
import com.cuboide.escriba.core.text.findAll
import com.cuboide.escriba.core.text.replaceAll
import com.cuboide.escriba.core.text.searchBook
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextToolsTest {
    @Test
    fun countsPortugueseWords() {
        assertEquals(0, countWords("   \n — … "))
        assertEquals(5, countWords("O guarda-chuva da Conceição — molhado!"))
        assertEquals(3, countWords("copo d'água 2024"))
        assertEquals(2, countWords("fim -- começo"))
    }

    @Test
    fun findRespectsOptions() {
        val text = "Ana viu ana. Banana não conta."
        assertEquals(3, findAll(text, "ana").size)
        assertEquals(2, findAll(text, "ana", FindOptions(wholeWord = true)).size)
        assertEquals(1, findAll(text, "ana", FindOptions(matchCase = true, wholeWord = true)).size)
        assertEquals(emptyList(), findAll(text, "(", FindOptions(regex = true)))
    }

    @Test
    fun replaceAllLiteralAndRegex() {
        val r1 = replaceAll("a.b a.b", "a.b", "x")
        assertEquals("x x", r1.text)
        assertEquals(2, r1.count)
        val r2 = replaceAll("João Silva, Maria Souza", "(\\p{L}+) (\\p{L}+)", "$2, $1", FindOptions(regex = true))
        assertEquals("Silva, João, Souza, Maria", r2.text)
        // Fora do modo regex, "$1" é literal.
        assertEquals("$1", replaceAll("x", "x", "$1").text)
    }

    @Test
    fun diffMarksInsertionsAndDeletions() {
        val segments = diffWords("O gato preto dormiu.", "O gato branco dormiu. Muito.")
        assertEquals("O gato branco dormiu. Muito.", segments.filter { it.kind != DiffKind.Deleted }.joinToString("") { it.text })
        assertEquals("O gato preto dormiu.", segments.filter { it.kind != DiffKind.Inserted }.joinToString("") { it.text })
        val stats = diffStats(segments)
        assertEquals(1, stats.deletedWords)
        assertTrue(stats.insertedWords >= 2)
    }

    @Test
    fun diffFallsBackForHugeTexts() {
        val a = (1..3000).joinToString(" ") { "a$it" }
        val b = a.replace("a1500 ", "b1500 ")
        val segments = diffWords(a, b, maxCells = 10_000)
        assertEquals(b, segments.filter { it.kind != DiffKind.Deleted }.joinToString("") { it.text })
    }

    @Test
    fun dayKeyArithmetic() {
        assertEquals(0, DayKey.toEpochDay("1970-01-01"))
        assertEquals("2024-03-01", DayKey.plusDays("2024-02-28", 2)) // ano bissexto
        assertEquals("2025-01-01", DayKey.plusDays("2024-12-31", 1))
        assertEquals(366, DayKey.daysBetween("2024-01-01", "2025-01-01"))
        for (d in listOf(-1000L, 0L, 19_000L, 50_000L)) assertEquals(d, DayKey.toEpochDay(DayKey.fromEpochDay(d)))
    }

    @Test
    fun searchBookReturnsSnippets() {
        val hits = searchBook(
            listOf(Triple("c1", "Um", "Era uma vez um dragão."), Triple("c2", "Dois", "O dragão voou.")),
            "dragão",
        )
        assertEquals(listOf("c1", "c2"), hits.map { it.chapterId })
        val h = hits.first()
        assertEquals("dragão", h.snippet.substring(h.snippetMatchStart, h.snippetMatchEnd))
    }

    @Test
    fun parsesInlineMarks() {
        assertEquals(
            listOf(InlineSpan("Um "), InlineSpan("forte", bold = true), InlineSpan(" e "), InlineSpan("leve", italic = true)),
            parseInline("Um **forte** e *leve*"),
        )
    }
}
