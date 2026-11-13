package com.cuboide.escriba.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.UniverseTool
import com.cuboide.escriba.core.board.ImageFrame
import com.cuboide.escriba.core.board.panned
import com.cuboide.escriba.core.board.zoomed
import com.cuboide.escriba.core.model.EntryAttribute
import com.cuboide.escriba.core.model.EntryBlock
import com.cuboide.escriba.core.model.EntryKind
import com.cuboide.escriba.core.model.UniverseEntry
import com.cuboide.escriba.core.text.BlockKind
import com.cuboide.escriba.core.text.Edit
import com.cuboide.escriba.core.text.MarkKind
import com.cuboide.escriba.core.text.stripMarkup
import com.cuboide.escriba.core.text.toggleBlock
import com.cuboide.escriba.core.text.toggleInline
import com.cuboide.escriba.core.universe.AttrSpec
import com.cuboide.escriba.core.universe.BlockSpec
import com.cuboide.escriba.core.universe.EntrySchemas
import com.cuboide.escriba.core.universe.blockText
import com.cuboide.escriba.core.universe.digest
import com.cuboide.escriba.core.universe.matchesFilter
import com.cuboide.escriba.core.universe.schema
import com.cuboide.escriba.core.universe.withAttr
import com.cuboide.escriba.core.universe.withBlock
import kotlin.math.pow

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
    if (c.universeTool == UniverseTool.Acervo && c.acervo == null) {
        AcervoScreen(c)
        return
    }
    var kind by remember { mutableStateOf(EntryKind.Personagem) }
    var filter by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var showTrash by remember { mutableStateOf(false) }
    var showBring by remember { mutableStateOf(false) }
    var showUnify by remember { mutableStateOf(false) }
    val acervo = c.acervo
    val all = c.universeEntries
    // Pedido vindo do mapa de enredo ou do Manuscrito: mostra a ficha e a aba do tipo dela.
    LaunchedEffect(c.universeFocus) {
        val id = c.universeFocus ?: return@LaunchedEffect
        all.firstOrNull { it.id == id }?.let { e -> kind = e.kind; filter = ""; selectedId = e.id }
        c.consumeUniverseFocus()
    }
    val entries = all.filter { it.kind == kind && it.matchesFilter(filter) }.sortedBy { it.name.lowercase() }
    val selected = all.firstOrNull { it.id == selectedId && it.kind == kind } ?: entries.firstOrNull()
    val trash = c.entryTrash

    val palette = LocalPalette.current
    val inFichas = c.universeTool == UniverseTool.Fichas || acervo != null
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        PageHeader(
            title = acervo?.meta?.name ?: "Universo",
            subtitle = when {
                acervo?.meta?.saga == true -> "Universo da saga, no Acervo · ${all.size} ficha(s) · muda em todos os ${acervo.meta.bookPaths.size} livros"
                acervo != null -> "Universo do Acervo · ${all.size} ficha(s), fora de qualquer livro"
                book.manifest.sagaId != null -> "${all.size} ficha(s) · saga “${book.manifest.sagaName}”: mudar uma ficha muda nos outros livros"
                else -> "${all.size} ficha(s) do livro"
            },
            actions = {
                if (acervo != null) {
                    TextButton(onClick = { c.showAcervo() }) { Text("← Universos do Acervo") }
                } else {
                    TextButton(onClick = { c.showAcervo() }) {
                        Icon(Icons.Outlined.Inventory2, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Acervo")
                    }
                    // Num livro de saga, a saga fica à vista: é o estado do universo, não só uma ação.
                    if (book.manifest.sagaId != null) {
                        TextButton(onClick = { c.refreshLibrary(); showUnify = true }) {
                            Icon(Icons.Outlined.Link, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Saga")
                        }
                    }
                }
                MoreMenu("Mais ações do universo") { close ->
                    if (acervo == null && book.manifest.sagaId == null) {
                        DropdownMenuItem(
                            text = { Text("Unificar livros") }, leadingIcon = { Icon(Icons.Outlined.Link, null) },
                            onClick = { close(); c.refreshLibrary(); showUnify = true },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Trazer fichas") }, leadingIcon = { Icon(Icons.Outlined.Inventory2, null) },
                        onClick = { close(); c.refreshLibrary(); showBring = true },
                    )
                    DropdownMenuItem(
                        text = { Text("Lixeira (${trash.size})") }, leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) },
                        onClick = { close(); showTrash = true },
                    )
                }
                if (inFichas) {
                    Spacer(Modifier.width(4.dp))
                    Button(onClick = { filter = ""; c.universeTool = UniverseTool.Fichas; selectedId = c.createEntry(kind).id }) {
                        Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Nova ficha de ${kind.label.lowercase()}")
                    }
                }
            },
        )
        // Abas: os tipos de ficha e, depois de um fio, as ferramentas do mundo (calendário e idiomas são do livro).
        val tabs = EntryKind.entries.map { TabItem(it.plural, all.count { e -> e.kind == it }, it.icon) } +
            if (acervo == null) listOf(TabItem("Calendário", icon = Icons.Outlined.CalendarMonth), TabItem("Idiomas", icon = Icons.Outlined.Translate)) else emptyList()
        val selectedTab = when {
            inFichas -> kind.ordinal
            c.universeTool == UniverseTool.Calendario -> EntryKind.entries.size
            else -> EntryKind.entries.size + 1
        }
        QuietTabs(
            tabs, selectedTab,
            onSelect = { i ->
                when {
                    i < EntryKind.entries.size -> { kind = EntryKind.entries[i]; c.universeTool = UniverseTool.Fichas }
                    i == EntryKind.entries.size -> c.universeTool = UniverseTool.Calendario
                    else -> c.universeTool = UniverseTool.Idiomas
                }
            },
            modifier = Modifier.padding(horizontal = Space.page),
            groupBreaks = if (acervo == null) setOf(EntryKind.entries.size) else emptySet(),
        )
        Column(Modifier.fillMaxSize().padding(horizontal = Space.page).padding(top = 20.dp, bottom = 20.dp)) {
            if (c.universeTool == UniverseTool.Calendario && acervo == null) {
                CalendarScreen(c)
                return@Column
            }
            if (c.universeTool == UniverseTool.Idiomas && acervo == null) {
                LanguageScreen(c)
                return@Column
            }
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.width(280.dp).fillMaxHeight().padding(end = 20.dp)) {
                    OutlinedTextField(
                        value = filter, onValueChange = { filter = it }, singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.size(18.dp)) },
                        placeholder = { Text("Buscar nome, apelido, texto…", maxLines = 1) },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(entries, key = { it.id }) { e -> EntryRow(c, e, e.id == selected?.id) { selectedId = e.id } }
                    }
                    if (entries.isEmpty()) {
                        Text(
                            if (filter.isBlank()) "Nenhuma ficha de ${kind.plural.lowercase()} ainda." else "Nada encontrado para “$filter”.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.muted,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                }
                VHairline()
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    if (selected == null) {
                        EmptyState(kind.icon, "Nenhuma ficha aberta", "Escolha alguém ou algum lugar à esquerda, ou crie uma nova ficha.")
                    } else {
                        EntrySheet(c, selected, onDeleted = { selectedId = null })
                    }
                }
            }
        }
    }
    if (showTrash) EntryTrashDialog(c) { showTrash = false }
    if (showBring) BringEntriesDialog(c) { showBring = false }
    if (showUnify) UnifyBooksDialog(c) { showUnify = false }
}

@Composable
private fun EntryRow(c: EscribaController, entry: UniverseEntry, selected: Boolean, onClick: () -> Unit) {
    HoverRow(selected, onClick, Modifier.fillMaxWidth()) {
        Spacer(Modifier.width(10.dp))
        EntryAvatar(c, entry, 34)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(vertical = 9.dp).padding(end = 10.dp)) {
            Text(entry.name.ifBlank { "(sem nome)" }, style = MaterialTheme.typography.bodyMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val line = entry.digest().ifBlank { entry.tags.joinToString(" · ") { "#$it" } }
            if (line.isNotEmpty()) {
                Text(line, style = MaterialTheme.typography.bodySmall, color = LocalPalette.current.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Retrato em círculo (com o enquadramento escolhido) ou, sem retrato, a inicial sobre uma cor do nome. */
@Composable
fun EntryAvatar(c: EscribaController, entry: UniverseEntry, sizeDp: Int) {
    val image = rememberEntryImage(c, entry.portrait?.path)
    if (entry.portrait != null && image != null) {
        FramedImage(image, entry.portrait.frame, Modifier.size(sizeDp.dp).clip(CircleShape))
    } else {
        Avatar(entry.name, sizeDp)
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

// ---------- ficha ----------

@Composable
private fun EntrySheet(c: EscribaController, entry: UniverseEntry, onDeleted: () -> Unit) {
    val schema = entry.schema
    /** Versão mais recente da ficha: cada campo altera só a sua parte, sem perder o que outro acabou de mudar. */
    fun latest(): UniverseEntry = c.universeEntries.firstOrNull { it.id == entry.id } ?: entry
    fun edit(transform: (UniverseEntry) -> UniverseEntry) = c.updateEntry(transform(latest()))

    var openBlock by remember(entry.id) { mutableStateOf<String?>(null) } // key ou id do bloco aberto no editor
    val nameFocus = remember { FocusRequester() }
    LaunchedEffect(entry.id) { if (entry.name.isBlank()) nameFocus.requestFocus() }

    Box(Modifier.fillMaxSize()) {
        BoxWithConstraints {
            val narrow = maxWidth < 760.dp
            Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 32.dp, end = 8.dp, top = 4.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                SiblingStrip(c, entry)
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    // Coluna da esquerda: retrato, galeria, frase, atributos e citações.
                    Column(Modifier.width(if (narrow) 220.dp else 260.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        PortraitPane(c, entry)
                        Gallery(c, entry)
                        if (schema.block(EntrySchemas.QUOTE) != null) {
                            QuoteField(entry) { text -> edit { it.withBlock(EntrySchemas.QUOTE, text, c::newId) } }
                        }
                        AttributesCard(c, entry, ::edit)
                        SealCheckCard(c, entry)
                        if (c.acervo == null) MentionsCard(c, entry)
                    }
                    // Coluna da direita: nome, subtítulo, blocos e seções próprias.
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(entry.kind.icon, null, Modifier.size(13.dp), tint = LocalPalette.current.accent)
                                    Spacer(Modifier.width(6.dp))
                                    Overline(entry.kind.label, accent = true)
                                }
                                Spacer(Modifier.height(4.dp))
                                PlainField(
                                    entry.id + ":name", entry.name, schema.titleLabel,
                                    MaterialTheme.typography.displaySmall, Modifier.focusRequester(nameFocus),
                                ) { v -> edit { it.copy(name = v.trim()) } }
                                PlainField(
                                    entry.id + ":subtitle", entry.subtitle, schema.subtitleLabel,
                                    MaterialTheme.typography.titleLarge.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal),
                                ) { v -> edit { it.copy(subtitle = v) } }
                            }
                            IconAction(Icons.Outlined.DeleteOutline, "Enviar ficha para a lixeira", onClick = { c.trashEntry(latest()); onDeleted() })
                        }
                        BlockGrid(entry, narrow) { openBlock = it }
                        CustomSections(c, entry, ::edit) { openBlock = it }
                        TagsField(entry) { tags -> edit { it.copy(tags = tags) } }
                    }
                }
            }
        }
    }

    openBlock?.let { target ->
        val current = latest()
        val spec = schema.block(target)
        val custom = current.blocks.firstOrNull { it.key == null && it.id == target }
        val title = spec?.title ?: custom?.title ?: return@let
        val initial = if (spec != null) current.blockText(target) else custom?.text.orEmpty()
        RichBlockDialog(
            title = title, subtitle = "${current.name.ifBlank { "Sem nome" }} · ${current.kind.label}", initial = initial,
            onDismiss = { openBlock = null },
        ) { text ->
            if (spec != null) edit { it.withBlock(target, text, c::newId) }
            else edit { e -> e.copy(blocks = e.blocks.map { if (it.id == target) it.copy(text = text) else it }) }
        }
    }
}

/** Faixa com as outras fichas do mesmo tipo, para pular de uma para outra sem voltar à lista. */
@Composable
private fun SiblingStrip(c: EscribaController, entry: UniverseEntry) {
    val siblings = c.universeEntries.filter { it.kind == entry.kind }.sortedBy { it.name.lowercase() }
    if (siblings.size < 2) return
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        siblings.forEach { e ->
            val current = e.id == entry.id
            Column(
                Modifier.width(64.dp).clip(Radius.surface)
                    .background(if (current) LocalPalette.current.navSelected else Color.Transparent)
                    .clickable { c.openUniverseEntry(e.id) }.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                EntryAvatar(c, e, 36)
                Text(e.name.ifBlank { "sem nome" }, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Campo de texto sem moldura (nome, subtítulo). Guarda o cursor localmente e grava pela ficha. */
@Composable
private fun PlainField(
    key: String,
    value: String,
    placeholder: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    onChange: (String) -> Unit,
) {
    var field by remember(key) { mutableStateOf(TextFieldValue(value)) }
    val ink = MaterialTheme.colorScheme.onSurface
    BasicTextField(
        value = field,
        onValueChange = { field = it; if (it.text != value) onChange(it.text) },
        singleLine = singleLine,
        textStyle = style.copy(color = ink),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box {
                if (field.text.isEmpty()) Text(placeholder, style = style, color = MaterialTheme.colorScheme.outline)
                inner()
            }
        },
    )
}

// ---------- retrato e galeria ----------

@Composable
private fun PortraitPane(c: EscribaController, entry: UniverseEntry) {
    val scheme = MaterialTheme.colorScheme
    val portrait = entry.portrait
    val image = rememberEntryImage(c, portrait?.path)
    var framing by remember(entry.id) { mutableStateOf(false) }
    var box by remember { mutableStateOf(IntSize.Zero) }
    val frame by rememberUpdatedState(portrait?.frame ?: ImageFrame())
    fun setFrame(f: ImageFrame) = c.setPortraitFrame(entry.id, f)
    fun zoom(factor: Float) {
        val img = image ?: return
        if (box.width == 0) return
        setFrame(frame.zoomed(factor, box.width.toFloat(), box.height.toFloat(), img.width.toFloat(), img.height.toFloat()))
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.fillMaxWidth().height(300.dp).clip(Radius.surface)
                .background(scheme.surfaceContainerHighest)
                .border(if (framing) 2.dp else 1.dp, if (framing) scheme.primary else LocalPalette.current.hairline, Radius.surface)
                .onSizeChanged { box = it }
                .semantics { contentDescription = if (portrait == null) "Sem retrato" else "Retrato" }
                .then(
                    if (framing && image != null) {
                        Modifier
                            .pointerInput(entry.id, image) {
                                detectDragGestures { change, drag ->
                                    change.consume()
                                    setFrame(frame.panned(drag.x, drag.y, size.width.toFloat(), size.height.toFloat(), image.width.toFloat(), image.height.toFloat()))
                                }
                            }
                            .pointerInput(entry.id, image) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val ev = awaitPointerEvent()
                                        if (ev.type != PointerEventType.Scroll) continue
                                        val ch = ev.changes.firstOrNull() ?: continue
                                        val dy = ch.scrollDelta.y
                                        if (dy != 0f) { zoom(1.12f.pow(-dy.coerceIn(-5f, 5f))); ch.consume() }
                                    }
                                }
                            }
                    } else Modifier.clickable { if (portrait == null) c.addEntryImage(entry.id, asPortrait = true) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (portrait != null && image != null) {
                FramedImage(image, portrait.frame, Modifier.fillMaxSize())
            } else if (portrait == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Avatar(entry.name, 72)
                    Spacer(Modifier.height(10.dp))
                    Text("Adicionar retrato", style = MaterialTheme.typography.labelMedium, color = scheme.primary)
                }
            }
            if (framing) {
                Text(
                    "Arraste para mover · roda: zoom",
                    style = MaterialTheme.typography.labelSmall, color = Color.White,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        if (portrait != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (framing) {
                    IconAction(Icons.Outlined.Remove, "Afastar", onClick = { zoom(1f / 1.2f) })
                    IconAction(Icons.Outlined.Add, "Aproximar", onClick = { zoom(1.2f) })
                    SmallChip(if (frame.fit) "Preencher" else "Inteira") { setFrame(frame.copy(fit = !frame.fit, zoom = 1f, x = 0.5f, y = 0.5f)) }
                    SmallChip("Restaurar") { setFrame(ImageFrame()) }
                    Spacer(Modifier.weight(1f))
                    IconAction(Icons.Outlined.Check, "Concluir ajuste", onClick = { framing = false })
                } else {
                    IconAction(Icons.Outlined.Crop, "Ajustar enquadramento", onClick = { framing = true })
                    IconAction(Icons.Outlined.AddPhotoAlternate, "Trocar retrato", onClick = { c.addEntryImage(entry.id, asPortrait = true) })
                    IconAction(Icons.Outlined.DeleteOutline, "Tirar retrato", onClick = { c.removeEntryImage(entry.id, portrait.path) })
                }
            }
        }
    }
}

@Composable
private fun SmallChip(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 5.dp),
    )
}

@Composable
private fun Gallery(c: EscribaController, entry: UniverseEntry) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        entry.gallery.forEach { img ->
            val bitmap = rememberEntryImage(c, img.path)
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(scheme.surfaceContainerHighest)) {
                FramedImage(bitmap, ImageFrame(), Modifier.fillMaxSize())
                Row(Modifier.align(Alignment.TopEnd)) {
                    TinyAction(Icons.Outlined.Star, "Tornar retrato") { c.makePortrait(entry.id, img.path) }
                    TinyAction(Icons.Outlined.Close, "Remover imagem") { c.removeEntryImage(entry.id, img.path) }
                }
            }
        }
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, scheme.outlineVariant, RoundedCornerShape(10.dp))
                .clickable { c.addEntryImage(entry.id, asPortrait = false) }
                .semantics { contentDescription = "Adicionar imagem à galeria" },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Add, null, tint = scheme.outline) }
    }
}

@Composable
private fun TinyAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier.padding(2.dp).size(20.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, Modifier.size(12.dp), tint = Color.White) }
}

@Composable
private fun QuoteField(entry: UniverseEntry, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        Box(Modifier.width(3.dp).heightIn(min = 40.dp).background(LocalPalette.current.accent))
        Spacer(Modifier.width(10.dp))
        Column {
            Overline("Frase marcante")
            PlainField(
                entry.id + ":quote", entry.blockText(EntrySchemas.QUOTE), "Algo que só essa pessoa diria…",
                MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic), singleLine = false, onChange = onChange,
            )
        }
    }
}

// ---------- atributos ----------

@Composable
private fun AttributesCard(c: EscribaController, entry: UniverseEntry, edit: ((UniverseEntry) -> UniverseEntry) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(shape = Radius.surface, color = LocalPalette.current.paper, border = androidx.compose.foundation.BorderStroke(1.dp, LocalPalette.current.hairline)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Overline("Atributos")
            entry.schema.attributes.forEach { spec ->
                val attr = entry.attributes.firstOrNull { it.key == spec.key }
                AttributeRow(
                    entry.id + ":" + spec.key, spec.label, attr?.value.orEmpty(), spec,
                    sealed = attr?.sealed == true,
                    onToggleSeal = if (spec.sealable) ({ c.toggleSeal(entry.id, spec.key) }) else null,
                ) { v -> edit { it.withAttr(spec.key, v, c::newId) } }
            }
            entry.attributes.filter { it.key == null }.forEach { attr ->
                CustomAttributeRow(attr, onChange = { updated ->
                    edit { e -> e.copy(attributes = e.attributes.map { if (it.id == attr.id) updated else it }) }
                }, onRemove = {
                    edit { e -> e.copy(attributes = e.attributes.filterNot { it.id == attr.id }) }
                })
            }
            TextButton(onClick = { edit { it.copy(attributes = it.attributes + EntryAttribute(c.newId(), null, "", "")) } }) {
                Icon(Icons.Outlined.Add, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Atributo")
            }
        }
    }
}

@Composable
private fun AttributeRow(
    key: String,
    label: String,
    value: String,
    spec: AttrSpec?,
    sealed: Boolean = false,
    onToggleSeal: (() -> Unit)? = null,
    onChange: (String) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Overline(label)
            if (onToggleSeal != null) {
                // Selo: o Manuscrito confere o texto contra este fato.
                Icon(
                    if (sealed) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                    if (sealed) "Tirar o selo de $label" else "Selar $label",
                    Modifier.padding(start = 6.dp).size(14.dp).clip(CircleShape).clickable(onClick = onToggleSeal),
                    tint = if (sealed) LocalPalette.current.accent else MaterialTheme.colorScheme.outline,
                )
            }
        }
        PlainField(key, value, spec?.placeholder?.ifBlank { "—" } ?: "—", MaterialTheme.typography.bodyMedium, onChange = onChange)
        HorizontalDivider(color = LocalPalette.current.hairline)
    }
}

@Composable
private fun CustomAttributeRow(attr: EntryAttribute, onChange: (EntryAttribute) -> Unit, onRemove: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                PlainField(attr.id + ":label", attr.label, "RÓTULO", MaterialTheme.typography.labelSmall) { onChange(attr.copy(label = it)) }
            }
            TinyAction(Icons.Outlined.Close, "Remover atributo", onRemove)
        }
        PlainField(attr.id + ":value", attr.value, "—", MaterialTheme.typography.bodyMedium) { onChange(attr.copy(value = it)) }
        HorizontalDivider(color = LocalPalette.current.hairline)
    }
}

// ---------- blocos ----------

@Composable
private fun BlockGrid(entry: UniverseEntry, narrow: Boolean, onOpen: (String) -> Unit) {
    val specs = entry.schema.blocks.filter { it.key != EntrySchemas.QUOTE }
    // Blocos estreitos vão dois a dois; os largos ocupam a linha toda.
    val rows = mutableListOf<List<BlockSpec>>()
    var pending: BlockSpec? = null
    for (s in specs) {
        if (s.wide || narrow) {
            pending?.let { rows += listOf(it); pending = null }
            rows += listOf(s)
        } else if (pending == null) pending = s
        else { rows += listOf(pending!!, s); pending = null }
    }
    pending?.let { rows += listOf(it) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { spec ->
                    BlockCard(spec.title, entry.blockText(spec.key), spec.placeholder, Modifier.weight(1f)) { onOpen(spec.key) }
                }
            }
        }
    }
}

@Composable
private fun BlockCard(title: String, text: String, placeholder: String, modifier: Modifier, trailing: @Composable () -> Unit = {}, onOpen: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier.heightIn(min = 96.dp).clip(Radius.surface).clickable(onClick = onOpen)
            .semantics { contentDescription = "Bloco $title" },
        shape = Radius.surface, color = LocalPalette.current.paper,
        border = androidx.compose.foundation.BorderStroke(1.dp, LocalPalette.current.hairline),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Overline(title, Modifier.weight(1f), accent = true)
                trailing()
            }
            val plain = stripMarkup(text).trim()
            Text(
                plain.ifEmpty { placeholder },
                style = MaterialTheme.typography.bodyMedium,
                color = if (plain.isEmpty()) scheme.outline else scheme.onSurface,
                maxLines = 8, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CustomSections(c: EscribaController, entry: UniverseEntry, edit: ((UniverseEntry) -> UniverseEntry) -> Unit, onOpen: (String) -> Unit) {
    val custom = entry.blocks.filter { it.key == null }
    var renaming by remember(entry.id) { mutableStateOf<EntryBlock?>(null) }
    custom.forEach { block ->
        BlockCard(
            block.title.ifBlank { "Seção sem título" }, block.text, "Escreva…", Modifier.fillMaxWidth(),
            trailing = {
                TinyAction(Icons.Outlined.Title, "Renomear seção") { renaming = block }
                TinyAction(Icons.Outlined.Close, "Apagar seção") { edit { e -> e.copy(blocks = e.blocks.filterNot { it.id == block.id }) } }
            },
        ) { onOpen(block.id) }
    }
    TextButton(onClick = { renaming = EntryBlock(c.newId(), null, "") }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.Add, null, Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Nova seção")
    }
    renaming?.let { block ->
        TextInputDialog(if (block.title.isEmpty()) "Nova seção" else "Renomear seção", block.title, onDismiss = { renaming = null }) { title ->
            val isNew = custom.none { it.id == block.id }
            edit { e ->
                if (isNew) e.copy(blocks = e.blocks + block.copy(title = title.trim()))
                else e.copy(blocks = e.blocks.map { if (it.id == block.id) it.copy(title = title.trim()) else it })
            }
            renaming = null
            if (isNew) onOpen(block.id)
        }
    }
}

@Composable
private fun TagsField(entry: UniverseEntry, onChange: (List<String>) -> Unit) {
    var text by remember(entry.id) { mutableStateOf(entry.tags.joinToString(", ")) }
    OutlinedTextField(
        text,
        { text = it; onChange(it.split(',').map { t -> t.trim().removePrefix("#") }.filter { t -> t.isNotEmpty() }.distinct()) },
        label = { Text("Etiquetas (separadas por vírgula)") }, singleLine = true,
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
    )
}

/** Editor de um bloco da ficha, com a mesma marcação e a mesma aparência do Manuscrito. */
@Composable
private fun RichBlockDialog(title: String, subtitle: String, initial: String, onDismiss: () -> Unit, onChange: (String) -> Unit) {
    var field by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    val palette = LocalPalette.current
    val scheme = MaterialTheme.colorScheme
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    fun apply(edit: (String, Int, Int) -> Edit) {
        val e = edit(field.text, field.selection.min, field.selection.max)
        field = TextFieldValue(e.text, TextRange(e.selStart, e.selEnd))
        onChange(e.text)
    }
    val styling = ManuscriptStyling(
        selection = field.selection, matches = emptyList(), currentMatch = -1,
        ink = scheme.onSurface, accent = palette.accent, matchColor = palette.match, currentMatchColor = palette.currentMatch,
        align = TextAlign.Start, indentSp = 0f, lineHeightEm = 1.45f, focusMode = false,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(title)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        },
        text = {
            Column(Modifier.width(560.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconAction(Icons.Outlined.FormatBold, "Negrito", onClick = { apply { t, s, e -> toggleInline(t, s, e, MarkKind.Bold) } })
                    IconAction(Icons.Outlined.FormatItalic, "Itálico", onClick = { apply { t, s, e -> toggleInline(t, s, e, MarkKind.Italic) } })
                    IconAction(Icons.Outlined.Title, "Título", onClick = { apply { t, s, e -> toggleBlock(t, s, e, BlockKind.Heading3) } })
                    IconAction(Icons.Outlined.FormatListBulleted, "Lista", onClick = { apply { t, s, e -> toggleBlock(t, s, e, BlockKind.ListItem) } })
                    IconAction(Icons.Outlined.FormatQuote, "Citação", onClick = { apply { t, s, e -> toggleBlock(t, s, e, BlockKind.Quote) } })
                }
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = field,
                    onValueChange = { field = it; onChange(it.text) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface, fontFamily = LocalFonts.current.book),
                    cursorBrush = SolidColor(scheme.primary),
                    visualTransformation = styling,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 420.dp)
                        .clip(RoundedCornerShape(10.dp)).background(scheme.surfaceContainerLowest)
                        .border(1.dp, scheme.outlineVariant, RoundedCornerShape(10.dp)).padding(14.dp)
                        .focusRequester(focus),
                )
                Spacer(Modifier.height(6.dp))
                Text("As mudanças são salvas sozinhas · Esc fecha", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
    )
}

// ---------- no manuscrito ----------

@Composable
private fun MentionsCard(c: EscribaController, entry: UniverseEntry) {
    val scheme = MaterialTheme.colorScheme
    val book = c.book ?: return
    // Recalcula quando muda o nome, os apelidos ou algum capítulo; os capítulos sem mudança vêm do cache.
    val mentions = remember(entry.name, entry.attributes, book.manifest.chapters) { c.entryMentions(entry) }
    var showAll by remember(entry.id) { mutableStateOf(false) }
    Surface(shape = Radius.surface, color = LocalPalette.current.paper, border = androidx.compose.foundation.BorderStroke(1.dp, LocalPalette.current.hairline)) {
        Column(Modifier.padding(14.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Overline("No manuscrito")
            when {
                mentions.terms.isEmpty() -> Hint("Dê um nome a esta ficha para procurá-la no manuscrito.")
                mentions.totalChapters == 0 -> Hint("O livro ainda não tem capítulos.")
                mentions.chapters.isEmpty() -> Hint("O nome ainda não aparece em nenhum capítulo.")
                else -> {
                    Text("Citado em ${mentions.citedIn} de ${mentions.totalChapters} capítulo(s)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    val visible = if (showAll) mentions.chapters else mentions.chapters.take(5)
                    visible.forEach { ch ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .clickable { c.openMention(ch.chapterId, ch.matches.first()) }.padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(ch.title, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${ch.matches.size}×", style = MaterialTheme.typography.labelMedium, color = scheme.primary)
                        }
                    }
                    if (mentions.chapters.size > 5) {
                        SmallChip(if (showAll) "Mostrar menos" else "Ver os ${mentions.chapters.size} capítulos") { showAll = !showAll }
                    }
                }
            }
            if (mentions.terms.isNotEmpty()) {
                Hint("Procurei por: ${mentions.terms.joinToString(", ")}. Nome escrito não quer dizer presença na cena.")
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// ---------- lixeira ----------

@Composable
private fun EntryTrashDialog(c: EscribaController, onDismiss: () -> Unit) {
    val items = c.entryTrash
    var confirmDelete by remember { mutableStateOf<com.cuboide.escriba.core.storage.TrashItem?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lixeira das fichas deste livro") },
        text = {
            Column(Modifier.width(480.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Hint("As fichas removidas ficam aqui por ${c.settings.trashRetentionDays} dias e depois somem sozinhas.")
                if (items.isEmpty()) Hint("Nenhuma ficha na lixeira.")
                items.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        item.entry?.let { Icon(it.kind.icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title.ifBlank { "Sem nome" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${item.entry?.kind?.label ?: "Ficha"} · ${c.trashDaysLeft(item)} dia(s) até apagar",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconAction(Icons.Outlined.RestoreFromTrash, "Restaurar", onClick = { c.restoreFromTrash(item) })
                        IconAction(Icons.Outlined.DeleteOutline, "Apagar de vez", onClick = { confirmDelete = item })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
    )
    confirmDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Apagar de vez?") },
            text = { Text("“${item.title.ifBlank { "Sem nome" }}” será apagada definitivamente. Isso não pode ser desfeito.") },
            confirmButton = { TextButton(onClick = { c.deleteForever(item); confirmDelete = null }) { Text("Apagar de vez") } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancelar") } },
        )
    }
}
