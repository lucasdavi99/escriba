package com.cuboide.escriba.core.layout

import kotlinx.serialization.Serializable
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Livro fechado em 3D para a prévia: uma caixa com as medidas reais (formato e lombada), girada e projetada em
 * perspectiva. As capas recebem a imagem da capa aberta; o corte, a cabeça e o pé mostram o bloco de páginas.
 */

/** Medidas que a vitrine precisa da capa (gravadas junto da imagem de capa, contracapa e lombada). */
@Serializable
data class CoverSnapshot(val trimWidthMm: Float, val trimHeightMm: Float, val spineMm: Float)

/** Lombada mínima desenhada, para que um livro fino ainda pareça ter corpo. */
const val MIN_3D_SPINE_MM = 1.5f

enum class BookFace { Capa, Lombada, Contracapa, Corte, Cabeca, Pe }

data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    infix fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
    fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    fun normalized(): Vec3 {
        val l = sqrt(this dot this)
        return if (l == 0f) this else Vec3(x / l, y / l, z / l)
    }
}

/** Face na tela: cantos na ordem da imagem (sup. esq., sup. dir., inf. dir., inf. esq.), em pixels com y para baixo. */
data class ProjectedFace(val face: BookFace, val xs: FloatArray, val ys: FloatArray, val shade: Float, val depth: Float)

/** Cantos de cada face no espaço do livro (x para a direita, y para cima, z para quem olha), centrado na origem. */
fun bookFaces(widthMm: Float, heightMm: Float, depthMm: Float): Map<BookFace, List<Vec3>> {
    val w = widthMm / 2
    val h = heightMm / 2
    val d = max(depthMm, MIN_3D_SPINE_MM) / 2
    return mapOf(
        BookFace.Capa to listOf(Vec3(-w, h, d), Vec3(w, h, d), Vec3(w, -h, d), Vec3(-w, -h, d)),
        // Lombada vista de frente: a borda direita encosta na capa.
        BookFace.Lombada to listOf(Vec3(-w, h, -d), Vec3(-w, h, d), Vec3(-w, -h, d), Vec3(-w, -h, -d)),
        // Contracapa vista por trás: a borda direita encosta na lombada.
        BookFace.Contracapa to listOf(Vec3(w, h, -d), Vec3(-w, h, -d), Vec3(-w, -h, -d), Vec3(w, -h, -d)),
        BookFace.Corte to listOf(Vec3(w, h, d), Vec3(w, h, -d), Vec3(w, -h, -d), Vec3(w, -h, d)),
        BookFace.Cabeca to listOf(Vec3(-w, h, -d), Vec3(w, h, -d), Vec3(w, h, d), Vec3(-w, h, d)),
        BookFace.Pe to listOf(Vec3(-w, -h, d), Vec3(w, -h, d), Vec3(w, -h, -d), Vec3(-w, -h, -d)),
    )
}

/** Gira em torno de y ([yawDeg] positivo mostra a lombada) e depois de x ([tiltDeg] positivo mostra a cabeça). */
fun rotateBook(v: Vec3, yawDeg: Float, tiltDeg: Float): Vec3 {
    val a = yawDeg * PI_F / 180f
    val b = tiltDeg * PI_F / 180f
    val x1 = v.x * cos(a) + v.z * sin(a)
    val z1 = -v.x * sin(a) + v.z * cos(a)
    val y2 = v.y * cos(b) - z1 * sin(b)
    val z2 = v.y * sin(b) + z1 * cos(b)
    return Vec3(x1, y2, z2)
}

private const val PI_F = 3.1415927f

/** Luz vinda do alto, da esquerda e da frente. */
private val LIGHT = Vec3(-0.45f, 0.55f, 0.7f).normalized()

/**
 * Projeta o livro numa área de [viewW] × [viewH] pixels: só as faces voltadas para quem olha, da mais funda para a
 * mais próxima. O tamanho não muda com o giro (a escala vem da diagonal da caixa).
 */
fun projectBook(widthMm: Float, heightMm: Float, depthMm: Float, yawDeg: Float, tiltDeg: Float, viewW: Float, viewH: Float): List<ProjectedFace> {
    val d = max(depthMm, MIN_3D_SPINE_MM)
    val diag = sqrt(widthMm * widthMm + heightMm * heightMm + d * d)
    val scale = minOf(viewW, viewH) * 0.86f / diag
    val camera = diag * 3.2f
    val cx = viewW / 2
    val cy = viewH / 2
    return bookFaces(widthMm, heightMm, depthMm).mapNotNull { (face, corners) ->
        val r = corners.map { rotateBook(it, yawDeg, tiltDeg) }
        // Cantos em sentido horário com y para cima: (inf. esq. − sup. esq.) × (sup. dir. − sup. esq.) aponta para fora.
        val normal = (r[3] - r[0]).cross(r[1] - r[0]).normalized()
        // Câmera em +z: a face aparece se aponta para ela (considerando a perspectiva do centro da face).
        val center = Vec3(r.sumOf { it.x.toDouble() }.toFloat() / 4, r.sumOf { it.y.toDouble() }.toFloat() / 4, r.sumOf { it.z.toDouble() }.toFloat() / 4)
        val toCamera = Vec3(-center.x, -center.y, camera - center.z).normalized()
        if ((normal dot toCamera) <= 0.01f) return@mapNotNull null
        val xs = FloatArray(4)
        val ys = FloatArray(4)
        r.forEachIndexed { i, p ->
            val k = camera / (camera - p.z)
            xs[i] = cx + p.x * k * scale
            ys[i] = cy - p.y * k * scale
        }
        val shade = (0.42f + 0.58f * (normal dot LIGHT).coerceAtLeast(0f)).coerceIn(0f, 1f)
        ProjectedFace(face, xs, ys, shade, center.z)
    }.sortedBy { it.depth }
}

/**
 * Homografia que leva o retângulo (0,0)–([srcW],[srcH]) para o quadrilátero ([xs], [ys]) (sup. esq., sup. dir.,
 * inf. dir., inf. esq.). Devolve a, b, c, d, e, f, g, h com x' = (a·x + b·y + c) / (g·x + h·y + 1) e
 * y' = (d·x + e·y + f) / (g·x + h·y + 1).
 */
fun rectToQuad(srcW: Float, srcH: Float, xs: FloatArray, ys: FloatArray): FloatArray {
    val (x0, x1, x2, x3) = xs
    val (y0, y1, y2, y3) = ys
    val dx1 = x1 - x2
    val dx2 = x3 - x2
    val dx3 = x0 - x1 + x2 - x3
    val dy1 = y1 - y2
    val dy2 = y3 - y2
    val dy3 = y0 - y1 + y2 - y3
    val den = dx1 * dy2 - dx2 * dy1
    val g = if (den == 0f) 0f else (dx3 * dy2 - dx2 * dy3) / den
    val h = if (den == 0f) 0f else (dx1 * dy3 - dx3 * dy1) / den
    val a = x1 - x0 + g * x1
    val b = x3 - x0 + h * x3
    val d = y1 - y0 + g * y1
    val e = y3 - y0 + h * y3
    // Do quadrado unitário para o retângulo da imagem: divide os termos de u por srcW e os de v por srcH.
    return floatArrayOf(a / srcW, b / srcH, x0, d / srcW, e / srcH, y0, g / srcW, h / srcH)
}

/** Aplica a homografia de [rectToQuad] a um ponto. */
fun applyHomography(m: FloatArray, x: Float, y: Float): Pair<Float, Float> {
    val w = m[6] * x + m[7] * y + 1f
    return (m[0] * x + m[1] * y + m[2]) / w to (m[3] * x + m[4] * y + m[5]) / w
}

/** Medidas do livro fechado desta capa. */
fun CoverGeometry.snapshot() = CoverSnapshot(trimWidthMm, trimHeightMm, spineMm)
