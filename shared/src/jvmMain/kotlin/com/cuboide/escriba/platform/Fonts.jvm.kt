package com.cuboide.escriba.platform

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.SystemFont
import java.awt.GraphicsEnvironment

private val installed: Set<String> by lazy {
    runCatching { GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.toSet() }.getOrDefault(emptySet())
}

private val bookFontCandidates = listOf(
    "Sitka Text", "Georgia", "Palatino Linotype", "Book Antiqua", "Constantia", "Cambria",
    "Garamond", "Baskerville Old Face", "Iowan Old Style", "Palatino", "Charter", "Hoefler Text",
    "Literata", "EB Garamond", "Lora", "Merriweather", "Crimson Pro", "Noto Serif", "DejaVu Serif", "Liberation Serif",
)

@OptIn(ExperimentalTextApi::class)
actual fun systemFontFamily(candidates: List<String>): FontFamily? {
    val name = candidates.firstOrNull { it in installed } ?: return null
    return FontFamily(
        SystemFont(name, FontWeight.Normal),
        SystemFont(name, FontWeight.Normal, FontStyle.Italic),
        SystemFont(name, FontWeight.SemiBold),
        SystemFont(name, FontWeight.Bold),
        SystemFont(name, FontWeight.Bold, FontStyle.Italic),
    )
}

actual fun installedBookFonts(): List<String> = bookFontCandidates.filter { it in installed }
