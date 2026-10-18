package com.cuboide.escriba.core.text

data class FindOptions(
    val matchCase: Boolean = false,
    val wholeWord: Boolean = false,
    val regex: Boolean = false,
)

data class TextMatch(val start: Int, val end: Int)

data class ReplaceResult(val text: String, val count: Int)

/** Constrói o padrão de busca; devolve `null` se a consulta for vazia ou uma regex inválida. */
fun buildPattern(query: String, options: FindOptions): Regex? {
    if (query.isEmpty()) return null
    var source = if (options.regex) query else Regex.escape(query)
    if (options.wholeWord) source = "(?<![\\p{L}\\p{N}_])(?:$source)(?![\\p{L}\\p{N}_])"
    val regexOptions = buildSet {
        if (!options.matchCase) add(RegexOption.IGNORE_CASE)
        if (options.regex) add(RegexOption.MULTILINE)
    }
    return runCatching { Regex(source, regexOptions) }.getOrNull()
}

fun findAll(text: String, query: String, options: FindOptions = FindOptions()): List<TextMatch> {
    val pattern = buildPattern(query, options) ?: return emptyList()
    return pattern.findAll(text)
        .filter { it.range.last >= it.range.first }
        .map { TextMatch(it.range.first, it.range.last + 1) }
        .toList()
}

/** Substitui todas as ocorrências. Em modo regex, `$1`… referenciam grupos; fora dele o texto é literal. */
fun replaceAll(text: String, query: String, replacement: String, options: FindOptions = FindOptions()): ReplaceResult {
    val pattern = buildPattern(query, options) ?: return ReplaceResult(text, 0)
    var count = 0
    val result = pattern.replace(text) { match ->
        if (match.value.isEmpty()) return@replace match.value
        count++
        if (options.regex) expandGroups(replacement, match) else replacement
    }
    return ReplaceResult(result, count)
}

/** Substitui apenas a ocorrência que começa em [match]. */
fun replaceOne(text: String, match: TextMatch, query: String, replacement: String, options: FindOptions): String {
    val value = if (options.regex) {
        val pattern = buildPattern(query, options) ?: return text
        val found = pattern.find(text, match.start)?.takeIf { it.range.first == match.start } ?: return text
        expandGroups(replacement, found)
    } else {
        replacement
    }
    return text.substring(0, match.start) + value + text.substring(match.end)
}

private fun expandGroups(replacement: String, match: MatchResult): String =
    Regex("\\$(\\d)").replace(replacement) { ref ->
        match.groupValues.getOrNull(ref.groupValues[1].toInt()) ?: ""
    }
