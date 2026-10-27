package com.cuboide.escriba

import com.cuboide.escriba.core.layout.BodyAlign
import com.cuboide.escriba.core.layout.ChapterNumberStyle
import com.cuboide.escriba.core.layout.FontSpec
import com.cuboide.escriba.core.layout.FrontMatter
import com.cuboide.escriba.core.layout.GlyphMeasurer
import com.cuboide.escriba.core.layout.LayoutSettings
import com.cuboide.escriba.core.layout.LayoutTheme
import com.cuboide.escriba.core.layout.LbBox
import com.cuboide.escriba.core.layout.LbGlue
import com.cuboide.escriba.core.layout.LbItem
import com.cuboide.escriba.core.layout.LbPenalty
import com.cuboide.escriba.core.layout.LB_INFINITY
import com.cuboide.escriba.core.layout.OpeningSide
import com.cuboide.escriba.core.layout.PageKind
import com.cuboide.escriba.core.layout.Run
import com.cuboide.escriba.core.layout.TextItem
import com.cuboide.escriba.core.layout.Typesetter
import com.cuboide.escriba.core.layout.TypesetChapter
import com.cuboide.escriba.core.layout.TypesetSource
import com.cuboide.escriba.core.layout.chapterLabel
import com.cuboide.escriba.core.layout.composeLines
import com.cuboide.escriba.core.layout.greedyBreaks
import com.cuboide.escriba.core.layout.hyphenationPoints
import com.cuboide.escriba.core.layout.knuthPlassBreaks
import com.cuboide.escriba.core.layout.spellOutPt
import com.cuboide.escriba.core.layout.syllableBoundaries
import com.cuboide.escriba.core.layout.toRoman
import com.cuboide.escriba.core.model.ChapterKind
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Medidor de largura fixa (meio corpo por caractere) para testes independentes de fontes instaladas. */
private object MonoMeasurer : GlyphMeasurer {
    override fun width(text: String, font: FontSpec, sizePt: Float, trackingEm: Float) =
        text.length * sizePt * 0.5f + trackingEm * sizePt * text.length
    override fun resolveFamily(candidates: List<String>) = candidates.firstOrNull { hasFamily(it) } ?: "Mono"
    override fun hasFamily(family: String) = family != "Inexistente"
}

private fun syllables(word: String): String {
    val cuts = syllableBoundaries(word)
    return buildString {
        word.forEachIndexed { i, c -> if (i in cuts) append('-'); append(c) }
    }
}

class LayoutTest {
    @Test
    fun portugueseSyllables() {
        assertEquals("ca-chor-ro", syllables("cachorro"))
        assertEquals("a-bra-ço", syllables("abraço"))
        assertEquals("pers-pi-caz", syllables("perspicaz"))
        assertEquals("ni-nho", syllables("ninho"))
        assertEquals("qua-tro", syllables("quatro"))
        assertEquals("guer-ra", syllables("guerra"))
        assertEquals("ins-tru-ir", syllables("instruir"))
        assertEquals("ex-ces-so", syllables("excesso"))
        assertEquals("sa-ú-de", syllables("saúde"))
        assertEquals("po-e-ta", syllables("poeta"))
        assertEquals("co-ra-ção", syllables("coração"))
        assertEquals("a-in-da", syllables("ainda"))
        assertEquals("ra-i-nha", syllables("rainha"))
        assertEquals("mui-to", syllables("muito"))
        assertEquals("lin-gua-gem", syllables("linguagem"))
        assertEquals("pai-sa-gem", syllables("paisagem"))
    }

    @Test
    fun hyphenationRespectsMinimumsAndSkipsAcronyms() {
        assertEquals(emptyList(), hyphenationPoints("casa"))           // curta demais
        assertEquals(listOf(4), hyphenationPoints("abraçado"))         // a-bra-ça-do → só "abra-çado"
        assertEquals(emptyList(), hyphenationPoints("UNESCO"))         // sigla
        assertEquals(listOf(2, 6), hyphenationPoints("cachorrada"))  // ca-chor-ra-da (direita mínima 3)
    }

    @Test
    fun numbering() {
        assertEquals("XIV", toRoman(14))
        assertEquals("MMXXVI", toRoman(2026))
        assertEquals("vinte e três", spellOutPt(23))
        assertEquals("cento e um", spellOutPt(101))
        assertEquals("Capítulo um", chapterLabel(ChapterNumberStyle.Extenso, 1))
        assertEquals(null, chapterLabel(ChapterNumberStyle.Nenhum, 3))
    }

    private fun words(vararg w: String, space: Float = 2f): List<LbItem> = buildList {
        w.forEachIndexed { i, word ->
            if (i > 0) add(LbGlue(space, 1f, 0.7f))
            add(LbBox(word.length.toFloat(), listOf(Run(word, FontSpec("Mono"), 2f, word.length.toFloat()))))
        }
        add(LbGlue(0f, LB_INFINITY, 0f))
        add(LbPenalty(0f, -LB_INFINITY, false))
    }

    @Test
    fun knuthPlassJustifiesEveryLineButTheLast() {
        val items = words("aaaa", "bbb", "cccccc", "dd", "eeeee", "ff", "gggg", "hhh", "iiiii", "jj")
        val breaks = assertNotNull(knuthPlassBreaks(items, { 20f }, tolerance = 10f))
        val lines = composeLines(items, breaks, { 20f }, justify = true)
        assertTrue(lines.size >= 3)
        lines.dropLast(1).forEach { assertTrue(abs(it.width - 20f) < 0.01f, "linha com ${it.width}") }
        assertTrue(lines.last().width <= 20f)
        // Todas as palavras aparecem, na ordem.
        assertEquals("aaaa bbb cccccc dd eeeee ff gggg hhh iiiii jj", lines.joinToString(" ") { l -> l.runs.joinToString(" ") { it.run.text } })
    }

    @Test
    fun greedyBreaksOverflowingWordOnItsOwnLine() {
        val items = words("a", "muitolongamesmo", "b")
        val lines = composeLines(items, greedyBreaks(items, { 6f }), { 6f }, justify = false)
        assertEquals(listOf("a", "muitolongamesmo", "b"), lines.map { l -> l.runs.joinToString("") { it.run.text } })
    }

    private fun source(paragraphs: Int, chapters: Int = 2, footnotes: Boolean = false) = TypesetSource(
        title = "O Farol", author = "Marina Duarte",
        chapters = (1..chapters).map { c ->
            TypesetChapter(
                id = "c$c", kind = ChapterKind.Capitulo, title = "Capítulo $c", number = c,
                paragraphs = (1..paragraphs).map { p ->
                    "Parágrafo $p do capítulo $c com algumas palavras para encher a linha e testar a quebra de página." +
                        if (footnotes && p == 3) " Nota aqui[^n1]." else ""
                },
                footnotes = if (footnotes) mapOf("n1" to "Texto da nota de rodapé.") else emptyMap(),
            )
        },
    )

    @Test
    fun chaptersOpenOnRectoAndBookEndsEven() {
        val settings = LayoutSettings(opening = LayoutSettings().opening.copy(side = OpeningSide.Direita))
        val book = Typesetter(settings, MonoMeasurer).typeset(source(paragraphs = 30))
        assertEquals(0, book.pages.size % 2)
        val openings = book.pages.filter { it.kind == PageKind.Abertura }
        assertEquals(2, openings.size)
        assertTrue(openings.all { it.recto }, "aberturas: ${openings.map { it.number }}")
        // Falsa folha (1), branca (2), rosto (3), créditos (4), sumário (5).
        assertEquals(listOf(PageKind.FalsaFolha, PageKind.Branca, PageKind.Rosto, PageKind.Creditos, PageKind.Sumario), book.pages.take(5).map { it.kind })
    }

    @Test
    fun tableOfContentsShowsRealPageNumbers() {
        val book = Typesetter(LayoutSettings(), MonoMeasurer).typeset(source(paragraphs = 30))
        val tocText = book.pages.first { it.kind == PageKind.Sumario }.items.filterIsInstance<TextItem>().map { it.text }
        val c2 = book.firstPageOf("c2")!!
        assertTrue(c2.toString() in tocText, "sumário: $tocText; capítulo 2 na página $c2")
        assertEquals(book.toc.map { it.page }, listOf(book.firstPageOf("c1"), book.firstPageOf("c2")))
    }

    @Test
    fun textStaysInsideTheTextBlockAndHeadsAppearOnlyOnTextPages() {
        val settings = LayoutSettings(front = FrontMatter(halfTitle = false, titlePage = false, credits = false, toc = false))
        val ts = Typesetter(settings, MonoMeasurer)
        val book = ts.typeset(source(paragraphs = 40, chapters = 1))
        val bottomLimit = ts.heightPt - com.cuboide.escriba.core.layout.mmToPt(settings.page.marginBottomMm)
        for (page in book.pages.filter { it.kind == PageKind.Texto }) {
            val body = page.items.filterIsInstance<TextItem>().filter { it.sizePt == settings.body.sizePt }
            assertTrue(body.all { it.y <= bottomLimit + 0.5f }, "página ${page.number} passou do pé")
            assertTrue(page.items.filterIsInstance<TextItem>().any { it.text == page.number.toString() && it.y > bottomLimit }, "fólio na página ${page.number}")
        }
        val opening = book.pages.first { it.kind == PageKind.Abertura }
        assertTrue(opening.items.filterIsInstance<TextItem>().none { it.y > bottomLimit }, "abertura sem fólio")
    }

    @Test
    fun footnoteLandsOnSamePageAsItsReference() {
        val settings = LayoutSettings(front = FrontMatter(halfTitle = false, titlePage = false, credits = false, toc = false))
        val book = Typesetter(settings, MonoMeasurer).typeset(source(paragraphs = 30, chapters = 1, footnotes = true))
        val noteSize = settings.body.sizePt * 0.8f
        val withNote = book.pages.filter { p -> p.items.filterIsInstance<TextItem>().any { it.sizePt == noteSize && it.text.startsWith("Texto") } }
        assertEquals(1, withNote.size)
        val refPage = withNote.single()
        // A chamada (número sobrescrito) está na mesma página.
        assertTrue(refPage.items.filterIsInstance<TextItem>().any { it.text == "1" && it.sizePt < settings.body.sizePt * 0.7f })
    }

    @Test
    fun noWidowLineAtTopOfPage() {
        // Um único parágrafo longo, com tamanhos variados: a última página nunca fica com uma linha só.
        val settings = LayoutSettings(front = FrontMatter(halfTitle = false, titlePage = false, credits = false, toc = false))
        for (words in 300..420 step 7) {
            val text = List(words) { "palavra" }.joinToString(" ")
            val src = TypesetSource("T", "A", listOf(TypesetChapter("c", ChapterKind.Capitulo, "Um", 1, listOf(text))))
            val book = Typesetter(settings, MonoMeasurer).typeset(src)
            val size = settings.body.sizePt
            val bodyPages = book.pages.filter { p -> p.items.filterIsInstance<TextItem>().any { it.sizePt == size } }
            assertTrue(bodyPages.size >= 2, "o parágrafo deveria ocupar mais de uma página ($words palavras)")
            bodyPages.forEach { p ->
                val lines = p.items.filterIsInstance<TextItem>().filter { it.sizePt == size }.map { it.y }.distinct().size
                assertTrue(lines >= 2, "página ${p.number} com $lines linha(s) do parágrafo ($words palavras)")
            }
        }
    }

    @Test
    fun themesKeepBookCredits() {
        val base = LayoutSettings(front = FrontMatter(isbn = "978-85-00000-00-0", publisher = "Editora X"))
        val themed = LayoutTheme.Fantasia.apply(base)
        assertEquals("978-85-00000-00-0", themed.front.isbn)
        assertEquals(3, themed.opening.dropCapLines)
        assertEquals(BodyAlign.Justificado, themed.body.align)
    }

    @Test
    fun dropCapReducesFirstLines() {
        val settings = LayoutSettings(
            front = FrontMatter(halfTitle = false, titlePage = false, credits = false, toc = false),
            opening = LayoutSettings().opening.copy(dropCapLines = 3),
        )
        val book = Typesetter(settings, MonoMeasurer).typeset(source(paragraphs = 5, chapters = 1))
        val opening = book.pages.first { it.kind == PageKind.Abertura }
        val cap = opening.items.filterIsInstance<TextItem>().first { it.text == "P" && it.sizePt > settings.body.sizePt * 2 }
        assertTrue(cap.sizePt > settings.body.sizePt * 2)
    }
}
