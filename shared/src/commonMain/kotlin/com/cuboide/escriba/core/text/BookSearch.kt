package com.cuboide.escriba.core.text

data class SearchHit(
    val chapterId: String,
    val chapterTitle: String,
    val start: Int,
    val end: Int,
    val snippet: String,
    val snippetMatchStart: Int,
    val snippetMatchEnd: Int,
)

/**
 * Busca em todos os capítulos do livro, devolvendo trechos com contexto. A busca e os trechos usam o texto
 * que o leitor vê (sem `**`, `[^1]`…), e [SearchHit.start]/[SearchHit.end] apontam para o texto guardado.
 */
fun searchBook(
    chapters: List<Triple<String, String, String>>, // id, título, texto
    query: String,
    options: FindOptions = FindOptions(),
    context: Int = 40,
    limit: Int = 500,
): List<SearchHit> {
    val hits = ArrayList<SearchHit>()
    for ((id, title, raw) in chapters) {
        val visible = visibleText(raw)
        val body = visible.text
        for (m in findAll(body, query, options)) {
            val from = maxOf(0, m.start - context)
            val to = minOf(body.length, m.end + context)
            val prefix = if (from > 0) "…" else ""
            val suffix = if (to < body.length) "…" else ""
            val snippet = prefix + body.substring(from, to).replace('\n', ' ') + suffix
            hits += SearchHit(
                chapterId = id,
                chapterTitle = title,
                start = visible.rawIndex[m.start],
                end = visible.rawEnd(m.end),
                snippet = snippet,
                snippetMatchStart = prefix.length + (m.start - from),
                snippetMatchEnd = prefix.length + (m.end - from),
            )
            if (hits.size >= limit) return hits
        }
    }
    return hits
}
