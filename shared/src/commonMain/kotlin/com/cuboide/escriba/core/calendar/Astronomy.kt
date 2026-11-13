package com.cuboide.escriba.core.calendar

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt

/*
 * Céu do mundo, em regras simples e previsíveis (o autor ajusta os números no editor):
 * - fase da lua = quanto do ciclo passou desde uma lua nova conhecida;
 * - luz do dia = curva de cosseno entre o dia mais curto e o mais longo do ano;
 * - eclipse = lua nova (do sol) ou cheia (da lua) perto do centro de uma temporada de eclipses.
 * Os dias são índices absolutos (ver CalendarMath); "meio-dia" de um dia d é d + 0,5.
 */

enum class MoonPhase(val label: String) {
    Nova("nova"), Crescente("crescente"), QuartoCrescente("quarto crescente"), GibosaCrescente("gibosa crescente"),
    Cheia("cheia"), GibosaMinguante("gibosa minguante"), QuartoMinguante("quarto minguante"), Minguante("minguante"),
}

/** Estado de uma lua num dia: [fraction] 0 = nova, 0,5 = cheia; [illumination] de 0 a 1. */
data class MoonState(val moon: Moon, val fraction: Double, val illumination: Double, val phase: MoonPhase, val dayOfCycle: Int)

private fun posMod(a: Double, m: Double): Double = ((a % m) + m) % m

/** Idade da lua (dias desde a última nova) no instante [t] (índice de dia com fração). */
fun Moon.ageAt(t: Double): Double = if (cycleDays <= 0.0) 0.0 else posMod(t - epochDay, cycleDays)

/** Estado da lua ao meio-dia do dia [day]. */
fun Moon.stateOn(day: Long): MoonState {
    val f = if (cycleDays <= 0.0) 0.0 else ageAt(day + 0.5) / cycleDays
    val illumination = (1 - cos(2 * PI * f)) / 2
    val phase = MoonPhase.entries[(floor(f * 8 + 0.5).toInt()) % 8]
    return MoonState(this, f, illumination, phase, floor(ageAt(day + 0.5)).toInt() + 1)
}

/** A lua cheia (meio ciclo) acontece durante o dia [day]. */
fun Moon.isFullOn(day: Long): Boolean = crosses(day, cycleDays / 2)

/** A lua nova acontece durante o dia [day]. */
fun Moon.isNewOn(day: Long): Boolean = crosses(day, 0.0)

/** O instante de idade [target] cai dentro do dia [day] (do começo ao fim). */
private fun Moon.crosses(day: Long, target: Double): Boolean {
    if (cycleDays <= 1.0) return false
    val start = ageAt(day.toDouble())
    val untilTarget = posMod(target - start, cycleDays)
    return untilTarget < 1.0
}

/** Próximo dia (a partir de [from], inclusive) com lua cheia; `null` se o ciclo é inválido. */
fun Moon.nextFull(from: Long): Long? = nextCrossing(from, cycleDays / 2)

fun Moon.nextNew(from: Long): Long? = nextCrossing(from, 0.0)

private fun Moon.nextCrossing(from: Long, target: Double): Long? {
    if (cycleDays <= 1.0) return null
    val wait = posMod(target - ageAt(from.toDouble()), cycleDays)
    return from + floor(wait).toLong()
}

/** Primeiro dia a partir de [from] em que todas as luas estão (quase) cheias, procurando até [maxDays] à frente. */
fun WorldCalendar.allFullTogether(from: Long, maxDays: Long = 200L * 366): Long? {
    if (moons.size < 2) return null
    var d = from
    val end = from + maxDays
    while (d <= end) {
        if (moons.all { it.stateOn(d).illumination >= 0.97 }) return d
        d++
    }
    return null
}

// ---------- sol ----------

/** Luz do dia: horas de claridade, nascer e pôr (em horas desde a meia-noite). */
data class Daylight(val hours: Double, val sunrise: Double, val sunset: Double)

/** Duração média do ano em dias (conta os bissextos). */
val WorldCalendar.meanYearLength: Double
    get() {
        val r = leap ?: return baseYearLength.toDouble()
        if (r.every <= 0 || monthIndex(r.monthId) < 0) return baseYearLength.toDouble()
        var extra = 1.0 / r.every
        if (r.skipEvery > 0) {
            extra -= 1.0 / r.skipEvery
            if (r.keepEvery > 0) extra += 1.0 / r.keepEvery
        }
        return baseYearLength + extra
    }

fun WorldCalendar.daylight(day: Long): Daylight? {
    val s = sun ?: return null
    val year = meanYearLength
    if (year <= 0.0) return null
    val date = dateAt(day) ?: return null
    // Dia mais longo do mesmo ano (se o mês sumiu, usa o começo do ano).
    val longest = dayIndex(WorldDate(date.year, s.longestMonthId, s.longestDay.coerceAtLeast(1)))
        ?: daysBeforeYear(date.year)
    val mid = (s.longestHours + s.shortestHours) / 2
    val amp = (s.longestHours - s.shortestHours) / 2
    val hours = (mid + amp * cos(2 * PI * (day - longest) / year)).coerceIn(0.0, s.dayHours)
    val noon = s.dayHours / 2
    return Daylight(hours, noon - hours / 2, noon + hours / 2)
}

/** Hora em "HH:MM" (minutos arredondados). */
fun formatHour(h: Double): String {
    val total = (h * 60).roundToInt().coerceAtLeast(0)
    return "${(total / 60).toString().padStart(2, '0')}:${(total % 60).toString().padStart(2, '0')}"
}

/** Duração em "11 h 27". */
fun formatDuration(h: Double): String {
    val total = (h * 60).roundToInt()
    return "${total / 60} h ${(total % 60).toString().padStart(2, '0')}"
}

// ---------- eclipses ----------

enum class EclipseKind(val label: String) { Solar("Eclipse do sol"), Lunar("Eclipse") }

data class Eclipse(val kind: EclipseKind, val moon: Moon)

/** O dia [day] está dentro de uma temporada de eclipses. */
fun WorldCalendar.inEclipseSeason(day: Long): Boolean {
    val s = sun ?: return false
    if (s.eclipseSeasonDays <= 0.0 || s.eclipseWindowDays <= 0.0) return false
    val pos = posMod(day + 0.5 - s.eclipseEpochDay, s.eclipseSeasonDays)
    val distance = minOf(pos, s.eclipseSeasonDays - pos)
    return distance <= s.eclipseWindowDays / 2
}

/** Eclipses do dia: lua nova numa temporada escurece o sol; lua cheia numa temporada fica avermelhada. */
fun WorldCalendar.eclipsesOn(day: Long): List<Eclipse> {
    if (!inEclipseSeason(day)) return emptyList()
    return moons.mapNotNull { m ->
        when {
            m.isNewOn(day) -> Eclipse(EclipseKind.Solar, m)
            m.isFullOn(day) -> Eclipse(EclipseKind.Lunar, m)
            else -> null
        }
    }
}

/** Todos os eclipses do ano [year], em ordem. */
fun WorldCalendar.eclipsesInYear(year: Int): List<Pair<Long, Eclipse>> {
    val start = daysBeforeYear(year)
    return (0 until yearLength(year)).flatMap { i -> eclipsesOn(start + i).map { (start + i) to it } }
}

/** Diferença de luz em relação ao dia anterior, em minutos ("+3 min", "−2 min"). */
fun WorldCalendar.daylightChange(day: Long): String? {
    val today = daylight(day) ?: return null
    val yesterday = daylight(day - 1) ?: return null
    val minutes = ((today.hours - yesterday.hours) * 60).roundToInt()
    return when {
        minutes == 0 -> "igual a ontem"
        minutes > 0 -> "+$minutes min que ontem"
        else -> "−${abs(minutes)} min que ontem"
    }
}
