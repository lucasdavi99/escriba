package com.cuboide.escriba.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.cuboide.escriba.core.storage.ThemeId
import com.cuboide.escriba.platform.systemFontFamily
import escriba.shared.generated.resources.Res
import escriba.shared.generated.resources.inter_italic
import escriba.shared.generated.resources.inter_medium
import escriba.shared.generated.resources.inter_regular
import escriba.shared.generated.resources.inter_semibold
import escriba.shared.generated.resources.literata_bold
import escriba.shared.generated.resources.literata_bold_italic
import escriba.shared.generated.resources.literata_italic
import escriba.shared.generated.resources.literata_regular
import escriba.shared.generated.resources.newsreader_italic
import escriba.shared.generated.resources.newsreader_medium
import escriba.shared.generated.resources.newsreader_regular
import escriba.shared.generated.resources.newsreader_semibold
import org.jetbrains.compose.resources.Font

/**
 * Cores além do Material. A identidade é "papel, tinta e um acento": [paper] e [desk] para o editor,
 * [hairline] para as divisões (no lugar de cartões), [muted] para o texto secundário e [accent] usado com parcimônia.
 */
@Immutable
data class EscribaPalette(
    val isDark: Boolean,
    /** Fundo do menu lateral (um tom ao lado do fundo, separado por um fio). */
    val navBackground: Color,
    val navContent: Color,
    /** Fundo do item selecionado no menu. */
    val navSelected: Color,
    val navSelectedContent: Color,
    val paper: Color,
    val ink: Color,
    val desk: Color,
    val accent: Color,
    /** Fio de 1dp que separa regiões. */
    val hairline: Color,
    /** Texto secundário e rótulos. */
    val muted: Color,
    /** Acento bem diluído: seleção, hover destacado, fundo de ícones. */
    val accentSoft: Color,
    /** Escala de status (capítulos e cenas): rascunho, revisão e pronto. */
    val warning: Color,
    val info: Color,
    val success: Color,
    val danger: Color,
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
        primary = Color(0xFFA14F2B), onPrimary = Color.White,
        primaryContainer = Color(0xFFF3E2D7), onPrimaryContainer = Color(0xFF3D1A0B),
        secondary = Color(0xFF5B6B7A), onSecondary = Color.White,
        secondaryContainer = Color(0xFFE6EAEE), onSecondaryContainer = Color(0xFF1C2733),
        tertiary = Color(0xFF4E7A59), onTertiary = Color.White,
        tertiaryContainer = Color(0xFFDDEBDF), onTertiaryContainer = Color(0xFF13301B),
        background = Color(0xFFF7F5F0), onBackground = Color(0xFF1E1C19),
        surface = Color(0xFFFBFAF7), onSurface = Color(0xFF1E1C19),
        surfaceVariant = Color(0xFFEDE9E1), onSurfaceVariant = Color(0xFF6C675F),
        surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF3F1EB),
        surfaceContainer = Color(0xFFEFECE5), surfaceContainerHigh = Color(0xFFF9F7F3),
        surfaceContainerHighest = Color(0xFFE6E2D9),
        outline = Color(0xFFC6BFB3), outlineVariant = Color(0xFFE4DFD5),
        inverseSurface = Color(0xFF2A2824), inverseOnSurface = Color(0xFFF4F1EB),
        error = Color(0xFFB3261E), errorContainer = Color(0xFFF7DEDB), onErrorContainer = Color(0xFF410E0B),
        scrim = Color(0xFF1E1C19),
    ),
    ThemeId.Sepia to lightColorScheme(
        primary = Color(0xFF8E4A2A), onPrimary = Color.White,
        primaryContainer = Color(0xFFF0DCCB), onPrimaryContainer = Color(0xFF3A1606),
        secondary = Color(0xFF6E6150), onSecondary = Color.White,
        secondaryContainer = Color(0xFFE9DFCD), onSecondaryContainer = Color(0xFF2B1D08),
        tertiary = Color(0xFF5E6B3A), onTertiary = Color.White,
        tertiaryContainer = Color(0xFFE2E6CC), onTertiaryContainer = Color(0xFF1D2408),
        background = Color(0xFFF2EADB), onBackground = Color(0xFF35291D),
        surface = Color(0xFFF7F0E3), onSurface = Color(0xFF35291D),
        surfaceVariant = Color(0xFFE9DECB), onSurfaceVariant = Color(0xFF75654F),
        surfaceContainerLowest = Color(0xFFFCF7EE), surfaceContainerLow = Color(0xFFEEE5D4),
        surfaceContainer = Color(0xFFEAE0CD), surfaceContainerHigh = Color(0xFFF6EFE2),
        surfaceContainerHighest = Color(0xFFE2D6C0),
        outline = Color(0xFFBCA987), outlineVariant = Color(0xFFE0D3BB),
        inverseSurface = Color(0xFF35291D), inverseOnSurface = Color(0xFFF7F0E3),
        error = Color(0xFFAA2E22), errorContainer = Color(0xFFF5DCD4),
        scrim = Color(0xFF35291D),
    ),
    ThemeId.Tinta to darkColorScheme(
        primary = Color(0xFFE2B066), onPrimary = Color(0xFF2E1F05),
        primaryContainer = Color(0xFF3E3220), onPrimaryContainer = Color(0xFFFBE4BD),
        secondary = Color(0xFF9DB4D0), onSecondary = Color(0xFF13233A),
        secondaryContainer = Color(0xFF263346), onSecondaryContainer = Color(0xFFD8E3F5),
        tertiary = Color(0xFF9CC5A2), onTertiary = Color(0xFF0E2A14),
        tertiaryContainer = Color(0xFF213628), onTertiaryContainer = Color(0xFFD5ECD8),
        background = Color(0xFF14161B), onBackground = Color(0xFFE8E4DA),
        surface = Color(0xFF181B21), onSurface = Color(0xFFE8E4DA),
        surfaceVariant = Color(0xFF242832), onSurfaceVariant = Color(0xFFA19C92),
        surfaceContainerLowest = Color(0xFF101216), surfaceContainerLow = Color(0xFF1B1E25),
        surfaceContainer = Color(0xFF20232B), surfaceContainerHigh = Color(0xFF21252D),
        surfaceContainerHighest = Color(0xFF2B2F38),
        outline = Color(0xFF4B505B), outlineVariant = Color(0xFF2B2F37),
        inverseSurface = Color(0xFFE8E4DA), inverseOnSurface = Color(0xFF1A1C21),
        error = Color(0xFFF2A49B), errorContainer = Color(0xFF4A2421), onErrorContainer = Color(0xFFFFDAD5),
    ),
    ThemeId.Floresta to darkColorScheme(
        primary = Color(0xFFAFCB9C), onPrimary = Color(0xFF1C3212),
        primaryContainer = Color(0xFF2F4127), onPrimaryContainer = Color(0xFFD9E7C6),
        secondary = Color(0xFFD8BE9C), onSecondary = Color(0xFF3A2610),
        secondaryContainer = Color(0xFF433628), onSecondaryContainer = Color(0xFFF1E1CC),
        tertiary = Color(0xFFE2A88C), onTertiary = Color(0xFF3E1C0C),
        tertiaryContainer = Color(0xFF4A2E22), onTertiaryContainer = Color(0xFFFBDCCB),
        background = Color(0xFF121512), onBackground = Color(0xFFE3E2D7),
        surface = Color(0xFF171B16), onSurface = Color(0xFFE3E2D7),
        surfaceVariant = Color(0xFF262C24), onSurfaceVariant = Color(0xFFA6A99A),
        surfaceContainerLowest = Color(0xFF0D100C), surfaceContainerLow = Color(0xFF1A1F19),
        surfaceContainer = Color(0xFF1F251E), surfaceContainerHigh = Color(0xFF1F241E),
        surfaceContainerHighest = Color(0xFF2A3128),
        outline = Color(0xFF4E5748), outlineVariant = Color(0xFF2A3127),
        inverseSurface = Color(0xFFE3E2D7), inverseOnSurface = Color(0xFF171B16),
        error = Color(0xFFF0A79B), errorContainer = Color(0xFF4B2520), onErrorContainer = Color(0xFFFFDAD5),
    ),
)

private val palettes: Map<ThemeId, EscribaPalette> = mapOf(
    ThemeId.Papel to EscribaPalette(
        isDark = false,
        navBackground = Color(0xFFF2EFE9), navContent = Color(0xFF5F5A51),
        navSelected = Color(0xFFE7E2D8), navSelectedContent = Color(0xFF1E1C19),
        paper = Color(0xFFFFFEFB), ink = Color(0xFF24211D), desk = Color(0xFFEDEAE3), accent = Color(0xFFA14F2B),
        hairline = Color(0xFFE4DFD5), muted = Color(0xFF7A746A), accentSoft = Color(0xFFF4E7DE),
        warning = Color(0xFFB7832A), info = Color(0xFF55779A), success = Color(0xFF4E7A59), danger = Color(0xFFB3261E),
    ),
    ThemeId.Sepia to EscribaPalette(
        isDark = false,
        navBackground = Color(0xFFECE3D2), navContent = Color(0xFF6A5843),
        navSelected = Color(0xFFE1D4BD), navSelectedContent = Color(0xFF35291D),
        paper = Color(0xFFFBF5E9), ink = Color(0xFF3A2C1F), desk = Color(0xFFE6DAC5), accent = Color(0xFF8E4A2A),
        hairline = Color(0xFFDDCFB6), muted = Color(0xFF85735B), accentSoft = Color(0xFFEFDDCB),
        warning = Color(0xFFA9792A), info = Color(0xFF5E7690), success = Color(0xFF5E6B3A), danger = Color(0xFFAA2E22),
    ),
    ThemeId.Tinta to EscribaPalette(
        isDark = true,
        navBackground = Color(0xFF111317), navContent = Color(0xFF9A968D),
        navSelected = Color(0xFF22252D), navSelectedContent = Color(0xFFF1EDE3),
        paper = Color(0xFF1C1F26), ink = Color(0xFFE6E1D5), desk = Color(0xFF111317), accent = Color(0xFFE2B066),
        hairline = Color(0xFF292D35), muted = Color(0xFF8F8B82), accentSoft = Color(0xFF2E2819),
        warning = Color(0xFFE0B062), info = Color(0xFF8FB0CF), success = Color(0xFF9CC59A), danger = Color(0xFFF2A49B),
    ),
    ThemeId.Floresta to EscribaPalette(
        isDark = true,
        navBackground = Color(0xFF0F120E), navContent = Color(0xFF979B8C),
        navSelected = Color(0xFF20261E), navSelectedContent = Color(0xFFE3E2D7),
        paper = Color(0xFF1A1F19), ink = Color(0xFFE4E2D5), desk = Color(0xFF0F120E), accent = Color(0xFFAFCB9C),
        hairline = Color(0xFF272E25), muted = Color(0xFF8D9083), accentSoft = Color(0xFF232D1F),
        warning = Color(0xFFDDB872), info = Color(0xFF93B4C9), success = Color(0xFFAFCB9C), danger = Color(0xFFF0A79B),
    ),
)

/** Escala tipográfica: Newsreader para títulos e numerais grandes, Inter para a interface. */
private fun typography(display: FontFamily, ui: FontFamily): Typography {
    fun d(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, tracking: Double = -0.01) =
        TextStyle(fontFamily = display, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.em)
    fun u(size: Double, line: Int, weight: FontWeight = FontWeight.Normal, tracking: Double = 0.0) =
        TextStyle(fontFamily = ui, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.em)
    return Typography(
        displayLarge = d(52, 58, tracking = -0.02),
        displayMedium = d(42, 48, tracking = -0.02),
        displaySmall = d(34, 40, tracking = -0.015),
        headlineLarge = d(30, 36),
        headlineMedium = d(27, 33),
        headlineSmall = d(22, 28, FontWeight.Medium),
        titleLarge = d(20, 26, FontWeight.Medium),
        titleMedium = u(14.5, 20, FontWeight.SemiBold),
        titleSmall = u(13.0, 18, FontWeight.SemiBold),
        bodyLarge = u(15.0, 23),
        bodyMedium = u(13.5, 20),
        bodySmall = u(12.5, 18),
        labelLarge = u(13.5, 18, FontWeight.Medium),
        labelMedium = u(12.0, 16, FontWeight.Medium),
        labelSmall = u(11.0, 15, FontWeight.Medium, tracking = 0.02),
    )
}

/** Três raios em toda a interface: 6 (controles), 10 (superfícies) e 16 (diálogos e folhas). */
private val shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

@Composable
fun EscribaTheme(theme: ThemeId, editorFont: String, content: @Composable () -> Unit) {
    val display = FontFamily(
        Font(Res.font.newsreader_regular, FontWeight.Normal),
        Font(Res.font.newsreader_italic, FontWeight.Normal, FontStyle.Italic),
        Font(Res.font.newsreader_medium, FontWeight.Medium),
        Font(Res.font.newsreader_semibold, FontWeight.SemiBold),
        Font(Res.font.newsreader_semibold, FontWeight.Bold),
    )
    val ui = FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_italic, FontWeight.Normal, FontStyle.Italic),
        Font(Res.font.inter_medium, FontWeight.Medium),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
        Font(Res.font.inter_semibold, FontWeight.Bold),
    )
    val literata = FontFamily(
        Font(Res.font.literata_regular, FontWeight.Normal),
        Font(Res.font.literata_italic, FontWeight.Normal, FontStyle.Italic),
        Font(Res.font.literata_bold, FontWeight.SemiBold),
        Font(Res.font.literata_bold, FontWeight.Bold),
        Font(Res.font.literata_bold_italic, FontWeight.Bold, FontStyle.Italic),
    )
    val book = remember(editorFont, literata) {
        editorFont.takeIf { it.isNotBlank() && it != EMBEDDED_BOOK_FONT }?.let { systemFontFamily(listOf(it)) } ?: literata
    }
    val fonts = remember(display, ui, book) { EscribaFonts(display, ui, book) }
    CompositionLocalProvider(LocalPalette provides palettes.getValue(theme), LocalFonts provides fonts) {
        MaterialTheme(
            colorScheme = schemes.getValue(theme),
            typography = remember(fonts) { typography(fonts.display, fonts.ui) },
            shapes = shapes,
            content = content,
        )
    }
}

/** Fonte de livro que vem com o Escriba (usada quando nenhuma outra foi escolhida). */
const val EMBEDDED_BOOK_FONT = "Literata"

/** Paleta e esquema de um tema qualquer, para miniaturas nas Preferências. */
fun paletteOf(theme: ThemeId): EscribaPalette = palettes.getValue(theme)
fun schemeOf(theme: ThemeId): ColorScheme = schemes.getValue(theme)
