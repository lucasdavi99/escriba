package com.cuboide.escriba.core.language

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/** Ponto do desenho do glifo, em unidades da fonte (1000 por em, y para cima, linha de base em 0). */
data class GPt(val x: Float, val y: Float)

/**
 * Letra da escrita inventada: o som que ela representa, o caractere da área de uso privado do Unicode que a
 * guarda no texto, os traços (para desenhar na tela) e o contorno preenchido (para a fonte).
 */
data class ScriptGlyph(
    val unit: String,
    val codepoint: Int,
    val strokes: List<List<GPt>>,
    val contours: List<List<GPt>>,
    val advance: Int,
)

/**
 * Alfabeto da língua: uma letra por som, montada com traços (retas ou curvas, conforme o estilo) numa grade.
 * Letras parecidas demais são sorteadas de novo. Tudo sai da semente: a mesma língua tem sempre a mesma escrita.
 */
class Script(val lang: Language) {
    val glyphs: List<ScriptGlyph>
    private val byUnit: Map<String, ScriptGlyph>
    private val byCodepoint: Map<Int, ScriptGlyph>
    /** Sons em ordem do mais longo para o mais curto (para ler "nh" antes de "n"). */
    private val matchOrder: List<String>
    val rounded: Boolean = lang.style in setOf(LangStyle.Rio, LangStyle.Coral, LangStyle.Areia)

    init {
        val r = Rng.stream(lang.seed, "escrita:${Language.GENERATOR_VERSION}:${lang.style.name}")
        val units = lang.consonants + lang.vowels
        val made = mutableListOf<ScriptGlyph>()
        val signatures = mutableListOf<Long>()
        units.forEachIndexed { i, unit ->
            val vowel = unit.first() in Language.VOWEL_LETTERS
            var best: List<List<GPt>>? = null
            var bestDistance = -1
            for (attempt in 0 until 60) {
                val strokes = design(r, vowel)
                val sig = signature(strokes)
                val d = signatures.minOfOrNull { (it xor sig).countOneBits() } ?: 64
                if (d > bestDistance) { best = strokes; bestDistance = d }
                if (d >= MIN_DISTANCE) break
            }
            val strokes = best!!
            signatures += signature(strokes)
            made += ScriptGlyph(unit, FIRST_CODEPOINT + i, strokes, outline(strokes), ADVANCE)
        }
        glyphs = made
        byUnit = made.associateBy { it.unit }
        byCodepoint = made.associateBy { it.codepoint }
        matchOrder = units.sortedByDescending { it.length }
    }

    /** Texto em letras nossas → texto na escrita (cada som vira o seu caractere; o resto fica). */
    fun transliterate(roman: String): String = buildString {
        var i = 0
        val lower = roman.lowercase()
        while (i < lower.length) {
            val unit = matchOrder.firstOrNull { lower.startsWith(it, i) }
            if (unit != null) {
                appendCodePoint(byUnit.getValue(unit).codepoint)
                i += unit.length
            } else {
                append(roman[i])
                i++
            }
        }
    }

    /** Volta da escrita para letras nossas (para exportar texto sem a fonte). */
    fun romanize(script: String): String = buildString {
        // A área de uso privado (E000–F8FF) é toda do plano básico: um caractere por letra.
        for (ch in script) append(byCodepoint[ch.code]?.unit ?: ch.toString())
    }

    private fun StringBuilder.appendCodePoint(cp: Int) = append(cp.toChar())

    // ---------- desenho ----------

    private fun design(r: Rng, vowel: Boolean): List<List<GPt>> {
        val top = if (vowel) 450f else 650f
        val xs = listOf(90f, 300f, 510f)
        val ys = listOf(0f, top / 2, top)
        fun point() = GPt(r.pick(xs), r.pick(ys))
        val count = if (vowel) 1 + r.nextInt(2) else 2 + r.nextInt(2)
        val strokes = mutableListOf<List<GPt>>()
        repeat(count) {
            val a = point()
            var b = point()
            var guard = 0
            while (b == a && guard++ < 10) b = point()
            if (b == a) return@repeat
            strokes += if (rounded) arc(a, b, (if (r.chance(0.5)) 1 else -1) * (0.25f + r.nextDouble().toFloat() * 0.25f))
            else if (r.chance(0.4)) listOf(a, b, point().takeIf { it != b } ?: a)
            else listOf(a, b)
        }
        // Ponto solto: marca de vogal ou diacrítico.
        if (r.chance(if (vowel) 0.6 else 0.25)) strokes += listOf(GPt(r.pick(xs), top + 120f))
        return strokes
    }

    /** Curva de [a] até [b], abaulada para o lado por [bulge] do comprimento. */
    private fun arc(a: GPt, b: GPt, bulge: Float): List<GPt> {
        val len = hypot(b.x - a.x, b.y - a.y)
        val nx = -(b.y - a.y) / len
        val ny = (b.x - a.x) / len
        return (0..12).map { i ->
            val t = i / 12f
            val bump = 4 * t * (1 - t) * bulge * len
            GPt(a.x + (b.x - a.x) * t + nx * bump, a.y + (b.y - a.y) * t + ny * bump)
        }
    }

    /** Assinatura 8×8 do desenho, para comparar letras (quantas casas diferem). */
    private fun signature(strokes: List<List<GPt>>): Long {
        var bits = 0L
        fun mark(p: GPt) {
            val cx = ((p.x / 600f) * 8).toInt().coerceIn(0, 7)
            val cy = ((p.y / 800f) * 8).toInt().coerceIn(0, 7)
            bits = bits or (1L shl (cy * 8 + cx))
        }
        for (s in strokes) {
            if (s.size == 1) { mark(s[0]); continue }
            for (k in 0 until s.size - 1) for (t in 0..10) {
                val a = s[k]; val b = s[k + 1]
                mark(GPt(a.x + (b.x - a.x) * t / 10f, a.y + (b.y - a.y) * t / 10f))
            }
        }
        return bits
    }

    /**
     * Contornos preenchidos dos traços: um quadrilátero por trecho, um octógono em cada junta e nos pontos. Todos no
     * sentido horário (o que a fonte espera para área cheia); sobreposições somam com a regra não nula.
     */
    private fun outline(strokes: List<List<GPt>>): List<List<GPt>> {
        val half = STROKE / 2
        val out = mutableListOf<List<GPt>>()
        for (s in strokes) {
            if (s.size == 1) { out += octagon(s[0], half * 1.3f); continue }
            for (k in 0 until s.size - 1) {
                val a = s[k]; val b = s[k + 1]
                val len = hypot(b.x - a.x, b.y - a.y)
                if (len < 1f) continue
                val nx = -(b.y - a.y) / len * half
                val ny = (b.x - a.x) / len * half
                out += listOf(GPt(a.x + nx, a.y + ny), GPt(b.x + nx, b.y + ny), GPt(b.x - nx, b.y - ny), GPt(a.x - nx, a.y - ny))
            }
            // Juntas e pontas arredondadas (sem elas, curvas e cantos ficam com frestas).
            val joints = if (rounded) listOf(s.first(), s.last()) else s
            joints.forEach { out += octagon(it, half) }
        }
        return out.map { clockwise(it) }
    }

    private fun octagon(c: GPt, r: Float): List<GPt> = (0 until 8).map { i ->
        val a = PI / 8 + i * PI / 4
        GPt(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat())
    }

    /** Área com sinal (positiva = anti-horário, com y para cima). */
    private fun area(p: List<GPt>): Float = p.indices.sumOf { i ->
        val a = p[i]; val b = p[(i + 1) % p.size]
        (a.x * b.y - b.x * a.y).toDouble()
    }.toFloat() / 2

    private fun clockwise(p: List<GPt>): List<GPt> = if (area(p) > 0) p.reversed() else p

    companion object {
        const val FIRST_CODEPOINT = 0xE000
        const val LAST_CODEPOINT = 0xF8FF
        const val ADVANCE = 620
        const val STROKE = 70f
        private const val MIN_DISTANCE = 6

        fun isScriptChar(cp: Int) = cp in FIRST_CODEPOINT..LAST_CODEPOINT
    }
}

/** Contornos em coordenadas inteiras (o que a fonte grava). */
fun ScriptGlyph.intContours(): List<List<Pair<Int, Int>>> =
    contours.map { c -> c.map { it.x.roundToInt() to it.y.roundToInt() }.distinctAdjacent() }.filter { it.size >= 3 && abs(it.size) > 0 }

private fun List<Pair<Int, Int>>.distinctAdjacent(): List<Pair<Int, Int>> {
    val out = ArrayList<Pair<Int, Int>>(size)
    for (p in this) if (out.isEmpty() || out.last() != p) out += p
    if (out.size > 1 && out.first() == out.last()) out.removeAt(out.size - 1)
    return out
}

/** Fonte TrueType da escrita, com um glifo por som nos caracteres da área de uso privado. */
fun Script.fontBytes(): ByteArray = com.cuboide.escriba.core.font.writeTtf(
    "${lang.name} Escrita",
    glyphs.map { com.cuboide.escriba.core.font.TtfGlyph(it.codepoint, it.advance, it.intContours()) },
)
