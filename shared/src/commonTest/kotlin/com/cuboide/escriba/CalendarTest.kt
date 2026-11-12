package com.cuboide.escriba

import com.cuboide.escriba.core.calendar.CalMonth
import com.cuboide.escriba.core.calendar.CalendarTemplate
import com.cuboide.escriba.core.calendar.CalendarTemplates
import com.cuboide.escriba.core.calendar.DatePrecision
import com.cuboide.escriba.core.calendar.LeapRule
import com.cuboide.escriba.core.calendar.ParsedDate
import com.cuboide.escriba.core.calendar.WorldCalendar
import com.cuboide.escriba.core.calendar.WorldDate
import com.cuboide.escriba.core.calendar.ageAt
import com.cuboide.escriba.core.calendar.ageLabel
import com.cuboide.escriba.core.calendar.dateAt
import com.cuboide.escriba.core.calendar.dayIndex
import com.cuboide.escriba.core.calendar.describeGap
import com.cuboide.escriba.core.calendar.festivalsOn
import com.cuboide.escriba.core.calendar.format
import com.cuboide.escriba.core.calendar.isLeap
import com.cuboide.escriba.core.calendar.parseDate
import com.cuboide.escriba.core.calendar.seasonOf
import com.cuboide.escriba.core.calendar.timelineOf
import com.cuboide.escriba.core.calendar.weekdayOf
import com.cuboide.escriba.core.calendar.yearLabel
import com.cuboide.escriba.core.calendar.yearLength
import com.cuboide.escriba.core.model.Beat
import com.cuboide.escriba.core.model.EntryKind
import com.cuboide.escriba.core.model.UniverseEntry
import com.cuboide.escriba.core.universe.withAttr
import kotlin.test.Test
import kotlinx.coroutines.cancel
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalendarTest {
    private val ours = CalendarTemplates.build(CalendarTemplate.ComoONosso)
    private val aldamar = CalendarTemplates.build(CalendarTemplate.Exemplo)

    private fun ok(cal: WorldCalendar, text: String): WorldDate = assertIs<ParsedDate.Ok>(cal.parseDate(text), text).date

    @Test
    fun leapYearsFollowTheGregorianRule() {
        assertTrue(ours.isLeap(2024))
        assertFalse(ours.isLeap(1900))
        assertTrue(ours.isLeap(2000))
        assertTrue(ours.isLeap(0))
        assertTrue(ours.isLeap(-4))
        assertEquals(366, ours.yearLength(2024))
        assertEquals(365, ours.yearLength(2025))
        assertNull(ours.dayIndex(WorldDate(2025, "m2", 29)))
    }

    @Test
    fun indexAndDateRoundTripAcrossTenThousandYears() {
        for (cal in listOf(ours, aldamar, CalendarTemplates.build(CalendarTemplate.EmBranco))) {
            var idx = cal.dayIndex(WorldDate(-10_000, cal.months.first().id, 1))!!
            val end = cal.dayIndex(WorldDate(10_000, cal.months.first().id, 1))!!
            var previous: WorldDate? = null
            while (idx <= end) {
                val d = cal.dateAt(idx)!!
                assertEquals(idx, cal.dayIndex(d), "${cal.name} $d")
                if (previous != null) assertTrue(d != previous)
                previous = d
                idx += 997 // passos primos pegam todos os meses e anos bissextos
            }
        }
        // Dias seguidos na virada do ano.
        val last = ours.dayIndex(WorldDate(2024, "m12", 31))!!
        assertEquals(WorldDate(2025, "m1", 1), ours.dateAt(last + 1))
    }

    @Test
    fun weekdaysMatchTheRealCalendarAndSkipOutsideDays() {
        assertEquals("Quarta", ours.weekdays[ours.weekdayOf(WorldDate(2026, "m11", 11))!!])
        assertEquals("Segunda", ours.weekdays[ours.weekdayOf(WorldDate(1, "m1", 1))!!])
        assertEquals("Sexta", ours.weekdays[ours.weekdayOf(WorldDate(1500, "m4", 13))!!]) // proléptico
        // Aldamar: os 5 dias da Lua Velha não avançam a semana de 5 dias, e o ano tem 360 dias de semana.
        assertNull(aldamar.weekdayOf(WorldDate(1111, "a0", 3)))
        val lastDay = aldamar.weekdayOf(WorldDate(1111, "a9", 40))!!
        val nextYear = aldamar.weekdayOf(WorldDate(1112, "a1", 1))!!
        assertEquals((lastDay + 1) % 5, nextYear)
    }

    @Test
    fun parsesTypedDates() {
        assertEquals(WorldDate(1111, "a3", 7), ok(aldamar, "7 de Abelha, 612 E.P."))
        assertEquals(WorldDate(1111, "a3", 7), ok(aldamar, "7 abelha 612 ep"))
        assertEquals(WorldDate(1111, "a3", 7), ok(aldamar, "7 de abe, 612 e. p."))
        assertEquals(WorldDate(1111, "a3", 7), ok(aldamar, "7 de Abelha")) // sem ano: o do presente
        assertEquals(WorldDate(1111, "a3", 7), ok(aldamar, "7/3/612 E.P."))
        assertEquals(WorldDate(40, "a1", 1, DatePrecision.Year), ok(aldamar, "ano 40 E.T."))
        assertEquals(WorldDate(509, "a6", 1, DatePrecision.Month), ok(aldamar, "Trigo de 10 Era da Ponte"))
        assertEquals(WorldDate(12, "m3", 3), ok(ours, "3 de março, 12"))
        assertEquals(WorldDate(-40, "m1", 1, DatePrecision.Year), ok(ours, "-40"))
        assertIs<ParsedDate.Problem>(ours.parseDate("31 de abril de 2020"))
        assertIs<ParsedDate.Problem>(ours.parseDate("12 de Brumário"))
        assertIs<ParsedDate.Problem>(ours.parseDate("ju 2020")) // "ju" é curto demais para escolher entre junho e julho
        assertIs<ParsedDate.Empty>(ours.parseDate("  "))
    }

    @Test
    fun formatsWithErasAndPrecision() {
        assertEquals("7 de Abelha, 612 E.P.", aldamar.format(WorldDate(1111, "a3", 7)))
        assertEquals("Trigo de 10 E.P.", aldamar.format(WorldDate(509, "a6", 1, DatePrecision.Month)))
        assertEquals("Ano 499 E.T.", aldamar.format(WorldDate(499, "a1", 1, DatePrecision.Year)))
        assertEquals("1 antes de E.T.", aldamar.yearLabel(0))
        assertEquals("2026", ours.yearLabel(2026))
        // Mês apagado não derruba a formatação.
        assertEquals("612 E.P. (mês apagado)", aldamar.format(WorldDate(1111, "sumiu", 7)))
        // Renomear um mês não muda a data guardada, só o texto.
        val renamed = aldamar.copy(months = aldamar.months.map { if (it.id == "a3") it.copy(name = "Colmeia") else it })
        assertEquals("7 de Colmeia, 612 E.P.", renamed.format(WorldDate(1111, "a3", 7)))
    }

    @Test
    fun agesSeasonsAndFestivals() {
        val birth = WorldDate(2000, "m5", 10)
        assertEquals(25, ours.ageAt(birth, WorldDate(2026, "m5", 9)))
        assertEquals(26, ours.ageAt(birth, WorldDate(2026, "m5", 10)))
        assertEquals("Outono", ours.seasonOf(WorldDate(2026, "m4", 1))?.name)
        assertEquals("Verão", ours.seasonOf(WorldDate(2026, "m1", 5))?.name) // antes da primeira estação do ano: a do fim do ano anterior
        assertEquals(listOf("Natal"), ours.festivalsOn(WorldDate(1, "m12", 25)).map { it.name })

        var n = 0
        val mara = UniverseEntry("m", EntryKind.Personagem, "Mara")
            .withAttr("nasceu", "10 de maio de 2000", { "a${++n}" })
            .withAttr("morreu", "1 de janeiro de 2061", { "a${++n}" })
        assertEquals("26 anos", ours.ageLabel(mara, WorldDate(2026, "m11", 11)))
        assertEquals("ainda não nasceu", ours.ageLabel(mara, WorldDate(1999, "m1", 1)))
        assertEquals("morreu aos 60", ours.ageLabel(mara, WorldDate(2070, "m1", 1)))
        assertNull(ours.ageLabel(UniverseEntry("x", EntryKind.Personagem, "X"), WorldDate(2026, "m1", 1)))
    }

    @Test
    fun timelineSortsDatedScenesAndFlagsBrokenOnes() {
        val beats = listOf(
            Beat("b1", "Fuga", whenText = "3 de março, 12", date = WorldDate(12, "m3", 3)),
            Beat("b2", "Prólogo", whenText = "ano 5", date = WorldDate(5, "m1", 1, DatePrecision.Year)),
            Beat("b3", "Sem data"),
            Beat("b4", "Mês apagado", whenText = "2 de Bruma", date = WorldDate(12, "bruma", 2)),
            Beat("b5", "Ilegível", whenText = "quando a lua cair"),
        )
        val t = ours.timelineOf(beats)
        assertEquals(listOf("Prólogo", "Fuga"), t.dated.map { it.beat.title })
        assertEquals(listOf("Sem data"), t.undated.map { it.title })
        assertEquals(listOf("Mês apagado", "Ilegível"), t.broken.map { it.beat.title })
        assertEquals("7 anos depois", ours.describeGap(t.dated[0].index, t.dated[1].index))
        assertEquals("10 dias antes", ours.describeGap(10, 0))
    }

    @Test
    fun calendarWithoutMonthsDoesNotCrash() {
        val empty = WorldCalendar()
        assertIs<ParsedDate.Problem>(empty.parseDate("3 de março"))
        assertNull(empty.dateAt(0))
        val oneMonth = WorldCalendar(months = listOf(CalMonth("x", "Único", 10)), leap = LeapRule("nao-existe"))
        assertEquals(10, oneMonth.yearLength(4))
    }
}

class CalendarControllerTest {
    private val fs = InMemoryFileStore()
    private val clock = TestClock()
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
    private val services = com.cuboide.escriba.app.EscribaServices(fs, "/lib", "/data", { clock.now }, clock::id, { "2026-11-11" })
    private val c = com.cuboide.escriba.app.EscribaController(services, scope)

    @kotlin.test.AfterTest
    fun tearDown() = scope.cancel()

    @Test
    fun scenesGetDatesAndKeepThemWhenTheCalendarChanges() {
        c.createBook("Livro", "")
        // Sem calendário próprio: vale o comum.
        val fuga = c.withWhen(c.newBeat().copy(title = "Fuga"), "3 de março, 12")
        assertEquals(WorldDate(12, "m3", 3), fuga.date)
        c.saveBeat(fuga)
        // Texto que o calendário comum não entende fica guardado sem data…
        c.saveBeat(c.withWhen(c.newBeat().copy(title = "Feira"), "10 de Vento Norte, 612 E.P."))
        assertNull(c.book!!.outline.beats.last().date)
        // …e ganha data quando o livro passa a ter um calendário que a entende.
        c.createCalendar(CalendarTemplate.Exemplo)
        assertEquals(WorldDate(1111, "a8", 10), c.book!!.outline.beats.last().date)
        // A data já lida continua a mesma (pela id do mês), mesmo que ela não exista no calendário novo.
        assertEquals(WorldDate(12, "m3", 3), c.book!!.outline.beats.first().date)
        assertEquals(listOf("Feira"), c.calendar.timelineOf(c.book!!.outline.beats).dated.map { it.beat.title })

        c.setPresent(WorldDate(1111, "a9", 1))
        val path = c.book!!.path
        c.closeBook()
        c.openBook(path)
        assertEquals("Calendário de Aldamar", c.book!!.calendar?.name)
        assertEquals(WorldDate(1111, "a9", 1), c.calendar.present)
        assertEquals(WorldDate(1111, "a8", 10), services.books.loadOutline(path).beats.last().date)
    }
}
