package com.cuboide.escriba.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.core.calendar.EclipseKind
import com.cuboide.escriba.core.calendar.Moon
import com.cuboide.escriba.core.calendar.MoonPhase
import com.cuboide.escriba.core.calendar.WorldCalendar
import com.cuboide.escriba.core.calendar.ageAt
import com.cuboide.escriba.core.calendar.dateAt
import com.cuboide.escriba.core.calendar.daylight
import com.cuboide.escriba.core.calendar.daylightChange
import com.cuboide.escriba.core.calendar.daysBeforeYear
import com.cuboide.escriba.core.calendar.eclipsesOn
import com.cuboide.escriba.core.calendar.formatDuration
import com.cuboide.escriba.core.calendar.formatHour
import com.cuboide.escriba.core.calendar.isFullOn
import com.cuboide.escriba.core.calendar.monthLength
import com.cuboide.escriba.core.calendar.nextFull
import com.cuboide.escriba.core.calendar.nextNew
import com.cuboide.escriba.core.calendar.seasonOf
import com.cuboide.escriba.core.calendar.stateOn
import com.cuboide.escriba.core.calendar.yearLabel
import com.cuboide.escriba.core.calendar.yearLength
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Cor de uma lua (ou o tom de pérola padrão). */
fun moonColor(moon: Moon): Color = hexColor(moon.color) ?: Color(0xFFE8E2D0)

/** Céu noturno das peças do calendário: o mesmo nos temas claros e escuros. */
private val NIGHT_TOP = Color(0xFF1A2233)
private val NIGHT_BOTTOM = Color(0xFF26324A)
private val DAYLIGHT = Color(0xFFF3C97A)
private val DAYLIGHT_SOFT = Color(0xFFF8E2B4)
private val TWILIGHT = Color(0xFF9C86C9)
val ECLIPSE = Color(0xFFE07A5F)

/**
 * Lua na fase [fraction] (0 = nova, 0,5 = cheia). Crescente acende a direita; minguante, a esquerda.
 *
 * O lado iluminado é um caminho só (o limbo e o terminador, que é meia elipse de largura |cos 2πf|·r), preenchido
 * com um degradê esférico e alguns "mares" discretos; o lado escuro é a própria lua apagada, com luz de terra.
 * Na lua cheia há um halo. [eclipse] desenha um anel coral em volta.
 */
@Composable
fun MoonGlyph(fraction: Double, color: Color, sizeDp: Int, modifier: Modifier = Modifier, eclipse: Boolean = false) {
    val palette = LocalPalette.current
    val shadow = if (palette.isDark) lerp(NIGHT_TOP, color, 0.18f) else lerp(NIGHT_TOP, NIGHT_BOTTOM, 0.4f)
    Canvas(modifier.size(sizeDp.dp)) { drawMoon(fraction, color, shadow, eclipse) }
}

fun DrawScope.drawMoon(fraction: Double, color: Color, shadow: Color, eclipse: Boolean = false) {
    val r = size.minDimension / 2 * 0.84f
    val c = Offset(size.width / 2, size.height / 2)
    val f = ((fraction % 1.0) + 1.0) % 1.0
    val illumination = (1 - cos(2 * PI * f)) / 2

    if (illumination > 0.96) drawCircle(color.copy(alpha = 0.16f), r * 1.18f, c)
    // Lado escuro com um pouco de luz de terra.
    drawCircle(Brush.radialGradient(listOf(lerp(shadow, color, 0.12f), shadow), center = c, radius = r), r, c)

    val lit = litPath(c, r, f)
    // Degradê esférico: mais claro perto do centro do lado iluminado, mais escuro no limbo.
    val waxing = f < 0.5
    val hot = Offset(c.x + (if (waxing) 0.3f else -0.3f) * r, c.y - 0.25f * r)
    val bright = lerp(color, Color.White, 0.35f)
    val rim = lerp(color, Color.Black, 0.18f)
    drawPath(lit, Brush.radialGradient(listOf(bright, color, rim), center = hot, radius = r * 1.5f))
    // Mares: manchas fixas, só dentro do lado iluminado.
    clipPath(lit) {
        val mare = lerp(color, Color.Black, 0.35f).copy(alpha = 0.16f)
        drawCircle(mare, r * 0.26f, Offset(c.x - r * 0.28f, c.y - r * 0.22f))
        drawCircle(mare, r * 0.18f, Offset(c.x + r * 0.22f, c.y - r * 0.32f))
        drawCircle(mare, r * 0.2f, Offset(c.x + r * 0.05f, c.y + r * 0.3f))
        drawCircle(mare.copy(alpha = 0.1f), r * 0.12f, Offset(c.x + r * 0.42f, c.y + r * 0.08f))
    }
    // Limbo.
    drawCircle(lerp(color, shadow, 0.5f).copy(alpha = 0.55f), r, c, style = Stroke(max(0.8f, r * 0.05f)))
    if (eclipse) drawCircle(ECLIPSE, r * 1.22f, c, style = Stroke(max(1.2f, r * 0.12f)))
}

/** Contorno do lado iluminado: limbo do lado aceso e terminador (meia elipse) de volta. */
private fun litPath(c: Offset, r: Float, f: Double): Path {
    val k = cos(2 * PI * f).toFloat() // 1 nova, 0 quarto, −1 cheia
    val w = max(abs(k) * r, 0.01f)
    val circle = Rect(c.x - r, c.y - r, c.x + r, c.y + r)
    val ellipse = Rect(c.x - w, c.y - r, c.x + w, c.y + r)
    val crescent = k > 0
    return Path().apply {
        moveTo(c.x, c.y - r)
        if (f < 0.5) {
            // Crescente: limbo pela direita, de cima para baixo.
            arcTo(circle, -90f, 180f, false)
            // Terminador de baixo para cima: estreita (crescente) curva para a direita; gibosa, para a esquerda.
            arcTo(ellipse, 90f, if (crescent) -180f else 180f, false)
        } else {
            arcTo(circle, -90f, -180f, false)
            arcTo(ellipse, 90f, if (crescent) 180f else -180f, false)
        }
        close()
    }
}

/** Fase principal (nova, quartos, cheia) que acontece durante o dia, como num almanaque; `null` nos outros dias. */
fun Moon.keyPhaseOn(day: Long): MoonPhase? {
    if (cycleDays <= 1.0) return null
    val start = ageAt(day.toDouble())
    fun crosses(target: Double) = ((target - start) % cycleDays + cycleDays) % cycleDays < 1.0
    return when {
        crosses(0.0) -> MoonPhase.Nova
        crosses(cycleDays / 4) -> MoonPhase.QuartoCrescente
        crosses(cycleDays / 2) -> MoonPhase.Cheia
        crosses(cycleDays * 3 / 4) -> MoonPhase.QuartoMinguante
        else -> null
    }
}

/** A fração "ideal" de cada fase principal, para desenhar a lua no dia da fase. */
fun MoonPhase.idealFraction(): Double = when (this) {
    MoonPhase.Nova -> 0.0
    MoonPhase.QuartoCrescente -> 0.25
    MoonPhase.Cheia -> 0.5
    MoonPhase.QuartoMinguante -> 0.75
    else -> ordinal / 8.0
}

// ---------------------------------------------------------------- O céu do dia

/** Sol e luas do dia, para o painel do dia no calendário. */
@Composable
fun DaySky(cal: WorldCalendar, day: Long) {
    val palette = LocalPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        cal.daylight(day)?.let { d ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SunArc(d.sunrise, d.sunset, cal.sun?.dayHours ?: 24.0, Modifier.fillMaxWidth().height(64.dp))
                Text(
                    "Nasce ${formatHour(d.sunrise)} · se põe ${formatHour(d.sunset)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                val change = cal.daylightChange(day)
                Text(
                    listOfNotNull("${formatDuration(d.hours)} de luz", change).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium, color = palette.muted,
                )
            }
        }
        cal.eclipsesOn(day).forEach { e ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).padding(0.dp)) {
                    Canvas(Modifier.size(8.dp)) { drawCircle(ECLIPSE) }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    e.kind.label + if (e.kind == EclipseKind.Lunar) " de ${e.moon.name}" else " (${e.moon.name})",
                    style = MaterialTheme.typography.bodyMedium, color = ECLIPSE, fontWeight = FontWeight.SemiBold,
                )
            }
        }
        cal.moons.forEach { m -> MoonLine(cal, m, day) }
    }
}

/** Arco do sol entre o nascer e o ocaso sobre a linha do horizonte, com o sol ao meio-dia. */
@Composable
private fun SunArc(sunrise: Double, sunset: Double, dayHours: Double, modifier: Modifier) {
    val palette = LocalPalette.current
    Canvas(modifier) {
        val w = size.width
        val horizon = size.height - 6f
        val x1 = (sunrise / dayHours * w).toFloat()
        val x2 = (sunset / dayHours * w).toFloat()
        val peak = horizon - size.height * 0.82f
        // Horizonte e as marcas de 6 em 6 horas.
        drawLine(palette.hairline, Offset(0f, horizon), Offset(w, horizon), 1.5f)
        for (q in 1..3) {
            val x = w * q / 4f
            drawLine(palette.hairline, Offset(x, horizon - 3f), Offset(x, horizon + 3f), 1f)
        }
        val arc = Path().apply {
            moveTo(x1, horizon)
            cubicTo(x1 + (x2 - x1) * 0.15f, peak, x2 - (x2 - x1) * 0.15f, peak, x2, horizon)
        }
        drawPath(arc, Brush.verticalGradient(listOf(DAYLIGHT.copy(alpha = 0.35f), DAYLIGHT.copy(alpha = 0.04f)), startY = peak, endY = horizon))
        drawPath(arc, DAYLIGHT, style = Stroke(1.6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))))
        // O sol no alto do arco.
        val sun = Offset((x1 + x2) / 2, peak + (horizon - peak) * 0.25f)
        drawCircle(DAYLIGHT.copy(alpha = 0.25f), 11f, sun)
        drawCircle(DAYLIGHT, 6f, sun)
        drawCircle(palette.accent.copy(alpha = 0.5f), 2f, Offset(x1, horizon))
        drawCircle(palette.accent.copy(alpha = 0.5f), 2f, Offset(x2, horizon))
    }
}

@Composable
private fun MoonLine(cal: WorldCalendar, moon: Moon, day: Long) {
    val palette = LocalPalette.current
    val s = moon.stateOn(day)
    val eclipse = cal.eclipsesOn(day).any { it.moon.id == moon.id }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics { contentDescription = "Lua ${moon.name}" }) {
        MoonGlyph(s.fraction, moonColor(moon), 38, eclipse = eclipse)
        Spacer(Modifier.width(12.dp))
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(moon.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.width(8.dp))
                Text("${s.phase.label} · ${(s.illumination * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = palette.muted)
            }
            val next = buildList {
                if (moon.isFullOn(day)) add("cheia hoje") else moon.nextFull(day)?.let { add("cheia ${relative(day, it)}") }
                moon.nextNew(day + 1)?.let { add("nova ${relative(day, it)}") }
            }
            Text(
                "dia ${s.dayOfCycle} de ${moon.cycleDays.toInt()} · " + next.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall, color = palette.muted,
            )
        }
    }
}

private fun relative(from: Long, to: Long): String = when (val d = to - from) {
    0L -> "hoje"
    1L -> "amanhã"
    else -> "em $d dias"
}

// ---------------------------------------------------------------- A fita do ano

private data class YearDay(
    val light: com.cuboide.escriba.core.calendar.Daylight?,
    val season: Color?,
    val fulls: List<Moon>,
    val eclipse: Boolean,
)

/**
 * O ano inteiro numa fita, que também serve para trocar de mês: cada mês ocupa a largura dos seus dias.
 * Com sol, mostra a faixa de luz do dia contra a noite (o dia mais longo é a parte mais larga);
 * em cima, as estações; embaixo, as luas cheias; eclipses em coral. O mês aberto fica aceso.
 */
@Composable
fun YearRibbon(
    cal: WorldCalendar,
    year: Int,
    monthIdx: Int,
    selected: Long?,
    onSelectDay: (Long) -> Unit,
    onSelectMonth: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
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
    val monthStarts = remember(cal, year) {
        var acc = 0
        cal.months.indices.map { i -> acc.also { acc += cal.monthLength(year, i) } }
    }
    val hasSky = cal.sun != null
    val dayHours = cal.sun?.dayHours ?: 24.0
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Overline("O ano de ${cal.yearLabel(year)}", Modifier.weight(1f))
            if (hasSky) {
                Legend(DAYLIGHT, "luz do dia")
                Legend(TWILIGHT, "crepúsculo")
                cal.moons.forEach { Legend(moonColor(it), "cheia de ${it.name}") }
                Legend(ECLIPSE, "eclipse")
            }
        }
        Box(
            Modifier.fillMaxWidth().height(if (hasSky) 76.dp else 28.dp).clip(Radius.surface)
                .semantics { contentDescription = "Gráfico do ano" }
                .pointerInput(cal, year) {
                    detectTapGestures { p -> onSelectDay(start + (p.x / size.width * length).toLong().coerceIn(0, length - 1L)) }
                },
        ) {
            Canvas(Modifier.matchParentSize()) {
                val w = size.width
                val col = w / length
                val top = 6f
                val bottom = size.height - if (hasSky) 12f else 0f
                val h = bottom - top
                if (hasSky) {
                    drawRect(Brush.verticalGradient(listOf(NIGHT_TOP, NIGHT_BOTTOM)))
                    // A faixa de luz como uma área contínua, com o crepúsculo em volta.
                    val twilight = (0.7 / dayHours * h).toFloat()
                    fun band(pad: Float): Path = Path().apply {
                        data.forEachIndexed { i, d ->
                            val y = top + ((d.light?.sunrise ?: (dayHours / 2)) / dayHours * h).toFloat() - pad
                            val x = i * col + col / 2
                            if (i == 0) moveTo(0f, y) else lineTo(x, y)
                        }
                        lineTo(w, top + ((data.last().light?.sunrise ?: (dayHours / 2)) / dayHours * h).toFloat() - pad)
                        for (i in data.indices.reversed()) {
                            val d = data[i]
                            val y = top + ((d.light?.sunset ?: (dayHours / 2)) / dayHours * h).toFloat() + pad
                            lineTo(i * col + col / 2, y)
                        }
                        lineTo(0f, top + ((data.first().light?.sunset ?: (dayHours / 2)) / dayHours * h).toFloat() + pad)
                        close()
                    }
                    drawPath(band(twilight), TWILIGHT.copy(alpha = 0.45f))
                    drawPath(band(0f), Brush.verticalGradient(listOf(DAYLIGHT, DAYLIGHT_SOFT, DAYLIGHT), startY = top, endY = bottom))
                    // Meio-dia.
                    drawLine(Color.White.copy(alpha = 0.18f), Offset(0f, top + h / 2), Offset(w, top + h / 2), 1f)
                } else {
                    drawRect(scheme.surfaceContainer)
                }
                // Estações, em cima.
                data.forEachIndexed { i, d -> d.season?.let { drawRect(it, Offset(i * col, 0f), Size(col + 0.6f, 3.5f)) } }
                // Luas cheias e eclipses, embaixo.
                if (hasSky) {
                    data.forEachIndexed { i, d ->
                        val x = i * col + col / 2
                        d.fulls.forEachIndexed { k, m -> drawCircle(moonColor(m), 2.6f, Offset(x + k * 5f, size.height - 6f)) }
                        if (d.eclipse) drawCircle(ECLIPSE, 3.2f, Offset(x, top + 7f))
                    }
                }
                // Os outros meses ficam na penumbra; o aberto, aceso e com moldura.
                val ms = monthStarts.getOrNull(monthIdx) ?: 0
                val ml = cal.monthLength(year, monthIdx)
                val shade = if (hasSky) NIGHT_TOP.copy(alpha = 0.62f) else scheme.background.copy(alpha = 0.55f)
                clipRect(right = ms * col) { drawRect(shade) }
                clipRect(left = (ms + ml) * col) { drawRect(shade) }
                drawRoundRect(
                    if (hasSky) Color.White.copy(alpha = 0.55f) else palette.accent,
                    topLeft = Offset(ms * col + 0.5f, 0.5f), size = Size(ml * col - 1f, size.height - 1f),
                    cornerRadius = CornerRadius(6f), style = Stroke(1.2f),
                )
                // Separação dos meses.
                monthStarts.drop(1).forEach { s -> drawLine(Color.White.copy(alpha = if (hasSky) 0.14f else 0f), Offset(s * col, 0f), Offset(s * col, size.height), 1f) }
                selected?.let { s ->
                    val i = s - start
                    if (i in 0 until length) {
                        val x = i * col + col / 2
                        drawLine(Color.White, Offset(x, top), Offset(x, bottom), 1.6f, cap = StrokeCap.Round)
                        drawCircle(Color.White, 2.6f, Offset(x, top))
                    }
                }
            }
        }
        // Os nomes dos meses, com a largura dos dias; clicar troca de mês.
        Row(Modifier.fillMaxWidth()) {
            cal.months.forEachIndexed { i, m ->
                val days = cal.monthLength(year, i)
                if (days > 0) {
                    val sel = i == monthIdx
                    Text(
                        m.name, Modifier.weight(days.toFloat()).clip(Radius.control).clickable { onSelectMonth(i) }.padding(horizontal = 3.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (sel) palette.accent else palette.muted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(8.dp)) { drawCircle(color) }
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = LocalPalette.current.muted)
    }
}

/** As oito fases em fila (ilustração do seletor de modelos). */
@Composable
fun MoonPhasesRow(modifier: Modifier = Modifier, color: Color = Color(0xFFEDE6D3), size: Int = 22) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until 8) {
            val f = i / 8.0
            val s = (size * (0.7 + 0.3 * sin(PI * f))).toInt()
            MoonGlyph(f, color, s)
        }
    }
}
