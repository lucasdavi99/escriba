package com.cuboide.escriba.core.calendar

/*
 * Toda data vira um índice absoluto de dias (o dia 0 é o primeiro dia do ano 0) e volta. Anos negativos valem.
 * Os meses podem ter tamanhos diferentes; o ano bissexto acrescenta um dia a um mês; meses "fora da semana" não
 * avançam o dia da semana.
 */

private fun floorDiv(a: Long, b: Long): Long = a.floorDiv(b)
private fun ceilDiv(a: Long, b: Long): Long = -((-a).floorDiv(b))

/** Quantos dias tem o ano sem bissexto. */
val WorldCalendar.baseYearLength: Int get() = months.sumOf { it.days.coerceAtLeast(0) }

fun WorldCalendar.isLeap(year: Int): Boolean {
    val r = leap ?: return false
    if (r.every <= 0 || monthIndex(r.monthId) < 0) return false
    if (year.mod(r.every) != 0) return false
    if (r.skipEvery > 0 && year.mod(r.skipEvery) == 0) return r.keepEvery > 0 && year.mod(r.keepEvery) == 0
    return true
}

/** Quantos anos bissextos há em [0, year) (negativo para anos antes do 0). */
private fun WorldCalendar.leapsBefore(year: Int): Long {
    val r = leap ?: return 0
    if (r.every <= 0 || monthIndex(r.monthId) < 0) return 0
    val y = year.toLong()
    var n = ceilDiv(y, r.every.toLong())
    if (r.skipEvery > 0) {
        n -= ceilDiv(y, r.skipEvery.toLong())
        if (r.keepEvery > 0) n += ceilDiv(y, r.keepEvery.toLong())
    }
    return n
}

fun WorldCalendar.monthLength(year: Int, monthIndex: Int): Int {
    val m = months.getOrNull(monthIndex) ?: return 0
    return m.days.coerceAtLeast(0) + if (leap?.monthId == m.id && isLeap(year)) 1 else 0
}

fun WorldCalendar.yearLength(year: Int): Int = baseYearLength + if (isLeap(year)) 1 else 0

/** Dias antes do primeiro dia do ano [year]. */
fun WorldCalendar.daysBeforeYear(year: Int): Long = year.toLong() * baseYearLength + leapsBefore(year)

/** Índice absoluto do dia, ou `null` se o mês não existe mais ou o dia não cabe no mês. */
fun WorldCalendar.dayIndex(date: WorldDate): Long? {
    val mi = monthIndex(date.monthId)
    if (mi < 0) return null
    val day = date.day.coerceAtLeast(1)
    if (day > monthLength(date.year, mi)) return null
    var idx = daysBeforeYear(date.year)
    for (i in 0 until mi) idx += monthLength(date.year, i)
    return idx + day - 1
}

/** Data do dia de índice [index] (o inverso de [dayIndex]). */
fun WorldCalendar.dateAt(index: Long): WorldDate? {
    val base = baseYearLength
    if (base <= 0) return null
    var year = floorDiv(index, base.toLong()).toInt()
    while (daysBeforeYear(year) > index) year--
    while (daysBeforeYear(year + 1) <= index) year++
    var rest = index - daysBeforeYear(year)
    for (i in months.indices) {
        val len = monthLength(year, i)
        if (rest < len) return WorldDate(year, months[i].id, rest.toInt() + 1)
        rest -= len
    }
    return null
}

/** Dias que não contam na semana antes do primeiro dia do ano [year]. */
private fun WorldCalendar.outsideDaysBeforeYear(year: Int): Long {
    val perYear = months.filter { it.outsideWeek }.sumOf { it.days.coerceAtLeast(0) }
    val leapOutside = leap?.let { month(it.monthId)?.outsideWeek } == true
    return year.toLong() * perYear + if (leapOutside) leapsBefore(year) else 0
}

/** Índice do dia da semana (em [WorldCalendar.weekdays]) ou `null` se o dia é fora da semana ou não há semana. */
fun WorldCalendar.weekdayOf(date: WorldDate): Int? {
    if (weekdays.isEmpty()) return null
    val mi = monthIndex(date.monthId)
    if (mi < 0 || months[mi].outsideWeek) return null
    val idx = dayIndex(date) ?: return null
    var outside = outsideDaysBeforeYear(date.year)
    for (i in 0 until mi) if (months[i].outsideWeek) outside += monthLength(date.year, i)
    return (idx - outside + weekdayOffset).mod(weekdays.size.toLong()).toInt()
}

/** Era do ano absoluto [year] (a última que já começou), ou `null` se não há eras ou o ano é anterior a todas. */
fun WorldCalendar.eraOf(year: Int): Era? = eras.filter { it.startYear <= year }.maxByOrNull { it.startYear }

/** Ano como aparece para o leitor: contado dentro da era, com a sigla ("347 E.C."), ou o número puro. */
fun WorldCalendar.yearLabel(year: Int): String {
    val era = eraOf(year)
    if (era != null) return "${year - era.startYear + 1} ${era.abbr}".trim()
    val first = eras.minByOrNull { it.startYear } ?: return "$year"
    return "${first.startYear - year} antes de ${first.abbr.ifBlank { first.name }}"
}

fun WorldCalendar.format(date: WorldDate): String {
    val month = month(date.monthId)?.name ?: return "${yearLabel(date.year)} (mês apagado)"
    return when (date.precision) {
        DatePrecision.Year -> "Ano ${yearLabel(date.year)}"
        DatePrecision.Month -> "$month de ${yearLabel(date.year)}"
        DatePrecision.Day -> "${date.day} de $month, ${yearLabel(date.year)}"
    }
}

/** Idade em anos completos de quem nasceu em [birth], na data [at] (negativa se ainda não nasceu). */
fun WorldCalendar.ageAt(birth: WorldDate, at: WorldDate): Int? {
    val bm = monthIndex(birth.monthId)
    val am = monthIndex(at.monthId)
    if (bm < 0 || am < 0) return null
    var years = at.year - birth.year
    val beforeBirthday = am < bm || (am == bm && at.day < birth.day)
    if (birth.precision != DatePrecision.Year && at.precision != DatePrecision.Year && beforeBirthday) years--
    return years
}

/** Estação em que cai a data (a última que começou até ela, contando a virada do ano). */
fun WorldCalendar.seasonOf(date: WorldDate): Season? {
    if (seasons.isEmpty()) return null
    fun key(s: Season) = monthIndex(s.monthId) * 1000 + s.day
    val ordered = seasons.filter { monthIndex(it.monthId) >= 0 }.sortedBy(::key)
    if (ordered.isEmpty()) return null
    val k = monthIndex(date.monthId) * 1000 + date.day
    return ordered.lastOrNull { key(it) <= k } ?: ordered.last()
}

fun WorldCalendar.festivalsOn(date: WorldDate): List<Festival> =
    festivals.filter { it.monthId == date.monthId && it.day == date.day }

/** Diferença entre dois dias em texto ("3 anos depois", "12 dias antes"); [from] e [to] são índices absolutos. */
fun WorldCalendar.describeGap(from: Long, to: Long): String {
    val d = to - from
    if (d == 0L) return "no mesmo dia"
    val days = kotlin.math.abs(d)
    val year = baseYearLength.coerceAtLeast(1).toDouble()
    val monthLen = (months.filterNot { it.outsideWeek }.map { it.days }.average().takeIf { !it.isNaN() } ?: 30.0).coerceAtLeast(1.0)
    val text = when {
        days >= year -> plural((days / year).toLong(), "ano", "anos")
        days >= monthLen -> plural((days / monthLen).toLong(), "mês", "meses")
        else -> plural(days, "dia", "dias")
    }
    return if (d > 0) "$text depois" else "$text antes"
}

private fun plural(n: Long, one: String, many: String) = if (n == 1L) "1 $one" else "$n $many"
