package com.cuboide.escriba.core.universe

import com.cuboide.escriba.core.model.EntryKind
import com.cuboide.escriba.core.model.UniverseEntry

/** Texto de um capítulo para conferir (o capítulo aberto entra com o texto do editor). */
data class ChapterText(val id: String, val title: String, val text: String)

/** Trecho do livro. */
data class TextSpot(val chapterId: String, val start: Int, val end: Int)

/**
 * O texto diz [said] sobre [kind] de [entryName] em [spot], mas a ficha selada diz [expected] ([source] = null) ou
 * o próprio texto já tinha dito [expected] em [source] ("onde foi dito").
 */
data class Contradiction(
    val entryId: String,
    val entryName: String,
    val kind: FactKind,
    val spot: TextSpot,
    val said: String,
    val expected: String,
    val source: TextSpot?,
) {
    val againstSeal: Boolean get() = source == null
    val message: String
        get() = if (againstSeal) "${kind.label} de $entryName: aqui diz “$said”, a ficha diz “$expected”."
        else "${kind.label} de $entryName: aqui diz “$said”, antes o texto disse “$expected”."
}

/** Problema entre fichas: o mesmo nome em Aliados e Inimigos, ou relação que não bate dos dois lados. */
data class RelationIssue(val entryId: String, val message: String)

/** Idade citada perto de um nome ("tinha 34 anos"): só para conferir, nunca acusada (personagem envelhece). */
data class AgeMention(val entryId: String, val entryName: String, val age: Int, val spot: TextSpot)

data class BookCheck(
    val contradictions: List<Contradiction>,
    val relations: List<RelationIssue>,
    val ages: List<AgeMention>,
    /** Quantos fatos selados existem e em quantas fichas de personagem. */
    val sealedFacts: Int,
    val sealedEntries: Int,
    val characters: Int,
    /** Fichas cuja descrição física já diz coisas que ainda não estão seladas. */
    val unsealed: List<UniverseEntry>,
)

private val SENTENCE_END = Regex("[.!?…]+[\"”’)]*\\s+|\\n+")
/** Frase sem nome que fala de "ele/ela" segue falando de quem foi citado por último. */
private val PRONOUN = Regex("\\b(ele|ela|seu|sua|seus|suas|dele|dela)\\b")
private val NEGATION = setOf("nao", "nem", "sem", "nunca", "jamais")
private val NUMBER_WORDS = mapOf(
    "um" to 1, "dois" to 2, "tres" to 3, "quatro" to 4, "cinco" to 5, "seis" to 6, "sete" to 7, "oito" to 8, "nove" to 9, "dez" to 10,
    "onze" to 11, "doze" to 12, "treze" to 13, "quatorze" to 14, "catorze" to 14, "quinze" to 15, "dezesseis" to 16, "dezessete" to 17,
    "dezoito" to 18, "dezenove" to 19, "vinte" to 20, "trinta" to 30, "quarenta" to 40, "cinquenta" to 50, "sessenta" to 60,
    "setenta" to 70, "oitenta" to 80, "noventa" to 90, "cem" to 100,
)
private val AGE = Regex("\\b(\\d{1,3}|(?:${NUMBER_WORDS.keys.joinToString("|")})(?: e (?:um|dois|tres|quatro|cinco|seis|sete|oito|nove))?) anos\\b")

private fun sentences(folded: String): List<IntRange> {
    val out = mutableListOf<IntRange>()
    var start = 0
    for (m in SENTENCE_END.findAll(folded)) {
        if (m.range.first > start) out += start until m.range.first + 1
        start = m.range.last + 1
    }
    if (start < folded.length) out += start until folded.length
    return out
}

private fun ageValue(text: String): Int? {
    text.toIntOrNull()?.let { return it }
    return text.split(" e ").sumOf { NUMBER_WORDS[it.trim()] ?: return null }
}

/** Valor do fato como o leitor o vê nos valores selados da ficha (para a mensagem). */
private fun UniverseEntry.sealedValue(kind: FactKind): String? =
    attributes.firstOrNull { it.key == kind.attrKey && it.sealed && it.value.isNotBlank() }?.value

/** Fatos que a ficha selou, já normalizados. */
fun UniverseEntry.sealedTerms(kind: FactKind): Set<String> = sealedValue(kind)?.let { attributeTerms(kind, it) }.orEmpty()

/** Confere o livro inteiro. Os capítulos vêm na ordem do livro: "antes" e "depois" seguem essa ordem. */
fun checkBook(entries: List<UniverseEntry>, chapters: List<ChapterText>): BookCheck {
    val characters = entries.filter { it.kind == EntryKind.Personagem && it.name.isNotBlank() }
    val contradictions = mutableListOf<Contradiction>()
    val ages = mutableListOf<AgeMention>()
    // Primeira vez que o texto disse algo de uma categoria (e grupo) não selada: (ficha, categoria, grupo) → fato.
    val firstSaid = HashMap<Triple<String, FactKind, String>, Pair<Fact, TextSpot>>()

    for (ch in chapters) {
        val folded = fold(ch.text)
        val mentions = characters.flatMap { e -> findMentions(ch.text, e.searchTerms()).map { e to it } }.sortedBy { it.second.start }
        if (mentions.isEmpty()) continue
        var lastSubject: UniverseEntry? = null
        for (sentence in sentences(folded)) {
            val inSentence = mentions.filter { it.second.start in sentence }
            val piece = ch.text.substring(sentence.first, sentence.last + 1)
            val pronoun = PRONOUN.containsMatchIn(folded.substring(sentence.first, sentence.last + 1))
            fun ownerAt(pos: Int): UniverseEntry? =
                inSentence.minByOrNull { kotlin.math.abs(it.second.start - pos) }?.first ?: if (pronoun) lastSubject else null

            for (raw in extractFacts(piece, narrative = true)) {
                val fact = raw.copy(start = raw.start + sentence.first, end = raw.end + sentence.first)
                val before = Regex("[a-z]+").findAll(folded.substring(sentence.first, fact.start)).map { it.value }.toList().takeLast(3)
                if (before.any { it in NEGATION }) continue
                val owner = ownerAt(fact.start) ?: continue
                val spot = TextSpot(ch.id, fact.start, fact.end)
                val sealed = owner.sealedTerms(fact.kind)
                if (sealed.isNotEmpty()) {
                    if (contradicts(fact, sealed)) {
                        contradictions += Contradiction(owner.id, owner.name, fact.kind, spot, fact.value, owner.sealedValue(fact.kind)!!, null)
                    }
                    continue
                }
                val key = Triple(owner.id, fact.kind, FactLexicon.group(fact.kind, fact.value))
                val first = firstSaid[key]
                if (first == null) firstSaid[key] = fact to spot
                else if (contradicts(fact, setOf(first.first.value))) {
                    contradictions += Contradiction(owner.id, owner.name, fact.kind, spot, fact.value, first.first.value, first.second)
                }
            }
            for (m in AGE.findAll(folded, sentence.first)) {
                if (m.range.first > sentence.last) break
                val owner = ownerAt(m.range.first) ?: continue
                val age = ageValue(m.groupValues[1]) ?: continue
                ages += AgeMention(owner.id, owner.name, age, TextSpot(ch.id, m.range.first, m.range.last + 1))
            }
            inSentence.lastOrNull()?.let { lastSubject = it.first }
        }
    }

    val sealedCounts = characters.map { e -> e.attributes.count { it.sealed && it.value.isNotBlank() } }
    return BookCheck(
        contradictions = contradictions,
        relations = relationIssues(entries),
        ages = ages,
        sealedFacts = sealedCounts.sum(),
        sealedEntries = sealedCounts.count { it > 0 },
        characters = characters.size,
        unsealed = characters.filter { e -> sealSuggestions(e).isNotEmpty() },
    )
}

// ---------- aliados e inimigos ----------

private fun UniverseEntry.listBlock(key: String): List<String> =
    blockText(key).lines().map { it.trim().removePrefix("- ").removePrefix("* ").trim() }.filter { it.isNotEmpty() }

/** Ficha com esse nome ou apelido (sem acento), se houver uma só. */
private fun resolve(name: String, entries: List<UniverseEntry>): UniverseEntry? {
    val n = fold(name)
    return entries.filter { e -> fold(e.name) == n || e.aliases.any { fold(it) == n } }.singleOrNull()
}

fun relationIssues(entries: List<UniverseEntry>): List<RelationIssue> {
    val out = mutableListOf<RelationIssue>()
    val people = entries.filter { it.kind == EntryKind.Personagem || it.kind == EntryKind.Grupo }
    for (e in people) {
        val allies = e.listBlock("aliados")
        val enemies = e.listBlock("inimigos")
        allies.filter { a -> enemies.any { fold(it) == fold(a) } }.forEach { out += RelationIssue(e.id, "$it está em Aliados e em Inimigos de ${e.name}.") }
        for (a in allies) {
            val other = resolve(a, people) ?: continue
            if (other.id == e.id) continue
            if (other.listBlock("inimigos").any { resolve(it, people)?.id == e.id }) {
                out += RelationIssue(e.id, "Na ficha de ${e.name}, ${other.name} é aliado; na ficha de ${other.name}, ${e.name} é inimigo.")
            }
        }
    }
    return out
}

// ---------- selar a descrição física ----------

enum class SealStatus { Novo, Igual, Conflito }

/** O que a descrição física diz de uma categoria e como isso se compara ao atributo da ficha. */
data class SealSuggestion(val kind: FactKind, val values: List<String>, val current: String, val sealed: Boolean, val status: SealStatus)

/** Fatos da Descrição física da ficha, comparados com os atributos (para "Para conferir"). */
fun sealSuggestions(entry: UniverseEntry): List<SealSuggestion> {
    if (entry.kind != EntryKind.Personagem) return emptyList()
    val facts = extractFacts(entry.blockText("aparencia"))
    return facts.groupBy { it.kind }.map { (kind, list) ->
        val values = list.map { it.value }.distinct()
        val attr = entry.attributes.firstOrNull { it.key == kind.attrKey }
        val current = attr?.value.orEmpty()
        val known = if (current.isBlank()) emptySet() else attributeTerms(kind, current)
        val status = when {
            current.isBlank() -> SealStatus.Novo
            kind == FactKind.Marca -> if (values.all { v -> fold(current).contains(v) }) SealStatus.Igual else SealStatus.Conflito
            values.all { it in known } -> SealStatus.Igual
            values.any { v -> contradicts(Fact(kind, v, 0, 0), known) } -> SealStatus.Conflito
            else -> SealStatus.Novo
        }
        SealSuggestion(kind, values, current, attr?.sealed == true, status)
    }.filterNot { it.status == SealStatus.Igual && it.sealed }
}

/** Texto do atributo a partir dos valores achados ("ruivo, curto"). */
fun sealText(values: List<String>): String = values.joinToString(", ")
