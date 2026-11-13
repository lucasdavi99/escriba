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
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EnredoView
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.EscribaServices
import com.cuboide.escriba.app.Section
import com.cuboide.escriba.app.UniverseTool
import com.cuboide.escriba.core.calendar.WorldDate
import com.cuboide.escriba.core.model.EntryKind
import com.cuboide.escriba.platform.JvmFileStore
import com.cuboide.escriba.core.universe.withAttr
import java.nio.file.Files
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Calendário do mundo (modelo, grade do mês, painel do dia, editor) e Linha do tempo do Enredo; grava PNGs se pedido. */
@OptIn(ExperimentalTestApi::class)
class CalendarScreenTest {
    @Test
    fun createsCalendarAndShowsTimeline() {
        val out = System.getProperty("escriba.layoutPreviewDir")?.let { java.io.File(it).apply { mkdirs() } }
        fun shot(name: String, image: java.awt.image.BufferedImage) { out?.let { javax.imageio.ImageIO.write(image, "png", java.io.File(it, name)) } }
        val dir = Files.createTempDirectory("escriba-calendario-ui").toFile()
        try {
            val services = EscribaServices(
                JvmFileStore(), "${dir.path}/lib", "${dir.path}/data",
                System::currentTimeMillis, { UUID.randomUUID().toString() }, { "2026-11-11" },
            )
            lateinit var c: EscribaController
            runDesktopComposeUiTest(width = 1500, height = 1000) {
                setContent {
                    val scope = rememberCoroutineScope()
                    c = remember {
                        EscribaController(services, scope).also {
                            it.createBook("Livro do mundo", "")
                            it.showCalendar()
                        }
                    }
                    Box(Modifier.size(1500.dp, 1000.dp)) { App(c) }
                }
                waitForIdle()
                assertEquals(UniverseTool.Calendario, c.universeTool)
                shot("calendario-modelos.png", onRoot().captureToImage().toAwtImage())

                // Escolher o modelo de exemplo cria o calendário já no presente da história.
                onNodeWithContentDescription("Modelo Aldamar (exemplo)").performClick()
                waitForIdle()
                assertEquals("Calendário de Aldamar", c.book!!.calendar?.name)
                runOnIdle {
                    val mara = c.createEntry(EntryKind.Personagem).copy(name = "Mara").withAttr("nasceu", "7 de Abelha, 580 E.P.", c::newId)
                    c.saveEntry(mara)
                    c.saveBeat(c.withWhen(c.newBeat().copy(title = "A chegada ao porto", characterIds = listOf(mara.id)), "7 de Abelha, 612 E.P."))
                    c.saveBeat(c.withWhen(c.newBeat().copy(title = "Infância", characterIds = listOf(mara.id)), "Trigo de 590 E.P."))
                    c.saveBeat(c.withWhen(c.newBeat().copy(title = "O cerco", summary = "Depois do presente."), "10 de Vento Norte, 612 E.P."))
                    c.saveBeat(c.newBeat().copy(title = "Ainda sem data"))
                }
                waitForIdle()
                onAllNodesWithText("É o presente da história.").fetchSemanticsNodes().let { assertEquals(1, it.size) }
                shot("calendario-mes.png", onRoot().captureToImage().toAwtImage())

                // Céu do dia (sol, as duas luas) e o gráfico do ano, embaixo da grade do mês.
                onNodeWithContentDescription("Lua Lume Pálido").fetchSemanticsNode()
                onNodeWithContentDescription("Lua Brasa").fetchSemanticsNode()
                onNodeWithText("☀ Nasce", substring = true).fetchSemanticsNode()
                onNodeWithContentDescription("Gráfico do ano").performScrollTo()
                waitForIdle()
                shot("calendario-ceu.png", onRoot().captureToImage().toAwtImage())

                // Calendário sem céu (como os criados antes das luas): aviso e botão que devolve as luas do modelo.
                val withSky = c.book!!.calendar!!
                runOnIdle { c.saveCalendar(withSky.copy(moons = emptyList(), sun = null)) }
                waitForIdle()
                onNodeWithText("Acrescentar luas e sol").performClick()
                waitForIdle()
                assertEquals(withSky.moons, c.book!!.calendar!!.moons)
                assertEquals(0, onAllNodesWithText("Acrescentar luas e sol").fetchSemanticsNodes().size)

                // Editor da estrutura.
                onNodeWithText("Editar a estrutura").performClick()
                waitForIdle()
                onNodeWithText("A estrutura do calendário").fetchSemanticsNode()
                shot("calendario-editor.png", onAllNodes(isRoot()).onLast().captureToImage().toAwtImage())
                onNodeWithText("Cancelar").performClick()
                waitForIdle()

                // Linha do tempo: ordem por data, idade da Mara em cada cena, presente entre as cenas.
                runOnIdle { c.enredoView = EnredoView.Linha; c.goTo(Section.Enredo) }
                waitForIdle()
                onNodeWithText("Mara · 10 anos").fetchSemanticsNode()  // Infância: Trigo vem depois de Abelha, aniversário já passou
                onNodeWithText("Mara · 32 anos").fetchSemanticsNode()  // chegada, no dia do aniversário
                onNodeWithContentDescription("Presente da história").fetchSemanticsNode()
                onNodeWithText("21 anos depois").fetchSemanticsNode() // de Trigo de 590 a 7 de Abelha de 612
                assertNotNull(onAllNodesWithText("Ainda sem data").fetchSemanticsNodes().firstOrNull())
                shot("linha-do-tempo.png", onRoot().captureToImage().toAwtImage())
                assertEquals(WorldDate(1111, "a3", 7), c.calendar.present)
            }
        } finally {
            dir.deleteRecursively()
        }
    }
}
