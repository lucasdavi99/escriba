package com.cuboide.escriba

import com.cuboide.escriba.core.export.ExportChapter
import com.cuboide.escriba.core.export.ExportDocument
import com.cuboide.escriba.core.export.InlineSpan
import com.cuboide.escriba.core.export.exportHtml
import com.cuboide.escriba.core.export.exportMarkdown
import com.cuboide.escriba.core.export.exportPlainText
import com.cuboide.escriba.core.export.isSceneBreak
import com.cuboide.escriba.core.export.parseInline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExportTest {
    private val doc = ExportDocument(
        title = "Livro",
        author = "Ana",
        synopsis = "",
        chapters = listOf(ExportChapter("Um", listOf("Antes.", "* * *", "Depois."))),
    )

    @Test
    fun recognizesSceneBreaks() {
        assertTrue(isSceneBreak("* * *"))
        assertTrue(isSceneBreak("  ***  "))
        assertTrue(isSceneBreak("#"))
        assertFalse(isSceneBreak("* *"))
        assertFalse(isSceneBreak("*Nota* final"))
    }

    @Test
    fun sceneBreakSurvivesHtmlAndText() {
        assertTrue("<p class=\"cena\">* * *</p>" in exportHtml(doc))
        assertTrue("<em>" !in exportHtml(doc))
        assertTrue("Antes.\n\n* * *\n\nDepois." in exportPlainText(doc))
    }

    @Test
    fun loneMarkersStayLiteral() {
        assertEquals(listOf(InlineSpan("2 * 3 * 4")), parseInline("2 * 3 * 4"))
        assertEquals(listOf(InlineSpan("arquivo_de_nome.txt")), parseInline("arquivo_de_nome.txt"))
        assertEquals(listOf(InlineSpan("snake_case e _ solto_")), parseInline("snake_case e _ solto_"))
    }

    @Test
    fun markupStillWorks() {
        assertEquals(
            listOf(InlineSpan("Um "), InlineSpan("a", italic = true), InlineSpan(", "), InlineSpan("dois", italic = true), InlineSpan(" e "), InlineSpan("três palavras", bold = true)),
            parseInline("Um *a*, _dois_ e **três palavras**"),
        )
    }

    private val rich = ExportDocument(
        title = "Livro",
        author = "",
        synopsis = "",
        chapters = listOf(
            ExportChapter(
                "Um",
                listOf("### Parte um", "# A chegada", "Era [Dom]{.smallcaps} e H~2~O^2^ nota[^7].", "* um", "* dois", "> citação", "->Fim<-", "![Mapa](assets/m.png)"),
                footnotes = mapOf("7" to "Texto da nota."),
            ),
        ),
        images = mapOf("assets/m.png" to byteArrayOf(1, 2, 3)),
    )

    @Test
    fun spansCarryEveryMark() {
        assertEquals(
            listOf(
                InlineSpan("Era "), InlineSpan("Dom", smallCaps = true), InlineSpan(" e H"), InlineSpan("2", subscript = true),
                InlineSpan("O"), InlineSpan("2", superscript = true), InlineSpan(" nota"), InlineSpan("", footnote = "7"), InlineSpan("."),
            ),
            parseInline("Era [Dom]{.smallcaps} e H~2~O^2^ nota[^7]."),
        )
        assertEquals(listOf(InlineSpan("a "), InlineSpan("b", bold = true, italic = true)), parseInline("a ***b***"))
    }

    @Test
    fun htmlHasBlocksNotesAndEmbeddedImage() {
        val html = exportHtml(rich)
        assertTrue("<p class=\"chamada\">Parte um</p>" in html)
        assertTrue("<h3>A chegada</h3>" in html)
        assertTrue("<span class=\"versalete\">Dom</span>" in html)
        assertTrue("<sub>2</sub>" in html && "<sup>2</sup>" in html)
        assertTrue("<ul>\n<li>um</li>\n<li>dois</li>\n</ul>" in html)
        assertTrue("<blockquote><p>citação</p></blockquote>" in html)
        assertTrue("<p class=\"centro\">Fim</p>" in html)
        assertTrue("<sup id=\"ref-1-1\"><a href=\"#nota-1-1\">1</a></sup>" in html)
        assertTrue("<li id=\"nota-1-1\">Texto da nota." in html)
        assertTrue("src=\"data:image/png;base64,AQID\"" in html)
        assertTrue("{.smallcaps}" !in html && "[^7]" !in html && "->" !in html)
    }

    @Test
    fun textAndMarkdownKeepNotes() {
        val txt = exportPlainText(rich)
        assertTrue("PARTE UM" in txt && "A CHEGADA" in txt)
        assertTrue("Era Dom e H2O2 nota[1]." in txt)
        assertTrue("• um\n\n• dois" in txt)
        assertTrue("[1] Texto da nota." in txt)
        assertTrue("[Imagem: Mapa]" in txt)
        val md = exportMarkdown(rich)
        assertTrue("##### Parte um" in md && "### A chegada" in md)
        assertTrue("nota[^c1-7]." in md && "[^c1-7]: Texto da nota." in md)
        assertTrue("* um\n* dois" in md)
        assertTrue("\nFim\n" in md)
    }
}
