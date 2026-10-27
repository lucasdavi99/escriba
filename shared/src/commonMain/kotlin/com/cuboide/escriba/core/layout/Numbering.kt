package com.cuboide.escriba.core.layout

fun toRoman(n: Int): String {
    if (n <= 0 || n >= 4000) return n.toString()
    val table = listOf(1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC", 50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I")
    var rest = n
    return buildString {
        for ((value, symbol) in table) while (rest >= value) { append(symbol); rest -= value }
    }
}

private val UNITS = listOf("zero", "um", "dois", "três", "quatro", "cinco", "seis", "sete", "oito", "nove", "dez",
    "onze", "doze", "treze", "catorze", "quinze", "dezesseis", "dezessete", "dezoito", "dezenove")
private val TENS = listOf("", "", "vinte", "trinta", "quarenta", "cinquenta", "sessenta", "setenta", "oitenta", "noventa")
private val HUNDREDS = listOf("", "cento", "duzentos", "trezentos", "quatrocentos", "quinhentos", "seiscentos", "setecentos", "oitocentos", "novecentos")

/** Número por extenso em português (1–999), para "Capítulo vinte e três". */
fun spellOutPt(n: Int): String {
    if (n < 0 || n > 999) return n.toString()
    if (n < 20) return UNITS[n]
    if (n == 100) return "cem"
    if (n < 100) {
        val t = TENS[n / 10]
        return if (n % 10 == 0) t else "$t e ${UNITS[n % 10]}"
    }
    val h = HUNDREDS[n / 100]
    return if (n % 100 == 0) h else "$h e ${spellOutPt(n % 100)}"
}

fun chapterLabel(style: ChapterNumberStyle, n: Int): String? = when (style) {
    ChapterNumberStyle.Nenhum -> null
    ChapterNumberStyle.Arabico -> "Capítulo $n"
    ChapterNumberStyle.Romano -> "Capítulo ${toRoman(n)}"
    ChapterNumberStyle.Extenso -> "Capítulo ${spellOutPt(n)}"
    ChapterNumberStyle.SoNumero -> "$n"
}
