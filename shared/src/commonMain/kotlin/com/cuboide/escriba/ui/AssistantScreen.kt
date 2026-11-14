package com.cuboide.escriba.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.Section
import kotlin.math.roundToInt

/** A aba Assistente: conversas do livro à esquerda e o chat no centro. */
@Composable
fun AssistantScreen(c: EscribaController) {
    val a = c.assistant
    SectionPage("Assistente", "Ideias, revisão e leitura crítica com o que já está no seu livro") {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.lg)) {
            Column(Modifier.width(220.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                OutlinedButton(onClick = { a.newConversation() }, modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.material3.Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                    Text("  Nova conversa")
                }
                if (a.conversations.isEmpty()) {
                    Text("As conversas ficam guardadas neste livro.", style = MaterialTheme.typography.bodySmall, color = LocalPalette.current.muted)
                }
                LazyColumn(Modifier.weight(1f)) {
                    items(a.conversations, key = { it.id }) { conv ->
                        HoverRow(selected = conv.id == a.currentId, onClick = { a.openConversation(conv.id) }, modifier = Modifier.fillMaxWidth()) { hovered ->
                            Text(
                                conv.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 8.dp),
                            )
                            if (hovered) IconAction(Icons.Outlined.DeleteOutline, "Apagar conversa", { a.deleteConversation(conv.id) }, size = 28.dp)
                        }
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                AssistantChat(c, Modifier.fillMaxSize().widthIn(max = 820.dp))
            }
        }
    }
}

/**
 * O assistente como painel flutuante: aparece sobre qualquer tela do livro (Ctrl+J), pode ser movido pelo título e
 * redimensionado pelo canto, e mantém a conversa ao trocar de seção. Fica fora da aba Assistente, que já mostra o chat.
 */
@Composable
fun AssistantPanelHost(c: EscribaController) {
    val a = c.assistant
    if (c.book == null || !a.panelVisible || c.section == Section.Assistente) return
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxW = maxWidth.value
        val maxH = maxHeight.value
        val w = a.panelWidth.coerceIn(300f, (maxW - 16f).coerceAtLeast(300f))
        val h = a.panelHeight.coerceIn(320f, (maxH - 16f).coerceAtLeast(320f))
        val x = (a.panelX ?: (maxW - w - 24f)).coerceIn(0f, (maxW - w).coerceAtLeast(0f))
        val y = (a.panelY ?: (maxH - h - 24f)).coerceIn(0f, (maxH - h).coerceAtLeast(0f))
        Box(
            Modifier
                .offset { IntOffset((x * density.density).roundToInt(), (y * density.density).roundToInt()) }
                .size(w.dp, h.dp),
        ) {
            FloatingPanel(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().padding(Space.sm)) {
                    Row(
                        Modifier.fillMaxWidth().pointerInput(Unit) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                a.panelX = (x + drag.x / density.density).coerceIn(0f, (maxW - w).coerceAtLeast(0f))
                                a.panelY = (y + drag.y / density.density).coerceIn(0f, (maxH - h).coerceAtLeast(0f))
                            }
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Icon(Icons.Outlined.DragIndicator, null, Modifier.size(18.dp), tint = LocalPalette.current.muted)
                        Text("Assistente", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).padding(start = 4.dp))
                        IconAction(Icons.Outlined.Add, "Nova conversa", { a.newConversation() }, size = 30.dp)
                        IconAction(Icons.Outlined.OpenInFull, "Abrir na aba Assistente", { a.panelVisible = false; c.goTo(Section.Assistente) }, size = 30.dp)
                        IconAction(Icons.Outlined.Close, "Fechar (Ctrl+J)", { a.panelVisible = false }, size = 30.dp)
                    }
                    Hairline(Modifier.padding(vertical = Space.xs))
                    AssistantChat(c, Modifier.weight(1f).fillMaxWidth(), compact = true)
                }
            }
            // Alça de redimensionar, no canto inferior direito.
            Box(
                Modifier.align(Alignment.BottomEnd).size(18.dp).pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        a.panelX = x
                        a.panelY = y
                        a.panelWidth = (w + drag.x / density.density).coerceIn(300f, (maxW - x).coerceAtLeast(300f))
                        a.panelHeight = (h + drag.y / density.density).coerceIn(320f, (maxH - y).coerceAtLeast(320f))
                    }
                },
            )
        }
    }
}
