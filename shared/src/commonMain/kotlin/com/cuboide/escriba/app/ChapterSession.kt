package com.cuboide.escriba.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.cuboide.escriba.core.text.DailyWordMark
import com.cuboide.escriba.core.text.applyTypingRules
import com.cuboide.escriba.core.text.rebaseFieldEdit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A sessão do capítulo: o capítulo aberto no Manuscrito, com o texto em edição, o histórico de desfazer e a
 * gravação pendente. Existe uma por capítulo aberto; abrir outro capítulo cria outra.
 *
 * Toda mudança de texto entra por aqui, pela origem: [type] (edição do teclado), [edit] (edição do app) ou
 * [select] (só a seleção).
 */
class ChapterSession(
    val chapterId: String,
    text: String,
    private val scope: CoroutineScope,
    private val now: () -> Long,
    private val mark: DailyWordMark,
    private val smartTypography: () -> Boolean,
) {
    /** O que o campo do Manuscrito mostra: texto, seleção e composição do IME. */
    var value by mutableStateOf(TextFieldValue(text)); private set
    val text: String get() = value.text
    var canUndo by mutableStateOf(false); private set
    var canRedo by mutableStateOf(false); private set

    private val undoStack = ArrayDeque<TextFieldValue>()
    private val redoStack = ArrayDeque<TextFieldValue>()
    private var lastUndoAt = 0L
    private var lastUndoMergeable = false

    /** Texto que o campo conhece enquanto ainda não recebeu nossa versão corrigida (ver [rebaseFieldEdit]). */
    private var fieldBase: String? = null
    private var fieldBaseCursor = 0
    private var fieldBaseJob: Job? = null

    /**
     * Edição do teclado (digitar, colar, apagar) ou só a seleção mudando no campo. Aplica as regras de digitação
     * (travessão, aspas, continuação de lista…) e agrupa a digitação seguida num só passo do desfazer.
     */
    fun type(incomingValue: TextFieldValue) {
        if (incomingValue.text == value.text) {
            value = incomingValue
            return
        }
        var incoming = incomingValue
        val base = fieldBase
        if (base != null && !isInsertionInto(value.text, incomingValue.text)) {
            // Tecla que chegou antes de o campo receber a correção anterior: aplica sobre o texto corrigido.
            rebaseFieldEdit(base, incomingValue.text, value.text, fieldBaseCursor)?.let { (text, delta) ->
                incoming = TextFieldValue(text, TextRange(incomingValue.selection.start + delta, incomingValue.selection.end + delta))
            }
        }
        val ruled = if (incoming.selection.collapsed) {
            applyTypingRules(value.text, incoming.text, incoming.selection.end, smartTypography())
        } else null
        recordUndo(value, mergeable = true)
        change(incoming)
        if (ruled != null) {
            // A versão sem a correção vira um passo próprio: Ctrl+Z desfaz só a correção, como num editor de texto.
            recordUndo(value, mergeable = false)
            change(TextFieldValue(ruled.text, TextRange(ruled.selStart, ruled.selEnd)))
        }
        // Até o próximo quadro o campo ainda acha que o texto é incomingValue.text.
        if (value.text != incomingValue.text) {
            fieldBase = incomingValue.text
            fieldBaseCursor = incomingValue.selection.end
            fieldBaseJob?.cancel()
            fieldBaseJob = scope.launch {
                delay(FIELD_SYNC_MS)
                fieldBase = null
            }
        } else {
            fieldBase = null
        }
    }

    /**
     * Edição do app (formatação, substituição, inserção, versão restaurada, texto do Assistente): sempre um passo
     * próprio no desfazer. Com o mesmo texto, só move a seleção.
     */
    fun edit(newValue: TextFieldValue) {
        if (newValue.text == value.text) {
            value = newValue
            return
        }
        recordUndo(value, mergeable = false)
        change(newValue)
    }

    /** Só a seleção muda (ir para uma ocorrência, clicar fora do texto): não é edição nem passo do desfazer. */
    fun select(selection: TextRange) {
        val end = value.text.length
        value = value.copy(selection = TextRange(selection.start.coerceIn(0, end), selection.end.coerceIn(0, end)))
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(value)
        lastUndoMergeable = false
        change(previous)
        updateUndoFlags()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(value)
        lastUndoMergeable = false
        change(next)
        updateUndoFlags()
    }

    private fun change(newValue: TextFieldValue) {
        value = newValue
    }

    private fun recordUndo(previous: TextFieldValue, mergeable: Boolean) {
        val t = now()
        val merge = mergeable && lastUndoMergeable && undoStack.isNotEmpty() && t - lastUndoAt < UNDO_MERGE_MS
        if (!merge) {
            undoStack.addLast(previous)
            if (undoStack.size > MAX_UNDO) undoStack.removeFirst()
        }
        lastUndoAt = t
        lastUndoMergeable = mergeable
        redoStack.clear()
        updateUndoFlags()
    }

    private fun updateUndoFlags() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    private fun isInsertionInto(old: String, new: String): Boolean {
        if (new.length <= old.length) return false
        var prefix = 0
        while (prefix < old.length && old[prefix] == new[prefix]) prefix++
        var suffix = 0
        while (suffix < old.length - prefix && old[old.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++
        return prefix + suffix >= old.length
    }

    companion object {
        const val UNDO_MERGE_MS = 1_500L
        const val FIELD_SYNC_MS = 120L
        const val MAX_UNDO = 300
    }
}
