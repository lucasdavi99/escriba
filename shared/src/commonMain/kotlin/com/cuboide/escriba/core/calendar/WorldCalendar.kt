package com.cuboide.escriba.core.calendar

import kotlinx.serialization.Serializable

/** Mês do calendário do mundo. [outsideWeek]: dias "fora da semana" (não avançam o dia da semana). */
@Serializable
data class CalMonth(val id: String, val name: String, val days: Int, val outsideWeek: Boolean = false)

/**
 * Ano bissexto: a cada [every] anos o mês [monthId] ganha um dia; exceto a cada [skipEvery] anos, a não ser que
 * também seja múltiplo de [keepEvery] (0 desliga a regra). O nosso calendário é (4, 100, 400).
 */
@Serializable
data class LeapRule(val monthId: String, val every: Int = 4, val skipEvery: Int = 0, val keepEvery: Int = 0)

/** Era a partir do ano absoluto [startYear]; dentro dela, o ano conta de 1. */
@Serializable
data class Era(val id: String, val name: String, val abbr: String, val startYear: Int)

/** Estação que começa no dia [day] do mês [monthId] e vai até a próxima. [color] em `#RRGGBB`. */
@Serializable
data class Season(val id: String, val name: String, val color: String, val monthId: String, val day: Int)

/** Festa que se repete todo ano. */
@Serializable
data class Festival(val id: String, val name: String, val monthId: String, val day: Int, val description: String = "")

/** Quanto da data foi informado: só o ano, mês e ano, ou o dia. */
@Serializable
enum class DatePrecision { Year, Month, Day }

/**
 * Data no calendário do mundo. [year] é o ano absoluto (independente da era). O mês vai pela id, então renomear
 * um mês não quebra nenhuma data.
 */
@Serializable
data class WorldDate(val year: Int, val monthId: String, val day: Int = 1, val precision: DatePrecision = DatePrecision.Day)

/** Calendário do livro, guardado em `calendar.json`. */
@Serializable
data class WorldCalendar(
    val name: String = "Calendário",
    val months: List<CalMonth> = emptyList(),
    val weekdays: List<String> = emptyList(),
    /** Dia da semana (índice em [weekdays]) do primeiro dia do ano 0. */
    val weekdayOffset: Int = 0,
    val leap: LeapRule? = null,
    val eras: List<Era> = emptyList(),
    val seasons: List<Season> = emptyList(),
    val festivals: List<Festival> = emptyList(),
    /** O "presente da história": de onde a Linha do tempo parte e a idade das fichas é contada. */
    val present: WorldDate? = null,
) {
    fun month(id: String): CalMonth? = months.firstOrNull { it.id == id }
    fun monthIndex(id: String): Int = months.indexOfFirst { it.id == id }
}
