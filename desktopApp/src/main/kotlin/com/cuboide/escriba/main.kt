package com.cuboide.escriba

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.cuboide.escriba.app.EscribaController

fun main() = application {
    val scope = rememberCoroutineScope()
    val controller = remember { EscribaController(defaultServices(), scope) }
    Window(
        // O autosave espera uma pausa na digitação; ao fechar, grava na hora o que ainda está pendente.
        onCloseRequest = { controller.flushSave(); exitApplication() },
        title = "Escriba",
        // Lembrado: recriar o estado a cada recomposição devolvia a janela ao tamanho inicial (desfazia maximizar).
        state = rememberWindowState(width = 1360.dp, height = 860.dp),
    ) {
        App(controller)
    }
}
