package com.cuboide.escriba.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.Section
import com.cuboide.escriba.core.model.EntryKind
import com.cuboide.escriba.core.model.UniverseEntry

val EntryKind.icon: ImageVector
    get() = when (this) {
        EntryKind.Personagem -> Icons.Outlined.Person
        EntryKind.Local -> Icons.Outlined.Place
        EntryKind.Grupo -> Icons.Outlined.Groups
        EntryKind.Lore -> Icons.Outlined.AutoStories
    }

@Composable
fun UniverseScreen(c: EscribaController) {
    val book = c.book ?: return
    var kind by remember { mutableStateOf(EntryKind.Personagem) }
    var filter by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    val all = book.universe.entries
    // Pedido vindo do mapa de enredo: mostra a ficha e a aba do tipo dela.
    LaunchedEffect(c.universeFocus) {
        val id = c.universeFocus ?: return@LaunchedEffect
        all.firstOrNull { it.id == id }?.let { e -> kind = e.kind; filter = ""; selectedId = e.id }
        c.consumeUniverseFocus()
    }
    val entries = all.filter { it.kind == kind }
        .filter { filter.isBlank() || it.name.contains(filter, true) || it.tags.any { t -> t.contains(filter, true) } }
        .sortedBy { it.name.lowercase() }
    val selected = all.firstOrNull { it.id == selectedId } ?: entries.firstOrNull()

    SectionPage(
        title = "Universo",
        subtitle = "${all.size} ficha(s): personagens, locais, grupos e lore do livro",
        actions = {
            Button(onClick = { selectedId = c.createEntry(kind).id }) { Text("Nova ficha de ${kind.label.lowercase()}") }
        },
    ) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EntryKind.entries.forEach { k ->
                FilterChip(
                    selected = kind == k,
                    onClick = { kind = k },
                    leadingIcon = { Icon(k.icon, null, Modifier.size(18.dp)) },
                    label = { Text("${k.plural}  ${all.count { it.kind == k }}") },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.width(320.dp).fillMaxHeight()) {
                OutlinedTextField(
                    value = filter, onValueChange = { filter = it }, singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    placeholder = { Text("Filtrar por nome ou etiqueta") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(entries, key = { it.id }) { e -> EntryRow(e, e.id == selected?.id) { selectedId = e.id } }
                }
                if (entries.isEmpty()) {
                    Text(
                        "Nenhuma ficha de ${kind.plural.lowercase()} ainda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                if (selected == null) {
                    EmptyState(Icons.Outlined.Public, "Selecione uma ficha", "Escolha alguém ou algum lugar à esquerda, ou crie uma nova ficha.")
                } else {
                    EntryEditor(c, selected, onDeleted = { selectedId = null })
                }
            }
        }
    }
}

@Composable
private fun EntryRow(entry: UniverseEntry, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent)
            .clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(entry.name, 38)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.name.ifBlank { "(sem nome)" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                entry.summary.ifBlank { entry.tags.joinToString(" · ") { "#$it" } },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun Avatar(name: String, sizeDp: Int) {
    val hue = ((name.hashCode().toLong() and 0xffff) % 360).toFloat()
    Box(
        Modifier.size(sizeDp.dp).clip(CircleShape).background(Color.hsl(hue, 0.35f, 0.42f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(name.trim().take(1).uppercase().ifEmpty { "?" }, color = Color.White, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun EntryEditor(c: EscribaController, entry: UniverseEntry, onDeleted: () -> Unit) {
    // Estado local por ficha; cada alteração é gravada na hora.
    var name by remember(entry.id) { mutableStateOf(entry.name) }
    var summary by remember(entry.id) { mutableStateOf(entry.summary) }
    var notes by remember(entry.id) { mutableStateOf(entry.notes) }
    var tags by remember(entry.id) { mutableStateOf(entry.tags.joinToString(", ")) }
    fun save() = c.saveEntry(
        entry.copy(
            name = name.trim(), summary = summary.trim(), notes = notes,
            tags = tags.split(',').map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }.distinct(),
        ),
    )
    val mentions = remember(entry.id, entry.name) { c.chaptersMentioning(entry.name) }
    val ink = MaterialTheme.colorScheme.onSurface
    // Ficha nova (sem nome) já abre com o cursor no nome.
    val nameFocus = remember { FocusRequester() }
    LaunchedEffect(entry.id) { if (entry.name.isBlank()) nameFocus.requestFocus() }

    Surface(Modifier.fillMaxSize(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(name, 64)
                Spacer(Modifier.width(20.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(entry.kind.icon, null, Modifier.size(14.dp), tint = LocalPalette.current.accent)
                        Spacer(Modifier.width(4.dp))
                        Text(entry.kind.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = LocalPalette.current.accent)
                    }
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it; save() },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineMedium.copy(color = ink),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.focusRequester(nameFocus),
                        decorationBox = { inner ->
                            if (name.isEmpty()) Text("Nome", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.outline)
                            inner()
                        },
                    )
                }
                IconAction(Icons.Outlined.DeleteOutline, "Enviar para a lixeira", onClick = { c.trashEntry(entry); onDeleted() })
            }
            OutlinedTextField(summary, { summary = it; save() }, label = { Text("Resumo em uma linha") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(
                notes, { notes = it; save() }, label = { Text("Anotações") },
                minLines = 8, modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp), shape = RoundedCornerShape(12.dp),
            )
            OutlinedTextField(tags, { tags = it; save() }, label = { Text("Etiquetas (separadas por vírgula)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("APARECE EM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                if (mentions.isEmpty()) {
                    Text("Ainda não citado em nenhum capítulo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    mentions.forEach { ch ->
                        TextButton(onClick = { c.selectChapter(ch.id); c.goTo(Section.Manuscrito) }) { Text(ch.title) }
                    }
                }
            }
        }
    }
}
