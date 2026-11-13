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

/**
 * Lua do mundo. [cycleDays] é o tempo de uma lua nova até a seguinte; [epochDay] é o índice absoluto (pode ter
 * fração) de uma lua nova qualquer, que ancora as fases. [color] em `#RRGGBB`.
 */
@Serializable
data class Moon(val id: String, val name: String, val color: String = "#E8E2D0", val cycleDays: Double = 29.53, val epochDay: Double = 0.0)

/**
 * Sol e eclipses. O dia mais longo do ano é o dia [longestDay] do mês [longestMonthId]; a duração da luz varia
 * entre [longestHours] e [shortestHours] numa curva suave. Eclipses só acontecem perto do centro das temporadas,
 * que se repetem a cada [eclipseSeasonDays] dias a partir de [eclipseEpochDay], numa janela de [eclipseWindowDays].
 */
@Serializable
data class SunSpec(
    val longestMonthId: String,
    val longestDay: Int = 1,
    val longestHours: Double = 15.0,
    val shortestHours: Double = 9.0,
    val dayHours: Double = 24.0,
    val eclipseSeasonDays: Double = 0.0,
    val eclipseWindowDays: Double = 0.0,
    val eclipseEpochDay: Double = 0.0,
)

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
    val moons: List<Moon> = emptyList(),
    /** Sol e eclipses; `null` = o calendário não acompanha a luz do dia. */
    val sun: SunSpec? = null,
    /** O "presente da história": de onde a Linha do tempo parte e a idade das fichas é contada. */
    val present: WorldDate? = null,
) {
    fun month(id: String): CalMonth? = months.firstOrNull { it.id == id }
    fun monthIndex(id: String): Int = months.indexOfFirst { it.id == id }
}
