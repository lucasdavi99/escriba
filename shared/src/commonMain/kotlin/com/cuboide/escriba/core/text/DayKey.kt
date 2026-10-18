package com.cuboide.escriba.core.text

/**
 * Aritmética de datas `yyyy-MM-dd` sem depender de bibliotecas de calendário
 * (algoritmo civil proleptic gregoriano, dias desde 1970-01-01).
 */
object DayKey {
    fun toEpochDay(key: String): Long {
        val (y0, m, d) = key.split('-').map { it.toInt() }
        val y = if (m <= 2) y0 - 1 else y0
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val mp = (m + 9) % 12
        val doy = (153 * mp + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097L + doe - 719468L
    }

    fun fromEpochDay(epochDay: Long): String {
        val z = epochDay + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = if (mp < 10) mp + 3 else mp - 9
        val y = yoe + era * 400 + if (m <= 2) 1 else 0
        return "${y.toString().padStart(4, '0')}-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
    }

    fun plusDays(key: String, days: Long): String = fromEpochDay(toEpochDay(key) + days)

    fun daysBetween(from: String, to: String): Long = toEpochDay(to) - toEpochDay(from)

    /** Dia da semana: 0 = domingo … 6 = sábado (1970-01-01 foi uma quinta). */
    fun dayOfWeek(key: String): Int = ((toEpochDay(key) + 4) % 7 + 7).toInt() % 7

    /** "17/10" a partir de "2026-10-17". */
    fun shortLabel(key: String): String = key.substring(8, 10) + "/" + key.substring(5, 7)
}

private val MONTHS = listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")

/** Uma coluna (semana, de domingo a sábado) do mapa de calor; `null` nas pontas fora do período. */
data class HeatmapColumn(val days: List<String?>, val monthLabel: String?)

/**
 * Arruma dias consecutivos em semanas de domingo a sábado. A coluna onde começa um mês leva o nome dele
 * (a primeira coluna só leva se tiver pelo menos três dias, para o rótulo não ficar espremido).
 */
fun heatmapColumns(days: List<String>): List<HeatmapColumn> {
    if (days.isEmpty()) return emptyList()
    val slots = List<String?>(DayKey.dayOfWeek(days.first())) { null } + days
    val weeks = slots.chunked(7).map { it + List(7 - it.size) { null } }
    var lastMonth = -1
    return weeks.mapIndexed { i, week ->
        val present = week.filterNotNull()
        val month = present.last().substring(5, 7).toInt() // a semana onde o mês começa leva o nome dele
        val label = if (month != lastMonth && (i > 0 || present.size >= 3)) MONTHS[month - 1] else null
        if (label != null) lastMonth = month
        HeatmapColumn(week, label)
    }
}
