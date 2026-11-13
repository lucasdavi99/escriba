package com.cuboide.escriba

import com.cuboide.escriba.core.calendar.CalendarTemplate
import com.cuboide.escriba.core.calendar.CalendarTemplates
import com.cuboide.escriba.core.calendar.EclipseKind
import com.cuboide.escriba.core.calendar.Moon
import com.cuboide.escriba.core.calendar.MoonPhase
import com.cuboide.escriba.core.calendar.SunSpec
import com.cuboide.escriba.core.calendar.WorldCalendar
import com.cuboide.escriba.core.calendar.WorldDate
import com.cuboide.escriba.core.calendar.allFullTogether
import com.cuboide.escriba.core.calendar.dateAt
import com.cuboide.escriba.core.calendar.dayIndex
import com.cuboide.escriba.core.calendar.daylight
import com.cuboide.escriba.core.calendar.daylightChange
import com.cuboide.escriba.core.calendar.eclipsesInYear
import com.cuboide.escriba.core.calendar.eclipsesOn
import com.cuboide.escriba.core.calendar.formatDuration
import com.cuboide.escriba.core.calendar.formatHour
import com.cuboide.escriba.core.calendar.isFullOn
import com.cuboide.escriba.core.calendar.isNewOn
import com.cuboide.escriba.core.calendar.nextFull
import com.cuboide.escriba.core.calendar.nextNew
import com.cuboide.escriba.core.calendar.stateOn
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AstronomyTest {
    private val ours = CalendarTemplates.build(CalendarTemplate.ComoONosso)
    private val aldamar = CalendarTemplates.build(CalendarTemplate.Exemplo)
    private fun day(y: Int, m: Int, d: Int): Long = ours.dayIndex(WorldDate(y, "m$m", d))!!
    private fun near(expected: Long, actual: Long?, label: String) =
        assertTrue(actual != null && abs(actual - expected) <= 1, "$label: esperado ${ours.dateAt(expected)}, veio ${actual?.let { ours.dateAt(it) }}")

    @Test
    fun phasesFollowTheCycle() {
        val m = Moon("m", "Teste", cycleDays = 30.0, epochDay = 0.0)
        // Meio-dia do dia 0: idade 0,5 → nova; dia 15 → cheia; dia 7 → quarto crescente.
        assertEquals(MoonPhase.Nova, m.stateOn(0).phase)
        assertEquals(MoonPhase.Cheia, m.stateOn(15).phase)
        assertEquals(MoonPhase.QuartoCrescente, m.stateOn(7).phase)
        assertEquals(MoonPhase.QuartoMinguante, m.stateOn(22).phase)
        assertTrue(m.stateOn(15).illumination > 0.99)
        assertTrue(m.stateOn(0).illumination < 0.01)
        assertEquals(16, m.stateOn(15).dayOfCycle)
        // Exatamente um dia de cheia e um de nova por ciclo, inclusive em dias negativos.
        for (start in listOf(0L, -300L, 9000L)) {
            val days = (start until start + 30)
            assertEquals(1, days.count { m.isFullOn(it) })
            assertEquals(1, days.count { m.isNewOn(it) })
        }
        assertEquals(15L, m.nextFull(1))
        assertEquals(30L, m.nextNew(1))
        assertEquals(15L, m.nextFull(15))
    }

    @Test
    fun realMoonsOf2026WithinADay() {
        val moon = ours.moons.single()
        val fulls = listOf(1 to 3, 2 to 1, 3 to 3, 4 to 2, 5 to 1, 5 to 31, 6 to 29, 7 to 29, 8 to 28, 9 to 26, 10 to 26, 11 to 24, 12 to 24)
        val news = listOf(1 to 18, 2 to 17, 3 to 19, 4 to 17, 5 to 16, 6 to 15, 7 to 14, 8 to 12, 9 to 11, 10 to 10, 11 to 9, 12 to 9)
        fulls.forEach { (m, d) -> near(day(2026, m, d), moon.nextFull(day(2026, m, d) - 5), "cheia $d/$m") }
        news.forEach { (m, d) -> near(day(2026, m, d), moon.nextNew(day(2026, m, d) - 5), "nova $d/$m") }
    }

    @Test
    fun eclipsesOf2025And2026WithinADay() {
        checkEclipses(2025, listOf(3 to 14 to EclipseKind.Lunar, 3 to 29 to EclipseKind.Solar, 9 to 7 to EclipseKind.Lunar, 9 to 21 to EclipseKind.Solar))
        checkEclipses(2026, listOf(2 to 17 to EclipseKind.Solar, 3 to 3 to EclipseKind.Lunar, 8 to 12 to EclipseKind.Solar, 8 to 28 to EclipseKind.Lunar))
    }

    private fun checkEclipses(year: Int, real: List<Pair<Pair<Int, Int>, EclipseKind>>) {
        val found = ours.eclipsesInYear(year)
        val expected = real.map { (md, kind) -> day(year, md.first, md.second) to kind }
        assertEquals(expected.size, found.size, found.map { ours.dateAt(it.first) to it.second.kind }.toString())
        expected.zip(found).forEach { (e, f) ->
            assertEquals(e.second, f.second.kind)
            near(e.first, f.first, "eclipse")
        }
    }

    @Test
    fun daylightCurve() {
        val longest = ours.daylight(day(2026, 12, 21))!!
        val shortest = ours.daylight(day(2026, 6, 21))!!
        assertEquals(13.5, longest.hours, 0.01)
        assertEquals(10.6, shortest.hours, 0.02)
        assertEquals(12.0, (longest.sunrise + longest.sunset) / 2, 1e-9) // meio-dia no meio
        assertEquals("05:15", formatHour(longest.sunrise))
        assertEquals("13 h 30", formatDuration(longest.hours))
        // Simetria em volta do dia mais longo e a diferença para ontem.
        assertEquals(ours.daylight(day(2026, 12, 11))!!.hours, ours.daylight(day(2026, 12, 31))!!.hours, 0.01)
        assertTrue(ours.daylightChange(day(2026, 10, 1))!!.startsWith("+"))
        assertTrue(ours.daylightChange(day(2026, 3, 1))!!.startsWith("−"))
        assertNull(WorldCalendar().daylight(0))
    }

    @Test
    fun inventedMoonsAlignAndEclipseWindowIsRespected() {
        val together = aldamar.allFullTogether(aldamar.dayIndex(WorldDate(1111, "a1", 1))!!)
        assertNotNull(together)
        assertTrue(aldamar.moons.all { it.stateOn(together).illumination >= 0.97 })
        // Sem temporada de eclipses: nenhum eclipse, mesmo com lua nova.
        val noSeason = aldamar.copy(sun = SunSpec("a4", 20))
        assertTrue((0L until 2000L).none { noSeason.eclipsesOn(it).isNotEmpty() })
        // Com temporada: só perto do centro dela.
        val withSeason = aldamar.eclipsesInYear(1111)
        assertTrue(withSeason.isNotEmpty())
        val s = aldamar.sun!!
        withSeason.forEach { (d, _) ->
            val pos = ((d + 0.5 - s.eclipseEpochDay) % s.eclipseSeasonDays + s.eclipseSeasonDays) % s.eclipseSeasonDays
            assertTrue(minOf(pos, s.eclipseSeasonDays - pos) <= s.eclipseWindowDays / 2)
        }
    }

    @Test
    fun oldCalendarsGetTheSkyOfTheirTemplate() {
        // Calendário de Aldamar gravado antes de existirem luas e sol: ganha as duas luas e o sol do modelo.
        val old = aldamar.copy(moons = emptyList(), sun = null, name = "Renomeado")
        val fixed = CalendarTemplates.suggestedSky(old)
        assertEquals(aldamar.moons, fixed.moons)
        assertEquals(aldamar.sun, fixed.sun)
        assertEquals("Renomeado", fixed.name)
        // Calendário feito à mão: uma lua de 30 dias e um sol sem eclipses, para ajustar.
        val custom = CalendarTemplates.build(CalendarTemplate.EmBranco).copy(months = listOf(com.cuboide.escriba.core.calendar.CalMonth("x", "Único", 90), com.cuboide.escriba.core.calendar.CalMonth("y", "Outro", 90), com.cuboide.escriba.core.calendar.CalMonth("z", "Último", 90)))
        val generic = CalendarTemplates.suggestedSky(custom)
        assertEquals(30.0, generic.moons.single().cycleDays)
        assertEquals("y", generic.sun?.longestMonthId)
        assertEquals(0.0, generic.sun?.eclipseSeasonDays)
        // Quem já tem céu não muda.
        assertEquals(aldamar, CalendarTemplates.suggestedSky(aldamar))
    }
}
