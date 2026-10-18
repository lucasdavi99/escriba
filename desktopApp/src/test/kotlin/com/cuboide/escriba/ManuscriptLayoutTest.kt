package com.cuboide.escriba

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.cuboide.escriba.ui.ManuscriptStyling
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Mede o manuscrito como o editor desenha (com a mesma transformação visual) para conferir o layout. */
class ManuscriptLayoutTest {
    private val fontSize = 20f
    private val lineHeight = 1.7f

    private fun styling(cursor: Int = 0, spacerSp: Float = 0f) = ManuscriptStyling(
        selection = TextRange(cursor), matches = emptyList(), currentMatch = -1,
        ink = Color.Black, accent = Color.Red, matchColor = Color.Yellow, currentMatchColor = Color.Yellow,
        align = TextAlign.Start, indentSp = 30f, lineHeightEm = lineHeight, focusMode = false, spacerSp = spacerSp,
    )

    private fun layout(text: String, cursor: Int = 0, spacerSp: Float = 0f, width: Int = 4000): MultiParagraph {
        return MultiParagraph(
            annotatedString = styling(cursor, spacerSp).filter(AnnotatedString(text)).text,
            style = TextStyle(fontSize = fontSize.sp, lineHeight = lineHeight.em),
            constraints = Constraints(maxWidth = width),
            density = Density(1f),
            fontFamilyResolver = createFontFamilyResolver(),
        )
    }

    @Test
    fun eachParagraphIsOneLineWithIndentAndLineSpacing() {
        val p = layout("Primeiro parágrafo\nSegundo parágrafo\n\nQuarto")
        // Sem linhas em branco a mais entre os parágrafos (só a linha vazia que existe no texto).
        assertEquals(4, p.lineCount)
        assertEquals(30f, p.getLineLeft(0))
        assertEquals(30f, p.getLineLeft(1))
        val spacing = p.getLineTop(1) - p.getLineTop(0)
        assertTrue(abs(spacing - fontSize * lineHeight) < 1.5f, "entrelinha $spacing")
    }

    @Test
    fun markersOutsideTheCursorTakeNoSpace() {
        val plain = layout("Era uma vez", cursor = 0)
        val marked = layout("Era *uma* vez", cursor = 0)
        val width = { p: MultiParagraph -> p.getLineRight(0) - p.getLineLeft(0) }
        assertTrue(abs(width(marked) - width(plain)) < 3f, "itálico ${width(marked)} × normal ${width(plain)}")
        // Com o cursor dentro do trecho, os asteriscos aparecem (apagados) e ocupam espaço.
        val editing = layout("Era *uma* vez", cursor = 6)
        assertTrue(width(editing) > width(plain) + 4f)
    }

    @Test
    fun centeredSceneBreakAndHeadingsKeepLineCount() {
        val p = layout("Texto\n* * *\n# Título\n* item\n> citação\n->fim<-", cursor = 0)
        assertEquals(6, p.lineCount)
        // Quebra de cena centralizada: começa bem depois da margem.
        assertTrue(p.getLineLeft(1) > 1000f)
    }

    @Test
    fun paragraphSpacingOnlyBetweenParagraphs() {
        val long = "palavra ".repeat(40).trim()
        val text = "$long\nSegundo\n\nQuarto"
        val plain = layout(text, width = 600)
        val spaced = layout(text, spacerSp = 44f, width = 600)
        // Mesmas linhas: o espaçador não cria linha nova.
        assertEquals(plain.lineCount, spaced.lineCount)
        val wrapped = (0 until plain.lineCount).count { plain.getLineStart(it) < long.length } // linhas do 1º parágrafo
        assertTrue(wrapped >= 2)
        // Dentro do parágrafo, a entrelinha não muda.
        assertEquals(plain.getLineBaseline(1) - plain.getLineBaseline(0), spaced.getLineBaseline(1) - spaced.getLineBaseline(0))
        // Entre parágrafos, o texto do parágrafo seguinte desce.
        val gapPlain = plain.getLineBaseline(wrapped) - plain.getLineBaseline(wrapped - 1)
        val gapSpaced = spaced.getLineBaseline(wrapped) - spaced.getLineBaseline(wrapped - 1)
        assertTrue(gapSpaced > gapPlain + 5f, "entre parágrafos $gapPlain → $gapSpaced")
    }

    @Test
    fun endKeyStaysOnTheLine() {
        val first = "Primeiro parágrafo"
        for (spacer in listOf(0f, 40f)) {
            val text = "$first\nSegundo"
            val transformed = styling(spacerSp = spacer).filter(AnnotatedString(text))
            val p = MultiParagraph(
                transformed.text, TextStyle(fontSize = fontSize.sp, lineHeight = lineHeight.em),
                Constraints(maxWidth = 4000), Density(1f), createFontFamilyResolver(),
            )
            // End usa o fim visível da linha (sem o espaço que fecha o parágrafo).
            assertEquals(first.length, transformed.offsetMapping.transformedToOriginal(p.getLineEnd(0, visibleEnd = true)), "End (espaçador $spacer)")
        }
    }

    @Test
    fun rawOffsetInvertsDisplayOffset() {
        val text = "Era **um** rei\n\n# Título\n* item\nfim"
        val s = styling(spacerSp = 40f)
        val mapping = s.filter(AnnotatedString(text)).offsetMapping
        for (raw in 0..text.length) assertEquals(raw, s.rawOffset(text, s.displayOffset(text, raw)))
        for (t in 0..text.length + text.count { it == '\n' }) assertEquals(mapping.transformedToOriginal(t), s.rawOffset(text, t), "posição $t")
    }

    @Test
    fun spacerMappingRoundTrips() {
        val text = "Era **um** rei\n\n# Título\n* item\nfim"
        val mapping = styling(spacerSp = 40f).filter(AnnotatedString(text)).offsetMapping
        for (raw in 0..text.length) assertEquals(raw, mapping.transformedToOriginal(mapping.originalToTransformed(raw)))
        val displayLength = text.length + text.count { it == '\n' }
        for (t in 0..displayLength) assertTrue(mapping.transformedToOriginal(t) in 0..text.length)
        assertEquals(displayLength, mapping.originalToTransformed(text.length))
        assertEquals(styling(spacerSp = 40f).displayOffset(text, 16), mapping.originalToTransformed(16))
    }
}
