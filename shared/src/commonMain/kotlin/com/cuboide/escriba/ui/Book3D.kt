package com.cuboide.escriba.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.cuboide.escriba.core.layout.BookFace
import com.cuboide.escriba.core.layout.CoverSnapshot
import com.cuboide.escriba.core.layout.MIN_3D_SPINE_MM
import com.cuboide.escriba.core.layout.ProjectedFace
import com.cuboide.escriba.core.layout.projectBook
import com.cuboide.escriba.core.layout.rectToQuad
import kotlin.math.max
import kotlin.math.roundToInt

private val PAGE_COLOR = Color(0xFFF2EAD8)
private val PAGE_LINE = Color(0xFF8C7F68)

/**
 * O livro fechado em 3D, nas medidas de [book], vestido com [skin] (contracapa, lombada e capa lado a lado, sem
 * sangria). Com [interactive], arrastar gira e o duplo clique volta ao ângulo inicial.
 */
@Composable
fun Book3D(
    skin: ImageBitmap,
    book: CoverSnapshot,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    initialYaw: Float = 28f,
    initialTilt: Float = 15f,
) {
    var yaw by remember(book) { mutableStateOf(initialYaw) }
    var tilt by remember(book) { mutableStateOf(initialTilt) }
    val paint = remember { Paint().apply { filterQuality = FilterQuality.High; isAntiAlias = true } }
    val gestures = if (!interactive) Modifier else Modifier
        .pointerInput(book) {
            detectDragGestures { change, drag ->
                change.consume()
                yaw += drag.x * 0.35f
                tilt = (tilt - drag.y * 0.25f).coerceIn(-35f, 35f)
            }
        }
        .pointerInput(book) { detectTapGestures(onDoubleTap = { yaw = initialYaw; tilt = initialTilt }) }

    Canvas(modifier.then(gestures).semantics { contentDescription = "Livro 3D" }) {
        val faces = projectBook(book.trimWidthMm, book.trimHeightMm, book.spineMm, yaw, tilt, size.width, size.height)
        if (faces.isEmpty()) return@Canvas
        // Sombra no chão, sob o livro.
        val minX = faces.minOf { it.xs.min() }
        val maxX = faces.maxOf { it.xs.max() }
        val maxY = faces.maxOf { it.ys.max() }
        val shadowW = (maxX - minX) * 1.05f
        val shadowH = shadowW * 0.12f
        drawOval(
            Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.28f), Color.Transparent), center = Offset((minX + maxX) / 2, maxY), radius = shadowW / 2),
            topLeft = Offset((minX + maxX) / 2 - shadowW / 2, maxY - shadowH / 2),
            size = Size(shadowW, shadowH),
        )
        // Faixas da pele: contracapa | lombada | capa, na proporção das medidas.
        val pxPerMm = skin.width / (2 * book.trimWidthMm + book.spineMm)
        fun band(startMm: Float, widthMm: Float): Pair<Int, Int> {
            val x = (startMm * pxPerMm).roundToInt().coerceIn(0, skin.width - 1)
            val w = (widthMm * pxPerMm).roundToInt().coerceIn(1, skin.width - x)
            return x to w
        }
        for (f in faces) {
            val path = Path().apply {
                moveTo(f.xs[0], f.ys[0]); lineTo(f.xs[1], f.ys[1]); lineTo(f.xs[2], f.ys[2]); lineTo(f.xs[3], f.ys[3]); close()
            }
            val skinBand = when (f.face) {
                BookFace.Contracapa -> band(0f, book.trimWidthMm)
                BookFace.Lombada -> band(book.trimWidthMm, book.spineMm)
                BookFace.Capa -> band(book.trimWidthMm + book.spineMm, book.trimWidthMm)
                else -> null
            }
            if (skinBand != null) {
                val (sx, sw) = skinBand
                val m = rectToQuad(sw.toFloat(), skin.height.toFloat(), f.xs, f.ys)
                val matrix = Matrix().apply {
                    values[Matrix.ScaleX] = m[0]; values[Matrix.SkewX] = m[1]; values[Matrix.TranslateX] = m[2]
                    values[Matrix.SkewY] = m[3]; values[Matrix.ScaleY] = m[4]; values[Matrix.TranslateY] = m[5]
                    values[Matrix.Perspective0] = m[6]; values[Matrix.Perspective1] = m[7]; values[Matrix.Perspective2] = 1f
                }
                drawIntoCanvas { canvas ->
                    canvas.save()
                    canvas.concat(matrix)
                    canvas.drawImageRect(skin, IntOffset(sx, 0), IntSize(sw, skin.height), IntOffset.Zero, IntSize(sw, skin.height), paint)
                    canvas.restore()
                }
            } else {
                drawPath(path, PAGE_COLOR)
                pageLines(f, book)
            }
            // Luz: escurece o que fica de costas para ela.
            // O papel claro escurece menos que a capa impressa, senão o bloco de páginas fica cinza.
            val darkness = if (skinBand != null) 0.7f else 0.45f
            drawPath(path, Color.Black.copy(alpha = ((1f - f.shade) * darkness).coerceIn(0f, 0.6f)))
        }
    }
}

/** Fios finos do bloco de páginas, paralelos às capas. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.pageLines(f: ProjectedFace, book: CoverSnapshot) {
    val n = (max(book.spineMm, MIN_3D_SPINE_MM) * 1.6f).roundToInt().coerceIn(3, 40)
    fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    for (k in 1 until n) {
        val t = k / n.toFloat()
        // No corte as linhas correm na altura (entre a borda de cima e a de baixo); na cabeça e no pé, na largura.
        val (a, b) = if (f.face == BookFace.Corte) {
            Offset(lerp(f.xs[0], f.xs[1], t), lerp(f.ys[0], f.ys[1], t)) to Offset(lerp(f.xs[3], f.xs[2], t), lerp(f.ys[3], f.ys[2], t))
        } else {
            Offset(lerp(f.xs[0], f.xs[3], t), lerp(f.ys[0], f.ys[3], t)) to Offset(lerp(f.xs[1], f.xs[2], t), lerp(f.ys[1], f.ys[2], t))
        }
        drawLine(PAGE_LINE.copy(alpha = 0.18f), a, b, strokeWidth = 0.8f)
    }
}
