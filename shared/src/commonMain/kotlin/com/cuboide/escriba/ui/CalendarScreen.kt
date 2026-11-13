package com.cuboide.escriba.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cuboide.escriba.app.EnredoView
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.Section
import com.cuboide.escriba.core.calendar.CalMonth
import com.cuboide.escriba.core.calendar.CalendarTemplate
import com.cuboide.escriba.core.calendar.Era
import com.cuboide.escriba.core.calendar.Festival
import com.cuboide.escriba.core.calendar.LeapRule
import com.cuboide.escriba.core.calendar.Moon
import com.cuboide.escriba.core.calendar.ParsedDate
import com.cuboide.escriba.core.calendar.Season
import com.cuboide.escriba.core.calendar.SunSpec
import com.cuboide.escriba.core.calendar.WorldCalendar
import com.cuboide.escriba.core.calendar.WorldDate
import com.cuboide.escriba.core.calendar.allFullTogether
import com.cuboide.escriba.core.calendar.baseYearLength
import com.cuboide.escriba.core.calendar.dateAt
import com.cuboide.escriba.core.calendar.dayIndex
import com.cuboide.escriba.core.calendar.daysBeforeYear
import com.cuboide.escriba.core.calendar.eclipsesInYear
import com.cuboide.escriba.core.calendar.eclipsesOn
import com.cuboide.escriba.core.calendar.entryDate
import com.cuboide.escriba.core.calendar.festivalsOn
import com.cuboide.escriba.core.calendar.format
import com.cuboide.escriba.core.calendar.formatDuration
import com.cuboide.escriba.core.calendar.monthLength
import com.cuboide.escriba.core.calendar.parseDate
import com.cuboide.escriba.core.calendar.seasonOf
import com.cuboide.escriba.core.calendar.stateOn
import com.cuboide.escriba.core.calendar.timelineOf
import com.cuboide.escriba.core.calendar.weekdayOf
import com.cuboide.escriba.core.calendar.yearLabel
import com.cuboide.escriba.core.universe.AttrRole

/** Aba Calendário do Universo: o mês do mundo como um almanaque, a fita do ano e o céu de cada dia. */
@Composable
fun CalendarScreen(c: EscribaController) {
    val book = c.book ?: return
    val cal = book.calendar
    if (cal == null) {
        CalendarTemplatePicker(c)
        return
    }
    val palette = LocalPalette.current
    val start = cal.present ?: WorldDate(1, cal.months.firstOrNull()?.id.orEmpty(), 1)
    var year by remember { mutableStateOf(start.year) }
    var monthIdx by remember { mutableStateOf(cal.monthIndex(start.monthId).coerceAtLeast(0)) }
    var selectedDay by remember { mutableStateOf<Int?>(start.day.takeIf { cal.present != null }) }
    var editing by remember { mutableStateOf(false) }
    if (monthIdx >= cal.months.size) monthIdx = 0
    val month = cal.months.getOrNull(monthIdx)
    fun stepMonth(delta: Int) {
        if (cal.months.isEmpty()) return
        var m = monthIdx + delta
        if (m < 0) { m = cal.months.size - 1; year-- }
        if (m >= cal.months.size) { m = 0; year++ }
        monthIdx = m
        selectedDay = null
    }

    Column(Modifier.fillMaxSize()) {
        // Cabeçalho: o mês em destaque, o ano ao lado e a navegação.
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Overline(cal.name, accent = true)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(month?.name ?: "Sem meses", style = MaterialTheme.typography.displayMedium, maxLines = 1)
                    Spacer(Modifier.width(14.dp))
                    Text(cal.yearLabel(year), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal), color = palette.muted, modifier = Modifier.padding(bottom = 6.dp))
                }
                Text(
                    cal.present?.let { "Presente da história: ${cal.format(it)}" } ?: "Sem presente da história definido.",
                    style = MaterialTheme.typography.bodySmall, color = palette.muted,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconAction(Icons.Outlined.KeyboardDoubleArrowLeft, "Ano anterior", onClick = { year--; selectedDay = null })
                IconAction(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Mês anterior", onClick = { stepMonth(-1) })
                IconAction(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Próximo mês", onClick = { stepMonth(1) })
                IconAction(Icons.Outlined.KeyboardDoubleArrowRight, "Próximo ano", onClick = { year++; selectedDay = null })
                Spacer(Modifier.width(6.dp))
                TextButton(enabled = cal.present != null, onClick = {
                    cal.present?.let { p -> year = p.year; monthIdx = cal.monthIndex(p.monthId).coerceAtLeast(0); selectedDay = p.day }
                }) { Text("Ir ao presente") }
                CalendarFacts(cal, year)
                Spacer(Modifier.width(6.dp))
                FilledTonalButton(onClick = { editing = true }) { Text("Editar a estrutura") }
            }
        }
        if (cal.moons.isEmpty() && cal.sun == null && cal.months.isNotEmpty()) {
            // Calendários criados antes das luas e do sol (ou feitos sem eles) não ganham céu sozinhos.
            Row(
                Modifier.fillMaxWidth().padding(bottom = 16.dp).clip(Radius.surface).background(palette.accentSoft)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MoonGlyph(0.3, Color(0xFFEDE6D3), 18)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Este calendário ainda não tem luas nem sol: sem eles, não há fases da lua, luz do dia nem eclipses.",
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { c.addSuggestedSky() }) { Text("Acrescentar luas e sol") }
                TextButton(onClick = { editing = true }) { Text("Configurar") }
            }
        }
        if (month == null) {
            EmptyState(Icons.Outlined.CalendarMonth, "Calendário sem meses", "Abra “Editar a estrutura” e acrescente os meses do seu mundo.")
        } else {
            Hairline()
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(top = 20.dp, end = 28.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    val selectedIdx = selectedDay?.let { cal.dayIndex(WorldDate(year, month.id, it)) }
                    YearRibbon(
                        cal, year, monthIdx, selectedIdx,
                        onSelectDay = { idx ->
                            cal.dateAt(idx)?.let { d -> year = d.year; monthIdx = cal.monthIndex(d.monthId).coerceAtLeast(0); selectedDay = d.day }
                        },
                        onSelectMonth = { monthIdx = it; selectedDay = null },
                    )
                    MonthGrid(c, cal, year, monthIdx, selectedDay) { selectedDay = it }
                }
                VHairline()
                Box(Modifier.width(320.dp).fillMaxHeight()) {
                    val day = selectedDay
                    if (day != null && day <= cal.monthLength(year, monthIdx)) DayPanel(c, cal, WorldDate(year, month.id, day))
                    else Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        MoonPhasesRow(size = 16)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Escolha um dia para ver o céu, as festas, as cenas e os aniversários.",
                            style = MaterialTheme.typography.bodyMedium, color = palette.muted, textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
    if (editing) CalendarEditorDialog(cal, onDismiss = { editing = false }) { c.saveCalendar(it); editing = false }
}

/** "(i)": o resumo do calendário (antes uma linha comprida embaixo da grade). */
@Composable
private fun CalendarFacts(cal: WorldCalendar, year: Int) {
    var open by remember { mutableStateOf(false) }
    val eclipseCount = remember(cal, year) { cal.eclipsesInYear(year).size }
    // Próxima vez que todas as luas ficam cheias juntas, contando do presente (ou do começo do ano mostrado).
    val together = remember(cal, open) {
        if (!open) null else cal.allFullTogether(cal.present?.let { cal.dayIndex(it) } ?: cal.daysBeforeYear(year))
    }
    Box {
        IconAction(Icons.Outlined.Info, "Sobre este calendário", onClick = { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Column(Modifier.width(300.dp).padding(horizontal = 18.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Overline("Sobre este calendário")
                Spacer(Modifier.height(2.dp))
                InfoRow("Meses", "${cal.months.size}")
                InfoRow("Dias no ano", "${cal.baseYearLength}" + if (cal.leap != null) " (+1 no bissexto)" else "")
                if (cal.weekdays.isNotEmpty()) InfoRow("Semana", "${cal.weekdays.size} dias")
                InfoRow("Eras", "${cal.eras.size}")
                InfoRow("Estações", "${cal.seasons.size}")
                InfoRow("Festas", "${cal.festivals.size}")
                if (cal.moons.isNotEmpty()) InfoRow("Luas", "${cal.moons.size}")
                cal.sun?.let { InfoRow("Luz no dia mais longo", formatDuration(it.longestHours)) }
                if (cal.sun != null && cal.moons.isNotEmpty()) InfoRow("Eclipses em ${cal.yearLabel(year)}", "$eclipseCount")
                together?.let { idx -> cal.dateAt(idx)?.let { d -> InfoRow("Todas as luas cheias", cal.format(d)) } }
            }
        }
    }
}

@Composable
private fun CalendarTemplatePicker(c: EscribaController) {
    val palette = LocalPalette.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 40.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.clip(RoundedCornerShape(40.dp)).background(Color(0xFF1A2233)).padding(horizontal = 22.dp, vertical = 14.dp)) {
            MoonPhasesRow()
        }
        Spacer(Modifier.height(28.dp))
        Text("O ano do seu mundo", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(10.dp))
        Text(
            "Meses, semana, eras, estações, festas, luas e sol com os nomes do seu mundo. As cenas do Enredo ganham data, " +
                "e a idade dos personagens é contada a partir do presente da história.",
            textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 560.dp), color = palette.muted,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Sem calendário próprio, o livro usa o comum: “3 de março, 12”, “ano 40”. Nada se perde ao criar um.",
            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 560.dp),
            color = palette.muted,
        )
        Spacer(Modifier.height(32.dp))
        Column(Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
            Overline("Comece por um modelo", Modifier.padding(start = 16.dp, bottom = 8.dp))
            Hairline()
            CalendarTemplate.entries.forEach { t ->
                HoverRow(false, onClick = { c.createCalendar(t) }, Modifier.fillMaxWidth().semantics { contentDescription = "Modelo ${t.title}" }) { hovered ->
                    Column(Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text(t.title, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(2.dp))
                        Text(t.description, style = MaterialTheme.typography.bodySmall, color = palette.muted)
                    }
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.padding(end = 16.dp).size(18.dp), tint = if (hovered) palette.accent else palette.muted)
                }
                Hairline()
            }
        }
    }
}

@Composable
private fun MonthGrid(c: EscribaController, cal: WorldCalendar, year: Int, monthIdx: Int, selected: Int?, onSelect: (Int) -> Unit) {
    val palette = LocalPalette.current
    val month = cal.months[monthIdx]
    val length = cal.monthLength(year, monthIdx)
    val book = c.book ?: return
    // Dias com cena datada neste mês.
    val sceneDays = remember(book.outline, cal, year, monthIdx) {
        cal.timelineOf(book.outline.beats).dated.filter { it.date.year == year && it.date.monthId == month.id }.groupBy { it.date.day }
    }
    val columns = if (month.outsideWeek || cal.weekdays.isEmpty()) minOf(length.coerceAtLeast(1), 7) else cal.weekdays.size
    val lead = if (month.outsideWeek) 0 else cal.weekdayOf(WorldDate(year, month.id, 1)) ?: 0
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
            Overline("${month.name} · $length dias", Modifier.weight(1f))
            // Fases principais do mês, como num almanaque.
            if (cal.moons.isNotEmpty()) Text(
                "fases principais nos dias", style = MaterialTheme.typography.labelSmall, color = palette.muted,
            )
        }
        if (month.outsideWeek) {
            Text("Dias fora da semana: não têm dia da semana.", style = MaterialTheme.typography.bodySmall, color = palette.muted, modifier = Modifier.padding(bottom = 8.dp))
        } else Row(Modifier.padding(bottom = 6.dp)) {
            cal.weekdays.forEach { w ->
                Text(
                    w, Modifier.weight(1f).padding(horizontal = 10.dp), style = MaterialTheme.typography.labelMedium,
                    color = palette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        val cells = List(lead) { null } + (1..length).toList()
        cells.chunked(columns).forEach { week ->
            Hairline()
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(Modifier.weight(1f)) {
                        if (day != null) DayCell(cal, WorldDate(year, month.id, day), day == selected, sceneDays[day].orEmpty().size) { onSelect(day) }
                    }
                }
                repeat(columns - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Hairline()
    }
}

@Composable
private fun DayCell(cal: WorldCalendar, date: WorldDate, selected: Boolean, scenes: Int, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val scheme = MaterialTheme.colorScheme
    val index = cal.dayIndex(date)
    val isPresent = cal.present?.let { it.year == date.year && it.monthId == date.monthId && it.day == date.day } == true
    val festivals = cal.festivalsOn(date)
    val seasonStart = cal.seasons.firstOrNull { it.monthId == date.monthId && it.day == date.day }
    val phases = remember(cal, index) {
        if (index == null) emptyList() else cal.moons.mapNotNull { m -> m.keyPhaseOn(index)?.let { m to it } }
    }
    val eclipses = remember(cal, index) { index?.let { cal.eclipsesOn(it) }.orEmpty() }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(
        when {
            selected -> palette.accentSoft
            hovered -> palette.navSelected.copy(alpha = 0.5f)
            else -> Color.Transparent
        },
        tween(Motion.QUICK),
    )
    val summary = buildList {
        festivals.forEach { add("◆ ${it.name}") }
        seasonStart?.let { add("Começa: ${it.name}") }
        phases.forEach { (m, p) -> add("${m.name}: lua ${p.label}") }
        eclipses.forEach { add(it.kind.label + " (${it.moon.name})") }
        if (scenes > 0) add(if (scenes == 1) "1 cena" else "$scenes cenas")
    }
    val cell = @Composable {
        Box(
            Modifier.fillMaxWidth().heightIn(min = 70.dp).padding(2.dp).clip(Radius.surface).background(bg)
                .hoverable(interaction).clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .semantics { contentDescription = "Dia ${date.day}" },
        ) {
            // Começo de estação: um fio na cor dela, no alto da célula.
            if (seasonStart != null) Box(Modifier.align(Alignment.TopStart).padding(horizontal = 8.dp).fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)).background(hexColor(seasonStart.color) ?: scheme.tertiary))
            Row(Modifier.align(Alignment.TopStart).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(if (isPresent) palette.accent else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${date.day}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isPresent || selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = when {
                            isPresent -> scheme.onPrimary
                            selected -> palette.accent
                            else -> scheme.onSurface
                        },
                    )
                }
                // As fases principais ficam coladas ao número do dia, como num almanaque.
                if (phases.isNotEmpty() || eclipses.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        phases.forEach { (m, p) -> MoonGlyph(p.idealFraction(), moonColor(m), 17, eclipse = eclipses.any { it.moon.id == m.id }) }
                        eclipses.filter { e -> phases.none { it.first.id == e.moon.id } }.forEach { e ->
                            MoonGlyph(index?.let { e.moon.stateOn(it).fraction } ?: 0.0, moonColor(e.moon), 17, eclipse = true)
                        }
                    }
                }
            }
            Row(
                Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                if (festivals.isNotEmpty()) Box(Modifier.size(6.dp).rotate(45f).background(palette.accent))
                repeat(minOf(scenes, 3)) { Box(Modifier.size(5.dp).clip(CircleShape).background(palette.info)) }
            }
        }
    }
    if (summary.isEmpty()) cell() else WithTooltip(summary.joinToString("\n")) { cell() }
}

@Composable
private fun DayPanel(c: EscribaController, cal: WorldCalendar, date: WorldDate) {
    val book = c.book ?: return
    val palette = LocalPalette.current
    val scenes = remember(book.outline, cal, date) { cal.timelineOf(book.outline.beats).dated.filter { it.date.year == date.year && it.date.monthId == date.monthId && it.date.day == date.day } }
    val births = remember(book.universe, cal, date) {
        book.universe.entries.filter { e -> cal.entryDate(e, AttrRole.Born)?.let { it.monthId == date.monthId && it.day == date.day } == true }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 28.dp, end = 8.dp, top = 22.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column {
            val weekday = cal.weekdayOf(date)?.let { cal.weekdays[it] }
            Overline(weekday ?: "Fora da semana")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${date.day}", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(cal.month(date.monthId)?.name.orEmpty(), style = MaterialTheme.typography.titleLarge)
                    Text(cal.yearLabel(date.year), style = MaterialTheme.typography.bodySmall, color = palette.muted)
                }
            }
            cal.seasonOf(date)?.let { s ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    StatusDot(hexColor(s.color) ?: palette.muted, 7.dp)
                    Spacer(Modifier.width(7.dp))
                    val starts = cal.seasons.any { it.id == s.id && it.monthId == date.monthId && it.day == date.day }
                    Text(if (starts) "Começa: ${s.name}" else s.name, style = MaterialTheme.typography.bodySmall, color = palette.muted)
                }
            }
        }
        cal.dayIndex(date)?.let { idx ->
            if (cal.sun != null || cal.moons.isNotEmpty()) {
                Hairline()
                Overline("Céu")
                DaySky(cal, idx)
            }
        }
        val festivals = cal.festivalsOn(date)
        if (festivals.isNotEmpty()) {
            Hairline()
            Overline("Festas")
            festivals.forEach { f ->
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).rotate(45f).background(palette.accent))
                        Spacer(Modifier.width(10.dp))
                        Text(f.name, style = MaterialTheme.typography.titleMedium)
                    }
                    if (f.description.isNotBlank()) Text(f.description, style = MaterialTheme.typography.bodySmall, color = palette.muted, modifier = Modifier.padding(start = 17.dp, top = 2.dp))
                }
            }
        }
        if (scenes.isNotEmpty()) {
            Hairline()
            Overline("Cenas neste dia")
            scenes.forEach { s ->
                HoverRow(false, onClick = { c.enredoView = EnredoView.Linha; c.goTo(Section.Enredo) }, Modifier.fillMaxWidth()) {
                    StatusDot(palette.info, 6.dp, Modifier.padding(start = 6.dp))
                    Text(s.beat.title.ifBlank { "Cena sem título" }, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }
        }
        if (births.isNotEmpty()) {
            Hairline()
            Overline("Aniversários")
            births.forEach { e ->
                HoverRow(false, onClick = { c.openUniverseEntry(e.id) }, Modifier.fillMaxWidth()) {
                    Spacer(Modifier.width(4.dp))
                    EntryAvatar(c, e, 22)
                    Text(e.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }
        }
        Hairline()
        if (cal.present == date) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(palette.accent, 7.dp)
                Spacer(Modifier.width(8.dp))
                Text("É o presente da história.", style = MaterialTheme.typography.bodyMedium, color = palette.accent)
            }
        } else {
            TextButton(onClick = { c.setPresent(date) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp)) {
                Text("Fazer deste dia o presente da história")
            }
        }
    }
}

@Composable
private fun Hint2(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = LocalPalette.current.muted)
}

// ---------------------------------------------------------------- editor da estrutura

private val SEASON_COLORS = listOf("#6FA35B", "#E0A33A", "#B8643A", "#5B86B0", "#8E6BB8", "#4E9C96")

/** Edita uma cópia do calendário; só grava em "Salvar". Cada parte da estrutura tem a sua página. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarEditorDialog(initial: WorldCalendar, onDismiss: () -> Unit, onSave: (WorldCalendar) -> Unit) {
    val palette = LocalPalette.current
    var cal by remember { mutableStateOf(initial) }
    var presentText by remember { mutableStateOf(initial.present?.let { initial.format(it) }.orEmpty()) }
    var page by remember { mutableStateOf(0) }
    fun newId(prefix: String): String = "$prefix-" + kotlin.random.Random.nextLong().toULong().toString(36)
    val presentParsed = cal.parseDate(presentText)
    val pages = listOf(
        "Geral" to null, "Meses" to cal.months.size, "Semana" to cal.weekdays.size, "Ano bissexto" to null,
        "Eras" to cal.eras.size, "Estações" to cal.seasons.size, "Festas" to cal.festivals.size, "Luas" to cal.moons.size,
        "Sol e eclipses" to null, "O presente" to null,
    )

    // Dialog próprio: o AlertDialog do Material limita a largura a 560dp e espreme as linhas de estações e festas.
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = Radius.sheet, color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 16.dp) {
            Column(Modifier.widthIn(max = 980.dp).fillMaxWidth(0.92f)) {
                Row(Modifier.fillMaxWidth().padding(start = 28.dp, end = 16.dp, top = 22.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Overline(cal.name.ifBlank { "Calendário" }, accent = true)
                        Spacer(Modifier.height(4.dp))
                        Text("A estrutura do calendário", style = MaterialTheme.typography.headlineMedium)
                    }
                    IconAction(Icons.Outlined.Close, "Fechar sem salvar", onClick = onDismiss)
                }
                Hairline()
                Row(Modifier.fillMaxWidth().height(560.dp)) {
                    Column(Modifier.width(210.dp).fillMaxHeight().padding(10.dp)) {
                        pages.forEachIndexed { i, (label, count) ->
                            HoverRow(i == page, onClick = { page = i }, Modifier.fillMaxWidth()) {
                                Text(
                                    label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 9.dp),
                                    fontWeight = if (i == page) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (i == page) MaterialTheme.colorScheme.onSurface else palette.muted,
                                )
                                if (count != null) Text("$count", style = MaterialTheme.typography.labelMedium, color = palette.muted, modifier = Modifier.padding(end = 12.dp))
                            }
                        }
                    }
                    VHairline()
                    Column(
                        Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        when (page) {
                            0 -> {
                                OutlinedTextField(cal.name, { cal = cal.copy(name = it) }, label = { Text("Nome do calendário") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                Hint2("Renomear um mês não perde nenhuma data das cenas. Apagar um mês deixa as datas dele sem lugar (a Linha do tempo avisa).")
                                Spacer(Modifier.height(4.dp))
                                Overline("Resumo")
                                InfoRow("Meses", "${cal.months.size}")
                                InfoRow("Dias no ano", "${cal.baseYearLength}" + if (cal.leap != null) " (+1 no bissexto)" else "")
                                InfoRow("Dias da semana", "${cal.weekdays.size}")
                                InfoRow("Eras · estações · festas", "${cal.eras.size} · ${cal.seasons.size} · ${cal.festivals.size}")
                                InfoRow("Luas", "${cal.moons.size}")
                                InfoRow("Sol", if (cal.sun != null) "acompanhando a luz do dia" else "sem sol")
                            }
                            1 -> EditorSection("Meses", onAdd = { cal = cal.copy(months = cal.months + CalMonth(newId("m"), "Mês ${cal.months.size + 1}", 30)) }) {
                                cal.months.forEachIndexed { i, m ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("${i + 1}", style = MaterialTheme.typography.titleLarge, color = palette.muted, modifier = Modifier.width(26.dp), textAlign = TextAlign.End)
                                        SmallField(m.name, "Nome", Modifier.weight(1f)) { v -> cal = cal.copy(months = cal.months.map { if (it.id == m.id) it.copy(name = v) else it }) }
                                        NumberField(m.days, "Dias", 1..999) { v -> cal = cal.copy(months = cal.months.map { if (it.id == m.id) it.copy(days = v) else it }) }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(m.outsideWeek, { v -> cal = cal.copy(months = cal.months.map { if (it.id == m.id) it.copy(outsideWeek = v) else it }) })
                                            Text("fora da semana", style = MaterialTheme.typography.bodySmall)
                                        }
                                        IconAction(Icons.Outlined.Close, "Apagar mês ${i + 1}", onClick = { cal = cal.copy(months = cal.months.filterNot { it.id == m.id }) })
                                    }
                                }
                            }
                            2 -> EditorSection("Dias da semana", onAdd = { cal = cal.copy(weekdays = cal.weekdays + "Dia ${cal.weekdays.size + 1}") }) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    cal.weekdays.forEachIndexed { i, w ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            SmallField(w, "Dia ${i + 1}", Modifier.width(170.dp)) { v -> cal = cal.copy(weekdays = cal.weekdays.toMutableList().also { it[i] = v }) }
                                            IconAction(Icons.Outlined.Close, "Apagar dia da semana ${i + 1}", onClick = { cal = cal.copy(weekdays = cal.weekdays.filterIndexed { j, _ -> j != i }) })
                                        }
                                    }
                                }
                            }
                            3 -> EditorSection("Ano bissexto", onAdd = null) {
                                val leap = cal.leap
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Checkbox(leap != null, { on -> cal = cal.copy(leap = if (on) LeapRule(cal.months.firstOrNull()?.id.orEmpty()) else null) })
                                    Text("Um mês ganha um dia a cada", style = MaterialTheme.typography.bodyMedium)
                                    if (leap != null) {
                                        NumberField(leap.every, "anos", 1..1000) { v -> cal = cal.copy(leap = leap.copy(every = v)) }
                                        Text("no mês", style = MaterialTheme.typography.bodyMedium)
                                        MonthPicker(cal.months, leap.monthId) { id -> cal = cal.copy(leap = leap.copy(monthId = id)) }
                                    }
                                }
                                if (leap != null && leap.skipEvery > 0) Hint2("Exceção: a cada ${leap.skipEvery} anos não há bissexto" + if (leap.keepEvery > 0) ", a não ser a cada ${leap.keepEvery}." else ".")
                            }
                            4 -> EditorSection("Eras", onAdd = {
                                val start = (cal.eras.maxOfOrNull { it.startYear } ?: 0) + 100
                                cal = cal.copy(eras = cal.eras + Era(newId("e"), "Nova era", "N.E.", start))
                            }) {
                                cal.eras.sortedBy { it.startYear }.forEach { e ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SmallField(e.name, "Nome", Modifier.weight(1f)) { v -> cal = cal.copy(eras = cal.eras.map { if (it.id == e.id) it.copy(name = v) else it }) }
                                        SmallField(e.abbr, "Sigla", Modifier.width(90.dp)) { v -> cal = cal.copy(eras = cal.eras.map { if (it.id == e.id) it.copy(abbr = v) else it }) }
                                        NumberField(e.startYear, "Começa no ano", -1_000_000..1_000_000) { v -> cal = cal.copy(eras = cal.eras.map { if (it.id == e.id) it.copy(startYear = v) else it }) }
                                        IconAction(Icons.Outlined.Close, "Apagar era ${e.name}", onClick = { cal = cal.copy(eras = cal.eras.filterNot { it.id == e.id }) })
                                    }
                                }
                                if (cal.eras.isNotEmpty()) Hint2("O ano de começo é contado sem era (o ano 1 da primeira era costuma ser o 1).")
                            }
                            5 -> EditorSection("Estações", onAdd = {
                                cal = cal.copy(seasons = cal.seasons + Season(newId("s"), "Estação", SEASON_COLORS[cal.seasons.size % SEASON_COLORS.size], cal.months.firstOrNull()?.id.orEmpty(), 1))
                            }) {
                                cal.seasons.forEach { s ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        ColorDot(s.color, "Trocar cor da estação") {
                                            val next = SEASON_COLORS[(SEASON_COLORS.indexOf(s.color) + 1).mod(SEASON_COLORS.size)]
                                            cal = cal.copy(seasons = cal.seasons.map { if (it.id == s.id) it.copy(color = next) else it })
                                        }
                                        SmallField(s.name, "Nome", Modifier.weight(1f)) { v -> cal = cal.copy(seasons = cal.seasons.map { if (it.id == s.id) it.copy(name = v) else it }) }
                                        Text("começa em", style = MaterialTheme.typography.bodySmall)
                                        NumberField(s.day, "Dia", 1..999) { v -> cal = cal.copy(seasons = cal.seasons.map { if (it.id == s.id) it.copy(day = v) else it }) }
                                        MonthPicker(cal.months, s.monthId) { id -> cal = cal.copy(seasons = cal.seasons.map { if (it.id == s.id) it.copy(monthId = id) else it }) }
                                        IconAction(Icons.Outlined.Close, "Apagar estação ${s.name}", onClick = { cal = cal.copy(seasons = cal.seasons.filterNot { it.id == s.id }) })
                                    }
                                }
                            }
                            6 -> EditorSection("Festas", onAdd = {
                                cal = cal.copy(festivals = cal.festivals + Festival(newId("f"), "Nova festa", cal.months.firstOrNull()?.id.orEmpty(), 1))
                            }) {
                                cal.festivals.forEach { f ->
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            SmallField(f.name, "Nome", Modifier.weight(1f)) { v -> cal = cal.copy(festivals = cal.festivals.map { if (it.id == f.id) it.copy(name = v) else it }) }
                                            NumberField(f.day, "Dia", 1..999) { v -> cal = cal.copy(festivals = cal.festivals.map { if (it.id == f.id) it.copy(day = v) else it }) }
                                            MonthPicker(cal.months, f.monthId) { id -> cal = cal.copy(festivals = cal.festivals.map { if (it.id == f.id) it.copy(monthId = id) else it }) }
                                            IconAction(Icons.Outlined.Close, "Apagar festa ${f.name}", onClick = { cal = cal.copy(festivals = cal.festivals.filterNot { it.id == f.id }) })
                                        }
                                        SmallField(f.description, "O que acontece nesse dia", Modifier.fillMaxWidth()) { v ->
                                            cal = cal.copy(festivals = cal.festivals.map { if (it.id == f.id) it.copy(description = v) else it })
                                        }
                                        Hairline(Modifier.padding(top = 6.dp))
                                    }
                                }
                            }
                            7 -> EditorSection("Luas", onAdd = {
                                val color = MOON_COLORS[cal.moons.size % MOON_COLORS.size]
                                cal = cal.copy(moons = cal.moons + Moon(newId("l"), "Nova lua", color, 30.0, cal.present?.let { cal.dayIndex(it)?.toDouble() } ?: 0.0))
                            }) {
                                cal.moons.forEach { m ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            Modifier.size(34.dp).clip(CircleShape).clickable {
                                                val next = MOON_COLORS[(MOON_COLORS.indexOf(m.color) + 1).mod(MOON_COLORS.size)]
                                                cal = cal.copy(moons = cal.moons.map { if (it.id == m.id) it.copy(color = next) else it })
                                            }.semantics { contentDescription = "Trocar cor da lua" },
                                            contentAlignment = Alignment.Center,
                                        ) { MoonGlyph(0.4, moonColor(m), 30) }
                                        SmallField(m.name, "Nome", Modifier.weight(1f)) { v -> cal = cal.copy(moons = cal.moons.map { if (it.id == m.id) it.copy(name = v) else it }) }
                                        DecimalField(m.cycleDays, "Ciclo (dias)", 1.5..10_000.0) { v -> cal = cal.copy(moons = cal.moons.map { if (it.id == m.id) it.copy(cycleDays = v) else it }) }
                                        DateAnchorField(cal, m.epochDay, "Uma lua nova em", Modifier.width(230.dp)) { v -> cal = cal.copy(moons = cal.moons.map { if (it.id == m.id) it.copy(epochDay = v) else it }) }
                                        IconAction(Icons.Outlined.Close, "Apagar lua ${m.name}", onClick = { cal = cal.copy(moons = cal.moons.filterNot { it.id == m.id }) })
                                    }
                                }
                                if (cal.moons.isNotEmpty()) Hint2("O ciclo é o tempo de uma lua nova até a seguinte. A data de uma lua nova qualquer ancora as fases. Clique na lua para trocar a cor.")
                            }
                            8 -> EditorSection("Sol e eclipses", onAdd = null) {
                                val sun = cal.sun
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Checkbox(sun != null, { on ->
                                        val firstMonth = cal.months.getOrNull(cal.months.size / 3)?.id ?: cal.months.firstOrNull()?.id.orEmpty()
                                        cal = cal.copy(sun = if (on) SunSpec(firstMonth, 1) else null)
                                    })
                                    Text("Acompanhar a luz do dia", style = MaterialTheme.typography.bodyMedium)
                                }
                                if (sun != null) {
                                    Overline("Luz do dia", Modifier.padding(top = 6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Dia mais longo:", style = MaterialTheme.typography.bodySmall)
                                        NumberField(sun.longestDay, "Dia", 1..999) { v -> cal = cal.copy(sun = sun.copy(longestDay = v)) }
                                        MonthPicker(cal.months, sun.longestMonthId) { id -> cal = cal.copy(sun = sun.copy(longestMonthId = id)) }
                                    }
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        DecimalField(sun.longestHours, "Luz no mais longo (h)", 0.0..sun.dayHours) { v -> cal = cal.copy(sun = sun.copy(longestHours = v)) }
                                        DecimalField(sun.shortestHours, "Luz no mais curto (h)", 0.0..sun.dayHours) { v -> cal = cal.copy(sun = sun.copy(shortestHours = v)) }
                                        DecimalField(sun.dayHours, "Horas do dia", 1.0..1000.0) { v -> cal = cal.copy(sun = sun.copy(dayHours = v)) }
                                    }
                                    Overline("Eclipses", Modifier.padding(top = 6.dp))
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        DecimalField(sun.eclipseSeasonDays, "Temporada de eclipses (dias)", 0.0..100_000.0) { v -> cal = cal.copy(sun = sun.copy(eclipseSeasonDays = v)) }
                                        DecimalField(sun.eclipseWindowDays, "Janela (dias)", 0.0..100_000.0) { v -> cal = cal.copy(sun = sun.copy(eclipseWindowDays = v)) }
                                        DateAnchorField(cal, sun.eclipseEpochDay, "Centro de uma temporada", Modifier.width(230.dp)) { v -> cal = cal.copy(sun = sun.copy(eclipseEpochDay = v)) }
                                    }
                                    Hint2(
                                        "Lua nova perto do centro de uma temporada escurece o sol; lua cheia perto do centro fica avermelhada. " +
                                            "Temporada 0 desliga os eclipses.",
                                    )
                                }
                            }
                            else -> EditorSection("O presente da história", onAdd = null) {
                                SmallField(presentText, "ex.: 12 de ${cal.months.firstOrNull()?.name ?: "mês"}, ${cal.yearLabel(1)}", Modifier.fillMaxWidth()) { presentText = it }
                                when (presentParsed) {
                                    is ParsedDate.Ok -> Hint2("→ ${cal.format(presentParsed.date)}. É de onde a Linha do tempo parte e a idade das fichas é contada.")
                                    is ParsedDate.Problem -> Text(presentParsed.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                    ParsedDate.Empty -> Hint2("Em branco: a história não tem um “hoje” definido.")
                                }
                            }
                        }
                    }
                }
                Hairline()
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (presentParsed is ParsedDate.Problem) {
                        Text("O presente da história tem um problema: ${presentParsed.message}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                    } else Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = presentParsed !is ParsedDate.Problem,
                        onClick = { onSave(cal.copy(present = (presentParsed as? ParsedDate.Ok)?.date)) },
                    ) { Text("Salvar") }
                }
            }
        }
    }
}

@Composable
private fun ColorDot(hex: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(22.dp).clip(CircleShape).background(hexColor(hex) ?: Color.Gray)
            .border(1.dp, LocalPalette.current.hairline, CircleShape)
            .clickable(onClick = onClick).semantics { contentDescription = description },
    )
}

@Composable
private fun EditorSection(title: String, onAdd: (() -> Unit)?, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            if (onAdd != null) TextButton(onClick = onAdd) {
                Icon(Icons.Outlined.Add, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Acrescentar")
            }
        }
        content()
    }
}

@Composable
private fun SmallField(value: String, label: String, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true, modifier = modifier, textStyle = MaterialTheme.typography.bodyMedium)
}

/** Campo numérico que aceita digitação parcial (vazio, sinal de menos) sem pular para o limite. */
@Composable
private fun NumberField(value: Int, label: String, range: IntRange, onChange: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        text,
        { t -> if (t.isEmpty() || t == "-" || t.matches(Regex("-?\\d{0,7}"))) { text = t; t.toIntOrNull()?.takeIf { it in range }?.let(onChange) } },
        label = { Text(label) }, singleLine = true, modifier = Modifier.width(110.dp), textStyle = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun MonthPicker(months: List<CalMonth>, selectedId: String, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) { Text((months.firstOrNull { it.id == selectedId }?.name ?: "(mês apagado)") + " ▾") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            months.forEach { m -> DropdownMenuItem(text = { Text(m.name) }, onClick = { open = false; onPick(m.id) }) }
        }
    }
}

private val MOON_COLORS = listOf("#E8E2D0", "#D9825B", "#9DB7D5", "#B9A5D6", "#9CC59A")

private val DECIMAL_INPUT = Regex("\\d{0,6}([.,]\\d{0,6})?")

/** Campo decimal (aceita vírgula) que tolera digitação parcial. */
@Composable
private fun DecimalField(value: Double, label: String, range: ClosedFloatingPointRange<Double>, onChange: (Double) -> Unit) {
    var text by remember(value) { mutableStateOf(formatDecimal(value)) }
    OutlinedTextField(
        text,
        { t ->
            if (t.isEmpty() || t.matches(DECIMAL_INPUT)) {
                text = t
                t.replace(',', '.').toDoubleOrNull()?.takeIf { it in range }?.let(onChange)
            }
        },
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }, singleLine = true,
        modifier = Modifier.width(150.dp), textStyle = MaterialTheme.typography.bodyMedium,
    )
}

private fun formatDecimal(v: Double): String =
    if (v == kotlin.math.floor(v)) v.toLong().toString() else v.toString().replace('.', ',')

/** Escolhe um dia pelo texto ("12 de março, 2000"); guarda o índice do meio-dia desse dia. */
@Composable
private fun DateAnchorField(cal: WorldCalendar, value: Double, label: String, modifier: Modifier, onChange: (Double) -> Unit) {
    val initial = remember(cal.months) { cal.dateAt(kotlin.math.floor(value).toLong())?.let { cal.format(it) }.orEmpty() }
    var text by remember { mutableStateOf(initial) }
    val parsed = cal.parseDate(text)
    OutlinedTextField(
        text,
        { t ->
            text = t
            (cal.parseDate(t) as? ParsedDate.Ok)?.let { ok -> cal.dayIndex(ok.date)?.let { onChange(it + 0.5) } }
        },
        label = { Text(label, maxLines = 1) }, singleLine = true, modifier = modifier, textStyle = MaterialTheme.typography.bodyMedium,
        isError = parsed is ParsedDate.Problem,
    )
}
