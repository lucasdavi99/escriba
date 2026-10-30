package com.cuboide.escriba.core.layout

import com.cuboide.escriba.core.export.InlineSpan
import com.cuboide.escriba.core.export.exportBlock
import com.cuboide.escriba.core.export.parseInline
import com.cuboide.escriba.core.export.splitParagraphs
import com.cuboide.escriba.core.model.BookManifest
import com.cuboide.escriba.core.model.ChapterKind
import com.cuboide.escriba.core.model.ChapterMeta
import com.cuboide.escriba.core.model.chapterNumbers
import com.cuboide.escriba.core.text.BlockKind
import com.cuboide.escriba.core.text.footnoteIds
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// ---------------------------------------------------------------- Entrada

data class TypesetChapter(
    val id: String,
    val kind: ChapterKind,
    val title: String,
    /** Número do capítulo (só capítulos numerados). */
    val number: Int?,
    /** Parágrafos na marcação do manuscrito (uma linha cada). */
    val paragraphs: List<String>,
    val footnotes: Map<String, String> = emptyMap(),
)

data class TypesetSource(val title: String, val author: String, val chapters: List<TypesetChapter>)

fun typesetSource(manifest: BookManifest, chapters: List<Pair<ChapterMeta, String>>): TypesetSource {
    val numbers = chapterNumbers(manifest.chapters)
    return TypesetSource(
        title = manifest.title,
        author = manifest.author,
        chapters = chapters.map { (meta, body) ->
            TypesetChapter(meta.id, meta.kind, meta.title, numbers[meta.id], splitParagraphs(body), meta.footnotes)
        },
    )
}

/** Tamanho em pixels de uma imagem do livro (para manter a proporção); `null` se não existir. */
fun interface ImageSizer {
    fun pixelSize(path: String): Pair<Int, Int>?
}

// ---------------------------------------------------------------- Saída

/** Elementos posicionados em pontos, a partir do canto superior esquerdo do formato final (sem sangria). */
sealed interface PageItem

/** Texto com a linha de base em [y]. [ink] é a fração de preto (1 = preto; 0,6 = cinza). */
data class TextItem(val x: Float, val y: Float, val text: String, val font: FontSpec, val sizePt: Float, val trackingEm: Float = 0f, val ink: Float = 1f) : PageItem

data class RuleItem(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val widthPt: Float = 0.5f) : PageItem

/** Losango decorativo (desenhado, não depende de glifos da fonte). */
data class DiamondItem(val cx: Float, val cy: Float, val radius: Float) : PageItem

data class ImageItem(val x: Float, val y: Float, val width: Float, val height: Float, val path: String) : PageItem

enum class PageKind(val label: String) {
    Branca("Página em branco"),
    FalsaFolha("Falsa folha de rosto"),
    Rosto("Folha de rosto"),
    Creditos("Créditos"),
    Dedicatoria("Dedicatória"),
    Sumario("Sumário"),
    Abertura("Abertura de capítulo"),
    Texto("Texto"),
}

/** [folio] é o número que a página mostra (ou mostraria): "7", "iii"… */
data class LaidPage(val number: Int, val kind: PageKind, val chapterId: String?, val items: List<PageItem>, val folio: String = number.toString()) {
    /** Página ímpar = direita. */
    val recto: Boolean get() = number % 2 == 1
}

data class TocEntry(val chapterId: String, val label: String, val page: Int, val folio: String = page.toString())

data class LaidOutBook(
    val settings: LayoutSettings,
    val widthPt: Float,
    val heightPt: Float,
    val pages: List<LaidPage>,
    val toc: List<TocEntry>,
    val warnings: List<String>,
    val bodyFamily: String,
    val titleFamily: String,
    val title: String = "",
    val author: String = "",
    /** Página física do 1º capítulo (onde a numeração arábica recomeça quando há romanos). */
    val bodyStart: Int = 1,
) {
    /** Estimativa da lombada pelo papel escolhido. */
    val spineMm: Float get() = pages.size * settings.page.paper.mmPerPage
    fun firstPageOf(chapterId: String): Int? = pages.firstOrNull { it.chapterId == chapterId }?.number
}

// ---------------------------------------------------------------- Motor

private enum class Align { Justify, Left, Center, Right }

/**
 * Diagramação do miolo: transforma os capítulos em páginas, com a mesma geometria usada na prévia e no PDF.
 * O sumário é resolvido em duas passagens (a primeira descobre em que página cada capítulo começa).
 */
class Typesetter(
    private val settings: LayoutSettings,
    private val measurer: GlyphMeasurer,
    private val images: ImageSizer = ImageSizer { null },
) {
    val widthPt = mmToPt(settings.page.trim.widthMm)
    val heightPt = mmToPt(settings.page.trim.heightMm)
    private val page = settings.page
    private val body = settings.body
    private val size = body.sizePt
    private val lead = body.sizePt * body.leading

    private val warnings = ArrayList<String>()
    val bodyFamily: String
    val titleFamily: String

    init {
        val wantedBody = body.fontFamily.ifBlank { null }
        bodyFamily = measurer.resolveFamily(listOfNotNull(wantedBody) + settings.theme.bodyFonts)
        if (wantedBody != null && !measurer.hasFamily(wantedBody)) warnings += "A fonte “$wantedBody” não está instalada; usando “$bodyFamily”."
        val wantedTitle = settings.opening.titleFont.ifBlank { null }
        titleFamily = measurer.resolveFamily(listOfNotNull(wantedTitle) + settings.theme.titleFonts + bodyFamily)
        if (wantedTitle != null && !measurer.hasFamily(wantedTitle)) warnings += "A fonte de título “$wantedTitle” não está instalada; usando “$titleFamily”."
    }

    private val regular = FontSpec(bodyFamily)
    private val titleFont = FontSpec(titleFamily)

    fun typeset(source: TypesetSource): LaidOutBook {
        val first = Pass(source, tocPages = null).run()
        val pass = Pass(source, tocPages = first.toc.associate { it.chapterId to it.folio })
        val book = pass.run()
        return book.copy(warnings = (warnings + book.warnings).distinct())
    }

    /** Mancha de uma página em pontos: a global ou a própria da página. */
    private class PageGeom(val top: Float, val bottom: Float, val left: Float, val textW: Float, val bottomMarginPt: Float)

    private fun geometry(pageNumber: Int): PageGeom {
        val m = page.marginsFor(pageNumber)
        val inner = mmToPt(m.innerMm)
        val outer = mmToPt(m.outerMm)
        val left = if (pageNumber % 2 == 1) inner else outer
        return PageGeom(mmToPt(m.topMm), heightPt - mmToPt(m.bottomMm), left, widthPt - inner - outer, mmToPt(m.bottomMm))
    }

    // ------------------------------------------------------------ Composição de parágrafos

    private data class Style(val bold: Boolean, val italic: Boolean, val smallCaps: Boolean, val sup: Boolean, val sub: Boolean)

    /** Itens de quebra de linha de um parágrafo. */
    private fun paragraphItems(
        spans: List<InlineSpan>,
        sizePt: Float,
        family: String,
        baseItalic: Boolean = false,
        baseBold: Boolean = false,
        firstIndent: Float = 0f,
        justify: Boolean,
        hyphenate: Boolean = body.hyphenate,
        noteNumbers: Map<String, Int> = emptyMap(),
        trackingEm: Float = 0f,
        uppercase: Boolean = false,
    ): List<LbItem> {
        fun runFor(text: String, st: Style): Run {
            val font = FontSpec(family, st.bold || baseBold, st.italic != baseItalic)
            val shown = if (st.smallCaps || uppercase) text.uppercase() else text
            val s = when {
                st.smallCaps -> sizePt * 0.8f
                st.sup || st.sub -> sizePt * 0.62f
                else -> sizePt
            }
            val rise = when {
                st.sup -> sizePt * 0.36f
                st.sub -> -sizePt * 0.12f
                else -> 0f
            }
            return Run(shown, font, s, measurer.width(shown, font, s, trackingEm), rise, trackingEm)
        }

        // Palavras: sequência de trechos (texto, estilo) e chamadas de nota.
        class Word { val parts = ArrayList<Pair<String, Style>>(); val notes = ArrayList<String>() }
        val words = ArrayList<Word>()
        var current: Word? = null
        for (span in spans) {
            val st = Style(span.bold, span.italic, span.smallCaps, span.superscript, span.subscript)
            if (span.footnote != null) {
                (current ?: words.lastOrNull() ?: Word().also { words += it; current = it }).notes += span.footnote
                continue
            }
            val sb = StringBuilder()
            fun flushPart() {
                if (sb.isNotEmpty()) {
                    val w = current ?: Word().also { words += it; current = it }
                    w.parts += sb.toString() to st
                    sb.clear()
                }
            }
            for (ch in span.text) {
                if (ch == ' ' || ch == '\t' || ch == '\n') {
                    flushPart()
                    current = null
                } else sb.append(ch)
            }
            flushPart()
        }

        val space = measurer.width(" ", FontSpec(family, baseBold, baseItalic), sizePt, trackingEm)
        val hyphenRun = run {
            val f = FontSpec(family, baseBold, baseItalic)
            Run("-", f, sizePt, measurer.width("-", f, sizePt))
        }
        val items = ArrayList<LbItem>()
        if (firstIndent > 0f) items += LbBox(firstIndent, emptyList())
        words.forEachIndexed { wi, word ->
            if (wi > 0) items += if (justify) LbGlue(space, space * 0.6f, space * 0.3f) else LbGlue(space, 0f, 0f)
            val text = word.parts.joinToString("") { it.first }
            // Pontos de quebra: hífen explícito e sílabas do miolo de letras.
            val cuts = HashSet<Int>()
            val explicit = HashSet<Int>()
            text.forEachIndexed { i, c -> if (c == '-' && i in 1 until text.length - 1) { cuts += i + 1; explicit += i + 1 } }
            if (hyphenate) {
                var segStart = 0
                for (seg in text.split('-')) {
                    val firstLetter = seg.indexOfFirst { it.isLetter() }
                    val lastLetter = seg.indexOfLast { it.isLetter() }
                    if (firstLetter >= 0 && lastLetter > firstLetter) {
                        val core = seg.substring(firstLetter, lastLetter + 1)
                        hyphenationPoints(core).forEach { cuts += segStart + firstLetter + it }
                    }
                    segStart += seg.length + 1
                }
            }
            // Corta os trechos nas posições de quebra.
            val pieces = ArrayList<MutableList<Pair<String, Style>>>()
            var piece = ArrayList<Pair<String, Style>>()
            var pos = 0
            for ((partText, st) in word.parts) {
                var from = 0
                // Corte exatamente na fronteira entre dois trechos de estilos diferentes.
                if (pos > 0 && pos in cuts && piece.isNotEmpty()) {
                    pieces += piece
                    piece = ArrayList()
                }
                for (cut in cuts.filter { it > pos && it < pos + partText.length }.sorted()) {
                    piece += partText.substring(from, cut - pos) to st
                    pieces += piece
                    piece = ArrayList()
                    from = cut - pos
                }
                piece += partText.substring(from) to st
                pos += partText.length
            }
            pieces += piece
            var offset = 0
            pieces.forEachIndexed { pi, p ->
                val runs = p.filter { it.first.isNotEmpty() }.map { runFor(it.first, it.second) }.toMutableList()
                offset += p.sumOf { it.first.length }
                val isLast = pi == pieces.lastIndex
                val notes = if (isLast) word.notes else emptyList()
                if (isLast) {
                    for (id in word.notes) {
                        val n = noteNumbers[id] ?: continue
                        runs += runFor(n.toString(), Style(false, false, false, sup = true, sub = false)).copy(font = FontSpec(family))
                    }
                }
                items += LbBox(runs.sumOf { it.width.toDouble() }.toFloat(), runs, notes)
                if (!isLast) {
                    items += if (offset in explicit) LbPenalty(0f, 50f, true) else LbPenalty(hyphenRun.width, 50f, true, hyphenRun)
                }
            }
        }
        items += LbGlue(0f, LB_INFINITY, 0f)
        items += LbPenalty(0f, -LB_INFINITY, false)
        return items
    }

    private fun breakLines(items: List<LbItem>, lineWidth: (Int) -> Float, justify: Boolean, fine: Boolean = body.fineComposition): List<ComposedLine> =
        breakLinesAt(items, lineWidth, justify, fine).first

    /** Linhas e as posições das quebras em [items] (para recompor o resto do parágrafo a partir de uma linha). */
    private fun breakLinesAt(items: List<LbItem>, lineWidth: (Int) -> Float, justify: Boolean, fine: Boolean = body.fineComposition): Pair<List<ComposedLine>, List<Int>> {
        val breaks = (if (fine && justify) knuthPlassBreaks(items, lineWidth, 2.5f) ?: knuthPlassBreaks(items, lineWidth, 8f) else null)
            ?: greedyBreaks(items, lineWidth)
        return composeLines(items, breaks, lineWidth, justify) to breaks
    }

    /** Linhas simples (títulos, créditos): sem justificar, sem hifenizar. */
    private fun simpleLines(text: String, font: FontSpec, sizePt: Float, width: Float, trackingEm: Float = 0f, uppercase: Boolean = false): List<ComposedLine> {
        val items = paragraphItems(
            listOf(InlineSpan(text)), sizePt, font.family, baseItalic = font.italic, baseBold = font.bold,
            justify = false, hyphenate = false, trackingEm = trackingEm, uppercase = uppercase,
        )
        return breakLines(items, { width }, justify = false, fine = false)
    }

    private fun baselineIn(lineTop: Float, sizePt: Float, lineHeight: Float) = lineTop + (lineHeight - sizePt) / 2f + sizePt * 0.8f

    // ------------------------------------------------------------ Passagem

    private inner class Pass(private val source: TypesetSource, private val tocPages: Map<String, String>?) {
        val pages = ArrayList<LaidPage>()
        val toc = ArrayList<TocEntry>()
        val passWarnings = ArrayList<String>()

        var items = ArrayList<PageItem>()
        var kind = PageKind.Branca
        var chapterId: String? = null
        var chapterTitle = ""
        var open = false
        var y = 0f
        val pageNotes = ArrayList<List<ComposedLine>>()
        val pageNoteIds = HashSet<String>()
        var notesHeight = 0f

        var noteNumbers: Map<String, Int> = emptyMap()
        var noteTexts: Map<String, String> = emptyMap()
        val noteSize = size * 0.8f
        val noteLead = noteSize * 1.3f
        val noteGap = noteLead * 1.2f
        private val noteCache = HashMap<Pair<String, Float>, List<ComposedLine>>()

        val number get() = pages.size + 1

        // Mancha da página corrente (cada página pode ter a sua).
        private var geomPage = -1
        private var geomCache: PageGeom? = null
        val geom: PageGeom
            get() {
                if (geomPage != number) { geomCache = geometry(number); geomPage = number }
                return geomCache!!
            }
        val top get() = geom.top
        val bottom get() = geom.bottom
        val textW get() = geom.textW
        val x0 get() = geom.left

        fun run(): LaidOutBook {
            frontMatter()
            var tocDone = !settings.front.toc
            for (ch in source.chapters) {
                val shortPart = ch.kind == ChapterKind.Dedicatoria || ch.kind == ChapterKind.Epigrafe
                if (!tocDone && !shortPart) { tableOfContents(); tocDone = true }
                if (shortPart) shortPage(ch) else chapter(ch)
            }
            if (!tocDone) tableOfContents()
            closePage()
            if (pages.size % 2 == 1) {
                blankPage()
                passWarnings += "Uma página em branco foi acrescentada no fim: livro impresso fecha em número par."
            }
            return LaidOutBook(settings, widthPt, heightPt, pages.toList(), toc.toList(), passWarnings, bodyFamily, titleFamily, source.title, source.author, bodyStart ?: 1)
        }

        // -------------------------------------------------------- Páginas

        /** Página do 1º capítulo: a partir dela a numeração é arábica. */
        var bodyStart: Int? = null

        fun folioOf(n: Int) = settings.heads.folioLabel(n, bodyStart)

        fun openPage(k: PageKind, id: String? = chapterId, title: String = chapterTitle) {
            if (open) closePage()
            if (k == PageKind.Abertura && bodyStart == null) bodyStart = number
            items = ArrayList()
            kind = k
            chapterId = id
            chapterTitle = title
            y = top
            open = true
            pageNotes.clear()
            pageNoteIds.clear()
            notesHeight = 0f
        }

        fun closePage() {
            if (!open) return
            if (kind == PageKind.Texto) furniture(opening = false)
            if (kind == PageKind.Abertura) furniture(opening = true)
            if (pageNotes.isNotEmpty()) placeNotes()
            pages += LaidPage(number, kind, chapterId, items.toList(), folioOf(number))
            open = false
        }

        fun blankPage() {
            openPage(PageKind.Branca, null, "")
            closePage()
        }

        fun startOn(side: OpeningSide) {
            closePage()
            val next = pages.size + 1
            if (side == OpeningSide.Direita && next % 2 == 0) blankPage()
            if (side == OpeningSide.Esquerda && next % 2 == 1) blankPage()
        }

        fun availableBottom() = bottom - if (pageNotes.isEmpty()) 0f else notesHeight + noteGap

        fun hasContent() = y > top + 0.5f

        /** Cabeçalho e número da página; na abertura de capítulo seguem as regras próprias dela. */
        fun furniture(opening: Boolean) {
            val heads = settings.heads
            val recto = number % 2 == 1
            val headBaseline = top - heads.sizePt * 1.8f - mmToPt(heads.raiseMm)
            val folio = folioOf(number)
            val folioSize = heads.sizePt + 0.5f
            val fw = measurer.width(folio, regular, folioSize)
            val showFolio = number !in heads.hiddenFolios &&
                when (heads.folioPages) {
                    FolioPages.Todas -> true
                    FolioPages.Impares -> recto
                    FolioPages.Pares -> !recto
                } &&
                !(opening && heads.openingFolio == OpeningFolio.Nenhum)
            val folioPos = if (opening && heads.openingFolio == OpeningFolio.Pe) FolioPosition.RodapeCentro else heads.folio

            val showHead = number !in heads.hiddenHeads && (!opening || heads.onOpening)
            val own = chapterId?.let { heads.chapterHeads[it] }?.let { if (recto) it.recto else it.verso }?.takeIf { it.isNotBlank() }
            val text = own ?: when (if (recto) heads.recto else heads.verso) {
                HeadContent.Nada -> ""
                HeadContent.Livro -> source.title
                HeadContent.Autor -> source.author
                HeadContent.Capitulo -> chapterTitle
            }
            if (showHead && text.isNotBlank()) {
                val tracking = if (heads.uppercase) 0.12f else 0f
                val shown = if (heads.uppercase) text.uppercase() else text
                val font = regular.copy(italic = !heads.uppercase)
                val w = measurer.width(shown, font, heads.sizePt, tracking)
                // O número no alto, do lado de fora, empurra o cabeçalho para dentro.
                val folioGap = if (showFolio && folioPos == FolioPosition.TopoExterno) fw + heads.sizePt * 1.5f else 0f
                val outerX = if (recto) x0 + textW - w - folioGap else x0 + folioGap
                val innerX = if (recto) x0 else x0 + textW - w
                val x = when (heads.position) {
                    HeadPosition.Centro -> x0 + (textW - w) / 2f
                    HeadPosition.Externo -> outerX
                    HeadPosition.Interno -> innerX
                }
                items += TextItem(x, headBaseline, shown, font, heads.sizePt, tracking, heads.ink.ink)
            }
            if (!showFolio) return
            val footBaseline = heightPt - geom.bottomMarginPt * 0.45f
            when (folioPos) {
                FolioPosition.RodapeCentro -> items += TextItem(x0 + (textW - fw) / 2f, footBaseline, folio, regular, folioSize)
                FolioPosition.RodapeExterno -> items += TextItem(if (recto) x0 + textW - fw else x0, footBaseline, folio, regular, folioSize)
                FolioPosition.TopoExterno -> items += TextItem(if (recto) x0 + textW - fw else x0, headBaseline, folio, regular, folioSize)
                FolioPosition.Nenhum -> Unit
            }
        }

        fun placeNotes() {
            var ny = bottom - notesHeight
            items += RuleItem(x0, ny - noteGap * 0.55f, x0 + textW * 0.3f, ny - noteGap * 0.55f, 0.4f)
            for (lines in pageNotes) {
                for (line in lines) {
                    emitLine(line, x0, baselineIn(ny, noteSize, noteLead))
                    ny += noteLead
                }
            }
        }

        fun emitLine(line: ComposedLine, x: Float, baseline: Float) {
            for (pr in line.runs) {
                if (pr.run.text.isEmpty()) continue
                items += TextItem(x + pr.x, baseline - pr.run.rise, pr.run.text, pr.run.font, pr.run.sizePt, pr.run.trackingEm)
            }
        }

        fun noteLines(id: String): List<ComposedLine> = noteCache.getOrPut(id to textW) {
            val n = noteNumbers[id]?.toString() ?: "?"
            val spans = listOf(InlineSpan("$n  ")) + parseInline(noteTexts[id].orEmpty())
            breakLines(paragraphItems(spans, noteSize, bodyFamily, justify = true), { textW }, justify = true)
        }

        // -------------------------------------------------------- Fluxo de linhas

        /**
         * Coloca as linhas a partir de [y], quebrando página quando preciso, respeitando viúvas e órfãs e
         * reservando no pé da página o espaço das notas citadas nas linhas.
         */
        fun placeLines(
            lines: List<ComposedLine>,
            lineHeight: Float,
            sizePt: Float,
            align: Align,
            xOffset: (Int) -> Float = { 0f },
            width: (Int) -> Float = { textW },
            minLines: Int = body.widowOrphanLines,
            recompose: Recompose? = null,
        ) {
            @Suppress("NAME_SHADOWING") var lines = lines
            var idx = 0
            while (idx < lines.size) {
                if (!open) openPage(PageKind.Texto)
                // Quantas linhas cabem nesta página (com as notas que elas trazem).
                var fit = 0
                var extra = 0f
                val seen = HashSet<String>()
                while (idx + fit < lines.size) {
                    val newNotes = lines[idx + fit].footnoteIds.filter { it !in seen && it !in pageNoteIds && it in noteNumbers }
                    val h = newNotes.sumOf { noteLines(it).size * noteLead.toDouble() }.toFloat()
                    val gap = if (pageNotes.isEmpty() && seen.isEmpty() && newNotes.isNotEmpty()) noteGap else 0f
                    val limit = availableBottom() - extra - h - gap
                    if (y + (fit + 1) * lineHeight > limit + 0.01f) break
                    extra += h + gap
                    seen += newNotes
                    fit++
                }
                val remaining = lines.size - idx
                var k = fit
                if (k < remaining && minLines > 1 && remaining > 1) {
                    if (idx == 0 && k < minLines && hasContent()) k = 0 // órfã: não começa o parágrafo com poucas linhas
                    if (remaining - k in 1 until minLines) k = remaining - minLines // viúva: leva linhas junto
                    if (idx == 0 && k in 1 until minLines) k = 0
                    if (k < 0) k = 0
                }
                if (k == 0 && !hasContent()) k = max(1, min(fit, remaining)) // página vazia: coloca o que der
                for (j in 0 until k) {
                    val i = idx + j
                    val line = lines[i]
                    val w = width(i)
                    val dx = when (align) {
                        Align.Justify, Align.Left -> 0f
                        Align.Center -> (w - line.width) / 2f
                        Align.Right -> w - line.width
                    }
                    emitLine(line, x0 + xOffset(i) + dx, baselineIn(y, sizePt, lineHeight))
                    for (id in line.footnoteIds) {
                        if (id in noteNumbers && pageNoteIds.add(id)) {
                            val nl = noteLines(id)
                            pageNotes += nl
                            notesHeight += nl.size * noteLead
                        }
                    }
                    y += lineHeight
                }
                idx += k
                if (idx < lines.size) {
                    closePage()
                    openPage(PageKind.Texto)
                    // A página nova tem outra largura de mancha: recompõe o resto do parágrafo nela.
                    if (recompose != null && abs(recompose.width - textW) > 0.01f) lines = lines.take(idx) + recompose.from(idx)
                }
            }
        }

        /**
         * Parágrafo que acompanha a largura da mancha: se ele passar para uma página com mancha diferente, o resto é
         * quebrado de novo na largura nova. [narrow] desconta da largura (recuo de citação, marcador de lista).
         */
        inner class Recompose(private var items: List<LbItem>, private val justify: Boolean, private val narrow: Float) {
            var width = 0f
                private set
            private var breaks: List<Int> = emptyList()
            private var offset = 0

            fun first(): List<ComposedLine> = compose()

            fun from(lineIndex: Int): List<ComposedLine> {
                val local = lineIndex - offset
                if (local > 0) items = items.subList(breaks[local - 1] + 1, items.size)
                offset = lineIndex
                return compose()
            }

            private fun compose(): List<ComposedLine> {
                width = textW
                val w = width - narrow
                val (lines, b) = breakLinesAt(items, { w }, justify)
                breaks = b
                return lines
            }
        }

        fun flowParagraph(items: List<LbItem>, justify: Boolean, lineHeight: Float, sizePt: Float, align: Align, narrow: Float = 0f, inset: Float = 0f) {
            val r = Recompose(items, justify, narrow)
            placeLines(r.first(), lineHeight, sizePt, align, xOffset = { inset }, width = { textW - narrow }, recompose = r)
        }

        fun ensureSpace(height: Float) {
            if (!open) openPage(PageKind.Texto)
            if (hasContent() && y + height > availableBottom()) openPage(PageKind.Texto)
        }

        fun centeredText(text: String, font: FontSpec, sizePt: Float, lineHeight: Float, trackingEm: Float = 0f, uppercase: Boolean = false) {
            val lines = simpleLines(text, font, sizePt, textW, trackingEm, uppercase)
            for (line in lines) {
                emitLine(line, x0 + (textW - line.width) / 2f, baselineIn(y, sizePt, lineHeight))
                y += lineHeight
            }
        }

        fun ornament() {
            val cy = y + lead * 0.5f
            val cx = x0 + textW / 2f
            items += DiamondItem(cx, cy, size * 0.22f)
            items += RuleItem(cx - size * 3.2f, cy, cx - size * 0.7f, cy, 0.4f)
            items += RuleItem(cx + size * 0.7f, cy, cx + size * 3.2f, cy, 0.4f)
            y += lead
        }

        // -------------------------------------------------------- Pré-textuais

        fun frontMatter() {
            val front = settings.front
            if (front.halfTitle) {
                openPage(PageKind.FalsaFolha, null, "")
                y = heightPt * 0.3f
                centeredText(source.title, titleFont, 15f, 15f * 1.3f)
                closePage()
            }
            if (front.titlePage) {
                startOn(OpeningSide.Direita)
                openPage(PageKind.Rosto, null, "")
                y = top + lead * 2
                if (source.author.isNotBlank()) centeredText(source.author, regular, 12f, 16f, trackingEm = 0.08f, uppercase = true)
                y = heightPt * 0.36f
                centeredText(source.title, titleFont, 24f, 30f)
                if (front.publisher.isNotBlank()) {
                    y = bottom - lead * 2
                    centeredText(front.publisher, regular, 10f, 13f, trackingEm = 0.1f, uppercase = true)
                }
                closePage()
            }
            if (front.credits && front.titlePage) {
                openPage(PageKind.Creditos, null, "")
                val credit = buildList {
                    add("Copyright © ${front.year.ifBlank { "" }} ${source.author}".replace("  ", " ").trim())
                    add("Todos os direitos reservados.")
                    if (front.creditsText.isNotBlank()) { add(""); addAll(front.creditsText.split('\n')) }
                    if (front.isbn.isNotBlank()) { add(""); add("ISBN ${front.isbn}") }
                    if (front.publisher.isNotBlank()) add(front.publisher)
                }
                val cSize = 8f
                val cLead = 11f
                val lines = credit.flatMap { if (it.isBlank()) listOf(null) else simpleLines(it, regular, cSize, textW) }
                y = bottom - lines.size * cLead
                for (line in lines) {
                    if (line != null) emitLine(line, x0, baselineIn(y, cSize, cLead))
                    y += cLead
                }
                closePage()
            }
        }

        /** Dedicatória e epígrafe: página ímpar, texto em itálico alinhado à direita no primeiro terço. */
        fun shortPage(ch: TypesetChapter) {
            startOn(OpeningSide.Direita)
            openPage(PageKind.Dedicatoria, ch.id, ch.title)
            toc += TocEntry(ch.id, ch.title, number, folioOf(number))
            y = heightPt * 0.3f
            val blockW = textW * 0.72f
            for (p in ch.paragraphs) {
                val spans = parseInline(exportBlock(p).content)
                val lines = breakLines(paragraphItems(spans, size, bodyFamily, baseItalic = true, justify = false, hyphenate = false), { blockW }, justify = false)
                placeLines(lines, lead, size, Align.Right, xOffset = { textW - blockW }, width = { blockW }, minLines = 1)
            }
            closePage()
        }

        fun tableOfContents() {
            val entries = source.chapters.filter { it.kind != ChapterKind.Dedicatoria && it.kind != ChapterKind.Epigrafe }
            if (entries.isEmpty()) return
            startOn(OpeningSide.Direita)
            openPage(PageKind.Sumario, null, "Sumário")
            y = top + mmToPt(settings.opening.sinkMm) * 0.6f
            centeredText("Sumário", titleFont, settings.opening.titleSizePt * 0.85f, settings.opening.titleSizePt * 1.2f, uppercase = settings.opening.titleUppercase)
            y += lead * 1.5f
            val dot = measurer.width(".", regular, size)
            for (ch in entries) {
                val label = ch.number?.let { "$it  ${ch.title}" } ?: ch.title
                val pageNo = tocPages?.get(ch.id) ?: "000"
                if (y + lead > availableBottom()) openPage(PageKind.Sumario, null, "Sumário")
                val numW = measurer.width(pageNo, regular, size)
                val lines = simpleLines(label, regular, size, textW - numW - size * 2f)
                lines.forEachIndexed { i, line ->
                    val baseline = baselineIn(y, size, lead)
                    emitLine(line, x0, baseline)
                    if (i == lines.lastIndex) {
                        items += TextItem(x0 + textW - numW, baseline, pageNo, regular, size)
                        val from = x0 + line.width + size * 0.5f
                        val to = x0 + textW - numW - size * 0.5f
                        val count = ((to - from) / (dot * 2f)).toInt()
                        if (count > 0) items += TextItem(to - count * dot * 2f, baseline, ". ".repeat(count).trimEnd(), regular, size)
                    }
                    y += lead
                }
            }
            closePage()
        }

        // -------------------------------------------------------- Capítulos

        fun chapter(ch: TypesetChapter) {
            val opening = settings.opening
            startOn(opening.side)
            openPage(PageKind.Abertura, ch.id, ch.title)
            toc += TocEntry(ch.id, ch.title, number, folioOf(number))
            noteNumbers = footnoteIds(ch.paragraphs.joinToString("\n")).withIndex().associate { (i, id) -> id to i + 1 }
            noteTexts = ch.footnotes
            noteCache.clear()

            y = top + mmToPt(opening.sinkMm)
            val label = if (ch.kind == ChapterKind.Capitulo && ch.number != null) chapterLabel(opening.numberStyle, ch.number) else null
            if (label != null) {
                if (opening.numberStyle == ChapterNumberStyle.SoNumero) {
                    centeredText(label, titleFont, opening.titleSizePt * 1.8f, opening.titleSizePt * 2.1f)
                } else {
                    centeredText(label, regular, size * 0.9f, lead * 1.2f, trackingEm = 0.18f, uppercase = true)
                    y += lead * 0.4f
                }
            }
            centeredText(ch.title, titleFont, opening.titleSizePt, opening.titleSizePt * 1.25f, uppercase = opening.titleUppercase)
            if (opening.ornament) { y += lead * 0.4f; ornament() }
            y += lead * 1.6f
            chapterBody(ch, dropCap = opening.dropCapLines > 0 && ch.kind == ChapterKind.Capitulo)
        }

        fun chapterBody(ch: TypesetChapter, dropCap: Boolean) {
            var noIndentNext = body.firstParagraphNoIndent
            var dropCapPending = dropCap
            for (raw in ch.paragraphs) {
                val block = exportBlock(raw)
                when (block.block.kind) {
                    BlockKind.SceneBreak -> {
                        if (!open) openPage(PageKind.Texto)
                        if (hasContent() && y + lead * 3 > availableBottom()) openPage(PageKind.Texto)
                        if (hasContent()) y += lead * 0.5f
                        centeredText(settings.sceneBreak, regular, size, lead)
                        y += lead * 0.5f
                        noIndentNext = body.firstParagraphNoIndent
                    }
                    BlockKind.Heading1, BlockKind.Heading2, BlockKind.Heading3 -> {
                        val (hs, bold, italic) = when (block.block.kind) {
                            BlockKind.Heading1 -> Triple(size * 1.3f, true, false)
                            BlockKind.Heading2 -> Triple(size * 1.12f, true, false)
                            else -> Triple(size, false, true)
                        }
                        val font = FontSpec(if (block.block.kind == BlockKind.Heading3) bodyFamily else titleFamily, bold, italic)
                        val hLead = hs * 1.3f
                        val lines = simpleLines(plainHeading(block.content), font, hs, textW)
                        ensureSpace(lead + lines.size * hLead + 2 * lead)
                        if (hasContent()) y += lead
                        lines.forEach { line ->
                            emitLine(line, x0 + (textW - line.width) / 2f, baselineIn(y, hs, hLead))
                            y += hLead
                        }
                        y += lead * 0.5f
                        noIndentNext = body.firstParagraphNoIndent
                    }
                    BlockKind.Image -> {
                        image(block.block.imagePath, block.content)
                        noIndentNext = body.firstParagraphNoIndent
                    }
                    BlockKind.Quote -> {
                        val qs = size * 0.93f
                        val pad = size * 1.5f
                        if (hasContent()) y += lead * 0.5f
                        flowParagraph(paragraphItems(parseInline(block.content), qs, bodyFamily, justify = true, noteNumbers = noteNumbers), body.align == BodyAlign.Justificado, lead, qs, Align.Justify, narrow = 2 * pad, inset = pad)
                        y += lead * 0.5f
                        noIndentNext = body.firstParagraphNoIndent
                    }
                    BlockKind.ListItem -> {
                        val hang = size * 1.4f
                        if (!open) openPage(PageKind.Texto)
                        ensureSpace(lead)
                        val bulletX = x0 + size * 0.4f
                        items += TextItem(bulletX, baselineIn(y, size, lead), "•", regular, size)
                        flowParagraph(paragraphItems(parseInline(block.content), size, bodyFamily, justify = true, noteNumbers = noteNumbers), body.align == BodyAlign.Justificado, lead, size, Align.Justify, narrow = hang, inset = hang)
                        noIndentNext = false
                    }
                    BlockKind.Paragraph -> {
                        val centered = block.block.centered
                        val spans = parseInline(block.content).toMutableList()
                        val justify = body.align == BodyAlign.Justificado && !centered
                        if (dropCapPending && !centered && dropCapFrom(spans)) {
                            dropCapPending = false
                            noIndentNext = false
                            continue
                        }
                        dropCapPending = false
                        val indent = if (centered || noIndentNext) 0f else size * body.indentEm
                        flowParagraph(paragraphItems(spans, size, bodyFamily, firstIndent = indent, justify = justify, noteNumbers = noteNumbers), justify, lead, size, if (centered) Align.Center else Align.Justify)
                        y += lead * body.paragraphSpacingLines
                        noIndentNext = false
                    }
                }
            }
            closePage()
        }

        /** Primeiro parágrafo com letra capitular; devolve `false` se o parágrafo não começa com letra. */
        fun dropCapFrom(spans: MutableList<InlineSpan>): Boolean {
            val firstIdx = spans.indexOfFirst { it.footnote == null && it.text.isNotEmpty() }
            if (firstIdx < 0) return false
            val first = spans[firstIdx]
            val letter = first.text.first()
            if (!letter.isLetter()) return false
            val n = settings.opening.dropCapLines.coerceIn(2, 5)
            spans[firstIdx] = first.copy(text = first.text.drop(1))
            val capSize = (lead * (n - 1) + size * 0.7f) / 0.7f
            val cap = letter.uppercase()
            val capFont = titleFont
            val capW = measurer.width(cap, capFont, capSize) + size * 0.35f
            if (!open) openPage(PageKind.Texto)
            ensureSpace(lead * n)
            items += TextItem(x0, baselineIn(y + lead * (n - 1), size, lead), cap, capFont, capSize)
            val lines = breakLines(
                paragraphItems(spans, size, bodyFamily, justify = body.align == BodyAlign.Justificado, noteNumbers = noteNumbers),
                { if (it < n) textW - capW else textW },
                body.align == BodyAlign.Justificado,
            )
            placeLines(lines, lead, size, Align.Justify, xOffset = { if (it < n) capW else 0f }, width = { if (it < n) textW - capW else textW }, minLines = 1)
            y += lead * body.paragraphSpacingLines
            return true
        }

        fun image(path: String, caption: String) {
            val px = images.pixelSize(path)
            if (px == null) {
                passWarnings += "Imagem não encontrada: $path"
                if (caption.isNotBlank()) centeredText(caption, regular.copy(italic = true), size * 0.85f, lead)
                return
            }
            var w = textW
            var h = w * px.second / px.first
            val maxH = (bottom - top) * 0.6f
            if (h > maxH) { h = maxH; w = h * px.first / px.second }
            // Resolução efetiva: abaixo de 200 ppi a impressão fica serrilhada.
            val ppi = px.first / (w / 72f)
            if (ppi < 200f) passWarnings += "Imagem com baixa resolução (${ppi.toInt()} ppi): $path"
            val captionLines = if (caption.isBlank()) emptyList() else simpleLines(caption, regular.copy(italic = true), size * 0.85f, textW)
            ensureSpace(h + captionLines.size * lead + lead)
            if (hasContent()) y += lead * 0.5f
            items += ImageItem(x0 + (textW - w) / 2f, y, w, h, path)
            y += h + lead * 0.3f
            captionLines.forEach { line ->
                emitLine(line, x0 + (textW - line.width) / 2f, baselineIn(y, size * 0.85f, lead))
                y += lead
            }
            y += lead * 0.5f
        }
    }

    private fun plainHeading(content: String) = parseInline(content).joinToString("") { it.text }
}

/** Gera a diagramação a partir do manuscrito. */
fun layoutBook(
    manifest: BookManifest,
    chapters: List<Pair<ChapterMeta, String>>,
    settings: LayoutSettings,
    measurer: GlyphMeasurer,
    images: ImageSizer = ImageSizer { null },
): LaidOutBook = Typesetter(settings, measurer, images).typeset(typesetSource(manifest, chapters))
