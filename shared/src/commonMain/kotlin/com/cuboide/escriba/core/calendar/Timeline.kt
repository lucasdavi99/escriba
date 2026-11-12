package com.cuboide.escriba.core.calendar

import com.cuboide.escriba.core.model.Beat
import com.cuboide.escriba.core.model.UniverseEntry
import com.cuboide.escriba.core.universe.AttrRole
import com.cuboide.escriba.core.universe.attrByRole

/** Cena com data, já posicionada no calendário. */
data class DatedBeat(val beat: Beat, val date: WorldDate, val index: Long)

/** Cena que tem "Quando" escrito mas não cabe no calendário (mês apagado, dia que não existe, texto ilegível). */
data class BrokenBeat(val beat: Beat, val reason: String)

data class Timeline(val dated: List<DatedBeat>, val broken: List<BrokenBeat>, val undated: List<Beat>)

/** Ordena as cenas pela data. Empates mantêm a ordem do roteiro. */
fun WorldCalendar.timelineOf(beats: List<Beat>): Timeline {
    val dated = mutableListOf<DatedBeat>()
    val broken = mutableListOf<BrokenBeat>()
    val undated = mutableListOf<Beat>()
    for (b in beats) {
        val date = b.date
        when {
            date != null -> {
                val idx = dayIndex(date)
                if (idx != null) dated += DatedBeat(b, date, idx)
                else broken += BrokenBeat(b, if (month(date.monthId) == null) "o mês desta data foi apagado do calendário" else "o dia não existe mais no mês")
            }
            b.whenText.isNotBlank() -> when (val p = parseDate(b.whenText)) {
                is ParsedDate.Ok -> dayIndex(p.date)?.let { dated += DatedBeat(b, p.date, it) } ?: run { broken += BrokenBeat(b, "data fora do calendário") }
                is ParsedDate.Problem -> broken += BrokenBeat(b, p.message)
                ParsedDate.Empty -> undated += b
            }
            else -> undated += b
        }
    }
    return Timeline(dated.sortedBy { it.index }, broken, undated)
}

/** Data do atributo de papel [role] (Nasceu, Morreu) lida neste calendário. */
fun WorldCalendar.entryDate(entry: UniverseEntry, role: AttrRole): WorldDate? =
    (parseDate(entry.attrByRole(role)) as? ParsedDate.Ok)?.date

/** Idade da ficha numa data: "34 anos", "ainda não nasceu", "morreu aos 61". `null` se não há data de nascimento. */
fun WorldCalendar.ageLabel(entry: UniverseEntry, at: WorldDate): String? {
    val birth = entryDate(entry, AttrRole.Born) ?: return null
    val age = ageAt(birth, at) ?: return null
    val atIdx = dayIndex(at)
    val birthIdx = dayIndex(birth)
    if (atIdx != null && birthIdx != null && atIdx < birthIdx) return "ainda não nasceu"
    val death = entryDate(entry, AttrRole.Died)
    val deathIdx = death?.let { dayIndex(it) }
    if (death != null && deathIdx != null && atIdx != null && atIdx > deathIdx) {
        val final = ageAt(birth, death)
        return if (final != null) "morreu aos $final" else "já morreu"
    }
    return if (age == 1) "1 ano" else "$age anos"
}
