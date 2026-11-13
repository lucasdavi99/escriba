package com.cuboide.escriba.core.language

import com.cuboide.escriba.core.universe.fold

/** Palavra traduzida, para o glossário: a palavra, o que quer dizer, como se fala e se foi cunhada agora. */
data class Gloss(val word: String, val meaning: String, val pronunciation: String, val isNew: Boolean, val lemma: String)

data class Translation(val sentence: String, val glosses: List<Gloss>, val newLemmas: Set<String>)

private data class Item(
    val lemma: String,
    val cls: WordClass,
    val meaning: String,
    val plural: Boolean = false,
    val tense: Tense = Tense.Presente,
    var negated: Boolean = false,
    var locative: Boolean = false,
    val proper: Boolean = false,
)

private enum class Tense { Presente, Passado, Futuro }

private val ARTICLES = setOf("o", "a", "os", "as", "um", "uma", "uns", "umas")
private val GENITIVE = setOf("de", "do", "da", "dos", "das")
private val LOCATIVE = setOf("em", "no", "na", "nas", "num", "numa")

/** Verbos irregulares mais comuns: forma → (lema, tempo). */
private val IRREGULAR: Map<String, Pair<String, Tense>> = mapOf(
    "é" to ("ser" to Tense.Presente), "e" to ("ser" to Tense.Presente), "sao" to ("ser" to Tense.Presente), "era" to ("ser" to Tense.Passado),
    "foi" to ("ir" to Tense.Passado), "foram" to ("ir" to Tense.Passado), "sera" to ("ser" to Tense.Futuro),
    "esta" to ("estar" to Tense.Presente), "estava" to ("estar" to Tense.Passado),
    "tem" to ("ter" to Tense.Presente), "tinha" to ("ter" to Tense.Passado), "teve" to ("ter" to Tense.Passado), "tera" to ("ter" to Tense.Futuro),
    "vai" to ("ir" to Tense.Presente), "vao" to ("ir" to Tense.Presente), "ia" to ("ir" to Tense.Passado), "ira" to ("ir" to Tense.Futuro),
    "vem" to ("vir" to Tense.Presente), "veio" to ("vir" to Tense.Passado), "vira" to ("vir" to Tense.Futuro),
    "ve" to ("ver" to Tense.Presente), "viu" to ("ver" to Tense.Passado), "via" to ("ver" to Tense.Passado), "vera" to ("ver" to Tense.Futuro),
    "diz" to ("dizer" to Tense.Presente), "disse" to ("dizer" to Tense.Passado), "dira" to ("dizer" to Tense.Futuro),
    "faz" to ("fazer" to Tense.Presente), "fez" to ("fazer" to Tense.Passado), "fara" to ("fazer" to Tense.Futuro),
    "deu" to ("dar" to Tense.Passado), "dara" to ("dar" to Tense.Futuro), "sabe" to ("saber" to Tense.Presente), "soube" to ("saber" to Tense.Passado),
    "quer" to ("querer" to Tense.Presente), "quis" to ("querer" to Tense.Passado), "pode" to ("poder" to Tense.Presente),
    "morreu" to ("morrer" to Tense.Passado), "nasceu" to ("nascer" to Tense.Passado),
)

private fun known(lemma: String, vararg classes: WordClass) = Concepts.find(lemma)?.cls?.let { it in classes } == true

/** Lê uma palavra em português: lema, classe, plural e tempo (se for verbo). */
private fun analyze(raw: String, first: Boolean): Item? {
    val lower = raw.lowercase()
    val f = fold(raw)
    if (f in ARTICLES) return null
    if (f == "nao") return Item("nao", WordClass.Outro, raw)
    // "é" (verbo) e "e" (conjunção) só se distinguem pelo acento.
    if (lower == "é" || (f != "e" && f in IRREGULAR)) IRREGULAR[if (lower == "é") "é" else f]?.let { (l, t) -> return Item(l, WordClass.Verbo, raw, tense = t) }
    Concepts.find(f)?.let { return Item(f, it.cls, raw) }

    // Nome próprio (maiúscula no meio da frase e desconhecido): vira "nome de fora".
    if (!first && raw.first().isUpperCase()) return Item(raw, WordClass.Nome, raw, proper = true)

    // Verbos regulares: passado e futuro pelas terminações.
    val verbForms = listOf(
        "aram" to ("ar" to Tense.Passado), "eram" to ("er" to Tense.Passado), "iram" to ("ir" to Tense.Passado),
        "avam" to ("ar" to Tense.Passado), "ou" to ("ar" to Tense.Passado), "eu" to ("er" to Tense.Passado), "iu" to ("ir" to Tense.Passado),
        "ava" to ("ar" to Tense.Passado), "ia" to ("er" to Tense.Passado), "ia" to ("ir" to Tense.Passado),
        "arao" to ("ar" to Tense.Futuro), "erao" to ("er" to Tense.Futuro), "irao" to ("ir" to Tense.Futuro),
        "ara" to ("ar" to Tense.Futuro), "era" to ("er" to Tense.Futuro), "ira" to ("ir" to Tense.Futuro),
        "am" to ("ar" to Tense.Presente), "em" to ("er" to Tense.Presente), "em" to ("ir" to Tense.Presente),
        "a" to ("ar" to Tense.Presente), "e" to ("er" to Tense.Presente), "e" to ("ir" to Tense.Presente),
    )
    for ((end, rule) in verbForms) {
        if (f.length > end.length + 1 && f.endsWith(end)) {
            val lemma = f.dropLast(end.length) + rule.first
            if (known(lemma, WordClass.Verbo)) return Item(lemma, WordClass.Verbo, raw, tense = rule.second)
        }
    }
    // Plural e feminino de nomes e qualidades conhecidos.
    val nominal = listOf(
        "oes" to "ao", "aes" to "ao", "ais" to "al", "eis" to "el", "ns" to "m", "es" to "", "s" to "",
    )
    for ((end, rep) in nominal) {
        if (f.length > end.length + 1 && f.endsWith(end)) {
            val base = f.dropLast(end.length) + rep
            Concepts.find(base)?.let { return Item(base, it.cls, raw, plural = true) }
            if (base.endsWith("a") && known(base.dropLast(1) + "o", WordClass.Adjetivo)) return Item(base.dropLast(1) + "o", WordClass.Adjetivo, raw, plural = true)
        }
    }
    if (f.endsWith("a") && known(f.dropLast(1) + "o", WordClass.Adjetivo)) return Item(f.dropLast(1) + "o", WordClass.Adjetivo, raw)
    // Verbo desconhecido com terminação clara de passado: cunha pelo infinitivo.
    for ((end, rule) in verbForms.take(9)) {
        if (f.length > end.length + 2 && f.endsWith(end)) return Item(f.dropLast(end.length) + rule.first, WordClass.Verbo, raw, tense = rule.second)
    }
    // Palavra desconhecida: vira um nome novo (plural pelo s final).
    return if (f.length > 3 && f.endsWith("s")) Item(f.dropLast(1), WordClass.Nome, raw, plural = true) else Item(f, WordClass.Nome, raw)
}

private val HONORED = setOf("rei", "rainha", "principe", "princesa", "deus", "deusa", "mae", "pai", "capitao")

/**
 * Traduz uma frase simples do português. Tira os artigos, acha o verbo, põe a frase na ordem da língua, coloca o
 * adjetivo antes ou depois do nome, aplica plural, tempo, negação e "em" (locativo) e passa a palavra pela
 * [variety]. Palavras que a língua ainda não tem são cunhadas na hora (sempre as mesmas para a mesma língua).
 */
fun translate(lang: Language, variety: Variety, text: String, known: Map<String, String> = emptyMap()): Translation {
    val tokens = Regex("""[\p{L}]+(?:-[\p{L}]+)*|[.!?…,;:]""").findAll(text).map { it.value }.toList()
    val ending = tokens.lastOrNull()?.takeIf { it in setOf(".", "!", "?", "…") } ?: "."
    // Orações separadas por vírgula ou por "e", "mas", "ou": cada uma vai para a ordem da língua sozinha.
    val clauses = mutableListOf<Pair<List<String>, String?>>()
    var current = mutableListOf<String>()
    for (t in tokens) {
        val f = fold(t)
        when {
            t in setOf(",", ";", ":") -> { if (current.isNotEmpty()) clauses += current to ","; current = mutableListOf() }
            t in setOf(".", "!", "?", "…") -> {}
            t != "é" && f in setOf("e", "mas", "ou") -> {
                // ", e Helena…": a conjunção toma o lugar da vírgula que já fechou a oração anterior.
                if (current.isNotEmpty()) clauses += current to f
                else if (clauses.isNotEmpty()) clauses[clauses.size - 1] = clauses.last().first to f
                current = mutableListOf()
            }
            else -> current += t
        }
    }
    if (current.isNotEmpty()) clauses += current to null

    val glosses = mutableListOf<Gloss>()
    val newLemmas = mutableSetOf<String>()
    val sentence = StringBuilder()
    clauses.forEachIndexed { ci, (words, sep) ->
        val clause = translateClause(lang, variety, words, ci == 0, known)
        glosses += clause
        clause.filter { it.isNew }.forEach { newLemmas += it.lemma }
        if (sentence.isNotEmpty() && sentence.last() != ' ') sentence.append(' ')
        sentence.append(clause.joinToString(" ") { it.word })
        when (sep) {
            null -> {}
            "," -> sentence.append(",")
            else -> {
                val conj = Language.tidy(variety.form(known[sep] ?: lang.protoWord(sep), lang))
                glosses += Gloss(conj, sep, lang.pronunciation(conj), false, sep)
                sentence.append(' ').append(conj)
            }
        }
    }
    val text2 = sentence.toString().trim().replaceFirstChar { it.uppercase() }
    return Translation(if (text2.isEmpty()) text2 else text2 + ending, glosses, newLemmas)
}

private fun translateClause(lang: Language, variety: Variety, tokens: List<String>, sentenceStart: Boolean, known: Map<String, String>): List<Gloss> {
    val items = mutableListOf<Item>()
    var negateNext = false
    var locativeNext = false
    tokens.forEachIndexed { i, t ->
        val f = fold(t)
        when {
            f in GENITIVE -> {}
            f in LOCATIVE -> locativeNext = true
            else -> {
                val item = analyze(t, sentenceStart && i == 0) ?: return@forEachIndexed
                if (item.lemma == "nao") { negateNext = true; return@forEachIndexed }
                if (item.cls == WordClass.Verbo && negateNext) { item.negated = true; negateNext = false }
                if (item.cls == WordClass.Nome && locativeNext) { item.locative = true; locativeNext = false }
                items += item
            }
        }
    }
    // Adjetivo: em português vem depois do nome; a língua pode querer antes.
    if (!lang.grammar.adjectiveAfter) {
        var k = 0
        while (k < items.size - 1) {
            if (items[k].cls == WordClass.Nome && items[k + 1].cls == WordClass.Adjetivo) { items.add(k, items.removeAt(k + 1)); k += 2 } else k++
        }
    }
    // Ordem da frase a partir do primeiro verbo.
    val v = items.indexOfFirst { it.cls == WordClass.Verbo }
    val ordered = if (v < 0) items else {
        val s = items.subList(0, v).toList()
        val verb = listOf(items[v])
        val o = items.subList(v + 1, items.size).toList()
        when (lang.grammar.order) {
            WordOrder.SVO -> s + verb + o
            WordOrder.SOV -> s + o + verb
            WordOrder.VSO -> verb + s + o
            WordOrder.VOS -> verb + o + s
            WordOrder.OSV -> o + s + verb
        }
    }

    val glosses = mutableListOf<Gloss>()
    for (item in ordered) {
        val isNew = item.proper.not() && Concepts.find(item.lemma) == null
        var w = when {
            item.proper -> adaptForeignName(lang, item.lemma)
            else -> known[item.lemma] ?: lang.protoWord(item.lemma)
        }
        if (!item.proper) w = variety.form(w, lang)
        val g = lang.grammar
        if (item.plural) w = if (g.pluralReduplicates || g.plural == null) lang.firstSyllable(w) + w else g.plural.apply(w)
        if (item.cls == WordClass.Verbo) {
            when (item.tense) {
                Tense.Passado -> w = g.past.apply(w)
                Tense.Futuro -> w = g.future.apply(w)
                Tense.Presente -> {}
            }
            if (item.negated) w = g.negation.apply(w)
        }
        if (item.locative) w = g.locative.apply(w)
        if (variety.honorific && (item.cls == WordClass.Pronome || item.lemma in HONORED)) w = g.honorific.apply(w)
        w = Language.tidy(w)
        val meaning = buildString {
            append(if (item.proper) item.meaning else displayLemma(item.lemma))
            if (item.plural) append(" (plural)")
            if (item.tense == Tense.Passado) append(" (passado)")
            if (item.tense == Tense.Futuro) append(" (futuro)")
            if (item.negated) append(" (não)")
            if (item.locative) append(" (em)")
        }
        glosses += Gloss(w, meaning, lang.pronunciation(w), isNew, item.lemma)
    }
    return glosses
}

// ---------- nomes ----------

/** Nome inventado com o que ele quer dizer. [feminine]: `null` para lugares. */
data class MeaningfulName(val name: String, val meaning: String, val pronunciation: String, val feminine: Boolean?)

/** Raiz de um conceito para compor nomes: as primeiras sílabas da palavra. */
private fun root(lang: Language, lemma: String, syllables: Int): String =
    lang.syllabify(lang.protoWord(lemma)).take(syllables).joinToString("")

/** Lote [batch] de nomes de pessoas (alternando gênero) ou de lugares. "Outros nomes" pede o lote seguinte. */
fun meaningfulNames(lang: Language, batch: Int, places: Boolean, count: Int = 8): List<MeaningfulName> {
    val r = Rng.stream(lang.seed, "nomes:${if (places) "lugar" else "pessoa"}:$batch")
    val out = mutableListOf<MeaningfulName>()
    var guard = 0
    while (out.size < count && guard++ < count * 6) {
        // "lobo de sombra", "flor do mar": dois nomes, sem adjetivo (que pediria concordância).
        val nouns = Concepts.forNames.filter { Concepts.find(it)?.cls == WordClass.Nome }
        val a = r.pick(nouns)
        val b = r.pick(nouns.filter { it != a })
        val bAdj = false
        var w = root(lang, a, 1 + r.nextInt(2)) + root(lang, b, 1)
        val feminine = if (places) null else out.size % 2 == 0
        when (feminine) {
            true -> w += lang.grammar.feminine
            false -> w += lang.grammar.masculine.takeIf { it.isNotBlank() && r.chance(0.6) }.orEmpty()
            null -> if (r.chance(0.5)) w = lang.grammar.locative.apply(w)
        }
        w = Language.tidy(w)
        if (w.length < 3 || w.none { it !in Language.VOWEL_LETTERS } || out.any { it.name.equals(w, true) }) continue
        val meaning = when {
            places && bAdj -> "${displayLemma(a)} ${displayLemma(b)}"
            places -> "lugar de ${displayLemma(a)} e ${displayLemma(b)}"
            bAdj -> "${displayLemma(a)} ${displayLemma(b)}"
            else -> "${displayLemma(a)} de ${displayLemma(b)}"
        }
        out += MeaningfulName(w.replaceFirstChar { it.uppercase() }, meaning, lang.pronunciation(w), feminine)
    }
    return out
}

// ---------- nome de fora ----------

private val SOURCE_DIGRAPHS = listOf("ch" to "x", "sh" to "x", "lh" to "lh", "nh" to "nh", "ph" to "f", "th" to "th", "qu" to "k", "gu" to "g", "ss" to "s", "rr" to "r", "ll" to "l")
private val CONSONANT_CLASSES = listOf(
    listOf("p", "b", "m", "f", "v"), listOf("t", "d", "n", "s", "z", "l", "r", "th"), listOf("k", "g", "h", "kh", "x"),
    listOf("ch", "j", "nh", "lh", "x", "s"),
)
private val VOWEL_NEAR = mapOf('a' to "aeo", 'e' to "eia", 'i' to "iea", 'o' to "oua", 'u' to "uoa")

/** Como este povo diz um nome estrangeiro: cada som vira o mais parecido que a língua tem. */
fun adaptForeignName(lang: Language, foreign: String): String {
    var s = fold(foreign).filter { it.isLetter() }
    if (s.isEmpty()) return ""
    // Grafia → sons.
    val units = mutableListOf<String>()
    var i = 0
    if (s.startsWith("h")) s = s.drop(1)
    while (i < s.length) {
        val two = s.substring(i, minOf(i + 2, s.length))
        val dig = SOURCE_DIGRAPHS.firstOrNull { it.first == two }
        if (dig != null) { units += dig.second; i += 2; continue }
        val ch = s[i]
        val next = s.getOrNull(i + 1)
        units += when (ch) {
            'c' -> if (next == 'e' || next == 'i') "s" else "k"
            'g' -> if (next == 'e' || next == 'i') "j" else "g"
            'q' -> "k"
            'y' -> "i"
            'w' -> "v"
            'x' -> "x"
            else -> ch.toString()
        }
        i++
    }
    fun isVowel(u: String) = u.length == 1 && u[0] in Language.VOWEL_LETTERS
    val singleVowels = lang.vowels.filter { it.length == 1 }
    val mapped = units.map { u ->
        when {
            isVowel(u) -> {
                if (u in singleVowels) u
                else VOWEL_NEAR[u[0]]?.map { it.toString() }?.firstOrNull { it in singleVowels }
                    ?: lang.vowels.firstOrNull { it.startsWith(u) } ?: lang.commonVowel
            }
            u in lang.consonants -> u
            else -> CONSONANT_CLASSES.filter { u in it }.flatten().firstOrNull { it in lang.consonants } ?: lang.consonants.first()
        }
    }
    // Encontros e finais que a língua não aceita ganham uma vogal.
    val out = StringBuilder()
    for ((k, u) in mapped.withIndex()) {
        val prevConsonant = out.isNotEmpty() && out.last() !in Language.VOWEL_LETTERS
        val nextIsVowel = mapped.getOrNull(k + 1)?.let(::isVowel) == true
        if (!isVowel(u) && prevConsonant) {
            val prev = out.takeLastWhile { it !in Language.VOWEL_LETTERS }.toString()
            val clusterOk = lang.allowsCluster(prev + u) && nextIsVowel
            // Só fecha sílaba se já houver uma vogal antes (no começo do nome, encontro estranho ganha vogal).
            val codaOk = out.any { it in Language.VOWEL_LETTERS } && lang.allowsCoda(prev)
            if (!clusterOk && !codaOk) out.append(lang.commonVowel)
        }
        out.append(u)
    }
    val last = out.takeLastWhile { it !in Language.VOWEL_LETTERS }.toString()
    if (last.isNotEmpty() && !lang.allowsCoda(last)) out.append(lang.commonVowel)
    return Language.tidy(out.toString()).replaceFirstChar { it.uppercase() }
}
