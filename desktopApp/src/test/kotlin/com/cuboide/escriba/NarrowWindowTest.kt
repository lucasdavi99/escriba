package com.cuboide.escriba

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.EscribaServices
import com.cuboide.escriba.platform.JvmFileStore
import java.nio.file.Files
import java.util.UUID
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.cuboide.escriba.ui.ManuscriptStyling
import com.cuboide.escriba.ui.rememberParagraphSpacer
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/** O Manuscrito numa janela estreita, com os painéis laterais abertos, não pode espremer a barra de status. */
@OptIn(ExperimentalTestApi::class)
class NarrowWindowTest {
    private fun ComposeUiTest.showManuscript(width: Int, dir: java.io.File) {
        setContent {
            val scope = rememberCoroutineScope()
            val controller = remember {
                val services = EscribaServices(
                    JvmFileStore(), "${dir.path}/lib", "${dir.path}/data",
                    System::currentTimeMillis, { UUID.randomUUID().toString() }, { "2026-10-18" },
                )
                EscribaController(services, scope).also { it.createBook("Livro estreito", "") }
            }
            Box(Modifier.size(width = width.dp, height = 640.dp)) { App(controller) }
        }
        waitForIdle()
    }

    private fun ComposeUiTest.assertOneLine(label: String) {
        val bounds = onNodeWithText(label).fetchSemanticsNode().boundsInRoot
        // Visível e numa linha só: mais largo que alto, sem altura de várias linhas.
        assertTrue(bounds.width > bounds.height && bounds.height in 1f..30f, "\"$label\" espremido ou escondido: $bounds")
    }

    private fun withTempDir(block: (java.io.File) -> Unit) {
        val dir = Files.createTempDirectory("escriba-ui").toFile()
        try { block(dir) } finally { dir.deleteRecursively() }
    }

    @Test
    fun narrowWindowHidesPanelsAndKeepsStatusBarReadable() = withTempDir { dir ->
        runComposeUiTest {
            showManuscript(760, dir)
            // Os números que não cabem rolam de lado; os primeiros e o "Salvo" ficam à vista, numa linha só.
            listOf("Salvo", "0 palavras", "0 parágrafos").forEach { assertOneLine(it) }
            // Sem espaço, os capítulos e o inspetor se recolhem para a folha aparecer.
            onNodeWithText("ESTATÍSTICAS").assertDoesNotExist()
            onNodeWithText("Capítulos").assertDoesNotExist()
        }
    }

    @Test
    fun mediumWindowKeepsChaptersAndStatusBar() = withTempDir { dir ->
        runComposeUiTest {
            showManuscript(1020, dir)
            listOf("Salvo", "0 palavras").forEach { assertOneLine(it) }
            onNodeWithText("Capítulos").assertExists()
            onNodeWithText("ESTATÍSTICAS").assertDoesNotExist()
        }
    }

    @Test
    fun wideWindowShowsEverything() = withTempDir { dir ->
        runDesktopComposeUiTest(width = 1600, height = 900) {
            showManuscript(1500, dir)
            onNodeWithText("Capítulos").assertExists()
            onNodeWithText("ESTATÍSTICAS").assertExists()
            assertOneLine("Salvo")
        }
    }

    @Test
    fun spacerCalibrationGivesTheRequestedGap() = runComposeUiTest {
        var spacer = 0f
        setContent { spacer = rememberParagraphSpacer(FontFamily.Serif, 20f, 1.7f, 0.5f) }
        waitForIdle()
        val style = TextStyle(fontFamily = FontFamily.Serif, fontSize = 20.sp, lineHeight = 1.7.em)
        fun baselineGap(spacerSp: Float): Float {
            val styling = ManuscriptStyling(
                TextRange(0), emptyList(), -1, Color.Black, Color.Red, Color.Yellow, Color.Yellow,
                TextAlign.Start, indentSp = 0f, lineHeightEm = 1.7f, focusMode = false, spacerSp = spacerSp,
            )
            val p = MultiParagraph(styling.filter(AnnotatedString("Ag\nAg")).text, style, Constraints(), density, createFontFamilyResolver())
            return p.getLineBaseline(1) - p.getLineBaseline(0)
        }
        // Meio em (0,5 × 20 sp) a mais entre os parágrafos, com tolerância de 2 px.
        val extra = baselineGap(spacer) - baselineGap(0f)
        val target = with(density) { 10.sp.toPx() }
        assertTrue(abs(extra - target) < 2f, "espaçador $spacer sp deu $extra px; esperado $target px")
    }
}
