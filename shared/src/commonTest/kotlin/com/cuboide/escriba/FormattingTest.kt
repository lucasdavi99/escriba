package com.cuboide.escriba

import com.cuboide.escriba.core.text.BlockKind
import com.cuboide.escriba.core.text.Edit
import com.cuboide.escriba.core.text.MarkKind
import com.cuboide.escriba.core.text.applyTypingRules
import com.cuboide.escriba.core.text.clearFormatting
import com.cuboide.escriba.core.text.insertFootnoteRef
import com.cuboide.escriba.core.text.insertStandaloneLine
import com.cuboide.escriba.core.text.nextFootnoteId
import com.cuboide.escriba.core.text.rebaseFieldEdit
import com.cuboide.escriba.core.text.toggleBlock
import com.cuboide.escriba.core.text.toggleCenter
import com.cuboide.escriba.core.text.toggleInline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FormattingTest {
    @Test
    fun wrapsAndUnwrapsSelection() {
        val wrapped = toggleInline("Era uma vez", 4, 7, MarkKind.Italic)
        assertEquals(Edit("Era *uma* vez", 5, 8), wrapped)
        // De novo com a mesma seleção: tira.
        assertEquals(Edit("Era uma vez", 4, 7), toggleInline(wrapped.text, wrapped.selStart, wrapped.selEnd, MarkKind.Italic))
    }

    @Test
    fun selectionSpacesStayOutside() {
        assertEquals("Era **uma** vez", toggleInline("Era uma vez", 3, 8, MarkKind.Bold).text)
    }

    @Test
    fun italicInsideBoldAndBack() {
        val bold = toggleInline("um rei", 3, 6, MarkKind.Bold).text // um **rei**
        val both = toggleInline(bold, 5, 8, MarkKind.Italic)
        assertEquals("um ***rei***", both.text)
        assertEquals("um **rei**", toggleInline(both.text, both.selStart, both.selEnd, MarkKind.Italic).text)
    }

    @Test
    fun cursorWithoutSelection() {
        // Insere o par e põe o cursor no meio; de novo, desfaz o par vazio.
        val pair = toggleInline("ab", 1, 1, MarkKind.Bold)
        assertEquals(Edit("a****b", 3), pair)
        assertEquals(Edit("ab", 1), toggleInline(pair.text, 3, 3, MarkKind.Bold))
        // Dentro de uma marca, pula para depois dela (para parar de escrever em itálico).
        assertEquals(Edit("um *rei* bom", 8), toggleInline("um *rei* bom", 7, 7, MarkKind.Italic))
    }

    @Test
    fun otherMarks() {
        assertEquals("[Dom]{.smallcaps}", toggleInline("Dom", 0, 3, MarkKind.SmallCaps).text)
        assertEquals("x^2^", toggleInline("x2", 1, 2, MarkKind.Superscript).text)
        assertEquals("H~2~O", toggleInline("H2O", 1, 2, MarkKind.Subscript).text)
    }

    @Test
    fun multiLineSelectionIsWrappedPerLine() {
        assertEquals("*um*\n\n*dois*", toggleInline("um\n\ndois", 0, 8, MarkKind.Italic).text)
    }

    @Test
    fun blocksToggleAndReplaceEachOther() {
        val h1 = toggleBlock("Capítulo\ntexto", 2, 2, BlockKind.Heading1)
        assertEquals("# Capítulo\ntexto", h1.text)
        assertEquals(4, h1.selStart) // cursor acompanha o conteúdo
        assertEquals("## Capítulo\ntexto", toggleBlock(h1.text, 4, 4, BlockKind.Heading2).text)
        assertEquals("Capítulo\ntexto", toggleBlock(h1.text, 4, 4, BlockKind.Heading1).text)
        assertEquals("* a\n* b", toggleBlock("a\nb", 0, 3, BlockKind.ListItem).text)
        assertEquals("a\nb", toggleBlock("* a\n* b", 0, 7, BlockKind.ListItem).text)
        // Quebra de cena não vira lista.
        assertEquals("* * *", toggleBlock("* * *", 0, 0, BlockKind.ListItem).text)
    }

    @Test
    fun centerAndClear() {
        assertEquals("->Fim<-", toggleCenter("Fim", 1, 1).text)
        assertEquals("Fim", toggleCenter("->Fim<-", 3, 3).text)
        assertEquals("Era um rei[^1] bom", clearFormatting("> Era **um** [rei]{.smallcaps}[^1] bom", 5, 5).text)
    }

    @Test
    fun standaloneLinesAndFootnotes() {
        assertEquals(Edit("ab\n* * *\ncd", 9), insertStandaloneLine("abcd", 2, 2, "* * *"))
        assertEquals(Edit("ab\n* * *\ncd", 9), insertStandaloneLine("ab\ncd", 3, 3, "* * *"))
        assertEquals(Edit("um rei[^1] bom", 10), insertFootnoteRef("um rei bom", 3, 7, "1"))
        assertEquals("4", nextFootnoteId("a[^1] b[^3]", listOf("2")))
    }

    @Test
    fun enterContinuesAndEndsLists() {
        assertEquals(Edit("* um\n* ", 7), applyTypingRules("* um", "* um\n", 5, smartTypography = false))
        assertEquals(Edit("* um\n", 5), applyTypingRules("* um\n* ", "* um\n* \n", 8, smartTypography = false))
        assertEquals(Edit("> a\n> ", 6), applyTypingRules("> a", "> a\n", 4, smartTypography = false))
        assertNull(applyTypingRules("texto", "texto\n", 6, smartTypography = true))
    }

    @Test
    fun smartTypography() {
        fun type(before: String, ch: Char): String {
            val after = before + ch
            return applyTypingRules(before, after, after.length, smartTypography = true)?.text ?: after
        }
        assertEquals("a —", type("a -", '-'))
        assertEquals("sim…", type("sim..", '.'))
        assertEquals("ele disse “", type("ele disse ", '"'))
        assertEquals("“ok”", type("“ok", '"'))
        assertEquals("d’", type("d", '\''))
        assertEquals("texto\nE", type("texto\n", 'e'))
        assertEquals("* I", type("* ", 'i'))
        // Inserir no começo de um parágrafo que já tem texto não muda a letra.
        assertNull(applyTypingRules("abc", "xabc", 1, smartTypography = true))
        // Sem a opção, nada muda.
        assertNull(applyTypingRules("a -", "a --", 4, smartTypography = false))
    }

    @Test
    fun rebasesFastKeystrokeOntoCorrectedText() {
        // O campo ainda acha que o texto é "a --" (antes de virar travessão) e manda o espaço seguinte.
        assertEquals("a — " to -1, rebaseFieldEdit("a --", "a -- ", "a —", baseCursor = 4))
        assertEquals("texto\nEl" to 0, rebaseFieldEdit("texto\ne", "texto\nel", "texto\nE", baseCursor = 7))
        // Mudança antes do ponto corrigido não dá para rebasear.
        assertNull(rebaseFieldEdit("a --", "b --", "a —", baseCursor = 4))
    }
}
