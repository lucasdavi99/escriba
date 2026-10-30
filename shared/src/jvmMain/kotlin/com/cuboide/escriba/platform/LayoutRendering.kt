package com.cuboide.escriba.platform

import com.cuboide.escriba.core.layout.DiamondItem
import com.cuboide.escriba.core.layout.ImageItem
import com.cuboide.escriba.core.layout.LaidOutBook
import com.cuboide.escriba.core.layout.LaidPage
import com.cuboide.escriba.core.layout.RuleItem
import com.cuboide.escriba.core.layout.TextItem
import com.cuboide.escriba.core.layout.mmToPt
import org.apache.fontbox.ttf.TrueTypeCollection
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDPageLabelRange
import org.apache.pdfbox.pdmodel.common.PDPageLabels
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitWidthDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.font.TextAttribute
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.util.Calendar
import javax.imageio.ImageIO
import kotlin.math.ceil

/** PDF do miolo com as fontes embutidas (subconjunto), caixas de corte/sangria e marcadores por capítulo. */
internal object LayoutPdfWriter {
    fun write(book: LaidOutBook, target: File, images: (String) -> ByteArray?) {
        val setup = book.settings.page
        val bleed = mmToPt(setup.bleed.mm)
        val marks = if (setup.cropMarks) mmToPt(10f) else 0f
        val off = bleed + marks
        val w = book.widthPt
        val h = book.heightPt
        val fonts = PdfFontCache()
        PDDocument().use { doc ->
            fun pdFont(face: FontFace): PDFont = fonts.font(doc, face)
            val pdImages = HashMap<String, PDImageXObject?>()
            val pdPages = ArrayList<PDPage>()
            for (laid in book.pages) {
                val page = PDPage(PDRectangle(w + 2 * off, h + 2 * off))
                page.trimBox = PDRectangle(off, off, w, h)
                page.bleedBox = PDRectangle(marks, marks, w + 2 * bleed, h + 2 * bleed)
                doc.addPage(page)
                pdPages += page
                PDPageContentStream(doc, page).use { cs ->
                    fun py(y: Float) = off + h - y
                    for (item in laid.items) when (item) {
                        is TextItem -> {
                            val face = FontCatalog.face(item.font) ?: continue
                            // Cinza do cabeçalho: tom de preto (0 = preto, 1 = branco).
                            cs.setNonStrokingColor(1f - item.ink.coerceIn(0f, 1f))
                            var dx = 0f
                            for ((seg, f) in coverageRuns(item.text, face)) {
                                val font = pdFont(f)
                                cs.beginText()
                                cs.setFont(font, item.sizePt)
                                cs.setCharacterSpacing(item.trackingEm * item.sizePt)
                                cs.newLineAtOffset(off + item.x + dx, py(item.y))
                                showSafely(cs, font, seg)
                                cs.endText()
                                dx += AwtGlyphMeasurer.advance(seg, f, item.sizePt) + item.trackingEm * item.sizePt * seg.length
                            }
                        }
                        is RuleItem -> {
                            cs.setLineWidth(item.widthPt)
                            cs.moveTo(off + item.x1, py(item.y1))
                            cs.lineTo(off + item.x2, py(item.y2))
                            cs.stroke()
                        }
                        is DiamondItem -> {
                            cs.setNonStrokingColor(0f)
                            cs.moveTo(off + item.cx, py(item.cy - item.radius))
                            cs.lineTo(off + item.cx + item.radius, py(item.cy))
                            cs.lineTo(off + item.cx, py(item.cy + item.radius))
                            cs.lineTo(off + item.cx - item.radius, py(item.cy))
                            cs.closePath()
                            cs.fill()
                        }
                        is ImageItem -> {
                            val img = pdImages.getOrPut(item.path) {
                                images(item.path)?.let { runCatching { PDImageXObject.createFromByteArray(doc, it, item.path) }.getOrNull() }
                            } ?: continue
                            cs.drawImage(img, off + item.x, py(item.y + item.height), item.width, item.height)
                        }
                    }
                    if (setup.cropMarks) cropMarks(cs, off, bleed, w, h)
                }
            }
            // Rótulos de página: o leitor de PDF mostra "iii" e "1" como o livro impresso.
            doc.documentCatalog.pageLabels = PDPageLabels(doc).apply {
                val heads = book.settings.heads
                if (heads.romanFrontMatter && book.bodyStart > 1) {
                    setLabelItem(0, PDPageLabelRange().apply { style = PDPageLabelRange.STYLE_ROMAN_LOWER; start = 1 })
                    setLabelItem(book.bodyStart - 1, PDPageLabelRange().apply { style = PDPageLabelRange.STYLE_DECIMAL; start = heads.startAt.coerceAtLeast(1) })
                } else {
                    setLabelItem(0, PDPageLabelRange().apply { style = PDPageLabelRange.STYLE_DECIMAL; start = heads.startAt.coerceAtLeast(1) })
                }
            }
            // Marcadores (índice lateral do leitor de PDF) por capítulo.
            if (book.toc.isNotEmpty()) {
                val outline = PDDocumentOutline()
                for (entry in book.toc) {
                    val target = pdPages.getOrNull(entry.page - 1) ?: continue
                    outline.addLast(PDOutlineItem().apply {
                        title = entry.label
                        destination = PDPageFitWidthDestination().apply { page = target }
                    })
                }
                doc.documentCatalog.documentOutline = outline
            }
            doc.documentInformation.apply {
                title = book.title
                author = book.author
                creator = "Escriba"
                producer = "Escriba (Apache PDFBox)"
                creationDate = Calendar.getInstance()
            }
            doc.save(target)
        }
        fonts.close()
    }

    private fun cropMarks(cs: PDPageContentStream, off: Float, bleed: Float, w: Float, h: Float) {
        val gap = bleed + 2f
        val len = off - gap - 1f
        if (len <= 0f) return
        cs.setLineWidth(0.25f)
        val xs = listOf(off, off + w)
        val ys = listOf(off, off + h)
        for (x in xs) for (y in ys) {
            val dirX = if (x == off) -1 else 1
            val dirY = if (y == off) -1 else 1
            // Traço horizontal e vertical saindo do canto, fora da sangria.
            cs.moveTo(x + dirX * gap, y); cs.lineTo(x + dirX * (gap + len), y)
            cs.moveTo(x, y + dirY * gap); cs.lineTo(x, y + dirY * (gap + len))
        }
        cs.stroke()
    }
}

/** Fontes do PDF carregadas uma vez por face (subconjunto embutido); fecha as coleções .ttc no fim. */
internal class PdfFontCache : AutoCloseable {
    private val fonts = HashMap<FontFace, PDFont>()
    private val collections = ArrayList<TrueTypeCollection>()

    fun font(doc: PDDocument, face: FontFace): PDFont = fonts.getOrPut(face) {
        if (face.isCollection) {
            val ttc = TrueTypeCollection(face.file).also { collections += it }
            PDType0Font.load(doc, ttc.getFontByName(face.postScriptName), true)
        } else {
            PDType0Font.load(doc, face.file)
        }
    }

    override fun close() = collections.forEach { runCatching { it.close() } }
}

/** Escreve o texto; caracteres que a fonte não codifica viram "?" em vez de abortar o PDF. */
internal fun showSafely(cs: PDPageContentStream, font: PDFont, text: String) {
    try {
        cs.showText(text)
    } catch (_: IllegalArgumentException) {
        val safe = buildString {
            text.forEach { ch -> append(if (runCatching { font.encode(ch.toString()) }.isSuccess) ch else '?') }
        }
        runCatching { cs.showText(safe) }
    }
}

/** Imagens do livro já decodificadas para a prévia (miolo e capa). */
internal object PreviewImages {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, BufferedImage>()

    fun get(path: String, images: (String) -> ByteArray?): BufferedImage? = cache[path] ?: images(path)?.let { bytes ->
        runCatching { ImageIO.read(ByteArrayInputStream(bytes)) }.getOrNull()
    }?.also { cache[path] = it }
}

/** Desenha uma página do layout numa imagem, com as mesmas fontes e posições do PDF. */
internal object LayoutPagePainter {
    fun paint(book: LaidOutBook, page: LaidPage, pxPerPt: Float, paper: Color, showGuides: Boolean, images: (String) -> ByteArray?): BufferedImage {
        val wPx = ceil(book.widthPt * pxPerPt).toInt().coerceAtLeast(1)
        val hPx = ceil(book.heightPt * pxPerPt).toInt().coerceAtLeast(1)
        val img = BufferedImage(wPx, hPx, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            g.color = paper
            g.fillRect(0, 0, wPx, hPx)
            g.scale(pxPerPt.toDouble(), pxPerPt.toDouble())
            if (showGuides) {
                val m = book.settings.page.marginsFor(page.number)
                val inner = mmToPt(m.innerMm)
                val outer = mmToPt(m.outerMm)
                val left = if (page.recto) inner else outer
                g.color = Color(120, 150, 200, 110)
                g.stroke = BasicStroke(0.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, floatArrayOf(2f, 2f), 0f)
                g.draw(Rectangle2D.Float(left, mmToPt(m.topMm), book.widthPt - inner - outer, book.heightPt - mmToPt(m.topMm) - mmToPt(m.bottomMm)))
            }
            val ink = Color(0x1C, 0x1A, 0x18)
            g.color = ink
            for (item in page.items) when (item) {
                is TextItem -> {
                    val face = FontCatalog.face(item.font) ?: continue
                    g.color = if (item.ink >= 1f) ink else mix(ink, paper, item.ink)
                    var dx = 0f
                    for ((seg, f) in coverageRuns(item.text, face)) {
                        var font = f.awt.deriveFont(item.sizePt)
                        if (item.trackingEm != 0f) font = font.deriveFont(mapOf(TextAttribute.TRACKING to item.trackingEm))
                        g.font = font
                        g.drawString(seg, item.x + dx, item.y)
                        dx += AwtGlyphMeasurer.advance(seg, f, item.sizePt) + item.trackingEm * item.sizePt * seg.length
                    }
                }
                is RuleItem -> {
                    g.color = ink
                    g.stroke = BasicStroke(item.widthPt)
                    g.draw(Line2D.Float(item.x1, item.y1, item.x2, item.y2))
                }
                is DiamondItem -> {
                    g.color = ink
                    val p = Path2D.Float()
                    p.moveTo(item.cx, item.cy - item.radius)
                    p.lineTo(item.cx + item.radius, item.cy)
                    p.lineTo(item.cx, item.cy + item.radius)
                    p.lineTo(item.cx - item.radius, item.cy)
                    p.closePath()
                    g.fill(p)
                }
                is ImageItem -> {
                    val bi = PreviewImages.get(item.path, images)
                    if (bi != null) g.drawImage(bi, item.x.toInt(), item.y.toInt(), item.width.toInt(), item.height.toInt(), null)
                    else {
                        g.color = Color(0, 0, 0, 40)
                        g.fill(Rectangle2D.Float(item.x, item.y, item.width, item.height))
                        g.color = ink
                    }
                }
            }
        } finally {
            g.dispose()
        }
        return img
    }
}

/** [a] com [amount] de força sobre [b] (1 = só [a]). */
private fun mix(a: Color, b: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    fun ch(x: Int, y: Int) = (x * t + y * (1f - t)).toInt().coerceIn(0, 255)
    return Color(ch(a.red, b.red), ch(a.green, b.green), ch(a.blue, b.blue))
}

internal fun imageSizeOf(bytes: ByteArray): Pair<Int, Int>? = runCatching {
    ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use { input ->
        val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull() ?: return null
        try {
            reader.input = input
            reader.getWidth(0) to reader.getHeight(0)
        } finally {
            reader.dispose()
        }
    }
}.getOrNull()
