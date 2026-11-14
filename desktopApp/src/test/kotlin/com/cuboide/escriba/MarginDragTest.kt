package com.cuboide.escriba

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.EscribaServices
import com.cuboide.escriba.app.Section
import com.cuboide.escriba.core.layout.FrontMatter
import com.cuboide.escriba.core.layout.mmToPt
import com.cuboide.escriba.platform.JvmFileStore
import java.io.File
import java.nio.file.Files
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Margens arrastadas na própria página da Diagramação: na mancha global e na mancha própria de uma página. */
@OptIn(ExperimentalTestApi::class)
class MarginDragTest {
    private fun ComposeUiTest.pageBounds(n: Int): Rect = onNodeWithContentDescription("Página $n").fetchSemanticsNode().boundsInRoot

    /** Arrasta com o mouse de [from] por [by] (px), em passos, como uma pessoa. */
    private fun ComposeUiTest.drag(from: Offset, by: Offset) {
        onRoot().performMouseInput {
            moveTo(from)
            press()
            repeat(10) { moveBy(by / 10f) }
            release()
        }
        waitForIdle()
    }

    @Test
    fun dragsGlobalAndPageMargins() {
        val dir = Files.createTempDirectory("escriba-margens-ui").toFile()
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
                            it.createBook("Livro das margens", "Ana Autora")
                            it.chapter!!.type(TextFieldValue(text))
                            it.goTo(Section.Diagramacao)
                            it.updateLayout { s -> s.copy(front = FrontMatter(halfTitle = false, titlePage = false, credits = false, toc = false)) }
                        }
                    }
                    Box(Modifier.size(1600.dp, 1000.dp)) { App(c) }
                }
                waitUntil(timeoutMillis = 20_000) { c.layout?.pages?.size?.let { it >= 2 } == true && !c.layoutBusy }
                waitForIdle()

                // Página 1 (direita): a borda esquerda da mancha é a margem interna.
                val p = pageBounds(1)
                val pxPerMm = p.width / c.layout!!.settings.page.trim.widthMm
                val inner0 = c.layoutSettings.page.marginInnerMm
                drag(Offset(p.left + inner0 * pxPerMm, p.center.y), Offset(10f * pxPerMm, 0f))
                val inner1 = c.layoutSettings.page.marginInnerMm
                assertTrue(kotlin.math.abs(inner1 - (inner0 + 10f)) <= 1f, "interna $inner0 → $inner1 (esperava +10 mm)")
                assertTrue(c.layoutSettings.page.pageMargins.isEmpty())

                // Seleciona a página clicando fora da mancha e dá a ela uma mancha própria.
                onRoot().performMouseInput { click(Offset(p.left + 6f, p.top + 6f)) }
                waitForIdle()
                onNodeWithText("Caixa da escrita · página 1", substring = true, ignoreCase = true).assertExists()
                onNodeWithText("Só desta página").performClick()
                waitForIdle()
                assertTrue(c.layoutSettings.page.hasOwnMargins(1))

                // Arrastar a borda de baixo para cima aumenta a margem inferior só desta página.
                waitUntil(timeoutMillis = 20_000) { !c.layoutBusy && c.layout?.settings == c.layoutSettings }
                waitForIdle()
                val p2 = pageBounds(1)
                val bottom0 = c.layoutSettings.page.marginsFor(1).bottomMm
                val globalBottom = c.layoutSettings.page.marginBottomMm
                val boxBottomY = p2.bottom - bottom0 * pxPerMm
                drag(Offset(p2.center.x, boxBottomY), Offset(0f, -15f * pxPerMm))
                val own = c.layoutSettings.page.marginsFor(1)
                assertTrue(kotlin.math.abs(own.bottomMm - (bottom0 + 15f)) <= 1f, "inferior própria $bottom0 → ${own.bottomMm}")
                assertEquals(globalBottom, c.layoutSettings.page.marginBottomMm)

                waitUntil(timeoutMillis = 20_000) { !c.layoutBusy && c.layout?.settings == c.layoutSettings }
                waitForIdle()
                System.getProperty("escriba.layoutPreviewDir")?.let { out ->
                    ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(out, "margens.png"))
                }
                // A mancha desta página vira a do livro inteiro.
                onNodeWithText("Definir como global").performClick()
                waitForIdle()
                assertEquals(own.bottomMm, c.layoutSettings.page.marginBottomMm)
                assertTrue(c.layoutSettings.page.pageMargins.isEmpty())
                assertTrue(mmToPt(1f) > 0f)
            }
        } finally {
            dir.deleteRecursively()
        }
    }
}
