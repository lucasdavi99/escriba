package com.cuboide.escriba

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.EscribaServices
import com.cuboide.escriba.app.Section
import com.cuboide.escriba.core.ai.AiProvider
import com.cuboide.escriba.core.ai.AiRef
import com.cuboide.escriba.core.ai.AiRequest
import com.cuboide.escriba.platform.JvmFileStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.nio.file.Files
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A aba Assistente e o painel flutuante, com um provedor de IA falso. */
@OptIn(ExperimentalTestApi::class)
class AssistantScreenTest {
    private val fake = object : AiProvider {
        // Chega em pedaços, como no streaming real, e com o Markdown que os modelos costumam usar.
        override fun stream(req: AiRequest): Flow<String> = flowOf(
            "Resposta de teste. Tenho o **Prólogo: Recomeço** inteiro.\n\n---\n\n### Análise do **Prólogo**\n\n",
            "**O que funciona:**\n* **Ritmo:** a abertura no vazio prende.\n" +
                "* **Gancho:** a frase *\"Mesmo tendo entregado tudo\"* planta um mistério.\n",
            "  * *\"pós vida\"* → **pós-vida**.\n1. Primeiro ponto\n2. Segundo ponto\n\n> Uma citação do texto.\n",
        )
    }

    @Test
    fun tabChatAndFloatingPanelKeepTheConversation() {
        val dir = Files.createTempDirectory("escriba-assistente-ui").toFile()
        try {
            val services = EscribaServices(
                JvmFileStore(), "${dir.path}/lib", "${dir.path}/data",
                System::currentTimeMillis, { UUID.randomUUID().toString() }, { "2026-11-14" },
                aiProvider = { fake },
            )
            lateinit var c: EscribaController
            runDesktopComposeUiTest(width = 1400, height = 900) {
                setContent {
                    val scope = rememberCoroutineScope()
                    c = remember {
                        EscribaController(services, scope).also {
                            it.assistant.updateConfig { cfg -> cfg.copy(apiKey = "k", consentGiven = true) }
                            it.createBook("Livro", "")
                        }
                    }
                    Box(Modifier.size(1400.dp, 900.dp)) { com.cuboide.escriba.App(c) }
                }
                waitForIdle()

                // A aba: o chat pede algo e recebe a resposta em streaming.
                runOnIdle { c.goTo(Section.Assistente) }
                waitForIdle()
                onNodeWithText("Pergunte ou peça algo", substring = true).assertExists()
                onNode(hasSetTextAction()).performTextInput("Ideias para o cap 2")
                onNodeWithContentDescription("Enviar (Enter)").performClick()
                waitUntil(timeoutMillis = 5000) { onAllNodesWithText("Resposta de teste.", substring = true).fetchSemanticsNodes().isNotEmpty() }
                waitForIdle()
                // O Markdown aparece formatado, sem os asteriscos.
                assertTrue(onAllNodesWithText("**", substring = true).fetchSemanticsNodes().isEmpty())
                // Modo da conversa: escolhido pelo seletor acima do campo (antes de abrir dicas de botões por cima dele).
                onNodeWithText("Modo: Livre", substring = true).performClick()
                waitForIdle()
                onNodeWithText("Revisor").performClick()
                waitForIdle()
                onNodeWithText("Modo: Revisor", substring = true).assertExists()
                runOnIdle { assertEquals("revisor", c.assistant.current!!.modeId) }
                // "Livro inteiro" ligado pelo botão ao lado do campo: aparece o chip com a estimativa de tokens.
                onNodeWithContentDescription("Ler o livro inteiro", substring = true).performClick()
                waitForIdle()
                onNodeWithText("Livro inteiro ·", substring = true).assertExists()
                System.getProperty("escriba.layoutPreviewDir")?.let { dir ->
                    val img = onAllNodes(isRoot()).onFirst().captureToImage().toAwtImage() // a dica do botão abre outra raiz
                    javax.imageio.ImageIO.write(img, "png", java.io.File(java.io.File(dir).apply { mkdirs() }, "assistente-markdown.png"))
                }

                // O painel abre em qualquer seção (aqui o Enredo) com a mesma conversa, e não aparece na aba.
                runOnIdle { c.goTo(Section.Enredo); c.assistant.togglePanel() }
                waitForIdle()
                onNodeWithContentDescription("Fechar (Ctrl+J)").assertExists()
                onAllNodesWithText("Resposta de teste.", substring = true).fetchSemanticsNodes().let { assertTrue(it.isNotEmpty()) }
                runOnIdle { c.goTo(Section.Universo) }
                waitForIdle()
                onNodeWithContentDescription("Fechar (Ctrl+J)").assertExists()
                runOnIdle { c.goTo(Section.Assistente) }
                waitForIdle()
                assertTrue(onAllNodesWithContentDescription("Fechar (Ctrl+J)").fetchSemanticsNodes().isEmpty())

                // `@` abre a lista de referências e a escolha vira um chip.
                onNode(hasSetTextAction()).performTextInput("@sino")
                waitForIdle()
                onNodeWithText("Sinopse").performClick()
                waitForIdle()
                runOnIdle { assertEquals(listOf<AiRef>(AiRef.Synopsis), c.assistant.draftRefs) }

                // Preferências: instruções gerais e modos editáveis.
                runOnIdle { c.goTo(Section.Preferencias) }
                waitForIdle()
                onNodeWithText("Modos do chat").performScrollTo()
                waitForIdle()
                onNodeWithText("Restaurar padrões").assertExists()
                System.getProperty("escriba.layoutPreviewDir")?.let { dir ->
                    val img = onAllNodes(isRoot()).onFirst().captureToImage().toAwtImage()
                    javax.imageio.ImageIO.write(img, "png", java.io.File(dir, "assistente-preferencias.png"))
                }
            }
        } finally {
            dir.deleteRecursively()
        }
    }
}
