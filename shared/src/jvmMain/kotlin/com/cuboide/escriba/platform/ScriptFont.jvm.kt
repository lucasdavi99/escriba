package com.cuboide.escriba.platform

import androidx.compose.ui.text.font.FontFamily
import java.awt.Font
import java.io.File

actual fun scriptFontFamily(bytes: ByteArray, id: String): FontFamily? =
    runCatching { FontFamily(androidx.compose.ui.text.platform.Font(identity = id, data = bytes)) }.getOrNull()

actual fun registerScriptFont(path: String?) {
    FontCatalog.script = path?.let { p ->
        runCatching {
            val file = File(p)
            val awt = Font.createFont(Font.TRUETYPE_FONT, file)
            FontFace(file, awt.getFamily(), awt.psName, bold = false, italic = false, awt = awt)
        }.getOrNull()
    }
    AwtGlyphMeasurer.clearCache()
}
