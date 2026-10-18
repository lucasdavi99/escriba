package com.cuboide.escriba

import com.cuboide.escriba.core.text.BlockKind
import com.cuboide.escriba.core.text.MarkKind
import com.cuboide.escriba.core.text.countManuscriptWords
import com.cuboide.escriba.core.text.findInlineMarks
import com.cuboide.escriba.core.text.footnoteIds
import com.cuboide.escriba.core.text.parseLine
import com.cuboide.escriba.core.text.stripMarkup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkupTest {
    @Test
    fun classifiesParagraphs() {
        assertEquals(BlockKind.Heading1, parseLine("# Título").kind)
        assertEquals(BlockKind.Heading2, parseLine("## Sub").kind)
        assertEquals(BlockKind.Heading3, parseLine("### Chamada").kind)
        assertEquals(BlockKind.ListItem, parseLine("* item").kind)
        assertEquals(BlockKind.Quote, parseLine("> citação").kind)
        assertEquals(BlockKind.SceneBreak, parseLine("* * *").kind)
        assertEquals(BlockKind.SceneBreak, parseLine("#").kind)
        // Travessão de diálogo com hífen não vira lista.
        assertEquals(BlockKind.Paragraph, parseLine("- Olá, disse ela.").kind)
        val centered = parseLine("->Fim<-")
        assertTrue(centered.centered)
        assertEquals("Fim", "->Fim<-".substring(centered.contentStart, centered.contentEnd))
        val image = parseLine("![Mapa do reino](assets/mapa.png)")
        assertEquals(BlockKind.Image, image.kind)
        assertEquals("Mapa do reino", image.imageCaption)
        assertEquals("assets/mapa.png", image.imagePath)
    }

    @Test
    fun findsNestedAndSpecialMarks() {
        val text = "Era **um *rei* bom**, [Dom Pedro]{.smallcaps}, H~2~O, 2^10^ e nota[^1]."
        val kinds = findInlineMarks(text).map { it.kind to text.substring(it.contentStart, it.contentEnd) }
        assertTrue(MarkKind.Bold to "um *rei* bom" in kinds)
        assertTrue(MarkKind.Italic to "rei" in kinds)
        assertTrue(MarkKind.SmallCaps to "Dom Pedro" in kinds)
        assertTrue(MarkKind.Subscript to "2" in kinds)
        assertTrue(MarkKind.Superscript to "10" in kinds)
        assertTrue(MarkKind.Footnote to "1" in kinds)
    }

    @Test
    fun boldItalicTogether() {
        val marks = findInlineMarks("***forte e leve***")
        assertEquals(setOf(MarkKind.Bold, MarkKind.Italic), marks.map { it.kind }.toSet())
    }

    @Test
    fun stripsMarkupForCounting() {
        val body = "# Título\n**Negrito** e [vers]{.smallcaps}[^1]\n* * *\n![Legenda da foto](assets/x.png)\n->Fim<-"
        assertEquals("Título\nNegrito e vers\n\nLegenda da foto\nFim", stripMarkup(body))
        assertEquals(8, countManuscriptWords(body))
    }

    @Test
    fun footnotesInOrderOfAppearance() {
        assertEquals(listOf("3", "1"), footnoteIds("a[^3] b[^1] c[^3]"))
    }
}
