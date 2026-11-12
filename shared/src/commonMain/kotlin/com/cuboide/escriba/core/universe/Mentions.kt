package com.cuboide.escriba.core.universe

import com.cuboide.escriba.core.model.UniverseEntry
import com.cuboide.escriba.core.text.TextMatch
import com.cuboide.escriba.core.text.stripMarkup

/**
 * Minúsculas sem acento, um caractere por caractere: as posições do texto dobrado são as mesmas do original,
 * então os achados valem direto no texto do capítulo.
 */
fun fold(text: String): String {
    val out = CharArray(text.length)
    for (i in text.indices) out[i] = foldChar(text[i])
    return out.concatToString()
}

private const val ACCENTED = "áàâãäåéèêëíìîïóòôõöúùûüçñýÿ"
private const val PLAIN = "aaaaaaeeeeiiiiooooouuuucnyy"

private fun foldChar(c: Char): Char {
    val lower = c.lowercaseChar()
    val i = ACCENTED.indexOf(lower)
    return if (i >= 0) PLAIN[i] else lower
}

/** Nome e apelidos da ficha usados para procurá-la no texto (sem repetições, com pelo menos 2 letras). */
fun UniverseEntry.searchTerms(): List<String> =
    (listOf(name.trim()) + aliases).filter { it.length >= 2 }.distinctBy { fold(it) }

/**
 * Onde cada termo aparece em [text], como palavra inteira, sem diferenciar maiúsculas nem acentos. Os termos são
 * tentados do mais longo para o mais curto, então "Ana Clara" não conta também como "Ana".
 */
fun findMentions(text: String, terms: List<String>): List<TextMatch> {
    val usable = terms.map { fold(it.trim()) }.filter { it.length >= 2 }.distinct().sortedByDescending { it.length }
    if (usable.isEmpty() || text.isEmpty()) return emptyList()
    val alternation = usable.joinToString("|") { Regex.escape(it) }
    val pattern = Regex("(?<![\\p{L}\\p{N}_])(?:$alternation)(?![\\p{L}\\p{N}_])")
    return pattern.findAll(fold(text)).map { TextMatch(it.range.first, it.range.last + 1) }.toList()
}

/** Citações da ficha num capítulo. */
data class ChapterMentions(val chapterId: String, val title: String, val matches: List<TextMatch>)

/** Resultado do painel "No manuscrito" de uma ficha. */
data class EntryMentions(val terms: List<String>, val chapters: List<ChapterMentions>, val totalChapters: Int) {
    val citedIn: Int get() = chapters.size
}

/**
 * Guarda as citações por capítulo; só relê um capítulo quando a versão dele ([version], ex.: `modifiedAt`) muda.
 * O capítulo aberto no editor entra com a versão do texto em edição.
 */
class MentionIndex {
    private data class Key(val chapterId: String, val version: Long, val terms: List<String>)
    private val cache = HashMap<Key, List<TextMatch>>()

    fun mentions(
        entry: UniverseEntry,
        chapters: List<Triple<String, String, Long>>,
        body: (String) -> String,
    ): EntryMentions {
        val terms = entry.searchTerms()
        if (terms.isEmpty()) return EntryMentions(terms, emptyList(), chapters.size)
        val found = chapters.mapNotNull { (id, title, version) ->
            val key = Key(id, version, terms)
            val matches = cache.getOrPut(key) { findMentions(body(id), terms) }
            if (matches.isEmpty()) null else ChapterMentions(id, title, matches)
        }
        if (cache.size > 2000) cache.clear()
        return EntryMentions(terms, found, chapters.size)
    }

    fun clear() = cache.clear()
}

/** A ficha passa no filtro da lista: nome, apelidos, etiquetas, subtítulo, atributos e texto dos blocos. */
fun UniverseEntry.matchesFilter(query: String): Boolean {
    val q = fold(query.trim())
    if (q.isEmpty()) return true
    val haystack = buildList {
        add(name); add(subtitle); add(summary); add(notes)
        addAll(tags); addAll(aliases)
        attributes.forEach { add(it.value) }
        blocks.forEach { add(it.text) }
    }
    return haystack.any { fold(it).contains(q) }
}

/** Uma linha que resume a ficha (para cards, cartões do Enredo e listas). */
fun UniverseEntry.digest(): String {
    if (summary.isNotBlank()) return summary.trim()
    val first = schema.blocks.firstNotNullOfOrNull { spec -> blockText(spec.key).takeIf { it.isNotBlank() } }
        ?: blocks.firstOrNull { it.text.isNotBlank() }?.text
        ?: return subtitle.trim()
    return stripMarkup(first).lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()
}
