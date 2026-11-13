package com.cuboide.escriba.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.Section
import com.cuboide.escriba.core.layout.CoverSnapshot
import com.cuboide.escriba.core.model.BookSummary
import com.cuboide.escriba.platform.currentTimeMillis
import com.cuboide.escriba.platform.decodeImageBitmap
import com.cuboide.escriba.platform.localHour
import com.cuboide.escriba.platform.revealInFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A entrada do app: uma saudação, uma frase de progresso e o livro em que se estava trabalhando.
 * Os números completos ficam em Metas; aqui só o que convida a voltar a escrever.
 */
@Composable
fun LibraryScreen(c: EscribaController) {
    var showNewBook by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().background(scheme.background)) {
        if (c.library.isEmpty()) {
            FirstBook { showNewBook = true }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(168.dp),
                horizontalArrangement = Arrangement.spacedBy(36.dp),
                verticalArrangement = Arrangement.spacedBy(40.dp),
                contentPadding = PaddingValues(start = 64.dp, end = 64.dp, top = 56.dp, bottom = 64.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) { Greeting(c) }
                item(span = { GridItemSpan(maxLineSpan) }) { ContinueHero(c, c.library.first()) }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.padding(top = 16.dp)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("Estante", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                            Text(
                                if (c.library.size == 1) "1 livro" else "${c.library.size} livros",
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalPalette.current.muted,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Hairline()
                    }
                }
                items(c.library, key = { it.path }) { ShelfBook(c, it) }
                item { NewBookSlot { showNewBook = true } }
            }
        }
    }
    if (showNewBook) NewBookDialog(onDismiss = { showNewBook = false }) { title, author ->
        showNewBook = false
        c.createBook(title, author)
    }
}

@Composable
private fun Greeting(c: EscribaController) {
    val palette = LocalPalette.current
    val p = c.productivity
    val hour = remember { localHour(currentTimeMillis()) }
    val hello = when (hour) {
        in 5..11 -> "Bom dia."
        in 12..17 -> "Boa tarde."
        else -> "Boa noite."
    }
    Column {
        Text(hello, style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(8.dp))
        val progress = buildString {
            if (p.today.words > 0) append("Hoje você escreveu ${p.today.words} palavras") else append("Nenhuma palavra hoje, ainda")
            if (p.streakDays > 1) append(" · ${p.streakDays} dias seguidos")
            append(".")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(progress, style = MaterialTheme.typography.bodyLarge, color = palette.muted)
            if (c.book != null) {
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = { c.goTo(Section.Metas) }) { Text("Ver metas") }
            }
        }
    }
}

/** O livro mais recente em composição aberta: capa grande e o convite para continuar. */
@Composable
private fun ContinueHero(c: EscribaController, summary: BookSummary) {
    val palette = LocalPalette.current
    val m = summary.manifest
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(220.dp).height(260.dp), contentAlignment = Alignment.Center) {
            val vitrine = rememberVitrine(c, summary)
            if (vitrine != null) {
                Book3D(vitrine.second, vitrine.first, Modifier.fillMaxSize(), initialYaw = 30f, initialTilt = 14f)
            } else {
                BookCover(m.title, m.author, Modifier.width(164.dp).height(240.dp).shadow(18.dp, RoundedCornerShape(6.dp), ambientColor = palette.ink, spotColor = palette.ink))
            }
        }
        Spacer(Modifier.width(48.dp))
        Column(Modifier.weight(1f).widthIn(max = 560.dp)) {
            Overline("Continuar escrevendo", accent = true)
            Spacer(Modifier.height(10.dp))
            Text(m.title, style = MaterialTheme.typography.displaySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (m.synopsis.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(m.synopsis, style = MaterialTheme.typography.bodyLarge, color = palette.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "${m.totalWords} palavras · ${m.chapters.size} ${if (m.chapters.size == 1) "capítulo" else "capítulos"} · editado ${relativeTime(m.modifiedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
            )
            if (m.bookGoal > 0) {
                Spacer(Modifier.height(12.dp))
                GoalLine(m.totalWords, m.bookGoal, Modifier.width(360.dp))
            }
            Spacer(Modifier.height(28.dp))
            Button(onClick = { c.openBook(summary.path) }) {
                Text("Continuar escrevendo")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(16.dp))
            }
        }
    }
}

/** Fio de progresso da meta do livro com o percentual ao lado. */
@Composable
fun GoalLine(words: Int, goal: Int, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val f = if (goal > 0) (words.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val anim by animateFloatAsState(f, tween(600))
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.weight(1f).height(3.dp)) {
            drawRoundRect(palette.hairline, cornerRadius = CornerRadius(2f))
            drawRoundRect(palette.accent, size = size.copy(width = size.width * anim), cornerRadius = CornerRadius(2f))
        }
        Spacer(Modifier.width(12.dp))
        Text("${(f * 100).toInt()}% de $goal", style = MaterialTheme.typography.labelMedium, color = palette.muted)
    }
}

@Composable
private fun ShelfBook(c: EscribaController, summary: BookSummary) {
    val palette = LocalPalette.current
    val m = summary.manifest
    var menu by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val lift by animateFloatAsState(if (hovered) 1f else 0f, tween(Motion.PANEL))
    Column(Modifier.hoverable(interaction)) {
        Box(
            Modifier.fillMaxWidth().height(236.dp).padding(top = (6 * (1 - lift)).dp)
                .clip(RoundedCornerShape(6.dp)).clickable { c.openBook(summary.path) },
            contentAlignment = Alignment.BottomCenter,
        ) {
            val vitrine = rememberVitrine(c, summary)
            if (vitrine != null) FrontCover(vitrine.first, vitrine.second, Modifier.fillMaxSize())
            else BookCover(m.title, m.author, Modifier.fillMaxSize().shadow((2 + 8 * lift).dp, RoundedCornerShape(6.dp)))
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).clickable(interactionSource = null, indication = null) { c.openBook(summary.path) }) {
                Text(m.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${m.totalWords} palavras", style = MaterialTheme.typography.labelMedium, color = palette.muted)
            }
            Box(Modifier.alpha(if (hovered || menu) 1f else 0f)) {
                IconAction(Icons.Outlined.MoreHoriz, "Opções do livro", onClick = { menu = true }, size = 30.dp)
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Abrir") }, onClick = { menu = false; c.openBook(summary.path) })
                    DropdownMenuItem(text = { Text("Mostrar pasta") }, onClick = { menu = false; revealInFileManager(summary.path) })
                    DropdownMenuItem(text = { Text("Enviar para a lixeira") }, onClick = { menu = false; c.trashBook(summary) })
                }
            }
        }
    }
}

/** Último lugar da estante: um livro em branco, tracejado, para começar outro. */
@Composable
private fun NewBookSlot(onClick: () -> Unit) {
    val palette = LocalPalette.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val color by animateColorAsState(if (hovered) palette.accent else palette.muted.copy(alpha = 0.6f), tween(Motion.QUICK))
    Column(Modifier.hoverable(interaction).clickable(interactionSource = interaction, indication = null, onClick = onClick)) {
        Box(Modifier.fillMaxWidth().height(236.dp).padding(top = 6.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                drawRoundRect(
                    color, cornerRadius = CornerRadius(6.dp.toPx()),
                    style = Stroke(1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Add, null, Modifier.size(22.dp), tint = color)
                Spacer(Modifier.height(6.dp))
                Text("Novo livro", style = MaterialTheme.typography.labelLarge, color = color)
            }
        }
    }
}

/** Primeiro uso: nada de números, só o convite. */
@Composable
private fun FirstBook(onNew: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        OpenBookArt(Modifier.size(width = 220.dp, height = 140.dp))
        Spacer(Modifier.height(32.dp))
        Text("Toda história começa com uma página em branco.", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(10.dp))
        Text(
            "Crie seu primeiro livro: capítulos, personagens e mundo ficam todos juntos, salvos enquanto você escreve.",
            style = MaterialTheme.typography.bodyLarge,
            color = LocalPalette.current.muted,
            modifier = Modifier.widthIn(max = 520.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = onNew) {
            Icon(Icons.Outlined.AutoStories, null, Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("Começar meu primeiro livro")
        }
    }
}

/** Livro aberto em traço fino, com as linhas de texto da página esquerda. */
@Composable
private fun OpenBookArt(modifier: Modifier) {
    val palette = LocalPalette.current
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(1.4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        val mid = w / 2
        fun page(left: Boolean) {
            val p = androidx.compose.ui.graphics.Path()
            val edge = if (left) 0f else w
            p.moveTo(mid, h * 0.18f)
            p.cubicTo(mid + (edge - mid) * 0.35f, h * 0.06f, mid + (edge - mid) * 0.75f, h * 0.06f, edge, h * 0.14f)
            p.lineTo(edge, h * 0.92f)
            p.cubicTo(mid + (edge - mid) * 0.75f, h * 0.84f, mid + (edge - mid) * 0.35f, h * 0.84f, mid, h * 0.96f)
            p.close()
            drawPath(p, palette.paper)
            drawPath(p, palette.muted.copy(alpha = 0.7f), style = stroke)
        }
        page(true); page(false)
        drawLine(palette.muted.copy(alpha = 0.7f), androidx.compose.ui.geometry.Offset(mid, h * 0.18f), androidx.compose.ui.geometry.Offset(mid, h * 0.96f), 1.4.dp.toPx())
        for (i in 0 until 5) {
            val y = h * (0.32f + i * 0.11f)
            val len = if (i == 4) 0.18f else 0.32f
            drawLine(palette.hairline, androidx.compose.ui.geometry.Offset(w * 0.1f, y), androidx.compose.ui.geometry.Offset(w * (0.1f + len), y), 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
        // A primeira linha da página direita, no acento, e o cursor.
        drawLine(palette.accent, androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.32f), androidx.compose.ui.geometry.Offset(w * 0.7f, h * 0.32f), 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(palette.accent, androidx.compose.ui.geometry.Offset(w * 0.73f, h * 0.27f), androidx.compose.ui.geometry.Offset(w * 0.73f, h * 0.37f), 1.5.dp.toPx())
    }
}

/** "agora há pouco", "há 3 horas", "ontem", "há 5 dias", "em 12/03/2026". */
fun relativeTime(epochMillis: Long, now: Long = currentTimeMillis()): String {
    val minutes = (now - epochMillis) / 60_000
    return when {
        minutes < 2 -> "agora há pouco"
        minutes < 60 -> "há $minutes minutos"
        minutes < 120 -> "há 1 hora"
        minutes < 24 * 60 -> "há ${minutes / 60} horas"
        minutes < 48 * 60 -> "ontem"
        minutes < 30 * 24 * 60 -> "há ${minutes / (24 * 60)} dias"
        else -> "em " + com.cuboide.escriba.platform.formatDateTime(epochMillis).substringBefore(' ')
    }
}

/** Vitrine gravada na Diagramação › Capa (pele do livro 3D e medidas), ou `null` se o livro ainda não tem capa. */
@Composable
private fun rememberVitrine(c: EscribaController, summary: BookSummary): Pair<CoverSnapshot, ImageBitmap>? {
    val state = produceState<Pair<CoverSnapshot, ImageBitmap>?>(null, summary.path, c.coverSnapshotVersion) {
        value = withContext(Dispatchers.Default) {
            c.coverSnapshot(summary.path)?.let { (snap, png) -> decodeImageBitmap(png)?.let { snap to it } }
        }
    }
    return state.value
}

/** Só a frente da capa (a faixa da direita da pele), sem distorcer. */
@Composable
private fun FrontCover(book: CoverSnapshot, skin: ImageBitmap, modifier: Modifier) {
    val pxPerMm = skin.width / (2 * book.trimWidthMm + book.spineMm)
    val x = ((book.trimWidthMm + book.spineMm) * pxPerMm).toInt().coerceIn(0, skin.width - 1)
    val painter = remember(skin, book) { BitmapPainter(skin, IntOffset(x, 0), IntSize(skin.width - x, skin.height)) }
    Box(modifier, contentAlignment = Alignment.BottomStart) {
        Image(
            painter, contentDescription = "Capa",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxHeight().aspectRatio(book.trimWidthMm / book.trimHeightMm).shadow(8.dp, RoundedCornerShape(3.dp)),
        )
    }
}
