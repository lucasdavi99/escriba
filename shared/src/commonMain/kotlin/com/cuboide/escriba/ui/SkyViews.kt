package com.cuboide.escriba.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.core.calendar.EclipseKind
import com.cuboide.escriba.core.calendar.Moon
import com.cuboide.escriba.core.calendar.WorldCalendar
import com.cuboide.escriba.core.calendar.daysBeforeYear
import com.cuboide.escriba.core.calendar.daylight
import com.cuboide.escriba.core.calendar.daylightChange
import com.cuboide.escriba.core.calendar.eclipsesOn
import com.cuboide.escriba.core.calendar.formatDuration
import com.cuboide.escriba.core.calendar.formatHour
import com.cuboide.escriba.core.calendar.isFullOn
import com.cuboide.escriba.core.calendar.monthLength
import com.cuboide.escriba.core.calendar.nextFull
import com.cuboide.escriba.core.calendar.nextNew
import com.cuboide.escriba.core.calendar.seasonOf
import com.cuboide.escriba.core.calendar.dateAt
import com.cuboide.escriba.core.calendar.format
import com.cuboide.escriba.core.calendar.stateOn
import com.cuboide.escriba.core.calendar.yearLength
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

/**
 * Lua desenhada na fase [fraction] (0 = nova, 0,5 = cheia). Crescente acende a direita; minguante, a esquerda.
 * O terminador é uma elipse de largura |cos(2πf)|·raio, clara nas gibosas e escura nas crescentes/minguantes.
 */
@Composable
fun MoonGlyph(fraction: Double, color: Color, sizeDp: Int, modifier: Modifier = Modifier) {
    // Sombra escura fixa: a parte não iluminada precisa contrastar nos temas claros e escuros.
    val dark = Color(0xFF3B4250)
    Canvas(modifier.size(sizeDp.dp)) {
        val r = size.minDimension / 2
        val c = Offset(size.width / 2, size.height / 2)
        drawCircle(dark, r, c)
        val waxing = fraction < 0.5
        val k = cos(2 * PI * fraction).toFloat() // 1 nova, −1 cheia
        // Metade iluminada.
        drawArc(color, startAngle = if (waxing) -90f else 90f, sweepAngle = 180f, useCenter = true, topLeft = Offset(c.x - r, c.y - r), size = Size(2 * r, 2 * r))
        // Terminador.
        val w = abs(k) * r
        val terminatorColor = if (k > 0) dark else color
        drawOval(terminatorColor, topLeft = Offset(c.x - w, c.y - r), size = Size(2 * w, 2 * r))
        drawCircle(dark.copy(alpha = 0.6f), r, c, style = Stroke(0.8f))
    }
}

/** Sol e luas do dia, para o painel do dia no calendário. */
@Composable
fun DaySky(cal: WorldCalendar, day: Long) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        cal.daylight(day)?.let { d ->
            Text(
                "☀ Nasce ${formatHour(d.sunrise)} · se põe ${formatHour(d.sunset)} · ${formatDuration(d.hours)} de luz",
                style = MaterialTheme.typography.bodySmall,
            )
            cal.daylightChange(day)?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant) }
        }
        cal.eclipsesOn(day).forEach { e ->
            Text(
                "● ${e.kind.label}" + if (e.kind == EclipseKind.Lunar) " de ${e.moon.name}" else " (${e.moon.name})",
                style = MaterialTheme.typography.bodySmall, color = scheme.error, fontWeight = FontWeight.SemiBold,
            )
        }
        cal.moons.forEach { m -> MoonLine(cal, m, day) }
    }
}

@Composable
private fun MoonLine(cal: WorldCalendar, moon: Moon, day: Long) {
    val s = moon.stateOn(day)
    val color = hexColor(moon.color) ?: Color(0xFFE8E2D0)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics { contentDescription = "Lua ${moon.name}" }) {
        MoonGlyph(s.fraction, color, 26)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                "${moon.name} · ${s.phase.label} · ${(s.illumination * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
            )
            val next = buildList {
                if (moon.isFullOn(day)) add("cheia hoje") else moon.nextFull(day)?.let { add("cheia ${relative(day, it)}") }
                moon.nextNew(day + 1)?.let { add("nova ${relative(day, it)}") }
            }
            Text("dia ${s.dayOfCycle} de ${moon.cycleDays.toInt()} do ciclo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(next.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


private fun relative(from: Long, to: Long): String = when (val d = to - from) {
    0L -> "hoje"
    1L -> "amanhã"
    else -> "em $d dias"
}

/**
 * O ano inteiro numa faixa: cada coluna é um dia (do primeiro ao último); de cima para baixo, as horas.
 * A faixa clara é o dia, a escura a noite; em cima, a cor da estação; embaixo, as luas cheias de cada lua;
 * eclipses em vermelho. Clicar escolhe o dia.
 */
@Composable
fun YearLightChart(cal: WorldCalendar, year: Int, selected: Long?, onSelect: (Long) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val start = cal.daysBeforeYear(year)
    val length = cal.yearLength(year)
    if (length <= 0) return
    val data = remember(cal, year) {
        (0 until length).map { i ->
            val d = start + i
            YearDay(
                light = cal.daylight(d),
                season = cal.dateAt(d)?.let { cal.seasonOf(it) }?.color?.let { hexColor(it) },
                fulls = cal.moons.filter { it.isFullOn(d) },
                eclipse = cal.eclipsesOn(d).isNotEmpty(),
            )
        }
    }
    val dayHours = cal.sun?.dayHours ?: 24.0
    val night = Color(0xFF1F2A3A)
    val dayColor = Color(0xFFF2C46B)
    val dusk = Color(0xFF8E6BB8).copy(alpha = 0.55f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("O ANO: ESTAÇÕES E LUZ", style = MaterialTheme.typography.labelMedium, color = LocalPalette.current.accent, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text("cada coluna é um dia · clique para ir até ele", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        }
        Box(
            Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(12.dp))
                .semantics { contentDescription = "Gráfico do ano" }
                .pointerInput(cal, year) {
                    detectTapGestures { p -> onSelect(start + (p.x / size.width * length).toLong().coerceIn(0, length - 1L)) }
                },
        ) {
            Canvas(Modifier.matchParentSize()) {
                val w = size.width
                val top = 8f
                val bottom = size.height - 22f
                val h = bottom - top
                val col = w / length
                drawRect(night, topLeft = Offset(0f, 0f), size = size)
                data.forEachIndexed { i, d ->
                    val x = i * col
                    d.season?.let { drawRect(it, topLeft = Offset(x, 0f), size = Size(col + 0.5f, 5f)) }
                    d.light?.let { l ->
                        val y1 = top + (l.sunrise / dayHours * h).toFloat()
                        val y2 = top + (l.sunset / dayHours * h).toFloat()
                        val twilight = 0.6f / dayHours.toFloat() * h
                        drawRect(dusk, topLeft = Offset(x, y1 - twilight), size = Size(col + 0.5f, y2 - y1 + 2 * twilight))
                        drawRect(dayColor, topLeft = Offset(x, y1), size = Size(col + 0.5f, y2 - y1))
                    }
                    d.fulls.forEachIndexed { k, m ->
                        drawCircle(hexColor(m.color) ?: Color.White, radius = 3.5f, center = Offset(x + col / 2, bottom + 6f + k * 8f))
                    }
                    if (d.eclipse) drawCircle(Color(0xFFD9534F), radius = 4f, center = Offset(x + col / 2, top + h / 2))
                }
                // Meio-dia e linhas de 6 em 6 horas.
                listOf(0.25, 0.5, 0.75).forEach { f ->
                    val y = top + (f * h).toFloat()
                    drawLine(Color.White.copy(alpha = 0.12f), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                }
                selected?.let { s ->
                    val i = s - start
                    if (i in 0 until length) {
                        val x = i * col + col / 2
                        drawLine(Color.White, Offset(x, 0f), Offset(x, bottom), strokeWidth = 2f)
                    }
                }
            }
        }
        // Meses embaixo, com largura proporcional aos dias.
        Row(Modifier.fillMaxWidth()) {
            cal.months.forEachIndexed { i, m ->
                val days = cal.monthLength(year, i)
                if (days > 0) {
                    Text(
                        m.name.take(3), Modifier.weight(days.toFloat()).padding(start = 2.dp),
                        style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Clip,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Legend(dayColor, "dia claro")
            Legend(dusk, "aurora e crepúsculo")
            Legend(night, "noite")
            cal.moons.forEach { Legend(hexColor(it.color) ?: Color.White, "cheia de ${it.name}") }
            Legend(Color(0xFFD9534F), "eclipse")
        }
    }
}

private data class YearDay(
    val light: com.cuboide.escriba.core.calendar.Daylight?,
    val season: Color?,
    val fulls: List<Moon>,
    val eclipse: Boolean,
)

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).padding(0.dp)) {
            Canvas(Modifier.matchParentSize()) { drawRect(color) }
        }
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
