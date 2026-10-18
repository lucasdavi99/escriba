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
import kotlin.test.assertEquals

/** Clique depois do fim de uma linha deixa o cursor nela (não no começo do parágrafo seguinte). */
@OptIn(ExperimentalTestApi::class)
class EditorClickTest {
    private fun clickPastFirstLineEnd(paragraphSpacing: Float): Int {
        val dir = Files.createTempDirectory("escriba-click").toFile()
        try {
            var controller: EscribaController? = null
            runDesktopComposeUiTest(width = 1600, height = 900) {
                setContent {
                    val scope = rememberCoroutineScope()
                    val c = remember {
                        val services = EscribaServices(
                            JvmFileStore(), "${dir.path}/lib", "${dir.path}/data",
                            System::currentTimeMillis, { UUID.randomUUID().toString() }, { "2026-10-18" },
                        )
                        EscribaController(services, scope).also {
                            it.updateSettings { s -> s.copy(paragraphSpacing = paragraphSpacing) }
                            it.createBook("Livro do clique", "")
                            // Capítulo mais alto que a janela: é quando o foco no primeiro clique rolava a folha.
                            it.onEditorChange(TextFieldValue("Primeiro parágrafo\nSegundo" + "\nMais uma linha do capítulo.".repeat(60)))
                        }
                    }
                    controller = c
                    Box(Modifier.size(1500.dp, 860.dp)) { App(c) }
                }
                waitForIdle()
                // O editor é o campo de texto editável mais alto da tela (os outros são de uma linha).
                val fields = onAllNodes(hasSetTextAction())
                val tallest = fields.fetchSemanticsNodes().withIndex().maxBy { it.value.size.height }.index
                // Clique de gente: o botão fica apertado um instante, tempo para a folha rolar se o foco pedir.
                fields[tallest].performMouseInput {
                    moveTo(Offset(width - 4f, 18f))
                    press()
                    advanceEventTime(400)
                    release()
                }
                waitForIdle()
            }
            return controller!!.editor.selection.start
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun clickPastLineEndStaysOnTheLine() {
        assertEquals("Primeiro parágrafo".length, clickPastFirstLineEnd(paragraphSpacing = 0f))
    }

    @Test
    fun clickPastLineEndWithParagraphSpacing() {
        assertEquals("Primeiro parágrafo".length, clickPastFirstLineEnd(paragraphSpacing = 1f))
    }
}
