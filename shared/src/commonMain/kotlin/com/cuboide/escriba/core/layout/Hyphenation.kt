package com.cuboide.escriba.core.layout

/**
 * Hifenização do português pelas regras de divisão silábica (não usa dicionário):
 *
 * - dígrafos `ch`, `lh`, `nh`, `qu`, `gu` (antes de e/i) contam como uma consoante;
 * - consoante + `l`/`r` (`br`, `pl`, `tr`…) começa a sílaba junto;
 * - uma consoante entre vogais vai para a sílaba seguinte (`ca-sa`);
 * - duas consoantes se separam (`par-te`, `car-ro`, `as-sim`, `ex-ce-to`);
 * - três ou mais: a última (ou o grupo inseparável final) vai para a seguinte (`pers-pi-caz`, `ins-tru-ir`);
 * - ditongos (`ai`, `ei`, `ou`, `ão`, `õe`…) ficam juntos; hiatos se separam (`sa-ú-de`, `po-e-ta`, `ra-i-nha`).
 *
 * Devolve as posições (índices em [word]) onde a palavra pode ser quebrada com hífen.
 */
fun hyphenationPoints(word: String, leftMin: Int = 2, rightMin: Int = 3): List<Int> {
    if (word.length < leftMin + rightMin) return emptyList()
    if (word.any { !it.isLetter() }) return emptyList()
    // Siglas e palavras em caixa alta não se quebram.
    if (word.length > 1 && word.all { it.isUpperCase() }) return emptyList()
    return syllableBoundaries(word.lowercase()).filter { it >= leftMin && word.length - it >= rightMin }
}

/** Fronteiras de sílaba (índices onde começa uma nova sílaba, exceto a primeira). */
fun syllableBoundaries(w: String): List<Int> {
    val units = toUnits(w)
    val boundaries = ArrayList<Int>()
    // Índices (em units) dos núcleos vocálicos, já separados entre ditongos e hiatos.
    var i = 0
    var lastNucleusEnd = -1 // índice em units logo após o último núcleo
    while (i < units.size) {
        if (!units[i].vowel) { i++; continue }
        // Sequência de vogais começando em i.
        var j = i + 1
        while (j < units.size && units[j].vowel) j++
        val vowelBoundaries = splitVowels(units, i, j)
        if (lastNucleusEnd >= 0) {
            val consonants = lastNucleusEnd until i
            val cut = consonantCut(units, consonants)
            boundaries += units[cut].start
        }
        vowelBoundaries.forEach { boundaries += units[it].start }
        lastNucleusEnd = j
        i = j
    }
    return boundaries.distinct().sorted()
}

private class Unit(val text: String, val start: Int, val vowel: Boolean)

private const val VOWELS = "aeiouáéíóúâêôãõàüy"
private val INSEPARABLE_FIRST = "bcdfgkptv"

private fun isVowel(c: Char) = c in VOWELS

/** Quebra a palavra em unidades: vogais isoladas e consoantes (com dígrafos e qu/gu juntos). */
private fun toUnits(w: String): List<Unit> {
    val out = ArrayList<Unit>()
    var i = 0
    while (i < w.length) {
        val c = w[i]
        val next = w.getOrNull(i + 1)
        val after = w.getOrNull(i + 2)
        when {
            (c == 'q' && (next == 'u' || next == 'ü')) -> { out += Unit(w.substring(i, i + 2), i, false); i += 2 }
            // gu + vogal: "guer-ra", "á-gua", "lin-gua-gem" (o u não forma sílaba própria)
            (c == 'g' && (next == 'u' || next == 'ü') && after != null && isVowel(after)) -> { out += Unit(w.substring(i, i + 2), i, false); i += 2 }
            (next == 'h' && c in "cln") -> { out += Unit(w.substring(i, i + 2), i, false); i += 2 }
            else -> { out += Unit(c.toString(), i, isVowel(c)); i++ }
        }
    }
    return out
}

/** Dentro de uma sequência de vogais [from, to), devolve os índices (em units) que começam nova sílaba. */
private fun splitVowels(units: List<Unit>, from: Int, to: Int): List<Int> {
    val out = ArrayList<Int>()
    var k = from + 1
    while (k < to) {
        val prev = units[k - 1].text[0]
        val cur = units[k].text[0]
        val diphthong = when {
            // ão, ãe, õe, õi
            prev in "ãõ" && cur in "oei" -> true
            // vogal + i/u sem acento forma ditongo, salvo antes de "nh" ou de nasal/consoante + consoante (ra-i-nha, a-in-da)
            cur in "iu" && prev != cur -> !hiatusAhead(units, k)
            else -> false
        }
        if (!diphthong) out += k
        k++
    }
    return out
}

/** i/u seguido de `nh`, ou de m/n/r/l/z + consoante (ou fim), soa como hiato: ra-i-nha, a-in-da, ju-iz. */
private fun hiatusAhead(units: List<Unit>, k: Int): Boolean {
    val next = units.getOrNull(k + 1) ?: return false
    if (next.vowel) return false
    if (next.text == "nh") return true
    if (next.text.length == 1 && next.text[0] in "mnrlz") {
        val after = units.getOrNull(k + 2)
        return after == null || !after.vowel
    }
    return false
}

/** Onde cortar o grupo de consoantes entre dois núcleos: devolve o índice (em units) que inicia a nova sílaba. */
private fun consonantCut(units: List<Unit>, consonants: IntRange): Int {
    val list = consonants.toList()
    return when (list.size) {
        0 -> consonants.first // não acontece (núcleos já separados), mas mantém o tipo
        1 -> list[0]
        else -> {
            val last = list.last()
            val beforeLast = list[list.size - 2]
            if (inseparable(units[beforeLast], units[last])) beforeLast else last
        }
    }
}

private fun inseparable(a: Unit, b: Unit): Boolean =
    a.text.length == 1 && b.text.length == 1 && a.text[0] in INSEPARABLE_FIRST && b.text[0] in "lr" &&
        !(a.text[0] == 'd' && b.text[0] == 'l')
