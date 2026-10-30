package com.cuboide.escriba

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.EscribaServices
import com.cuboide.escriba.app.Section
import com.cuboide.escriba.core.layout.CoverPiece
import com.cuboide.escriba.core.layout.SpineDirection
import com.cuboide.escriba.platform.JvmFileStore
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Capa, segunda rodada: degradê, arrastar texto e arte na prévia, sentido da lombada. */
@OptIn(ExperimentalTestApi::class)
class CoverEditTest {
    private fun ComposeUiTest.dragNode(description: String, by: Offset) {
        val r = onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot
        onRoot().performMouseInput {
            moveTo(r.center)
            press()
            repeat(12) { moveBy(by / 12f) }
            release()
        }
        waitForIdle()
    }

    @Test
    fun gradientDragAndSpine() {
        val dir = Files.createTempDirectory("escriba-capa-edicao").toFile()
        try {
            val services = EscribaServices(
                JvmFileStore(), "${dir.path}/lib", "${dir.path}/data",
                System::currentTimeMillis, { UUID.randomUUID().toString() }, { "2026-10-30" },
            )
            lateinit var c: EscribaController
            runDesktopComposeUiTest(width = 1600, height = 1000) {
                setContent {
                    val scope = rememberCoroutineScope()
                    c = remember {
                        EscribaController(services, scope).also {
                            it.createBook("Capa montada", "Ana Autora")
                            it.goTo(Section.Diagramacao)
                            it.updateLayout { s -> s.copy(front = s.front.copy(isbn = "978-85-0000-000-3")) }
                        }
                    }
                    Box(Modifier.size(1600.dp, 1000.dp)) { App(c) }
                }
                waitUntil(timeoutMillis = 20_000) { c.layout != null && !c.layoutBusy }
                onNodeWithText("Capa").performClick()
                waitUntil(timeoutMillis = 10_000) { onAllNodesWithContentDescriptionCount("Arrastar Título") > 0 }

                // Sem arte, o degradê basta para gerar a capa.
                onNodeWithText("Degradê").performClick()
                waitForIdle()
                assertNotNull(c.layoutSettings.cover.gradient)
                waitUntil(timeoutMillis = 10_000) { onAllNodesWithTextCount("Sem arte de capa", substring = true) == 0 }

                // Arrastar o título 60 px para baixo.
                dragNode("Arrastar Título", Offset(0f, 60f))
                val off = c.layoutSettings.cover.offsets[CoverPiece.Titulo]
                assertTrue(off != null && off.y > 3f, "deslocamento do título: $off")

                // Lombada de baixo para cima.
                onNodeWithText("Sobe ↑").performScrollTo().performClick()
                waitForIdle()
                assertEquals(SpineDirection.Ascendente, c.layoutSettings.cover.spineDirection)

                // Com arte ampliada, o modo "A arte" desloca o enquadramento.
                val art = BufferedImage(4200, 2800, BufferedImage.TYPE_INT_RGB).apply {
                    createGraphics().run { color = Color(90, 60, 120); fillRect(0, 0, 4200, 2800); color = Color(230, 200, 120); fillOval(1500, 900, 1200, 1000); dispose() }
                }
                val bytes = ByteArrayOutputStream().also { ImageIO.write(art, "png", it) }.toByteArray()
                val relative = services.books.importAsset(c.book!!.path, bytes, "png")
                runOnIdle { c.updateCover { it.copy(image = relative, artZoom = 1.6f) } }
                waitForIdle()
                onNodeWithText("A arte").performScrollTo().performClick()
                waitUntil(timeoutMillis = 10_000) { onAllNodesWithContentDescriptionCount("Arrastar a arte") > 0 }
                waitForIdle()
                dragNode("Arrastar a arte", Offset(-80f, 0f))
                assertTrue(c.layoutSettings.cover.artOffsetXMm < -3f, "arte em x: ${c.layoutSettings.cover.artOffsetXMm}")

                Thread.sleep(400)
                waitForIdle()
                System.getProperty("escriba.layoutPreviewDir")?.let { out ->
                    ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(out, "capa-edicao.png"))
                }
            }
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun ComposeUiTest.onAllNodesWithContentDescriptionCount(d: String) =
        onAllNodes(androidx.compose.ui.test.hasContentDescription(d)).fetchSemanticsNodes().size

    private fun ComposeUiTest.onAllNodesWithTextCount(t: String, substring: Boolean) =
        onAllNodes(androidx.compose.ui.test.hasText(t, substring = substring)).fetchSemanticsNodes().size
}
