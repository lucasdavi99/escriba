package com.cuboide.escriba.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuboide.escriba.core.layout.MarginEdge
import com.cuboide.escriba.core.layout.PageMargins
import com.cuboide.escriba.core.layout.TrimSize
import com.cuboide.escriba.core.layout.mmToPt
import com.cuboide.escriba.core.layout.ptToMm
import com.cuboide.escriba.platform.movePointerIcon
import com.cuboide.escriba.platform.resizePointerIcon
import kotlin.math.roundToInt

/** Largura das réguas em volta das páginas. */
val RULER_SIZE: Dp = 16.dp

private fun mm(v: Float): String {
    val r = (v * 10f).roundToInt() / 10f
    return (if (r == r.toInt().toFloat()) r.toInt().toString() else r.toString()).replace('.', ',')
}

/** Qual margem fica de cada lado da página: na direita (ímpar) a interna é a da esquerda; na esquerda, o contrário. */
private fun leftEdge(recto: Boolean) = if (recto) MarginEdge.Inner else MarginEdge.Outer
private fun rightEdge(recto: Boolean) = if (recto) MarginEdge.Outer else MarginEdge.Inner

private enum class Grab { Left, Right, Top, Bottom, Move }

/**
 * Mancha da página desenhada por cima da prévia, com alças: arrastar uma borda muda aquela margem; arrastar o
 * miolo move a mancha inteira. Durante o arraste mostra a medida; ao soltar chama [onCommit] (a diagramação
 * é refeita uma vez só). [zoom] é dp por ponto tipográfico.
 */
@Composable
fun MarginOverlay(
    margins: PageMargins,
    trim: TrimSize,
    recto: Boolean,
    zoom: Float,
    selected: Boolean,
    own: Boolean,
    onSelect: () -> Unit,
    onCommit: (PageMargins) -> Unit,
) {
    var live by remember { mutableStateOf<PageMargins?>(null) }
    var grabbed by remember { mutableStateOf<Grab?>(null) }
    val base by rememberUpdatedState(margins)
    val commit by rememberUpdatedState(onCommit)
    val select by rememberUpdatedState(onSelect)
    val density = LocalDensity.current.density
    val m = live ?: margins
    val accent = LocalPalette.current.accent
    val boxColor = when {
        live != null -> accent
        own -> Color(0xFFB0662B)
        else -> Color(0xFF5A82C8)
    }

    fun mmOf(px: Float) = ptToMm(px / density / zoom)
    fun Modifier.drag(grab: Grab) = pointerInput(grab, recto, trim) {
        detectDragGestures(
            onDragStart = { select(); live = base; grabbed = grab },
            onDragEnd = { live?.let { commit(it.fitIn(trim)) }; live = null; grabbed = null },
            onDragCancel = { live = null; grabbed = null },
        ) { change, drag ->
            change.consume()
            val cur = live ?: return@detectDragGestures
            val dx = mmOf(drag.x)
            val dy = mmOf(drag.y)
            val l = leftEdge(recto)
            val r = rightEdge(recto)
            live = when (grab) {
                Grab.Left -> cur.with(l, cur.get(l) + dx)
                Grab.Right -> cur.with(r, cur.get(r) - dx)
                Grab.Top -> cur.with(MarginEdge.Top, cur.topMm + dy)
                Grab.Bottom -> cur.with(MarginEdge.Bottom, cur.bottomMm - dy)
                Grab.Move -> cur.with(l, cur.get(l) + dx).let { it.with(r, it.get(r) - dx) }
                    .let { it.copy(topMm = it.topMm + dy, bottomMm = it.bottomMm - dy) }
            }
        }
    }

    val leftPt = mmToPt(m.get(leftEdge(recto)))
    val rightPt = mmToPt(m.get(rightEdge(recto)))
    val topPt = mmToPt(m.topMm)
    val bottomPt = mmToPt(m.bottomMm)
    val wPt = mmToPt(trim.widthMm)
    val hPt = mmToPt(trim.heightMm)
    val boxL = (leftPt * zoom).dp
    val boxT = (topPt * zoom).dp
    val boxW = ((wPt - leftPt - rightPt) * zoom).coerceAtLeast(1f).dp
    val boxH = ((hPt - topPt - bottomPt) * zoom).coerceAtLeast(1f).dp
    val grip = 10.dp

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val z = zoom * density
            val dash = PathEffect.dashPathEffect(floatArrayOf(4f * density, 3f * density))
            drawRect(
                boxColor.copy(alpha = if (live != null || selected) 0.95f else 0.55f),
                topLeft = Offset(leftPt * z, topPt * z),
                size = Size((wPt - leftPt - rightPt) * z, (hPt - topPt - bottomPt) * z),
                style = Stroke(width = (if (selected || live != null) 1.5f else 1f) * density, pathEffect = if (live != null) null else dash),
            )
        }
        // Miolo da mancha: mover tudo (e clicar seleciona a página).
        Box(Modifier.offset(boxL + grip / 2, boxT + grip / 2).size(boxW - grip, boxH - grip).pointerHoverIcon(movePointerIcon()).drag(Grab.Move))
        Box(Modifier.offset(boxL - grip / 2, boxT).size(grip, boxH).pointerHoverIcon(resizePointerIcon(true)).drag(Grab.Left))
        Box(Modifier.offset(boxL + boxW - grip / 2, boxT).size(grip, boxH).pointerHoverIcon(resizePointerIcon(true)).drag(Grab.Right))
        Box(Modifier.offset(boxL, boxT - grip / 2).size(boxW, grip).pointerHoverIcon(resizePointerIcon(false)).drag(Grab.Top))
        Box(Modifier.offset(boxL, boxT + boxH - grip / 2).size(boxW, grip).pointerHoverIcon(resizePointerIcon(false)).drag(Grab.Bottom))
        val g = grabbed
        if (live != null && g != null) {
            val label = when (g) {
                Grab.Left -> leftEdge(recto).let { "${it.label} ${mm(m.get(it))} mm" }
                Grab.Right -> rightEdge(recto).let { "${it.label} ${mm(m.get(it))} mm" }
                Grab.Top -> "Superior ${mm(m.topMm)} mm"
                Grab.Bottom -> "Inferior ${mm(m.bottomMm)} mm"
                Grab.Move -> "Interna ${mm(m.innerMm)} · Externa ${mm(m.outerMm)} · Sup. ${mm(m.topMm)} · Inf. ${mm(m.bottomMm)} mm"
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.offset(boxL + 4.dp, boxT + 4.dp)
                    .background(accent.copy(alpha = 0.92f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

/**
 * Régua em mm ao longo de uma página: traço a cada mm (quando há espaço), maior a cada 5 e número a cada 10.
 * [marksMm] são as bordas da mancha, destacadas na régua.
 */
@Composable
fun MmRuler(lengthMm: Float, zoom: Float, horizontal: Boolean, marksMm: List<Float>, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    val bg = MaterialTheme.colorScheme.surfaceContainerLow
    val accent = LocalPalette.current.accent
    val lengthDp = (mmToPt(lengthMm) * zoom).dp
    Canvas(modifier.then(if (horizontal) Modifier.size(lengthDp, RULER_SIZE) else Modifier.size(RULER_SIZE, lengthDp))) {
        drawRect(bg)
        val pxPerMm = mmToPt(1f) * zoom * density
        val thick = size.minDimension
        // Com zoom pequeno os traços de 1 mm viram um borrão: só os de 5 em 5.
        val step = if (pxPerMm >= 3f) 1 else 5
        val labelEvery = if (pxPerMm * 10f >= 22f) 10 else 50
        val style = TextStyle(fontSize = 8.sp, color = ink)
        var i = 0
        while (i <= lengthMm) {
            val p = i * pxPerMm
            val len = when {
                i % 10 == 0 -> thick * 0.55f
                i % 5 == 0 -> thick * 0.35f
                else -> thick * 0.18f
            }
            if (horizontal) drawLine(ink, Offset(p, thick), Offset(p, thick - len), 1f)
            else drawLine(ink, Offset(thick, p), Offset(thick - len, p), 1f)
            if (i % labelEvery == 0 && i > 0) {
                val t = measurer.measure((i / 10).toString(), style)
                if (horizontal) drawText(t, topLeft = Offset(p + 2f, 0f))
                else drawText(t, topLeft = Offset(1f, p + 1f))
            }
            i += step
        }
        for (mark in marksMm) {
            val p = mark * pxPerMm
            if (horizontal) drawLine(accent, Offset(p, 0f), Offset(p, thick), 2f)
            else drawLine(accent, Offset(0f, p), Offset(thick, p), 2f)
        }
    }
}
