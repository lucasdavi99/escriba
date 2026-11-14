package com.cuboide.escriba.core.ai

import com.cuboide.escriba.core.universe.fold
import kotlin.math.ln

/** Um trecho do livro que pode entrar no contexto da IA. */
data class ContextPiece(
    val label: String,
    val text: String,
    val ref: AiRef?,
    /** Capítulo ou ficha de onde veio, para não repetir o que já foi referenciado. */
    val sourceKey: String,
)

/** Estimativa de tokens: ~4 caracteres por token em português. */
fun estimateTokens(text: String): Int = (text.length + 3) / 4

private val STOPWORDS = setOf(
    "para", "como", "com", "uma", "uns", "umas", "que", "por", "mais", "mas", "dos", "das", "nos", "nas", "numa", "num",
    "seu", "sua", "seus", "suas", "ele", "ela", "eles", "elas", "isso", "esse", "essa", "este", "esta", "aqui", "ali",
    "ser", "foi", "era", "tem", "tinha", "quando", "onde", "qual", "quais", "sobre", "pelo", "pela", "meu", "minha",
    "voce", "pode", "quero", "preciso", "fazer", "algo", "alguma", "algum", "muito", "cada", "entre", "depois", "antes",
    "ate", "tambem", "ainda", "capitulo", "livro", "historia", "texto",
    // Palavras de pedido ("analise o capítulo e veja se está bom"): não dizem nada sobre o conteúdo procurado.
    "desse", "dessa", "deste", "desta", "nesse", "nessa", "neste", "nesta", "esses", "essas", "estes", "estas",
    "analise", "analisar", "analisa", "veja", "ver", "acha", "acho", "achar", "bom", "boa", "bem", "ficou", "fica",
    "melhore", "melhorar", "melhor", "revise", "revisar", "sugira", "sugerir", "sugestao", "sugestoes", "ideia", "ideias",
    "escreva", "escrever", "continue", "continuar", "favor", "trecho", "trechos", "parte", "partes", "capitulos", "cena",
    "cenas", "poderia", "consegue", "diga", "fale", "explique", "resuma", "resumo", "corrija", "corrigir", "erro", "erros",
    "todo", "toda", "todos", "todas", "inteiro", "inteira", "agora", "nao", "sim", "obrigado", "obrigada", "porque",
    "seria", "deixe", "deixa", "faca", "faz", "dar", "der", "achou", "gostou", "opiniao", "avalie", "avaliar", "confira",
)

/** Quantas palavras diferentes do pedido aparecem em cada trecho (na mesma ordem de [pieces]). */
fun matchedTermCounts(pieces: List<ContextPiece>, query: String): List<Int> {
    val terms = searchTokens(query).toSet()
    if (terms.isEmpty()) return List(pieces.size) { 0 }
    return pieces.map { p -> searchTokens(p.label + " " + p.text).toSet().count { it in terms } }
}

/** Palavras úteis para a busca: sem acento, sem caixa, sem palavras comuns. */
fun searchTokens(text: String): List<String> =
    fold(text).split(Regex("[^\\p{L}\\p{N}]+")).filter { it.length >= 3 && it !in STOPWORDS }

/**
 * Ordena os trechos pela relevância ao pedido (ranking por termos, no estilo BM25). Devolve uma nota por trecho, na
 * mesma ordem de [pieces]; 0 quer dizer que nenhuma palavra do pedido aparece nele.
 */
fun scorePieces(pieces: List<ContextPiece>, query: String): List<Double> {
    val terms = searchTokens(query).distinct()
    if (terms.isEmpty() || pieces.isEmpty()) return List(pieces.size) { 0.0 }
    val docs = pieces.map { p -> searchTokens(p.label + " " + p.text) }
    val avgLen = docs.sumOf { it.size }.toDouble().coerceAtLeast(1.0) / docs.size
    val n = docs.size.toDouble()
    val df = terms.associateWith { t -> docs.count { d -> t in d } }
    val k1 = 1.4
    val b = 0.75
    return docs.map { d ->
        if (d.isEmpty()) return@map 0.0
        val counts = d.groupingBy { it }.eachCount()
        terms.sumOf { t ->
            val f = (counts[t] ?: 0).toDouble()
            if (f == 0.0) 0.0 else {
                val idf = ln(1 + (n - df.getValue(t) + 0.5) / (df.getValue(t) + 0.5))
                idf * (f * (k1 + 1)) / (f + k1 * (1 - b + b * d.size / avgLen))
            }
        }
    }
}

private val CHAPTER_NUMBER = Regex("""(?<![\p{L}])cap(?:itulos?|s?\.)?\s*(\d{1,3})(?![\p{N}])""")
private val DEFAULT_TITLE = Regex("""capitulo \d+""")

/**
 * Capítulos que o pedido cita: pelo número ("capítulo 2", "cap. 3"), pelo tipo ("prólogo", "epílogo") ou pelo
 * título. Assim "analise o capítulo 2" leva o capítulo 2 inteiro, sem o autor precisar apontá-lo com `@`.
 */
fun chaptersNamedIn(query: String, chapters: List<com.cuboide.escriba.core.model.ChapterMeta>): List<String> {
    val q = fold(query)
    val found = LinkedHashSet<String>()
    val byNumber = com.cuboide.escriba.core.model.chapterNumbers(chapters).entries.associate { it.value to it.key }
    CHAPTER_NUMBER.findAll(q).forEach { m -> m.groupValues[1].toIntOrNull()?.let { byNumber[it] }?.let(found::add) }
    fun word(term: String) = Regex("""(?<![\p{L}\p{N}])""" + Regex.escape(term) + """(?![\p{L}\p{N}])""")
    for (ch in chapters) {
        if (ch.kind != com.cuboide.escriba.core.model.ChapterKind.Capitulo && word(fold(ch.kind.label)).containsMatchIn(q)) found += ch.id
        val title = fold(ch.title.trim())
        if (title.length >= 5 && !DEFAULT_TITLE.matches(title) && word(title).containsMatchIn(q)) found += ch.id
    }
    return found.toList()
}
