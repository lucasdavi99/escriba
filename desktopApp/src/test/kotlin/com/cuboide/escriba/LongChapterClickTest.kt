package com.cuboide.escriba

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.EscribaServices
import com.cuboide.escriba.platform.JvmFileStore
import java.nio.file.Files
import java.util.UUID
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
/** Em capítulo longo o clique continua acertando o parágrafo mesmo muito abaixo do começo (antes parava em ~8000 px). */
class LongChapterClickTest {
    @Test
    fun clickFarDownStillMovesCursor() {
        val dir = Files.createTempDirectory("escriba-probe").toFile()
        try {
            var controller: EscribaController? = null
            val lines = (1..400).map { "Parágrafo $it com algum texto para ocupar a linha inteira da folha e passar de uma linha ou não." }
            val text = lines.joinToString("\n")
            runDesktopComposeUiTest(width = 1600, height = 900) {
                setContent {
                    val scope = rememberCoroutineScope()
                    val c = remember {
                        val services = EscribaServices(JvmFileStore(), "${dir.path}/lib", "${dir.path}/data",
                            System::currentTimeMillis, { UUID.randomUUID().toString() }, { "2026-10-18" })
                        EscribaController(services, scope).also {
                            it.createBook("Livro", "")
                            it.chapter!!.type(TextFieldValue(text))
                        }
                    }
                    controller = c
                    Box(Modifier.size(1500.dp, 860.dp)) { App(c) }
                }
                waitForIdle()
                val fields = onAllNodes(hasSetTextAction())
                val tallest = fields.fetchSemanticsNodes().withIndex().maxBy { it.value.size.height }.index
                var last = 0
                for (step in 0..14) {
                    waitForIdle()
                    if (step > 0) fields[tallest].performMouseInput { moveTo(Offset(300f, 100f - fields.fetchSemanticsNodes()[tallest].boundsInRoot.top.coerceAtMost(0f))); scroll(40f) }
                    waitForIdle()
                    val n = fields.fetchSemanticsNodes()[tallest]
                    // clique a 400px do topo da janela
                    val y = 400f - n.boundsInRoot.top
                    fields[tallest].performMouseInput { moveTo(Offset(300f, y)); press(); advanceEventTime(100); release() }
                    waitForIdle()
                    val sel = controller!!.chapter!!.value.selection.start
                    last = text.substring(0, sel).count { it == '\n' } + 1
                }
                kotlin.test.assertTrue(last > 250, "cursor parou na linha $last")
                // Arrastar com o mouse também seleciona lá embaixo.
                val top = fields.fetchSemanticsNodes()[tallest].boundsInRoot.top
                fields[tallest].performMouseInput {
                    moveTo(Offset(100f, 400f - top)); press(); advanceEventTime(50)
                    moveTo(Offset(500f, 480f - top)); advanceEventTime(50)
                    release()
                }
                waitForIdle()
                kotlin.test.assertFalse(controller!!.chapter!!.value.selection.collapsed, "arrasto não selecionou nada")
            }
        } finally { dir.deleteRecursively() }
    }
}
