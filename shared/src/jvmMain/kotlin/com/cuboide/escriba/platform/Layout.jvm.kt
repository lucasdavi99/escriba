package com.cuboide.escriba.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerIcon
import com.cuboide.escriba.core.layout.CoverLayout
import com.cuboide.escriba.core.layout.EbookCoverSize
import com.cuboide.escriba.core.layout.GlyphMeasurer
import com.cuboide.escriba.core.layout.LaidOutBook
import com.cuboide.escriba.core.layout.LaidPage
import java.awt.Cursor
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import javax.imageio.ImageIO

actual fun layoutMeasurer(): GlyphMeasurer = AwtGlyphMeasurer

actual fun layoutFontFamilies(): List<String> = FontCatalog.families()

actual fun imagePixelSize(bytes: ByteArray): Pair<Int, Int>? = imageSizeOf(bytes)

actual fun renderLayoutPage(
    book: LaidOutBook,
    page: LaidPage,
    pxPerPt: Float,
    showGuides: Boolean,
    images: (String) -> ByteArray?,
): ImageBitmap {
    return LayoutPagePainter.paint(book, page, pxPerPt, argbColor(book.settings.page.paper.tint), showGuides, images).toComposeImageBitmap()
}

actual fun writeLayoutPdf(book: LaidOutBook, path: String, images: (String) -> ByteArray?) =
    LayoutPdfWriter.write(book, File(path), images)

actual fun renderCover(cover: CoverLayout, pxPerPt: Float, showGuides: Boolean, images: (String) -> ByteArray?): ImageBitmap =
    CoverPainter.paint(cover, pxPerPt, showGuides, images).toComposeImageBitmap()

actual fun writeCoverPdf(cover: CoverLayout, path: String, images: (String) -> ByteArray?) =
    CoverPdfWriter.write(cover, File(path), images)

actual fun resizePointerIcon(horizontal: Boolean): PointerIcon =
    PointerIcon(Cursor(if (horizontal) Cursor.E_RESIZE_CURSOR else Cursor.N_RESIZE_CURSOR))

actual fun movePointerIcon(): PointerIcon = PointerIcon(Cursor(Cursor.MOVE_CURSOR))

actual fun copyToClipboard(text: String) {
    runCatching { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null) }
}

actual fun writeEbookCover(cover: CoverLayout, path: String, size: EbookCoverSize, images: (String) -> ByteArray?) =
    writeEbookJpeg(CoverPainter.paintEbook(cover, size, images), File(path))

actual fun coverFacesPng(cover: CoverLayout, widthPx: Int, images: (String) -> ByteArray?): ByteArray =
    java.io.ByteArrayOutputStream().also { ImageIO.write(CoverPainter.paintFaces(cover, widthPx, images), "png", it) }.toByteArray()

actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? =
    runCatching { org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
