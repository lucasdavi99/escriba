package com.cuboide.escriba

import androidx.compose.foundation.layout.Box
import com.cuboide.escriba.ui.Book3D
import com.cuboide.escriba.platform.decodeImageBitmap
import com.cuboide.escriba.core.layout.CoverSnapshot
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.EscribaServices
import com.cuboide.escriba.app.Section
import com.cuboide.escriba.core.layout.COVER_SNAPSHOT_FILE
import com.cuboide.escriba.core.layout.COVER_SNAPSHOT_IMAGE
import com.cuboide.escriba.core.layout.PaperStock
import com.cuboide.escriba.platform.JvmFileStore
import java.awt.BasicStroke
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/** Livro 3D na aba Capa e a vitrine na Biblioteca. */
@OptIn(ExperimentalTestApi::class)
class Book3DViewTest {
    /** Arte com grade e um círculo: deixa ver a perspectiva nas capturas. */
    private fun art(): ByteArray {
        val img = BufferedImage(3600, 2600, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.color = Color(28, 52, 92); g.fillRect(0, 0, 3600, 2600)
        g.color = Color(255, 255, 255, 60); g.stroke = BasicStroke(6f)
        for (x in 0 until 3600 step 150) g.drawLine(x, 0, x, 2600)
        for (y in 0 until 2600 step 150) g.drawLine(0, y, 3600, y)
        g.color = Color(232, 190, 96); g.fillOval(2300, 500, 900, 900)
        g.dispose()
        return ByteArrayOutputStream().also { ImageIO.write(img, "png", it) }.toByteArray()
    }

    @Test
    fun book3dInCoverTabAndLibrary() {
        val dir = Files.createTempDirectory("escriba-livro-3d").toFile()
        try {
            val services = EscribaServices(
                JvmFileStore(), "${dir.path}/lib", "${dir.path}/data",
                System::currentTimeMillis, { UUID.randomUUID().toString() }, { "2026-10-30" },
            )
            val text = List(120) { "Naquela manhã o farol amanheceu coberto de areia, e ninguém na vila sabia explicar como o vento tinha subido tão alto." }
                .joinToString("\n")
            lateinit var c: EscribaController
            val out = System.getProperty("escriba.layoutPreviewDir")
            runDesktopComposeUiTest(width = 1600, height = 1000) {
                setContent {
                    val scope = rememberCoroutineScope()
                    c = remember {
                        EscribaController(services, scope).also {
                            it.createBook("O Farol de Areia", "Marina Duarte")
                            it.onEditorChange(TextFieldValue(text))
                            it.goTo(Section.Diagramacao)
                            // Corpo grande e papel encorpado: lombada visível sem um livro enorme no teste.
                            it.updateLayout { s -> s.copy(body = s.body.copy(sizePt = 16f), page = s.page.copy(paper = PaperStock.PolenBold90)) }
                        }
                    }
                    Box(Modifier.size(1600.dp, 1000.dp)) { App(c) }
                }
                waitUntil(timeoutMillis = 30_000) { c.layout != null && !c.layoutBusy }
                val relative = services.books.importAsset(c.book!!.path, art(), "png")
                runOnIdle { c.updateCover { it.copy(image = relative) } }
                onNodeWithText("Capa").performClick()
                onNodeWithContentDescription("Livro 3D: o livro pronto, nas medidas reais").performClick()
                waitUntil(timeoutMillis = 20_000) { c.coverSnapshotVersion > 0 && onAllNodes(hasContentDescription("Livro 3D")).fetchSemanticsNodes().isNotEmpty() }
                waitForIdle()
                assertTrue(File(c.book!!.path, COVER_SNAPSHOT_IMAGE).exists() && File(c.book!!.path, COVER_SNAPSHOT_FILE).exists())
                out?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it, "livro-3d.png")) }

                // Arrastar gira o livro (mostra a contracapa de lado).
                val r = onNodeWithContentDescription("Livro 3D").fetchSemanticsNode().boundsInRoot
                onAllNodes(isRoot())[0].performMouseInput {
                    moveTo(r.center); press(); repeat(10) { moveBy(Offset(-25f, 4f)) }; release()
                }
                waitForIdle()
                out?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it, "livro-3d-girado.png")) }

                // Na Biblioteca, o "Continuar escrevendo" mostra o livro 3D.
                runOnIdle { c.goTo(Section.Biblioteca) }
                waitUntil(timeoutMillis = 10_000) { onAllNodes(hasContentDescription("Livro 3D")).fetchSemanticsNodes().isNotEmpty() }
                waitForIdle()
                out?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it, "biblioteca-3d.png")) }
            }
        } finally {
            dir.deleteRecursively()
        }
    }

    /** Pele sintética com faixas rotuladas: confere qual faixa vai em qual face e o sentido delas. */
    @Test
    fun skinBandsLandOnTheRightFaces() {
        val book = CoverSnapshot(140f, 210f, 30f)
        val pxPerMm = 6
        val img = BufferedImage(((2 * 140 + 30) * pxPerMm), 210 * pxPerMm, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.font = java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 90)
        g.color = Color(150, 40, 40); g.fillRect(0, 0, 140 * pxPerMm, img.height)
        g.color = Color(40, 120, 60); g.fillRect(140 * pxPerMm, 0, 30 * pxPerMm, img.height)
        g.color = Color(40, 60, 150); g.fillRect(170 * pxPerMm, 0, 140 * pxPerMm, img.height)
        g.color = Color.WHITE
        g.drawString("VERSO", 120, 300); g.drawString("FRENTE", 170 * pxPerMm + 120, 300)
        // Na lombada, o texto desce (como no Brasil): topo da faixa = cabeça do livro.
        val t = g.transform; g.translate(150.0 * pxPerMm, 120.0); g.rotate(Math.PI / 2); g.drawString("LOMBADA ↓", 0, 0); g.transform = t
        g.dispose()
        val png = ByteArrayOutputStream().also { ImageIO.write(img, "png", it) }.toByteArray()
        val skin = decodeImageBitmap(png)!!
        runDesktopComposeUiTest(width = 900, height = 520) {
            setContent {
                Row {
                    Book3D(skin, book, Modifier.size(300.dp), initialYaw = 40f)
                    Book3D(skin, book, Modifier.size(300.dp), initialYaw = -40f, initialTilt = -8f)
                    Book3D(skin, book, Modifier.size(300.dp), initialYaw = 150f, initialTilt = 10f)
                }
            }
            waitForIdle()
            System.getProperty("escriba.layoutPreviewDir")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it, "livro-3d-faixas.png")) }
        }
    }
}
