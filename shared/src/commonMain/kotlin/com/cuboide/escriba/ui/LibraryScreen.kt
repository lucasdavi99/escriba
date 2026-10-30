package com.cuboide.escriba.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.cuboide.escriba.platform.decodeImageBitmap
import com.cuboide.escriba.core.layout.CoverSnapshot
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.produceState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.Image
import com.cuboide.escriba.core.model.BookSummary
import com.cuboide.escriba.platform.formatDateTime
import com.cuboide.escriba.platform.revealInFileManager

@Composable
fun LibraryScreen(c: EscribaController) {
    var showNewBook by remember { mutableStateOf(false) }
    val p = c.productivity
    SectionPage(
        title = "Biblioteca",
        subtitle = if (c.library.isEmpty()) "Sua estante está esperando o primeiro livro" else "${c.library.size} livro(s) na estante",
        actions = {
            Button(onClick = { showNewBook = true }) {
                Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Novo livro")
            }
        },
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(176.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatTile(Icons.Outlined.Edit, "Hoje", "${p.today.words}", Modifier.weight(1f), "palavras escritas")
                    StatTile(Icons.Outlined.LocalFireDepartment, "Sequência", "${p.streakDays}", Modifier.weight(1f), "dia(s) seguidos")
                    StatTile(Icons.Outlined.EmojiEvents, "Melhor dia", "${p.bestDay?.second ?: 0}", Modifier.weight(1f), p.bestDay?.first ?: "ainda não houve")
                    StatTile(Icons.AutoMirrored.Outlined.TrendingUp, "Total", "${p.totalWords}", Modifier.weight(1f), "palavras no Escriba")
                }
            }
            val latest = c.library.firstOrNull()
            if (latest != null) {
                item(span = { GridItemSpan(maxLineSpan) }) { ContinueCard(c, latest) }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("Estante", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
                }
                items(c.library, key = { it.path }) { ShelfBook(c, it) }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(Icons.Outlined.AutoStories, "Nenhum livro ainda", "Cada grande história começou com uma página em branco.") {
                        Button(onClick = { showNewBook = true }) { Text("Começar meu primeiro livro") }
                    }
                }
            }
        }
    }
    if (showNewBook) NewBookDialog(onDismiss = { showNewBook = false }) { title, author ->
        showNewBook = false
        c.createBook(title, author)
    }
}

@Composable
private fun ContinueCard(c: EscribaController, summary: BookSummary) {
    val m = summary.manifest
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            val vitrine = rememberVitrine(c, summary)
            if (vitrine != null) {
                Book3D(vitrine.second, vitrine.first, Modifier.width(190.dp).height(200.dp), initialYaw = 30f, initialTilt = 14f)
            } else {
                BookCover(m.title, m.author, Modifier.width(120.dp).height(172.dp), compact = true)
            }
            Spacer(Modifier.width(28.dp))
            Column(Modifier.weight(1f)) {
                Text("CONTINUAR ESCREVENDO", style = MaterialTheme.typography.labelSmall, color = LocalPalette.current.accent)
                Spacer(Modifier.height(4.dp))
                Text(m.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (m.synopsis.isNotBlank()) {
                    Text(m.synopsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "${m.totalWords} palavras · ${m.chapters.size} capítulo(s) · editado em ${formatDateTime(m.modifiedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (m.bookGoal > 0) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (m.totalWords.toFloat() / m.bookGoal).coerceIn(0f, 1f) },
                        modifier = Modifier.width(320.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                        drawStopIndicator = {},
                    )
                    Text(
                        "${(m.totalWords * 100 / m.bookGoal).coerceAtMost(100)}% da meta de ${m.bookGoal} palavras",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(onClick = { c.openBook(summary.path) }) { Text("Continuar") }
        }
    }
}

@Composable
private fun ShelfBook(c: EscribaController, summary: BookSummary) {
    val m = summary.manifest
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable { c.openBook(summary.path) }.padding(4.dp)) {
        val vitrine = rememberVitrine(c, summary)
        if (vitrine != null) FrontCover(vitrine.first, vitrine.second, Modifier.fillMaxWidth().height(236.dp))
        else BookCover(m.title, m.author, Modifier.fillMaxWidth().height(236.dp))
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(m.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${m.totalWords} palavras", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                IconAction(Icons.Outlined.MoreVert, "Opções do livro", onClick = { menu = true })
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Abrir") }, onClick = { menu = false; c.openBook(summary.path) })
                    DropdownMenuItem(text = { Text("Mostrar pasta") }, onClick = { menu = false; revealInFileManager(summary.path) })
                    DropdownMenuItem(text = { Text("Enviar para a lixeira") }, onClick = { menu = false; c.trashBook(summary) })
                }
            }
        }
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
    Box(modifier, contentAlignment = Alignment.Center) {
        Image(
            painter, contentDescription = "Capa",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxHeight().aspectRatio(book.trimWidthMm / book.trimHeightMm).shadow(6.dp, RoundedCornerShape(3.dp)),
        )
    }
}
