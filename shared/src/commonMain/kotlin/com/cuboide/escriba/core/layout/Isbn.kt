package com.cuboide.escriba.core.layout

/** Leitura do ISBN para o código de barras da contracapa. */
sealed interface IsbnCheck {
    data object Empty : IsbnCheck
    /** [ean] tem os 13 dígitos do código de barras (ISBN-10 já convertido para 978…). */
    data class Valid(val ean: String) : IsbnCheck
    data class Invalid(val reason: String) : IsbnCheck
}

/** Dígito verificador do EAN-13 / ISBN-13: pesos 1 e 3 alternados, completando a dezena. */
fun ean13CheckDigit(first12: String): Int {
    require(first12.length == 12 && first12.all { it in '0'..'9' }) { "esperava 12 dígitos" }
    val sum = first12.mapIndexed { i, c -> (c - '0') * if (i % 2 == 0) 1 else 3 }.sum()
    return (10 - sum % 10) % 10
}

/** Dígito verificador do ISBN-10 (0–9 ou X), pesos 10 a 2, módulo 11. */
fun isbn10CheckDigit(first9: String): Char {
    require(first9.length == 9 && first9.all { it in '0'..'9' }) { "esperava 9 dígitos" }
    val sum = first9.mapIndexed { i, c -> (c - '0') * (10 - i) }.sum()
    val d = (11 - sum % 11) % 11
    return if (d == 10) 'X' else '0' + d
}

fun checkIsbn(raw: String): IsbnCheck {
    val text = raw.trim().removePrefix("ISBN").removePrefix("isbn").trim().removePrefix(":").trim()
    if (text.isEmpty()) return IsbnCheck.Empty
    if (text.any { !(it.isDigit() || it == '-' || it == ' ' || it == 'X' || it == 'x') }) {
        return IsbnCheck.Invalid("O ISBN só pode ter números, hífens e, no formato antigo, um X no fim.")
    }
    val code = text.filter { it != '-' && it != ' ' }.uppercase()
    if (code.dropLast(1).contains('X')) return IsbnCheck.Invalid("O X só pode aparecer como último dígito do ISBN-10.")
    return when (code.length) {
        13 -> {
            if (code.contains('X')) return IsbnCheck.Invalid("O ISBN de 13 dígitos não tem X.")
            if (!code.startsWith("978") && !code.startsWith("979")) return IsbnCheck.Invalid("O ISBN de 13 dígitos começa com 978 ou 979.")
            val expected = ean13CheckDigit(code.take(12))
            if (code.last() - '0' != expected) IsbnCheck.Invalid("Dígito verificador errado: o último dígito deveria ser $expected.")
            else IsbnCheck.Valid(code)
        }
        10 -> {
            val expected = isbn10CheckDigit(code.take(9))
            if (code.last() != expected) IsbnCheck.Invalid("Dígito verificador errado: o último dígito deveria ser $expected.")
            else {
                val first12 = "978" + code.take(9)
                IsbnCheck.Valid(first12 + ean13CheckDigit(first12))
            }
        }
        else -> IsbnCheck.Invalid("O ISBN deve ter 13 dígitos (ou 10, no formato antigo); este tem ${code.length}.")
    }
}

// Codificação EAN-13: cada dígito ocupa 7 módulos. Os da metade esquerda usam o conjunto L ou G conforme o
// primeiro dígito (que não vira barra); os da direita usam o conjunto R.
private val EAN_L = listOf("0001101", "0011001", "0010011", "0111101", "0100011", "0110001", "0101111", "0111011", "0110111", "0001011")
private val EAN_G = listOf("0100111", "0110011", "0011011", "0100001", "0011101", "0111001", "0000101", "0010001", "0001001", "0010111")
private val EAN_R = listOf("1110010", "1100110", "1101100", "1000010", "1011100", "1001110", "1010000", "1000100", "1001000", "1110100")
private val EAN_PARITY = listOf("LLLLLL", "LLGLGG", "LLGGLG", "LLGGGL", "LGLLGG", "LGGLLG", "LGGGLL", "LGLGLG", "LGLGGL", "LGGLGL")

const val EAN13_MODULES = 95

/** Os 95 módulos do código (true = barra), com as guardas de início, meio e fim. */
fun ean13Modules(code: String): BooleanArray {
    require(code.length == 13 && code.all { it in '0'..'9' }) { "esperava 13 dígitos" }
    val d = code.map { it - '0' }
    val parity = EAN_PARITY[d[0]]
    val bits = buildString {
        append("101")
        for (i in 1..6) append(if (parity[i - 1] == 'L') EAN_L[d[i]] else EAN_G[d[i]])
        append("01010")
        for (i in 7..12) append(EAN_R[d[i]])
        append("101")
    }
    return BooleanArray(bits.length) { bits[it] == '1' }
}

/** Módulos das guardas (barras mais compridas que descem entre os números). */
fun ean13IsGuard(module: Int): Boolean = module < 3 || module in 45..49 || module >= 92
