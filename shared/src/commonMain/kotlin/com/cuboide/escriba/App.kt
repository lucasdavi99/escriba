package com.cuboide.escriba

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.EscribaServices
import com.cuboide.escriba.app.Section
import com.cuboide.escriba.core.text.MarkKind
import com.cuboide.escriba.platform.appDataDir
import com.cuboide.escriba.platform.currentTimeMillis
import com.cuboide.escriba.platform.defaultLibraryDir
import com.cuboide.escriba.platform.localDayKey
import com.cuboide.escriba.platform.newId
import com.cuboide.escriba.platform.platformFileStore
import com.cuboide.escriba.ui.AppShell
import com.cuboide.escriba.ui.EscribaTheme

fun defaultServices() = EscribaServices(
    fs = platformFileStore(),
    libraryDir = defaultLibraryDir(),
    dataDir = appDataDir(),
    now = ::currentTimeMillis,
    newId = ::newId,
    dayKey = ::localDayKey,
)

/** Atalhos: Ctrl+1…9 trocam de seção na ordem do menu. */
private val sectionShortcuts = listOf(
    Key.One to Section.Manuscrito, Key.Two to Section.Enredo, Key.Three to Section.Universo,
    Key.Four to Section.Metas, Key.Five to Section.Versoes, Key.Six to Section.Busca, Key.Seven to Section.Exportar,
).toMap()

@Composable
fun App(controller: EscribaController? = null) {
    val scope = rememberCoroutineScope()
    val c = controller ?: remember { EscribaController(defaultServices(), scope) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(c.message) {
        val text = c.message ?: return@LaunchedEffect
        snackbar.showSnackbar(text)
        c.message = null
    }

    EscribaTheme(theme = c.settings.theme, editorFont = c.settings.editorFont) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize().onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (e.key == Key.F11 && c.book != null) {
                    c.goTo(Section.Manuscrito); c.focusMode = !c.focusMode; return@onPreviewKeyEvent true
                }
                if (e.key == Key.Escape && c.focusMode) { c.focusMode = false; return@onPreviewKeyEvent true }
                val mod = e.isCtrlPressed || e.isMetaPressed
                if (!mod || c.book == null) return@onPreviewKeyEvent false
                sectionShortcuts[e.key]?.let { c.goTo(it); return@onPreviewKeyEvent true }
                val inEditor = c.section == Section.Manuscrito
                // Ctrl+Z no editor usa o desfazer do manuscrito (que também desfaz formatação e substituições).
                val typing = inEditor && c.editorFocused
                when (e.key) {
                    Key.S -> { c.flushSave(); c.map.flush(); true }
                    Key.F -> { if (e.isShiftPressed) c.goTo(Section.Busca) else c.openFind(); true }
                    Key.H -> { c.openFind(); true }
                    Key.B -> if (inEditor) { c.toggleMark(MarkKind.Bold); true } else false
                    Key.I -> if (inEditor) { c.toggleMark(MarkKind.Italic); true } else false
                    Key.Z -> if (typing) { if (e.isShiftPressed) c.redo() else c.undo(); true } else false
                    Key.Y -> if (typing) { c.redo(); true } else false
                    else -> false
                }
            },
        ) {
            Box(Modifier.fillMaxSize()) {
                AppShell(c)
                SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp))
            }
        }
    }
}
