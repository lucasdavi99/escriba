package com.cuboide.escriba.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.AssistantController
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.Section
import com.cuboide.escriba.core.ai.AiRef
import com.cuboide.escriba.core.ai.AiTask
import com.cuboide.escriba.core.ai.ChatMessage
import com.cuboide.escriba.core.ai.ChatRole
import com.cuboide.escriba.core.text.DiffKind
import com.cuboide.escriba.core.text.diffStats
import com.cuboide.escriba.core.text.diffWords

/**
 * A conversa com o assistente: mensagens, pedidos prontos, referências (`@`) e campo de texto. Usada na aba
 * Assistente e no painel flutuante.
 */
@Composable
fun AssistantChat(c: EscribaController, modifier: Modifier = Modifier, compact: Boolean = false) {
    val a = c.assistant
    var replacing by remember { mutableStateOf<ChatMessage?>(null) }
    Column(modifier) {
        if (!a.config.isConfigured) {
            NotConnected(c, Modifier.fillMaxWidth().padding(bottom = Space.sm))
        }
        val messages = a.current?.messages.orEmpty()
        val listState = rememberLazyListState()
        val lastLen = messages.lastOrNull()?.text?.length ?: 0
        LaunchedEffect(messages.size, lastLen / 200) {
            if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (messages.isEmpty()) {
                EmptyChat(compact)
            } else {
                LazyColumn(Modifier.fillMaxSize(), state = listState, verticalArrangement = Arrangement.spacedBy(Space.md)) {
                    items(messages, key = { it.id }) { m ->
                        val question = if (m.role == ChatRole.Assistant) messages.getOrNull(messages.indexOfFirst { it.id == m.id } - 1) else null
                        MessageView(c, m, question, working = a.busy && m.id == messages.last().id, onReplace = { replacing = it })
                    }
                }
            }
        }
        a.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = Space.xs))
        }
        QuickTasks(a)
        Composer(c, compact)
    }
    replacing?.let { ReplaceDialog(a, it) { replacing = null } }
    if (a.consentPending) ConsentDialog(a)
}

@Composable
private fun NotConnected(c: EscribaController, modifier: Modifier) {
    FloatingPanel(modifier) {
        Row(Modifier.padding(Space.md), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Conecte uma IA com a sua chave de API para usar o assistente.",
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Space.sm))
            Button(onClick = { c.assistant.panelVisible = false; c.goTo(Section.Preferencias) }) { Text("Conectar") }
        }
    }
}

@Composable
private fun EmptyChat(compact: Boolean) {
    Column(Modifier.fillMaxSize().padding(Space.md), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Assistente", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.size(Space.xs))
        Text(
            if (compact) "Pergunte sobre o livro. Use @ para apontar capítulos, cenas e fichas."
            else "Peça ideias, melhorias ou uma leitura crítica. Ele conhece o seu Enredo e o Universo. Use @ para apontar capítulos, cenas, fichas e outros livros.",
            style = MaterialTheme.typography.bodyMedium, color = LocalPalette.current.muted,
        )
    }
}

@Composable
private fun MessageView(c: EscribaController, m: ChatMessage, question: ChatMessage?, working: Boolean, onReplace: (ChatMessage) -> Unit) {
    val a = c.assistant
    val palette = LocalPalette.current
    if (m.role == ChatRole.User) {
        Column(Modifier.fillMaxWidth().padding(start = Space.xl), horizontalAlignment = Alignment.End) {
            Column(
                Modifier.clip(Radius.surface).background(palette.accentSoft).padding(horizontal = Space.sm, vertical = Space.xs),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SelectionContainer { Text(m.text, style = MaterialTheme.typography.bodyMedium) }
                if (m.refs.isNotEmpty()) {
                    Text(m.refs.joinToString(" · ") { a.refLabel(it) }, style = MaterialTheme.typography.labelSmall, color = palette.muted)
                }
            }
        }
        return
    }
    Column(Modifier.fillMaxWidth().padding(end = Space.lg), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        SelectionContainer {
            if (m.text.isEmpty() && working) Text("Pensando…", style = MaterialTheme.typography.bodyMedium, color = palette.muted)
            else ChatMarkdownView(m.text)
        }
        if (!working && m.truncated) {
            // O provedor parou pelo limite de tamanho: o texto acima está incompleto.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(
                    "A resposta foi cortada pelo limite de tamanho.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                )
                Button(onClick = { a.continueAnswer(m) }, enabled = !a.busy) { Text("Continuar resposta") }
            }
        }
        if (!working && m.text.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                IconAction(Icons.Outlined.ContentCopy, "Copiar resposta", { a.copy(m) }, size = 30.dp)
                IconAction(Icons.Outlined.NoteAdd, "Guardar nas anotações do livro", { a.saveAsNote(m) }, size = 30.dp)
                TextButton(onClick = { a.insertAtCursor(m) }, enabled = c.currentChapterId != null) { Text("Inserir no cursor") }
                if (question != null && question.refs.any { it is AiRef.Selection }) {
                    TextButton(onClick = { onReplace(m) }) { Text("Substituir trecho") }
                }
            }
        }
        if (question != null && question.usedContext.isNotEmpty()) {
            var open by remember { mutableStateOf(false) }
            Text(
                (if (open) "▾ " else "▸ ") + "Contexto usado (${question.usedContext.size})",
                style = MaterialTheme.typography.labelSmall, color = palette.muted,
                modifier = Modifier.clickable { open = !open },
            )
            if (open) {
                // O que o autor pediu (ou o capítulo aberto) separado do que a busca achou sozinha.
                val (found, asked) = question.usedContext.partition { it.label.contains("(passagem") }
                Column(Modifier.padding(start = Space.sm)) {
                    asked.forEach { Text("• ${it.label}", style = MaterialTheme.typography.labelSmall, color = palette.muted) }
                    if (found.isNotEmpty()) {
                        Text("Achados pela busca no livro:", style = MaterialTheme.typography.labelSmall, color = palette.muted, modifier = Modifier.padding(top = 4.dp))
                        found.forEach { Text("• ${it.label}", style = MaterialTheme.typography.labelSmall, color = palette.muted) }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTasks(a: AssistantController) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = Space.xs), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AiTask.entries.forEach { t ->
            OutlinedButton(onClick = { a.runTask(t) }, enabled = !a.busy && a.config.isConfigured) { Text(t.label, style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
private fun Composer(c: EscribaController, compact: Boolean) {
    val a = c.assistant
    val palette = LocalPalette.current
    // `@consulta` logo antes do cursor abre a lista de referências.
    val text = a.draft.text
    val cursor = a.draft.selection.max.coerceIn(0, text.length)
    val at = text.lastIndexOf('@', (cursor - 1).coerceAtLeast(0)).takeIf { cursor > 0 && it >= 0 }
    val query = if (at != null && text.substring(at, cursor).none { it.isWhitespace() }) text.substring(at + 1, cursor) else null
    val options = if (query != null) a.refOptions(query) else emptyList()

    fun pick(ref: AiRef) {
        if (at != null) a.draft = TextFieldValue(text.removeRange(at, cursor), androidx.compose.ui.text.TextRange(at))
        a.addRef(ref)
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        if (options.isNotEmpty()) {
            FloatingPanel(Modifier.fillMaxWidth()) {
                LazyColumn(Modifier.heightIn(max = 220.dp)) {
                    items(options, key = { it.ref.toString() }) { o ->
                        HoverRow(selected = false, onClick = { pick(o.ref) }) {
                            Text(o.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(o.detail, style = MaterialTheme.typography.labelSmall, color = palette.muted)
                        }
                    }
                }
            }
        }
        val autoChapter = a.autoChapterRef
        val selection = a.selectionAvailable
        var editingBook by remember { mutableStateOf(false) }
        if (editingBook) BookInstructionsDialog(c) { editingBook = false }
        run {
            // Barra do pedido: modo, livro inteiro, instruções do livro e as referências que vão junto.
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ModePicker(a)
                IconAction(
                    Icons.Outlined.AutoStories,
                    if (a.fullBook) "Livro inteiro ligado: cada pergunta lê todos os capítulos (clique para desligar)"
                    else "Ler o livro inteiro: compara o capítulo com o livro todo (repetições, coerência). Custa mais por pergunta.",
                    { a.toggleFullBook() },
                    selected = a.fullBook,
                    size = 30.dp,
                )
                IconAction(
                    Icons.Outlined.Tune,
                    if (a.bookInstructions.isBlank()) "Instruções deste livro para a IA (regras da história, tom, público)"
                    else "Instruções deste livro para a IA (ativas)",
                    { editingBook = true },
                    selected = a.bookInstructions.isNotBlank(),
                    size = 30.dp,
                )
                if (a.fullBook) {
                    RefChip("Livro inteiro · ~${formatThousands(a.fullBookTokens)} tokens", "Desligar a leitura do livro inteiro") { a.toggleFullBook() }
                }
                autoChapter?.let { r ->
                    RefChip("${a.refLabel(r)} · capítulo aberto", "Não enviar o capítulo aberto") { a.dismissAutoChapter() }
                }
                a.draftRefs.forEach { r -> RefChip(a.refLabel(r), "Tirar do pedido") { a.removeRef(r) } }
                if (selection != null) {
                    // A seleção do editor só vai quando o autor pede: seleções deixadas pela Busca ou por menções não vão sozinhas.
                    Text(
                        "+ Trecho selecionado (${com.cuboide.escriba.core.text.countWords(selection)} palavras)",
                        style = MaterialTheme.typography.labelMedium, color = palette.accent,
                        modifier = Modifier.clip(Radius.control).clickable { a.attachSelection() }.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            OutlinedTextField(
                value = a.draft,
                onValueChange = { a.draft = it },
                placeholder = { Text(if (compact) "Pergunte… (@ aponta algo)" else "Pergunte ou peça algo. Use @ para apontar capítulos, cenas e fichas.") },
                modifier = Modifier.weight(1f).onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown && e.key == Key.Enter && !e.isShiftPressed) {
                        if (options.isNotEmpty()) pick(options.first().ref) else a.sendDraft()
                        true
                    } else false
                },
                minLines = 1,
                maxLines = 6,
            )
            if (a.busy) {
                IconAction(Icons.Outlined.Stop, "Parar", { a.stop() })
            } else {
                IconAction(Icons.Outlined.Send, "Enviar (Enter)", { a.sendDraft() }, enabled = a.draft.text.isNotBlank())
            }
        }
    }
}

/** Escolhe o papel do assistente nesta conversa (fica guardado nela). */
@Composable
private fun ModePicker(a: AssistantController) {
    var open by remember { mutableStateOf(false) }
    val palette = LocalPalette.current
    Box {
        WithTooltip("Papel do assistente nesta conversa") {
            Text(
                "Modo: ${a.mode?.name ?: "Livre"} ▾",
                style = MaterialTheme.typography.labelMedium,
                color = if (a.mode != null) palette.accent else palette.muted,
                modifier = Modifier.clip(Radius.control).clickable { open = true }.padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        androidx.compose.material3.DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Column { Text("Livre"); Text("Sem papel definido", style = MaterialTheme.typography.labelSmall, color = palette.muted) } },
                onClick = { open = false; a.setMode(null) },
            )
            a.config.modes.forEach { m ->
                androidx.compose.material3.DropdownMenuItem(
                    text = {
                        Column(Modifier.widthIn(max = 320.dp)) {
                            Text(m.name)
                            Text(m.instructions, style = MaterialTheme.typography.labelSmall, color = palette.muted, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    },
                    onClick = { open = false; a.setMode(m.id) },
                )
            }
        }
    }
}

/** Instruções do livro: o que a IA deve saber e seguir sobre esta história. */
@Composable
private fun BookInstructionsDialog(c: EscribaController, onClose: () -> Unit) {
    val a = c.assistant
    var text by remember { mutableStateOf(a.bookInstructions) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Instruções deste livro") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Text(
                    "Vão em toda pergunta sobre “${c.book?.manifest?.title.orEmpty()}”. Bom para regras que não estão escritas em lugar nenhum: " +
                        "ponto de vista, tempo verbal, público, tom, variante do português, o que nunca acontece na história. " +
                        "As instruções gerais (para todos os livros) ficam em Preferências.",
                    style = MaterialTheme.typography.bodySmall, color = LocalPalette.current.muted,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Ex.: Prólogo em 1ª pessoa, o resto em 3ª. Público jovem adulto, tom sombrio. Diálogos com travessão. A magia nunca é explicada.") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
                    minLines = 6,
                )
            }
        },
        confirmButton = { Button(onClick = { a.saveBookInstructions(text.trim()); onClose() }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancelar") } },
    )
}

/** 34200 → "34 mil"; abaixo de mil, o número. */
private fun formatThousands(n: Int): String = if (n >= 1000) "${(n + 500) / 1000} mil" else n.toString()

@Composable
private fun Icon16(icon: androidx.compose.ui.graphics.vector.ImageVector) =
    androidx.compose.material3.Icon(icon, null, Modifier.padding(start = 2.dp).size(14.dp), tint = LocalPalette.current.muted)

/** Mostra o trecho antes e depois da troca; só aplica quando o autor confirma. */
@Composable
private fun ReplaceDialog(a: AssistantController, answer: ChatMessage, onClose: () -> Unit) {
    val preview = remember(answer.id, answer.text) { a.replacementPreview(answer) }
    if (preview == null) { onClose(); return }
    val (before, after) = preview
    val segments = remember(before, after) { diffWords(before, after) }
    val stats = remember(segments) { diffStats(segments) }
    val removed = MaterialTheme.colorScheme.error
    val added = LocalPalette.current.accent
    val styled: AnnotatedString = remember(segments, removed, added) {
        buildAnnotatedString {
            segments.forEach { s ->
                when (s.kind) {
                    DiffKind.Equal -> append(s.text)
                    DiffKind.Inserted -> withStyle(SpanStyle(color = added)) { append(s.text) }
                    DiffKind.Deleted -> withStyle(SpanStyle(color = removed, textDecoration = TextDecoration.LineThrough)) { append(s.text) }
                }
            }
        }
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Substituir o trecho?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Text("+${stats.insertedWords} palavras · −${stats.deletedWords} palavras. A versão atual fica guardada em Versões.", style = MaterialTheme.typography.bodySmall, color = LocalPalette.current.muted)
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) { Text(styled, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = { Button(onClick = { a.replaceSelection(answer); onClose() }) { Text("Substituir") } },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancelar") } },
    )
}

/** Aviso na primeira vez: o texto do livro vai para o provedor escolhido. */
@Composable
private fun ConsentDialog(a: AssistantController) {
    AlertDialog(
        onDismissRequest = { a.declineConsent() },
        title = { Text("Enviar trechos do livro à IA?") },
        text = {
            Text(
                "Para responder, o Escriba envia o seu pedido e os trechos do livro que ele escolher como contexto " +
                    "(capítulos, cenas e fichas) ao provedor configurado: ${a.config.provider.label}. " +
                    "Nada é enviado sem você pedir, e a chave fica só neste computador.",
            )
        },
        confirmButton = { Button(onClick = { a.acceptConsent() }) { Text("Entendi, continuar") } },
        dismissButton = { TextButton(onClick = { a.declineConsent() }) { Text("Cancelar") } },
    )
}

/** Referência do pedido: clicar tira do pedido. */
@Composable
private fun RefChip(label: String, tooltip: String, onRemove: () -> Unit) {
    WithTooltip(tooltip) {
        Row(
            Modifier.clip(Radius.control).background(LocalPalette.current.accentSoft).clickable(onClick = onRemove)
                .padding(start = 8.dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Icon16(Icons.Outlined.Close)
        }
    }
}
