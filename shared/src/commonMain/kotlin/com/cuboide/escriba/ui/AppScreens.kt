package com.cuboide.escriba.ui

import androidx.compose.ui.draw.shadow
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.core.storage.ThemeId
import com.cuboide.escriba.core.storage.TrashItem
import com.cuboide.escriba.core.storage.TrashKind
import com.cuboide.escriba.core.storage.TrashStore
import com.cuboide.escriba.platform.formatDateTime
import com.cuboide.escriba.platform.installedBookFonts
import com.cuboide.escriba.platform.revealInFileManager
import com.cuboide.escriba.platform.systemFontFamily

// ---------------------------------------------------------------- Lixeira

private val TrashKind.icon
    get() = when (this) {
        TrashKind.Livro -> Icons.AutoMirrored.Outlined.MenuBook
        TrashKind.Capitulo -> Icons.Outlined.Description
        TrashKind.Ficha -> Icons.Outlined.Person
        TrashKind.Cena -> Icons.Outlined.Timeline
        TrashKind.Universo -> Icons.Outlined.Public
    }

@Composable
fun TrashScreen(c: EscribaController) {
    var confirmDelete by remember { mutableStateOf<TrashItem?>(null) }
    var confirmEmpty by remember { mutableStateOf(false) }
    // null = todos os tipos
    var filter by remember { mutableStateOf<TrashKind?>(null) }
    val visible = c.trashItems.filter { filter == null || it.kind == filter }
    SectionPage("Lixeira", "Itens apagados ficam aqui por ${c.settings.trashRetentionDays} dias antes de sumirem de vez") {
        if (c.trashItems.isEmpty()) {
            EmptyState(Icons.Outlined.DeleteOutline, "A lixeira está vazia", "Livros, capítulos, fichas e cenas apagados aparecem aqui e podem ser restaurados.")
            return@SectionPage
        }
        val kinds = TrashKind.entries.filter { k -> c.trashItems.any { it.kind == k } }
        Row(verticalAlignment = Alignment.CenterVertically) {
            QuietTabs(
                listOf(TabItem("Todos", c.trashItems.size)) + kinds.map { k -> TabItem(k.label, c.trashItems.count { it.kind == k }) },
                selected = filter?.let { kinds.indexOf(it) + 1 } ?: 0,
                onSelect = { i -> filter = if (i == 0) null else kinds[i - 1] },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(16.dp))
            OutlinedButton(onClick = { c.restoreAllFromTrash(visible) }, enabled = visible.isNotEmpty()) { Text("Restaurar todos") }
            TextButton(onClick = { confirmEmpty = true }, enabled = visible.isNotEmpty()) { Text(if (filter == null) "Esvaziar lixeira" else "Excluir todos") }
        }
        Spacer(Modifier.height(8.dp))
        val palette = LocalPalette.current
        LazyColumn {
            items(visible, key = { it.id }) { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(Radius.surface).background(palette.accentSoft), contentAlignment = Alignment.Center) {
                        Icon(item.kind.icon, null, Modifier.size(19.dp), tint = palette.accent)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title.ifBlank { "(sem título)" }, style = MaterialTheme.typography.titleMedium)
                        val origin = if (item.bookTitle != null && item.kind != TrashKind.Livro) " · de “${item.bookTitle}”" else ""
                        Text(
                            "${item.kind.label}$origin · apagado ${relativeTime(item.deletedAt)}",
                            style = MaterialTheme.typography.bodySmall, color = palette.muted,
                        )
                    }
                    val left = c.trashDaysLeft(item)
                    StatusPill(if (left == 1) "1 dia" else "$left dias", if (left <= 3) palette.danger else palette.muted)
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = { confirmDelete = item }) { Text("Excluir") }
                    FilledTonalButton(onClick = { c.restoreFromTrash(item) }) { Text("Restaurar") }
                }
                Hairline()
            }
        }
    }
    confirmDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Excluir definitivamente?") },
            text = { Text("“${item.title}” será apagado e não poderá ser recuperado.") },
            confirmButton = { Button(onClick = { c.deleteForever(item); confirmDelete = null }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancelar") } },
        )
    }
    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            title = { Text(if (filter == null) "Esvaziar a lixeira?" else "Excluir todos os itens deste tipo?") },
            text = { Text("${visible.size} item(ns) serão apagados e não poderão ser recuperados.") },
            confirmButton = {
                Button(
                    onClick = { c.emptyTrash(visible); confirmEmpty = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Excluir definitivamente") }
            },
            dismissButton = { TextButton(onClick = { confirmEmpty = false }) { Text("Cancelar") } },
        )
    }
}

// ---------------------------------------------------------------- Preferências

@Composable
fun SettingsScreen(c: EscribaController) {
    val s = c.settings
    SectionPage("Preferências", "Aparência, editor e dados") {
        Column(Modifier.verticalScroll(rememberScrollState()).widthIn(max = 880.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(40.dp)) {
            SettingsGroup("Tema") {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    ThemeId.entries.forEach { t -> ThemeCard(t, selected = s.theme == t, Modifier.weight(1f)) { c.updateSettings { it.copy(theme = t) } } }
                }
            }
            SettingsGroup("Editor") {
                FontPicker(s.editorFont) { name -> c.updateSettings { it.copy(editorFont = name) } }
                Hairline()
                StepperRow("Tamanho da fonte", "${s.editorFontSize}",
                    onMinus = { c.updateSettings { it.copy(editorFontSize = (it.editorFontSize - 1).coerceAtLeast(13)) } },
                    onPlus = { c.updateSettings { it.copy(editorFontSize = (it.editorFontSize + 1).coerceAtMost(32)) } })
                Hairline()
                StepperRow("Entrelinha", "${(s.lineSpacing * 10).toInt() / 10f}",
                    onMinus = { c.updateSettings { it.copy(lineSpacing = (it.lineSpacing - 0.1f).coerceAtLeast(1.2f)) } },
                    onPlus = { c.updateSettings { it.copy(lineSpacing = (it.lineSpacing + 0.1f).coerceAtMost(2.4f)) } })
                Hairline()
                StepperRow("Largura da página", "${s.editorWidth}",
                    onMinus = { c.updateSettings { it.copy(editorWidth = (it.editorWidth - 40).coerceAtLeast(520)) } },
                    onPlus = { c.updateSettings { it.copy(editorWidth = (it.editorWidth + 40).coerceAtMost(1100)) } })
                Hairline()
                SwitchRow("Recuo de primeira linha (estilo livro)", s.firstLineIndent) { v -> c.updateSettings { it.copy(firstLineIndent = v) } }
                Hairline()
                SwitchRow("Menu lateral expandido", s.navExpanded) { v -> c.updateSettings { it.copy(navExpanded = v) } }
            }
            SettingsGroup("Dados") {
                StepperRow("Dias na lixeira", "${s.trashRetentionDays}",
                    onMinus = { c.updateSettings { it.copy(trashRetentionDays = (it.trashRetentionDays - 5).coerceAtLeast(TrashStore.MIN_RETENTION_DAYS)) } },
                    onPlus = { c.updateSettings { it.copy(trashRetentionDays = (it.trashRetentionDays + 5).coerceAtMost(TrashStore.MAX_RETENTION_DAYS)) } })
                Hairline()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Pasta dos livros", style = MaterialTheme.typography.bodyMedium)
                        Text(c.libraryDir, style = MaterialTheme.typography.bodySmall, color = LocalPalette.current.muted)
                    }
                    OutlinedButton(onClick = { revealInFileManager(c.libraryDir) }) {
                        Icon(Icons.Outlined.FolderOpen, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Abrir pasta")
                    }
                }
            }
            SettingsGroup("Atalhos") {
                listOf(
                    "Ctrl+1…7" to "Trocar de seção", "Ctrl+F" to "Localizar e substituir no capítulo", "Ctrl+Shift+F" to "Buscar no livro",
                    "Ctrl+B · Ctrl+I" to "Negrito · itálico", "F11" to "Modo foco (Esc sai)", "Ctrl+S" to "Salvar agora",
                ).forEach { (keys, what) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(what, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(
                            keys, style = MaterialTheme.typography.labelMedium, color = LocalPalette.current.muted,
                            modifier = Modifier.clip(Radius.control).background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal))
        Hairline()
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) { content() }
    }
}

@Composable
private fun StepperRow(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Stepper(label, value, onMinus, onPlus)
    }
}

@Composable
private fun ThemeCard(theme: ThemeId, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    // Prévia renderizada com as cores do próprio tema.
    EscribaTheme(theme, editorFont = "") {
        val palette = LocalPalette.current
        Column(
            modifier.clip(Radius.surface)
                .border(if (selected) 2.dp else 1.dp, if (selected) palette.accent else palette.hairline, Radius.surface)
                .clickable(onClick = onClick),
        ) {
            Row(Modifier.fillMaxWidth().height(86.dp)) {
                Column(Modifier.width(30.dp).height(86.dp).background(palette.navBackground).padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(palette.accent))
                    repeat(3) { i -> Box(Modifier.size(width = 14.dp, height = 3.dp).clip(RoundedCornerShape(2.dp)).background(if (i == 0) palette.navSelectedContent.copy(alpha = 0.7f) else palette.navContent.copy(alpha = 0.45f))) }
                }
                Box(Modifier.width(1.dp).height(86.dp).background(palette.hairline))
                Box(Modifier.weight(1f).height(86.dp).background(palette.desk).padding(10.dp)) {
                    Column(Modifier.fillMaxWidth().height(66.dp).shadow(3.dp, RoundedCornerShape(2.dp)).background(palette.paper).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Aa", color = palette.ink, style = MaterialTheme.typography.titleMedium, fontFamily = LocalFonts.current.book)
                        Box(Modifier.fillMaxWidth(0.8f).height(3.dp).background(palette.ink.copy(alpha = 0.3f)))
                        Box(Modifier.fillMaxWidth(0.6f).height(3.dp).background(palette.ink.copy(alpha = 0.3f)))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(theme.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(theme.description, style = MaterialTheme.typography.labelSmall, color = palette.muted)
                }
                if (selected) Icon(Icons.Outlined.Check, null, tint = palette.accent)
            }
        }
    }
}

@Composable
private fun FontPicker(current: String, onPick: (String) -> Unit) {
    val fonts = remember { installedBookFonts() }
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Fonte do texto", style = MaterialTheme.typography.bodyMedium)
            Text(
                "O rápido escritor contou à jovem cigana a história do farol.",
                fontFamily = LocalFonts.current.book,
                style = MaterialTheme.typography.bodyLarge,
                color = LocalPalette.current.muted,
            )
        }
        Box {
            OutlinedButton(onClick = { open = true }) { Text(current.ifBlank { "$EMBEDDED_BOOK_FONT (padrão)" }) }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DropdownMenuItem(text = { Text("$EMBEDDED_BOOK_FONT (padrão, vem com o Escriba)") }, onClick = { open = false; onPick("") })
                fonts.forEach { name ->
                    val family = remember(name) { systemFontFamily(listOf(name)) }
                    DropdownMenuItem(text = { Text(name, fontFamily = family) }, onClick = { open = false; onPick(name) })
                }
            }
        }
    }
}

@Composable
private fun SliderRow(label: String, value: String, current: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(200.dp))
        Slider(value = current, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(72.dp).padding(start = 12.dp))
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
