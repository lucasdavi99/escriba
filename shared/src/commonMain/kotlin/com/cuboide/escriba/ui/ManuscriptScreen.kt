package com.cuboide.escriba.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.draw.alpha
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

/** Escala única de status (a mesma das cenas do Enredo): ideia, rascunho, revisão, pronto. */
@Composable
fun statusColor(status: ChapterStatus): Color = statusTone(status.ordinal)

/** Os quatro degraus da escala de status: 0 discreto, 1 rascunho, 2 revisão, 3 pronto. */
@Composable
fun statusTone(step: Int): Color {
    val p = LocalPalette.current
    return when (step) {
        0 -> p.muted.copy(alpha = 0.7f)
        1 -> p.warning
        2 -> p.info
        else -> p.success
    }
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
                VHairline()
            }
            Column(Modifier.weight(1f).fillMaxHeight().background(LocalPalette.current.desk)) {
                if (!c.focusMode) WritingToolbar(c, chaptersVisible, inspectorVisible)
                if (c.findVisible && !c.focusMode) FindBar(c)
                Desk(c, Modifier.weight(1f))
                if (!c.focusMode) StatusBar(c)
            }
            if (inspectorVisible) {
                VHairline()
                Inspector(c, Modifier.width(INSPECTOR_WIDTH).fillMaxHeight())
            }
        }
    }
}

// ---------------------------------------------------------------- Capítulos

@Composable
private fun ChaptersPanel(c: EscribaController, modifier: Modifier) {
    val book = c.book ?: return
    val palette = LocalPalette.current
    val chapters = book.manifest.chapters
    val numbers = c.chapterNumbers
    var partsMenu by remember { mutableStateOf(false) }
    Column(modifier.background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 10.dp, top = 22.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Capítulos", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal))
                val parts = chapters.size - numbers.size
                Text(
                    "${numbers.size} ${if (numbers.size == 1) "capítulo" else "capítulos"}" + if (parts > 0) " · $parts ${if (parts == 1) "parte" else "partes"}" else "",
                    style = MaterialTheme.typography.labelMedium, color = palette.muted,
                )
            }
            Box {
                IconAction(Icons.Outlined.AutoStories, "Nova parte do livro (prólogo, dedicatória, epílogo…)", onClick = { partsMenu = true }, size = 32.dp)
                DropdownMenu(expanded = partsMenu, onDismissRequest = { partsMenu = false }) {
                    PartKindItems { kind -> partsMenu = false; c.addPart(kind) }
                }
            }
            IconAction(Icons.Outlined.Add, "Novo capítulo", onClick = { c.addChapter() }, size = 32.dp)
        }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            itemsIndexed(chapters, key = { _, ch -> ch.id }) { index, chapter ->
                ChapterItem(c, chapter, numbers[chapter.id], index, chapters.size)
            }
        }
        Hairline()
        val goal = book.manifest.bookGoal
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Overline("Livro", Modifier.weight(1f))
                Text(if (goal > 0) "${c.bookWords} de $goal" else "${c.bookWords} palavras", style = MaterialTheme.typography.labelMedium, color = palette.muted)
            }
            Spacer(Modifier.height(8.dp))
            val f = if (goal > 0) (c.bookWords.toFloat() / goal).coerceIn(0f, 1f) else 0f
            Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(palette.hairline)) {
                Box(Modifier.fillMaxWidth(f).fillMaxHeight().background(palette.accent))
            }
        }
    }
}

/** Itens de menu com as partes do livro agrupadas (antes, entre e depois da história). */
@Composable
private fun PartKindItems(onPick: (ChapterKind) -> Unit) {
    BookPartGroup.entries.forEach { group ->
        Overline(group.label, Modifier.padding(start = 12.dp, top = 10.dp, bottom = 2.dp))
        ChapterKind.entries.filter { it.group == group }.forEach { kind ->
            DropdownMenuItem(text = { Text(kind.label) }, onClick = { onPick(kind) })
        }
    }
    HorizontalDivider(color = LocalPalette.current.hairline)
    Text(
        "Não contam como capítulo nem entram na numeração.", style = MaterialTheme.typography.labelSmall,
        color = LocalPalette.current.muted, modifier = Modifier.padding(12.dp).widthIn(max = 220.dp),
    )
}

@Composable
private fun ChapterItem(c: EscribaController, chapter: ChapterMeta, number: Int?, index: Int, count: Int) {
    val palette = LocalPalette.current
    val selected = chapter.id == c.currentChapterId
    var menu by remember { mutableStateOf(false) }
    var kindMenu by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    val words = if (selected) c.currentWords else chapter.words
    val color = statusColor(chapter.status)
    HoverRow(selected, onClick = { c.selectChapter(chapter.id) }, Modifier.fillMaxWidth().padding(vertical = 1.dp)) { hovered ->
        Box(Modifier.width(3.dp).height(30.dp).clip(RoundedCornerShape(2.dp)).background(if (selected) palette.accent else Color.Transparent))
        Text(
            number?.toString() ?: "§",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
            color = if (selected) palette.accent else palette.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(36.dp),
        )
        Column(Modifier.weight(1f).padding(vertical = 9.dp)) {
            Text(
                chapter.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(color, 6.dp)
                Spacer(Modifier.width(6.dp))
                val kind = if (chapter.kind == ChapterKind.Capitulo) "" else "${chapter.kind.label} · "
                Text("$kind${chapter.status.label} · $words palavras", style = MaterialTheme.typography.labelSmall, color = palette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(Modifier.alpha(if (selected || hovered || menu || kindMenu) 1f else 0f)) {
            IconAction(Icons.Outlined.MoreHoriz, "Opções do capítulo", onClick = { menu = true }, size = 30.dp)
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
    // Barra flutuante sobre a mesa: os painéis nas pontas e, no meio, as ações de escrita em grupos.
    // Formatação e inserções ficam em menus ("Estilo", "Inserir"); B e I, as mais usadas, ficam à vista.
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        ToolAction(Icons.AutoMirrored.Outlined.ViewSidebar, "Mostrar/ocultar capítulos", onClick = { c.togglePanel(chapters = true, visible = chaptersVisible) }, selected = chaptersVisible)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            FloatingPanel {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ToolAction(Icons.AutoMirrored.Outlined.Undo, "Desfazer (Ctrl+Z)", onClick = { c.undo(); c.requestEditorFocus() }, enabled = c.canUndo)
                    ToolAction(Icons.AutoMirrored.Outlined.Redo, "Refazer (Ctrl+Y)", onClick = { c.redo(); c.requestEditorFocus() }, enabled = c.canRedo)
                    ToolbarDivider()
                    ToolAction(Icons.Outlined.FormatBold, "Negrito (Ctrl+B)", onClick = { c.toggleMark(MarkKind.Bold) })
                    ToolAction(Icons.Outlined.FormatItalic, "Itálico (Ctrl+I)", onClick = { c.toggleMark(MarkKind.Italic) })
                    StyleMenu(c)
                    InsertMenu(c, onFootnote = { footnoteDialog = true })
                    ToolbarDivider()
                    MeasuresMenu(c)
                    ToolAction(Icons.Outlined.FindReplace, "Localizar e substituir (Ctrl+F)", onClick = { if (c.findVisible) c.closeFind() else c.openFind() }, selected = c.findVisible)
                    ToolAction(Icons.Outlined.SaveAs, "Salvar versão do capítulo", onClick = { c.snapshotCurrent(); c.message = "Versão salva." })
                    ToolAction(Icons.Outlined.CenterFocusStrong, "Modo foco (F11)", onClick = { c.focusMode = true })
                }
            }
        }
        ToolAction(Icons.AutoMirrored.Outlined.ViewSidebar, "Mostrar/ocultar inspetor", onClick = { c.togglePanel(chapters = false, visible = inspectorVisible) }, selected = inspectorVisible)
    }
    if (footnoteDialog) FootnoteDialog(onDismiss = { footnoteDialog = false; c.requestEditorFocus() }) { text ->
        footnoteDialog = false
        c.addFootnote(text)
    }
}

/** Item de menu de formatação: ícone (ou glifo), nome e atalho; devolve o foco ao texto. */
@Composable
private fun FormatItem(label: String, icon: ImageVector? = null, glyph: String? = null, hint: String? = null, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = {
            Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                if (icon != null) Icon(icon, null, Modifier.size(18.dp))
                else Text(glyph.orEmpty(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        },
        trailingIcon = hint?.let { { Text(it, style = MaterialTheme.typography.labelSmall, color = LocalPalette.current.muted) } },
        onClick = onClick,
        modifier = Modifier.focusProperties { canFocus = false },
    )
}

/** Barra do menu: rótulo com seta, sem tirar o foco do texto. */
@Composable
private fun MenuTrigger(label: String, tooltip: String, open: Boolean, onClick: () -> Unit) {
    WithTooltip(tooltip) {
        Row(
            Modifier.height(36.dp).clip(Radius.control)
                .background(if (open) LocalPalette.current.accentSoft else Color.Transparent)
                .focusProperties { canFocus = false }.clickable(onClick = onClick).padding(start = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(Icons.Outlined.KeyboardArrowDown, null, Modifier.size(18.dp), tint = LocalPalette.current.muted)
        }
    }
}

@Composable
private fun StyleMenu(c: EscribaController) {
    var open by remember { mutableStateOf(false) }
    fun run(action: () -> Unit) { open = false; action(); c.requestEditorFocus() }
    Box {
        MenuTrigger("Estilo", "Estilos de texto e de parágrafo", open) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false; c.requestEditorFocus() }) {
            Overline("Texto", Modifier.padding(start = 14.dp, top = 6.dp, bottom = 2.dp))
            FormatItem("Versalete", glyph = "Aa") { run { c.toggleMark(MarkKind.SmallCaps) } }
            FormatItem("Sobrescrito", Icons.Outlined.Superscript) { run { c.toggleMark(MarkKind.Superscript) } }
            FormatItem("Subscrito", Icons.Outlined.Subscript) { run { c.toggleMark(MarkKind.Subscript) } }
            FormatItem("Aspas", glyph = "“ ”") { run { c.wrapQuotes() } }
            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = LocalPalette.current.hairline)
            Overline("Parágrafo", Modifier.padding(start = 14.dp, top = 6.dp, bottom = 2.dp))
            FormatItem("Título", glyph = "H1") { run { c.toggleBlockStyle(BlockKind.Heading1) } }
            FormatItem("Subtítulo", glyph = "H2") { run { c.toggleBlockStyle(BlockKind.Heading2) } }
            FormatItem("Chamada (texto pequeno acima do título)", glyph = "H3") { run { c.toggleBlockStyle(BlockKind.Heading3) } }
            FormatItem("Lista", Icons.AutoMirrored.Outlined.FormatListBulleted) { run { c.toggleBlockStyle(BlockKind.ListItem) } }
            FormatItem("Citação em bloco", Icons.Outlined.FormatQuote) { run { c.toggleBlockStyle(BlockKind.Quote) } }
            FormatItem("Centralizar parágrafo", Icons.Outlined.FormatAlignCenter) { run { c.toggleCentered() } }
            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = LocalPalette.current.hairline)
            FormatItem("Limpar formatação", Icons.Outlined.FormatClear) { run { c.clearFormat() } }
        }
    }
}

@Composable
private fun InsertMenu(c: EscribaController, onFootnote: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    fun run(action: () -> Unit) { open = false; action(); c.requestEditorFocus() }
    Box {
        MenuTrigger("Inserir", "Travessão, quebra de cena, imagem e nota", open) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false; c.requestEditorFocus() }) {
            FormatItem("Travessão de diálogo", glyph = "—", hint = "--") { run { c.insertText("— ") } }
            FormatItem("Quebra de cena", glyph = "⁂") { run { c.insertSceneBreak() } }
            FormatItem("Imagem", Icons.Outlined.Image) { run { c.insertImage() } }
            FormatItem("Nota de rodapé", glyph = "¹") { open = false; onFootnote() }
        }
    }
}

/** Medidas do texto: fonte, entrelinha, largura da página, recuo, alinhamento e correções automáticas. */
@Composable
private fun MeasuresMenu(c: EscribaController) {
    val s = c.settings
    var open by remember { mutableStateOf(false) }
    Box {
        ToolAction(Icons.Outlined.TextFields, "Medidas e parágrafo (Aa)", onClick = { open = true }, selected = open)
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
            Overline("Alinhamento", Modifier.padding(start = 14.dp, top = 10.dp, bottom = 6.dp))
            SegmentedControl(
                EditorAlign.entries.map { it.label }, EditorAlign.entries.indexOf(s.textAlign),
                { i -> c.updateSettings { it.copy(textAlign = EditorAlign.entries[i]) } },
                Modifier.padding(horizontal = 12.dp), dense = true,
            )
            Overline("Espaço entre parágrafos", Modifier.padding(start = 14.dp, top = 12.dp, bottom = 6.dp))
            SegmentedControl(
                ParagraphSpacing.entries.map { it.label }, ParagraphSpacing.entries.indexOfFirst { it.em == s.paragraphSpacing },
                { i -> c.updateSettings { it.copy(paragraphSpacing = ParagraphSpacing.entries[i].em) } },
                Modifier.padding(horizontal = 12.dp), dense = true,
            )
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
    Row(Modifier.width(300.dp).padding(horizontal = 14.dp, vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Stepper(label, value, onMinus, onPlus)
    }
}

/** − valor +, numa trilha suave (o mesmo controle do menu Aa e das Preferências). */
@Composable
fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        Modifier.clip(Radius.control).background(MaterialTheme.colorScheme.surfaceContainer),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconAction(Icons.Outlined.Remove, "Diminuir $label", onClick = onMinus, size = 30.dp)
        Text(value, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
        IconAction(Icons.Outlined.Add, "Aumentar $label", onClick = onPlus, size = 30.dp)
    }
}

/** Botão com um caractere tipográfico no lugar do ícone. */
@Composable
private fun GlyphAction(glyph: String, tooltip: String, size: Int = 40, onClick: () -> Unit) {
    WithTooltip(tooltip) {
        Box(
            Modifier.size(size.dp).clip(Radius.control).focusProperties { canFocus = false }.clickable(onClick = onClick),
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
    Box(Modifier.padding(horizontal = 6.dp).width(1.dp).height(20.dp).background(LocalPalette.current.hairline))
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
        Modifier.fillMaxHeight().then(deep.modifier()).verticalScroll(scroll).padding(top = 6.dp).widthIn(max = s.editorWidth.dp)
            .shadow(18.dp, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp), ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.14f))
            .background(palette.paper, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
            // Clique na folha fora do texto (margens, abaixo do fim) leva o cursor para o fim do capítulo.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { c.focusEditorAtEnd() }
            .padding(horizontal = 72.dp)
            .padding(top = 64.dp, bottom = 160.dp),
    ) {
        val number = c.chapterNumbers[chapter.id]
        Text(
            if (number != null) "CAPÍTULO $number" else chapter.kind.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 3.sp,
            color = palette.accent,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(12.dp))
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
        textStyle = MaterialTheme.typography.displaySmall.copy(color = palette.ink, textAlign = TextAlign.Center),
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
        issues = c.currentIssues,
        scriptFamily = rememberScriptFamily(c),
        issueColor = MaterialTheme.colorScheme.error,
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
    FloatingPanel {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            SmallAction(Icons.Outlined.FormatBold, "Negrito (Ctrl+B)") { c.toggleMark(MarkKind.Bold) }
            SmallAction(Icons.Outlined.FormatItalic, "Itálico (Ctrl+I)") { c.toggleMark(MarkKind.Italic) }
            GlyphAction("Aa", "Versalete", size = 32) { c.toggleMark(MarkKind.SmallCaps) }
            SmallAction(Icons.Outlined.Superscript, "Sobrescrito") { c.toggleMark(MarkKind.Superscript) }
            SmallAction(Icons.Outlined.Subscript, "Subscrito") { c.toggleMark(MarkKind.Subscript) }
            GlyphAction("“ ”", "Aspas", size = 32) { c.wrapQuotes() }
            SmallAction(Icons.Outlined.FormatAlignCenter, "Centralizar parágrafo") { c.toggleCentered() }
            GlyphAction("¹", "Nota de rodapé", size = 32) { onFootnote() }
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
        Box(Modifier.size(32.dp).clip(Radius.control).focusProperties { canFocus = false }.clickable(onClick = onClick), contentAlignment = Alignment.Center) {
            Icon(icon, tooltip, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusBar(c: EscribaController) {
    val book = c.book ?: return
    val palette = LocalPalette.current
    val words = c.currentWords
    val goal = book.manifest.dailyGoal
    val today = c.productivity.today.words
    val text = c.editor.text
    val pages = maxOf(1, (words + WORDS_PER_PAGE - 1) / WORDS_PER_PAGE)
    val cursorPage = minOf(pages, 1 + countManuscriptWords(text.substring(0, c.editor.selection.end.coerceIn(0, text.length))) / WORDS_PER_PAGE)
    val notes = c.currentFootnotes.size
    val style = MaterialTheme.typography.labelMedium
    val muted = palette.muted
    var details by remember { mutableStateOf(false) }

    // Cada número fica numa linha só; em janela estreita os números rolam de lado em vez de quebrar letra por letra.
    @Composable
    fun Stat(value: String, color: Color = Color.Unspecified) = Text(value, style = style, color = color, maxLines = 1, softWrap = false)

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // As palavras abrem os detalhes (página, leitura, notas e o livro inteiro).
            Box {
                Row(Modifier.clip(Radius.control).clickable { details = true }, verticalAlignment = Alignment.CenterVertically) {
                    Stat("$words palavras", MaterialTheme.colorScheme.onSurface)
                }
                DropdownMenu(expanded = details, onDismissRequest = { details = false }) {
                    Column(Modifier.width(240.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Overline("Este capítulo")
                        InfoRow("Página", "$cursorPage de $pages")
                        InfoRow("Leitura", "${readingMinutes(words)} min")
                        if (notes > 0) InfoRow("Notas de rodapé", "$notes")
                        Spacer(Modifier.height(4.dp))
                        Overline("O livro")
                        InfoRow("Palavras", "${c.bookWords}")
                        if (book.manifest.bookGoal > 0) InfoRow("Meta do livro", "${book.manifest.bookGoal}")
                    }
                }
            }
            Stat("${countParagraphs(stripMarkup(text))} parágrafos", muted)
            if (goal > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Stat("Hoje $today/$goal", muted)
                    Spacer(Modifier.width(8.dp))
                    val f = (today.toFloat() / goal).coerceIn(0f, 1f)
                    Box(Modifier.width(56.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(palette.hairline)) {
                        Box(Modifier.fillMaxWidth(f).fillMaxHeight().background(if (f >= 1f) palette.success else palette.accent))
                    }
                }
            }
        }
        // O estado de salvamento fica sempre visível à direita.
        Spacer(Modifier.width(16.dp))
        val (label, color) = when (c.saveState) {
            SaveState.Salvo -> "Salvo" to palette.success
            SaveState.Pendente -> "Editando…" to palette.warning
            SaveState.Salvando -> "Salvando…" to palette.warning
        }
        StatusDot(color, 6.dp)
        Spacer(Modifier.width(6.dp))
        Stat(label, muted)
    }
}

// ---------------------------------------------------------------- Localizar

/** Campo de texto compacto (36dp), com fio e foco no acento. */
@Composable
fun CompactField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
) {
    val palette = LocalPalette.current
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(palette.accent),
        modifier = modifier.onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Row(
                Modifier.height(36.dp).clip(Radius.control).background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(1.dp, if (focused) palette.accent else palette.hairline, Radius.control)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leading != null) {
                    Icon(leading, null, Modifier.size(16.dp), tint = palette.muted)
                    Spacer(Modifier.width(8.dp))
                }
                Box(Modifier.weight(1f)) {
                    if (value.text.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = palette.muted)
                    inner()
                }
            }
        },
    )
}

@Composable
private fun OptionToggle(label: String, tooltip: String, on: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    WithTooltip(tooltip) {
        Box(
            Modifier.size(32.dp).clip(Radius.control).background(if (on) palette.accentSoft else Color.Transparent).clickable(onClick = onClick)
                .semantics { contentDescription = tooltip },
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = if (on) palette.accent else palette.muted)
        }
    }
}

@Composable
private fun FindBar(c: EscribaController) {
    // Ctrl+F leva o cursor para "Localizar" com a busca anterior selecionada, para não digitar no texto do livro.
    val queryFocus = remember { FocusRequester() }
    var query by remember { mutableStateOf(TextFieldValue(c.findQuery)) }
    var replace by remember { mutableStateOf(TextFieldValue(c.replaceText)) }
    var replaceMenu by remember { mutableStateOf(false) }
    LaunchedEffect(c.findFocusRequests) {
        query = TextFieldValue(c.findQuery, TextRange(0, c.findQuery.length))
        queryFocus.requestFocus()
    }
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp), contentAlignment = Alignment.Center) {
        FloatingPanel(Modifier.widthIn(max = 920.dp)) {
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 6.dp).horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CompactField(
                    query,
                    {
                        query = it
                        if (it.text != c.findQuery) { c.findQuery = it.text; c.refreshMatches() }
                    },
                    "Localizar",
                    Modifier.width(230.dp).focusRequester(queryFocus).onPreviewKeyEvent { e ->
                        if (e.type == KeyEventType.KeyDown && e.key == Key.Enter) { c.goToMatch(1); true }
                        else if (e.type == KeyEventType.KeyDown && e.key == Key.Escape) { c.closeFind(); c.requestEditorFocus(); true }
                        else false
                    },
                    leading = Icons.Outlined.Search,
                )
                OptionToggle("Aa", "Diferenciar maiúsculas", c.findOptions.matchCase) { c.findOptions = c.findOptions.copy(matchCase = !c.findOptions.matchCase); c.refreshMatches() }
                OptionToggle("ab", "Palavra inteira", c.findOptions.wholeWord) { c.findOptions = c.findOptions.copy(wholeWord = !c.findOptions.wholeWord); c.refreshMatches() }
                OptionToggle(".*", "Expressão regular", c.findOptions.regex) { c.findOptions = c.findOptions.copy(regex = !c.findOptions.regex); c.refreshMatches() }
                Text(
                    if (c.matches.isEmpty()) "0 de 0" else "${c.currentMatch + 1} de ${c.matches.size}",
                    style = MaterialTheme.typography.labelMedium, color = LocalPalette.current.muted,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
                IconAction(Icons.Outlined.KeyboardArrowUp, "Anterior", onClick = { c.goToMatch(-1) }, size = 32.dp)
                IconAction(Icons.Outlined.KeyboardArrowDown, "Próxima (Enter)", onClick = { c.goToMatch(1) }, size = 32.dp)
                Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(20.dp).background(LocalPalette.current.hairline))
                CompactField(replace, { replace = it; c.replaceText = it.text }, "Substituir por", Modifier.width(200.dp))
                TextButton(onClick = { c.replaceCurrent() }, enabled = c.currentMatch >= 0) { Text("Substituir") }
                Box {
                    IconAction(Icons.Outlined.MoreHoriz, "Mais formas de substituir", onClick = { replaceMenu = true }, size = 32.dp)
                    DropdownMenu(expanded = replaceMenu, onDismissRequest = { replaceMenu = false }) {
                        DropdownMenuItem(text = { Text("Substituir tudo no capítulo") }, enabled = c.matches.isNotEmpty(), onClick = { replaceMenu = false; c.replaceAllInChapter() })
                        DropdownMenuItem(text = { Text("Substituir tudo no livro") }, enabled = c.findQuery.isNotEmpty(), onClick = { replaceMenu = false; c.replaceAllInBook() })
                        HorizontalDivider(color = LocalPalette.current.hairline)
                        DropdownMenuItem(text = { Text("Apagar todas as ocorrências") }, enabled = c.matches.isNotEmpty(), onClick = { replaceMenu = false; c.deleteAllInChapter() })
                    }
                }
                IconAction(Icons.Outlined.Close, "Fechar (Esc)", onClick = { c.closeFind(); c.requestEditorFocus() }, size = 32.dp)
            }
        }
    }
}

/** Fio com losango central, desenhado (não depende de glifos da fonte). */
@Composable
private fun Ornament(color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(40.dp).height(1.dp).background(color.copy(alpha = 0.35f)))
        Box(Modifier.padding(horizontal = 10.dp).size(5.dp).rotate(45f).background(color.copy(alpha = 0.75f)))
        Box(Modifier.width(40.dp).height(1.dp).background(color.copy(alpha = 0.35f)))
    }
}

@Composable
fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(Radius.control).clickable { onChange(!checked) }.padding(end = 12.dp)) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

// ---------------------------------------------------------------- Inspetor

@Composable
private fun Inspector(c: EscribaController, modifier: Modifier) {
    val book = c.book ?: return
    val chapter = c.currentChapter ?: return
    val palette = LocalPalette.current
    val text = c.editor.text
    Column(
        modifier.background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Overline("Status")
            SegmentedControl(
                ChapterStatus.entries.map { it.label },
                chapter.status.ordinal,
                { c.setChapterStatus(chapter.id, ChapterStatus.entries[it]) },
                dots = ChapterStatus.entries.map { if (it == chapter.status) statusColor(it) else null },
                dense = true,
            )
        }
        Hairline()
        CollapsibleSection("Estatísticas") {
            val words = c.currentWords
            val plain = remember(text) { stripMarkup(text) }
            Row(Modifier.fillMaxWidth()) {
                MiniStat("$words", "palavras", Modifier.weight(1f))
                MiniStat("${countCharacters(plain)}", "caracteres", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth()) {
                MiniStat("${countParagraphs(plain)}", "parágrafos", Modifier.weight(1f))
                MiniStat("${readingMinutes(words)} min", "de leitura", Modifier.weight(1f))
            }
        }
        // Nome e apelidos, palavra inteira, sem diferenciar acentos; as mais citadas primeiro.
        val mentioned = remember(text, book.universe) { c.entriesInCurrentChapter() }
        if (mentioned.isNotEmpty()) {
            Hairline()
            CollapsibleSection("Neste capítulo") {
                mentioned.forEach { (e, count) ->
                    HoverRow(false, onClick = { c.openUniverseEntry(e.id) }, Modifier.fillMaxWidth()) {
                        Spacer(Modifier.width(4.dp))
                        EntryAvatar(c, e, 26)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f).padding(vertical = 5.dp)) {
                            Text(e.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(e.kind.label, style = MaterialTheme.typography.labelSmall, color = palette.muted)
                        }
                        Text("${count}×", style = MaterialTheme.typography.labelMedium, color = palette.muted, modifier = Modifier.padding(end = 6.dp))
                    }
                }
            }
        }
        Hairline()
        ContradictionsBlock(c)
        val beats = book.outline.beats.filter { it.chapterId == chapter.id }
        if (beats.isNotEmpty()) {
            Hairline()
            CollapsibleSection("Cenas do roteiro") {
                beats.forEach { b ->
                    Row(Modifier.fillMaxWidth().clip(Radius.control).clickable { c.goTo(Section.Enredo) }.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(statusTone(b.status.ordinal), 6.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(b.title.ifBlank { "Cena sem título" }, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        FootnotesBlock(c)
        Hairline()
        NoteBlocksBlock(c)
        Hairline()
        CollapsibleSection("Versões recentes", initiallyOpen = c.snapshots.isNotEmpty()) {
            if (c.snapshots.isEmpty()) {
                Text("Nenhuma versão salva ainda.", style = MaterialTheme.typography.bodySmall, color = palette.muted)
            }
            c.snapshots.take(3).forEach { s ->
                Column(Modifier.padding(vertical = 2.dp)) {
                    Text(s.label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text("${relativeTime(s.createdAt)} · ${s.words} palavras", style = MaterialTheme.typography.labelSmall, color = palette.muted)
                }
            }
            TextButton(onClick = { c.goTo(Section.Versoes) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp)) { Text("Ver todas e comparar") }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 2.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = LocalPalette.current.muted)
    }
}

/** Notas de rodapé do capítulo, na ordem do texto, com o texto editável. Só aparece quando há notas. */
@Composable
private fun FootnotesBlock(c: EscribaController) {
    val notes = c.currentFootnotes
    if (notes.isEmpty()) return
    Hairline()
    CollapsibleSection("Notas de rodapé") {
        notes.forEachIndexed { i, (id, noteText) ->
            Row(verticalAlignment = Alignment.Top) {
                Text("${i + 1}", style = MaterialTheme.typography.titleLarge, color = LocalPalette.current.accent, modifier = Modifier.padding(top = 4.dp, end = 10.dp))
                BasicTextField(
                    value = noteText, onValueChange = { c.updateFootnote(id, it) },
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(LocalPalette.current.accent),
                    modifier = Modifier.weight(1f).padding(top = 8.dp),
                )
                IconAction(Icons.Outlined.DeleteOutline, "Tirar a nota do texto", onClick = { c.removeFootnote(id) }, size = 30.dp)
            }
        }
    }
}

/** Blocos de anotações livres do livro (valem para todos os capítulos). */
@Composable
private fun NoteBlocksBlock(c: EscribaController) {
    val blocks = c.book?.notes?.blocks.orEmpty()
    val palette = LocalPalette.current
    var confirmDelete by remember { mutableStateOf<NoteBlock?>(null) }
    CollapsibleSection("Blocos de notas", initiallyOpen = blocks.isNotEmpty()) {
        blocks.forEach { block ->
            Column(
                Modifier.fillMaxWidth().clip(Radius.surface).background(palette.paper)
                    .border(1.dp, palette.hairline, Radius.surface).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = block.title, onValueChange = { c.updateNoteBlock(block.copy(title = it)) }, singleLine = true,
                        textStyle = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(palette.accent),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner -> if (block.title.isEmpty()) Text("Nome do bloco", style = MaterialTheme.typography.labelLarge, color = palette.muted); inner() },
                    )
                    IconAction(Icons.Outlined.DeleteOutline, "Apagar bloco", onClick = {
                        if (block.title.isBlank() && block.body.isBlank()) c.removeNoteBlock(block.id) else confirmDelete = block
                    }, size = 28.dp)
                }
                BasicTextField(
                    value = block.body, onValueChange = { c.updateNoteBlock(block.copy(body = it)) },
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(palette.accent),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
                    decorationBox = { inner -> if (block.body.isEmpty()) Text("Escreva aqui…", style = MaterialTheme.typography.bodySmall, color = palette.muted); inner() },
                )
            }
        }
        TextButton(onClick = { c.addNoteBlock() }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp)) {
            Icon(Icons.Outlined.Add, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Novo bloco")
        }
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
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = LocalPalette.current.muted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
