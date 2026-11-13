package com.cuboide.escriba.core.universe

/*
 * Fatos físicos escritos em português: "olhos azuis", "cabelo ruivo e curto", "pele sardenta", "alto e magro",
 * "canhota", "uma cicatriz no queixo". O texto é dividido em orações (vírgula, ponto, " e ", " mas "…) e cada padrão
 * só olha dentro da sua oração: "olhos azuis e cabelo ruivo" dá olhos = azul e cabelo = ruivo, sem misturar.
 * As palavras vêm de listas fechadas e são normalizadas (azuis → azul, ruiva → ruivo), então "verdes" e "verde"
 * são o mesmo fato. Tudo trabalha no texto dobrado (sem acento, minúsculo), que tem as mesmas posições do original.
 */

/** Categoria de um fato, ligada ao atributo da ficha de personagem com a mesma chave. */
enum class FactKind(val attrKey: String, val label: String) {
    Olhos("olhos", "Olhos"),
    Cabelo("cabelo", "Cabelo"),
    Pele("pele", "Pele"),
    Porte("porte", "Altura e porte"),
    Mao("mao", "Mão"),
    Marca("marca", "Marca"),
}

/** Fato achado no texto: [value] normalizado ("azul"), [start]/[end] no texto original. */
data class Fact(val kind: FactKind, val value: String, val start: Int, val end: Int)

object FactLexicon {
    /** Forma escrita (dobrada) → forma normal. */
    private fun forms(vararg groups: Pair<String, List<String>>): Map<String, String> =
        groups.flatMap { (norm, written) -> (written + norm).map { it to norm } }.toMap()

    val eyeColors = forms(
        "azul" to listOf("azuis"), "verde" to listOf("verdes"), "castanho" to listOf("castanhos", "castanha", "castanhas"),
        "preto" to listOf("pretos", "preta", "pretas"), "negro" to listOf("negros", "negra", "negras"),
        "cinza" to listOf("cinzas", "cinzento", "cinzentos", "cinzenta", "cinzentas", "acinzentados", "acinzentado"),
        "ambar" to listOf(), "mel" to listOf(), "violeta" to listOf("violetas"), "vermelho" to listOf("vermelhos", "vermelha", "vermelhas"),
        "dourado" to listOf("dourados", "dourada", "douradas"), "prateado" to listOf("prateados", "prateada", "prateadas"),
        "escuro" to listOf("escuros", "escura", "escuras"), "claro" to listOf("claros", "clara", "claras"),
    )

    val hairColors = forms(
        "ruivo" to listOf("ruivos", "ruiva", "ruivas"), "loiro" to listOf("loiros", "loira", "loiras", "louro", "louros", "loura", "louras"),
        "castanho" to listOf("castanhos", "castanha", "castanhas"), "preto" to listOf("pretos", "preta", "pretas"),
        "negro" to listOf("negros", "negra", "negras"), "grisalho" to listOf("grisalhos", "grisalha", "grisalhas"),
        "branco" to listOf("brancos", "branca", "brancas"), "prateado" to listOf("prateados", "prateada", "prateadas"),
        "acobreado" to listOf("acobreados", "acobreada", "acobreadas"), "azul" to listOf("azuis"), "verde" to listOf("verdes"),
    )

    /** Textura e tamanho do cabelo: não contradizem a cor ("ruivo" e "curto" convivem). */
    val hairShapes = forms(
        "cacheado" to listOf("cacheados", "cacheada", "cacheadas", "encaracolado", "encaracolados", "encaracolada", "encaracoladas"),
        "liso" to listOf("lisos", "lisa", "lisas"), "ondulado" to listOf("ondulados", "ondulada", "onduladas"),
        "crespo" to listOf("crespos", "crespa", "crespas"), "curto" to listOf("curtos", "curta", "curtas"),
        "longo" to listOf("longos", "longa", "longas", "comprido", "compridos", "comprida", "compridas"),
        "raspado" to listOf("raspados", "raspada", "raspadas"),
    )

    val skin = forms(
        "clara" to listOf("claro"), "escura" to listOf("escuro"), "morena" to listOf("moreno"), "negra" to listOf("negro"),
        "palida" to listOf("palido"), "bronzeada" to listOf("bronzeado"), "sardenta" to listOf("sardento"),
        "alva" to listOf("alvo"), "oliva" to listOf(), "parda" to listOf("pardo"),
    )

    /** Altura (um eixo) e corpo (outro): "alto" contradiz "baixo", mas não "magro". */
    val height = forms("alto" to listOf("alta", "altos", "altas"), "baixo" to listOf("baixa", "baixos", "baixas"))
    val build = forms(
        "magro" to listOf("magra", "magros", "magras", "esguio", "esguia", "franzino", "franzina"),
        "gordo" to listOf("gorda", "gordos", "gordas", "corpulento", "corpulenta", "rechonchudo", "rechonchuda"),
        "forte" to listOf("fortes", "robusto", "robusta", "musculoso", "musculosa", "parrudo", "parruda"),
    )

    val hand = forms("canhoto" to listOf("canhota", "canhotos", "canhotas"), "destro" to listOf("destra", "destros", "destras"), "ambidestro" to listOf("ambidestra"))

    /** Grupo de exclusão de um valor: dois valores do mesmo grupo e diferentes se contradizem. */
    fun group(kind: FactKind, value: String): String = when (kind) {
        FactKind.Cabelo -> if (value in hairShapes.values || value == "careca") "forma" else "cor"
        FactKind.Porte -> if (value in height.values) "altura" else "corpo"
        else -> "valor"
    }
}

private val CLAUSE_BREAK = Regex("[,;.!?:()\\n—–]|\\s(?:e|mas|porem|enquanto|com)\\s")
private val WORD = Regex("[a-z0-9]+")

/** Pedaços do texto (início, fim) separados por pontuação e conjunções. */
private fun clauses(folded: String): List<IntRange> {
    val out = mutableListOf<IntRange>()
    var start = 0
    for (m in CLAUSE_BREAK.findAll(folded)) {
        if (m.range.first > start) out += start until m.range.first
        start = m.range.last + 1
    }
    if (start < folded.length) out += start until folded.length
    return out
}

/**
 * Fatos físicos do texto. [narrative]: texto do livro (mais cauteloso: altura e corpo só com verbo de estado,
 * "era alto", "estava magra", para não confundir com "falou alto"); fora disso (descrição física da ficha) os
 * adjetivos soltos valem.
 */
fun extractFacts(text: String, narrative: Boolean = false): List<Fact> {
    val folded = fold(text)
    val facts = mutableListOf<Fact>()
    for (clause in clauses(folded)) {
        val words = WORD.findAll(folded.substring(clause.first, clause.last + 1)).map { it.value to (clause.first + it.range.first) }.toList()
        fun add(kind: FactKind, value: String, start: Int, end: Int) {
            if (facts.none { it.kind == kind && it.value == value && it.start == start }) facts += Fact(kind, value, start, end)
        }
        words.forEachIndexed { i, (w, at) ->
            when {
                w == "olho" || w == "olhos" -> words.drop(i + 1).take(4).firstOrNull { it.first in FactLexicon.eyeColors }?.let { (c, cAt) ->
                    add(FactKind.Olhos, FactLexicon.eyeColors.getValue(c), at, cAt + c.length)
                }
                w == "cabelo" || w == "cabelos" || w == "cabeleira" || w == "madeixas" || w == "fios" -> {
                    words.drop(i + 1).take(4).forEach { (c, cAt) ->
                        FactLexicon.hairColors[c]?.let { add(FactKind.Cabelo, it, at, cAt + c.length) }
                        FactLexicon.hairShapes[c]?.let { add(FactKind.Cabelo, it, at, cAt + c.length) }
                    }
                }
                w == "careca" || w == "calvo" || w == "calva" -> add(FactKind.Cabelo, "careca", at, at + w.length)
                w == "pele" -> words.drop(i + 1).take(3).firstOrNull { it.first in FactLexicon.skin }?.let { (c, cAt) ->
                    add(FactKind.Pele, FactLexicon.skin.getValue(c), at, cAt + c.length)
                }
                w in FactLexicon.hand -> add(FactKind.Mao, FactLexicon.hand.getValue(w), at, at + w.length)
                w == "cicatriz" || w == "tatuagem" || (w == "sinal" && words.getOrNull(i + 2)?.first == "nascenca") -> {
                    val tail = words.drop(i + 1).take(5).takeWhile { it.first in MARK_WORDS || it.first.length > 2 }
                    val end = tail.lastOrNull()?.let { it.second + it.first.length } ?: (at + w.length)
                    add(FactKind.Marca, folded.substring(at, end), at, end)
                }
                (w in FactLexicon.height || w in FactLexicon.build) -> {
                    val previous = words.getOrNull(i - 1)?.first
                    val beforePrevious = words.getOrNull(i - 2)?.first
                    val stateVerb = previous in STATE_VERBS || (previous in INTENSIFIERS && beforePrevious in STATE_VERBS)
                    if (!narrative || stateVerb) {
                        val norm = FactLexicon.height[w] ?: FactLexicon.build.getValue(w)
                        add(FactKind.Porte, norm, at, at + w.length)
                    }
                }
            }
        }
    }
    return facts.sortedBy { it.start }
}

private val MARK_WORDS = setOf("no", "na", "nos", "nas", "do", "da", "de", "sobre", "acima", "abaixo", "em", "o", "a")
private val STATE_VERBS = setOf("era", "e", "eram", "sao", "foi", "parecia", "pareciam", "estava", "estavam", "ficou", "ficara", "tao", "muito", "bem")
private val INTENSIFIERS = setOf("muito", "bem", "tao", "bastante", "meio", "pouco")

/** Palavras normalizadas do valor de um atributo ("Verdes, puxando para o mel" → {verde, mel}). */
fun attributeTerms(kind: FactKind, value: String): Set<String> {
    val all = extractFacts(fakeSentence(kind, value)).filter { it.kind == kind }.map { it.value }.toSet()
    return all
}

/** Põe o valor solto do atributo numa frase para os mesmos padrões o lerem ("verdes" → "olhos verdes"). */
private fun fakeSentence(kind: FactKind, value: String): String = when (kind) {
    FactKind.Olhos -> "olhos ${value.replace(",", " ")}"
    FactKind.Cabelo -> "cabelo ${value.replace(",", " ")}".replace(" e ", " ")
    FactKind.Pele -> "pele ${value.replace(",", " ")}"
    else -> value
}

/**
 * O fato [fact] contradiz o que já se sabe ([known], valores normalizados da mesma categoria)? Só quando há algo
 * conhecido no mesmo grupo (cor com cor, altura com altura) e o valor novo não está entre eles.
 */
fun contradicts(fact: Fact, known: Set<String>): Boolean {
    val group = FactLexicon.group(fact.kind, fact.value)
    val sameGroup = known.filter { FactLexicon.group(fact.kind, it) == group }
    if (fact.kind == FactKind.Marca) return false // marcas se acumulam: uma cicatriz não nega a outra
    return sameGroup.isNotEmpty() && fact.value !in sameGroup
}
