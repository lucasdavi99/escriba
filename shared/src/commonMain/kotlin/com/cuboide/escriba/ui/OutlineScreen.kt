package com.cuboide.escriba.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.core.model.Beat
import com.cuboide.escriba.core.model.BeatStatus
import com.cuboide.escriba.core.model.ChapterMeta
import com.cuboide.escriba.core.model.EntryKind
import com.cuboide.escriba.core.model.UniverseEntry
import com.cuboide.escriba.core.outline.BeatFilter
import com.cuboide.escriba.core.outline.StructureTemplate
import com.cuboide.escriba.core.outline.StructureTemplates
import com.cuboide.escriba.core.outline.groupByAct
import com.cuboide.escriba.core.outline.statusCounts

/** Quadro de Enredo: cartões de cena na ordem do roteiro, agrupados por etapa. */
@Composable
fun OutlineBoard(c: EscribaController, modifier: Modifier) {
    val book = c.book ?: return
    var filter by remember { mutableStateOf(BeatFilter()) }
    var editing by remember { mutableStateOf<Beat?>(null) }
    var confirmTemplate by remember { mutableStateOf<StructureTemplate?>(null) }
    val beats = book.outline.beats
    val characters = book.universe.entries.filter { it.kind == EntryKind.Personagem }.sortedBy { it.name.lowercase() }
    val visible = beats.filter(filter::matches)
    val filtering = filter != BeatFilter()

    Column(modifier.background(MaterialTheme.colorScheme.background)) {
        OutlineToolbar(c, filter, characters, onFilter = { filter = it }, onNew = { editing = c.newBeat() }, onTemplate = { confirmTemplate = it })
        if (beats.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("O roteiro está vazio.", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Crie cenas uma a uma ou comece por um modelo de estrutura em “Modelos”.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }
        if (visible.isEmpty()) {
            Text("Nenhuma cena corresponde ao filtro.", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(260.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = 36.dp, end = 28.dp, top = 8.dp, bottom = 24.dp),
        ) {
            groupByAct(visible).forEachIndexed { g, group ->
                if (group.act.isNotEmpty()) {
                    item(key = "act-$g", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            group.act,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = if (g == 0) 0.dp else 8.dp),
                        )
                    }
                }
                items(group.beats, key = { it.id }) { beat ->
                    BeatCard(
                        c, beat,
                        number = beats.indexOf(beat) + 1,
                        // Com filtro ativo os vizinhos podem estar ocultos: reordenar só na lista completa.
                        canMoveUp = !filtering && beats.first() != beat,
                        canMoveDown = !filtering && beats.last() != beat,
                        chapters = book.manifest.chapters,
                        characters = characters,
                        onEdit = { editing = beat },
                    )
                }
            }
        }
    }

    editing?.let { beat ->
        BeatDialog(
            beat = beat,
            chapters = book.manifest.chapters,
            characters = characters,
            onDismiss = { editing = null },
            onSave = { c.saveBeat(it); editing = null },
            onTrash = if (beats.any { it.id == beat.id }) ({ c.trashBeat(beat.id); editing = null }) else null,
        )
    }
    confirmTemplate?.let { t ->
        AlertDialog(
            onDismissRequest = { confirmTemplate = null },
            title = { Text("Aplicar “${t.name}”?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t.description)
                    Text(
                        "${t.steps.size} cenas serão adicionadas ao fim do roteiro. As cenas atuais não mudam.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = { Button(onClick = { c.applyTemplate(t); confirmTemplate = null }) { Text("Aplicar") } },
            dismissButton = { TextButton(onClick = { confirmTemplate = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun OutlineToolbar(
    c: EscribaController,
    filter: BeatFilter,
    characters: List<UniverseEntry>,
    onFilter: (BeatFilter) -> Unit,
    onNew: () -> Unit,
    onTemplate: (StructureTemplate) -> Unit,
) {
    val outline = c.book?.outline ?: return
    var statusMenu by remember { mutableStateOf(false) }
    var characterMenu by remember { mutableStateOf(false) }
    var templateMenu by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().padding(start = 36.dp, end = 28.dp, top = 28.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enredo", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                Box {
                    TextButton(onClick = { templateMenu = true }) { Text("Modelos ▾") }
                    DropdownMenu(expanded = templateMenu, onDismissRequest = { templateMenu = false }) {
                        StructureTemplates.all.forEach { t ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(t.name)
                                        Text("${t.steps.size} cenas", style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                onClick = { templateMenu = false; onTemplate(t) },
                            )
                        }
                    }
                }
                Button(onClick = onNew) { Text("+ Cena") }
            }
            Text(
                "${outline.beats.size} cena(s) · " + outline.statusCounts().filter { it.second > 0 }.joinToString(" · ") { "${it.first.label}: ${it.second}" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = filter.query,
                    onValueChange = { onFilter(filter.copy(query = it)) },
                    singleLine = true,
                    placeholder = { Text("Filtrar cenas") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.width(260.dp),
                )
                Box {
                    TextButton(onClick = { statusMenu = true }) { Text("Status: ${filter.status?.label ?: "todos"} ▾") }
                    DropdownMenu(expanded = statusMenu, onDismissRequest = { statusMenu = false }) {
                        DropdownMenuItem(text = { Text("Todos") }, onClick = { statusMenu = false; onFilter(filter.copy(status = null)) })
                        BeatStatus.entries.forEach { s ->
                            DropdownMenuItem(text = { Text(s.label) }, onClick = { statusMenu = false; onFilter(filter.copy(status = s)) })
                        }
                    }
                }
                Box {
                    val selected = characters.firstOrNull { it.id == filter.characterId }
                    TextButton(onClick = { characterMenu = true }, enabled = characters.isNotEmpty()) {
                        Text("Personagem: ${selected?.name ?: "todos"} ▾", maxLines = 1)
                    }
                    DropdownMenu(expanded = characterMenu, onDismissRequest = { characterMenu = false }) {
                        DropdownMenuItem(text = { Text("Todos") }, onClick = { characterMenu = false; onFilter(filter.copy(characterId = null)) })
                        characters.forEach { e ->
                            DropdownMenuItem(text = { Text(e.name) }, onClick = { characterMenu = false; onFilter(filter.copy(characterId = e.id)) })
                        }
                    }
                }
                if (filter != BeatFilter()) TextButton(onClick = { onFilter(BeatFilter()) }) { Text("Limpar filtros") }
            }
        }
    }
}

@Composable
private fun BeatCard(
    c: EscribaController,
    beat: Beat,
    number: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    chapters: List<ChapterMeta>,
    characters: List<UniverseEntry>,
    onEdit: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val chapter = beat.chapterId?.let { id -> chapters.firstOrNull { it.id == id } }
    val names = beat.characterIds.mapNotNull { id -> characters.firstOrNull { it.id == id }?.name }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp).clickable(onClick = onEdit),
    ) {
        Column(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$number · ${beat.status.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor(beat.status),
                    modifier = Modifier.weight(1f),
                )
                Box {
                    TextButton(onClick = { menu = true }) { Text("⋯") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Editar") }, onClick = { menu = false; onEdit() })
                        if (canMoveUp) DropdownMenuItem(text = { Text("Mover para antes") }, onClick = { menu = false; c.moveBeat(beat.id, -1) })
                        if (canMoveDown) DropdownMenuItem(text = { Text("Mover para depois") }, onClick = { menu = false; c.moveBeat(beat.id, 1) })
                        HorizontalDivider()
                        BeatStatus.entries.forEach { s ->
                            DropdownMenuItem(
                                text = { Text((if (s == beat.status) "● " else "○ ") + s.label) },
                                onClick = { menu = false; c.saveBeat(beat.copy(status = s)) },
                            )
                        }
                        HorizontalDivider()
                        if (chapter != null) {
                            DropdownMenuItem(text = { Text("Abrir capítulo") }, onClick = { menu = false; c.openBeatChapter(beat) })
                        } else {
                            DropdownMenuItem(text = { Text("Criar capítulo para a cena") }, onClick = { menu = false; c.createChapterForBeat(beat) })
                        }
                        DropdownMenuItem(text = { Text("Enviar para a lixeira") }, onClick = { menu = false; c.trashBeat(beat.id) })
                    }
                }
            }
            Column(Modifier.padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(beat.title.ifBlank { "(sem título)" }, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (beat.summary.isNotBlank()) {
                    Text(
                        beat.summary, style = MaterialTheme.typography.bodySmall, maxLines = 4,
                        overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                when {
                    chapter != null -> Text(
                        "📖 ${chapter.title}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { c.openBeatChapter(beat) },
                    )
                    beat.chapterId != null -> Text(
                        "Capítulo vinculado não existe mais",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (names.isNotEmpty()) {
                    Text(
                        names.joinToString(" · ") { "@$it" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun statusColor(status: BeatStatus) = when (status) {
    BeatStatus.Planejada -> MaterialTheme.colorScheme.onSurfaceVariant
    BeatStatus.Escrevendo -> MaterialTheme.colorScheme.tertiary
    BeatStatus.Escrita -> MaterialTheme.colorScheme.primary
    BeatStatus.Revisada -> MaterialTheme.colorScheme.secondary
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BeatDialog(
    beat: Beat,
    chapters: List<ChapterMeta>,
    characters: List<UniverseEntry>,
    onDismiss: () -> Unit,
    onSave: (Beat) -> Unit,
    onTrash: (() -> Unit)?,
) {
    var title by remember { mutableStateOf(beat.title) }
    var act by remember { mutableStateOf(beat.act) }
    var summary by remember { mutableStateOf(beat.summary) }
    var status by remember { mutableStateOf(beat.status) }
    var chapterId by remember { mutableStateOf(beat.chapterId) }
    var characterIds by remember { mutableStateOf(beat.characterIds) }
    var chapterMenu by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (onTrash == null) "Nova cena" else "Cena") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(act, { act = it }, label = { Text("Etapa (ex.: Ato I)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(summary, { summary = it }, label = { Text("Resumo") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                Text("Status", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    BeatStatus.entries.forEach { s ->
                        FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s.label) })
                    }
                }
                Text("Capítulo", style = MaterialTheme.typography.labelLarge)
                Box {
                    val current = chapters.firstOrNull { it.id == chapterId }
                    TextButton(onClick = { chapterMenu = true }) {
                        Text(
                            when {
                                current != null -> current.title
                                chapterId != null -> "(capítulo removido)"
                                else -> "Nenhum"
                            } + " ▾",
                        )
                    }
                    DropdownMenu(expanded = chapterMenu, onDismissRequest = { chapterMenu = false }) {
                        DropdownMenuItem(text = { Text("Nenhum") }, onClick = { chapterMenu = false; chapterId = null })
                        chapters.forEach { ch ->
                            DropdownMenuItem(text = { Text(ch.title) }, onClick = { chapterMenu = false; chapterId = ch.id })
                        }
                    }
                }
                Text("Personagens", style = MaterialTheme.typography.labelLarge)
                if (characters.isEmpty()) {
                    Text(
                        "Crie fichas de personagem no painel Universo para marcá-las aqui.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    characters.forEach { e ->
                        val on = e.id in characterIds
                        FilterChip(
                            selected = on,
                            onClick = { characterIds = if (on) characterIds - e.id else characterIds + e.id },
                            label = { Text(e.name) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    // Ids de fichas que estão na lixeira são mantidos: voltam a aparecer se a ficha for restaurada.
                    onSave(beat.copy(title = title, act = act.trim(), summary = summary.trim(), status = status, chapterId = chapterId, characterIds = characterIds))
                },
            ) { Text("Salvar") }
        },
        dismissButton = {
            Row {
                if (onTrash != null) TextButton(onClick = onTrash) { Text("Enviar para a lixeira") }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )
}
