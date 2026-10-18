package com.cuboide.escriba.core.text

enum class DiffKind { Equal, Inserted, Deleted }

data class DiffSegment(val kind: DiffKind, val text: String)

data class DiffStats(val insertedWords: Int, val deletedWords: Int)

/**
 * Diferença palavra a palavra entre duas versões (LCS). Textos muito grandes caem para
 * comparação por parágrafo para manter memória e tempo previsíveis.
 */
fun diffWords(old: String, new: String, maxCells: Long = 4_000_000): List<DiffSegment> {
    val a = tokenize(old)
    val b = tokenize(new)
    val tokensA: List<String>
    val tokensB: List<String>
    if (a.size.toLong() * b.size.toLong() <= maxCells) {
        tokensA = a; tokensB = b
    } else {
        tokensA = splitKeepingNewlines(old); tokensB = splitKeepingNewlines(new)
        if (tokensA.size.toLong() * tokensB.size.toLong() > maxCells) {
            return listOf(DiffSegment(DiffKind.Deleted, old), DiffSegment(DiffKind.Inserted, new))
        }
    }
    return merge(lcsDiff(tokensA, tokensB))
}

fun diffStats(segments: List<DiffSegment>): DiffStats = DiffStats(
    insertedWords = segments.filter { it.kind == DiffKind.Inserted }.sumOf { countWords(it.text) },
    deletedWords = segments.filter { it.kind == DiffKind.Deleted }.sumOf { countWords(it.text) },
)

/** Palavras e espaços viram tokens separados, para que o texto recomposto seja idêntico ao original. */
private fun tokenize(text: String): List<String> =
    Regex("\\s+|[^\\s]+").findAll(text).map { it.value }.toList()

private fun splitKeepingNewlines(text: String): List<String> =
    Regex("[^\\n]*\\n|[^\\n]+").findAll(text).map { it.value }.toList()

private fun lcsDiff(a: List<String>, b: List<String>): List<DiffSegment> {
    // Prefixo e sufixo comuns saem antes da tabela: o caso típico (pequena edição) fica barato.
    var prefix = 0
    while (prefix < a.size && prefix < b.size && a[prefix] == b[prefix]) prefix++
    var suffix = 0
    while (suffix < a.size - prefix && suffix < b.size - prefix &&
        a[a.size - 1 - suffix] == b[b.size - 1 - suffix]
    ) suffix++

    val midA = a.subList(prefix, a.size - suffix)
    val midB = b.subList(prefix, b.size - suffix)
    val n = midA.size
    val m = midB.size
    val table = Array(n + 1) { IntArray(m + 1) }
    for (i in n - 1 downTo 0) {
        for (j in m - 1 downTo 0) {
            table[i][j] = if (midA[i] == midB[j]) table[i + 1][j + 1] + 1
            else maxOf(table[i + 1][j], table[i][j + 1])
        }
    }

    val out = ArrayList<DiffSegment>()
    a.subList(0, prefix).forEach { out += DiffSegment(DiffKind.Equal, it) }
    var i = 0
    var j = 0
    while (i < n && j < m) {
        when {
            midA[i] == midB[j] -> { out += DiffSegment(DiffKind.Equal, midA[i]); i++; j++ }
            table[i + 1][j] >= table[i][j + 1] -> { out += DiffSegment(DiffKind.Deleted, midA[i]); i++ }
            else -> { out += DiffSegment(DiffKind.Inserted, midB[j]); j++ }
        }
    }
    while (i < n) out += DiffSegment(DiffKind.Deleted, midA[i++])
    while (j < m) out += DiffSegment(DiffKind.Inserted, midB[j++])
    a.subList(a.size - suffix, a.size).forEach { out += DiffSegment(DiffKind.Equal, it) }
    return out
}

private fun merge(segments: List<DiffSegment>): List<DiffSegment> {
    val out = ArrayList<DiffSegment>()
    for (s in segments) {
        val last = out.lastOrNull()
        if (last != null && last.kind == s.kind) out[out.lastIndex] = last.copy(text = last.text + s.text)
        else out += s
    }
    return out
}
