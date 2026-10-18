package com.cuboide.escriba.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.Section
import com.cuboide.escriba.core.export.ExportFormat
import com.cuboide.escriba.core.model.ChapterStatus
import com.cuboide.escriba.core.storage.Snapshot
import com.cuboide.escriba.core.text.DiffKind
import com.cuboide.escriba.core.text.diffStats
import com.cuboide.escriba.core.text.diffWords
import androidx.compose.foundation.layout.wrapContentWidth
import com.cuboide.escriba.core.text.heatmapColumns
import com.cuboide.escriba.core.text.DayKey
import com.cuboide.escriba.core.text.stripMarkup
import com.cuboide.escriba.platform.formatDateTime

// ---------------------------------------------------------------- Metas

@Composable
fun GoalsScreen(c: EscribaController) {
    val book = c.book ?: return
    val m = book.manifest
    val p = c.productivity
    SectionPage("Metas e progresso", "Acompanhe o ritmo de escrita de “${m.title}”") {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RingCard("Hoje", p.today.words, m.dailyGoal, "palavras", Modifier.weight(1f))
                RingCard("Livro", c.bookWords, m.bookGoal, "palavras", Modifier.weight(1f))
                Card(Modifier.weight(1f)) {
                    Text("SEQUÊNCIA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${p.streakDays}", style = MaterialTheme.typography.displayMedium)
                    Text("dia(s) seguidos escrevendo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("Tempo ativo hoje: ${p.today.activeSeconds / 60} min", style = MaterialTheme.typography.bodySmall)
                    Text("Melhor dia: ${p.bestDay?.let { "${it.second} em ${it.first}" } ?: "—"}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Text("Definir metas", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                var daily by remember(m.dailyGoal) { mutableStateOf(m.dailyGoal.toString()) }
                var total by remember(m.bookGoal) { mutableStateOf(m.bookGoal.toString()) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(daily, { daily = it.filter(Char::isDigit).take(7) }, label = { Text("Palavras por dia") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(total, { total = it.filter(Char::isDigit).take(8) }, label = { Text("Palavras no livro") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                    Button(onClick = { c.setGoals(daily.toIntOrNull() ?: 0, total.toIntOrNull() ?: 0); c.message = "Metas atualizadas." }) { Text("Salvar") }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Text("Últimas 12 semanas", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                WeeksHeatmap(p.recent.map { it.first to it.second.words }, m.dailyGoal)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(Modifier.weight(1f)) {
                    Text("Últimos 14 dias", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    DayBars(p.recent.takeLast(14).map { it.first.takeLast(2) to it.second.words }, m.dailyGoal)
                }
                Card(Modifier.weight(1f)) {
                    Text("Capítulos", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    val max = (m.chapters.maxOfOrNull { it.words } ?: 0).coerceAtLeast(1)
                    m.chapters.forEach { ch ->
                        Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(ch.title, Modifier.width(140.dp), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                                Box(Modifier.fillMaxWidth(ch.words.toFloat() / max).fillMaxHeight().background(statusColor(ch.status)))
                            }
                            Text("${ch.words}", Modifier.width(56.dp), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ChapterStatus.entries.forEach { s ->
                            val n = m.chapters.count { it.status == s }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(statusColor(s)))
                                Spacer(Modifier.width(4.dp))
                                Text("${s.label} $n", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(20.dp)) { content() }
    }
}

@Composable
private fun RingCard(label: String, value: Int, goal: Int, unit: String, modifier: Modifier) {
    Card(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { if (goal > 0) (value.toFloat() / goal).coerceIn(0f, 1f) else 0f },
                    modifier = Modifier.size(96.dp),
                    strokeWidth = 9.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    gapSize = 0.dp,
                )
                Text(if (goal > 0) "${(value * 100L / goal).coerceAtMost(999)}%" else "—", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$value", style = MaterialTheme.typography.headlineMedium)
                Text(if (goal > 0) "de $goal $unit" else "sem meta definida", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (goal in 1..value) Text("Meta atingida!", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/** 12 colunas (semanas) × 7 linhas (dias); intensidade = fração da meta diária. */
@Composable
private fun WeeksHeatmap(days: List<Pair<String, Int>>, dailyGoal: Int) {
    val words = days.toMap()
    val reference = if (dailyGoal > 0) dailyGoal else (words.values.maxOrNull() ?: 1).coerceAtLeast(1)
    val primary = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceContainerHighest
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val columns = remember(days) { heatmapColumns(days.map { it.first }) }
    val cell = 16.dp
    val gap = 5.dp
    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
        // Dias da semana (seg, qua, sex), alinhados às linhas.
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            Spacer(Modifier.height(14.dp))
            listOf("", "seg", "", "qua", "", "sex", "").forEach { label ->
                Box(Modifier.height(cell), contentAlignment = Alignment.CenterStart) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = muted)
                }
            }
        }
        columns.forEach { column ->
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                Box(Modifier.height(14.dp).width(cell)) {
                    // O nome do mês pode passar da largura da coluna sem empurrar as outras.
                    column.monthLabel?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = muted, softWrap = false, modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true)) }
                }
                column.days.forEach { day ->
                    if (day == null) {
                        Spacer(Modifier.size(cell))
                    } else {
                        val w = words[day] ?: 0
                        val f = (w.toFloat() / reference).coerceIn(0f, 1f)
                        WithTooltip("${DayKey.shortLabel(day)}: $w palavra(s)") {
                            Box(Modifier.size(cell).clip(RoundedCornerShape(4.dp)).background(if (w == 0) empty else primary.copy(alpha = 0.25f + 0.75f * f)))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Spacer(Modifier.height(14.dp))
            Text("Menos", style = MaterialTheme.typography.labelSmall, color = muted)
            listOf(0f, 0.33f, 0.66f, 1f).forEach { f ->
                Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(if (f == 0f) empty else primary.copy(alpha = 0.25f + 0.75f * f)))
            }
            Text("Mais", style = MaterialTheme.typography.labelSmall, color = muted)
        }
    }
}

@Composable
private fun DayBars(days: List<Pair<String, Int>>, goal: Int) {
    val max = maxOf(goal, days.maxOfOrNull { it.second } ?: 0, 1)
    Row(Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
        days.forEach { (day, w) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.fillMaxWidth().height((110 * w / max).coerceAtLeast(2).dp).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (goal in 1..w) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                )
                Text(day, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ---------------------------------------------------------------- Versões

@Composable
fun VersionsScreen(c: EscribaController) {
    val book = c.book ?: return
    val chapter = c.currentChapter
    var selected by remember(c.currentChapterId) { mutableStateOf<Snapshot?>(null) }
    var chapterMenu by remember { mutableStateOf(false) }
    SectionPage(
        "Versões",
        "Compare o texto atual com versões anteriores e restaure quando quiser",
        actions = {
            Box {
                OutlinedButton(onClick = { chapterMenu = true }) { Text(chapter?.title ?: "Escolher capítulo") }
                DropdownMenu(expanded = chapterMenu, onDismissRequest = { chapterMenu = false }) {
                    book.manifest.chapters.forEach { ch ->
                        DropdownMenuItem(text = { Text(ch.title) }, onClick = { chapterMenu = false; c.selectChapter(ch.id) })
                    }
                }
            }
            Button(onClick = { c.snapshotCurrent() }, enabled = chapter != null) { Text("Salvar versão agora") }
        },
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            LazyColumn(Modifier.width(300.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(c.snapshots, key = { it.id }) { s ->
                    val isSel = (selected ?: c.snapshots.firstOrNull())?.id == s.id
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable { selected = s }.padding(14.dp),
                    ) {
                        Text(s.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("${formatDateTime(s.createdAt)} · ${s.words} palavras", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                val s = selected ?: c.snapshots.firstOrNull()
                when {
                    c.snapshots.isEmpty() -> EmptyState(Icons.Outlined.History, "Nenhuma versão deste capítulo", "Salve uma versão antes de grandes mudanças. O Escriba também salva sozinho antes de substituições e restaurações.")
                    s == null -> EmptyState(Icons.Outlined.History, "Escolha uma versão", "As diferenças em relação ao texto atual aparecem aqui.")
                    else -> DiffView(c, s) { selected = null }
                }
            }
        }
    }
}

@Composable
private fun DiffView(c: EscribaController, snapshot: Snapshot, onDone: () -> Unit) {
    val palette = LocalPalette.current
    val current = c.editor.text
    // Compara o texto como o leitor vê; mudanças só de formatação aparecem num aviso à parte.
    val before = remember(snapshot) { stripMarkup(snapshot.body) }
    val now = remember(current) { stripMarkup(current) }
    val segments = remember(before, now) { diffWords(before, now) }
    val stats = remember(segments) { diffStats(segments) }
    val formattingOnly = before == now && snapshot.body != current
    Surface(Modifier.fillMaxSize(), shape = RoundedCornerShape(20.dp), color = palette.paper) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(snapshot.label, style = MaterialTheme.typography.titleLarge, color = palette.ink)
                    Text(
                        "${formatDateTime(snapshot.createdAt)} → agora · +${stats.insertedWords} / −${stats.deletedWords} palavras",
                        style = MaterialTheme.typography.labelMedium, color = palette.ink.copy(alpha = 0.6f),
                    )
                }
                TextButton(onClick = { c.deleteSnapshot(snapshot); onDone() }) { Text("Apagar") }
                Button(onClick = { c.restoreSnapshot(snapshot); onDone() }) { Text("Restaurar esta versão") }
            }
            if (formattingOnly) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "O texto é o mesmo; mudou só a formatação (negrito, títulos, notas…).",
                    style = MaterialTheme.typography.labelMedium, color = palette.accent,
                )
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text(
                buildAnnotatedString {
                    for (seg in segments) when (seg.kind) {
                        DiffKind.Equal -> append(seg.text)
                        DiffKind.Inserted -> withStyle(SpanStyle(background = palette.inserted)) { append(seg.text) }
                        DiffKind.Deleted -> withStyle(SpanStyle(background = palette.deleted, textDecoration = TextDecoration.LineThrough)) { append(seg.text) }
                    }
                },
                fontFamily = LocalFonts.current.book,
                color = palette.ink,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        }
    }
}

// ---------------------------------------------------------------- Busca

@Composable
fun SearchScreen(c: EscribaController) {
    val palette = LocalPalette.current
    SectionPage("Busca", "Encontre qualquer trecho em todos os capítulos (Ctrl+Shift+F)") {
        OutlinedTextField(
            value = c.bookSearchQuery,
            onValueChange = { c.bookSearchQuery = it; c.runBookSearch() },
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            placeholder = { Text("Buscar no livro…") },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.titleMedium,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Toggle("Maiúsculas", c.findOptions.matchCase) { c.findOptions = c.findOptions.copy(matchCase = it); c.runBookSearch() }
            Toggle("Palavra inteira", c.findOptions.wholeWord) { c.findOptions = c.findOptions.copy(wholeWord = it); c.runBookSearch() }
            Toggle("Regex", c.findOptions.regex) { c.findOptions = c.findOptions.copy(regex = it); c.runBookSearch() }
            Spacer(Modifier.weight(1f))
            if (c.bookSearchQuery.isNotEmpty()) {
                val chapters = c.bookSearchHits.map { it.chapterId }.distinct().size
                Text("${c.bookSearchHits.size} resultado(s) em $chapters capítulo(s)", style = MaterialTheme.typography.labelLarge)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (c.bookSearchQuery.isEmpty()) {
            EmptyState(Icons.Outlined.Search, "Busque no livro inteiro", "Nomes, frases, palavras repetidas: os resultados aparecem agrupados por capítulo.")
            return@SectionPage
        }
        val highlight = SpanStyle(background = palette.match, fontWeight = FontWeight.Bold)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            c.bookSearchHits.groupBy { it.chapterTitle to it.chapterId }.forEach { (key, hits) ->
                item(key = "h-${key.second}") {
                    Text("${key.first}  ·  ${hits.size}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                }
                items(hits) { hit ->
                    Text(
                        buildAnnotatedString {
                            append(hit.snippet.substring(0, hit.snippetMatchStart))
                            withStyle(highlight) { append(hit.snippet.substring(hit.snippetMatchStart, hit.snippetMatchEnd)) }
                            append(hit.snippet.substring(hit.snippetMatchEnd))
                        },
                        fontFamily = LocalFonts.current.book,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable { c.openHit(hit); c.goTo(Section.Manuscrito) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Exportar

private val ExportFormat.icon: ImageVector
    get() = when (this) {
        ExportFormat.Markdown -> Icons.Outlined.Code
        ExportFormat.Texto -> Icons.AutoMirrored.Outlined.TextSnippet
        ExportFormat.Html -> Icons.Outlined.Language
        ExportFormat.Docx -> Icons.Outlined.Description
    }

private val ExportFormat.description: String
    get() = when (this) {
        ExportFormat.Markdown -> "Texto com marcação leve. Ideal para versionar, publicar em blogs ou converter com Pandoc."
        ExportFormat.Texto -> "Somente o texto, sem formatação. Funciona em qualquer lugar."
        ExportFormat.Html -> "Página pronta para o navegador, com tipografia de livro."
        ExportFormat.Docx -> "Documento do Word com títulos de capítulo, recuo e quebra de página. Bom para enviar a editoras e revisores."
    }

@Composable
fun ExportScreen(c: EscribaController) {
    val m = c.book?.manifest ?: return
    var title by remember(m.id) { mutableStateOf(m.title) }
    var author by remember(m.id) { mutableStateOf(m.author) }
    var genre by remember(m.id) { mutableStateOf(m.genre) }
    var synopsis by remember(m.id) { mutableStateOf(m.synopsis) }
    SectionPage("Exportar", "Gere arquivos do livro para revisão, envio ou publicação") {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.width(360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Dados do livro", style = MaterialTheme.typography.titleMedium)
                Text("Usados na folha de rosto dos arquivos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(title, { title = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(author, { author = it }, label = { Text("Autor(a)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(genre, { genre = it }, label = { Text("Gênero") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(synopsis, { synopsis = it }, label = { Text("Sinopse") }, minLines = 4, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Button(onClick = {
                    c.updateBookDetails(title, author, synopsis, genre, m.dailyGoal, m.bookGoal); c.message = "Dados do livro salvos."
                }) { Text("Salvar dados") }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExportFormat.entries.forEach { f ->
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                                Icon(f.icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(f.label, style = MaterialTheme.typography.titleMedium)
                                Text(f.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.width(12.dp))
                            OutlinedButton(onClick = { c.export(f) }) { Text("Exportar") }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Outlined.Article, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${m.chapters.size} capítulo(s), ${m.totalWords} palavras. Títulos, listas, citações, versalete, notas de rodapé e imagens saem formatados no DOCX e no HTML.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
