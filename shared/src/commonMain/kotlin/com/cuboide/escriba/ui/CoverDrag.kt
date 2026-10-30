package com.cuboide.escriba.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.core.layout.CoverLayout
import com.cuboide.escriba.core.layout.CoverPiece
import com.cuboide.escriba.core.layout.PtRect
import com.cuboide.escriba.core.layout.ptToMm
import com.cuboide.escriba.platform.movePointerIcon
import kotlin.math.roundToInt

/** O que o arraste na prévia da capa move. */
enum class CoverDragMode(val label: String) { Pecas("Textos e imagens"), Arte("A arte") }

private fun mm1(v: Float): String {
    val r = (v * 10f).roundToInt() / 10f
    return (if (r == r.toInt().toFloat()) r.toInt().toString() else r.toString()).replace('.', ',')
}

private class DragTarget(val key: String, val label: String, val rect: PtRect, val lockX: Boolean)

/**
 * Camada de arraste sobre a prévia da capa. [zoom] é dp por ponto; a prévia começa na sangria. Durante o arraste
 * o contorno acompanha o mouse; ao soltar, o deslocamento (em mm) vai para [onPiece], [onOverlay] ou [onArt].
 */
@Composable
fun CoverDragLayer(
    cover: CoverLayout,
    zoom: Float,
    mode: CoverDragMode,
    onPiece: (CoverPiece, Float, Float) -> Unit,
    onOverlay: (String, Float, Float) -> Unit,
    onArt: (Float, Float) -> Unit,
) {
    val density = LocalDensity.current.density
    val bleed = cover.geometry.bleedPt
    val accent = LocalPalette.current.accent
    var dragging by remember { mutableStateOf<String?>(null) }
    var dxMm by remember { mutableStateOf(0f) }
    var dyMm by remember { mutableStateOf(0f) }
    val piece by rememberUpdatedState(onPiece)
    val overlay by rememberUpdatedState(onOverlay)
    val art by rememberUpdatedState(onArt)
    fun mmOf(px: Float) = ptToMm(px / density / zoom)

    fun Modifier.draggable(key: String, lockX: Boolean, commit: (Float, Float) -> Unit) = pointerInput(key, lockX) {
        detectDragGestures(
            onDragStart = { dragging = key; dxMm = 0f; dyMm = 0f },
            onDragEnd = { if (dxMm != 0f || dyMm != 0f) commit(dxMm, dyMm); dragging = null },
            onDragCancel = { dragging = null },
        ) { change, drag ->
            change.consume()
            if (!lockX) dxMm += mmOf(drag.x)
            dyMm += mmOf(drag.y)
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (mode == CoverDragMode.Arte) {
            Box(
                Modifier.fillMaxSize().semantics { contentDescription = "Arrastar a arte" }.pointerHoverIcon(movePointerIcon())
                    .draggable("arte", false) { dx, dy -> art(dx, dy) },
            )
            if (dragging == "arte") Label("Arte: ${mm1(dxMm)} × ${mm1(dyMm)} mm", accent, Modifier.padding(8.dp))
            return@Box
        }
        val targets = cover.pieces.map { (p, r) ->
            DragTarget(p.name, p.label, r, lockX = p == CoverPiece.LombadaTitulo || p == CoverPiece.LombadaAutor)
        } + cover.overlays.mapNotNull { o -> o.overlayId?.let { DragTarget("o:$it", "Imagem", PtRect(o.x, o.y, o.width, o.height), false) } }
        for (t in targets) {
            val active = dragging == t.key
            val mx = if (active) dxMm else 0f
            val my = if (active) dyMm else 0f
            Box(
                Modifier
                    .offset(((t.rect.x + bleed + com.cuboide.escriba.core.layout.mmToPt(mx)) * zoom).dp, ((t.rect.y + bleed + com.cuboide.escriba.core.layout.mmToPt(my)) * zoom).dp)
                    .size((t.rect.width * zoom).coerceAtLeast(6f).dp, (t.rect.height * zoom).coerceAtLeast(6f).dp)
                    .border(if (active) 1.5.dp else 1.dp, if (active) accent else Color.White.copy(alpha = 0.45f))
                    .semantics { contentDescription = "Arrastar ${t.label}" }
                    .pointerHoverIcon(movePointerIcon())
                    .draggable(t.key, t.lockX) { dx, dy ->
                        if (t.key.startsWith("o:")) overlay(t.key.removePrefix("o:"), dx, dy)
                        else piece(CoverPiece.valueOf(t.key), dx, dy)
                    },
            )
            if (active) {
                Label(
                    "${t.label}: ${mm1(mx)} × ${mm1(my)} mm", accent,
                    Modifier.offset(((t.rect.x + bleed) * zoom).dp, ((t.rect.y + bleed) * zoom).dp - 22.dp),
                )
            }
        }
    }
}

@Composable
private fun Label(text: String, color: Color, modifier: Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier.background(color.copy(alpha = 0.92f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
