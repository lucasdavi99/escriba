package com.cuboide.escriba.core.language

/**
 * Sorteio reproduzível (SplitMix64): a mesma semente dá sempre os mesmos números, em qualquer máquina. Cada parte
 * da língua usa uma sequência própria ([stream]), então acrescentar palavras novas não muda as que já existem.
 */
class Rng(seed: Long) {
    private var state = seed

    fun nextLong(): Long {
        state += -0x61c8864680b583ebL // 0x9E3779B97F4A7C15
        var z = state
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L // 0xBF58476D1CE4E5B9
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L // 0x94D049BB133111EB
        return z xor (z ushr 31)
    }

    /** Inteiro em [0, n). */
    fun nextInt(n: Int): Int = if (n <= 1) 0 else ((nextLong() ushr 1) % n).toInt()

    fun nextDouble(): Double = (nextLong() ushr 11) * (1.0 / (1L shl 53))

    fun chance(p: Double): Boolean = nextDouble() < p

    fun <T> pick(items: List<T>): T = items[nextInt(items.size)]

    /** Escolha ponderada: cada item com o seu peso. */
    fun <T> weighted(items: Map<T, Int>): T {
        val total = items.values.sum().coerceAtLeast(1)
        var r = nextInt(total)
        for ((item, w) in items) {
            if (r < w) return item
            r -= w
        }
        return items.keys.first()
    }

    companion object {
        /** Sequência própria de uma semente para um assunto ("sons", "lex:mar"…). */
        fun stream(seed: Long, stream: String): Rng = Rng(seed xor fnv1a64(stream) * 31)
    }
}

/** Hash FNV-1a de 64 bits: a semente padrão vem do título do livro. */
fun fnv1a64(text: String): Long {
    var h = -0x340d631b7bdddcdbL // 0xcbf29ce484222325
    for (ch in text) {
        h = h xor ch.code.toLong()
        h *= 0x100000001b3L
    }
    return h
}

/** Semente em texto, como aparece na tela ("K7QM-2XRA"). */
fun seedCode(seed: Long): String {
    val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    var v = seed
    val chars = CharArray(8) { alphabet[((v ushr 0) and 31).toInt()].also { v = v ushr 5 } }
    return chars.concatToString(0, 4) + "-" + chars.concatToString(4, 8)
}
