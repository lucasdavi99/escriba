package com.cuboide.escriba.core.board

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/** Ponto em unidades do quadro. */
data class Pt(val x: Float, val y: Float) {
    operator fun plus(o: Pt) = Pt(x + o.x, y + o.y)
    operator fun minus(o: Pt) = Pt(x - o.x, y - o.y)
    operator fun times(k: Float) = Pt(x * k, y * k)
    fun distanceTo(o: Pt): Float { val dx = x - o.x; val dy = y - o.y; return sqrt(dx * dx + dy * dy) }
}

data class Rect(val x: Float, val y: Float, val width: Float, val height: Float) {
    val right: Float get() = x + width
    val bottom: Float get() = y + height
    fun contains(p: Pt) = p.x >= x && p.x <= right && p.y >= y && p.y <= bottom
    fun overlaps(o: Rect) = x < o.right && o.x < right && y < o.bottom && o.y < bottom
}

val BoardNode.rect: Rect get() = Rect(x, y, width, height)

/** Ponto no meio do lado [side] do nó. */
fun BoardNode.anchor(side: Side): Pt = when (side) {
    Side.Topo -> Pt(centerX, y)
    Side.Direita -> Pt(right, centerY)
    Side.Base -> Pt(centerX, bottom)
    Side.Esquerda -> Pt(x, centerY)
}

private fun Side.normal(): Pt = when (this) {
    Side.Topo -> Pt(0f, -1f)
    Side.Direita -> Pt(1f, 0f)
    Side.Base -> Pt(0f, 1f)
    Side.Esquerda -> Pt(-1f, 0f)
}

/**
 * Lados em que a ligação de [a] para [b] se prende: horizontal quando o vão entre os nós é maior na horizontal,
 * vertical caso contrário.
 */
fun autoSides(a: BoardNode, b: BoardNode): Pair<Side, Side> {
    val dx = b.centerX - a.centerX
    val dy = b.centerY - a.centerY
    val gapX = abs(dx) - (a.width + b.width) / 2
    val gapY = abs(dy) - (a.height + b.height) / 2
    return if (gapX >= gapY) {
        if (dx >= 0) Side.Direita to Side.Esquerda else Side.Esquerda to Side.Direita
    } else {
        if (dy >= 0) Side.Base to Side.Topo else Side.Topo to Side.Base
    }
}

/** Curva cúbica de uma ligação: início, dois pontos de controle, fim. */
data class Curve(val p0: Pt, val c1: Pt, val c2: Pt, val p3: Pt) {
    fun at(t: Float): Pt {
        val u = 1 - t
        val a = u * u * u
        val b = 3 * u * u * t
        val c = 3 * u * t * t
        val d = t * t * t
        return Pt(a * p0.x + b * c1.x + c * c2.x + d * p3.x, a * p0.y + b * c1.y + c * c2.y + d * p3.y)
    }

    val mid: Pt get() = at(0.5f)

    /** Menor distância de [p] até a curva, por amostragem. */
    fun distanceTo(p: Pt, samples: Int = 32): Float {
        var best = Float.MAX_VALUE
        var prev = p0
        for (i in 1..samples) {
            val cur = at(i / samples.toFloat())
            best = minOf(best, segmentDistance(p, prev, cur))
            prev = cur
        }
        return best
    }
}

/** Curva entre dois pontos saindo pelos lados dados. */
fun curveBetween(p0: Pt, s0: Side, p3: Pt, s3: Side?): Curve {
    val k = max(40f, p0.distanceTo(p3) * 0.4f)
    val c2 = if (s3 != null) p3 + s3.normal() * k else p3
    return Curve(p0, p0 + s0.normal() * k, c2, p3)
}

/** Curva de uma ligação, ou `null` se um dos nós não existe. */
fun Board.curveOf(edge: BoardEdge): Curve? {
    val a = node(edge.from) ?: return null
    val b = node(edge.to) ?: return null
    val (autoA, autoB) = autoSides(a, b)
    val sa = edge.fromSide ?: autoA
    val sb = edge.toSide ?: autoB
    return curveBetween(a.anchor(sa), sa, b.anchor(sb), sb)
}

/** Ligação sob o ponto (a mais próxima dentro da tolerância). */
fun Board.hitEdge(p: Pt, tolerance: Float): BoardEdge? =
    edges.mapNotNull { e -> curveOf(e)?.let { e to it.distanceTo(p) } }
        .filter { it.second <= tolerance }
        .minByOrNull { it.second }?.first

/** Nó sob o ponto; o de cima (último da lista) ganha. */
fun Board.nodeAt(p: Pt, except: String? = null): BoardNode? =
    nodes.lastOrNull { it.id != except && it.rect.contains(p) }

private fun segmentDistance(p: Pt, a: Pt, b: Pt): Float {
    val ab = b - a
    val len2 = ab.x * ab.x + ab.y * ab.y
    if (len2 == 0f) return p.distanceTo(a)
    val t = (((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / len2).coerceIn(0f, 1f)
    return p.distanceTo(a + ab * t)
}

// ---- arranjo automático ----

private const val GAP_X = 90f
private const val GAP_Y = 40f
private const val GAP_GROUP = 120f

/**
 * Reorganiza o quadro em camadas: cada grupo de nós ligados vira colunas da esquerda para a direita, a partir dos nós
 * sem ligação de entrada; grupos ficam um abaixo do outro e nós soltos em grade no fim. Nenhum nó fica sobre outro.
 */
fun Board.arrange(): Board {
    if (nodes.isEmpty()) return this
    val origin = bounds()!!
    val ids = nodes.map { it.id }
    val byId = nodes.associateBy { it.id }
    val neighbors = ids.associateWith { mutableSetOf<String>() }
    val incoming = ids.associateWith { 0 }.toMutableMap()
    for (e in edges) {
        if (e.from !in byId || e.to !in byId) continue
        neighbors.getValue(e.from) += e.to
        neighbors.getValue(e.to) += e.from
        incoming[e.to] = incoming.getValue(e.to) + 1
    }
    // Ordem de leitura atual (de cima para baixo, da esquerda para a direita) decide os empates.
    val reading = compareBy<BoardNode>({ it.y }, { it.x })
    val seen = mutableSetOf<String>()
    val groups = mutableListOf<List<String>>()
    for (n in nodes.sortedWith(reading)) {
        if (n.id in seen || neighbors.getValue(n.id).isEmpty()) continue
        val group = mutableListOf<String>()
        val queue = ArrayDeque(listOf(n.id)); seen += n.id
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst(); group += id
            for (m in neighbors.getValue(id)) if (seen.add(m)) queue += m
        }
        groups += group
    }
    val loose = nodes.filter { neighbors.getValue(it.id).isEmpty() }.sortedWith(reading)

    val placed = mutableMapOf<String, Pt>()
    var top = origin.y
    var widest = 0f
    for (group in groups) {
        val members = group.map { byId.getValue(it) }
        val roots = members.filter { incoming.getValue(it.id) == 0 }.ifEmpty { listOf(members.minWith(reading)) }
        val layer = mutableMapOf<String, Int>()
        val queue = ArrayDeque<String>()
        roots.sortedWith(reading).forEach { layer[it.id] = 0; queue += it.id }
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            for (m in neighbors.getValue(id)) if (m !in layer) { layer[m] = layer.getValue(id) + 1; queue += m }
        }
        val columns = members.groupBy { layer.getValue(it.id) }.toSortedMap().values.map { it.sortedWith(reading) }
        val heights = columns.map { col -> col.sumOf { it.height.toDouble() }.toFloat() + GAP_Y * (col.size - 1) }
        val groupHeight = heights.max()
        var left = origin.x
        columns.forEachIndexed { i, col ->
            var y = top + (groupHeight - heights[i]) / 2
            val colWidth = col.maxOf { it.width }
            for (n in col) {
                placed[n.id] = Pt(left + (colWidth - n.width) / 2, y)
                y += n.height + GAP_Y
            }
            left += colWidth + GAP_X
        }
        widest = max(widest, left - GAP_X - origin.x)
        top += groupHeight + GAP_GROUP
    }
    if (loose.isNotEmpty()) {
        val rowLimit = max(widest, 4 * (NodeSizes.CARD_W + GAP_X))
        var left = origin.x
        var rowHeight = 0f
        for (n in loose) {
            if (left > origin.x && left - origin.x + n.width > rowLimit) {
                left = origin.x; top += rowHeight + GAP_Y; rowHeight = 0f
            }
            placed[n.id] = Pt(left, top)
            left += n.width + GAP_X / 2
            rowHeight = max(rowHeight, n.height)
        }
    }
    return copy(nodes = nodes.map { n -> placed[n.id]?.let { n.copy(x = it.x, y = it.y) } ?: n })
}
