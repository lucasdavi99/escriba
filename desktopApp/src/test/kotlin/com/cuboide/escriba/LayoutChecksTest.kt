package com.cuboide.escriba

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.EscribaServices
import com.cuboide.escriba.app.Section
import com.cuboide.escriba.core.layout.BleedPreset
import com.cuboide.escriba.core.layout.CheckLevel
import com.cuboide.escriba.core.layout.checkLayout
import com.cuboide.escriba.platform.JvmFileStore
import java.io.File
import java.nio.file.Files
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Conferência em semáforo, portão antes do PDF, ficha técnica e cabeçalho/número por página e por capítulo. */
@OptIn(ExperimentalTestApi::class)
class LayoutChecksTest {
    @Test
    fun gateFixSpecAndPageHeads() {
        val dir = Files.createTempDirectory("escriba-conferencia-ui").toFile()
        try {
            val services = EscribaServices(
                JvmFileStore(), "${dir.path}/lib", "${dir.path}/data",
                System::currentTimeMillis, { UUID.randomUUID().toString() }, { "2026-10-30" },
            )
            val text = List(40) { "Naquela manhã o farol amanheceu coberto de areia, e ninguém na vila sabia explicar como o vento tinha subido tão alto." }
                .joinToString("\n")
            lateinit var c: EscribaController
            runDesktopComposeUiTest(width = 1600, height = 1000) {
                setContent {
                    val scope = rememberCoroutineScope()
                    c = remember {
                        EscribaController(services, scope).also {
                            it.createBook("Livro conferido", "Ana Autora")
                            it.onEditorChange(TextFieldValue(text))
                            it.goTo(Section.Diagramacao)
                            it.updateLayout { s ->
                                s.copy(
                                    page = s.page.copy(marginOuterMm = 5f, bleed = BleedPreset.Nacional),
                                    heads = s.heads.copy(romanFrontMatter = true),
                                )
                            }
                        }
                    }
                    Box(Modifier.size(1600.dp, 1000.dp)) { App(c) }
                }
                fun settled() = waitUntil(timeoutMillis = 20_000) { !c.layoutBusy && c.layout?.settings == c.layoutSettings }
                settled()
                waitForIdle()

                // Margem externa a 5 mm com sangria: vermelho, e o PDF para no portão.
                onNodeWithText("Tem coisa que sairia errada no impresso").assertExists()
                onNodeWithText("Gerar PDF").performClick()
                waitForIdle()
                assertTrue(c.layoutGate?.isNotEmpty() == true)
                onNodeWithText("Ver e corrigir").performClick()
                waitForIdle()
                assertNull(c.layoutGate)

                // O conserto de um clique tira o ponto crítico.
                onNodeWithText("Aumentar as margens").performClick()
                settled()
                waitForIdle()
                assertTrue(checkLayout(c.layout!!).none { it.level == CheckLevel.Critico })

                // Ficha técnica para orçamento.
                onNodeWithText("FICHA TÉCNICA PARA ORÇAMENTO").performClick()
                waitForIdle()
                onNode(hasText("Formato fechado: 140 × 210 mm", substring = true)).assertExists()
                onNodeWithText("Copiar a ficha").performClick()

                // Página de texto do capítulo: sem número nesta página e cabeçalho próprio do capítulo.
                // Pelo índice da esquerda a lista rola até a abertura do capítulo; clicar nela seleciona a página.
                val laid = c.layout!!
                val page = laid.pages[laid.bodyStart - 1]
                onAllNodesWithText(laid.toc.first().label)[0].performClick()
                mainClock.advanceTimeBy(1_000)
                waitForIdle()
                onNodeWithContentDescription("Página ${page.number}").performClick()
                waitForIdle()
                onNodeWithText("Sem número nesta página (continua contando)").performClick()
                waitForIdle()
                assertTrue(page.number in c.layoutSettings.heads.hiddenFolios)
                onNode(hasText("Página direita") and hasSetTextAction()).performTextInput("A areia")
                waitForIdle()
                assertEquals("A areia", c.layoutSettings.heads.chapterHeads[page.chapterId]?.recto)
                settled()
                waitForIdle()
                System.getProperty("escriba.layoutPreviewDir")?.let { out ->
                    ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(out, "conferencia.png"))
                }
            }
        } finally {
            dir.deleteRecursively()
        }
    }
}
