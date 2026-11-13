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
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.EscribaServices
import com.cuboide.escriba.app.Section
import com.cuboide.escriba.core.model.EntryKind
import com.cuboide.escriba.platform.JvmFileStore
import java.nio.file.Files
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Acervo (lista, importar, abrir um universo), "Trazer fichas" e "Unificar livros"; grava PNGs se pedido. */
@OptIn(ExperimentalTestApi::class)
class AcervoScreenTest {
    @Test
    fun acervoAndSagaDialogs() {
        val out = System.getProperty("escriba.layoutPreviewDir")?.let { java.io.File(it).apply { mkdirs() } }
        fun shot(name: String, image: java.awt.image.BufferedImage) { out?.let { javax.imageio.ImageIO.write(image, "png", java.io.File(it, name)) } }
        val dir = Files.createTempDirectory("escriba-acervo-ui").toFile()
        try {
            val services = EscribaServices(
                JvmFileStore(), "${dir.path}/lib", "${dir.path}/data",
                System::currentTimeMillis, { UUID.randomUUID().toString() }, { "2026-11-12" },
            )
            lateinit var c: EscribaController
            runDesktopComposeUiTest(width = 1500, height = 1000) {
                setContent {
                    val scope = rememberCoroutineScope()
                    c = remember {
                        EscribaController(services, scope).also {
                            it.createBook("A Travessia — Livro Dois", "")
                            it.saveEntry(it.createEntry(EntryKind.Personagem).copy(name = "Tobias"))
                            it.createBook("A Travessia — Livro Um", "")
                            it.saveEntry(it.createEntry(EntryKind.Personagem).copy(name = "Mara", subtitle = "Capitã do Albatroz"))
                            it.saveEntry(it.createEntry(EntryKind.Local).copy(name = "Porto Velho"))
                            it.goTo(Section.Universo)
                        }
                    }
                    Box(Modifier.size(1500.dp, 1000.dp)) { App(c) }
                }
                waitForIdle()
                onNodeWithText("Acervo").performClick()
                waitForIdle()
                onNodeWithText("Nenhum universo no Acervo").fetchSemanticsNode()
                onNodeWithText("Importar do livro atual").performClick()
                waitForIdle()
                shot("acervo-lista.png", onRoot().captureToImage().toAwtImage())

                // Abrir o universo importado: as fichas são as dele, sem calendário.
                onNodeWithContentDescription("Universo A Travessia — Livro Um — universo").performClick()
                waitForIdle()
                assertNotNull(c.acervo)
                onNodeWithText("Universo do Acervo · 2 ficha(s), fora de qualquer livro").fetchSemanticsNode()
                shot("acervo-universo.png", onRoot().captureToImage().toAwtImage())

                // Trazer fichas de outro livro para este universo.
                onNodeWithContentDescription("Mais ações do universo").performClick()
                waitForIdle()
                onNodeWithText("Trazer fichas").performClick()
                waitForIdle()
                shot("acervo-trazer.png", onAllNodes(isRoot()).onLast().captureToImage().toAwtImage())
                onNodeWithText("Cancelar").performClick()
                waitForIdle()

                // De volta ao livro: diálogo de unificar.
                onNodeWithText("← Universos do Acervo").performClick()
                waitForIdle()
                onNodeWithText("← Universo de “A Travessia — Livro Um”").performClick()
                waitForIdle()
                runOnIdle { c.closeAcervoUniverse() }
                assertNull(c.acervo)
                onNodeWithContentDescription("Mais ações do universo").performClick()
                waitForIdle()
                onNodeWithText("Unificar livros").performClick()
                waitForIdle()
                onNodeWithText("A Travessia — Livro Dois").performClick()
                shot("unificar-livros.png", onAllNodes(isRoot()).onLast().captureToImage().toAwtImage())
                onNodeWithText("Unificar").performClick()
                waitForIdle()
                assertEquals(listOf("Mara", "Porto Velho", "Tobias"), c.book!!.universe.entries.map { it.name })
                onNodeWithText("Saga").fetchSemanticsNode()
            }
        } finally {
            dir.deleteRecursively()
        }
    }
}
