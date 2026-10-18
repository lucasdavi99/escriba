package com.cuboide.escriba.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.cuboide.escriba.core.text.BlockKind
import com.cuboide.escriba.core.text.MarkKind
import com.cuboide.escriba.core.text.TextMatch
import com.cuboide.escriba.core.text.findInlineMarks
import com.cuboide.escriba.core.text.parseLines

/**
 * Mostra o manuscrito formatado sem mudar o texto guardado: negrito, itálico, títulos, listas, citações,
 * versalete, notas e quebras de cena aparecem com o estilo, e os marcadores (`**`, `# `, `[^1]`…) ficam
 * invisíveis. Quando o cursor entra num trecho marcado, os marcadores dele reaparecem em tom apagado,
 * para que dê para editá-los.
 *
 * Com [spacerSp] > 0, cada parágrafo (a partir do segundo) ganha na tela um caractere invisível e alto no
 * começo, que empurra o parágrafo para baixo: é o espaço entre parágrafos. O Compose não tem esse ajuste
 * pronto, e esse caractere não existe no texto guardado; o mapa de posições pula ele. Sem espaçamento,
 * o mapa é identidade: cada caractere continua no lugar.
 */
data class ManuscriptStyling(
    val selection: TextRange,
    val matches: List<TextMatch>,
    val currentMatch: Int,
    val ink: Color,
    val accent: Color,
    val matchColor: Color,
    val currentMatchColor: Color,
    val align: TextAlign,
    /** Recuo de primeira linha já em sp (0 = sem recuo). */
    val indentSp: Float,
    /** Entrelinha em em; vai em cada parágrafo, porque o estilo de parágrafo substitui a do TextStyle. */
    val lineHeightEm: Float,
    val focusMode: Boolean,
    /** Tamanho (sp) do caractere espaçador no começo de cada parágrafo; 0 = parágrafos colados. */
    val spacerSp: Float = 0f,
) : VisualTransformation {

    /** Posição no texto guardado de uma posição da tela (o inverso de [displayOffset]). */
    fun rawOffset(raw: String, display: Int): Int {
        if (spacerSp <= 0f) return display.coerceIn(0, raw.length)
        var line = 0
        var start = 0
        while (true) {
            val newline = raw.indexOf('\n', start)
            val end = if (newline < 0) raw.length else newline
            val shownEnd = end + line
            if (display <= shownEnd || newline < 0) return (display - line).coerceIn(start, end)
            // Logo depois do fim da linha vem o espaçador do parágrafo seguinte: conta como o começo dele.
            if (display == shownEnd + 1) return newline + 1
            start = newline + 1
            line++
        }
    }

    /** Posição na tela de uma posição do texto guardado (difere só quando há espaçamento entre parágrafos). */
    fun displayOffset(raw: String, offset: Int): Int =
        if (spacerSp <= 0f) offset else offset + raw.subSequence(0, offset.coerceIn(0, raw.length)).count { it == '\n' }

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val display = raw.toCharArray()
        val spans = ArrayList<AnnotatedString.Range<SpanStyle>>()
        val paragraphs = ArrayList<AnnotatedString.Range<ParagraphStyle>>()
        val selMin = selection.min
        val selMax = selection.max
        val marker = SpanStyle(color = ink.copy(alpha = 0.35f))
        fun span(style: SpanStyle, from: Int, to: Int) {
            if (to > from) spans += AnnotatedString.Range(style, from, to)
        }
        fun touches(from: Int, to: Int) = selMin <= to && selMax >= from
        fun hideOrDim(from: Int, to: Int, active: Boolean) = span(if (active) marker else HIDDEN, from, to)

        val lines = parseLines(raw)
        for ((index, line) in lines.withIndex()) {
            val ls = line.start
            val le = line.end
            val b = line.block
            val active = touches(ls, le)
            val cs = ls + b.contentStart
            val ce = ls + b.contentEnd
            // Cada estilo de parágrafo já quebra a linha; o '\n' do texto entra no parágrafo como um espaço
            // invisível (senão apareceria uma linha em branco a mais). Tem que ser espaço de verdade: o End e o
            // clique depois do fim da linha param antes de espaços finais, e o cursor fica nesta linha.
            val paragraphEnd = if (index < lines.lastIndex) le + 1 else le
            if (paragraphEnd > le) display[le] = ' '
            val paragraph = when (b.kind) {
                BlockKind.Paragraph -> ParagraphStyle(
                    textAlign = if (b.centered) TextAlign.Center else align,
                    textIndent = if (indentSp > 0f && !b.centered) TextIndent(firstLine = indentSp.sp) else TextIndent.None,
                    lineHeight = lineHeightEm.em,
                    lineHeightStyle = KEEP_LINE_SPACING,
                )
                BlockKind.Heading1, BlockKind.Heading2, BlockKind.Heading3 ->
                    ParagraphStyle(textAlign = TextAlign.Start, lineHeight = (lineHeightEm * 1.25f).em, lineHeightStyle = KEEP_LINE_SPACING)
                BlockKind.ListItem -> ParagraphStyle(textIndent = TextIndent(firstLine = indentSp.sp, restLine = (indentSp + 16f).sp), lineHeight = lineHeightEm.em, lineHeightStyle = KEEP_LINE_SPACING)
                BlockKind.Quote -> ParagraphStyle(textIndent = TextIndent(firstLine = 2.em, restLine = 2.em), lineHeight = lineHeightEm.em, lineHeightStyle = KEEP_LINE_SPACING)
                BlockKind.SceneBreak, BlockKind.Image -> ParagraphStyle(textAlign = TextAlign.Center, lineHeight = lineHeightEm.em, lineHeightStyle = KEEP_LINE_SPACING)
            }
            if (paragraphEnd > ls) paragraphs += AnnotatedString.Range(paragraph, ls, paragraphEnd)

            if (focusMode && !active) span(SpanStyle(color = ink.copy(alpha = 0.28f)), ls, le)

            when (b.kind) {
                BlockKind.Heading1 -> span(SpanStyle(fontSize = 1.55.em, fontWeight = FontWeight.Medium, letterSpacing = 0.02.em), cs, ce)
                BlockKind.Heading2 -> span(SpanStyle(fontSize = 1.25.em, fontWeight = FontWeight.SemiBold), cs, ce)
                BlockKind.Heading3 -> {
                    span(SpanStyle(fontSize = 0.8.em, fontWeight = FontWeight.SemiBold, letterSpacing = 0.18.em, color = accent), cs, ce)
                    for (i in cs until ce) display[i] = display[i].uppercaseChar()
                }
                BlockKind.Quote -> span(SpanStyle(fontStyle = FontStyle.Italic), cs, ce)
                BlockKind.SceneBreak -> span(SpanStyle(color = accent, letterSpacing = 0.3.em), ls, le)
                BlockKind.Image -> span(SpanStyle(color = accent, fontStyle = FontStyle.Italic, fontSize = 0.85.em), ls, le)
                else -> Unit
            }

            when (b.kind) {
                BlockKind.ListItem -> {
                    // "* " aparece como marcador de lista; nada some.
                    display[ls] = '•'
                    span(SpanStyle(color = accent), ls, ls + 1)
                }
                BlockKind.SceneBreak, BlockKind.Image -> Unit
                else -> {
                    hideOrDim(ls, cs, active)
                    hideOrDim(ce, le, active)
                }
            }

            if (b.kind == BlockKind.SceneBreak || b.kind == BlockKind.Image) continue
            for (m in findInlineMarks(raw, cs, ce)) {
                val markActive = touches(m.start, m.end)
                when (m.kind) {
                    MarkKind.Bold -> span(SpanStyle(fontWeight = FontWeight.Bold), m.contentStart, m.contentEnd)
                    MarkKind.Italic -> span(SpanStyle(fontStyle = FontStyle.Italic), m.contentStart, m.contentEnd)
                    MarkKind.SmallCaps -> {
                        span(SpanStyle(fontSize = 0.8.em, letterSpacing = 0.06.em), m.contentStart, m.contentEnd)
                        for (i in m.contentStart until m.contentEnd) display[i] = display[i].uppercaseChar()
                    }
                    MarkKind.Superscript -> span(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.7.em), m.contentStart, m.contentEnd)
                    MarkKind.Subscript -> span(SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 0.7.em), m.contentStart, m.contentEnd)
                    MarkKind.Footnote -> span(
                        SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.65.em, fontWeight = FontWeight.Bold, color = accent),
                        m.contentStart, m.contentEnd,
                    )
                }
                hideOrDim(m.start, m.contentStart, markActive)
                hideOrDim(m.contentEnd, m.end, markActive)
            }
        }

        matches.forEachIndexed { i, m ->
            if (m.end <= raw.length) span(SpanStyle(background = if (i == currentMatch) currentMatchColor else matchColor), m.start, m.end)
        }
        if (spacerSp <= 0f || lines.size < 2) {
            return TransformedText(AnnotatedString(String(display), spans, paragraphs), OffsetMapping.Identity)
        }
        return withParagraphSpacers(display, spans, paragraphs, lines.map { it.start to it.end })
    }

    /**
     * Insere o espaçador antes de cada parágrafo (menos o primeiro). A linha i (a partir de 0) anda i posições
     * para a direita; o espaçador dela fica logo antes do começo dela, dentro do mesmo parágrafo.
     */
    private fun withParagraphSpacers(
        display: CharArray,
        spans: List<AnnotatedString.Range<SpanStyle>>,
        paragraphs: List<AnnotatedString.Range<ParagraphStyle>>,
        lines: List<Pair<Int, Int>>,
    ): TransformedText {
        val starts = IntArray(lines.size) { lines[it].first }
        val ends = IntArray(lines.size) { lines[it].second }
        fun lineOf(raw: Int): Int {
            var lo = 0
            var hi = starts.lastIndex
            while (lo < hi) {
                val mid = (lo + hi + 1) / 2
                if (starts[mid] <= raw) lo = mid else hi = mid - 1
            }
            return lo
        }
        fun toDisplay(raw: Int) = raw + lineOf(raw)

        val text = StringBuilder(display.size + lines.size)
        for ((i, line) in lines.withIndex()) {
            if (i > 0) text.append(ZERO_WIDTH)
            val end = if (i < lines.lastIndex) line.second + 1 else line.second
            text.append(display, line.first, end - line.first)
        }
        val spacer = SpanStyle(fontSize = spacerSp.sp, color = Color.Transparent)
        val shiftedSpans = spans.map { AnnotatedString.Range(it.item, toDisplay(it.start), toDisplay(it.end)) } +
            (1..lines.lastIndex).map { i -> AnnotatedString.Range(spacer, starts[i] + i - 1, starts[i] + i) }
        // Cada parágrafo começa no próprio espaçador e termina na quebra de linha (largura zero) que o fecha.
        val shiftedParagraphs = paragraphs.map { p ->
            val i = lineOf(p.start)
            val start = if (i == 0) 0 else starts[i] + i - 1
            AnnotatedString.Range(p.item, start, p.end + i)
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = toDisplay(offset)
            override fun transformedToOriginal(offset: Int): Int {
                var lo = 0
                var hi = starts.lastIndex
                while (lo < hi) {
                    val mid = (lo + hi + 1) / 2
                    if (starts[mid] + mid <= offset) lo = mid else hi = mid - 1
                }
                val raw = offset - lo
                // Depois do fim da linha só existe o espaçador da próxima: vai para o começo dela.
                return if (lo < starts.lastIndex && raw > ends[lo]) starts[lo + 1] else raw.coerceIn(0, ends.last())
            }
        }
        return TransformedText(AnnotatedString(text.toString(), shiftedSpans, shiftedParagraphs), mapping)
    }

    private companion object {
        /** Marcador fora do cursor: sem cor e praticamente sem largura. */
        val HIDDEN = SpanStyle(color = Color.Transparent, fontSize = 0.5.sp, letterSpacing = 0.sp)
        /**
         * Por padrão o Compose corta o espaço da entrelinha no topo da primeira e no pé da última linha de cada
         * parágrafo; como cada linha do manuscrito é um parágrafo, o espaçamento sumiria por inteiro.
         */
        val KEEP_LINE_SPACING = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
    }
}

/**
 * Tamanho (sp) do espaçador que produz [gapEm] de espaço entre parágrafos nesta fonte. A relação entre o
 * tamanho do espaçador e o espaço depende das medidas da fonte, então ela é medida e não estimada.
 */
@Composable
fun rememberParagraphSpacer(fontFamily: FontFamily, fontSizeSp: Float, lineHeightEm: Float, gapEm: Float): Float {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(fontFamily, fontSizeSp, lineHeightEm, gapEm, density) {
        if (gapEm <= 0f) return@remember 0f
        val style = TextStyle(fontFamily = fontFamily, fontSize = fontSizeSp.sp, lineHeight = lineHeightEm.em)
        val paragraph = ParagraphStyle(lineHeight = lineHeightEm.em, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None))
        // "Ag", o espaço que fecha o parágrafo, o espaçador (U+200B) e "Ag": o mesmo arranjo do editor.
        val sample = "Ag " + ZERO_WIDTH + "Ag"
        fun secondBaseline(spacer: Float): Float {
            val spans = if (spacer > 0f) listOf(AnnotatedString.Range(SpanStyle(fontSize = spacer.sp), 3, 4)) else emptyList()
            val text = AnnotatedString(sample, spans, listOf(AnnotatedString.Range(paragraph, 0, 3), AnnotatedString.Range(paragraph, 3, 6)))
            return measurer.measure(text, style).let { it.getLineBaseline(1) }
        }
        val base = secondBaseline(0f)
        val target = with(density) { (gapEm * fontSizeSp).sp.toPx() }
        var lo = fontSizeSp
        var hi = fontSizeSp * (2f + gapEm * 8f)
        repeat(18) {
            val mid = (lo + hi) / 2
            if (secondBaseline(mid) - base < target) lo = mid else hi = mid
        }
        hi
    }
}

/** Espaço de largura zero (U+200B), usado como espaçador entre parágrafos. */
internal const val ZERO_WIDTH = "\u200B"
