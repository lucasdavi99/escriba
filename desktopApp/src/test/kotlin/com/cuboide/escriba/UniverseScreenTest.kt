package com.cuboide.escriba

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.EscribaServices
import com.cuboide.escriba.app.Section
import com.cuboide.escriba.core.model.EntryKind
import com.cuboide.escriba.core.universe.EntrySchemas
import com.cuboide.escriba.core.universe.blockText
import com.cuboide.escriba.core.universe.withAttr
import com.cuboide.escriba.core.universe.withBlock
import com.cuboide.escriba.platform.JvmFileStore
import java.nio.file.Files
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/** Ficha estruturada: retrato, galeria, atributos, blocos, "No manuscrito" e editor de bloco; grava PNGs se pedido. */
@OptIn(ExperimentalTestApi::class)
class UniverseScreenTest {
    private fun png(hue: Float, w: Int = 360, h: Int = 460): ByteArray {
        val img = java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.paint = java.awt.GradientPaint(0f, 0f, java.awt.Color.getHSBColor(hue, 0.5f, 0.55f), w.toFloat(), h.toFloat(), java.awt.Color.getHSBColor(hue + 0.08f, 0.5f, 0.18f))
        g.fillRect(0, 0, w, h)
        g.color = java.awt.Color.getHSBColor(hue, 0.25f, 0.92f)
        g.fillOval(w / 2 - w / 5, h / 5, w * 2 / 5, w * 2 / 5)
        g.fillRoundRect(w / 2 - w / 3, h * 3 / 5, w * 2 / 3, h / 2, 80, 80)
        g.dispose()
        return java.io.ByteArrayOutputStream().also { javax.imageio.ImageIO.write(img, "png", it) }.toByteArray()
    }

    @Test
    fun rendersStructuredEntry() {
        val out = System.getProperty("escriba.layoutPreviewDir")?.let { java.io.File(it).apply { mkdirs() } }
        val dir = Files.createTempDirectory("escriba-universo-ui").toFile()
        try {
            val services = EscribaServices(
                JvmFileStore(), "${dir.path}/lib", "${dir.path}/data",
                System::currentTimeMillis, { UUID.randomUUID().toString() }, { "2026-11-11" },
            )
            lateinit var c: EscribaController
            lateinit var maraId: String
            runDesktopComposeUiTest(width = 1500, height = 1000) {
                setContent {
                    val scope = rememberCoroutineScope()
                    c = remember {
                        EscribaController(services, scope).also {
                            it.createBook("Livro do mundo", "")
                            it.chapter!!.type(androidx.compose.ui.text.input.TextFieldValue("Mara olhou o porto. A Capitã não dormia. Tobias chegou."))
                            it.flushSave()
                            val path = it.book!!.path
                            val mara = it.createEntry(EntryKind.Personagem)
                            maraId = mara.id
                            it.saveEntry(
                                mara.copy(name = "Mara Velasco", subtitle = "Capitã do Albatroz", tags = listOf("mar"))
                                    .withAttr(EntrySchemas.ALIASES, "Mara, a Capitã", it::newId)
                                    .withAttr("olhos", "cinzentos", it::newId)
                                    .withAttr("mao", "canhota", it::newId)
                                    .withBlock("aparencia", "Alta, magra, com uma **cicatriz** no queixo.", it::newId)
                                    .withBlock("personalidade", "Calada; decide rápido e não volta atrás.", it::newId)
                                    .withBlock("aliados", "Tobias\nIrmã Odete", it::newId)
                                    .withBlock(EntrySchemas.QUOTE, "O mar não espera ninguém.", it::newId),
                            )
                            it.addEntryImagePath(mara.id, services.books.importAsset(path, png(0.08f), "png"), asPortrait = true)
                            it.addEntryImagePath(mara.id, services.books.importAsset(path, png(0.55f), "png"), asPortrait = false)
                            it.saveEntry(it.createEntry(EntryKind.Personagem).copy(name = "Tobias"))
                            it.goTo(Section.Universo)
                        }
                    }
                    Box(Modifier.size(1500.dp, 1000.dp)) { App(c) }
                }
                waitForIdle()
                onNodeWithText("Citado em 1 de 1 capítulo(s)").fetchSemanticsNode()
                onNodeWithText("2×").fetchSemanticsNode() // "Mara" e "A Capitã" (apelidos contam)
                onNodeWithContentDescription("Retrato").fetchSemanticsNode()
                out?.let { javax.imageio.ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", java.io.File(it, "universo-ficha.png")) }

                // Editor do bloco: digitar grava na ficha.
                onNodeWithContentDescription("Bloco Arco do personagem").performClick()
                waitForIdle()
                onNodeWithText("Fechar").fetchSemanticsNode()
                // Com o diálogo aberto há duas raízes; a última é a do diálogo.
                out?.let { javax.imageio.ImageIO.write(onAllNodes(isRoot()).onLast().captureToImage().toAwtImage(), "png", java.io.File(it, "universo-bloco.png")) }
                onNodeWithText("Fechar").performClick()
                waitForIdle()

                // Busca pela lista: por apelido e por texto do bloco.
                onNodeWithText("Buscar nome, apelido, texto…").performTextInput("cicatriz")
                waitForIdle()
                // A busca pelo texto do bloco acha a Mara: o trecho aparece na linha da lista e no bloco da ficha.
                assertEquals(2, onAllNodesWithText("Alta, magra, com uma cicatriz no queixo.", substring = true).fetchSemanticsNodes().size)
                runOnIdle { c.flushEntrySave() }
                assertEquals("Alta, magra, com uma **cicatriz** no queixo.", services.books.loadUniverse(c.book!!.path).entries.first { it.id == maraId }.blockText("aparencia"))
            }
        } finally {
            dir.deleteRecursively()
        }
    }
}
