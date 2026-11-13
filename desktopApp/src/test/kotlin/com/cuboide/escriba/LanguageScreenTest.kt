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
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.EscribaServices
import com.cuboide.escriba.app.Section
import com.cuboide.escriba.app.UniverseTool
import com.cuboide.escriba.core.language.LangStyle
import com.cuboide.escriba.platform.JvmFileStore
import java.nio.file.Files
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/** Aba Idiomas: escolher o estilo, tradutor, nomes, variedades e sorteio; grava PNGs se pedido. */
@OptIn(ExperimentalTestApi::class)
class LanguageScreenTest {
    @Test
    fun createsAndShowsLanguage() {
        val out = System.getProperty("escriba.layoutPreviewDir")?.let { java.io.File(it).apply { mkdirs() } }
        fun shot(name: String, image: java.awt.image.BufferedImage) { out?.let { javax.imageio.ImageIO.write(image, "png", java.io.File(it, name)) } }
        val dir = Files.createTempDirectory("escriba-idiomas-ui").toFile()
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
                            it.createBook("Livro do mundo", "")
                            it.goTo(Section.Universo)
                            it.universeTool = UniverseTool.Idiomas
                        }
                    }
                    Box(Modifier.size(1500.dp, 1000.dp)) { App(c) }
                }
                waitForIdle()
                onNodeWithText("A língua do seu mundo").fetchSemanticsNode()
                shot("idiomas-estilos.png", onRoot().captureToImage().toAwtImage())
                onNodeWithContentDescription("Estilo Areia").performClick()
                waitForIdle()
                assertEquals(LangStyle.Areia, c.book!!.language?.style)
                onNodeWithContentDescription("Tradução").fetchSemanticsNode()
                shot("idiomas-lingua.png", onRoot().captureToImage().toAwtImage())
                onNodeWithText("Outros nomes").performScrollTo()
                waitForIdle()
                shot("idiomas-nomes.png", onRoot().captureToImage().toAwtImage())

                // Sortear outra e voltar.
                val name = c.bookLanguage!!.name
                onNodeWithText("Sortear outra língua").performScrollTo().performClick()
                waitForIdle()
                assertNotEquals(name, c.bookLanguage!!.name)
                onNodeWithText("Voltar à anterior").performClick()
                waitForIdle()
                assertEquals(name, c.bookLanguage!!.name)
            }
        } finally {
            dir.deleteRecursively()
        }
    }
}
