package com.cuboide.escriba.platform

import com.cuboide.escriba.core.layout.CoverGradient
import com.cuboide.escriba.core.layout.CoverImage
import com.cuboide.escriba.core.layout.CoverLayout
import com.cuboide.escriba.core.layout.CoverPanel
import com.cuboide.escriba.core.layout.CoverRect
import com.cuboide.escriba.core.layout.CoverText
import com.cuboide.escriba.core.layout.COVER_SAFE_MM
import com.cuboide.escriba.core.layout.EbookCoverSize
import com.cuboide.escriba.core.layout.PtRect
import com.cuboide.escriba.core.layout.TextTurn
import com.cuboide.escriba.core.layout.ebookCoverPixels
import com.cuboide.escriba.core.layout.mmToPt
import org.apache.pdfbox.cos.COSArray
import org.apache.pdfbox.cos.COSBoolean
import org.apache.pdfbox.cos.COSDictionary
import org.apache.pdfbox.cos.COSFloat
import org.apache.pdfbox.cos.COSInteger
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.common.function.PDFunctionType2
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType2
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType3
import org.apache.pdfbox.util.Matrix
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.LinearGradientPaint
import java.awt.RadialGradientPaint
import java.awt.RenderingHints
import java.awt.font.TextAttribute
import java.awt.geom.AffineTransform
import java.awt.geom.Line2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import java.util.Calendar
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

internal fun argbColor(argb: Long) = Color(((argb shr 16) and 0xFF).toInt(), ((argb shr 8) and 0xFF).toInt(), (argb and 0xFF).toInt())

/** Pontas do degradê sobre [area] (em pt, y para baixo): início e fim da linha, ou centro e raio no radial. */
private class GradientGeometry(val x0: Float, val y0: Float, val x1: Float, val y1: Float, val radius: Float)

private fun gradientGeometry(g: CoverGradient, area: PtRect): GradientGeometry {
    val cx = area.x + area.width / 2
    val cy = area.y + area.height / 2
    if (g.radial) return GradientGeometry(cx, cy, cx, cy, max(area.width, area.height) * 0.6f)
    val a = g.angleDeg / 180.0 * PI
    val dx = cos(a).toFloat()
    val dy = sin(a).toFloat()
    val half = (abs(area.width * dx) + abs(area.height * dy)) / 2
    return GradientGeometry(cx - dx * half, cy - dy * half, cx + dx * half, cy + dy * half, 0f)
}

/** PDF da capa aberta numa página só: arte com sangria, textos com fontes embutidas, marcas de corte e dobra. */
internal object CoverPdfWriter {
    fun write(cover: CoverLayout, target: File, images: (String) -> ByteArray?) {
        val g = cover.geometry
        val bleed = g.bleedPt
        val slug = g.slugPt
        val off = bleed + slug
        val w = g.widthPt
        val h = g.heightPt
        val fonts = PdfFontCache()
        PDDocument().use { doc ->
            val page = PDPage(PDRectangle(w + 2 * off, h + 2 * off))
            page.trimBox = PDRectangle(off, off, w, h)
            page.bleedBox = PDRectangle(slug, slug, w + 2 * bleed, h + 2 * bleed)
            doc.addPage(page)
            val pdImages = HashMap<String, PDImageXObject?>()
            fun pdImage(path: String) = pdImages.getOrPut(path) {
                images(path)?.let { runCatching { PDImageXObject.createFromByteArray(doc, it, path) }.getOrNull() }
            }
            PDPageContentStream(doc, page).use { cs ->
                fun px(x: Float) = off + x
                fun py(y: Float) = off + h - y
                cs.setNonStrokingColor(argbColor(cover.background))
                cs.addRect(slug, slug, w + 2 * bleed, h + 2 * bleed)
                cs.fill()
                cover.gradient?.let { grad ->
                    val geo = gradientGeometry(grad, PtRect(-bleed, -bleed, w + 2 * bleed, h + 2 * bleed))
                    cs.saveGraphicsState()
                    cs.addRect(slug, slug, w + 2 * bleed, h + 2 * bleed)
                    cs.clip()
                    cs.shadingFill(shading(grad, px(geo.x0), py(geo.y0), px(geo.x1), py(geo.y1), geo.radius))
                    cs.restoreGraphicsState()
                }
                cover.image?.let { art ->
                    val pd = pdImage(art.path)
                    if (pd != null) {
                        cs.saveGraphicsState()
                        cs.addRect(slug, slug, w + 2 * bleed, h + 2 * bleed)
                        cs.clip()
                        cs.drawImage(pd, px(art.x), py(art.y + art.height), art.width, art.height)
                        cs.restoreGraphicsState()
                    }
                }
                for (o in cover.overlays) {
                    val pd = pdImage(o.path) ?: continue
                    cs.drawImage(pd, px(o.x), py(o.y + o.height), o.width, o.height)
                }
                for (item in cover.items) when (item) {
                    is CoverRect -> {
                        cs.setNonStrokingColor(argbColor(item.color))
                        cs.addRect(px(item.x), py(item.y + item.height), item.width, item.height)
                        cs.fill()
                    }
                    is CoverText -> {
                        val face = FontCatalog.face(item.font) ?: continue
                        cs.setNonStrokingColor(argbColor(item.color))
                        var d = 0f
                        for ((seg, f) in coverageRuns(item.text, face)) {
                            val font = fonts.font(doc, f)
                            cs.beginText()
                            cs.setFont(font, item.sizePt)
                            cs.setCharacterSpacing(item.trackingEm * item.sizePt)
                            // No sistema do PDF (y para cima): descer é girar −90°, subir é +90°.
                            when (item.turn) {
                                TextTurn.Nenhum -> cs.newLineAtOffset(px(item.x + d), py(item.y))
                                TextTurn.Desce -> cs.setTextMatrix(Matrix.getRotateInstance(-PI / 2, px(item.x), py(item.y + d)))
                                TextTurn.Sobe -> cs.setTextMatrix(Matrix.getRotateInstance(PI / 2, px(item.x), py(item.y - d)))
                            }
                            showSafely(cs, font, seg)
                            cs.endText()
                            d += AwtGlyphMeasurer.advance(seg, f, item.sizePt) + item.trackingEm * item.sizePt * seg.length
                        }
                    }
                }
                if (cover.marks.isNotEmpty()) {
                    cs.setStrokingColor(Color.BLACK)
                    cs.setLineWidth(0.25f)
                    for (m in cover.marks) {
                        cs.moveTo(px(m.x1), py(m.y1))
                        cs.lineTo(px(m.x2), py(m.y2))
                    }
                    cs.stroke()
                }
            }
            doc.documentInformation.apply {
                title = "${cover.title} (capa)"
                author = cover.author
                creator = "Escriba"
                producer = "Escriba (Apache PDFBox)"
                creationDate = Calendar.getInstance()
            }
            doc.save(target)
        }
        fonts.close()
    }

    /** Degradê de duas cores (função exponencial de grau 1 = interpolação linear), estendido além das pontas. */
    private fun shading(g: CoverGradient, x0: Float, y0: Float, x1: Float, y1: Float, radius: Float): PDShading {
        fun rgb(c: Long) = COSArray().apply {
            add(COSFloat(((c shr 16) and 0xFF) / 255f))
            add(COSFloat(((c shr 8) and 0xFF) / 255f))
            add(COSFloat((c and 0xFF) / 255f))
        }
        fun floats(vararg v: Float) = COSArray().apply { v.forEach { add(COSFloat(it)) } }
        val fn = PDFunctionType2(COSDictionary().apply {
            setItem(COSName.FUNCTION_TYPE, COSInteger.get(2))
            setItem(COSName.DOMAIN, floats(0f, 1f))
            setItem(COSName.C0, rgb(g.from))
            setItem(COSName.C1, rgb(g.to))
            setItem(COSName.N, COSInteger.get(1))
        })
        val extend = COSArray().apply { add(COSBoolean.TRUE); add(COSBoolean.TRUE) }
        return if (g.radial) {
            PDShadingType3(COSDictionary()).apply {
                shadingType = PDShading.SHADING_TYPE3
                colorSpace = PDDeviceRGB.INSTANCE
                coords = floats(x0, y0, 0f, x1, y1, radius)
                function = fn
                setExtend(extend)
            }
        } else {
            PDShadingType2(COSDictionary()).apply {
                shadingType = PDShading.SHADING_TYPE2
                colorSpace = PDDeviceRGB.INSTANCE
                coords = floats(x0, y0, x1, y1)
                function = fn
                setExtend(extend)
            }
        }
    }
}

/** Prévia da capa aberta com a sangria; com [showGuides], marca o corte, as dobras e a área segura. */
internal object CoverPainter {
    fun paint(cover: CoverLayout, pxPerPt: Float, showGuides: Boolean, images: (String) -> ByteArray?): BufferedImage {
        val geo = cover.geometry
        val bleed = geo.bleedPt
        val region = PtRect(-bleed, -bleed, geo.widthPt + 2 * bleed, geo.heightPt + 2 * bleed)
        val wPx = ceil(region.width * pxPerPt).toInt().coerceAtLeast(1)
        val hPx = ceil(region.height * pxPerPt).toInt().coerceAtLeast(1)
        return paintRegion(cover, region, pxPerPt, wPx, hPx, showGuides, images, BufferedImage.TYPE_INT_ARGB)
    }

    /** Capa de e-book: só a frente, no tamanho pedido, cortando ao centro o que sobrar da proporção. */
    fun paintEbook(cover: CoverLayout, size: EbookCoverSize, images: (String) -> ByteArray?): BufferedImage {
        val geo = cover.geometry
        val (tw, th) = ebookCoverPixels(size, geo)
        val front = geo.zone(CoverPanel.Capa)!!
        val fx = mmToPt(front.xMm)
        val fw = mmToPt(front.widthMm)
        val fh = geo.heightPt
        val scale = max(tw / fw, th / fh)
        val rw = tw / scale
        val rh = th / scale
        val region = PtRect(fx + (fw - rw) / 2, (fh - rh) / 2, rw, rh)
        return paintRegion(cover, region, scale, tw, th, false, images, BufferedImage.TYPE_INT_RGB)
    }

    private fun paintRegion(
        cover: CoverLayout, region: PtRect, pxPerPt: Float, wPx: Int, hPx: Int, showGuides: Boolean,
        images: (String) -> ByteArray?, type: Int,
    ): BufferedImage {
        val geo = cover.geometry
        val bleed = geo.bleedPt
        val w = geo.widthPt
        val h = geo.heightPt
        val img = BufferedImage(wPx, hPx, type)
        val g = img.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g.scale(pxPerPt.toDouble(), pxPerPt.toDouble())
            g.translate(-region.x.toDouble(), -region.y.toDouble())
            val area = Rectangle2D.Float(-bleed, -bleed, w + 2 * bleed, h + 2 * bleed)
            g.color = argbColor(cover.background)
            g.fill(area)
            cover.gradient?.let { grad ->
                val gg = gradientGeometry(grad, PtRect(-bleed, -bleed, w + 2 * bleed, h + 2 * bleed))
                val colors = arrayOf(argbColor(grad.from), argbColor(grad.to))
                g.paint = if (grad.radial) RadialGradientPaint(Point2D.Float(gg.x0, gg.y0), gg.radius, floatArrayOf(0f, 1f), colors)
                else LinearGradientPaint(Point2D.Float(gg.x0, gg.y0), Point2D.Float(gg.x1, gg.y1), floatArrayOf(0f, 1f), colors)
                g.fill(area)
            }
            cover.image?.let { art ->
                val clip = g.clip
                g.clip(area)
                drawImage(g, art, images)
                g.clip = clip
            }
            for (o in cover.overlays) drawImage(g, o, images)
            for (item in cover.items) when (item) {
                is CoverRect -> {
                    g.color = argbColor(item.color)
                    g.fill(Rectangle2D.Float(item.x, item.y, item.width, item.height))
                }
                is CoverText -> {
                    val face = FontCatalog.face(item.font) ?: continue
                    g.color = argbColor(item.color)
                    var d = 0f
                    for ((seg, f) in coverageRuns(item.text, face)) {
                        var font = f.awt.deriveFont(item.sizePt)
                        if (item.trackingEm != 0f) font = font.deriveFont(mapOf(TextAttribute.TRACKING to item.trackingEm))
                        g.font = font
                        when (item.turn) {
                            TextTurn.Nenhum -> g.drawString(seg, item.x + d, item.y)
                            TextTurn.Desce, TextTurn.Sobe -> {
                                val saved = g.transform
                                val down = item.turn == TextTurn.Desce
                                g.translate(item.x.toDouble(), (if (down) item.y + d else item.y - d).toDouble())
                                g.rotate(if (down) PI / 2 else -PI / 2)
                                g.drawString(seg, 0f, 0f)
                                g.transform = saved
                            }
                        }
                        d += AwtGlyphMeasurer.advance(seg, f, item.sizePt) + item.trackingEm * item.sizePt * seg.length
                    }
                }
            }
            if (showGuides) guides(g, cover, w, h, bleed)
        } finally {
            g.dispose()
        }
        return img
    }

    private fun drawImage(g: Graphics2D, art: CoverImage, images: (String) -> ByteArray?) {
        val bi = PreviewImages.get(art.path, images) ?: return
        val at = AffineTransform.getTranslateInstance(art.x.toDouble(), art.y.toDouble())
        at.scale(art.width.toDouble() / bi.width, art.height.toDouble() / bi.height)
        g.drawImage(bi, at, null)
    }

    private fun guides(g: Graphics2D, cover: CoverLayout, w: Float, h: Float, bleed: Float) {
        // Véu sobre a sangria (o que a guilhotina corta).
        if (bleed > 0f) {
            g.color = Color(255, 255, 255, 90)
            g.fill(Rectangle2D.Float(-bleed, -bleed, w + 2 * bleed, bleed))
            g.fill(Rectangle2D.Float(-bleed, h, w + 2 * bleed, bleed))
            g.fill(Rectangle2D.Float(-bleed, 0f, bleed, h))
            g.fill(Rectangle2D.Float(w, 0f, bleed, h))
        }
        g.color = Color(220, 40, 60, 200)
        g.stroke = BasicStroke(0.6f)
        g.draw(Rectangle2D.Float(0f, 0f, w, h))
        g.stroke = BasicStroke(0.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, floatArrayOf(4f, 3f), 0f)
        for (f in cover.geometry.foldsMm) {
            val x = mmToPt(f)
            g.draw(Line2D.Float(x, -bleed, x, h + bleed))
        }
        // Área segura de capa e contracapa.
        val safe = mmToPt(COVER_SAFE_MM)
        g.color = Color(80, 140, 230, 170)
        g.stroke = BasicStroke(0.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, floatArrayOf(2f, 2f), 0f)
        for (z in cover.geometry.zones.filter { it.panel == CoverPanel.Capa || it.panel == CoverPanel.Contracapa }) {
            g.draw(Rectangle2D.Float(mmToPt(z.xMm) + safe, safe, mmToPt(z.widthMm) - 2 * safe, h - 2 * safe))
        }
    }
}

/** Grava a capa de e-book em JPEG de alta qualidade. */
internal fun writeEbookJpeg(image: BufferedImage, target: File) {
    val writer = ImageIO.getImageWritersByFormatName("jpg").next()
    try {
        ImageIO.createImageOutputStream(target).use { out ->
            writer.output = out
            val param = writer.defaultWriteParam.apply {
                compressionMode = ImageWriteParam.MODE_EXPLICIT
                compressionQuality = 0.92f
            }
            writer.write(null, IIOImage(image, null, null), param)
        }
    } finally {
        writer.dispose()
    }
}
