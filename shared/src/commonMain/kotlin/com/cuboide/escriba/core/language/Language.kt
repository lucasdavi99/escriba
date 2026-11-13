package com.cuboide.escriba.core.language

import kotlinx.serialization.Serializable

/**
 * Estilo da língua: o clima dos sons. Os estilos e as listas de sons foram pensados para o Escriba; o sorteio
 * escolhe, dentro do estilo, quais sons, sílabas e regras a língua tem.
 */
@Serializable
enum class LangStyle(val label: String, val description: String) {
    Rio("Rio", "Corre fácil: l, r, m e n, muitas vogais e quase nenhum encontro."),
    Pedra("Pedra", "Bate e range: k, g, t e d, encontros como kr e st, sílabas fechadas."),
    Areia("Areia", "Quente e longa: s, z e h, vogais compridas (aa, ee) e palavras sem pressa."),
    Neve("Neve", "Abafada: f e v, vogais longas e e o, sílabas curtas e frias."),
    Coral("Coral", "Só sílabas abertas, poucas consoantes, sons que se repetem."),
    Oraculo("Oráculo", "Solene: th e kh, vogais longas, palavras compridas e sílabas que ecoam."),
}

private data class StyleProfile(
    val consonants: Map<String, Int>,
    val vowels: Map<String, Int>,
    /** Moldes de sílaba (C = consoante, V = vogal, K = encontro) e pesos. */
    val templates: Map<String, Int>,
    val clusters: List<String>,
    val codas: List<String>,
    val syllables: IntRange,
    val reduplication: Double,
)

private val PROFILES = mapOf(
    LangStyle.Rio to StyleProfile(
        mapOf("l" to 5, "r" to 4, "m" to 4, "n" to 4, "v" to 2, "s" to 2, "nh" to 1, "lh" to 1, "d" to 2, "t" to 1, "th" to 1, "f" to 1),
        mapOf("a" to 5, "e" to 4, "i" to 3, "o" to 2, "u" to 1, "ai" to 1, "ia" to 1, "ei" to 1),
        mapOf("CV" to 7, "V" to 2, "CVC" to 2), emptyList(), listOf("l", "n", "r"), 2..3, 0.05,
    ),
    LangStyle.Pedra to StyleProfile(
        mapOf("k" to 5, "g" to 4, "t" to 4, "d" to 3, "r" to 3, "kh" to 2, "x" to 2, "b" to 2, "z" to 2, "m" to 1, "n" to 2, "s" to 2),
        mapOf("a" to 4, "o" to 4, "u" to 4, "e" to 1, "i" to 1),
        mapOf("CVC" to 6, "KVC" to 2, "CV" to 2, "KV" to 1), listOf("kr", "gr", "tr", "dr", "br", "kl", "gl", "st", "sk"),
        listOf("k", "t", "g", "r", "z", "x", "d"), 1..2, 0.02,
    ),
    LangStyle.Areia to StyleProfile(
        mapOf("s" to 5, "z" to 3, "h" to 3, "r" to 3, "m" to 3, "n" to 3, "d" to 2, "k" to 2, "j" to 2, "x" to 2, "f" to 1, "b" to 1),
        mapOf("aa" to 4, "a" to 4, "i" to 3, "ee" to 2, "u" to 2, "ai" to 2, "e" to 1),
        mapOf("CV" to 5, "CVC" to 4, "V" to 1), emptyList(), listOf("n", "r", "s", "h", "m"), 2..3, 0.06,
    ),
    LangStyle.Neve to StyleProfile(
        mapOf("f" to 4, "v" to 4, "s" to 3, "t" to 3, "k" to 2, "n" to 3, "l" to 2, "r" to 2, "j" to 1, "h" to 2, "m" to 2),
        mapOf("ee" to 3, "oo" to 3, "e" to 3, "o" to 3, "a" to 2, "i" to 2, "u" to 1),
        mapOf("CV" to 4, "CVC" to 4, "KV" to 1), listOf("fr", "fl", "sv", "tv", "kv", "st"), listOf("n", "s", "t", "f", "l"), 1..3, 0.03,
    ),
    LangStyle.Coral to StyleProfile(
        mapOf("m" to 4, "n" to 4, "l" to 3, "k" to 3, "h" to 3, "p" to 3, "t" to 3, "v" to 2, "r" to 2),
        mapOf("a" to 4, "e" to 3, "i" to 3, "o" to 3, "u" to 3),
        mapOf("CV" to 9, "V" to 2), emptyList(), emptyList(), 2..3, 0.25,
    ),
    LangStyle.Oraculo to StyleProfile(
        mapOf("th" to 4, "kh" to 3, "h" to 3, "m" to 3, "n" to 3, "s" to 3, "r" to 3, "l" to 2, "d" to 2, "z" to 2, "b" to 1),
        mapOf("uu" to 3, "ii" to 3, "aa" to 3, "a" to 3, "e" to 2, "o" to 2, "u" to 2, "i" to 2),
        mapOf("CV" to 5, "CVC" to 3, "V" to 1), emptyList(), listOf("n", "m", "s", "th", "r"), 2..3, 0.15,
    ),
)

enum class WordOrder(val label: String) {
    SVO("quem faz · ação · o quê"), SOV("quem faz · o quê · ação"), VSO("ação · quem faz · o quê"),
    VOS("ação · o quê · quem faz"), OSV("o quê · quem faz · ação"),
}

enum class Stress(val label: String) { Primeira("na primeira sílaba"), Penultima("na penúltima sílaba"), Ultima("na última sílaba") }

enum class Direction(val label: String) { Ltr("da esquerda para a direita"), Rtl("da direita para a esquerda"), Ttb("de cima para baixo") }

/** Afixo gramatical: [form] vai antes ([prefix]) ou depois da palavra. */
data class Affix(val form: String, val prefix: Boolean) {
    fun apply(word: String): String = if (prefix) form + word else word + form
    val label: String get() = if (prefix) "$form-" else "-$form"
}

data class Grammar(
    val order: WordOrder,
    val adjectiveAfter: Boolean,
    val stress: Stress,
    val plural: Affix?,
    /** Plural repetindo a primeira sílaba, em vez de afixo. */
    val pluralReduplicates: Boolean,
    val past: Affix,
    val future: Affix,
    val negation: Affix,
    val honorific: Affix,
    val locative: Affix,
    val feminine: String,
    val masculine: String,
    val direction: Direction,
)

/**
 * Língua gerada. Tudo sai de ([seed], [style], [GENERATOR_VERSION]): o mesmo livro dá sempre a mesma língua.
 * [lexicon]: conceito → palavra na forma antiga (proto); as variedades mudam a palavra a partir daí.
 */
class Language(val seed: Long, val style: LangStyle) {
    private val profile = PROFILES.getValue(style)
    val consonants: List<String>
    val vowels: List<String>
    private val consonantWeights: Map<String, Int>
    private val vowelWeights: Map<String, Int>
    val grammar: Grammar
    val name: String
    val code: String = seedCode(seed)
    val changes: SoundChanges
    /** Encontros e finais de sílaba do estilo que só usam sons que esta língua tem. */
    private val clusters: List<String>
    private val codas: List<String>

    init {
        val r = Rng.stream(seed, "sons:$GENERATOR_VERSION:${style.name}")
        // A língua usa a maior parte dos sons do estilo, mas não todos: cada uma fica com o seu jeito.
        val cs = profile.consonants.entries.filter { r.chance(0.82) || it.value >= 4 }.associate { it.key to (it.value + r.nextInt(3)) }
        val vs = profile.vowels.entries.filter { r.chance(0.85) || it.value >= 3 }.associate { it.key to (it.value + r.nextInt(2)) }
        consonantWeights = cs.ifEmpty { profile.consonants }
        vowelWeights = vs.ifEmpty { profile.vowels }
        consonants = consonantWeights.keys.sorted()
        clusters = profile.clusters.filter { cl -> cl.all { ch -> ch.toString() in consonantWeights } }
        codas = profile.codas.filter { it in consonantWeights }
        vowels = vowelWeights.keys.sortedBy { it.length * 10 + "aeiou".indexOf(it.first()) }

        val g = Rng.stream(seed, "gramatica:$GENERATOR_VERSION")
        val usedAffixes = mutableSetOf<String>()
        fun affix(prefixChance: Double): Affix {
            var form: String
            var tries = 0
            do {
                form = syllable(g, coda = g.chance(0.4)).let { if (tries > 5) it + syllable(g, false) else it }
                tries++
            } while (form in usedAffixes && tries < 12)
            usedAffixes += form
            return Affix(form, g.chance(prefixChance))
        }
        val reduplicates = g.chance(0.12 + profile.reduplication)
        grammar = Grammar(
            order = g.weighted(mapOf(WordOrder.SVO to 3, WordOrder.SOV to 3, WordOrder.VSO to 2, WordOrder.VOS to 1, WordOrder.OSV to 1)),
            adjectiveAfter = g.chance(0.55),
            stress = g.pick(Stress.entries),
            plural = if (reduplicates) null else affix(0.2),
            pluralReduplicates = reduplicates,
            past = affix(0.25),
            future = affix(0.25),
            negation = affix(0.5),
            honorific = affix(0.2),
            locative = affix(0.15),
            feminine = g.pick(vowels.filter { it.length == 1 }.ifEmpty { listOf("a") }),
            masculine = syllable(g, coda = true).takeLast(2),
            direction = g.weighted(mapOf(Direction.Ltr to 7, Direction.Rtl to 2, Direction.Ttb to 1)),
        )
        name = word(Rng.stream(seed, "nome-da-lingua"), 2 + Rng.stream(seed, "nome-tamanho").nextInt(2)).replaceFirstChar { it.uppercase() }
        changes = SoundChanges.generate(seed)
    }

    private fun syllable(r: Rng, coda: Boolean): String {
        val c = { r.weighted(consonantWeights) }
        val v = { r.weighted(vowelWeights) }
        return buildString {
            if (r.chance(0.85)) append(c())
            append(v())
            if (coda && codas.isNotEmpty()) append(r.pick(codas))
        }
    }

    /** Palavra com [syllables] sílabas, sorteada pela sequência [r]. */
    fun word(r: Rng, syllables: Int): String {
        val sb = StringBuilder()
        // Uma vogal longa ou ditongo por palavra basta: mais que isso fica pesado de ler.
        val shortVowels = vowelWeights.filterKeys { it.length == 1 }.ifEmpty { vowelWeights }
        var heavy = false
        fun vowel(): String = r.weighted(if (heavy) shortVowels else vowelWeights).also { if (it.length > 1) heavy = true }
        repeat(syllables.coerceAtLeast(1)) { i ->
            val t = r.weighted(profile.templates)
            for (slot in t) {
                when (slot) {
                    'C' -> sb.append(r.weighted(consonantWeights))
                    'K' -> sb.append(if (clusters.isEmpty()) r.weighted(consonantWeights) else r.pick(clusters))
                    'V' -> {
                        // Vogal logo depois de vogal vira a vogal sozinha com uma consoante na frente (sem "ae", "iu" soltos).
                        if (sb.isNotEmpty() && sb.last() in VOWEL_LETTERS) sb.append(r.weighted(consonantWeights))
                        sb.append(vowel())
                    }
                }
            }
            // Coda só no meio ou no fim se o molde pediu; o "C" final de CVC precisa ser coda permitida.
            if (t.endsWith("VC") && codas.isNotEmpty()) {
                val last = sb.takeLastWhile { it !in VOWEL_LETTERS }.toString()
                if (last !in codas) {
                    sb.setLength(sb.length - last.length)
                    sb.append(r.pick(codas))
                }
            } else if (t.endsWith("VC") && codas.isEmpty() && i == syllables - 1) {
                sb.setLength(sb.length - sb.takeLastWhile { it !in VOWEL_LETTERS }.length)
            }
        }
        return tidy(sb.toString())
    }

    /** Palavra do conceito [lemma]: sempre a mesma para esta língua. */
    fun protoWord(lemma: String): String {
        val concept = Concepts.find(lemma)
        val r = Rng.stream(seed, "lex:$GENERATOR_VERSION:$lemma")
        val range = if (concept?.short == true) 1..2 else profile.syllables
        val n = range.first + r.nextInt(range.last - range.first + 1)
        var w = word(r, n)
        if (r.chance(profile.reduplication) && w.length <= 6) w = tidy(firstSyllable(w) + w)
        return w
    }

    /** Encontro de consoantes permitido no começo de sílaba ("kr", "st"). */
    fun allowsCluster(cluster: String): Boolean = cluster in clusters

    /** Consoante que pode fechar sílaba. */
    fun allowsCoda(consonant: String): Boolean = consonant in codas

    /** Vogal mais usada (para desfazer encontros em nomes de fora). */
    val commonVowel: String get() = vowelWeights.filterKeys { it.length == 1 }.maxByOrNull { it.value }?.key ?: vowels.first()

    /** A primeira sílaba (para plural por repetição e reduplicação). */
    fun firstSyllable(word: String): String = syllabify(word).firstOrNull() ?: word

    /** Sílabas da palavra. */
    fun syllabify(word: String): List<String> = syllables(word)

    /** Índice da sílaba tônica. */
    fun stressIndex(word: String): Int {
        val n = syllabify(word).size
        return when (grammar.stress) {
            Stress.Primeira -> 0
            Stress.Penultima -> (n - 2).coerceAtLeast(0)
            Stress.Ultima -> n - 1
        }
    }

    /** Pronúncia com a sílaba tônica em maiúsculas: "lau·TRAAK". */
    fun pronunciation(word: String): String {
        val s = syllabify(word)
        val t = stressIndex(word)
        return s.mapIndexed { i, syl -> if (i == t) syl.uppercase() else syl }.joinToString("·")
    }

    companion object {
        const val GENERATOR_VERSION = 1
        const val VOWEL_LETTERS = "aeiou"
        private val DIGRAPHS = listOf("nh", "lh", "ch", "th", "kh", "sh")

        /** Junta letras repetidas demais e tira sequências difíceis ("kkk" → "k", "aaa" → "aa"). */
        fun tidy(word: String): String {
            var w = word.replace(Regex("([^aeiou])\\1+"), "$1").replace(Regex("([aeiou])\\1{2,}"), "$1$1")
            w = w.replace(Regex("([^aeiou]{2})[^aeiou]+"), "$1") // no máximo duas consoantes seguidas
            w = w.replace(Regex("([aeiou]{2})[aeiou]+"), "$1") // e no máximo duas vogais
            return w
        }

        /** Divide em sílabas: grupos de vogais são o núcleo; dígrafos (nh, th…) não se separam. */
        fun syllables(word: String): List<String> {
            if (word.isEmpty()) return emptyList()
            // Unidades: dígrafo ou letra.
            val units = mutableListOf<String>()
            var i = 0
            while (i < word.length) {
                val two = if (i + 1 < word.length) word.substring(i, i + 2) else ""
                if (two in DIGRAPHS) { units += two; i += 2 } else { units += word[i].toString(); i++ }
            }
            fun isV(u: String) = u.length == 1 && u[0] in VOWEL_LETTERS
            val out = mutableListOf<StringBuilder>()
            var k = 0
            while (k < units.size) {
                val syl = StringBuilder()
                while (k < units.size && !isV(units[k])) { syl.append(units[k]); k++ }
                var vowelsTaken = 0
                while (k < units.size && isV(units[k]) && vowelsTaken < 2) { syl.append(units[k]); k++; vowelsTaken++ }
                // Consoantes até a próxima vogal: com duas ou mais, a primeira fecha esta sílaba.
                var j = k
                while (j < units.size && !isV(units[j])) j++
                val between = j - k
                if (j >= units.size) { while (k < units.size) { syl.append(units[k]); k++ } }
                else if (between >= 2) { syl.append(units[k]); k++ }
                out += syl
            }
            // Sílaba sem vogal (sobra de consoantes) junta com a anterior.
            val merged = mutableListOf<String>()
            for (s in out.map { it.toString() }) {
                if (merged.isNotEmpty() && s.none { it in VOWEL_LETTERS }) merged[merged.size - 1] = merged.last() + s else merged += s
            }
            return merged.filter { it.isNotEmpty() }
        }
    }
}
