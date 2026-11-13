package com.cuboide.escriba.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.Section
import com.cuboide.escriba.core.calendar.DatedBeat
import com.cuboide.escriba.core.calendar.DatePrecision
import com.cuboide.escriba.core.calendar.ageLabel
import com.cuboide.escriba.core.calendar.dayIndex
import com.cuboide.escriba.core.calendar.describeGap
import com.cuboide.escriba.core.calendar.format
import com.cuboide.escriba.core.calendar.timelineOf
import com.cuboide.escriba.core.calendar.weekdayOf
import com.cuboide.escriba.core.model.Beat
import com.cuboide.escriba.core.model.EntryKind

/** Linha do tempo do Enredo: as cenas com "Quando", em ordem de data, com os intervalos e o presente da história. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimelineView(c: EscribaController, header: @Composable () -> Unit, modifier: Modifier) {
    val book = c.book ?: return
    val cal = c.calendar
    val timeline = remember(book.outline, cal) { cal.timelineOf(book.outline.beats) }
    val characters = book.universe.entries.filter { it.kind == EntryKind.Personagem }.sortedBy { it.name.lowercase() }
    val presentIdx = cal.present?.let { cal.dayIndex(it) }
    var editing by remember { mutableStateOf<Beat?>(null) }

    Column(modifier.background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(start = Space.page, end = Space.xl, top = 36.dp, bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { header() }
            Text(
                if (book.calendar == null) "Calendário comum" else cal.name,
                style = MaterialTheme.typography.labelLarge, color = LocalPalette.current.muted,
            )
            TextButton(onClick = { c.goTo(Section.Universo); c.showCalendar() }) { Text("Abrir calendário") }
        }
        if (timeline.dated.isEmpty() && timeline.broken.isEmpty()) {
            EmptyState(
                Icons.Outlined.Timeline, "Nenhuma cena com data",
                "Abra uma cena no Roteiro e preencha “Quando” (por exemplo, “12 de março, 40”). Ela aparece aqui, em ordem de data.",
            )
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = Space.page, end = Space.xl, bottom = 32.dp)) {
            var presentShown = presentIdx == null
            timeline.dated.forEachIndexed { i, item ->
                if (!presentShown && presentIdx!! < item.index) {
                    presentShown = true
                    item(key = "present") { PresentMarker(cal.format(cal.present!!)) }
                }
                if (i > 0) {
                    val gap = cal.describeGap(timeline.dated[i - 1].index, item.index)
                    item(key = "gap-$i") { GapLabel(gap) }
                }
                item(key = item.beat.id) { TimelineRow(c, item, characters.filter { it.id in item.beat.characterIds }) { editing = item.beat } }
            }
            if (!presentShown) item(key = "present") { PresentMarker(cal.format(cal.present!!)) }
            if (timeline.broken.isNotEmpty()) {
                item(key = "broken-title") { SectionLabel("Datas com problema") }
                timeline.broken.forEach { b ->
                    item(key = "broken-${b.beat.id}") {
                        LooseBeat(b.beat.title, "“${b.beat.whenText}”: ${b.reason}", warning = true) { editing = b.beat }
                    }
                }
            }
            if (timeline.undated.isNotEmpty()) {
                item(key = "undated-title") { SectionLabel("Sem data (${timeline.undated.size})") }
                item(key = "undated") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        timeline.undated.forEach { b -> LooseBeat(b.title, null, warning = false) { editing = b } }
                    }
                }
            }
        }
    }
    editing?.let { beat ->
        BeatDialog(
            c = c, beat = beat, chapters = book.manifest.chapters, characters = characters,
            onDismiss = { editing = null },
            onSave = { c.saveBeat(it); editing = null },
            onTrash = { c.trashBeat(beat.id); editing = null },
        )
    }
}

/** Coluna da espinha: o fio vertical contínuo e, opcionalmente, o nó da cena. */
@Composable
private fun Spine(dot: Boolean, dashed: Boolean = false, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Canvas(modifier.width(28.dp).fillMaxHeight()) {
        val x = size.width / 2
        drawLine(
            palette.hairline.copy(alpha = 1f), androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 2f,
            pathEffect = if (dashed) androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 5f)) else null,
        )
        if (dot) {
            val y = 22.dp.toPx()
            drawCircle(palette.paper, 8.dp.toPx(), androidx.compose.ui.geometry.Offset(x, y))
            drawCircle(palette.accent, 5.dp.toPx(), androidx.compose.ui.geometry.Offset(x, y))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimelineRow(c: EscribaController, item: DatedBeat, characters: List<com.cuboide.escriba.core.model.UniverseEntry>, onEdit: () -> Unit) {
    val cal = c.calendar
    val palette = LocalPalette.current
    Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min).heightIn(min = 76.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.width(210.dp).padding(top = 12.dp, end = 16.dp), horizontalAlignment = Alignment.End) {
            Text(cal.format(item.date), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            // Dia da semana só faz sentido quando a data tem dia.
            if (item.date.precision == DatePrecision.Day) {
                cal.weekdayOf(item.date)?.let { Text(cal.weekdays[it], style = MaterialTheme.typography.labelSmall, color = palette.muted) }
            }
        }
        Spine(dot = true)
        Surface(
            Modifier.weight(1f).padding(start = 12.dp, top = 4.dp, bottom = 8.dp).clip(Radius.surface).clickable(onClick = onEdit)
                .semantics { contentDescription = "Cena na linha do tempo: ${item.beat.title}" },
            shape = Radius.surface, color = palette.paper, border = BorderStroke(1.dp, palette.hairline),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.beat.title.ifBlank { "(sem título)" }, style = MaterialTheme.typography.titleMedium)
                if (item.beat.summary.isNotBlank()) {
                    Text(item.beat.summary, style = MaterialTheme.typography.bodySmall, color = palette.muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                if (characters.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        characters.forEach { e ->
                            val age = cal.ageLabel(e, item.date)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(Radius.control).clickable { c.openUniverseEntry(e.id) }) {
                                EntryAvatar(c, e, 20)
                                Spacer(Modifier.width(6.dp))
                                Text(e.name + (age?.let { " · $it" } ?: ""), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GapLabel(text: String) {
    Row(Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(210.dp))
        Spine(dot = false, dashed = true)
        Text(text, style = MaterialTheme.typography.labelSmall, color = LocalPalette.current.muted, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun PresentMarker(date: String) {
    val palette = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp).semantics { contentDescription = "Presente da história" }, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(210.dp).padding(end = 16.dp), horizontalAlignment = Alignment.End) {
            Overline("Presente da história", accent = true)
            Text(date, style = MaterialTheme.typography.labelMedium, color = palette.accent)
        }
        Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(palette.accent))
        }
        Box(Modifier.weight(1f).padding(start = 12.dp).height(1.5.dp).background(palette.accent.copy(alpha = 0.6f)))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Overline(text, Modifier.padding(top = 28.dp, bottom = 10.dp))
}

@Composable
private fun LooseBeat(title: String, detail: String?, warning: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val palette = LocalPalette.current
    Surface(
        Modifier.padding(bottom = 6.dp).clip(Radius.surface).clickable(onClick = onClick),
        shape = Radius.surface, color = palette.paper,
        border = BorderStroke(1.dp, if (warning) scheme.error.copy(alpha = 0.6f) else palette.hairline),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
            Text(title.ifBlank { "(sem título)" }, style = MaterialTheme.typography.bodyMedium)
            if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = if (warning) scheme.error else palette.muted)
        }
    }
}
