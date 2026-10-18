package com.cuboide.escriba.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cuboide.escriba.core.storage.ThemeId
import com.cuboide.escriba.platform.systemFontFamily

/** Cores além do Material: menu lateral, "papel" do editor e marcações. */
@Immutable
data class EscribaPalette(
    val isDark: Boolean,
    val navBackground: Color,
    val navContent: Color,
    val navSelected: Color,
    val navSelectedContent: Color,
    val paper: Color,
    val ink: Color,
    val desk: Color,
    val accent: Color,
    val inserted: Color = Color(0x5534A853),
    val deleted: Color = Color(0x55D93025),
    val match: Color = Color(0x66E8B04B),
    val currentMatch: Color = Color(0xCCE8B04B),
)

@Immutable
data class EscribaFonts(val display: FontFamily, val ui: FontFamily, val book: FontFamily)

val LocalPalette = staticCompositionLocalOf { palettes.getValue(ThemeId.Papel) }
val LocalFonts = staticCompositionLocalOf { EscribaFonts(FontFamily.Serif, FontFamily.SansSerif, FontFamily.Serif) }

private val schemes: Map<ThemeId, ColorScheme> = mapOf(
    ThemeId.Papel to lightColorScheme(
        primary = Color(0xFF4F6B3A), onPrimary = Color.White,
        primaryContainer = Color(0xFFDCE8CB), onPrimaryContainer = Color(0xFF1B2A10),
        secondary = Color(0xFF8A6A42), onSecondary = Color.White,
        secondaryContainer = Color(0xFFF1E3CE), onSecondaryContainer = Color(0xFF2C1A08),
        tertiary = Color(0xFF9A5B3C),
        background = Color(0xFFF3EEE4), onBackground = Color(0xFF2B2620),
        surface = Color(0xFFFBF8F2), onSurface = Color(0xFF2B2620),
        surfaceVariant = Color(0xFFEAE3D6), onSurfaceVariant = Color(0xFF6B6255),
        surfaceContainerLowest = Color(0xFFFFFDF9), surfaceContainerLow = Color(0xFFFAF6EF),
        surfaceContainer = Color(0xFFF6F1E8), surfaceContainerHigh = Color(0xFFF1EBE0),
        surfaceContainerHighest = Color(0xFFEBE4D8),
        outline = Color(0xFFB7AD9D), outlineVariant = Color(0xFFDDD4C5),
    ),
    ThemeId.Sepia to lightColorScheme(
        primary = Color(0xFF8C4B2F), onPrimary = Color.White,
        primaryContainer = Color(0xFFF4D9C6), onPrimaryContainer = Color(0xFF3A1606),
        secondary = Color(0xFF7A6040), onSecondary = Color.White,
        secondaryContainer = Color(0xFFEFDFC4), onSecondaryContainer = Color(0xFF2B1D08),
        tertiary = Color(0xFF5E6B3A),
        background = Color(0xFFEDE1CC), onBackground = Color(0xFF3B2E22),
        surface = Color(0xFFF7EEDD), onSurface = Color(0xFF3B2E22),
        surfaceVariant = Color(0xFFE6D6BC), onSurfaceVariant = Color(0xFF6F5D48),
        surfaceContainerLowest = Color(0xFFFCF6EA), surfaceContainerLow = Color(0xFFF7EEDD),
        surfaceContainer = Color(0xFFF2E7D3), surfaceContainerHigh = Color(0xFFEDE0C9),
        surfaceContainerHighest = Color(0xFFE6D6BC),
        outline = Color(0xFFB59E7E), outlineVariant = Color(0xFFDCC9AB),
    ),
    ThemeId.Tinta to darkColorScheme(
        primary = Color(0xFFE7B55C), onPrimary = Color(0xFF3A2600),
        primaryContainer = Color(0xFF4A3814), onPrimaryContainer = Color(0xFFFCE3B4),
        secondary = Color(0xFF9DB4D6), onSecondary = Color(0xFF13233A),
        secondaryContainer = Color(0xFF263449), onSecondaryContainer = Color(0xFFD8E3F5),
        tertiary = Color(0xFFD99A87),
        background = Color(0xFF10141D), onBackground = Color(0xFFE6E1D6),
        surface = Color(0xFF171C27), onSurface = Color(0xFFE6E1D6),
        surfaceVariant = Color(0xFF242B39), onSurfaceVariant = Color(0xFFB0AA9C),
        surfaceContainerLowest = Color(0xFF0C1017), surfaceContainerLow = Color(0xFF141924),
        surfaceContainer = Color(0xFF1A202C), surfaceContainerHigh = Color(0xFF212835),
        surfaceContainerHighest = Color(0xFF2A3140),
        outline = Color(0xFF4F586A), outlineVariant = Color(0xFF2E3646),
    ),
    ThemeId.Floresta to darkColorScheme(
        primary = Color(0xFFB4CF98), onPrimary = Color(0xFF213613),
        primaryContainer = Color(0xFF34482A), onPrimaryContainer = Color(0xFFD9E7C6),
        secondary = Color(0xFFE0C2A0), onSecondary = Color(0xFF3A2610),
        secondaryContainer = Color(0xFF4A3A28), onSecondaryContainer = Color(0xFFF1E1CC),
        tertiary = Color(0xFFE2A88C),
        background = Color(0xFF141813), onBackground = Color(0xFFE4E2D6),
        surface = Color(0xFF1B201A), onSurface = Color(0xFFE4E2D6),
        surfaceVariant = Color(0xFF2B3229), onSurfaceVariant = Color(0xFFBDBBAA),
        surfaceContainerLowest = Color(0xFF0F130E), surfaceContainerLow = Color(0xFF181D17),
        surfaceContainer = Color(0xFF1E241D), surfaceContainerHigh = Color(0xFF252C24),
        surfaceContainerHighest = Color(0xFF2E362C),
        outline = Color(0xFF59624F), outlineVariant = Color(0xFF353D32),
    ),
)

private val palettes: Map<ThemeId, EscribaPalette> = mapOf(
    ThemeId.Papel to EscribaPalette(
        isDark = false,
        navBackground = Color(0xFF263022), navContent = Color(0xFFB9C2AE),
        navSelected = Color(0xFF3B4A32), navSelectedContent = Color(0xFFF2EBD9),
        paper = Color(0xFFFFFDF8), ink = Color(0xFF2A251F), desk = Color(0xFFEAE3D6), accent = Color(0xFFB98A3E),
    ),
    ThemeId.Sepia to EscribaPalette(
        isDark = false,
        navBackground = Color(0xFF3A2A1E), navContent = Color(0xFFD2BFA4),
        navSelected = Color(0xFF55402E), navSelectedContent = Color(0xFFFBEBD2),
        paper = Color(0xFFFBF3E3), ink = Color(0xFF3A2C1F), desk = Color(0xFFE3D3B7), accent = Color(0xFFB0643E),
    ),
    ThemeId.Tinta to EscribaPalette(
        isDark = true,
        navBackground = Color(0xFF0B0E15), navContent = Color(0xFF8E96A6),
        navSelected = Color(0xFF231E14), navSelectedContent = Color(0xFFF2C878),
        paper = Color(0xFF1B2130), ink = Color(0xFFE8E2D4), desk = Color(0xFF10141D), accent = Color(0xFFE7B55C),
    ),
    ThemeId.Floresta to EscribaPalette(
        isDark = true,
        navBackground = Color(0xFF0E120D), navContent = Color(0xFF9AA58E),
        navSelected = Color(0xFF26331F), navSelectedContent = Color(0xFFD9E7C6),
        paper = Color(0xFF1F251D), ink = Color(0xFFE6E3D5), desk = Color(0xFF141813), accent = Color(0xFFB4CF98),
    ),
)

private fun typography(display: FontFamily, ui: FontFamily): Typography {
    val base = Typography()
    fun TextStyle.with(f: FontFamily) = copy(fontFamily = f)
    return Typography(
        displayLarge = base.displayLarge.with(display),
        displayMedium = base.displayMedium.with(display),
        displaySmall = base.displaySmall.with(display),
        headlineLarge = base.headlineLarge.with(display),
        headlineMedium = base.headlineMedium.with(display),
        headlineSmall = base.headlineSmall.with(display),
        titleLarge = base.titleLarge.copy(fontFamily = display, fontSize = 22.sp),
        titleMedium = base.titleMedium.copy(fontFamily = ui, fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontFamily = ui, fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.with(ui),
        bodyMedium = base.bodyMedium.with(ui),
        bodySmall = base.bodySmall.with(ui),
        labelLarge = base.labelLarge.with(ui),
        labelMedium = base.labelMedium.with(ui),
        labelSmall = base.labelSmall.copy(fontFamily = ui, letterSpacing = 0.6.sp),
    )
}

@Composable
fun EscribaTheme(theme: ThemeId, editorFont: String, content: @Composable () -> Unit) {
    val fonts = remember(editorFont) {
        val display = systemFontFamily(listOf("Sitka Display", "Palatino Linotype", "Book Antiqua", "Palatino", "Georgia"))
            ?: FontFamily.Serif
        val ui = systemFontFamily(listOf("Segoe UI Variable Text", "Segoe UI", "SF Pro Text", "Helvetica Neue", "Inter", "Noto Sans"))
            ?: FontFamily.SansSerif
        val book = systemFontFamily(listOfNotNull(editorFont.ifBlank { null }, "Sitka Text", "Georgia", "Palatino Linotype"))
            ?: FontFamily.Serif
        EscribaFonts(display, ui, book)
    }
    CompositionLocalProvider(LocalPalette provides palettes.getValue(theme), LocalFonts provides fonts) {
        MaterialTheme(colorScheme = schemes.getValue(theme), typography = remember(fonts) { typography(fonts.display, fonts.ui) }, content = content)
    }
}
