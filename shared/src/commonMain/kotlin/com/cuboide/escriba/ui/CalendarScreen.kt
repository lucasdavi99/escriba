package com.cuboide.escriba.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.EnredoView
import com.cuboide.escriba.app.Section
import com.cuboide.escriba.core.calendar.CalMonth
import com.cuboide.escriba.core.calendar.CalendarTemplate
import com.cuboide.escriba.core.calendar.Era
import com.cuboide.escriba.core.calendar.Festival
import com.cuboide.escriba.core.calendar.LeapRule
import com.cuboide.escriba.core.calendar.ParsedDate
import com.cuboide.escriba.core.calendar.Season
import com.cuboide.escriba.core.calendar.WorldCalendar
import com.cuboide.escriba.core.calendar.WorldDate
import com.cuboide.escriba.core.calendar.baseYearLength
import com.cuboide.escriba.core.calendar.entryDate
import com.cuboide.escriba.core.calendar.festivalsOn
import com.cuboide.escriba.core.calendar.format
import com.cuboide.escriba.core.calendar.monthLength
import com.cuboide.escriba.core.calendar.parseDate
import com.cuboide.escriba.core.calendar.seasonOf
import com.cuboide.escriba.core.calendar.timelineOf
import com.cuboide.escriba.core.calendar.weekdayOf
import com.cuboide.escriba.core.calendar.yearLabel
import com.cuboide.escriba.core.model.EntryKind
import com.cuboide.escriba.core.universe.AttrRole

/** Aba Calendário do Universo: escolher um modelo, navegar pelos meses e editar a estrutura. */
@Composable
fun CalendarScreen(c: EscribaController) {
    val book = c.book ?: return
    val cal = book.calendar
    if (cal == null) {
        CalendarTemplatePicker(c)
        return
    }
    val start = cal.present ?: WorldDate(1, cal.months.firstOrNull()?.id.orEmpty(), 1)
    var year by remember { mutableStateOf(start.year) }
    var monthIdx by remember { mutableStateOf(cal.monthIndex(start.monthId).coerceAtLeast(0)) }
    var selectedDay by remember { mutableStateOf<Int?>(start.day.takeIf { cal.present != null }) }
    var editing by remember { mutableStateOf(false) }
    if (monthIdx >= cal.months.size) monthIdx = 0
    val month = cal.months.getOrNull(monthIdx)

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(cal.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    cal.present?.let { "Presente da história: ${cal.format(it)}" } ?: "Sem presente da história definido.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconAction(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Ano anterior", onClick = { year--; selectedDay = null })
            Text("Ano ${cal.yearLabel(year)}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { contentDescription = "Ano mostrado" })
            IconAction(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Próximo ano", onClick = { year++; selectedDay = null })
            OutlinedButton(enabled = cal.present != null, onClick = {
                cal.present?.let { p -> year = p.year; monthIdx = cal.monthIndex(p.monthId).coerceAtLeast(0); selectedDay = p.day }
            }) { Text("Ir ao presente") }
            Button(onClick = { editing = true }) { Text("Editar a estrutura") }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            cal.months.forEachIndexed { i, m ->
                FilterChip(selected = i == monthIdx, onClick = { monthIdx = i; selectedDay = null }, label = { Text(m.name) })
            }
        }
        if (month == null) {
            EmptyState(Icons.Outlined.CalendarMonth, "Calendário sem meses", "Abra “Editar a estrutura” e acrescente os meses do seu mundo.")
        } else {
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    MonthGrid(c, cal, year, monthIdx, selectedDay) { selectedDay = it }
                }
                Box(Modifier.width(320.dp).fillMaxHeight()) {
                    val day = selectedDay
                    if (day != null && day <= cal.monthLength(year, monthIdx)) DayPanel(c, cal, WorldDate(year, month.id, day))
                    else Hint2("Clique num dia para ver festas, estação e cenas.")
                }
            }
        }
        Text(
            buildString {
                append("${cal.months.size} meses · ${cal.baseYearLength} dias")
                if (cal.leap != null) append(" (+1 no bissexto)")
                if (cal.weekdays.isNotEmpty()) append(" · semana de ${cal.weekdays.size}")
                append(" · ${cal.eras.size} era(s) · ${cal.seasons.size} estação(ões) · ${cal.festivals.size} festa(s)")
            },
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (editing) CalendarEditorDialog(cal, onDismiss = { editing = false }) { c.saveCalendar(it); editing = false }
}

@Composable
private fun CalendarTemplatePicker(c: EscribaController) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(48.dp), tint = LocalPalette.current.accent)
        Spacer(Modifier.height(12.dp))
        Text("O ano do seu mundo", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            "Meses, semana, eras, estações e festas com os nomes do seu mundo. As cenas do Enredo ganham data, " +
                "e a idade dos personagens é contada a partir do presente da história.",
            textAlign = TextAlign.Center, modifier = Modifier.width(560.dp), color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Sem calendário próprio, o livro usa o comum: “3 de março, 12”, “ano 40”. Nada se perde ao criar um.",
            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.width(560.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CalendarTemplate.entries.forEach { t ->
                Surface(
                    Modifier.width(220.dp).clip(RoundedCornerShape(14.dp)).clickable { c.createCalendar(t) }
                        .semantics { contentDescription = "Modelo ${t.title}" },
                    shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(t.title.uppercase(), style = MaterialTheme.typography.labelMedium, color = LocalPalette.current.accent, fontWeight = FontWeight.SemiBold)
                        Text(t.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(c: EscribaController, cal: WorldCalendar, year: Int, monthIdx: Int, selected: Int?, onSelect: (Int) -> Unit) {
    val month = cal.months[monthIdx]
    val length = cal.monthLength(year, monthIdx)
    val book = c.book ?: return
    // Dias com cena datada neste mês.
    val sceneDays = remember(book.outline, cal, year, monthIdx) {
        cal.timelineOf(book.outline.beats).dated.filter { it.date.year == year && it.date.monthId == month.id }.groupBy { it.date.day }
    }
    val columns = if (month.outsideWeek || cal.weekdays.isEmpty()) minOf(length.coerceAtLeast(1), 7) else cal.weekdays.size
    val lead = if (month.outsideWeek) 0 else cal.weekdayOf(WorldDate(year, month.id, 1)) ?: 0
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(month.name, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(10.dp))
            Text("$length dias · ${cal.yearLabel(year)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (month.outsideWeek) Hint2("Dias fora da semana: não têm dia da semana.")
        else Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            cal.weekdays.forEach { w ->
                Text(w.uppercase(), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        val cells = List(lead) { null } + (1..length).toList()
        cells.chunked(columns).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { day ->
                    Box(Modifier.weight(1f)) {
                        if (day != null) DayCell(cal, WorldDate(year, month.id, day), day == selected, sceneDays[day].orEmpty().size) { onSelect(day) }
                    }
                }
                repeat(columns - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DayCell(cal: WorldCalendar, date: WorldDate, selected: Boolean, scenes: Int, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val isPresent = cal.present?.let { it.year == date.year && it.monthId == date.monthId && it.day == date.day } == true
    val festival = cal.festivalsOn(date).isNotEmpty()
    val seasonStart = cal.seasons.firstOrNull { it.monthId == date.monthId && it.day == date.day }
    Surface(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick)
            .semantics { contentDescription = "Dia ${date.day}" },
        shape = RoundedCornerShape(10.dp),
        color = if (isPresent) scheme.primaryContainer else scheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant),
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${date.day}", style = MaterialTheme.typography.bodyMedium, fontWeight = if (isPresent) FontWeight.Bold else FontWeight.Normal)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (seasonStart != null) Box(Modifier.size(8.dp).clip(CircleShape).background(hexColor(seasonStart.color) ?: scheme.tertiary))
                if (festival) Text("◆", style = MaterialTheme.typography.labelSmall, color = LocalPalette.current.accent)
                if (scenes > 0) Text("$scenes cena(s)", style = MaterialTheme.typography.labelSmall, color = scheme.primary, maxLines = 1)
            }
        }
    }
}

@Composable
private fun DayPanel(c: EscribaController, cal: WorldCalendar, date: WorldDate) {
    val book = c.book ?: return
    val scheme = MaterialTheme.colorScheme
    val scenes = remember(book.outline, cal, date) { cal.timelineOf(book.outline.beats).dated.filter { it.date.year == date.year && it.date.monthId == date.monthId && it.date.day == date.day } }
    val births = remember(book.universe, cal, date) {
        book.universe.entries.filter { e -> cal.entryDate(e, AttrRole.Born)?.let { it.monthId == date.monthId && it.day == date.day } == true }
    }
    Surface(Modifier.fillMaxSize(), shape = RoundedCornerShape(16.dp), color = scheme.surfaceContainerLow) {
        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(cal.format(date), style = MaterialTheme.typography.titleMedium)
            val weekday = cal.weekdayOf(date)?.let { cal.weekdays[it] }
            Text(listOfNotNull(weekday ?: "fora da semana", cal.seasonOf(date)?.name).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            cal.seasons.filter { it.monthId == date.monthId && it.day == date.day }.forEach { Text("Começa: ${it.name}", style = MaterialTheme.typography.bodySmall) }
            cal.festivalsOn(date).forEach { f ->
                Text("◆ ${f.name}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                if (f.description.isNotBlank()) Text(f.description, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            if (scenes.isNotEmpty()) {
                HorizontalDivider()
                Text("CENAS NESTE DIA", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                scenes.forEach { s ->
                    Text(
                        "• ${s.beat.title.ifBlank { "Cena sem título" }}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable { c.enredoView = EnredoView.Linha; c.goTo(Section.Enredo) },
                    )
                }
            }
            if (births.isNotEmpty()) {
                HorizontalDivider()
                Text("ANIVERSÁRIOS", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                births.forEach { e -> Text("• ${e.name}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.clickable { c.openUniverseEntry(e.id) }) }
            }
            HorizontalDivider()
            if (cal.present == date) Text("É o presente da história.", style = MaterialTheme.typography.bodySmall, color = scheme.primary)
            else TextButton(onClick = { c.setPresent(date) }) { Text("Fazer deste dia o presente da história") }
        }
    }
}

@Composable
private fun Hint2(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// ---------- editor da estrutura ----------

private val SEASON_COLORS = listOf("#6FA35B", "#E0A33A", "#B8643A", "#5B86B0", "#8E6BB8", "#4E9C96")

/** Edita uma cópia do calendário; só grava em "Salvar". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarEditorDialog(initial: WorldCalendar, onDismiss: () -> Unit, onSave: (WorldCalendar) -> Unit) {
    var cal by remember { mutableStateOf(initial) }
    var presentText by remember { mutableStateOf(initial.present?.let { initial.format(it) }.orEmpty()) }
    fun newId(prefix: String): String = "$prefix-" + kotlin.random.Random.nextLong().toULong().toString(36)
    val presentParsed = cal.parseDate(presentText)

    // Dialog próprio: o AlertDialog do Material limita a largura a 560dp e espreme as linhas de estações e festas.
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 6.dp) {
            Column(Modifier.widthIn(max = 880.dp).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("A estrutura do calendário", style = MaterialTheme.typography.headlineSmall)
                Column(Modifier.fillMaxWidth().heightIn(max = 620.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(cal.name, { cal = cal.copy(name = it) }, label = { Text("Nome do calendário") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Hint2("Renomear um mês não perde nenhuma data das cenas. Apagar um mês deixa as datas dele sem lugar (a Linha do tempo avisa).")

                    EditorSection("Meses (${cal.months.size})", onAdd = { cal = cal.copy(months = cal.months + CalMonth(newId("m"), "Mês ${cal.months.size + 1}", 30)) }) {
                        cal.months.forEachIndexed { i, m ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    EditorSection("Dias da semana (${cal.weekdays.size})", onAdd = { cal = cal.copy(weekdays = cal.weekdays + "Dia ${cal.weekdays.size + 1}") }) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            cal.weekdays.forEachIndexed { i, w ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SmallField(w, "Dia", Modifier.width(150.dp)) { v -> cal = cal.copy(weekdays = cal.weekdays.toMutableList().also { it[i] = v }) }
                                    IconAction(Icons.Outlined.Close, "Apagar dia da semana ${i + 1}", onClick = { cal = cal.copy(weekdays = cal.weekdays.filterIndexed { j, _ -> j != i }) })
                                }
                            }
                        }
                    }
                    EditorSection("Ano bissexto", onAdd = null) {
                        val leap = cal.leap
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Checkbox(leap != null, { on -> cal = cal.copy(leap = if (on) LeapRule(cal.months.firstOrNull()?.id.orEmpty()) else null) })
                            Text("Um mês ganha um dia a cada", style = MaterialTheme.typography.bodySmall)
                            if (leap != null) {
                                NumberField(leap.every, "anos", 1..1000) { v -> cal = cal.copy(leap = leap.copy(every = v)) }
                                Text("no mês", style = MaterialTheme.typography.bodySmall)
                                MonthPicker(cal.months, leap.monthId) { id -> cal = cal.copy(leap = leap.copy(monthId = id)) }
                            }
                        }
                        if (leap != null && leap.skipEvery > 0) Hint2("Exceção: a cada ${leap.skipEvery} anos não há bissexto" + if (leap.keepEvery > 0) ", a não ser a cada ${leap.keepEvery}." else ".")
                    }
                    EditorSection("Eras (${cal.eras.size})", onAdd = {
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
                    EditorSection("Estações (${cal.seasons.size})", onAdd = {
                        cal = cal.copy(seasons = cal.seasons + Season(newId("s"), "Estação", SEASON_COLORS[cal.seasons.size % SEASON_COLORS.size], cal.months.firstOrNull()?.id.orEmpty(), 1))
                    }) {
                        cal.seasons.forEach { s ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    Modifier.size(22.dp).clip(CircleShape).background(hexColor(s.color) ?: Color.Gray)
                                        .clickable {
                                            val next = SEASON_COLORS[(SEASON_COLORS.indexOf(s.color) + 1).mod(SEASON_COLORS.size)]
                                            cal = cal.copy(seasons = cal.seasons.map { if (it.id == s.id) it.copy(color = next) else it })
                                        }
                                        .semantics { contentDescription = "Trocar cor da estação" },
                                )
                                SmallField(s.name, "Nome", Modifier.weight(1f)) { v -> cal = cal.copy(seasons = cal.seasons.map { if (it.id == s.id) it.copy(name = v) else it }) }
                                Text("começa em", style = MaterialTheme.typography.bodySmall)
                                NumberField(s.day, "Dia", 1..999) { v -> cal = cal.copy(seasons = cal.seasons.map { if (it.id == s.id) it.copy(day = v) else it }) }
                                MonthPicker(cal.months, s.monthId) { id -> cal = cal.copy(seasons = cal.seasons.map { if (it.id == s.id) it.copy(monthId = id) else it }) }
                                IconAction(Icons.Outlined.Close, "Apagar estação ${s.name}", onClick = { cal = cal.copy(seasons = cal.seasons.filterNot { it.id == s.id }) })
                            }
                        }
                    }
                    EditorSection("Festas (${cal.festivals.size})", onAdd = {
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
                            }
                        }
                    }
                    EditorSection("O presente da história", onAdd = null) {
                        SmallField(presentText, "ex.: 12 de ${cal.months.firstOrNull()?.name ?: "mês"}, ${cal.yearLabel(1)}", Modifier.fillMaxWidth()) { presentText = it }
                        when (presentParsed) {
                            is ParsedDate.Ok -> Hint2("→ ${cal.format(presentParsed.date)}. É de onde a Linha do tempo parte e a idade das fichas é contada.")
                            is ParsedDate.Problem -> Text(presentParsed.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            ParsedDate.Empty -> Hint2("Em branco: a história não tem um “hoje” definido.")
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
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
private fun EditorSection(title: String, onAdd: (() -> Unit)?, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = LocalPalette.current.accent, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (onAdd != null) TextButton(onClick = onAdd) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Acrescentar")
                }
            }
            content()
        }
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
