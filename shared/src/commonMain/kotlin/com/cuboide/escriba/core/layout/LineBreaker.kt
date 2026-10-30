package com.cuboide.escriba.core.layout

import kotlin.math.abs

/** Fonte de um trecho: família instalada mais estilo. */
data class FontSpec(val family: String, val bold: Boolean = false, val italic: Boolean = false)

/** Mede texto em pontos. A implementação de cada plataforma usa os mesmos arquivos de fonte do PDF. */
interface GlyphMeasurer {
    /** Largura do texto; o espaçamento entre letras ([trackingEm]) conta depois de cada caractere, como no PDF. */
    fun width(text: String, font: FontSpec, sizePt: Float, trackingEm: Float = 0f): Float
    /** Primeira família instalada de [candidates]; se nenhuma existir, uma serifada padrão do sistema. */
    fun resolveFamily(candidates: List<String>): String
    fun hasFamily(family: String): Boolean
}

/** Trecho com um único estilo, já medido. [rise] sobe (positivo) ou desce o texto em relação à linha de base. */
data class Run(val text: String, val font: FontSpec, val sizePt: Float, val width: Float, val rise: Float = 0f, val trackingEm: Float = 0f)

sealed class LbItem { abstract val width: Float }

class LbBox(override val width: Float, val runs: List<Run>, val footnoteIds: List<String> = emptyList()) : LbItem()

class LbGlue(override val width: Float, val stretch: Float, val shrink: Float) : LbItem()

/** Ponto de quebra opcional; [hyphen] aparece no fim da linha quando a quebra cai aqui. */
class LbPenalty(override val width: Float, val penalty: Float, val flagged: Boolean, val hyphen: Run? = null) : LbItem()

const val LB_INFINITY = 10000f

/** Trecho posicionado dentro da linha (x relativo ao início da linha). */
data class PlacedRun(val x: Float, val run: Run)

data class ComposedLine(val runs: List<PlacedRun>, val width: Float, val footnoteIds: List<String>)

private class Node(
    val position: Int,
    val line: Int,
    val fitness: Int,
    val totalWidth: Float,
    val totalStretch: Float,
    val totalShrink: Float,
    val demerits: Float,
    val flagged: Boolean,
    val previous: Node?,
)

/**
 * Quebra de parágrafo de Knuth e Plass (1981): escolhe o conjunto de quebras que minimiza a soma dos
 * deméritos de todas as linhas, em vez de encher uma linha por vez. Devolve os índices dos itens onde as
 * linhas terminam, ou `null` se nenhuma solução couber em [tolerance].
 */
fun knuthPlassBreaks(
    items: List<LbItem>,
    lineWidth: (Int) -> Float,
    tolerance: Float = 3f,
    linePenalty: Float = 10f,
    flaggedDemerits: Float = 3000f,
    fitnessDemerits: Float = 3000f,
): List<Int>? {
    val n = items.size
    val sumW = FloatArray(n + 1)
    val sumY = FloatArray(n + 1)
    val sumZ = FloatArray(n + 1)
    for (i in 0 until n) {
        val it = items[i]
        sumW[i + 1] = sumW[i] + if (it is LbPenalty) 0f else it.width
        sumY[i + 1] = sumY[i] + if (it is LbGlue) it.stretch else 0f
        sumZ[i + 1] = sumZ[i] + if (it is LbGlue) it.shrink else 0f
    }
    var active = mutableListOf(Node(0, 0, 1, 0f, 0f, 0f, 0f, false, null))

    for (b in 0 until n) {
        val item = items[b]
        val legal = when (item) {
            is LbGlue -> b > 0 && items[b - 1] is LbBox
            is LbPenalty -> item.penalty < LB_INFINITY
            is LbBox -> false
        }
        if (!legal) continue
        val penalty = (item as? LbPenalty)?.penalty ?: 0f
        val flagged = (item as? LbPenalty)?.flagged ?: false
        val forced = penalty <= -LB_INFINITY
        val best = arrayOfNulls<Node>(4)
        val bestDemerits = FloatArray(4) { Float.MAX_VALUE }
        val next = ArrayList<Node>(active.size)
        for (a in active) {
            val length = sumW[b] - a.totalWidth + if (item is LbPenalty) item.width else 0f
            val target = lineWidth(a.line)
            val ratio = when {
                length < target -> (sumY[b] - a.totalStretch).let { if (it > 0f) (target - length) / it else LB_INFINITY }
                length > target -> (sumZ[b] - a.totalShrink).let { if (it > 0f) (target - length) / it else -LB_INFINITY }
                else -> 0f
            }
            if (ratio >= -1f && ratio <= tolerance) {
                val badness = 100f * abs(ratio) * abs(ratio) * abs(ratio)
                var d = when {
                    penalty >= 0f -> (linePenalty + badness).let { it * it } + penalty * penalty
                    penalty > -LB_INFINITY -> (linePenalty + badness).let { it * it } - penalty * penalty
                    else -> (linePenalty + badness).let { it * it }
                }
                if (flagged && a.flagged) d += flaggedDemerits
                val fitness = when {
                    ratio < -0.5f -> 0
                    ratio <= 0.5f -> 1
                    ratio <= 1f -> 2
                    else -> 3
                }
                if (abs(fitness - a.fitness) > 1) d += fitnessDemerits
                val total = a.demerits + d
                if (total < bestDemerits[fitness]) {
                    bestDemerits[fitness] = total
                    best[fitness] = a
                }
            }
            // Linha já longa demais (ou quebra obrigatória): este nó não serve mais para linhas futuras.
            if (!(ratio < -1f || forced)) next += a
        }
        if (best.any { it != null }) {
            // Totais depois da quebra: pula colas e penalidades até a próxima caixa.
            var tw = sumW[b]
            var ty = sumY[b]
            var tz = sumZ[b]
            var i = b
            while (i < n) {
                val x = items[i]
                if (x is LbBox) break
                if (x is LbGlue) { tw += x.width; ty += x.stretch; tz += x.shrink }
                if (x is LbPenalty && x.penalty <= -LB_INFINITY && i > b) break
                i++
            }
            for (f in 0 until 4) {
                val a = best[f] ?: continue
                next += Node(b, a.line + 1, f, tw, ty, tz, bestDemerits[f], flagged, a)
            }
        }
        if (next.isEmpty()) return null
        active = next
    }
    val end = active.filter { it.position == n - 1 }.minByOrNull { it.demerits } ?: return null
    val breaks = ArrayList<Int>()
    var node: Node? = end
    while (node != null && node.previous != null) {
        breaks += node.position
        node = node.previous
    }
    return breaks.reversed()
}

/** Quebra linha a linha (primeira que couber), usada quando a composição fina não acha solução ou está desligada. */
fun greedyBreaks(items: List<LbItem>, lineWidth: (Int) -> Float): List<Int> {
    val breaks = ArrayList<Int>()
    var line = 0
    var width = 0f
    var lineStart = 0
    var lastLegal = -1
    var i = 0
    fun breakAt(k: Int) {
        breaks += k
        line++
        // A próxima linha começa na próxima caixa (colas e penalidades depois da quebra somem).
        i = k + 1
        while (i < items.size && items[i] !is LbBox && !(items[i] is LbPenalty && (items[i] as LbPenalty).penalty <= -LB_INFINITY)) i++
        lineStart = i
        width = 0f
        lastLegal = lineStart - 1
    }
    while (i < items.size) {
        val item = items[i]
        if (item is LbPenalty && item.penalty <= -LB_INFINITY) { breaks += i; break }
        val legal = (item is LbGlue && i > 0 && items[i - 1] is LbBox) || (item is LbPenalty && item.penalty < LB_INFINITY)
        if (legal) {
            val lineWidthHere = width + if (item is LbPenalty) item.width else 0f
            if (lineWidthHere <= lineWidth(line)) lastLegal = i
            else if (lastLegal < lineStart) { breakAt(i); continue } // nada coube: a linha estoura aqui
        }
        if (item !is LbPenalty) width += item.width
        if (item is LbBox && width > lineWidth(line) && lastLegal >= lineStart) { breakAt(lastLegal); continue }
        i++
    }
    if (breaks.isEmpty() || breaks.last() != items.lastIndex) breaks += items.lastIndex
    return breaks
}

/**
 * Monta as linhas a partir das quebras: distribui o espaço das colas quando [justify] (exceto na última linha
 * e nas que terminam numa quebra obrigatória) e acrescenta o hífen quando a quebra cai numa sílaba.
 */
fun composeLines(items: List<LbItem>, breaks: List<Int>, lineWidth: (Int) -> Float, justify: Boolean): List<ComposedLine> {
    val lines = ArrayList<ComposedLine>()
    var start = 0
    for ((lineIndex, b) in breaks.withIndex()) {
        // Pula colas e penalidades no começo da linha.
        while (start < b && items[start] !is LbBox) start++
        val content = items.subList(start, b)
        val breakItem = items[b]
        val hyphen = (breakItem as? LbPenalty)?.hyphen
        val forced = breakItem is LbPenalty && breakItem.penalty <= -LB_INFINITY
        val target = lineWidth(lineIndex)
        var natural = 0f
        var stretch = 0f
        var shrink = 0f
        // Colas no fim da linha (antes da quebra) não contam.
        val lastBox = content.indexOfLast { it is LbBox }
        content.forEachIndexed { i, it ->
            if (i > lastBox) return@forEachIndexed
            when (it) {
                is LbBox -> natural += it.width
                is LbGlue -> { natural += it.width; stretch += it.stretch; shrink += it.shrink }
                is LbPenalty -> Unit
            }
        }
        if (hyphen != null) natural += hyphen.width
        val ratio = when {
            !justify -> 0f
            // Inclusive a última linha: a composição fina pode tê-la apertado para caber, e sem o aperto ela estoura a margem.
            natural > target && shrink > 0f -> ((target - natural) / shrink).coerceAtLeast(-1f)
            forced -> 0f
            natural < target && stretch > 0f -> ((target - natural) / stretch).coerceAtMost(4f)
            else -> 0f
        }
        val placed = ArrayList<PlacedRun>()
        val notes = ArrayList<String>()
        var x = 0f
        content.forEachIndexed { i, it ->
            if (i > lastBox) return@forEachIndexed
            when (it) {
                is LbBox -> {
                    var rx = x
                    it.runs.forEach { r -> placed.addMerging(PlacedRun(rx, r)); rx += r.width }
                    x += it.width // caixas sem texto (recuo) também ocupam espaço
                    notes += it.footnoteIds
                }
                is LbGlue -> x += it.width + if (ratio >= 0f) ratio * it.stretch else ratio * it.shrink
                is LbPenalty -> Unit
            }
        }
        if (hyphen != null) { placed.addMerging(PlacedRun(x, hyphen)); x += hyphen.width }
        lines += ComposedLine(placed, x, notes)
        start = b + 1
    }
    return lines
}

/**
 * Junta um trecho ao anterior quando ele continua exatamente onde o outro termina, com o mesmo estilo
 * (sílabas de uma palavra hifenizável viram um texto só no PDF, o que ajuda a busca e a cópia de texto).
 */
private fun MutableList<PlacedRun>.addMerging(next: PlacedRun) {
    val last = lastOrNull()
    if (last != null) {
        val a = last.run
        val b = next.run
        if (a.font == b.font && a.sizePt == b.sizePt && a.rise == b.rise && a.trackingEm == b.trackingEm &&
            kotlin.math.abs(last.x + a.width - next.x) < 0.01f
        ) {
            this[lastIndex] = PlacedRun(last.x, a.copy(text = a.text + b.text, width = a.width + b.width))
            return
        }
    }
    add(next)
}
