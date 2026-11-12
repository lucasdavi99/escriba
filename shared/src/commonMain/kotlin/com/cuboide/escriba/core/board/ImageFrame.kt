package com.cuboide.escriba.core.board

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

/**
 * Enquadramento da imagem dentro do nó. [x] e [y] (0 a 1) dizem que ponto da imagem fica no centro da área;
 * [zoom] amplia a partir do tamanho base; [fit] mostra a imagem inteira (com sobras) em vez de preencher a área.
 */
@Serializable
data class ImageFrame(
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val zoom: Float = 1f,
    val fit: Boolean = false,
) {
    companion object {
        const val MAX_ZOOM = 5f
        /** Fração da altura do cartão que a imagem ocupa. */
        const val DEFAULT_CARD_RATIO = 0.45f
        const val MIN_CARD_RATIO = 0.15f
        const val MAX_CARD_RATIO = 0.85f
    }
}

/** Área da imagem do nó, em unidades do quadro, ou `null` se o nó não mostra imagem. */
fun BoardNode.imageRect(): Rect? = when {
    image == null -> null
    kind == NodeKind.Cartao -> Rect(x, y, width, height * imageRatio)
    kind == NodeKind.Imagem -> Rect(x, y, width, height)
    else -> null
}

/** Onde a imagem ([imgW] × [imgH]) é desenhada numa área [boxW] × [boxH], relativo ao canto da área. */
fun placeImage(boxW: Float, boxH: Float, imgW: Float, imgH: Float, frame: ImageFrame): Rect {
    if (imgW <= 0f || imgH <= 0f || boxW <= 0f || boxH <= 0f) return Rect(0f, 0f, boxW, boxH)
    val base = if (frame.fit) min(boxW / imgW, boxH / imgH) else max(boxW / imgW, boxH / imgH)
    val s = base * frame.zoom.coerceIn(1f, ImageFrame.MAX_ZOOM)
    val w = imgW * s
    val h = imgH * s
    return Rect(axis(boxW, w, frame.x), axis(boxH, h, frame.y), w, h)
}

/** Imagem menor que a área fica centrada; maior, segue o ponto focal sem deixar sobra. */
private fun axis(box: Float, size: Float, focus: Float): Float =
    if (size <= box) (box - size) / 2 else (box / 2 - focus * size).coerceIn(box - size, 0f)

/** Faixa útil do ponto focal num eixo: além dela a imagem já encostou na borda. */
private fun clampFocus(focus: Float, box: Float, size: Float): Float {
    if (size <= box) return 0.5f
    val half = box / (2 * size)
    return focus.coerceIn(half, 1f - half)
}

/** Mantém o ponto focal na faixa em que mover ainda tem efeito. */
fun ImageFrame.clamped(boxW: Float, boxH: Float, imgW: Float, imgH: Float): ImageFrame {
    val r = placeImage(boxW, boxH, imgW, imgH, this)
    return copy(
        x = clampFocus(x, boxW, r.width),
        y = clampFocus(y, boxH, r.height),
        zoom = zoom.coerceIn(1f, ImageFrame.MAX_ZOOM),
    )
}

/** Arrastar a imagem por ([dx], [dy]) (mesma unidade da área): o ponto focal anda no sentido contrário. */
fun ImageFrame.panned(dx: Float, dy: Float, boxW: Float, boxH: Float, imgW: Float, imgH: Float): ImageFrame {
    val r = placeImage(boxW, boxH, imgW, imgH, this)
    return copy(x = x - dx / r.width, y = y - dy / r.height).clamped(boxW, boxH, imgW, imgH)
}

fun ImageFrame.zoomed(factor: Float, boxW: Float, boxH: Float, imgW: Float, imgH: Float): ImageFrame =
    copy(zoom = (zoom * factor).coerceIn(1f, ImageFrame.MAX_ZOOM)).clamped(boxW, boxH, imgW, imgH)
