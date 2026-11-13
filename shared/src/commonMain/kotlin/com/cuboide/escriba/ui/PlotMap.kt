package com.cuboide.escriba.ui

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.AutoAwesomeMosaic
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.core.board.BOARD_COLORS
import com.cuboide.escriba.core.board.Board
import com.cuboide.escriba.core.board.NodeKind
import com.cuboide.escriba.core.board.PlotBoards
import com.cuboide.escriba.core.board.bounds
import com.cuboide.escriba.core.board.edge
import com.cuboide.escriba.core.board.legendName
import com.cuboide.escriba.core.board.node
import com.cuboide.escriba.platform.currentTimeMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlin.math.roundToInt

/** Mapa de enredo: abas dos quadros, ferramentas, o quadro, a barra da seleção e a legenda das ligações. */
@Composable
fun PlotMap(c: EscribaController, header: @Composable () -> Unit, modifier: Modifier = Modifier) {
    val boards = c.map.boards ?: return
    val board = boards.active ?: return
    val view = remember(board.id) { BoardView(board.viewport) }
    var linkMode by remember(board.id) { mutableStateOf(false) }

    // Pan e zoom vão para o arquivo pouco depois de parar de mexer.
    LaunchedEffect(view) {
        snapshotFlow { view.viewport }.drop(1).collectLatest {
            delay(500)
            c.map.setViewport(it)
        }
    }

    Column(modifier.background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().padding(start = Space.page, end = Space.xl, top = 36.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            header()
            Spacer(Modifier.width(16.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                FloatingPanel {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        var addMenu by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { addMenu = true }) {
                                Icon(Icons.Outlined.Add, null, Modifier.size(17.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Adicionar")
                            }
                            DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
                                DropdownMenuItem(text = { Text("Cartão") }, leadingIcon = { Icon(Icons.AutoMirrored.Outlined.StickyNote2, null) }, onClick = { addMenu = false; c.map.addCard(view.center()) })
                                DropdownMenuItem(text = { Text("Imagem") }, leadingIcon = { Icon(Icons.Outlined.Image, null) }, onClick = { addMenu = false; c.map.addImage(view.center()) })
                                DropdownMenuItem(text = { Text("Texto") }, leadingIcon = { Icon(Icons.Outlined.Title, null) }, onClick = { addMenu = false; c.map.addText(view.center()) })
                            }
                        }
                        TextButton(onClick = { linkMode = !linkMode }, enabled = board.nodes.size >= 2) {
                            Icon(Icons.Outlined.Timeline, null, Modifier.size(17.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (linkMode) "Cancelar ligação" else "Ligar")
                        }
                        IconAction(Icons.Outlined.AutoAwesomeMosaic, "Arranjar os cartões", { c.map.arrange(); view.fit(c.map.board?.bounds()) }, enabled = board.nodes.size >= 2)
                        Box(Modifier.padding(horizontal = 6.dp).width(1.dp).height(20.dp).background(LocalPalette.current.hairline))
                        IconAction(Icons.AutoMirrored.Outlined.Undo, "Desfazer (Ctrl+Z)", { c.map.undo() }, enabled = c.map.canUndo)
                        IconAction(Icons.AutoMirrored.Outlined.Redo, "Refazer (Ctrl+Shift+Z)", { c.map.redo() }, enabled = c.map.canRedo)
                        Box(Modifier.padding(horizontal = 6.dp).width(1.dp).height(20.dp).background(LocalPalette.current.hairline))
                        IconAction(Icons.Outlined.Remove, "Diminuir zoom", { view.zoomBy(1f / 1.2f) })
                        Text(
                            "${(view.zoom * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.width(48.dp).clip(Radius.control).clickable { view.zoomBy(1f / view.zoom) }.padding(vertical = 6.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        IconAction(Icons.Outlined.Add, "Aumentar zoom", { view.zoomBy(1.2f) })
                        IconAction(Icons.Outlined.FitScreen, "Enquadrar tudo", { view.fit(board.bounds()) })
                    }
                }
            }
        }
        BoardTabs(c, boards)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            PlotCanvas(c, board, view, linkMode, { linkMode = it }, Modifier.matchParentSize())
            SelectionBar(c, boards, board, Modifier.align(Alignment.TopCenter).padding(top = 12.dp))
            LegendPanel(c, boards, Modifier.align(Alignment.TopEnd).padding(12.dp))
            if (linkMode) {
                Hint("Clique no cartão de origem e depois no de destino · Esc cancela", Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp))
            }
        }
    }
}

@Composable
private fun Hint(text: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.inverseSurface, shadowElevation = 2.dp) {
        Text(text, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.inverseOnSurface, style = MaterialTheme.typography.labelLarge)
    }
}

/** Abas dos quadros do livro: clique troca, duplo clique renomeia, × exclui (com confirmação). */
@Composable
private fun BoardTabs(c: EscribaController, boards: PlotBoards) {
    val scheme = MaterialTheme.colorScheme
    var renaming by remember { mutableStateOf<Board?>(null) }
    var removing by remember { mutableStateOf<Board?>(null) }
    var lastClick by remember { mutableStateOf("" to 0L) }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = Space.page - 6.dp, end = 20.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        boards.boards.forEach { b ->
            val active = b.id == boards.active?.id
            val shape = Radius.control
            Row(
                Modifier.clip(shape)
                    .background(if (active) LocalPalette.current.navSelected else Color.Transparent)
                    .clickable {
                        val t = currentTimeMillis()
                        if (lastClick.first == b.id && t - lastClick.second < 400) renaming = b
                        lastClick = b.id to t
                        c.map.selectBoard(b.id)
                    }
                    .padding(start = 12.dp, end = if (boards.boards.size > 1) 4.dp else 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(b.name, style = MaterialTheme.typography.labelLarge, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium, color = if (active) scheme.onSurface else LocalPalette.current.muted, maxLines = 1)
                if (boards.boards.size > 1) {
                    Icon(
                        Icons.Outlined.Close, "Excluir quadro ${b.name}",
                        Modifier.padding(start = 4.dp).size(18.dp).clip(CircleShape).clickable { removing = b }.padding(3.dp),
                        tint = scheme.onSurfaceVariant,
                    )
                }
            }
        }
        IconAction(Icons.Outlined.Add, "Novo quadro", { c.map.addBoard() })
    }

    renaming?.let { b ->
        var name by remember(b.id) { mutableStateOf(b.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Renomear quadro") },
            text = { OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Nome") }) },
            confirmButton = { Button(onClick = { c.map.renameBoard(b.id, name); renaming = null }, enabled = name.isNotBlank()) { Text("Salvar") } },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancelar") } },
        )
    }
    removing?.let { b ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Excluir “${b.name}”?") },
            text = { Text("O quadro e seus ${b.nodes.size} item(ns) serão apagados. Dá para voltar com Desfazer enquanto o livro estiver aberto.") },
            confirmButton = { Button(onClick = { c.map.removeBoard(b.id); removing = null }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Cancelar") } },
        )
    }
}

/** Barra flutuante da seleção: cores, e ações do que está selecionado. */
@Composable
private fun SelectionBar(c: EscribaController, boards: PlotBoards, board: Board, modifier: Modifier) {
    val map = c.map
    val edge = map.selectedEdge?.let { board.edge(it) }
    val nodes = map.selection.mapNotNull { board.node(it) }
    if (edge == null && nodes.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val label = when {
        edge != null -> "LIGAÇÃO"
        nodes.size > 1 -> "${nodes.size} ITENS"
        else -> when (nodes.single().kind) {
            NodeKind.Cartao -> "CARTÃO"
            NodeKind.Imagem -> "IMAGEM"
            NodeKind.Texto -> "TEXTO"
        }
    }
    val current = edge?.color ?: nodes.map { it.color }.distinct().singleOrNull()
    var editingLabel by remember(edge?.id) { mutableStateOf(false) }
    FloatingPanel(modifier) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Overline(label)
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BOARD_COLORS.chunked(10).forEachIndexed { row, colors ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        colors.forEach { hex -> Swatch(hex, boards.legendName(hex), current.equals(hex, true)) { map.setSelectionColor(hex) } }
                        if (row == 1) Swatch(null, "Sem cor", current == null) { map.setSelectionColor(null) }
                    }
                }
            }
            Box(Modifier.padding(horizontal = 12.dp).width(1.dp).height(28.dp).background(LocalPalette.current.hairline))
            if (edge != null) {
                if (editingLabel) {
                    var text by remember(edge.id) { mutableStateOf(edge.label) }
                    BasicTextField(
                        text, { text = it; map.setEdgeLabel(edge.id, it, undoable = false) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = scheme.onSurface),
                        cursorBrush = SolidColor(scheme.primary),
                        decorationBox = { inner ->
                            Box(Modifier.width(160.dp).border(1.dp, scheme.outline, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 6.dp)) {
                                if (text.isEmpty()) Text("Rótulo da ligação", style = MaterialTheme.typography.bodyMedium, color = scheme.outline)
                                inner()
                            }
                        },
                    )
                    TextButton(onClick = { editingLabel = false }) { Text("OK") }
                } else {
                    TextButton(onClick = { map.checkpoint(); editingLabel = true }) {
                        Text(if (edge.label.isBlank()) "Rótulo" else "Rótulo: ${edge.label}", maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 160.dp))
                    }
                }
            } else {
                TextButton(onClick = { map.duplicateSelection() }) { Text("Duplicar") }
            }
            TextButton(onClick = { map.deleteSelection() }) { Text("Excluir", color = scheme.error) }
        }
    }
}

@Composable
private fun Swatch(hex: String?, tooltip: String?, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val color = hexColor(hex)
    val content = @Composable {
        Box(
            Modifier.size(18.dp).clip(CircleShape)
                .background(color ?: Color.Transparent)
                .border(if (selected) 2.dp else 1.dp, if (selected) scheme.onSurface else if (color == null) scheme.outline else Color.Transparent, CircleShape)
                .clickable(onClick = onClick),
        ) {
            if (color == null) Box(Modifier.align(Alignment.Center).size(8.dp).border(1.dp, scheme.outline, CircleShape))
        }
    }
    if (tooltip != null) WithTooltip(tooltip, content) else content()
}

/** Legenda das cores de ligação, recolhível. */
@Composable
private fun LegendPanel(c: EscribaController, boards: PlotBoards, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    // Começa recolhida: a legenda é consulta, não precisa ocupar o quadro o tempo todo.
    var open by remember { mutableStateOf(false) }
    FloatingPanel(modifier.width(230.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth().clip(Radius.control).clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
                Overline("Legenda das ligações", Modifier.weight(1f))
                Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, if (open) "Recolher legenda" else "Abrir legenda", Modifier.size(18.dp), tint = scheme.onSurfaceVariant)
            }
            if (!open) return@Column
            Spacer(Modifier.height(6.dp))
            boards.legend.forEachIndexed { i, item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).background(hexColor(item.color) ?: scheme.outline, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    var name by remember(item.color, i) { mutableStateOf(item.name) }
                    BasicTextField(
                        name, { name = it; c.map.renameLegend(i, it) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = scheme.onSurface),
                        cursorBrush = SolidColor(scheme.primary),
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                    )
                    Icon(
                        Icons.Outlined.Close, "Remover ${item.name} da legenda",
                        Modifier.size(18.dp).clip(CircleShape).clickable { c.map.removeLegend(i) }.padding(3.dp),
                        tint = scheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(onClick = { c.map.addLegendColor() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Add, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Adicionar cor")
            }
        }
    }
}
