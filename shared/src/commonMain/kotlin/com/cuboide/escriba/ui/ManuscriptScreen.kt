package com.cuboide.escriba.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.automirrored.outlined.ViewSidebar
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FindReplace
import androidx.compose.material.icons.outlined.FormatAlignCenter
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatClear
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.SaveAs
import androidx.compose.material.icons.outlined.Subscript
import androidx.compose.material.icons.outlined.Superscript
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.SaveState
import com.cuboide.escriba.app.Section
import com.cuboide.escriba.core.model.BookPartGroup
import com.cuboide.escriba.core.model.ChapterKind
import com.cuboide.escriba.core.model.ChapterMeta
import com.cuboide.escriba.core.model.ChapterStatus
import com.cuboide.escriba.core.model.EntryKind
import com.cuboide.escriba.core.model.NoteBlock
import com.cuboide.escriba.core.storage.AppSettings
import com.cuboide.escriba.core.storage.EditorAlign
import kotlin.math.roundToInt
import com.cuboide.escriba.core.storage.ParagraphSpacing
import com.cuboide.escriba.core.text.BlockKind
import com.cuboide.escriba.core.text.MarkKind
import com.cuboide.escriba.core.text.countCharacters
import com.cuboide.escriba.core.text.countManuscriptWords
import com.cuboide.escriba.core.text.countParagraphs
import com.cuboide.escriba.core.text.readingMinutes
import com.cuboide.escriba.core.text.stripMarkup
import com.cuboide.escriba.platform.formatDateTime

@Composable
fun statusColor(status: ChapterStatus): Color = when (status) {
    ChapterStatus.Ideia -> MaterialTheme.colorScheme.outline
    ChapterStatus.Rascunho -> MaterialTheme.colorScheme.secondary
    ChapterStatus.Revisao -> MaterialTheme.colorScheme.tertiary
    ChapterStatus.Final -> MaterialTheme.colorScheme.primary
}

/** Palavras por página de livro, para a estimativa "página x de y" da barra de status. */
private const val WORDS_PER_PAGE = 250

/**
 * Abaixo de ~8000 px do começo do texto o campo deixa de receber cliques (só dava para mexer pelo teclado). O clique
 * fundo é pego na janela da rolagem, onde ainda chega, e traduzido para a posição no campo.
 */
private class DeepClicks {
    var viewport: LayoutCoordinates? = null
    var box: LayoutCoordinates? = null
    /** Clique ([anchor] == [current]) ou arrasto; [clicks] = 2 para duplo clique (palavra). */
    var onSelect: (anchor: Offset, current: Offset, clicks: Int) -> Unit = { _, _, _ -> }

    private fun local(v: Offset): Offset? {
        val vp = viewport?.takeIf { it.isAttached } ?: return null
        val b = box?.takeIf { it.isAttached } ?: return null
        return b.localPositionOf(vp, v)
    }

    /** Mouse sobre a parte funda do texto: o campo não responde ali, então o cursor de texto vem daqui. */
    private var hovering by mutableStateOf(false)

    fun modifier(): Modifier = Modifier.onGloballyPositioned { viewport = it }
        .pointerHoverIcon(if (hovering) PointerIcon.Text else PointerIcon.Default)
        .pointerInput(Unit) {
        awaitPointerEventScope {
            var lastTime = 0L
            var lastAt = Offset.Zero
            while (true) {
                val down = awaitPointerEvent(PointerEventPass.Initial)
                val start = down.changes.firstOrNull()
                if (down.type == PointerEventType.Move || down.type == PointerEventType.Enter) {
                    hovering = start?.let { p -> local(p.position)?.let { it.y > DEEP_CLICK_PX && it.x in 0f..(box?.size?.width ?: 0).toFloat() } } == true
                } else if (down.type == PointerEventType.Exit) hovering = false
                if (down.type != PointerEventType.Press || start == null || !down.buttons.isPrimaryPressed) continue
                val anchor = local(start.position) ?: continue
                if (anchor.y <= DEEP_CLICK_PX) continue
                val double = start.uptimeMillis - lastTime < 450 && (start.position - lastAt).getDistance() < viewConfiguration.touchSlop * 2
                lastTime = start.uptimeMillis
                lastAt = start.position
                down.changes.forEach { it.consume() }
                onSelect(anchor, anchor, if (double) 2 else 1)
                while (true) {
                    val e = awaitPointerEvent(PointerEventPass.Initial)
                    val ch = e.changes.firstOrNull() ?: break
                    ch.consume()
                    if (!ch.pressed) break
                    if (!double) local(ch.position)?.let { onSelect(anchor, it, 1) }
                }
            }
        }
    }
}

/** Altura (px) a partir da qual o clique do campo de texto deixa de acertar a posição. */
private const val DEEP_CLICK_PX = 7000f
private val CHAPTERS_WIDTH = 264.dp
private val INSPECTOR_WIDTH = 300.dp
/** Largura mínima para a folha e a barra de status caberem sem espremer. */
private val MIN_DESK_WIDTH = 460.dp

@Composable
fun ManuscriptScreen(c: EscribaController) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Em janela estreita os painéis se recolhem sozinhos (primeiro o inspetor, depois os capítulos),
        // para a folha nunca ficar sem espaço. A preferência de mostrar cada painel continua guardada.
        val roomForChapters = maxWidth >= MIN_DESK_WIDTH + CHAPTERS_WIDTH
        val roomForBoth = maxWidth >= MIN_DESK_WIDTH + CHAPTERS_WIDTH + INSPECTOR_WIDTH
        val chaptersVisible = !c.focusMode && c.showChapters && roomForChapters
        val inspectorVisible = !c.focusMode && c.showInspector &&
            (if (chaptersVisible) roomForBoth else maxWidth >= MIN_DESK_WIDTH + INSPECTOR_WIDTH)
        Row(Modifier.fillMaxSize()) {
            if (chaptersVisible) {
                ChaptersPanel(c, Modifier.width(CHAPTERS_WIDTH).fillMaxHeight())
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                if (!c.focusMode) WritingToolbar(c, chaptersVisible, inspectorVisible)
                if (c.findVisible && !c.focusMode) FindBar(c)
                Desk(c, Modifier.weight(1f))
                if (!c.focusMode) StatusBar(c)
            }
            if (inspectorVisible) {
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Inspector(c, Modifier.width(INSPECTOR_WIDTH).fillMaxHeight())
            }
        }
    }
}

// ---------------------------------------------------------------- Capítulos

@Composable
private fun ChaptersPanel(c: EscribaController, modifier: Modifier) {
    val book = c.book ?: return
    val chapters = book.manifest.chapters
    val numbers = c.chapterNumbers
    var partsMenu by remember { mutableStateOf(false) }
    Column(modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Capítulos", style = MaterialTheme.typography.titleLarge)
                Text("${numbers.size} capítulo(s) · ${chapters.size - numbers.size} parte(s)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                IconAction(Icons.Outlined.AutoStories, "Nova parte do livro (prólogo, dedicatória, epílogo…)", onClick = { partsMenu = true })
                DropdownMenu(expanded = partsMenu, onDismissRequest = { partsMenu = false }) {
                    PartKindItems { kind -> partsMenu = false; c.addPart(kind) }
                }
            }
            IconAction(Icons.Outlined.Add, "Novo capítulo", onClick = { c.addChapter() })
        }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            itemsIndexed(chapters, key = { _, ch -> ch.id }) { index, chapter ->
                ChapterItem(c, chapter, numbers[chapter.id], index, chapters.size)
            }
        }
        val goal = book.manifest.bookGoal
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row {
                Text("Livro", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                Text(if (goal > 0) "${c.bookWords} / $goal" else "${c.bookWords}", style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { if (goal > 0) (c.bookWords.toFloat() / goal).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                drawStopIndicator = {},
            )
        }
    }
}

/** Itens de menu com as partes do livro agrupadas (antes, entre e depois da história). */
@Composable
private fun PartKindItems(onPick: (ChapterKind) -> Unit) {
    BookPartGroup.entries.forEach { group ->
        Text(
            group.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 2.dp),
        )
        ChapterKind.entries.filter { it.group == group }.forEach { kind ->
            DropdownMenuItem(text = { Text(kind.label) }, onClick = { onPick(kind) })
        }
    }
    HorizontalDivider()
    Text(
        "Não contam como capítulo nem entram na numeração.", style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp).widthIn(max = 220.dp),
    )
}

@Composable
private fun ChapterItem(c: EscribaController, chapter: ChapterMeta, number: Int?, index: Int, count: Int) {
    val selected = chapter.id == c.currentChapterId
    var menu by remember { mutableStateOf(false) }
    var kindMenu by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    val words = if (selected) c.currentWords else chapter.words
    val color = statusColor(chapter.status)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent)
            .clickable { c.selectChapter(chapter.id) }
            .padding(start = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(if (selected) MaterialTheme.colorScheme.primary else color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                number?.toString() ?: chapter.kind.label.take(1),
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else color,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                chapter.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(6.dp))
                val kind = if (chapter.kind == ChapterKind.Capitulo) "" else "${chapter.kind.label} · "
                Text("$kind${chapter.status.label} · $words palavras", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Box {
            IconAction(Icons.Outlined.MoreHoriz, "Opções do capítulo", onClick = { menu = true })
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Renomear") }, onClick = { menu = false; rename = true })
                DropdownMenuItem(text = { Text("Tipo: ${chapter.kind.label}…") }, onClick = { menu = false; kindMenu = true })
                if (index > 0) DropdownMenuItem(text = { Text("Mover para cima") }, leadingIcon = { Icon(Icons.Outlined.KeyboardArrowUp, null) }, onClick = { menu = false; c.moveChapter(chapter.id, -1) })
                if (index < count - 1) DropdownMenuItem(text = { Text("Mover para baixo") }, leadingIcon = { Icon(Icons.Outlined.KeyboardArrowDown, null) }, onClick = { menu = false; c.moveChapter(chapter.id, 1) })
                HorizontalDivider()
                DropdownMenuItem(text = { Text("Enviar para a lixeira") }, onClick = { menu = false; c.trashChapter(chapter.id) })
            }
            DropdownMenu(expanded = kindMenu, onDismissRequest = { kindMenu = false }) {
                DropdownMenuItem(text = { Text("Capítulo (numerado)") }, onClick = { kindMenu = false; c.setChapterKind(chapter.id, ChapterKind.Capitulo) })
                HorizontalDivider()
                PartKindItems { kind -> kindMenu = false; c.setChapterKind(chapter.id, kind) }
            }
        }
    }
    if (rename) TextInputDialog("Renomear capítulo", chapter.title, onDismiss = { rename = false }) {
        rename = false
        c.renameChapter(chapter.id, it)
    }
}

// ---------------------------------------------------------------- Barra de escrita

@Composable
private fun WritingToolbar(c: EscribaController, chaptersVisible: Boolean, inspectorVisible: Boolean) {
    var footnoteDialog by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToolAction(Icons.AutoMirrored.Outlined.ViewSidebar, "Mostrar/ocultar capítulos", onClick = { c.togglePanel(chapters = true, visible = chaptersVisible) }, selected = chaptersVisible)
                ToolbarDivider()
                ToolAction(Icons.AutoMirrored.Outlined.Undo, "Desfazer (Ctrl+Z)", onClick = { c.undo(); c.requestEditorFocus() }, enabled = c.canUndo)
                ToolAction(Icons.AutoMirrored.Outlined.Redo, "Refazer (Ctrl+Y)", onClick = { c.redo(); c.requestEditorFocus() }, enabled = c.canRedo)
                ToolbarDivider()
                MeasuresMenu(c)
                ToolbarDivider()
                ToolAction(Icons.Outlined.FormatBold, "Negrito (Ctrl+B)", onClick = { c.toggleMark(MarkKind.Bold) })
                ToolAction(Icons.Outlined.FormatItalic, "Itálico (Ctrl+I)", onClick = { c.toggleMark(MarkKind.Italic) })
                GlyphAction("Aa", "Versalete") { c.toggleMark(MarkKind.SmallCaps) }
                ToolAction(Icons.Outlined.Superscript, "Sobrescrito", onClick = { c.toggleMark(MarkKind.Superscript) })
                ToolAction(Icons.Outlined.Subscript, "Subscrito", onClick = { c.toggleMark(MarkKind.Subscript) })
                GlyphAction("“ ”", "Aspas") { c.wrapQuotes() }
                GlyphAction("—", "Travessão de diálogo") { c.insertText("— ") }
                GlyphAction("⁂", "Quebra de cena") { c.insertSceneBreak() }
                ToolAction(Icons.Outlined.Image, "Inserir imagem", onClick = { c.insertImage() })
                GlyphAction("¹", "Nota de rodapé") { footnoteDialog = true }
                ToolbarDivider()
                GlyphAction("H1", "Título") { c.toggleBlockStyle(BlockKind.Heading1) }
                GlyphAction("H2", "Subtítulo") { c.toggleBlockStyle(BlockKind.Heading2) }
                GlyphAction("H3", "Chamada (texto pequeno acima do título)") { c.toggleBlockStyle(BlockKind.Heading3) }
                ToolAction(Icons.AutoMirrored.Outlined.FormatListBulleted, "Lista", onClick = { c.toggleBlockStyle(BlockKind.ListItem) })
                ToolAction(Icons.Outlined.FormatQuote, "Citação em bloco", onClick = { c.toggleBlockStyle(BlockKind.Quote) })
                ToolAction(Icons.Outlined.FormatAlignCenter, "Centralizar parágrafo", onClick = { c.toggleCentered() })
                ToolAction(Icons.Outlined.FormatClear, "Limpar formatação", onClick = { c.clearFormat() })
                ToolbarDivider()
                ToolAction(Icons.Outlined.FindReplace, "Localizar e substituir (Ctrl+F)", onClick = { if (c.findVisible) c.closeFind() else c.openFind() }, selected = c.findVisible)
                ToolAction(Icons.Outlined.SaveAs, "Salvar versão do capítulo", onClick = { c.snapshotCurrent(); c.message = "Versão salva." })
                ToolAction(Icons.Outlined.CenterFocusStrong, "Modo foco (F11)", onClick = { c.focusMode = true })
            }
            ToolAction(Icons.AutoMirrored.Outlined.ViewSidebar, "Mostrar/ocultar inspetor", onClick = { c.togglePanel(chapters = false, visible = inspectorVisible) }, selected = inspectorVisible)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    if (footnoteDialog) FootnoteDialog(onDismiss = { footnoteDialog = false; c.requestEditorFocus() }) { text ->
        footnoteDialog = false
        c.addFootnote(text)
    }
}

/** Medidas do texto: fonte, entrelinha, largura da página, recuo, alinhamento e correções automáticas. */
@Composable
private fun MeasuresMenu(c: EscribaController) {
    val s = c.settings
    var open by remember { mutableStateOf(false) }
    Box {
        ToolAction(Icons.Outlined.TextFields, "Medidas e parágrafo", onClick = { open = true }, selected = open)
        DropdownMenu(expanded = open, onDismissRequest = { open = false; c.requestEditorFocus() }) {
            StepperItem("Fonte", "${s.editorFontSize}",
                onMinus = { c.updateSettings { it.copy(editorFontSize = (it.editorFontSize - 1).coerceAtLeast(13)) } },
                onPlus = { c.updateSettings { it.copy(editorFontSize = (it.editorFontSize + 1).coerceAtMost(32)) } })
            StepperItem("Entrelinha", "${(s.lineSpacing * 10).toInt() / 10f}",
                onMinus = { c.updateSettings { it.copy(lineSpacing = (it.lineSpacing - 0.1f).coerceAtLeast(1.2f)) } },
                onPlus = { c.updateSettings { it.copy(lineSpacing = (it.lineSpacing + 0.1f).coerceAtMost(2.4f)) } })
            StepperItem("Largura", "${s.editorWidth}",
                onMinus = { c.updateSettings { it.copy(editorWidth = (it.editorWidth - 40).coerceAtLeast(520)) } },
                onPlus = { c.updateSettings { it.copy(editorWidth = (it.editorWidth + 40).coerceAtMost(1100)) } })
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Recuo de primeira linha") },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.FormatIndentIncrease, null) },
                trailingIcon = { Checkbox(checked = s.firstLineIndent, onCheckedChange = null) },
                onClick = { c.updateSettings { it.copy(firstLineIndent = !it.firstLineIndent) } },
            )
            if (s.firstLineIndent) {
                StepperItem("Tamanho do recuo", "${(s.indentSize * 10).toInt() / 10f}",
                    onMinus = { c.updateSettings { it.copy(indentSize = (it.indentSize - 0.4f).coerceAtLeast(0.4f)) } },
                    onPlus = { c.updateSettings { it.copy(indentSize = (it.indentSize + 0.4f).coerceAtMost(4f)) } })
            }
            Text("ALINHAMENTO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp, top = 8.dp))
            Row(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                EditorAlign.entries.forEach { a ->
                    FilterChip(s.textAlign == a, { c.updateSettings { it.copy(textAlign = a) } }, label = { Text(a.label, style = MaterialTheme.typography.labelSmall) })
                }
            }
            Text("ESPAÇO ENTRE PARÁGRAFOS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp, top = 8.dp))
            Row(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ParagraphSpacing.entries.forEach { preset ->
                    FilterChip(
                        s.paragraphSpacing == preset.em, { c.updateSettings { it.copy(paragraphSpacing = preset.em) } },
                        label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
            StepperItem("Ajuste fino", "${(s.paragraphSpacing * 10).toInt() / 10f}",
                onMinus = { c.updateSettings { it.copy(paragraphSpacing = (((it.paragraphSpacing - 0.1f) * 10).roundToInt() / 10f).coerceAtLeast(0f)) } },
                onPlus = { c.updateSettings { it.copy(paragraphSpacing = (((it.paragraphSpacing + 0.1f) * 10).roundToInt() / 10f).coerceAtMost(2f)) } })
            HorizontalDivider(Modifier.padding(top = 6.dp))
            DropdownMenuItem(
                text = { Column { Text("Correções ao digitar"); Text("-- vira —, ... vira …, aspas curvas, maiúscula no início", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                trailingIcon = { Checkbox(checked = s.smartTypography, onCheckedChange = null) },
                onClick = { c.updateSettings { it.copy(smartTypography = !it.smartTypography) } },
            )
            DropdownMenuItem(
                text = { Text("Restaurar medidas padrão") },
                onClick = {
                    val d = AppSettings()
                    c.updateSettings {
                        it.copy(
                            editorFontSize = d.editorFontSize, lineSpacing = d.lineSpacing, editorWidth = d.editorWidth,
                            firstLineIndent = d.firstLineIndent, indentSize = d.indentSize, textAlign = d.textAlign,
                            paragraphSpacing = d.paragraphSpacing,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun StepperItem(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.width(280.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        IconAction(Icons.Outlined.Remove, "Diminuir $label", onClick = onMinus)
        Text(value, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
        IconAction(Icons.Outlined.Add, "Aumentar $label", onClick = onPlus)
    }
}

/** Botão com um caractere tipográfico no lugar do ícone. */
@Composable
private fun GlyphAction(glyph: String, tooltip: String, size: Int = 40, onClick: () -> Unit) {
    WithTooltip(tooltip) {
        Box(
            Modifier.size(size.dp).clip(CircleShape).focusProperties { canFocus = false }.clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                glyph, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold,
                style = if (glyph.length > 1) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleLarge,
            )
        }
    }
}

/** Botão da barra de escrita: não pega o foco, para o cursor continuar no texto. */
@Composable
private fun ToolAction(icon: ImageVector, tooltip: String, onClick: () -> Unit, enabled: Boolean = true, selected: Boolean = false) =
    IconAction(icon, tooltip, onClick, enabled = enabled, selected = selected, takesFocus = false)

@Composable
private fun ToolbarDivider() {
    Box(Modifier.padding(horizontal = 6.dp).width(1.dp).height(22.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

@Composable
private fun FootnoteDialog(initial: String = "", onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nota de rodapé") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("A chamada entra logo depois do texto selecionado. Na exportação a nota vai para o pé da página (DOCX) ou para o fim do capítulo.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(text, { text = it }, label = { Text("Texto da nota") }, minLines = 3, modifier = Modifier.fillMaxWidth().focusRequester(focus))
            }
        },
        confirmButton = { Button(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text("Inserir nota") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

// ---------------------------------------------------------------- Folha

@Composable
private fun Desk(c: EscribaController, modifier: Modifier) {
    val palette = LocalPalette.current
    val chapter = c.currentChapter
    Box(modifier.fillMaxWidth().background(palette.desk), contentAlignment = Alignment.TopCenter) {
        if (chapter == null) {
            EmptyState(Icons.Outlined.Add, "Nenhum capítulo", "Crie o primeiro capítulo para começar a escrever.") {
                TextButton(onClick = { c.addChapter() }) { Text("Novo capítulo") }
            }
            return@Box
        }
        // Cada capítulo tem a própria rolagem: trocar de capítulo volta ao topo.
        key(chapter.id) { Page(c, chapter) }
        if (c.focusMode) {
            Row(Modifier.align(Alignment.TopEnd).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${c.currentWords} palavras · Esc para sair",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.ink.copy(alpha = 0.45f),
                )
                IconAction(Icons.Outlined.Close, "Sair do modo foco", onClick = { c.focusMode = false }, tint = palette.ink.copy(alpha = 0.45f))
            }
        }
    }
}

@Composable
private fun Page(c: EscribaController, chapter: ChapterMeta) {
    val palette = LocalPalette.current
    val fonts = LocalFonts.current
    val s = c.settings
    val scroll = rememberScrollState()
    val deep = remember { DeepClicks() }
    // A barra de rolagem fica na borda da mesa (não da folha) e dá para arrastá-la para andar pelo capítulo.
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
        Modifier.fillMaxHeight().then(deep.modifier()).verticalScroll(scroll).padding(top = 24.dp).widthIn(max = s.editorWidth.dp)
            .shadow(10.dp, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
            .background(palette.paper, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
            // Clique na folha fora do texto (margens, abaixo do fim) leva o cursor para o fim do capítulo.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { c.focusEditorAtEnd() }
            .padding(horizontal = 64.dp)
            .padding(top = 56.dp, bottom = 160.dp),
    ) {
        val number = c.chapterNumbers[chapter.id]
        Text(
            if (number != null) "CAPÍTULO $number" else chapter.kind.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 3.sp,
            color = palette.accent,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(8.dp))
        ChapterTitleField(c, chapter)
        Ornament(palette.accent, Modifier.align(Alignment.CenterHorizontally).padding(vertical = 22.dp))
        ManuscriptEditor(c, deep, s, fonts.book, palette)
    }
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(scroll),
        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 4.dp),
    )
    }
}

/** Título editável direto na folha; Enter grava e leva o cursor para o texto. */
@Composable
private fun ChapterTitleField(c: EscribaController, chapter: ChapterMeta) {
    val palette = LocalPalette.current
    val focus = remember { FocusRequester() }
    var value by remember { mutableStateOf(TextFieldValue(chapter.title)) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(c.titleFocusRequests) {
        if (c.titleFocusRequests == 0) return@LaunchedEffect
        value = TextFieldValue(chapter.title, TextRange(0, chapter.title.length))
        focus.requestFocus()
    }
    LaunchedEffect(chapter.title) { if (!focused) value = TextFieldValue(chapter.title) }
    fun commit() {
        if (value.text.isNotBlank() && value.text.trim() != chapter.title) c.renameChapter(chapter.id, value.text)
    }
    BasicTextField(
        value = value,
        onValueChange = { value = it.copy(text = it.text.replace("\n", "")) },
        singleLine = true,
        textStyle = MaterialTheme.typography.headlineMedium.copy(color = palette.ink, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(palette.accent),
        modifier = Modifier.fillMaxWidth().focusRequester(focus)
            .onFocusChanged { if (focused && !it.isFocused) commit(); focused = it.isFocused }
            .onPreviewKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown && (e.key == Key.Enter || e.key == Key.NumPadEnter)) {
                    commit()
                    c.requestEditorFocus()
                    true
                } else false
            },
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ManuscriptEditor(c: EscribaController, deep: DeepClicks, s: AppSettings, bookFont: androidx.compose.ui.text.font.FontFamily, palette: EscribaPalette) {
    val focus = remember { FocusRequester() }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var footnoteDialog by remember { mutableStateOf(false) }
    LaunchedEffect(c.editorFocusRequests) { if (c.editorFocusRequests > 0) focus.requestFocus() }
    val align = when (s.textAlign) {
        EditorAlign.Esquerda -> TextAlign.Start
        EditorAlign.Justificado -> TextAlign.Justify
        EditorAlign.Centro -> TextAlign.Center
        EditorAlign.Direita -> TextAlign.End
    }
    val styling = ManuscriptStyling(
        selection = c.editor.selection,
        matches = c.matches,
        currentMatch = c.currentMatch,
        ink = palette.ink,
        accent = palette.accent,
        matchColor = palette.match,
        currentMatchColor = palette.currentMatch,
        align = align,
        indentSp = if (s.firstLineIndent) s.indentSize * s.editorFontSize else 0f,
        lineHeightEm = s.lineSpacing,
        spacerSp = rememberParagraphSpacer(bookFont, s.editorFontSize.toFloat(), s.lineSpacing, s.paragraphSpacing),
        focusMode = c.focusMode,
    )
    // Onde foi o último clique: cada parágrafo é um parágrafo separado na tela, e clicar depois do fim de uma
    // linha devolve a mesma posição que o começo da linha seguinte. Com o lugar do clique, o cursor fica
    // na linha clicada.
    val press = remember { arrayOfNulls<Offset>(1) }
    // Ao ganhar foco, o Compose rola a folha para mostrar o campo inteiro; no meio de um clique isso move o
    // texto sob o mouse e o clique vira arrasto (o cursor ia parar no começo). Pedidos do tamanho de várias
    // linhas viram só o retângulo do cursor; os pedidos pequenos (cursor ao digitar) passam como estão.
    val currentLayout by rememberUpdatedState(layout)
    val currentSelection by rememberUpdatedState(c.editor)
    val currentStyling by rememberUpdatedState(styling)
    deep.onSelect = { anchor, current, clicks ->
        val l = currentLayout
        if (l != null) {
            val v = currentSelection
            val st = currentStyling
            // Clicar depois do fim de uma linha devolve o começo da seguinte: fica na linha clicada.
            fun at(pos: Offset): Int {
                var shown = l.getOffsetForPosition(pos)
                val line = l.getLineForVerticalPosition(pos.y)
                if (l.getLineForOffset(shown) > line) shown = l.getLineEnd(line, visibleEnd = true)
                return st.rawOffset(v.text, shown)
            }
            val range = if (clicks == 2) {
                val w = l.getWordBoundary(l.getOffsetForPosition(anchor))
                TextRange(st.rawOffset(v.text, w.start), st.rawOffset(v.text, w.end))
            } else {
                TextRange(at(anchor), at(current))
            }
            c.onEditorChange(v.copy(selection = range))
            c.requestEditorFocus()
        }
    }
    val manyLines = with(LocalDensity.current) { (s.editorFontSize * s.lineSpacing * 3).sp.toPx() }
    val cursorOnly = remember(manyLines) {
        object : BringIntoViewResponder {
            override fun calculateRectForParent(localRect: Rect): Rect {
                val l = currentLayout ?: return localRect
                if (localRect.height <= manyLines) return localRect
                val value = currentSelection
                val shown = currentStyling.displayOffset(value.text, value.selection.end).coerceIn(0, l.layoutInput.text.length)
                return l.getCursorRect(shown)
            }

            override suspend fun bringChildIntoView(localRect: () -> Rect?) = Unit
        }
    }
    Box(
        Modifier.bringIntoViewResponder(cursorOnly).onGloballyPositioned { deep.box = it },
    ) {
        BasicTextField(
            value = c.editor,
            onValueChange = { v ->
                // O campo avisa a posição no apertar e de novo no soltar: o clique vale até a próxima tecla.
                val p = press[0]
                val l = layout
                var value = v
                if (p != null && l != null && v.text == c.editor.text && v.selection.collapsed) {
                    val clickedLine = l.getLineForVerticalPosition(p.y)
                    val shown = styling.displayOffset(v.text, v.selection.start)
                    if (l.getLineForOffset(shown) > clickedLine) {
                        value = v.copy(selection = TextRange(styling.rawOffset(v.text, l.getLineEnd(clickedLine, visibleEnd = true))))
                    }
                }
                c.onEditorChange(value)
            },
            textStyle = TextStyle(fontFamily = bookFont, fontSize = s.editorFontSize.sp, lineHeight = s.lineSpacing.em, color = palette.ink),
            cursorBrush = SolidColor(palette.accent),
            visualTransformation = styling,
            onTextLayout = { layout = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 360.dp).focusRequester(focus)
                .onFocusChanged { c.editorFocused = it.isFocused }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.type == PointerEventType.Press) press[0] = event.changes.firstOrNull()?.position
                        }
                    }
                }
                // Teclas (setas, End…) não são clique: esquece a posição guardada.
                .onPreviewKeyEvent { press[0] = null; false },
        )
        val sel = c.editor.selection
        val l = layout
        val selStart = styling.displayOffset(c.editor.text, sel.min)
        if (c.editorFocused && !footnoteDialog && !sel.collapsed && l != null && selStart < l.layoutInput.text.length) {
            val box = l.getBoundingBox(selStart)
            val gap = with(LocalDensity.current) { 52.dp.roundToPx() }
            Popup(
                offset = IntOffset(box.left.toInt().coerceAtLeast(0), (box.top.toInt() - gap).coerceAtLeast(-gap)),
                properties = PopupProperties(focusable = false),
            ) {
                SelectionBar(c, onFootnote = { footnoteDialog = true })
            }
        }
    }
    if (footnoteDialog) FootnoteDialog(onDismiss = { footnoteDialog = false; c.requestEditorFocus() }) { text ->
        footnoteDialog = false
        c.addFootnote(text)
    }
}

/** Barra que aparece sobre o texto selecionado (como a "barrinha flutuante" de editores de livro). */
@Composable
private fun SelectionBar(c: EscribaController, onFootnote: () -> Unit) {
    var universeMenu by remember { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(12.dp), shadowElevation = 6.dp, tonalElevation = 2.dp) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            SmallAction(Icons.Outlined.FormatBold, "Negrito (Ctrl+B)") { c.toggleMark(MarkKind.Bold) }
            SmallAction(Icons.Outlined.FormatItalic, "Itálico (Ctrl+I)") { c.toggleMark(MarkKind.Italic) }
            GlyphAction("Aa", "Versalete", size = 34) { c.toggleMark(MarkKind.SmallCaps) }
            SmallAction(Icons.Outlined.Superscript, "Sobrescrito") { c.toggleMark(MarkKind.Superscript) }
            SmallAction(Icons.Outlined.Subscript, "Subscrito") { c.toggleMark(MarkKind.Subscript) }
            GlyphAction("“ ”", "Aspas", size = 34) { c.wrapQuotes() }
            SmallAction(Icons.Outlined.FormatAlignCenter, "Centralizar parágrafo") { c.toggleCentered() }
            GlyphAction("¹", "Nota de rodapé", size = 34) { onFootnote() }
            Box {
                SmallAction(Icons.Outlined.PersonAdd, "Criar no Universo") { universeMenu = true }
                DropdownMenu(expanded = universeMenu, onDismissRequest = { universeMenu = false }) {
                    EntryKind.entries.forEach { kind ->
                        DropdownMenuItem(text = { Text("Ficha de ${kind.label.lowercase()}") }, onClick = { universeMenu = false; c.createEntryFromSelection(kind) })
                    }
                }
            }
            SmallAction(Icons.Outlined.FormatClear, "Limpar formatação") { c.clearFormat() }
        }
    }
}

@Composable
private fun SmallAction(icon: ImageVector, tooltip: String, onClick: () -> Unit) {
    WithTooltip(tooltip) {
        Box(Modifier.size(34.dp).clip(CircleShape).focusProperties { canFocus = false }.clickable(onClick = onClick), contentAlignment = Alignment.Center) {
            Icon(icon, tooltip, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusBar(c: EscribaController) {
    val book = c.book ?: return
    val words = c.currentWords
    val goal = book.manifest.dailyGoal
    val today = c.productivity.today.words
    val text = c.editor.text
    val pages = maxOf(1, (words + WORDS_PER_PAGE - 1) / WORDS_PER_PAGE)
    val cursorPage = minOf(pages, 1 + countManuscriptWords(text.substring(0, c.editor.selection.end.coerceIn(0, text.length))) / WORDS_PER_PAGE)
    val notes = c.currentFootnotes.size
    val style = MaterialTheme.typography.labelMedium
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    // Cada número fica numa linha só; em janela estreita os números rolam de lado em vez de quebrar letra por letra.
    @Composable
    fun Stat(value: String, color: Color = Color.Unspecified) = Text(value, style = style, color = color, maxLines = 1, softWrap = false)

    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Stat("$words palavras")
                Stat("${countParagraphs(stripMarkup(text))} parágrafos", muted)
                Stat("página $cursorPage de $pages", muted)
                Stat("${readingMinutes(words)} min de leitura", muted)
                if (notes > 0) Stat("$notes nota(s)", muted)
                Stat("Livro: ${c.bookWords}", muted)
                if (goal > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Stat("Hoje $today/$goal", muted)
                        Spacer(Modifier.width(8.dp))
                        LinearProgressIndicator(
                            progress = { (today.toFloat() / goal).coerceIn(0f, 1f) },
                            modifier = Modifier.width(80.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                            drawStopIndicator = {},
                        )
                    }
                }
            }
            // O estado de salvamento fica sempre visível à direita.
            Spacer(Modifier.width(16.dp))
            val (label, color) = when (c.saveState) {
                SaveState.Salvo -> "Salvo" to MaterialTheme.colorScheme.primary
                SaveState.Pendente -> "Editando…" to muted
                SaveState.Salvando -> "Salvando…" to muted
            }
            Box(Modifier.size(7.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Stat(label, muted)
        }
    }
}

// ---------------------------------------------------------------- Localizar

@Composable
private fun FindBar(c: EscribaController) {
    // Ctrl+F leva o cursor para "Localizar" com a busca anterior selecionada, para não digitar no texto do livro.
    val queryFocus = remember { FocusRequester() }
    var query by remember { mutableStateOf(TextFieldValue(c.findQuery)) }
    LaunchedEffect(c.findFocusRequests) {
        query = TextFieldValue(c.findQuery, TextRange(0, c.findQuery.length))
        queryFocus.requestFocus()
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        if (it.text != c.findQuery) { c.findQuery = it.text; c.refreshMatches() }
                    },
                    placeholder = { Text("Localizar") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).focusRequester(queryFocus).onPreviewKeyEvent { e ->
                        if (e.type == KeyEventType.KeyDown && e.key == Key.Enter) { c.goToMatch(1); true }
                        else if (e.type == KeyEventType.KeyDown && e.key == Key.Escape) { c.closeFind(); c.requestEditorFocus(); true }
                        else false
                    },
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = c.replaceText,
                    onValueChange = { c.replaceText = it },
                    placeholder = { Text("Substituir por") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                )
                Text(
                    if (c.matches.isEmpty()) "0 de 0" else "${c.currentMatch + 1} de ${c.matches.size}",
                    style = MaterialTheme.typography.labelMedium,
                )
                IconAction(Icons.Outlined.KeyboardArrowUp, "Anterior", onClick = { c.goToMatch(-1) })
                IconAction(Icons.Outlined.KeyboardArrowDown, "Próxima (Enter)", onClick = { c.goToMatch(1) })
                IconAction(Icons.Outlined.Close, "Fechar (Esc)", onClick = { c.closeFind(); c.requestEditorFocus() })
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(c.findOptions.matchCase, { c.findOptions = c.findOptions.copy(matchCase = !c.findOptions.matchCase); c.refreshMatches() }, label = { Text("Aa") })
                FilterChip(c.findOptions.wholeWord, { c.findOptions = c.findOptions.copy(wholeWord = !c.findOptions.wholeWord); c.refreshMatches() }, label = { Text("Palavra inteira") })
                FilterChip(c.findOptions.regex, { c.findOptions = c.findOptions.copy(regex = !c.findOptions.regex); c.refreshMatches() }, label = { Text(".* Regex") })
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { c.replaceCurrent() }, enabled = c.currentMatch >= 0) { Text("Substituir") }
                TextButton(onClick = { c.replaceAllInChapter() }, enabled = c.matches.isNotEmpty()) { Text("Tudo (capítulo)") }
                TextButton(onClick = { c.replaceAllInBook() }, enabled = c.findQuery.isNotEmpty()) { Text("Tudo (livro)") }
                TextButton(onClick = { c.deleteAllInChapter() }, enabled = c.matches.isNotEmpty()) { Text("Apagar todas") }
            }
        }
    }
}

/** Fio com losango central, desenhado (não depende de glifos da fonte). */
@Composable
private fun Ornament(color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(36.dp).height(1.dp).background(color.copy(alpha = 0.5f)))
        Box(Modifier.padding(horizontal = 10.dp).size(6.dp).rotate(45f).background(color.copy(alpha = 0.8f)))
        Box(Modifier.width(36.dp).height(1.dp).background(color.copy(alpha = 0.5f)))
    }
}

@Composable
fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onChange(!checked) }.padding(end = 12.dp)) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

// ---------------------------------------------------------------- Inspetor

@Composable
private fun Inspector(c: EscribaController, modifier: Modifier) {
    val book = c.book ?: return
    val chapter = c.currentChapter ?: return
    val text = c.editor.text
    Column(
        modifier.background(MaterialTheme.colorScheme.surfaceContainerLow).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        InspectorBlock("Status") {
            ChapterStatus.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { s ->
                        FilterChip(
                            selected = chapter.status == s,
                            onClick = { c.setChapterStatus(chapter.id, s) },
                            label = { Text(s.label) },
                            leadingIcon = { Box(Modifier.size(8.dp).clip(CircleShape).background(statusColor(s))) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        InspectorBlock("Estatísticas") {
            val words = c.currentWords
            val plain = remember(text) { stripMarkup(text) }
            InfoRow("Palavras", "$words")
            InfoRow("Caracteres", "${countCharacters(plain)}")
            InfoRow("Parágrafos", "${countParagraphs(plain)}")
            InfoRow("Leitura", "${readingMinutes(words)} min")
        }
        FootnotesBlock(c)
        // Nome e apelidos, palavra inteira, sem diferenciar acentos; as mais citadas primeiro.
        val mentioned = remember(text, book.universe) { c.entriesInCurrentChapter() }
        InspectorBlock("Aparecem neste capítulo") {
            if (mentioned.isEmpty()) {
                Text("Nenhuma ficha do Universo citada.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            mentioned.forEach { (e, count) ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { c.openUniverseEntry(e.id) }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EntryAvatar(c, e, 26)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.name, style = MaterialTheme.typography.bodyMedium)
                        Text(e.kind.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${count}×", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        val beats = book.outline.beats.filter { it.chapterId == chapter.id }
        if (beats.isNotEmpty()) {
            InspectorBlock("Cenas do roteiro") {
                beats.forEach { b ->
                    Text("• ${b.title.ifBlank { "Cena sem título" }}", style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable { c.goTo(Section.Enredo) })
                }
            }
        }
        NoteBlocksBlock(c)
        InspectorBlock("Versões recentes") {
            if (c.snapshots.isEmpty()) {
                Text("Nenhuma versão salva ainda.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            c.snapshots.take(3).forEach { s ->
                Column(Modifier.padding(vertical = 2.dp)) {
                    Text(s.label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text("${formatDateTime(s.createdAt)} · ${s.words} palavras", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextButton(onClick = { c.goTo(Section.Versoes) }) { Text("Ver todas e comparar") }
        }
    }
}

/** Notas de rodapé do capítulo, na ordem do texto, com o texto editável. */
@Composable
private fun FootnotesBlock(c: EscribaController) {
    val notes = c.currentFootnotes
    InspectorBlock("Notas de rodapé") {
        if (notes.isEmpty()) {
            Text("Selecione um trecho e use ¹ na barra para criar uma nota.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        notes.forEachIndexed { i, (id, noteText) ->
            Row(verticalAlignment = Alignment.Top) {
                Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = LocalPalette.current.accent, modifier = Modifier.padding(top = 12.dp, end = 8.dp))
                OutlinedTextField(
                    value = noteText, onValueChange = { c.updateFootnote(id, it) },
                    textStyle = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f),
                )
                IconAction(Icons.Outlined.DeleteOutline, "Tirar a nota do texto", onClick = { c.removeFootnote(id) })
            }
        }
    }
}

/** Blocos de anotações livres do livro (valem para todos os capítulos). */
@Composable
private fun NoteBlocksBlock(c: EscribaController) {
    val blocks = c.book?.notes?.blocks.orEmpty()
    var confirmDelete by remember { mutableStateOf<NoteBlock?>(null) }
    InspectorBlock("Blocos de notas") {
        blocks.forEach { block ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = block.title, onValueChange = { c.updateNoteBlock(block.copy(title = it)) }, singleLine = true,
                        textStyle = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner -> if (block.title.isEmpty()) Text("Nome do bloco", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline); inner() },
                    )
                    IconAction(Icons.Outlined.DeleteOutline, "Apagar bloco", onClick = {
                        if (block.title.isBlank() && block.body.isBlank()) c.removeNoteBlock(block.id) else confirmDelete = block
                    })
                }
                BasicTextField(
                    value = block.body, onValueChange = { c.updateNoteBlock(block.copy(body = it)) },
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    decorationBox = { inner -> if (block.body.isEmpty()) Text("Escreva aqui…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline); inner() },
                )
            }
        }
        TextButton(onClick = { c.addNoteBlock() }) { Text("+ Novo bloco") }
    }
    confirmDelete?.let { block ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Apagar o bloco “${block.title.ifBlank { "sem nome" }}”?") },
            text = { Text("O texto do bloco será apagado.") },
            confirmButton = { Button(onClick = { c.removeNoteBlock(block.id); confirmDelete = null }) { Text("Apagar") } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun InspectorBlock(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
