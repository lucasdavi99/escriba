package com.cuboide.escriba.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.core.model.EntryKind
import com.cuboide.escriba.core.storage.AcervoSummary
import com.cuboide.escriba.core.universe.digest
import com.cuboide.escriba.core.universe.fold
import com.cuboide.escriba.core.universe.hasSameName
import com.cuboide.escriba.platform.decodeImageThumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Imagem de ficha do universo em uso (livro ou Acervo), com o mesmo cache das imagens do Enredo. */
@Composable
fun rememberEntryImage(c: EscribaController, path: String?): ImageBitmap? {
    val host = c.universeHostPath
    val key = "$host|$path"
    val cached = path?.let { BoardImages.get(key) }
    val state = produceState(cached, key) {
        if (path == null || value != null) return@produceState
        value = withContext(Dispatchers.Default) {
            c.readUniverseAsset(path)?.let { decodeImageThumbnail(it, 1280) }?.also { BoardImages.put(key, it) }
        }
    }
    return state.value
}

/** Lista de universos do Acervo: universos independentes de livro, que podem alimentar vários livros. */
@Composable
fun AcervoScreen(c: EscribaController) {
    val book = c.book ?: return
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<AcervoSummary?>(null) }
    LaunchedEffect(Unit) { c.refreshAcervo() }
    SectionPage(
        title = "Acervo",
        subtitle = "Mundos, personagens, locais e lore fora de qualquer livro. Cada universo pode alimentar vários livros.",
        actions = {
            TextButton(onClick = { c.universeTool = com.cuboide.escriba.app.UniverseTool.Fichas }) { Text("← Universo de “${book.manifest.title}”") }
            OutlinedButton(onClick = { c.importBookToAcervo() }) { Text("Importar do livro atual") }
            Button(onClick = { creating = true }) { Text("Novo universo") }
        },
    ) {
        if (c.acervoList.isEmpty()) {
            EmptyState(
                Icons.Outlined.Inventory2, "Nenhum universo no Acervo",
                "Crie um universo novo ou importe as fichas deste livro. Depois, use “Trazer fichas” em qualquer livro para usá-las.",
            )
        }
        FlowRowCards(c, onDelete = { deleting = it })
    }
    if (creating) NewUniverseDialog(onDismiss = { creating = false }) { name, desc -> creating = false; c.openAcervoUniverse(c.createAcervoUniverse(name, desc).path) }
    deleting?.let { u ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Apagar universo") },
            text = { Text("“${u.meta.name}” e as ${u.entries} ficha(s) dele vão para a lixeira (dá para restaurar por lá).") },
            confirmButton = { TextButton(onClick = { c.trashAcervoUniverse(u); deleting = null }) { Text("Apagar") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun FlowRowCards(c: EscribaController, onDelete: (AcervoSummary) -> Unit) {
    val palette = LocalPalette.current
    Column(Modifier.verticalScroll(rememberScrollState()).widthIn(max = 920.dp)) {
        if (c.acervoList.isNotEmpty()) Hairline()
        c.acervoList.forEach { u ->
            HoverRow(false, onClick = { c.openAcervoUniverse(u.path) }, Modifier.fillMaxWidth().semantics { contentDescription = "Universo ${u.meta.name}" }) { hovered ->
                Box(
                    Modifier.padding(start = 14.dp).size(40.dp).clip(RoundedCornerShape(10.dp)).background(palette.accentSoft),
                    contentAlignment = Alignment.Center,
                ) { Icon(if (u.meta.saga) Icons.Outlined.Link else Icons.Outlined.Inventory2, null, Modifier.size(18.dp), tint = palette.accent) }
                Column(Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(u.meta.name, style = MaterialTheme.typography.titleLarge)
                    when {
                        u.meta.saga -> Text("Saga · ${u.meta.bookPaths.size} livro(s) com as mesmas fichas", style = MaterialTheme.typography.labelMedium, color = palette.accent)
                        u.meta.sourceTitle != null -> Text("Importado de “${u.meta.sourceTitle}”", style = MaterialTheme.typography.labelMedium, color = palette.muted)
                    }
                    if (u.meta.description.isNotBlank()) Text(u.meta.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    Text(
                        EntryKind.entries.joinToString(" · ") { k -> "${u.byKind[k] ?: 0} ${k.plural.lowercase()}" },
                        style = MaterialTheme.typography.labelSmall, color = palette.muted,
                    )
                }
                Box(Modifier.padding(end = 10.dp).alpha(if (hovered) 1f else 0.4f)) {
                    IconAction(Icons.Outlined.Close, "Apagar universo ${u.meta.name}", onClick = { onDelete(u) }, size = 32.dp)
                }
            }
            Hairline()
        }
    }
}

@Composable
private fun NewUniverseDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo universo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("Descrição (opcional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(enabled = name.isNotBlank(), onClick = { onCreate(name, description) }) { Text("Criar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Escolhe fichas de outro livro ou de um universo do Acervo e copia para o universo em uso (com imagens). */
@Composable
fun BringEntriesDialog(c: EscribaController, onDismiss: () -> Unit) {
    val sources = remember { c.entrySources() }
    var source by remember { mutableStateOf(sources.firstOrNull()?.first) }
    var menu by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf(emptySet<String>()) }
    val entries = remember(source) { source?.let { c.entriesAt(it) }.orEmpty() }
    val here = c.universeEntries
    val visible = entries.filter { query.isBlank() || fold(it.name).contains(fold(query)) }
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Trazer fichas") },
        text = {
            Column(Modifier.width(520.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Marque as fichas que quer copiar para cá, com imagens e tudo. A ficha de origem continua lá, do jeito que está.",
                    style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant,
                )
                if (sources.isEmpty()) {
                    Text("Não há outros livros nem universos no Acervo.", style = MaterialTheme.typography.bodyMedium)
                    return@Column
                }
                Text("DE ONDE", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                Box {
                    OutlinedButton(onClick = { menu = true }) { Text((sources.firstOrNull { it.first == source }?.second ?: "Escolha") + " ▾") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        sources.forEach { (path, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { menu = false; source = path; chosen = emptySet() }) }
                    }
                }
                OutlinedTextField(query, { query = it }, placeholder = { Text("Procurar pelo nome…") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState())) {
                    if (entries.isEmpty()) Text("Este lugar ainda não tem fichas.", style = MaterialTheme.typography.bodySmall)
                    EntryKind.entries.forEach { k ->
                        val group = visible.filter { it.kind == k }
                        if (group.isEmpty()) return@forEach
                        Text("${k.plural.uppercase()}  ${group.size}", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelSmall, color = LocalPalette.current.accent)
                        group.sortedBy { it.name.lowercase() }.forEach { e ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { chosen = if (e.id in chosen) chosen - e.id else chosen + e.id }) {
                                Checkbox(e.id in chosen, { on -> chosen = if (on) chosen + e.id else chosen - e.id })
                                Column(Modifier.weight(1f)) {
                                    Text(e.name.ifBlank { "(sem nome)" }, style = MaterialTheme.typography.bodyMedium)
                                    val note = if (here.hasSameName(e)) "já há uma ficha com este nome aqui" else e.digest()
                                    if (note.isNotBlank()) Text(note, style = MaterialTheme.typography.labelSmall, color = if (here.hasSameName(e)) scheme.error else scheme.onSurfaceVariant, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = chosen.isNotEmpty() && source != null, onClick = { c.bringEntries(source!!, chosen); onDismiss() }) {
                Text(if (chosen.isEmpty()) "Trazer" else "Trazer ${chosen.size}")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Unificar livros de uma saga: todos passam a usar as mesmas fichas. */
@Composable
fun UnifyBooksDialog(c: EscribaController, onDismiss: () -> Unit) {
    val book = c.book ?: return
    val scheme = MaterialTheme.colorScheme
    val inSaga = book.manifest.sagaId != null
    var chosen by remember { mutableStateOf(emptySet<String>()) }
    var name by remember { mutableStateOf(book.manifest.sagaName) }
    var confirmLeave by remember { mutableStateOf(false) }
    val candidates = c.sagaCandidates
    val members = c.acervoList.firstOrNull { it.meta.saga && it.meta.id == book.manifest.sagaId }?.meta?.bookPaths.orEmpty()
    LaunchedEffect(Unit) { c.refreshAcervo() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (inSaga) "Saga “${book.manifest.sagaName}”" else "Unificar livros") },
        text = {
            Column(Modifier.width(520.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Livros da mesma saga passam a usar as MESMAS fichas: personagens, locais, grupos e lore. Mudou uma ficha num livro, " +
                        "ela muda em todos. O texto, o calendário e o enredo de cada livro continuam separados.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(if (inSaga) "ACRESCENTAR À SAGA" else "UNIFICAR ESTE LIVRO COM", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                val available = candidates.filter { it.path !in members }
                if (available.isEmpty()) {
                    Text("Não há outros livros na sua biblioteca para unificar. Crie ou abra outro livro da saga e volte aqui.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
                available.forEach { b ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { chosen = if (b.path in chosen) chosen - b.path else chosen + b.path }) {
                        Checkbox(b.path in chosen, { on -> chosen = if (on) chosen + b.path else chosen - b.path })
                        Text(b.manifest.title, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                OutlinedTextField(name, { name = it }, label = { Text("Nome da saga (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text(
                    "Fichas com o mesmo nome nos livros viram UMA ficha: fica a versão deste livro, e o que era diferente no outro " +
                        "entra numa seção “Em ‹nome do livro›”. Nada se perde, e antes de juntar o Escriba guarda uma cópia das fichas de cada livro.",
                    style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant,
                )
                if (inSaga) {
                    HorizontalDivider()
                    TextButton(onClick = { confirmLeave = true }) { Text("Tirar este livro da saga") }
                }
            }
        },
        confirmButton = {
            Button(enabled = chosen.isNotEmpty() || (inSaga && name != book.manifest.sagaName), onClick = { c.unifyBooks(chosen.toList(), name); onDismiss() }) {
                Text(if (inSaga) "Atualizar a saga" else "Unificar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Tirar da saga?") },
            text = { Text("O livro fica com uma cópia das fichas, que deixam de mudar junto com os outros livros.") },
            confirmButton = { TextButton(onClick = { c.leaveSaga(); confirmLeave = false; onDismiss() }) { Text("Tirar da saga") } },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Cancelar") } },
        )
    }
}
