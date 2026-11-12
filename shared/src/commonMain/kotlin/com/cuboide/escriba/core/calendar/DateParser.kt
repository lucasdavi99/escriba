package com.cuboide.escriba.core.calendar

import com.cuboide.escriba.core.universe.fold

/** Resultado de ler uma data digitada: a data ou o motivo de não ter dado. */
sealed interface ParsedDate {
    data class Ok(val date: WorldDate) : ParsedDate
    data class Problem(val message: String) : ParsedDate
    data object Empty : ParsedDate
}

/**
 * Lê datas como "12 de Bruma, 347 E.C.", "12 bruma 347", "Bruma de 347", "ano 12", "12/3/347" e "-40".
 * O mês vale pelo nome inteiro ou pelo começo (3 letras ou mais, sem ambiguidade); a era, pela sigla (com ou sem
 * pontos) ou pelo nome. Sem ano, usa o ano do presente da história (ou 1).
 */
fun WorldCalendar.parseDate(input: String): ParsedDate {
    val raw = fold(input.trim()).replace(Regex("\\s+"), " ")
    if (raw.isEmpty()) return ParsedDate.Empty
    if (months.isEmpty()) return ParsedDate.Problem("O calendário não tem meses.")

    // Era no fim: "347 e.c.", "347 era da coroa".
    var text = raw
    var era: Era? = null
    for (e in eras.sortedByDescending { it.abbr.length + it.name.length }) {
        val byName = fold(e.name).takeIf { it.isNotEmpty() && text.endsWith(" $it") }?.let { text.removeSuffix(it) }
        val rest = byName ?: stripEraAbbr(text, e.abbr)
        if (rest != null) { era = e; text = rest; break }
    }
    text = text.trim().trimEnd(',').trim()

    fun year(s: String): Int? {
        val n = s.toIntOrNull() ?: return null
        val e = era ?: return n
        return e.startYear + n - 1
    }
    val defaultYear = present?.year ?: 1

    // dd/mm/aaaa
    Regex("^(\\d{1,3})/(\\d{1,3})(?:/(-?\\d+))?$").matchEntire(text)?.let { m ->
        val mi = m.groupValues[2].toInt() - 1
        val month = months.getOrNull(mi) ?: return ParsedDate.Problem("O calendário tem ${months.size} meses.")
        val y = m.groupValues[3].takeIf { it.isNotEmpty() }?.let(::year) ?: defaultYear
        return checked(WorldDate(y, month.id, m.groupValues[1].toInt()))
    }
    // ano 12 · 12 · -40
    Regex("^(?:ano )?(-?\\d+)$").matchEntire(text)?.let { m ->
        val y = year(m.groupValues[1]) ?: return ParsedDate.Problem("Ano inválido.")
        return ParsedDate.Ok(WorldDate(y, months.first().id, 1, DatePrecision.Year))
    }
    // [dia] [de] mês [,|de] [ano] [ano]
    val m = Regex("^(?:(\\d{1,3}) )?(?:de )?([\\p{L}][\\p{L} '-]*?)(?:,? (?:de )?(?:ano )?(-?\\d+))?$").matchEntire(text)
        ?: return ParsedDate.Problem("Não entendi a data. Exemplo: “12 de ${months.first().name}, ${yearLabel(defaultYear)}”.")
    val month = findMonth(m.groupValues[2].trim()) ?: return ParsedDate.Problem("Mês “${m.groupValues[2].trim()}” não existe neste calendário.")
    val y = m.groupValues[3].takeIf { it.isNotEmpty() }?.let(::year) ?: defaultYear
    val day = m.groupValues[1].takeIf { it.isNotEmpty() }?.toInt()
    return if (day == null) ParsedDate.Ok(WorldDate(y, month.id, 1, DatePrecision.Month)) else checked(WorldDate(y, month.id, day))
}

/**
 * Tira a sigla da era do fim do texto aceitando pontos e espaços no meio ("e.c.", "e. c.", "ec"). Só vale se antes
 * dela vier um número (o ano), para não confundir com o fim do nome de um mês. Devolve `null` se não achou.
 */
private fun stripEraAbbr(text: String, abbr: String): String? {
    val letters = fold(abbr).filter { it.isLetterOrDigit() }
    if (letters.isEmpty()) return null
    var i = text.length
    var j = letters.length
    while (i > 0 && j > 0) {
        val ch = text[i - 1]
        if (ch == '.' || ch == ' ') { i--; continue }
        if (ch != letters[j - 1]) return null
        i--; j--
    }
    if (j > 0) return null
    val rest = text.substring(0, i).trimEnd()
    return rest.takeIf { it.lastOrNull()?.isDigit() == true }
}

private fun WorldCalendar.findMonth(name: String): CalMonth? {
    val n = fold(name)
    months.firstOrNull { fold(it.name) == n }?.let { return it }
    if (n.length < 3) return null
    val prefix = months.filter { fold(it.name).startsWith(n) }
    return prefix.singleOrNull()
}

private fun WorldCalendar.checked(date: WorldDate): ParsedDate {
    val mi = monthIndex(date.monthId)
    val len = monthLength(date.year, mi)
    if (date.day < 1 || date.day > len) return ParsedDate.Problem("${month(date.monthId)?.name} tem $len dias em ${yearLabel(date.year)}.")
    return ParsedDate.Ok(date)
}
