package com.cuboide.escriba.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
    val palette = LocalPalette.current
    var allChapters by remember { mutableStateOf(false) }
    SectionPage("Metas", "O ritmo de escrita de “${m.title}”") {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(32.dp)) {
            // Faixa principal: os dois anéis e a sequência, sem caixas.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(48.dp), modifier = Modifier.padding(top = 4.dp)) {
                GoalRing("Hoje", p.today.words, m.dailyGoal)
                GoalRing("Livro", c.bookWords, m.bookGoal)
                VHairline(Modifier.height(96.dp))
                Column {
                    Overline("Sequência")
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${p.streakDays}", style = MaterialTheme.typography.displayMedium)
                        Spacer(Modifier.width(8.dp))
                        Text(if (p.streakDays == 1) "dia seguido" else "dias seguidos", style = MaterialTheme.typography.bodyMedium, color = palette.muted, modifier = Modifier.padding(bottom = 10.dp))
                    }
                    Text(
                        "Tempo ativo hoje: ${p.today.activeSeconds / 60} min · Melhor dia: ${p.bestDay?.let { "${it.second} em ${DayKey.shortLabel(it.first)}" } ?: "—"}",
                        style = MaterialTheme.typography.bodySmall, color = palette.muted,
                    )
                }
            }
            // As metas, editáveis numa linha.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                var daily by remember(m.dailyGoal) { mutableStateOf(TextFieldValue(m.dailyGoal.toString())) }
                var total by remember(m.bookGoal) { mutableStateOf(TextFieldValue(m.bookGoal.toString())) }
                Text("Meta diária", style = MaterialTheme.typography.bodyMedium, color = palette.muted)
                CompactField(daily, { daily = it.copy(text = it.text.filter(Char::isDigit).take(7)) }, "palavras por dia", Modifier.width(110.dp))
                Spacer(Modifier.width(12.dp))
                Text("Meta do livro", style = MaterialTheme.typography.bodyMedium, color = palette.muted)
                CompactField(total, { total = it.copy(text = it.text.filter(Char::isDigit).take(8)) }, "palavras no livro", Modifier.width(120.dp))
                val changed = daily.text != m.dailyGoal.toString() || total.text != m.bookGoal.toString()
                FilledTonalButton(enabled = changed, onClick = { c.setGoals(daily.text.toIntOrNull() ?: 0, total.text.toIntOrNull() ?: 0); c.message = "Metas atualizadas." }) { Text("Salvar") }
            }
            Hairline()
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Últimas 12 semanas", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal))
                WeeksHeatmap(p.recent.map { it.first to it.second.words }, m.dailyGoal)
            }
            Hairline()
            Row(horizontalArrangement = Arrangement.spacedBy(56.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Últimos 14 dias", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal))
                    DayBars(p.recent.takeLast(14).map { it.first.takeLast(2) to it.second.words }, m.dailyGoal)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Capítulos", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal))
                    val max = (m.chapters.maxOfOrNull { it.words } ?: 0).coerceAtLeast(1)
                    val shown = if (allChapters) m.chapters else m.chapters.take(8)
                    shown.forEach { ch ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(ch.title, Modifier.width(150.dp), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(palette.hairline)) {
                                Box(Modifier.fillMaxWidth(ch.words.toFloat() / max).fillMaxHeight().background(statusColor(ch.status)))
                            }
                            Text("${ch.words}", Modifier.width(56.dp), style = MaterialTheme.typography.labelMedium, color = palette.muted, textAlign = TextAlign.End)
                        }
                    }
                    if (m.chapters.size > 8) {
                        TextButton(onClick = { allChapters = !allChapters }, contentPadding = PaddingValues(0.dp)) {
                            Text(if (allChapters) "Mostrar menos" else "Ver todos (${m.chapters.size})")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 4.dp)) {
                        ChapterStatus.entries.forEach { s ->
                            val n = m.chapters.count { it.status == s }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusDot(statusColor(s), 6.dp)
                                Spacer(Modifier.width(5.dp))
                                Text("${s.label} $n", style = MaterialTheme.typography.labelMedium, color = palette.muted)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Anel fino de progresso com o percentual no meio e o número ao lado. */
@Composable
private fun GoalRing(label: String, value: Int, goal: Int) {
    val palette = LocalPalette.current
    val f = if (goal > 0) (value.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val anim by animateFloatAsState(f, tween(700))
    val done = goal in 1..value
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                val stroke = 5.dp.toPx()
                val inset = stroke / 2
                val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                drawArc(palette.hairline, 0f, 360f, false, androidx.compose.ui.geometry.Offset(inset, inset), arcSize, style = Stroke(stroke))
                drawArc(if (done) palette.success else palette.accent, -90f, 360f * anim, false, androidx.compose.ui.geometry.Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Text(if (goal > 0) "${(value * 100L / goal).coerceAtMost(999)}%" else "—", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.width(20.dp))
        Column {
            Overline(label)
            Text("$value", style = MaterialTheme.typography.headlineLarge)
            Text(if (goal > 0) "de $goal palavras" else "sem meta definida", style = MaterialTheme.typography.bodySmall, color = palette.muted)
            if (done) Text("Meta atingida", style = MaterialTheme.typography.labelMedium, color = palette.success)
        }
    }
}

/** 12 colunas (semanas) × 7 linhas (dias); intensidade = fração da meta diária. */
@Composable
private fun WeeksHeatmap(days: List<Pair<String, Int>>, dailyGoal: Int) {
    val words = days.toMap()
    val reference = if (dailyGoal > 0) dailyGoal else (words.values.maxOrNull() ?: 1).coerceAtLeast(1)
    val primary = LocalPalette.current.accent
    val empty = LocalPalette.current.hairline
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
                    Modifier.fillMaxWidth().height((110 * w / max).coerceAtLeast(2).dp).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(if (goal in 1..w) LocalPalette.current.accent else LocalPalette.current.accent.copy(alpha = 0.35f)),
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
            LazyColumn(Modifier.width(300.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(c.snapshots, key = { it.id }) { s ->
                    val isSel = (selected ?: c.snapshots.firstOrNull())?.id == s.id
                    HoverRow(isSel, onClick = { selected = s }, Modifier.fillMaxWidth()) {
                        Box(Modifier.padding(start = 12.dp).size(8.dp).clip(CircleShape).background(if (isSel) LocalPalette.current.accent else LocalPalette.current.hairline))
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text(s.label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium)
                            Text("${formatDateTime(s.createdAt)} · ${s.words} palavras", style = MaterialTheme.typography.labelSmall, color = LocalPalette.current.muted)
                        }
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
    val current = c.chapter?.text.orEmpty()
    // Compara o texto como o leitor vê; mudanças só de formatação aparecem num aviso à parte.
    val before = remember(snapshot) { stripMarkup(snapshot.body) }
    val now = remember(current) { stripMarkup(current) }
    val segments = remember(before, now) { diffWords(before, now) }
    val stats = remember(segments) { diffStats(segments) }
    val formattingOnly = before == now && snapshot.body != current
    Surface(Modifier.fillMaxSize(), shape = Radius.surface, color = palette.paper, border = androidx.compose.foundation.BorderStroke(1.dp, palette.hairline)) {
        Column(Modifier.padding(horizontal = 40.dp, vertical = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(snapshot.label, style = MaterialTheme.typography.headlineSmall, color = palette.ink)
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
            Hairline()
            Spacer(Modifier.height(20.dp))
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
        // Campo grande, só com um fio embaixo: a busca é o assunto da tela.
        BasicTextField(
            value = c.bookSearchQuery,
            onValueChange = { c.bookSearchQuery = it; c.runBookSearch() },
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(palette.accent),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 10.dp)) {
                        Icon(Icons.Outlined.Search, null, Modifier.size(26.dp), tint = palette.muted)
                        Spacer(Modifier.width(14.dp))
                        Box(Modifier.weight(1f)) {
                            if (c.bookSearchQuery.isEmpty()) Text("Buscar no livro…", style = MaterialTheme.typography.headlineMedium, color = palette.muted.copy(alpha = 0.6f))
                            inner()
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.5.dp).background(if (c.bookSearchQuery.isEmpty()) palette.hairline else palette.accent))
                }
            },
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
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 20.dp, bottom = 6.dp)) {
                        Text(key.first, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(10.dp))
                        Text("${hits.size}", style = MaterialTheme.typography.labelMedium, color = palette.muted, modifier = Modifier.padding(bottom = 3.dp))
                    }
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
                        modifier = Modifier.fillMaxWidth().clip(Radius.surface)
                            .clickable { c.openHit(hit); c.goTo(Section.Manuscrito) }
                            .border(1.dp, palette.hairline, Radius.surface)
                            .background(palette.paper)
                            .padding(horizontal = 18.dp, vertical = 12.dp),
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
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.width(360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Dados do livro", style = MaterialTheme.typography.headlineSmall)
                Text("Usados na folha de rosto dos arquivos.", style = MaterialTheme.typography.bodySmall, color = LocalPalette.current.muted)
                OutlinedTextField(title, { title = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(author, { author = it }, label = { Text("Autor(a)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(genre, { genre = it }, label = { Text("Gênero") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(synopsis, { synopsis = it }, label = { Text("Sinopse") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    c.updateBookDetails(title, author, synopsis, genre, m.dailyGoal, m.bookGoal); c.message = "Dados do livro salvos."
                }) { Text("Salvar dados") }
            }
            VHairline()
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Text("Formatos", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 12.dp))
                Hairline()
                ExportFormat.entries.forEach { f ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(Radius.surface).background(LocalPalette.current.accentSoft), contentAlignment = Alignment.Center) {
                            Icon(f.icon, null, Modifier.size(20.dp), tint = LocalPalette.current.accent)
                        }
                        Spacer(Modifier.width(18.dp))
                        Column(Modifier.weight(1f)) {
                            Text(f.label, style = MaterialTheme.typography.titleLarge)
                            Text(f.description, style = MaterialTheme.typography.bodySmall, color = LocalPalette.current.muted)
                        }
                        Spacer(Modifier.width(16.dp))
                        OutlinedButton(onClick = { c.export(f) }) { Text("Exportar") }
                    }
                    Hairline()
                }
                Spacer(Modifier.height(14.dp))
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
