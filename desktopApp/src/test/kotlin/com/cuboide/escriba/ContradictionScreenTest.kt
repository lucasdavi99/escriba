package com.cuboide.escriba

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.EscribaServices
import com.cuboide.escriba.app.Section
import com.cuboide.escriba.core.model.EntryKind
import com.cuboide.escriba.core.universe.withAttr
import com.cuboide.escriba.core.universe.withBlock
import com.cuboide.escriba.platform.JvmFileStore
import java.nio.file.Files
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Selo na ficha, "Para conferir", contradições sublinhadas no Manuscrito e relatório; grava PNGs se pedido. */
@OptIn(ExperimentalTestApi::class)
class ContradictionScreenTest {
    @Test
    fun sealsAndShowsContradictions() {
        val out = System.getProperty("escriba.layoutPreviewDir")?.let { java.io.File(it).apply { mkdirs() } }
        fun shot(name: String, image: java.awt.image.BufferedImage) { out?.let { javax.imageio.ImageIO.write(image, "png", java.io.File(it, name)) } }
        val dir = Files.createTempDirectory("escriba-contradicoes-ui").toFile()
        try {
            val services = EscribaServices(
                JvmFileStore(), "${dir.path}/lib", "${dir.path}/data",
                System::currentTimeMillis, { UUID.randomUUID().toString() }, { "2026-11-12" },
            )
            lateinit var c: EscribaController
            lateinit var maraId: String
            runDesktopComposeUiTest(width = 1500, height = 1000) {
                setContent {
                    val scope = rememberCoroutineScope()
                    c = remember {
                        EscribaController(services, scope).also {
                            it.createBook("Livro do mundo", "")
                            it.chapter!!.type(
                                TextFieldValue(
                                    "Mara chegou ao porto ao amanhecer. Ela tinha 34 anos e o cabelo ruivo preso numa trança.\n" +
                                        "Quando o capitão a viu, os olhos azuis de Mara brilharam. Mais tarde, ela soltou o cabelo loiro.",
                                ),
                            )
                            it.flushSave()
                            val mara = it.createEntry(EntryKind.Personagem).copy(name = "Mara")
                                .withBlock("aparencia", "Olhos verdes e cabelo ruivo, canhota. Uma cicatriz no queixo.", it::newId)
                                .withAttr("olhos", "castanhos", it::newId)
                                .withBlock("aliados", "Tobias", it::newId)
                            maraId = mara.id
                            it.saveEntry(mara)
                            it.saveEntry(it.createEntry(EntryKind.Personagem).copy(name = "Tobias").withBlock("inimigos", "Mara", it::newId))
                            it.openUniverseEntry(mara.id)
                        }
                    }
                    Box(Modifier.size(1500.dp, 1000.dp)) { App(c) }
                }
                waitForIdle()
                // Ficha: selos e "Para conferir" (conflito nos olhos, relação que não bate).
                onNodeWithText("Selar na ficha").performScrollTo()
                waitForIdle()
                onNodeWithText("Na ficha de Mara, Tobias é aliado; na ficha de Tobias, Mara é inimigo.", substring = true).fetchSemanticsNode()
                shot("ficha-para-conferir.png", onRoot().captureToImage().toAwtImage())
                onNodeWithText("Selar na ficha").performClick()
                waitForIdle()
                val sealed = c.book!!.universe.entries.first { it.id == maraId }.attributes.filter { it.sealed }.mapNotNull { it.key }.toSet()
                assertEquals(setOf("cabelo", "mao", "marca"), sealed) // olhos ficou: estava em conflito e não foi escolhido
                onNodeWithContentDescription("Selar Olhos").performClick() // sela os olhos como estão ("castanhos")
                waitForIdle()

                // Manuscrito: conferência ligada, trechos sublinhados e o bloco do inspetor.
                runOnIdle { c.goTo(Section.Manuscrito) }
                waitForIdle()
                onNodeWithContentDescription("Conferir contradições").performClick()
                waitForIdle()
                val messages = c.bookCheck!!.contradictions.map { it.message }
                assertEquals(
                    listOf(
                        "Olhos de Mara: aqui diz “azul”, a ficha diz “castanhos”.",
                        "Cabelo de Mara: aqui diz “loiro”, a ficha diz “ruivo”.",
                    ),
                    messages,
                )
                assertEquals(2, c.currentIssues.size)
                onNodeWithText(messages[0]).fetchSemanticsNode()
                shot("manuscrito-contradicoes.png", onRoot().captureToImage().toAwtImage())

                // Relatório do livro inteiro: idades citadas e contradições.
                onNodeWithText("Relatório do livro inteiro").performClick()
                waitForIdle()
                onNodeWithText("• Mara: 34 anos", substring = true).fetchSemanticsNode()
                shot("relatorio-livro.png", onAllNodes(isRoot()).onLast().captureToImage().toAwtImage())
                assertTrue(onAllNodesWithText("Fatos com selo: 4", substring = true).fetchSemanticsNodes().isNotEmpty())
            }
        } finally {
            dir.deleteRecursively()
        }
    }
}
