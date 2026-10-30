package com.cuboide.escriba

import com.cuboide.escriba.core.layout.BleedPreset
import com.cuboide.escriba.core.layout.SpineDirection
import com.cuboide.escriba.core.layout.EbookCoverSize
import com.cuboide.escriba.core.layout.CoverOverlay
import com.cuboide.escriba.core.layout.COVER_GRADIENTS
import com.cuboide.escriba.core.layout.CoverRect
import com.cuboide.escriba.core.layout.CoverSettings
import com.cuboide.escriba.core.layout.LaidPage
import com.cuboide.escriba.core.layout.LayoutSettings
import com.cuboide.escriba.core.layout.LayoutTheme
import com.cuboide.escriba.core.layout.PageKind
import com.cuboide.escriba.core.layout.Typesetter
import com.cuboide.escriba.core.layout.layoutCover
import com.cuboide.escriba.core.layout.mmToPt
import com.cuboide.escriba.platform.imagePixelSize
import com.cuboide.escriba.platform.layoutMeasurer
import com.cuboide.escriba.platform.CoverPainter
import com.cuboide.escriba.platform.writeCoverPdf
import com.cuboide.escriba.platform.writeEbookCover
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.Color
import java.awt.GradientPaint
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoverPdfTest {
    private fun artPng(w: Int, h: Int): ByteArray {
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.paint = GradientPaint(0f, 0f, Color(30, 50, 80), w.toFloat(), h.toFloat(), Color(190, 120, 60))
        g.fillRect(0, 0, w, h)
        g.dispose()
        return ByteArrayOutputStream().also { ImageIO.write(img, "png", it) }.toByteArray()
    }

    @Test
    fun writesCoverPdfWithSpreadTrimBox() {
        val settings = LayoutTheme.Classico.apply(LayoutSettings())
        val typeset = Typesetter(settings, layoutMeasurer()).typeset(sampleSource)
        // Completa até 240 páginas para a lombada ter largura de texto.
        val book = typeset.copy(pages = typeset.pages + List(240 - typeset.pages.size) { LaidPage(typeset.pages.size + it + 1, PageKind.Branca, null, emptyList()) })
        val art = artPng(3900, 2700)
        val coverSettings = CoverSettings(
            flapMm = 80f, bleed = BleedPreset.Nacional, marks = true, image = "assets/capa.png",
            backText = "Naquela manhã o farol amanheceu coberto de areia.\nNinguém na vila sabia explicar como o vento tinha subido tão alto.",
        )
        val cover = layoutCover(book, coverSettings, "978-85-0000-000-3", layoutMeasurer(), imagePixelSize(art))
        assertTrue(cover.canExport, cover.issues.toString())
        val out = File(System.getProperty("java.io.tmpdir"), "escriba-capa-test.pdf")
        writeCoverPdf(cover, out.path) { if (it == "assets/capa.png") art else null }

        val trim = settings.page.trim
        val expectedWidthMm = 2 * trim.widthMm + book.spineMm + 2 * 80f
        Loader.loadPDF(out).use { doc ->
            assertEquals(1, doc.numberOfPages)
            val page = doc.getPage(0)
            assertEquals(mmToPt(expectedWidthMm), page.trimBox.width, 0.5f)
            assertEquals(mmToPt(trim.heightMm), page.trimBox.height, 0.5f)
            assertEquals(mmToPt(expectedWidthMm + 2 * 5f), page.bleedBox.width, 0.5f)
            // Marcas ficam fora da sangria: a mídia é maior que a caixa de sangria.
            assertTrue(page.mediaBox.width > page.bleedBox.width + mmToPt(10f))
            val fonts = page.resources.fontNames.map { page.resources.getFont(it) }
            assertTrue(fonts.isNotEmpty() && fonts.all { it.isEmbedded }, "fontes: ${fonts.map { it.name }}")
            assertEquals(1, page.resources.xObjectNames.count())
            System.getProperty("escriba.layoutPreviewDir")?.let { dir ->
                val dpi = 300f
                val full = PDFRenderer(doc).renderImageWithDPI(0, dpi)
                ImageIO.write(full, "png", File(dir, "capa-pdf.png"))
                // Recorte do código de barras (o retângulo branco) para conferir as barras e os dígitos.
                val box = cover.items.filterIsInstance<CoverRect>().first { it.color == 0xFFFFFFFFL }
                val k = dpi / 72f
                val off = cover.geometry.bleedPt + cover.geometry.slugPt
                ImageIO.write(full.getSubimage(((off + box.x) * k).toInt(), ((off + box.y) * k).toInt(), (box.width * k).toInt(), (box.height * k).toInt()), "png", File(dir, "capa-codigo.png"))
            }
        }
        // A prévia desenha a mesma capa, com a sangria (o ImageBitmap do Compose não carrega neste teste).
        val preview = CoverPainter.paint(cover, 0.5f, true) { art }
        assertEquals(kotlin.math.ceil((cover.geometry.widthPt + 2 * cover.geometry.bleedPt) * 0.5f).toInt(), preview.width)
        System.getProperty("escriba.layoutPreviewDir")?.let { dir -> ImageIO.write(preview, "png", File(dir, "capa-previa.png")) }
    }

    @Test
    fun gradientCoverAndEbookJpeg() {
        val settings = LayoutTheme.Classico.apply(LayoutSettings())
        val typeset = Typesetter(settings, layoutMeasurer()).typeset(sampleSource)
        val book = typeset.copy(pages = typeset.pages + List(240 - typeset.pages.size) { LaidPage(typeset.pages.size + it + 1, PageKind.Branca, null, emptyList()) })
        val logo = artPng(400, 200)
        val coverSettings = CoverSettings(
            gradient = COVER_GRADIENTS.first { it.first == "Aurora" }.second,
            spineDirection = SpineDirection.Ascendente,
            overlays = listOf(CoverOverlay("l", "assets/logo.png", 20f, 185f, 20f)),
            backText = "Naquela manhã o farol amanheceu coberto de areia.",
        )
        val cover = layoutCover(book, coverSettings, "978-85-0000-000-3", layoutMeasurer(), null) { if (it == "assets/logo.png") 400 to 200 else null }
        assertTrue(cover.canExport, cover.issues.toString())
        val images: (String) -> ByteArray? = { if (it == "assets/logo.png") logo else null }
        val out = File(System.getProperty("java.io.tmpdir"), "escriba-capa-degrade.pdf")
        writeCoverPdf(cover, out.path, images)
        Loader.loadPDF(out).use { doc ->
            val page = doc.getPage(0)
            assertEquals(1, page.resources.shadingNames.count())
            assertEquals(1, page.resources.xObjectNames.count())
            System.getProperty("escriba.layoutPreviewDir")?.let { dir ->
                ImageIO.write(PDFRenderer(doc).renderImageWithDPI(0, 60f), "png", File(dir, "capa-degrade-pdf.png"))
            }
        }
        val jpg = File(System.getProperty("java.io.tmpdir"), "escriba-capa-ebook.jpg")
        writeEbookCover(cover, jpg.path, EbookCoverSize.Kindle, images)
        val read = ImageIO.read(jpg)
        assertEquals(1600, read.width)
        assertEquals(2560, read.height)
        System.getProperty("escriba.layoutPreviewDir")?.let { dir -> jpg.copyTo(File(dir, "capa-ebook.jpg"), overwrite = true) }
    }
}
