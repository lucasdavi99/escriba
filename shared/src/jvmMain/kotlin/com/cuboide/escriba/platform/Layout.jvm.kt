package com.cuboide.escriba.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.cuboide.escriba.core.layout.GlyphMeasurer
import com.cuboide.escriba.core.layout.LaidOutBook
import com.cuboide.escriba.core.layout.LaidPage
import java.awt.Color
import java.io.File

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
    val tint = book.settings.page.paper.tint
    val paper = Color(((tint shr 16) and 0xFF).toInt(), ((tint shr 8) and 0xFF).toInt(), (tint and 0xFF).toInt())
    return LayoutPagePainter.paint(book, page, pxPerPt, paper, showGuides, images).toComposeImageBitmap()
}

actual fun writeLayoutPdf(book: LaidOutBook, path: String, images: (String) -> ByteArray?) =
    LayoutPdfWriter.write(book, File(path), images)
