package com.cuboide.escriba.core.text

/**
 * Conta palavras como um leitor contaria: sequências de letras/dígitos, mantendo juntas
 * palavras com hífen ou apóstrofo ("guarda-chuva", "d'água") e ignorando pontuação solta.
 */
fun countWords(text: String): Int {
    var count = 0
    var inWord = false
    for (i in text.indices) {
        val c = text[i]
        val isWordChar = c.isLetterOrDigit() ||
            (inWord && (c == '-' || c == '\'' || c == '’') && i + 1 < text.length && text[i + 1].isLetterOrDigit())
        if (isWordChar && !inWord) count++
        inWord = isWordChar
    }
    return count
}

fun countCharacters(text: String, includeSpaces: Boolean = true): Int =
    if (includeSpaces) text.length else text.count { !it.isWhitespace() }

fun countParagraphs(text: String): Int = text.split('\n').count { it.isNotBlank() }

/** Minutos estimados de leitura a [wordsPerMinute] palavras por minuto (mínimo 1 se houver texto). */
fun readingMinutes(words: Int, wordsPerMinute: Int = 220): Int =
    if (words == 0) 0 else maxOf(1, (words + wordsPerMinute - 1) / wordsPerMinute)
