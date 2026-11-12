package com.cuboide.escriba.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.core.board.Board
import com.cuboide.escriba.core.board.BoardNode
import com.cuboide.escriba.core.board.ImageFrame
import com.cuboide.escriba.core.board.imageRect
import com.cuboide.escriba.core.board.panned
import com.cuboide.escriba.core.board.placeImage
import com.cuboide.escriba.core.board.zoomed
import com.cuboide.escriba.core.board.NodeKind
import com.cuboide.escriba.core.board.NodeSizes
import com.cuboide.escriba.core.board.Pt
import com.cuboide.escriba.core.board.Rect
import com.cuboide.escriba.core.board.Side
import com.cuboide.escriba.core.board.Viewport
import com.cuboide.escriba.core.board.anchor
import com.cuboide.escriba.core.board.curveBetween
import com.cuboide.escriba.core.board.curveOf
import com.cuboide.escriba.core.board.hitEdge
import com.cuboide.escriba.core.board.moveNodes
import com.cuboide.escriba.core.board.nodeAt
import com.cuboide.escriba.core.board.rect
import com.cuboide.escriba.core.model.EntryKind
import com.cuboide.escriba.core.model.UniverseEntry
import com.cuboide.escriba.platform.decodeImageThumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

const val BOARD_MIN_ZOOM = 0.25f
const val BOARD_MAX_ZOOM = 2.5f

/** Vista do quadro: deslocamento (dp de tela) e zoom. Converte entre pixels da tela e unidades do quadro. */
class BoardView(initial: Viewport) {
    var pan by mutableStateOf(Offset(initial.x, initial.y))
    var zoom by mutableStateOf(initial.zoom.coerceIn(BOARD_MIN_ZOOM, BOARD_MAX_ZOOM))
    var size by mutableStateOf(IntSize.Zero)
    var density = 1f

    val viewport: Viewport get() = Viewport(pan.x, pan.y, zoom)

    fun toBoard(px: Offset): Pt = Pt((px.x / density - pan.x) / zoom, (px.y / density - pan.y) / zoom)

    fun toScreen(p: Pt): Offset = Offset((p.x * zoom + pan.x) * density, (p.y * zoom + pan.y) * density)

    /** Centro da área visível, em unidades do quadro. */
    fun center(): Pt = toBoard(Offset(size.width / 2f, size.height / 2f))

    /** Zoom mantendo o ponto [anchorPx] da tela parado (o centro, se nulo). */
    fun zoomBy(factor: Float, anchorPx: Offset? = null) {
        val anchor = anchorPx ?: Offset(size.width / 2f, size.height / 2f)
        val before = toBoard(anchor)
        zoom = (zoom * factor).coerceIn(BOARD_MIN_ZOOM, BOARD_MAX_ZOOM)
        pan = Offset(anchor.x / density - before.x * zoom, anchor.y / density - before.y * zoom)
    }

    /** Enquadra o retângulo inteiro com uma margem. */
    fun fit(r: Rect?) {
        if (r == null || size.width == 0) { pan = Offset(0f, 0f); zoom = 1f; return }
        val w = size.width / density
        val h = size.height / density
        zoom = min((w - 120f) / max(r.width, 1f), (h - 120f) / max(r.height, 1f)).coerceIn(BOARD_MIN_ZOOM, 1.2f)
        pan = Offset(w / 2 - (r.x + r.width / 2) * zoom, h / 2 - (r.y + r.height / 2) * zoom)
    }
}

fun hexColor(hex: String?): Color? = hex?.removePrefix("#")?.toLongOrNull(16)?.let { Color(0xFF000000 or it) }

/** O que um arraste em andamento está fazendo (prévia ao vivo; só grava ao soltar). */
private sealed interface Drag {
    data class Move(val ids: Set<String>, val dx: Float, val dy: Float) : Drag
    data class Resize(val id: String, val width: Float, val height: Float) : Drag
    data class Link(val from: String, val side: Side, val pointer: Pt) : Drag
}

/** Alvo de um clique no quadro, do mais específico para o mais geral. */
private sealed interface Hit {
    data class ResizeHandle(val node: BoardNode) : Hit
    data class LinkHandle(val node: BoardNode, val side: Side) : Hit
    data class Node(val node: BoardNode) : Hit
    data class Edge(val id: String) : Hit
    data object Empty : Hit
}

private const val HANDLE_DP = 9f
private const val DOUBLE_CLICK_MS = 400L

/**
 * O quadro infinito: fundo, ligações e nós. Arrastar o fundo move a vista, a roda do mouse dá zoom no cursor,
 * clique seleciona (Shift soma), arrastar move; as alças dos lados puxam ligações e a do canto redimensiona.
 */
@Composable
fun PlotCanvas(
    c: EscribaController,
    board: Board,
    view: BoardView,
    linkMode: Boolean,
    onLinkModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val map = c.map
    val density = LocalDensity.current.density
    view.density = density
    val palette = LocalPalette.current
    val scheme = MaterialTheme.colorScheme
    var drag by remember(board.id) { mutableStateOf<Drag?>(null) }
    var hovered by remember(board.id) { mutableStateOf<String?>(null) }
    var linkSource by remember(board.id) { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    val entries = c.book?.universe?.entries.orEmpty()
    val legend = c.map.boards?.legend.orEmpty()

    // Quadro com a prévia do arraste aplicada: é o que se desenha.
    val shown = when (val d = drag) {
        is Drag.Move -> board.moveNodes(d.ids, d.dx, d.dy)
        is Drag.Resize -> board.copy(nodes = board.nodes.map { n ->
            if (n.id != d.id) n else {
                val (mw, mh) = NodeSizes.min(n.kind)
                n.copy(width = max(d.width, mw), height = max(d.height, mh))
            }
        })
        else -> board
    }
    val current by rememberUpdatedState(shown)
    val currentLinkMode by rememberUpdatedState(linkMode)
    val setLinkMode by rememberUpdatedState(onLinkModeChange)
    if (!linkMode && linkSource != null) linkSource = null

    /** Nós que mostram alças: o que está sob o mouse e o único selecionado. */
    fun activeIds(): Set<String> = buildSet {
        hovered?.let { add(it) }
        map.selection.singleOrNull()?.let { add(it) }
        map.editingId?.let { remove(it) }
        map.framingId?.let { remove(it) }
    }

    fun hitTest(bd: Board, p: Pt): Hit {
        val tol = HANDLE_DP / view.zoom
        for (id in activeIds()) {
            val n = bd.nodes.firstOrNull { it.id == id } ?: continue
            if (abs(p.x - n.right) <= tol && abs(p.y - n.bottom) <= tol) return Hit.ResizeHandle(n)
            Side.entries.forEach { s -> if (n.anchor(s).distanceTo(p) <= tol) return Hit.LinkHandle(n, s) }
        }
        bd.nodeAt(p)?.let { return Hit.Node(it) }
        bd.hitEdge(p, 8f / view.zoom)?.let { return Hit.Edge(it.id) }
        return Hit.Empty
    }

    var lastClickId by remember { mutableStateOf<String?>(null) }
    var lastClickAt by remember { mutableStateOf(0L) }

    Box(
        modifier
            .clipToBounds()
            .background(boardBackground(palette.isDark, scheme.background))
            .onSizeChanged { view.size = it }
            .focusRequester(focus)
            .focusable()
            .semantics { contentDescription = "Mapa de enredo" }
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                val mod = e.isCtrlPressed || e.isMetaPressed
                when {
                    e.key == Key.Escape -> {
                        when {
                            linkMode -> onLinkModeChange(false)
                            map.editingId != null -> map.stopEditing()
                            map.framingId != null -> map.stopFraming()
                            else -> map.clearSelection()
                        }
                        focus.requestFocus(); true
                    }
                    map.editingId != null -> false
                    e.key == Key.Delete || e.key == Key.Backspace -> { map.deleteSelection(); true }
                    mod && e.key == Key.D -> { map.duplicateSelection(); true }
                    mod && e.key == Key.Z -> { if (e.isShiftPressed) map.redo() else map.undo(); true }
                    mod && e.key == Key.Y -> { map.redo(); true }
                    else -> false
                }
            }
            // Roda do mouse (zoom) e passagem do mouse (alças).
            .pointerInput(board.id) {
                awaitPointerEventScope {
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull() ?: continue
                        when (ev.type) {
                            PointerEventType.Scroll -> {
                                val dy = ch.scrollDelta.y
                                if (dy != 0f) {
                                    // Proporcional ao giro: touchpads mandam frações, rodas mandam passos inteiros.
                                    val factor = 1.12f.pow(-dy.coerceIn(-5f, 5f))
                                    // Na imagem em ajuste, a roda amplia a imagem em vez do quadro.
                                    val framed = map.framingId?.let { id -> current.nodes.firstOrNull { it.id == id } }
                                    val r = framed?.imageRect()
                                    val size = loadedImageSize(c, framed?.image)
                                    if (framed != null && r != null && size != null && r.contains(view.toBoard(ch.position))) {
                                        map.setFrame(framed.id, framed.frame.zoomed(factor, r.width, r.height, size.first, size.second))
                                    } else {
                                        view.zoomBy(factor, ch.position)
                                    }
                                    ch.consume()
                                }
                            }
                            PointerEventType.Move, PointerEventType.Enter -> if (drag == null) {
                                val p = view.toBoard(ch.position)
                                val keep = hovered?.let { id ->
                                    current.nodes.firstOrNull { it.id == id }?.rect?.let { r ->
                                        val pad = (HANDLE_DP + 4) / view.zoom
                                        p.x >= r.x - pad && p.x <= r.right + pad && p.y >= r.y - pad && p.y <= r.bottom + pad
                                    }
                                } == true
                                if (!keep) hovered = current.nodeAt(p)?.id
                            }
                            PointerEventType.Exit -> if (drag == null) hovered = null
                        }
                    }
                }
            }
            // Cliques e arrastes.
            .pointerInput(board.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    val shift = currentEvent.keyboardModifiers.isShiftPressed
                    val bd = current
                    val start = view.toBoard(down.position)
                    map.editingId?.let { editing ->
                        if (bd.nodes.firstOrNull { it.id == editing }?.rect?.contains(start) == true) return@awaitEachGesture
                        map.stopEditing()
                    }
                    focus.requestFocus()
                    val slop = viewConfiguration.touchSlop
                    var moved = false
                    var released = false
                    fun deltaOf(pos: Offset) = Pt((pos.x - down.position.x) / (density * view.zoom), (pos.y - down.position.y) / (density * view.zoom))

                    // Acompanha o arraste até soltar; [onMove] recebe o deslocamento total em unidades do quadro.
                    suspend fun AwaitPointerEventScope.track(onMove: (Pt, Offset) -> Unit) {
                        while (true) {
                            val ev = awaitPointerEvent()
                            val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                            if (!ch.pressed) { released = true; break }
                            if (!moved && (ch.position - down.position).getDistance() > slop) moved = true
                            if (moved) {
                                onMove(deltaOf(ch.position), ch.position - ch.previousPosition)
                                ch.consume()
                            }
                        }
                    }

                    fun isDoubleClick(id: String): Boolean {
                        val now = down.uptimeMillis
                        val double = lastClickId == id && now - lastClickAt <= DOUBLE_CLICK_MS
                        lastClickId = if (double) null else id
                        lastClickAt = now
                        return double
                    }

                    // Imagem em ajuste: arrastar move a imagem; a borda de baixo (cartão) muda a altura dela.
                    map.framingId?.let { id ->
                        val n = bd.nodes.firstOrNull { it.id == id }
                        val r = n?.imageRect()
                        if (n != null && r != null) {
                            val tol = HANDLE_DP / view.zoom
                            val onHeightHandle = n.kind == NodeKind.Cartao && abs(start.y - r.bottom) <= tol && start.x >= r.x && start.x <= r.right
                            if (onHeightHandle) {
                                down.consume()
                                track { d, _ -> map.setImageRatio(n.id, (r.height + d.y) / n.height) }
                                return@awaitEachGesture
                            }
                            if (r.contains(start)) {
                                down.consume()
                                val size = loadedImageSize(c, n.image)
                                track { d, _ ->
                                    if (size != null) map.setFrame(n.id, n.frame.panned(d.x, d.y, r.width, r.height, size.first, size.second))
                                }
                                return@awaitEachGesture
                            }
                        }
                        map.stopFraming()
                    }
                    val hit = hitTest(bd, start)

                    when (hit) {
                        is Hit.ResizeHandle -> {
                            down.consume()
                            val n = hit.node
                            track { d, _ -> drag = Drag.Resize(n.id, n.width + d.x, n.height + d.y) }
                            (drag as? Drag.Resize)?.let { if (released) map.resizeNode(it.id, it.width, it.height) }
                            drag = null
                        }
                        is Hit.LinkHandle -> {
                            down.consume()
                            val n = hit.node
                            track { d, _ -> drag = Drag.Link(n.id, hit.side, Pt(start.x + d.x, start.y + d.y)) }
                            val link = drag as? Drag.Link
                            drag = null
                            if (link != null && released) {
                                val target = current.nodeAt(link.pointer, except = n.id)
                                if (target != null) map.connect(n.id, target.id)
                                else if (n.anchor(hit.side).distanceTo(link.pointer) > 40f) map.connectToNewCard(n.id, link.pointer)
                            }
                        }
                        is Hit.Node -> {
                            down.consume()
                            val n = hit.node
                            if (currentLinkMode) {
                                val from = linkSource
                                if (from == null || from == n.id) linkSource = n.id
                                else { map.connect(from, n.id); linkSource = null; setLinkMode(false) }
                                return@awaitEachGesture
                            }
                            val wasSelected = n.id in map.selection
                            if (shift) map.select(n.id, additive = true) else if (!wasSelected) map.select(n.id)
                            val ids = if (n.id in map.selection) map.selection else setOf(n.id)
                            track { d, _ -> drag = Drag.Move(ids, d.x, d.y) }
                            val move = drag as? Drag.Move
                            drag = null
                            if (moved) {
                                if (move != null && released) map.moveNodes(move.ids, move.dx, move.dy)
                            } else if (released && !shift) {
                                if (wasSelected && map.selection.size > 1) map.select(n.id)
                                if (isDoubleClick(n.id)) {
                                    // Duplo clique na imagem ajusta a imagem; no resto do nó, edita o texto (ou a legenda).
                                    val r = n.imageRect()
                                    val onCaption = n.kind == NodeKind.Imagem && start.y > n.bottom - 40f
                                    if (r != null && r.contains(start) && !onCaption) map.startFraming(n.id) else map.startEditing(n.id)
                                }
                            }
                        }
                        is Hit.Edge -> {
                            down.consume()
                            if (!currentLinkMode) map.selectEdge(hit.id)
                            track { _, _ -> }
                        }
                        Hit.Empty -> {
                            track { _, step -> view.pan += Offset(step.x / density, step.y / density) }
                            if (!moved && released) {
                                if (currentLinkMode) { linkSource = null; setLinkMode(false) }
                                else if (isDoubleClick("")) map.addCard(start) else map.clearSelection()
                            }
                        }
                    }
                }
            },
    ) {
        BoardBackground(view, palette.accent, scheme.onBackground)
        EdgesLayer(shown, view, map.selectedEdge, drag as? Drag.Link, scheme.onSurfaceVariant, scheme.primary)
        // Rótulos das ligações, em pílulas no meio da curva.
        for (e in shown.edges) {
            if (e.label.isBlank()) continue
            val curve = shown.curveOf(e) ?: continue
            val mid = view.toScreen(curve.mid)
            val color = hexColor(e.color) ?: scheme.outline
            Text(
                e.label,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurface,
                maxLines = 1,
                modifier = Modifier
                    .layout { m, cons ->
                        val p = m.measure(cons.copy(minWidth = 0, minHeight = 0))
                        layout(0, 0) { p.place((mid.x - p.width / 2).toInt(), (mid.y - p.height / 2).toInt()) }
                    }
                    .background(scheme.surface, RoundedCornerShape(50))
                    .border(1.dp, color, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        for (n in shown.nodes) {
            key(n.id) {
                val origin = view.toScreen(Pt(n.x, n.y))
                NodeView(
                    c, n, view.zoom,
                    selected = n.id in map.selection || n.id == linkSource,
                    active = n.id in activeIds() && drag == null,
                    editing = map.editingId == n.id,
                    entries = entries,
                    legendColor = legend.firstOrNull { it.color.equals(n.color, true) }?.name,
                    modifier = Modifier.layout { m, _ ->
                        val w = (n.width * density).toInt()
                        val h = (n.height * density).toInt()
                        val p = m.measure(Constraints.fixed(w, h))
                        layout(0, 0) {
                            p.placeWithLayer(origin.x.toInt(), origin.y.toInt()) {
                                scaleX = view.zoom; scaleY = view.zoom
                                transformOrigin = TransformOrigin(0f, 0f)
                            }
                        }
                    },
                )
            }
        }
        HandlesLayer(shown, view, activeIds(), scheme.primary, scheme.surface)
        if (board.nodes.isEmpty()) {
            Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("O mapa está vazio.", style = MaterialTheme.typography.titleMedium, color = scheme.onBackground)
                Text(
                    "Clique duas vezes no quadro para criar um cartão, ou use os botões acima. Puxe as bolinhas da borda de um cartão para ligá-lo a outro.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(420.dp),
                )
            }
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}

private fun boardBackground(dark: Boolean, base: Color): Color =
    if (dark) lerp(base, Color.Black, 0.25f) else lerp(base, Color(0xFF8A8070), 0.06f)

/** Grade de pontos e constelações decorativas; ambas acompanham pan e zoom. */
@Composable
private fun BoardBackground(view: BoardView, accent: Color, ink: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val zoom = view.zoom
        val d = view.density
        val tl = view.toBoard(Offset.Zero)
        val br = view.toBoard(Offset(size.width, size.height))
        // Pontos.
        var step = 24f
        while (step * zoom * d < 12f) step *= 2
        val dot = ink.copy(alpha = 0.10f)
        var gx = floor(tl.x / step) * step
        while (gx <= br.x) {
            var gy = floor(tl.y / step) * step
            while (gy <= br.y) {
                drawCircle(dot, radius = 1.1f * d, center = view.toScreen(Pt(gx, gy)))
                gy += step
            }
            gx += step
        }
        // Constelações: uma por célula grande, sorteada pela posição da célula (sempre igual).
        val cell = 640f
        val starColor = accent.copy(alpha = 0.32f)
        val lineColor = accent.copy(alpha = 0.10f)
        var cx = floor(tl.x / cell).toInt()
        while (cx * cell <= br.x) {
            var cy = floor(tl.y / cell).toInt()
            while (cy * cell <= br.y) {
                drawConstellation(view, cx, cy, cell, starColor, lineColor)
                cy++
            }
            cx++
        }
    }
}

private fun DrawScope.drawConstellation(view: BoardView, cx: Int, cy: Int, cell: Float, star: Color, line: Color) {
    var seed = (cx * 73856093) xor (cy * 19349663) xor 0x5bd1e995
    fun next(): Float {
        seed = seed * 1103515245 + 12345
        return ((seed ushr 8) and 0xFFFF) / 65535f
    }
    val d = view.density
    // Estrelas soltas.
    repeat(5) {
        val p = Pt((cx + next()) * cell, (cy + next()) * cell)
        drawCircle(star.copy(alpha = star.alpha * (0.4f + next() * 0.6f)), radius = (0.8f + next() * 1.2f) * d, center = view.toScreen(p))
    }
    if (next() < 0.35f) return
    val count = 4 + (next() * 3).toInt()
    val ox = (cx + 0.2f + next() * 0.5f) * cell
    val oy = (cy + 0.2f + next() * 0.5f) * cell
    var prev: Offset? = null
    repeat(count) {
        val p = view.toScreen(Pt(ox + (next() - 0.5f) * cell * 0.45f, oy + (next() - 0.5f) * cell * 0.45f))
        prev?.let { drawLine(line, it, p, strokeWidth = 1f * d) }
        drawCircle(star, radius = 1.8f * d, center = p)
        drawCircle(star.copy(alpha = star.alpha * 0.25f), radius = 4.5f * d, center = p)
        prev = p
    }
}

@Composable
private fun EdgesLayer(board: Board, view: BoardView, selectedEdge: String?, link: Drag.Link?, neutral: Color, accent: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val d = view.density
        val width = 1.8f * d * max(view.zoom, 0.6f)
        for (e in board.edges) {
            val curve = board.curveOf(e) ?: continue
            val color = hexColor(e.color) ?: neutral.copy(alpha = 0.7f)
            val selected = e.id == selectedEdge
            val path = Path().apply {
                val p0 = view.toScreen(curve.p0); val c1 = view.toScreen(curve.c1)
                val c2 = view.toScreen(curve.c2); val p3 = view.toScreen(curve.p3)
                moveTo(p0.x, p0.y); cubicTo(c1.x, c1.y, c2.x, c2.y, p3.x, p3.y)
            }
            if (selected) drawPath(path, color.copy(alpha = 0.25f), style = Stroke(width * 4))
            drawPath(path, color, style = Stroke(if (selected) width * 1.6f else width))
        }
        if (link != null) {
            val from = board.nodes.firstOrNull { it.id == link.from } ?: return@Canvas
            val curve = curveBetween(from.anchor(link.side), link.side, link.pointer, null)
            val path = Path().apply {
                val p0 = view.toScreen(curve.p0); val c1 = view.toScreen(curve.c1)
                val c2 = view.toScreen(curve.c2); val p3 = view.toScreen(curve.p3)
                moveTo(p0.x, p0.y); cubicTo(c1.x, c1.y, c2.x, c2.y, p3.x, p3.y)
            }
            drawPath(path, accent, style = Stroke(width * 1.4f))
            drawCircle(accent, radius = 4f * d, center = view.toScreen(link.pointer))
        }
    }
}

/** Bolinhas dos lados (puxar ligação) e quadradinho do canto (redimensionar) dos nós ativos. */
@Composable
private fun HandlesLayer(board: Board, view: BoardView, active: Set<String>, accent: Color, fill: Color) {
    if (active.isEmpty()) return
    Canvas(Modifier.fillMaxSize()) {
        val d = view.density
        for (id in active) {
            val n = board.nodes.firstOrNull { it.id == id } ?: continue
            Side.entries.forEach { s ->
                val p = view.toScreen(n.anchor(s))
                drawCircle(fill, radius = 5f * d, center = p)
                drawCircle(accent, radius = 5f * d, center = p, style = Stroke(1.5f * d))
            }
            val corner = view.toScreen(Pt(n.right, n.bottom))
            val s = 4f * d
            drawRect(accent, topLeft = Offset(corner.x - s, corner.y - s), size = androidx.compose.ui.geometry.Size(s * 2, s * 2))
        }
    }
}

// ---------- nós ----------

/** Miniaturas decodificadas, por livro e caminho (as últimas usadas ficam em memória). */
internal object BoardImages {
    private val cache = object : LinkedHashMap<String, ImageBitmap>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?) = size > 60
    }

    fun get(key: String): ImageBitmap? = synchronized(cache) { cache[key] }
    fun put(key: String, image: ImageBitmap) = synchronized(cache) { cache[key] = image }
}

@Composable
internal fun rememberBoardImage(c: EscribaController, path: String?): ImageBitmap? {
    val bookPath = c.book?.path
    val key = "$bookPath|$path"
    val cached = path?.let { BoardImages.get(key) }
    val state = produceState(cached, key) {
        if (path == null || value != null) return@produceState
        value = withContext(Dispatchers.Default) {
            c.layoutImage(path)?.let { decodeImageThumbnail(it, 1280) }?.also { BoardImages.put(key, it) }
        }
    }
    return state.value
}

/** Tamanho (só a proporção importa) da imagem já carregada do nó, para os gestos de enquadramento. */
internal fun loadedImageSize(c: EscribaController, path: String?): Pair<Float, Float>? =
    path?.let { BoardImages.get("${c.book?.path}|$it") }?.let { it.width.toFloat() to it.height.toFloat() }

/** Imagem desenhada segundo o enquadramento do nó (ponto focal, zoom, preencher ou inteira). */
@Composable
internal fun FramedImage(image: ImageBitmap?, frame: ImageFrame, modifier: Modifier) {
    Canvas(modifier.clipToBounds()) {
        if (image == null) return@Canvas
        val r = placeImage(size.width, size.height, image.width.toFloat(), image.height.toFloat(), frame)
        drawImage(
            image,
            dstOffset = IntOffset(r.x.roundToInt(), r.y.roundToInt()),
            dstSize = IntSize(r.width.roundToInt(), r.height.roundToInt()),
            filterQuality = FilterQuality.High,
        )
    }
}

/** Sobre a imagem em ajuste: contorno, dica, alça da altura (cartão) e botões de zoom, modo e concluir. */
@Composable
private fun FramingOverlay(c: EscribaController, node: BoardNode, image: ImageBitmap?, isCard: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val frame = node.frame
    val r = node.imageRect()
    fun zoom(f: Float) {
        if (image == null || r == null) return
        c.map.setFrame(node.id, frame.zoomed(f, r.width, r.height, image.width.toFloat(), image.height.toFloat()))
    }
    Box(Modifier.fillMaxSize().border(2.dp, scheme.primary).semantics { contentDescription = "Ajustando imagem" }) {
        Text(
            "Arraste para mover · roda: zoom",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
            modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
        )
        Row(
            Modifier.align(Alignment.BottomEnd).padding(start = 6.dp, end = 6.dp, bottom = if (isCard) 12.dp else 6.dp)
                .clip(RoundedCornerShape(8.dp)).background(scheme.surface.copy(alpha = 0.94f))
                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(8.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolIcon(Icons.Outlined.Remove, "Diminuir imagem") { zoom(1f / 1.2f) }
            ToolIcon(Icons.Outlined.Add, "Ampliar imagem") { zoom(1.2f) }
            FrameChip(if (frame.fit) "Preencher" else "Inteira") { c.map.setFrame(node.id, frame.copy(fit = !frame.fit, zoom = 1f, x = 0.5f, y = 0.5f)) }
            FrameChip("Restaurar") { c.map.resetFrame(node.id) }
            ToolIcon(Icons.Outlined.Check, "Concluir ajuste") { c.map.stopFraming() }
        }
        if (isCard) {
            // Alça da altura da imagem no cartão (o arraste é tratado pelo quadro).
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp).size(44.dp, 6.dp)
                    .background(scheme.primary, RoundedCornerShape(50)).semantics { contentDescription = "Altura da imagem" },
            )
        }
    }
}

@Composable
private fun FrameChip(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 5.dp),
    )
}

@Composable
private fun NodeView(
    c: EscribaController,
    node: BoardNode,
    zoom: Float,
    selected: Boolean,
    active: Boolean,
    editing: Boolean,
    entries: List<UniverseEntry>,
    legendColor: String?,
    modifier: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val tint = hexColor(node.color)
    val borderColor = when {
        selected -> scheme.primary
        tint != null -> tint
        else -> scheme.outlineVariant
    }
    val description = "${node.kind.label}: ${node.title.ifBlank { "sem título" }}" + (legendColor?.let { " ($it)" } ?: "")
    val framing = c.map.framingId == node.id
    Box(modifier.semantics { contentDescription = description }) {
        when (node.kind) {
            NodeKind.Cartao -> CardNode(c, node, tint, borderColor, selected, editing, framing, entries)
            NodeKind.Imagem -> ImageNode(c, node, borderColor, selected, editing, framing)
            NodeKind.Texto -> TextNode(c, node, tint, selected, editing)
        }
        if (active && !editing && !framing && zoom >= 0.45f) NodeTools(c, node, entries, Modifier.align(Alignment.TopEnd).padding(6.dp))
    }
}

@Composable
private fun CardNode(
    c: EscribaController,
    node: BoardNode,
    tint: Color?,
    border: Color,
    selected: Boolean,
    editing: Boolean,
    framing: Boolean,
    entries: List<UniverseEntry>,
) {
    val scheme = MaterialTheme.colorScheme
    val fonts = LocalFonts.current
    val bg = if (tint != null) lerp(scheme.surface, tint, 0.10f) else scheme.surface
    val shape = RoundedCornerShape(10.dp)
    val image = rememberBoardImage(c, node.image)
    Column(
        Modifier.fillMaxSize().clip(shape).background(bg).border(if (selected) 2.dp else 1.dp, border, shape),
    ) {
        if (node.image != null) {
            Box(Modifier.fillMaxWidth().fillMaxHeight(node.imageRatio).background(scheme.surfaceVariant)) {
                FramedImage(image, node.frame, Modifier.fillMaxSize())
                if (framing) FramingOverlay(c, node, image, isCard = true)
            }
        }
        Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp, vertical = 10.dp)) {
            val titleStyle = TextStyle(fontFamily = fonts.display, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = scheme.onSurface)
            val bodyStyle = TextStyle(fontFamily = fonts.book, fontSize = 13.sp, lineHeight = 18.sp, color = scheme.onSurfaceVariant)
            if (editing) {
                var title by remember(node.id) { mutableStateOf(node.title) }
                var body by remember(node.id) { mutableStateOf(node.body) }
                val focus = remember { FocusRequester() }
                BasicTextField(
                    title, { title = it; c.map.setNodeText(node.id, it, body) },
                    textStyle = titleStyle, cursorBrush = SolidColor(scheme.primary),
                    decorationBox = { inner -> Box { if (title.isEmpty()) Text("Título", style = titleStyle.copy(color = scheme.outline)); inner() } },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
                Spacer(Modifier.height(4.dp))
                BasicTextField(
                    body, { body = it; c.map.setNodeText(node.id, title, it) },
                    textStyle = bodyStyle, cursorBrush = SolidColor(scheme.primary),
                    decorationBox = { inner -> Box { if (body.isEmpty()) Text("Escreva o que acontece, a ideia, a cena…", style = bodyStyle.copy(color = scheme.outline)); inner() } },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                LaunchedEffect(Unit) { focus.requestFocus() }
            } else {
                Text(node.title.ifBlank { "Sem título" }, style = titleStyle.copy(color = if (node.title.isBlank()) scheme.outline else scheme.onSurface), maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (node.body.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(node.body, style = bodyStyle, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                }
            }
            node.entryId?.let { id ->
                Spacer(Modifier.weight(1f, fill = !editing))
                EntryChip(c, node, entries.firstOrNull { it.id == id })
            }
        }
    }
}

/** Ficha vinculada: clique abre no Universo, × desfaz o vínculo. */
@Composable
private fun EntryChip(c: EscribaController, node: BoardNode, entry: UniverseEntry?) {
    val scheme = MaterialTheme.colorScheme
    val accent = hexColor(node.color) ?: scheme.primary
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(6.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).clickable(enabled = entry != null) { entry?.let { c.openUniverseEntry(it.id) } }.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(7.dp).background(accent, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(
                entry?.name ?: "(ficha removida)",
                style = MaterialTheme.typography.labelMedium,
                color = if (entry == null) scheme.error else scheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.Outlined.Close, "Desvincular ficha",
            Modifier.padding(end = 4.dp).size(16.dp).clip(CircleShape).clickable { c.map.linkEntry(node.id, null) }.padding(2.dp),
            tint = scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ImageNode(c: EscribaController, node: BoardNode, border: Color, selected: Boolean, editing: Boolean, framing: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)
    val image = rememberBoardImage(c, node.image)
    Box(Modifier.fillMaxSize().clip(shape).background(scheme.surfaceVariant).border(if (selected) 2.dp else 1.dp, border, shape)) {
        FramedImage(image, node.frame, Modifier.fillMaxSize())
        if (framing) FramingOverlay(c, node, image, isCard = false)
        if (!framing && (editing || node.title.isNotBlank())) {
            val style = TextStyle(fontSize = 12.sp, color = Color.White, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))))
                    .padding(start = 10.dp, end = 10.dp, top = 18.dp, bottom = 8.dp),
            ) {
                if (editing) {
                    var caption by remember(node.id) { mutableStateOf(node.title) }
                    val focus = remember { FocusRequester() }
                    BasicTextField(
                        caption, { caption = it; c.map.setNodeText(node.id, it, node.body) },
                        textStyle = style, cursorBrush = SolidColor(Color.White),
                        decorationBox = { inner -> Box(contentAlignment = Alignment.Center) { if (caption.isEmpty()) Text("Legenda", style = style.copy(color = Color.White.copy(alpha = 0.6f))); inner() } },
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                    LaunchedEffect(Unit) { focus.requestFocus() }
                } else {
                    Text(node.title, style = style, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun TextNode(c: EscribaController, node: BoardNode, tint: Color?, selected: Boolean, editing: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val fonts = LocalFonts.current
    val size = (node.height * 0.5f).coerceIn(14f, 120f)
    val style = TextStyle(
        fontFamily = fonts.display, fontWeight = FontWeight.SemiBold, fontSize = size.sp, lineHeight = (size * 1.15f).sp,
        color = tint ?: scheme.primary, textAlign = TextAlign.Center,
    )
    val shape = RoundedCornerShape(6.dp)
    Box(
        Modifier.fillMaxSize().then(if (selected) Modifier.border(1.dp, scheme.primary.copy(alpha = 0.6f), shape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (editing) {
            var text by remember(node.id) { mutableStateOf(node.title) }
            val focus = remember { FocusRequester() }
            BasicTextField(
                text, { text = it; c.map.setNodeText(node.id, it, node.body) },
                textStyle = style, cursorBrush = SolidColor(scheme.primary),
                decorationBox = { inner -> Box(contentAlignment = Alignment.Center) { if (text.isEmpty()) Text("Título", style = style.copy(color = style.color.copy(alpha = 0.4f))); inner() } },
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            LaunchedEffect(Unit) { focus.requestFocus() }
        } else {
            Text(
                node.title.ifBlank { "Título" }, style = if (node.title.isBlank()) style.copy(color = style.color.copy(alpha = 0.4f)) else style,
                maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Botões que aparecem sobre o nó ativo: vincular ficha, imagem, excluir. */
@Composable
private fun NodeTools(c: EscribaController, node: BoardNode, entries: List<UniverseEntry>, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    var linkMenu by remember { mutableStateOf(false) }
    var imageMenu by remember { mutableStateOf(false) }
    Row(
        modifier.clip(RoundedCornerShape(8.dp)).background(scheme.surface.copy(alpha = 0.92f)).border(1.dp, scheme.outlineVariant, RoundedCornerShape(8.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (node.kind == NodeKind.Cartao) {
            Box {
                ToolIcon(Icons.Outlined.Link, "Vincular ficha") { linkMenu = true }
                DropdownMenu(expanded = linkMenu, onDismissRequest = { linkMenu = false }) {
                    val linkable = entries.filter { it.kind != EntryKind.Lore }.sortedWith(compareBy({ it.kind.ordinal }, { it.name.lowercase() }))
                    if (linkable.isEmpty()) {
                        DropdownMenuItem(text = { Text("Crie fichas no Universo para vinculá-las") }, onClick = { linkMenu = false }, enabled = false)
                    }
                    var lastKind: EntryKind? = null
                    linkable.forEach { e ->
                        if (e.kind != lastKind) {
                            if (lastKind != null) HorizontalDivider()
                            Text(e.kind.plural, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                            lastKind = e.kind
                        }
                        DropdownMenuItem(
                            text = { Text((if (e.id == node.entryId) "● " else "") + e.name) },
                            leadingIcon = { Icon(e.kind.icon, null, Modifier.size(18.dp)) },
                            onClick = { linkMenu = false; c.map.linkEntry(node.id, e.id) },
                        )
                    }
                }
            }
        }
        if (node.kind != NodeKind.Texto) {
            Box {
                ToolIcon(Icons.Outlined.Image, if (node.image == null) "Adicionar imagem" else "Imagem") {
                    if (node.image == null) c.map.chooseNodeImage(node.id) else imageMenu = true
                }
                DropdownMenu(expanded = imageMenu, onDismissRequest = { imageMenu = false }) {
                    DropdownMenuItem(text = { Text("Ajustar imagem (mover e ampliar)") }, onClick = { imageMenu = false; c.map.startFraming(node.id) })
                    if (node.kind == NodeKind.Imagem) {
                        DropdownMenuItem(text = { Text("Usar a proporção da imagem") }, onClick = { imageMenu = false; c.map.fitNodeToImage(node.id) })
                    }
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Trocar imagem") }, onClick = { imageMenu = false; c.map.chooseNodeImage(node.id) })
                    if (node.kind == NodeKind.Cartao) {
                        DropdownMenuItem(text = { Text("Remover imagem") }, onClick = { imageMenu = false; c.map.removeNodeImage(node.id) })
                    }
                }
            }
        }
        ToolIcon(Icons.Outlined.Close, "Excluir") { c.map.deleteNode(node.id) }
    }
}

@Composable
private fun ToolIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Icon(
        icon, label,
        Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(5.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
