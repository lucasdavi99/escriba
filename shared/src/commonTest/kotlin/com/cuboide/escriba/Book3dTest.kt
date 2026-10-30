package com.cuboide.escriba

import com.cuboide.escriba.core.layout.BookFace
import com.cuboide.escriba.core.layout.applyHomography
import com.cuboide.escriba.core.layout.projectBook
import com.cuboide.escriba.core.layout.rectToQuad
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Book3dTest {
    private fun faces(yaw: Float, tilt: Float) = projectBook(140f, 210f, 15f, yaw, tilt, 600f, 600f).map { it.face }.toSet()

    @Test
    fun visibleFacesFollowTheTurn() {
        assertEquals(setOf(BookFace.Capa), faces(0f, 0f))
        // Girando para a esquerda aparece a lombada; inclinando, a cabeça (bloco de páginas).
        assertEquals(setOf(BookFace.Capa, BookFace.Lombada, BookFace.Cabeca), faces(30f, 10f))
        assertEquals(setOf(BookFace.Capa, BookFace.Corte), faces(-30f, 0f))
        assertEquals(setOf(BookFace.Contracapa), faces(180f, 0f))
        assertTrue(BookFace.Pe in faces(0f, -15f))
    }

    @Test
    fun frontFaceKeepsItsOrientationAndSize() {
        val front = projectBook(140f, 210f, 15f, 0f, 0f, 600f, 600f).single { it.face == BookFace.Capa }
        assertTrue(front.xs[0] < front.xs[1] && front.ys[0] < front.ys[3], "cantos fora de ordem")
        // Livro em pé: mais alto que largo, dentro da área e centrado.
        val w = front.xs[1] - front.xs[0]
        val h = front.ys[3] - front.ys[0]
        assertEquals(210f / 140f, h / w, 0.01f)
        assertTrue(front.xs.all { it in 0f..600f } && front.ys.all { it in 0f..600f })
        assertEquals(300f, (front.xs[0] + front.xs[1]) / 2, 0.5f)
        // A escala não muda com o giro: a altura da capa varia pouco (só a perspectiva).
        val turned = projectBook(140f, 210f, 15f, 35f, 0f, 600f, 600f).single { it.face == BookFace.Capa }
        val turnedH = maxOf(turned.ys[3] - turned.ys[0], turned.ys[2] - turned.ys[1])
        assertTrue(abs(turnedH - h) / h < 0.08f, "altura $h → $turnedH")
        // A face mais próxima é desenhada por último.
        val order = projectBook(140f, 210f, 15f, 30f, 10f, 600f, 600f)
        assertEquals(order.sortedBy { it.depth }, order)
    }

    @Test
    fun homographyHitsTheFourCorners() {
        val xs = floatArrayOf(110f, 420f, 400f, 130f)
        val ys = floatArrayOf(80f, 120f, 520f, 470f)
        val m = rectToQuad(800f, 1200f, xs, ys)
        val src = listOf(0f to 0f, 800f to 0f, 800f to 1200f, 0f to 1200f)
        src.forEachIndexed { i, (x, y) ->
            val (px, py) = applyHomography(m, x, y)
            assertEquals(xs[i], px, 0.01f)
            assertEquals(ys[i], py, 0.01f)
        }
        // Paralelogramo: sem termo de perspectiva.
        val affine = rectToQuad(10f, 10f, floatArrayOf(0f, 10f, 15f, 5f), floatArrayOf(0f, 0f, 10f, 10f))
        assertEquals(0f, affine[6], 1e-6f)
        assertEquals(0f, affine[7], 1e-6f)
    }
}
