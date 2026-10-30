package com.cuboide.escriba

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.EscribaServices
import com.cuboide.escriba.app.Section
import com.cuboide.escriba.platform.JvmFileStore
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.test.Test

/** A aba Capa da Diagramação: conferência bloqueia o PDF sem arte e libera com arte e ISBN válidos. */
@OptIn(ExperimentalTestApi::class)
class CoverTabTest {
    @Test
    fun coverTabBlocksExportUntilArtAndIsbn() {
        val dir = Files.createTempDirectory("escriba-capa-ui").toFile()
        try {
            val services = EscribaServices(
                JvmFileStore(), "${dir.path}/lib", "${dir.path}/data",
                System::currentTimeMillis, { UUID.randomUUID().toString() }, { "2026-10-27" },
            )
            lateinit var controller: EscribaController
            runDesktopComposeUiTest(width = 1600, height = 900) {
                setContent {
                    val scope = rememberCoroutineScope()
                    controller = remember {
                        EscribaController(services, scope).also {
                            it.createBook("Livro com capa", "Ana Autora")
                            it.goTo(Section.Diagramacao)
                        }
                    }
                    Box(Modifier.size(1600.dp, 900.dp)) { App(controller) }
                }
                waitUntil(timeoutMillis = 20_000) { controller.layout != null }
                onNodeWithText("Capa").performClick()
                waitUntil(timeoutMillis = 10_000) { onAllNodes(hasText("Sem arte de capa", substring = true)).fetchSemanticsNodes().isNotEmpty() }
                onNodeWithText("Gerar PDF da capa").assertIsNotEnabled()

                val art = BufferedImage(3800, 2700, BufferedImage.TYPE_INT_RGB).apply {
                    createGraphics().run { color = Color(40, 70, 110); fillRect(0, 0, 3800, 2700); dispose() }
                }
                val bytes = ByteArrayOutputStream().also { ImageIO.write(art, "png", it) }.toByteArray()
                val relative = services.books.importAsset(controller.book!!.path, bytes, "png")
                runOnIdle {
                    controller.updateCover { it.copy(image = relative) }
                    controller.updateLayout { it.copy(front = it.front.copy(isbn = "978-85-0000-000-3")) }
                }
                waitUntil(timeoutMillis = 20_000) {
                    onAllNodes(hasText("Sem arte de capa", substring = true)).fetchSemanticsNodes().isEmpty() &&
                        onAllNodes(hasText("ISBN inválido", substring = true)).fetchSemanticsNodes().isEmpty() &&
                        onAllNodes(hasText("Sem ISBN", substring = true)).fetchSemanticsNodes().isEmpty() &&
                        !controller.layoutBusy
                }
                waitForIdle()
                onNodeWithText("Gerar PDF da capa").assertIsEnabled()
                System.getProperty("escriba.layoutPreviewDir")?.let { out ->
                    Thread.sleep(500)
                    waitForIdle()
                    ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(out, "aba-capa.png"))
                }
            }
        } finally {
            dir.deleteRecursively()
        }
    }
}
