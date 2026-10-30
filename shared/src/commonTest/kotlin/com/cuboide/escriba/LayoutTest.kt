package com.cuboide.escriba

import com.cuboide.escriba.core.layout.BodyAlign
import com.cuboide.escriba.core.layout.OpeningFolio
import com.cuboide.escriba.core.layout.HeadPosition
import com.cuboide.escriba.core.layout.HeadInk
import com.cuboide.escriba.core.layout.FolioPages
import com.cuboide.escriba.core.layout.ChapterHead
import kotlin.test.assertFalse
import com.cuboide.escriba.core.storage.EscribaJson
import com.cuboide.escriba.core.layout.mmToPt
import com.cuboide.escriba.core.layout.PageSetup
import com.cuboide.escriba.core.layout.PageMargins
import com.cuboide.escriba.core.layout.MarginEdge
import com.cuboide.escriba.core.layout.MIN_MARGIN_MM
import com.cuboide.escriba.core.layout.MAX_MARGIN_MM
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

    @Test
    fun marginEditsGoToThePageOrToTheWholeBook() {
        val p = PageSetup()
        // Sem mancha própria, a margem muda no livro inteiro (arredondada a 0,5 mm).
        assertEquals(22.5f, p.withMargin(7, MarginEdge.Outer, 22.3f).marginOuterMm)
        val own = p.ownMargins(7).withMargin(7, MarginEdge.Top, 30f)
        assertEquals(30f, own.marginsFor(7).topMm)
        assertEquals(p.marginTopMm, own.marginTopMm)
        assertEquals(own.globalMargins, own.marginsFor(8))
        assertFalse(own.followGlobal(7).hasOwnMargins(7))
        val promoted = own.makeGlobal(7)
        assertEquals(30f, promoted.marginTopMm)
        assertTrue(promoted.pageMargins.isEmpty())
        // Limites: cada margem até 60 mm e a mancha com ao menos 40 mm de largura (14 cm − 40 = 100 mm para as duas).
        assertEquals(MAX_MARGIN_MM, p.withMargin(null, MarginEdge.Inner, 200f).marginInnerMm)
        val wide = p.withMargin(null, MarginEdge.Inner, 60f).withMargin(null, MarginEdge.Outer, 60f)
        assertEquals(60f, wide.marginInnerMm)
        assertEquals(40f, wide.marginOuterMm)
        assertEquals(MIN_MARGIN_MM, p.withMargin(null, MarginEdge.Bottom, 1f).marginBottomMm)
    }

    @Test
    fun pageMarginsSurviveSavingAndThemes() {
        val settings = LayoutSettings(page = PageSetup().ownMargins(12, PageMargins(30f, 25f, 22f, 18f)))
        val json = EscribaJson.encodeToString(LayoutSettings.serializer(), settings)
        assertEquals(settings, EscribaJson.decodeFromString(LayoutSettings.serializer(), json))
        assertEquals(settings.page.pageMargins, LayoutTheme.Elegante.apply(settings).page.pageMargins)
    }

    @Test
    fun pageWithOwnMarginsKeepsItsTextInsideItsBox() {
        val base = LayoutSettings(front = FrontMatter(halfTitle = false, titlePage = false, credits = false, toc = false))
        val sentence = "Naquela manhã o farol amanheceu coberto de areia e ninguém sabia explicar o vento. "
        val long = TypesetSource(
            "O Farol", "Marina Duarte",
            listOf(TypesetChapter("c1", ChapterKind.Capitulo, "Um", 1, List(24) { sentence.repeat(6).trim() })),
        )
        val plain = Typesetter(base, MonoMeasurer).typeset(long)
        val target = plain.pages.filter { it.kind == PageKind.Texto }[2].number
        val settings = base.copy(page = base.page.ownMargins(target, PageMargins(topMm = 40f, bottomMm = 30f, innerMm = 20f, outerMm = 45f)))
        val ts = Typesetter(settings, MonoMeasurer)
        val book = ts.typeset(long)
        val size = settings.body.sizePt
        val textPages = book.pages.filter { it.kind == PageKind.Texto }
        assertTrue(textPages.any { it.number == target })
        for (page in textPages) {
            val m = settings.page.marginsFor(page.number)
            val left = mmToPt(if (page.recto) m.innerMm else m.outerMm)
            val right = ts.widthPt - mmToPt(if (page.recto) m.outerMm else m.innerMm)
            val body = page.items.filterIsInstance<TextItem>().filter { it.sizePt == size }
            assertTrue(body.isNotEmpty(), "página ${page.number} sem texto")
            for (t in body) {
                val end = t.x + MonoMeasurer.width(t.text, t.font, t.sizePt, t.trackingEm)
                assertTrue(t.x >= left - 0.5f && end <= right + 0.5f, "página ${page.number}: “${t.text}” fora da mancha ($left–$right, ${t.x}–$end)")
                assertTrue(t.y >= mmToPt(m.topMm) && t.y <= ts.heightPt - mmToPt(m.bottomMm) + 0.5f, "página ${page.number}: linha fora da altura da mancha")
            }
        }
        // O parágrafo que entra na página estreita é recomposto: as linhas dela são mais curtas que as da vizinha.
        fun longestLine(n: Int) = book.pages.first { it.number == n }.items.filterIsInstance<TextItem>().filter { it.sizePt == size }
            .groupBy { it.y }.values.maxOf { line -> line.maxOf { it.x + MonoMeasurer.width(it.text, it.font, it.sizePt) } - line.minOf { it.x } }
        assertTrue(longestLine(target) < longestLine(target + 2) - mmToPt(20f), "página $target não ficou mais estreita")
    }

    private fun heads(page: com.cuboide.escriba.core.layout.LaidPage, settings: LayoutSettings) =
        page.items.filterIsInstance<TextItem>().filter { it.sizePt == settings.heads.sizePt }

    private fun folios(page: com.cuboide.escriba.core.layout.LaidPage, settings: LayoutSettings) =
        page.items.filterIsInstance<TextItem>().filter { it.sizePt == settings.heads.sizePt + 0.5f }

    @Test
    fun romanFrontMatterRestartsAtTheFirstChapter() {
        val base = LayoutSettings()
        val settings = base.copy(heads = base.heads.copy(romanFrontMatter = true))
        val book = Typesetter(settings, MonoMeasurer).typeset(source(paragraphs = 30))
        val start = book.firstPageOf("c1")!!
        assertEquals(start, book.bodyStart)
        assertTrue(start > 3, "o 1º capítulo deveria vir depois das pré-textuais (página $start)")
        assertEquals("1", book.pages[start - 1].folio)
        assertEquals("iii", book.pages[2].folio)
        val c2 = book.firstPageOf("c2")!!
        assertEquals((c2 - start + 1).toString(), book.pages[c2 - 1].folio)
        // O sumário mostra o número da página como ela sai impressa, não a posição no arquivo.
        val tocText = book.pages.first { it.kind == PageKind.Sumario }.items.filterIsInstance<TextItem>().map { it.text }
        assertTrue("1" in tocText && (c2 - start + 1).toString() in tocText, "sumário: $tocText")
        assertEquals(listOf("1", (c2 - start + 1).toString()), book.toc.map { it.folio })
        // Nas páginas de texto, o fólio impresso é o rótulo.
        val text = book.pages.first { it.kind == PageKind.Texto }
        assertEquals(listOf(text.folio), folios(text, settings).map { it.text })
    }

    @Test
    fun numberingStartsWhereAsked() {
        val base = LayoutSettings(front = FrontMatter(halfTitle = false, titlePage = false, credits = false, toc = false))
        val settings = base.copy(heads = base.heads.copy(startAt = 11))
        val book = Typesetter(settings, MonoMeasurer).typeset(source(paragraphs = 20, chapters = 1))
        book.pages.forEach { assertEquals((it.number + 10).toString(), it.folio) }
    }

    @Test
    fun folioRulesPerSideOpeningAndPage() {
        val base = LayoutSettings(front = FrontMatter(halfTitle = false, titlePage = false, credits = false, toc = false))
        val oddOnly = base.copy(heads = base.heads.copy(folioPages = FolioPages.Impares, openingFolio = OpeningFolio.Pe))
        val book = Typesetter(oddOnly, MonoMeasurer).typeset(source(paragraphs = 40, chapters = 1))
        for (p in book.pages.filter { it.kind == PageKind.Texto }) {
            assertEquals(p.recto, folios(p, oddOnly).isNotEmpty(), "página ${p.number}")
        }
        // Abertura: número centrado no pé, sem cabeçalho.
        val opening = book.pages.first { it.kind == PageKind.Abertura }
        val f = folios(opening, oddOnly).single()
        val ts = Typesetter(oddOnly, MonoMeasurer)
        val m = oddOnly.page.marginsFor(opening.number)
        val left = mmToPt(m.innerMm)
        val textW = ts.widthPt - mmToPt(m.innerMm) - mmToPt(m.outerMm)
        assertEquals(left + textW / 2f, f.x + MonoMeasurer.width(f.text, f.font, f.sizePt) / 2f, 0.5f)
        assertTrue(heads(opening, oddOnly).isEmpty())

        // Uma página sem número e sem cabeçalho continua contando; a seguinte segue normal.
        val target = book.pages.first { it.kind == PageKind.Texto && it.recto }.number
        val hidden = base.copy(heads = base.heads.copy(hiddenFolios = setOf(target), hiddenHeads = setOf(target)))
        val b2 = Typesetter(hidden, MonoMeasurer).typeset(source(paragraphs = 40, chapters = 1))
        assertTrue(folios(b2.pages[target - 1], hidden).isEmpty() && heads(b2.pages[target - 1], hidden).isEmpty())
        assertEquals(listOf((target + 1).toString()), folios(b2.pages[target], hidden).map { it.text })
        assertTrue(heads(b2.pages[target], hidden).isNotEmpty())
    }

    @Test
    fun runningHeadPositionInkAndChapterText() {
        val base = LayoutSettings(front = FrontMatter(halfTitle = false, titlePage = false, credits = false, toc = false))
        val settings = base.copy(
            heads = base.heads.copy(
                position = HeadPosition.Externo, ink = HeadInk.Cinza,
                chapterHeads = mapOf("c1" to ChapterHead(recto = "Meu cabeçalho")),
            ),
        )
        val ts = Typesetter(settings, MonoMeasurer)
        val book = ts.typeset(source(paragraphs = 40, chapters = 1))
        val recto = book.pages.first { it.kind == PageKind.Texto && it.recto }
        val verso = book.pages.first { it.kind == PageKind.Texto && !it.recto }
        val rh = heads(recto, settings).single()
        assertEquals("MEU CABEÇALHO", rh.text)
        assertEquals(HeadInk.Cinza.ink, rh.ink)
        // Canto de fora: na direita o cabeçalho termina na margem externa; na esquerda começa nela.
        val m = settings.page.globalMargins
        val rhEnd = rh.x + MonoMeasurer.width(rh.text, rh.font, rh.sizePt, rh.trackingEm)
        assertEquals(ts.widthPt - mmToPt(m.outerMm), rhEnd, 0.5f)
        assertEquals(mmToPt(m.outerMm), heads(verso, settings).single().x, 0.5f)
        // O tema troca o visual, mas não o que é do livro.
        val themed = LayoutTheme.Elegante.apply(settings.copy(heads = settings.heads.copy(hiddenFolios = setOf(9))))
        assertEquals(setOf(9), themed.heads.hiddenFolios)
        assertEquals(settings.heads.chapterHeads, themed.heads.chapterHeads)
    }
}
