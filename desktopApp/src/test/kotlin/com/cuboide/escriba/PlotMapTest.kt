package com.cuboide.escriba

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EnredoView
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.EscribaServices
import com.cuboide.escriba.app.Section
import com.cuboide.escriba.core.board.Pt
import com.cuboide.escriba.platform.JvmFileStore
import java.nio.file.Files
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Mapa de enredo com o mouse: mover cartão, puxar ligação da alça, excluir com Delete, alternar para o Roteiro. */
@OptIn(ExperimentalTestApi::class)
class PlotMapTest {
    private fun ComposeUiTest.bounds(description: String): Rect =
        onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot

    private fun ComposeUiTest.drag(from: Offset, to: Offset) {
        onRoot().performMouseInput {
            moveTo(from)
            press()
            repeat(12) { i -> moveTo(from + (to - from) * ((i + 1) / 12f)) }
            release()
        }
        waitForIdle()
    }

    @Test
    fun movesConnectsAndDeletesCards() {
        val dir = Files.createTempDirectory("escriba-mapa-ui").toFile()
        try {
            val services = EscribaServices(
                JvmFileStore(), "${dir.path}/lib", "${dir.path}/data",
                System::currentTimeMillis, { UUID.randomUUID().toString() }, { "2026-11-11" },
            )
            lateinit var c: EscribaController
            lateinit var a: String
            lateinit var b: String
            runDesktopComposeUiTest(width = 1500, height = 950) {
                setContent {
                    val scope = rememberCoroutineScope()
                    c = remember {
                        EscribaController(services, scope).also {
                            it.createBook("Livro do mapa", "")
                            it.goTo(Section.Enredo)
                            a = it.map.addCard(Pt(300f, 250f))
                            it.map.setNodeText(a, "Fenalia", "Rainha")
                            b = it.map.addCard(Pt(800f, 250f))
                            it.map.setNodeText(b, "Wirtoris", "Capitão")
                            it.map.clearSelection()
                        }
                    }
                    Box(Modifier.size(1500.dp, 950.dp)) { App(c) }
                }
                waitForIdle()
                assertEquals(EnredoView.Mapa, c.enredoView)

                // Arrastar o cartão A 100 × 150 para baixo e para a direita.
                val before = c.map.board!!.nodes.first { it.id == a }
                val ra = bounds("Cartão: Fenalia")
                drag(ra.center, ra.center + Offset(100f, 150f) * density.density)
                val moved = c.map.board!!.nodes.first { it.id == a }
                assertEquals(before.x + 100f, moved.x, 2f)
                assertEquals(before.y + 150f, moved.y, 2f)
                assertEquals(setOf(a), c.map.selection)

                // Puxar a alça da direita de A até o cartão B cria uma ligação.
                val ra2 = bounds("Cartão: Fenalia")
                val rb = bounds("Cartão: Wirtoris")
                onRoot().performMouseInput { moveTo(ra2.center) }
                drag(Offset(ra2.right, ra2.center.y), rb.center)
                val edges = c.map.board!!.edges
                assertEquals(1, edges.size)
                assertEquals(a to b, edges.single().from to edges.single().to)

                // Clicar em B e apertar Delete: some o cartão e a ligação.
                onRoot().performMouseInput { click(bounds("Cartão: Wirtoris").center) }
                waitForIdle()
                assertEquals(setOf(b), c.map.selection)
                onNodeWithContentDescription("Mapa de enredo").performKeyInput { pressKey(Key.Delete) }
                waitForIdle()
                assertEquals(listOf(a), c.map.board!!.nodes.map { it.id })
                assertTrue(c.map.board!!.edges.isEmpty())

                // Desfazer traz de volta.
                onNodeWithContentDescription("Desfazer (Ctrl+Z)").performClick()
                waitForIdle()
                assertEquals(2, c.map.board!!.nodes.size)
                assertEquals(1, c.map.board!!.edges.size)

                // O Roteiro continua lá.
                onNodeWithText("Roteiro").performClick()
                waitForIdle()
                onNodeWithText("O roteiro está vazio.").fetchSemanticsNode()
            }
        } finally {
            dir.deleteRecursively()
        }
    }

    /** Retrato de mentira: gradiente com um círculo, para os nós com imagem. */
    private fun portrait(hue: Float, w: Int = 400, h: Int = 300): ByteArray {
        val img = java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.paint = java.awt.GradientPaint(0f, 0f, java.awt.Color.getHSBColor(hue, 0.6f, 0.5f), w.toFloat(), h.toFloat(), java.awt.Color.getHSBColor(hue + 0.1f, 0.5f, 0.15f))
        g.fillRect(0, 0, w, h)
        g.color = java.awt.Color.getHSBColor(hue, 0.3f, 0.9f)
        g.fillOval(w / 2 - h / 5, h / 6, h * 2 / 5, h * 2 / 5)
        g.dispose()
        return java.io.ByteArrayOutputStream().also { javax.imageio.ImageIO.write(img, "png", it) }.toByteArray()
    }

    /** Quadro cheio (imagens, ligações coloridas com rótulo, ficha vinculada, texto solto); grava PNGs se pedido. */
    @Test
    fun rendersRichBoard() {
        val out = System.getProperty("escriba.layoutPreviewDir")?.let { java.io.File(it).apply { mkdirs() } }
        val dir = Files.createTempDirectory("escriba-mapa-rico").toFile()
        try {
            val services = EscribaServices(
                JvmFileStore(), "${dir.path}/lib", "${dir.path}/data",
                System::currentTimeMillis, { UUID.randomUUID().toString() }, { "2026-11-11" },
            )
            lateinit var c: EscribaController
            lateinit var edgeId: String
            runDesktopComposeUiTest(width = 1500, height = 950) {
                setContent {
                    val scope = rememberCoroutineScope()
                    c = remember {
                        EscribaController(services, scope).also {
                            it.updateSettings { s -> s.copy(theme = com.cuboide.escriba.core.storage.ThemeId.Tinta) }
                            it.createBook("Ecos", "")
                            it.goTo(Section.Enredo)
                            val path = it.book!!.path
                            val fen = it.createEntry(com.cuboide.escriba.core.model.EntryKind.Personagem)
                            it.saveEntry(fen.copy(name = "Fenalia Lancelace", summary = "Rainha de Cerulean, candidata a Divina"))
                            val m = it.map
                            fun img(h: Float) = services.books.importAsset(path, portrait(h), "png")
                            val a = m.addCard(Pt(250f, 200f)); m.linkEntry(a, fen.id)
                            val b = m.addCard(Pt(560f, 200f)); m.setNodeText(b, "Wirtoris", "Capitão da Guarda real do reino de Cerulean")
                            val d = m.addCard(Pt(250f, 520f)); m.setNodeText(d, "Mékira Bludbolt", "Descendente de Fákalo, sem Fides.")
                            val e = m.addCard(Pt(560f, 520f)); m.setNodeText(e, "Rolts Wegrun", "Sobreviveu a Elevenlore graças ao deus Zoruma.")
                            m.edit { bs -> bs.copy(boards = bs.boards.map { bd -> bd.copy(nodes = bd.nodes.mapIndexed { i, n ->
                                n.copy(image = img(0.1f * i + 0.55f), height = 280f, color = listOf("#5B8DEF", "#5DAE7B", "#D9605A", "#9C6ADE")[i])
                            }) }) }
                            m.connect(a, b); m.connect(a, d)
                            edgeId = m.board!!.edges.first().id
                            m.selectEdge(edgeId); m.setSelectionColor("#D9B44A"); m.setEdgeLabel(edgeId, "Lealdade")
                            m.selectEdge(m.board!!.edges.last().id); m.setSelectionColor("#D9605A")
                            m.edit { bs -> bs.copy(boards = bs.boards.map { bd -> bd.copy(nodes = bd.nodes + com.cuboide.escriba.core.board.BoardNode(
                                "castelo", com.cuboide.escriba.core.board.NodeKind.Imagem, 900f, 220f, 240f, 320f, title = "Segredos, magia, força – SPERANZA", image = img(0.62f),
                            ) + com.cuboide.escriba.core.board.BoardNode("t", com.cuboide.escriba.core.board.NodeKind.Texto, 870f, 90f, 300f, 64f, title = "FALCONIA", color = "#D9605A")) }) }
                            m.connect("castelo", b)
                            m.clearSelection()
                        }
                    }
                    Box(Modifier.size(1500.dp, 950.dp)) { App(c) }
                }
                waitForIdle()
                Thread.sleep(800)
                waitForIdle()
                out?.let { javax.imageio.ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", java.io.File(it, "mapa-escuro.png")) }
                // Ligação selecionada: barra de cores e rótulo.
                runOnIdle { c.map.selectEdge(edgeId) }
                waitForIdle()
                onNodeWithText("LIGAÇÃO").fetchSemanticsNode()
                out?.let { javax.imageio.ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", java.io.File(it, "mapa-ligacao.png")) }
                // Passar o mouse num cartão mostra as alças e as ferramentas.
                runOnIdle { c.map.clearSelection() }
                onRoot().performMouseInput { moveTo(bounds("Cartão: Wirtoris").center) }
                waitForIdle()
                onNodeWithContentDescription("Vincular ficha").fetchSemanticsNode()
                out?.let { javax.imageio.ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", java.io.File(it, "mapa-hover.png")) }
                // Duplo clique na imagem do cartão: ajuste da imagem. Arrastar move, roda amplia, alça muda a altura.
                val card = bounds("Cartão: Wirtoris")
                val imgPoint = Offset(card.center.x, card.top + card.height * 0.2f)
                onRoot().performMouseInput { doubleClick(imgPoint) }
                waitForIdle()
                val wid = c.map.board!!.nodes.first { it.title == "Wirtoris" }.id
                assertEquals(wid, c.map.framingId)
                onNodeWithContentDescription("Ajustando imagem").fetchSemanticsNode()
                onRoot().performMouseInput { moveTo(imgPoint); scroll(-3f) }
                waitForIdle()
                assertTrue(c.map.board!!.nodes.first { it.id == wid }.frame.zoom > 1.2f, "a roda ampliou a imagem, não o quadro")
                assertEquals(1f, c.map.board!!.viewport.zoom, 0.01f)
                val before = c.map.board!!.nodes.first { it.id == wid }.frame
                drag(imgPoint, imgPoint + Offset(-60f, 0f))
                assertTrue(c.map.board!!.nodes.first { it.id == wid }.frame.x > before.x, "arrastar para a esquerda mostra mais da direita")
                val ratioBefore = c.map.board!!.nodes.first { it.id == wid }.imageRatio
                val handle = bounds("Altura da imagem")
                drag(Offset(handle.center.x, handle.bottom + 2f), Offset(handle.center.x, handle.bottom + 62f))
                assertTrue(c.map.board!!.nodes.first { it.id == wid }.imageRatio > ratioBefore + 0.1f, "a alça aumentou a imagem no cartão")
                out?.let { javax.imageio.ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", java.io.File(it, "mapa-ajuste.png")) }
                onNodeWithContentDescription("Mapa de enredo").performKeyInput { pressKey(Key.Escape) }
                waitForIdle()
                assertEquals(null, c.map.framingId)
                out?.let { javax.imageio.ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", java.io.File(it, "mapa-ajustado.png")) }
                // Desfazer volta o enquadramento inteiro de uma vez.
                runOnIdle { c.map.undo() }
                assertEquals(com.cuboide.escriba.core.board.ImageFrame(), c.map.board!!.nodes.first { it.id == wid }.frame)
                runOnIdle { c.map.redo() }
                // Tema claro e zoom pela roda.
                runOnIdle { c.updateSettings { s -> s.copy(theme = com.cuboide.escriba.core.storage.ThemeId.Papel) } }
                onRoot().performMouseInput { moveTo(Offset(700f, 500f)); scroll(3f) }
                waitForIdle()
                out?.let { javax.imageio.ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", java.io.File(it, "mapa-claro.png")) }
            }
        } finally {
            dir.deleteRecursively()
        }
    }
}
