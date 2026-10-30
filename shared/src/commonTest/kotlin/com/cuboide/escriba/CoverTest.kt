package com.cuboide.escriba

import com.cuboide.escriba.core.layout.BleedPreset
import com.cuboide.escriba.core.layout.ptToMm
import com.cuboide.escriba.core.layout.ebookCoverPixels
import com.cuboide.escriba.core.layout.contrastRatio
import com.cuboide.escriba.core.layout.checkLayout
import com.cuboide.escriba.core.layout.TextTurn
import com.cuboide.escriba.core.layout.SpineDirection
import com.cuboide.escriba.core.layout.MmOffset
import com.cuboide.escriba.core.layout.EbookCoverSize
import com.cuboide.escriba.core.layout.CoverPiece
import com.cuboide.escriba.core.layout.CoverOverlay
import com.cuboide.escriba.core.layout.CoverExportRecord
import com.cuboide.escriba.core.layout.COVER_GRADIENTS
import com.cuboide.escriba.core.layout.CoverPanel
import com.cuboide.escriba.core.layout.CoverSettings
import com.cuboide.escriba.core.layout.CoverText
import com.cuboide.escriba.core.layout.FontSpec
import com.cuboide.escriba.core.layout.GlyphMeasurer
import com.cuboide.escriba.core.layout.IsbnCheck
import com.cuboide.escriba.core.layout.LaidOutBook
import com.cuboide.escriba.core.layout.LaidPage
import com.cuboide.escriba.core.layout.LayoutSettings
import com.cuboide.escriba.core.layout.LayoutTheme
import com.cuboide.escriba.core.layout.PageKind
import com.cuboide.escriba.core.layout.PaperStock
import com.cuboide.escriba.core.layout.TrimSize
import com.cuboide.escriba.core.layout.checkIsbn
import com.cuboide.escriba.core.layout.coverGeometry
import com.cuboide.escriba.core.layout.ean13CheckDigit
import com.cuboide.escriba.core.layout.ean13Modules
import com.cuboide.escriba.core.layout.isbn10CheckDigit
import com.cuboide.escriba.core.layout.layoutCover
import com.cuboide.escriba.core.layout.mmToPt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private object CoverMeasurer : GlyphMeasurer {
    override fun width(text: String, font: FontSpec, sizePt: Float, trackingEm: Float) =
        text.length * sizePt * 0.5f + trackingEm * sizePt * text.length
    override fun resolveFamily(candidates: List<String>) = candidates.firstOrNull() ?: "Mono"
    override fun hasFamily(family: String) = true
}

private fun fakeBook(pages: Int, settings: LayoutSettings = LayoutSettings()) = LaidOutBook(
    settings = settings,
    widthPt = mmToPt(settings.page.trim.widthMm),
    heightPt = mmToPt(settings.page.trim.heightMm),
    pages = List(pages) { LaidPage(it + 1, PageKind.Texto, null, emptyList()) },
    toc = emptyList(),
    warnings = emptyList(),
    bodyFamily = "Mono",
    titleFamily = "Mono",
    title = "O Farol de Areia",
    author = "Marina Duarte",
)

private const val VALID_ISBN = "978-85-0000-000-3"

class CoverTest {
    @Test
    fun ean13CheckDigits() {
        // Exemplos conferidos à mão: soma ponderada 1-3 completando a dezena.
        assertEquals(3, ean13CheckDigit("978850000000"))
        assertEquals(7, ean13CheckDigit("978030640615"))
        assertEquals('2', isbn10CheckDigit("030640615"))
        assertEquals('X', isbn10CheckDigit("080442957"))
    }

    @Test
    fun isbnValidation() {
        assertEquals(IsbnCheck.Empty, checkIsbn("  "))
        assertEquals(IsbnCheck.Valid("9788500000003"), checkIsbn(VALID_ISBN))
        assertEquals(IsbnCheck.Valid("9780306406157"), checkIsbn("ISBN 978 0 306 40615 7"))
        // ISBN-10 vira EAN 978 com novo dígito verificador.
        assertEquals(IsbnCheck.Valid("9780306406157"), checkIsbn("0-306-40615-2"))
        assertEquals(IsbnCheck.Valid("9780804429573"), checkIsbn("080442957X"))
        val wrong = checkIsbn("978-85-0000-000-2")
        assertIs<IsbnCheck.Invalid>(wrong)
        assertTrue("deveria ser 3" in wrong.reason, wrong.reason)
        assertIs<IsbnCheck.Invalid>(checkIsbn("977-85-0000-000-3"))
        assertIs<IsbnCheck.Invalid>(checkIsbn("12345"))
        assertIs<IsbnCheck.Invalid>(checkIsbn("97885000X0001"))
        assertIs<IsbnCheck.Invalid>(checkIsbn("978-85-abc"))
    }

    @Test
    fun ean13Encoding() {
        val m = ean13Modules("9780306406157")
        assertEquals(95, m.size)
        val bits = m.joinToString("") { if (it) "1" else "0" }
        assertTrue(bits.startsWith("101") && bits.endsWith("101"))
        assertEquals("01010", bits.substring(45, 50))
        // 1º dígito 9 → paridade LGGLGL; o 2º dígito (7) usa L: 0111011.
        assertEquals("0111011", bits.substring(3, 10))
        // 3º dígito (8) usa G: 0001001.
        assertEquals("0001001", bits.substring(10, 17))
        // Último dígito (7) na metade direita usa R: 1000100.
        assertEquals("1000100", bits.substring(85, 92))
        // Cada dígito tem duas barras e dois espaços: 30 barras + 3 guardas (2+2+2 barras).
        val bars = Regex("1+").findAll(bits).count()
        assertEquals(30, bars)
    }

    @Test
    fun geometryAddsSpineAndFlaps() {
        val settings = LayoutSettings().let { it.copy(page = it.page.copy(trim = TrimSize.Br14x21, paper = PaperStock.Polen80)) }
        val book = fakeBook(200, settings)
        assertEquals(12f, book.spineMm, 0.001f)
        val g = coverGeometry(book, CoverSettings(flapMm = 80f, bleed = BleedPreset.Nacional))
        assertEquals(2 * 140f + 12f + 2 * 80f, g.widthMm, 0.001f)
        assertEquals(210f, g.heightMm)
        assertEquals(listOf(CoverPanel.OrelhaVerso, CoverPanel.Contracapa, CoverPanel.Lombada, CoverPanel.Capa, CoverPanel.OrelhaCapa), g.zones.map { it.panel })
        listOf(80f, 220f, 232f, 372f).zip(g.foldsMm).forEach { (want, got) -> assertEquals(want, got, 0.001f) }
        assertEquals(5f, g.bleedMm)
        val noFlaps = coverGeometry(book, CoverSettings(flapMm = 0f, marks = false))
        assertEquals(292f, noFlaps.widthMm, 0.001f)
        listOf(140f, 152f).zip(noFlaps.foldsMm).forEach { (want, got) -> assertEquals(want, got, 0.001f) }
        assertEquals(2, noFlaps.foldsMm.size)
        assertEquals(0f, noFlaps.slugMm)
    }

    @Test
    fun preflightBlocksMissingArtInvalidIsbnAndOddPages() {
        val blank = layoutCover(fakeBook(201), CoverSettings(), "978-85-0000-000-2", CoverMeasurer, null)
        assertFalse(blank.canExport)
        val blocking = blank.issues.filter { it.blocking }.joinToString(" | ") { it.message }
        assertTrue("Sem arte" in blocking, blocking)
        assertTrue("ISBN inválido" in blocking, blocking)
        assertTrue("201 páginas" in blocking, blocking)

        val ok = layoutCover(fakeBook(200), CoverSettings(image = "assets/capa.jpg"), VALID_ISBN, CoverMeasurer, 4200 to 3000)
        assertTrue(ok.canExport, ok.issues.toString())
        // Sem código de barras, o ISBN em branco não bloqueia.
        assertTrue(layoutCover(fakeBook(200), CoverSettings(image = "a.png", barcode = false), "", CoverMeasurer, 4200 to 3000).canExport)
    }

    @Test
    fun lowResolutionArtWarnsWithoutBlocking() {
        val book = fakeBook(200)
        val cover = layoutCover(book, CoverSettings(image = "assets/capa.png"), VALID_ISBN, CoverMeasurer, 1200 to 800)
        assertTrue(cover.canExport)
        val warn = cover.issues.single { "ppi" in it.message }
        assertFalse(warn.blocking)
        // A arte cobre a capa aberta inteira, sangria incluída, sem distorcer.
        val img = cover.image!!
        val g = cover.geometry
        assertTrue(img.x <= -g.bleedPt + 0.01f && img.y <= -g.bleedPt + 0.01f)
        assertTrue(img.x + img.width >= g.widthPt + g.bleedPt - 0.01f && img.y + img.height >= g.heightPt + g.bleedPt - 0.01f)
        assertEquals(1200f / 800f, img.width / img.height, 0.001f)
        // 1200 px em ~31 cm de largura com sangria dá bem menos de 300 ppi.
        assertTrue(img.ppi < 120f)
        val sharp = layoutCover(book, CoverSettings(image = "a.png"), VALID_ISBN, CoverMeasurer, 4200 to 2800)
        assertTrue(sharp.issues.none { "ppi" in it.message }, sharp.issues.toString())
    }

    @Test
    fun textGoesOnFrontSpineAndBack() {
        val book = fakeBook(300)
        val cover = layoutCover(book, CoverSettings(image = "a.png", backText = "Uma vila acorda com o farol coberto de areia.\nE o mar recua."), VALID_ISBN, CoverMeasurer, 4200 to 2800)
        val g = cover.geometry
        val front = g.zone(CoverPanel.Capa)!!
        val spine = g.zone(CoverPanel.Lombada)!!
        val back = g.zone(CoverPanel.Contracapa)!!
        val texts = cover.items.filterIsInstance<CoverText>()
        fun inZone(t: CoverText, z: com.cuboide.escriba.core.layout.CoverZone) = t.x >= mmToPt(z.xMm) - 1f && t.x <= mmToPt(z.endMm) + 1f
        assertTrue(texts.any { !it.vertical && it.text == "O Farol de Areia" && inZone(it, front) })
        assertTrue(texts.any { !it.vertical && it.text == "Marina Duarte" && inZone(it, front) })
        val spineTexts = texts.filter { it.vertical }
        assertEquals(listOf("O Farol de Areia", "Marina Duarte"), spineTexts.map { it.text })
        assertTrue(spineTexts.all { inZone(it, spine) })
        assertTrue(texts.any { it.text.startsWith("Uma vila") && inZone(it, back) })
        // Dígitos do código de barras na contracapa.
        assertTrue(texts.any { it.text.startsWith("ISBN 978-85") && inZone(it, back) })
    }

    @Test
    fun frontTitleAndAuthorCanBeHidden() {
        val book = fakeBook(300)
        val cover = layoutCover(book, CoverSettings(image = "a.png", frontTitle = false, frontAuthor = false), VALID_ISBN, CoverMeasurer, 4200 to 2800)
        val front = cover.geometry.zone(CoverPanel.Capa)!!
        val onFront = cover.items.filterIsInstance<CoverText>().filter { !it.vertical && it.x >= mmToPt(front.xMm) - 1f }
        assertTrue(onFront.isEmpty(), onFront.toString())
        // A lombada continua com título e autor.
        assertEquals(2, cover.items.filterIsInstance<CoverText>().count { it.vertical })
        val onlyAuthor = layoutCover(book, CoverSettings(image = "a.png", frontTitle = false), VALID_ISBN, CoverMeasurer, 4200 to 2800)
        val texts = onlyAuthor.items.filterIsInstance<CoverText>().filter { !it.vertical }
        assertTrue(texts.none { it.text == "O Farol de Areia" } && texts.any { it.text == "Marina Duarte" })
    }

    @Test
    fun thinSpineSkipsText() {
        val cover = layoutCover(fakeBook(60), CoverSettings(image = "a.png"), VALID_ISBN, CoverMeasurer, 4200 to 2800)
        assertTrue(cover.items.filterIsInstance<CoverText>().none { it.vertical })
        assertTrue(cover.issues.any { "estreita" in it.message && !it.blocking })
    }

    @Test
    fun marksSitOutsideBleed() {
        val cover = layoutCover(fakeBook(200), CoverSettings(image = "a.png", flapMm = 70f), VALID_ISBN, CoverMeasurer, 4200 to 2800)
        val g = cover.geometry
        // 4 cantos × 2 traços + 4 dobras × 2 (alto e pé).
        assertEquals(8 + 8, cover.marks.size)
        for (line in cover.marks) {
            val outsideX = minOf(line.x1, line.x2) >= g.widthPt + g.bleedPt || maxOf(line.x1, line.x2) <= -g.bleedPt
            val outsideY = minOf(line.y1, line.y2) >= g.heightPt + g.bleedPt || maxOf(line.y1, line.y2) <= -g.bleedPt
            assertTrue(outsideX || outsideY, "marca dentro da sangria: $line")
        }
        // Traços verticais acima da capa: 2 cantos + 4 dobras.
        assertEquals(2 + 4, cover.marks.count { it.x1 == it.x2 && it.y1 < 0 })
    }

    @Test
    fun themeKeepsCover() {
        val base = LayoutSettings(cover = CoverSettings(flapMm = 90f, backText = "Sinopse"))
        assertEquals(base.cover, LayoutTheme.Bolso.apply(base).cover)
    }

    @Test
    fun defaultCoverWithArtHasNoIssues() {
        val cover = layoutCover(fakeBook(300), CoverSettings(image = "a.png", backText = "Uma vila acorda com o farol coberto de areia."), VALID_ISBN, CoverMeasurer, 4200 to 2800)
        assertTrue(cover.issues.isEmpty(), cover.issues.toString())
        assertEquals(setOf(CoverPiece.Titulo, CoverPiece.Autor, CoverPiece.LombadaTitulo, CoverPiece.LombadaAutor, CoverPiece.Sinopse, CoverPiece.CodigoBarras), cover.pieces.keys)
    }

    @Test
    fun artZoomAndOffsetFrameTheArt() {
        val book = fakeBook(300)
        val full = layoutCover(book, CoverSettings(image = "a.png"), VALID_ISBN, CoverMeasurer, 4200 to 2800).image!!
        val small = layoutCover(book, CoverSettings(image = "a.png", artZoom = 0.8f), VALID_ISBN, CoverMeasurer, 4200 to 2800)
        val img = small.image!!
        assertEquals(full.width * 0.8f, img.width, 0.5f)
        assertEquals(full.ppi / 0.8f, img.ppi, 0.5f)
        assertTrue(small.issues.any { "não chega ao corte" in it.message && !it.blocking }, small.issues.toString())
        // Ampliada, a arte anda com o arraste mas nunca deixa vão na sangria.
        val g = small.geometry
        val big = layoutCover(book, CoverSettings(image = "a.png", artZoom = 1.5f, artOffsetXMm = 9999f, artOffsetYMm = -9999f), VALID_ISBN, CoverMeasurer, 4200 to 2800)
        val b = big.image!!
        assertTrue(b.x <= -g.bleedPt + 0.01f && b.x + b.width >= g.widthPt + g.bleedPt - 0.01f)
        assertTrue(b.y <= -g.bleedPt + 0.01f && b.y + b.height >= g.heightPt + g.bleedPt - 0.01f)
        assertEquals(-g.bleedPt, b.x, 0.5f) // encostada à esquerda: foi arrastada toda para a direita
    }

    @Test
    fun draggedPiecesMoveAndAreChecked() {
        val book = fakeBook(300)
        val base = layoutCover(book, CoverSettings(image = "a.png"), VALID_ISBN, CoverMeasurer, 4200 to 2800)
        val moved = layoutCover(book, CoverSettings(image = "a.png", offsets = mapOf(CoverPiece.Titulo to MmOffset(5f, 10f), CoverPiece.LombadaTitulo to MmOffset(30f, 4f))), VALID_ISBN, CoverMeasurer, 4200 to 2800)
        val a = base.pieces.getValue(CoverPiece.Titulo)
        val b = moved.pieces.getValue(CoverPiece.Titulo)
        assertEquals(a.x + mmToPt(5f), b.x, 0.01f)
        assertEquals(a.y + mmToPt(10f), b.y, 0.01f)
        // Na lombada a peça só anda no comprimento dela.
        assertEquals(base.pieces.getValue(CoverPiece.LombadaTitulo).x, moved.pieces.getValue(CoverPiece.LombadaTitulo).x, 0.01f)
        assertEquals(base.pieces.getValue(CoverPiece.LombadaTitulo).y + mmToPt(4f), moved.pieces.getValue(CoverPiece.LombadaTitulo).y, 0.01f)

        val nearCut = layoutCover(book, CoverSettings(image = "a.png", offsets = mapOf(CoverPiece.Titulo to MmOffset(60f, 0f))), VALID_ISBN, CoverMeasurer, 4200 to 2800)
        assertTrue(nearCut.issues.any { it.message.startsWith("Título: está perto do corte") })
        // Arrastar o autor para cima do título avisa da sobreposição.
        val dy = ptToMm(a.y - base.pieces.getValue(CoverPiece.Autor).y)
        val over = layoutCover(book, CoverSettings(image = "a.png", offsets = mapOf(CoverPiece.Autor to MmOffset(0f, dy))), VALID_ISBN, CoverMeasurer, 4200 to 2800)
        assertTrue(over.issues.any { "por cima de “Título”" in it.message }, over.issues.toString())
    }

    @Test
    fun spineCanReadUpwards() {
        val cover = layoutCover(fakeBook(300), CoverSettings(image = "a.png", spineDirection = SpineDirection.Ascendente), VALID_ISBN, CoverMeasurer, 4200 to 2800)
        val spine = cover.items.filterIsInstance<CoverText>().filter { it.vertical }
        assertTrue(spine.isNotEmpty() && spine.all { it.turn == TextTurn.Sobe })
        val z = cover.geometry.zone(CoverPanel.Lombada)!!
        for (p in listOf(CoverPiece.LombadaTitulo, CoverPiece.LombadaAutor)) {
            val r = cover.pieces.getValue(p)
            assertTrue(r.x >= mmToPt(z.xMm) && r.right <= mmToPt(z.endMm), "$p fora da lombada: $r")
        }
        assertTrue(cover.issues.isEmpty(), cover.issues.toString())
    }

    @Test
    fun gradientCountsAsArtAndContrastIsChecked() {
        val noite = COVER_GRADIENTS.first().second
        val ok = layoutCover(fakeBook(300), CoverSettings(gradient = noite), VALID_ISBN, CoverMeasurer, null)
        assertTrue(ok.canExport, ok.issues.toString())
        assertEquals(noite, ok.gradient)
        val dim = layoutCover(fakeBook(300), CoverSettings(gradient = noite, ink = 0xFF35507AL), VALID_ISBN, CoverMeasurer, null)
        assertTrue(dim.issues.any { "contraste" in it.message })
        assertTrue(contrastRatio(0xFFFFFFFFL, 0xFF000000L) > 20f)
    }

    @Test
    fun overlaysExportRecordAndPodFlaps() {
        val book = fakeBook(300)
        val logo = CoverOverlay("l1", "assets/logo.png", xMm = 20f, yMm = 180f, widthMm = 25f)
        val cover = layoutCover(book, CoverSettings(image = "a.png", overlays = listOf(logo)), VALID_ISBN, CoverMeasurer, 4200 to 2800) { 200 to 100 }
        val o = cover.overlays.single()
        assertEquals(mmToPt(25f) / 2, o.height, 0.01f)
        assertFalse(o.clip)
        assertTrue(cover.issues.any { "Imagem solta com" in it.message })
        val missing = layoutCover(book, CoverSettings(image = "a.png", overlays = listOf(logo)), VALID_ISBN, CoverMeasurer, 4200 to 2800)
        assertTrue(missing.overlays.isEmpty() && missing.issues.any { "não foi encontrada" in it.message })

        val stale = CoverSettings(image = "a.png", lastExport = CoverExportRecord(200, 12f))
        assertTrue(layoutCover(book, stale, VALID_ISBN, CoverMeasurer, 4200 to 2800).issues.any { "Gere a capa de novo" in it.message })
        val settings = LayoutSettings(cover = stale)
        assertTrue(checkLayout(fakeBook(300, settings)).any { "Gere a capa de novo" in it.message })

        val kdp = LayoutSettings().let { it.copy(page = it.page.copy(trim = TrimSize.In6x9)) }
        assertTrue(layoutCover(fakeBook(300, kdp), CoverSettings(image = "a.png", flapMm = 80f), VALID_ISBN, CoverMeasurer, 4200 to 2800).issues.any { "orelha" in it.message })
    }

    @Test
    fun ebookPixels() {
        val g = coverGeometry(fakeBook(300), CoverSettings())
        assertEquals(1600 to 2400, ebookCoverPixels(EbookCoverSize.Livro, g))
        assertEquals(1600 to 2560, ebookCoverPixels(EbookCoverSize.Kindle, g))
    }
}
