package com.cuboide.escriba.core.text

/** Resultado de uma edição: texto novo e seleção (início e fim) depois dela. */
data class Edit(val text: String, val selStart: Int, val selEnd: Int = selStart)

private fun lineStartOf(text: String, pos: Int) = if (pos <= 0) 0 else text.lastIndexOf('\n', pos - 1) + 1
private fun lineEndOf(text: String, pos: Int) = text.indexOf('\n', pos).let { if (it < 0) text.length else it }

/**
 * Liga ou desliga uma marca de texto (negrito, itálico, versalete, sobrescrito, subscrito) na seleção.
 * - Seleção dentro de uma marca do mesmo tipo: tira a marca.
 * - Seleção comum: envolve (espaços nas pontas ficam de fora, para a marcação valer).
 * - Sem seleção: dentro da marca, pula para depois dela; entre um par vazio, apaga o par;
 *   senão insere o par e deixa o cursor no meio, para digitar já formatado.
 * Seleções de várias linhas são tratadas linha a linha.
 */
fun toggleInline(text: String, selStart: Int, selEnd: Int, kind: MarkKind): Edit {
    require(kind != MarkKind.Footnote)
    val s = minOf(selStart, selEnd)
    val e = maxOf(selStart, selEnd)
    if (s == e) return toggleAtCursor(text, s, kind)
    if (text.substring(s, e).contains('\n')) return perLine(text, s, e) { t, ls, le -> toggleRange(t, ls, le, kind) }
    return toggleRange(text, s, e, kind)
}

private fun toggleAtCursor(text: String, pos: Int, kind: MarkKind): Edit {
    val open = kind.open
    val close = kind.close
    if (pos >= open.length && text.startsWith(open, pos - open.length) && text.startsWith(close, pos)) {
        // Par vazio recém-criado: desfaz.
        return Edit(text.removeRange(pos - open.length, pos + close.length), pos - open.length)
    }
    val inside = findInlineMarks(text, lineStartOf(text, pos), lineEndOf(text, pos))
        .filter { it.kind == kind && pos in it.contentStart..it.contentEnd }
        .minByOrNull { it.end - it.start }
    if (inside != null) return Edit(text, inside.end)
    return Edit(text.substring(0, pos) + open + close + text.substring(pos), pos + open.length)
}

private fun toggleRange(text: String, rawStart: Int, rawEnd: Int, kind: MarkKind): Edit {
    var s = rawStart
    var e = rawEnd
    while (s < e && text[s].isWhitespace()) s++
    while (e > s && text[e - 1].isWhitespace()) e--
    if (s == e) return Edit(text, rawStart, rawEnd)
    val marks = findInlineMarks(text, lineStartOf(text, s), lineEndOf(text, s)).filter { it.kind == kind }
    val existing = marks.firstOrNull { it.start == s && it.end == e }
        ?: marks.filter { it.contentStart <= s && e <= it.contentEnd }.minByOrNull { it.end - it.start }
    if (existing != null) {
        val openLen = existing.contentStart - existing.start
        val unwrapped = text.substring(0, existing.start) +
            text.substring(existing.contentStart, existing.contentEnd) + text.substring(existing.end)
        return Edit(unwrapped, existing.start, existing.contentEnd - openLen)
    }
    val wrapped = text.substring(0, s) + kind.open + text.substring(s, e) + kind.close + text.substring(e)
    return Edit(wrapped, s + kind.open.length, e + kind.open.length)
}

/** Aplica [op] ao trecho selecionado de cada linha, de baixo para cima (offsets de cima não mudam). */
private fun perLine(text: String, s: Int, e: Int, op: (String, Int, Int) -> Edit): Edit {
    var current = text
    val ranges = ArrayList<Pair<Int, Int>>()
    var lineStart = lineStartOf(text, s)
    while (lineStart <= e) {
        val lineEnd = lineEndOf(text, lineStart)
        val from = maxOf(lineStart, s)
        val to = minOf(lineEnd, e)
        if (to > from && text.substring(from, to).isNotBlank()) ranges += from to to
        if (lineEnd >= text.length) break
        lineStart = lineEnd + 1
    }
    for ((from, to) in ranges.asReversed()) current = op(current, from, to).text
    return Edit(current, s, (e + current.length - text.length).coerceIn(s, current.length))
}

/** Linhas tocadas pela seleção: (início, fim) de cada uma. */
private fun touchedLines(text: String, s: Int, e: Int): List<Pair<Int, Int>> {
    val out = ArrayList<Pair<Int, Int>>()
    var lineStart = lineStartOf(text, s)
    while (true) {
        val lineEnd = lineEndOf(text, lineStart)
        out += lineStart to lineEnd
        if (lineEnd >= e || lineEnd >= text.length) break
        lineStart = lineEnd + 1
    }
    return out
}

/** Reescreve as linhas tocadas pela seleção e reposiciona a seleção pelas diferenças de tamanho. */
private fun rewriteLines(text: String, selStart: Int, selEnd: Int, transform: (String) -> String): Edit {
    val s = minOf(selStart, selEnd)
    val e = maxOf(selStart, selEnd)
    val lines = touchedLines(text, s, e)
    val sb = StringBuilder(text)
    var newS = s
    var newE = e
    for ((ls, le) in lines.asReversed()) {
        val old = text.substring(ls, le)
        val updated = transform(old)
        if (updated == old) continue
        sb.replace(ls, le, updated)
        val delta = updated.length - old.length
        // Posições depois da linha andam junto; dentro dela, ficam presas ao conteúdo.
        if (s > le) newS += delta else if (s > ls) newS = (newS + delta).coerceAtLeast(ls)
        if (e > le) newE += delta else if (e > ls) newE = (newE + delta).coerceIn(ls, ls + updated.length)
    }
    val out = sb.toString()
    return Edit(out, newS.coerceIn(0, out.length), newE.coerceIn(0, out.length))
}

/** Conteúdo da linha sem prefixo de bloco nem marcadores de centralização. */
fun lineContent(line: String): String {
    val block = parseLine(line)
    return when (block.kind) {
        BlockKind.SceneBreak, BlockKind.Image -> line
        else -> line.substring(block.contentStart, block.contentEnd)
    }
}

/**
 * Transforma as linhas tocadas em [kind] (título, lista, citação). Se todas já são desse tipo,
 * voltam a ser parágrafos comuns. Quebras de cena e imagens não mudam.
 */
fun toggleBlock(text: String, selStart: Int, selEnd: Int, kind: BlockKind): Edit {
    val prefix = prefixOf(kind)
    require(prefix.isNotEmpty()) { "Bloco sem prefixo: $kind" }
    val s = minOf(selStart, selEnd)
    val e = maxOf(selStart, selEnd)
    val editable = touchedLines(text, s, e).map { (a, b) -> parseLine(text.substring(a, b)).kind }
        .filter { it != BlockKind.SceneBreak && it != BlockKind.Image }
    val allAlready = editable.isNotEmpty() && editable.all { it == kind }
    return rewriteLines(text, s, e) { line ->
        val block = parseLine(line)
        if (block.kind == BlockKind.SceneBreak || block.kind == BlockKind.Image) line
        else if (allAlready) lineContent(line)
        else prefix + lineContent(line)
    }
}

/** Centraliza (ou volta ao alinhamento normal) os parágrafos tocados. Títulos, listas e citações não mudam. */
fun toggleCenter(text: String, selStart: Int, selEnd: Int): Edit {
    val s = minOf(selStart, selEnd)
    val e = maxOf(selStart, selEnd)
    val paragraphs = touchedLines(text, s, e).map { (a, b) -> parseLine(text.substring(a, b)) }.filter { it.kind == BlockKind.Paragraph }
    val allCentered = paragraphs.isNotEmpty() && paragraphs.all { it.centered }
    return rewriteLines(text, s, e) { line ->
        val block = parseLine(line)
        when {
            block.kind != BlockKind.Paragraph -> line
            allCentered -> lineContent(line)
            block.centered || line.isBlank() -> line
            else -> CENTER_OPEN + line + CENTER_CLOSE
        }
    }
}

/** Tira toda a formatação (bloco, centralização, negrito etc.) das linhas tocadas. Notas de rodapé ficam. */
fun clearFormatting(text: String, selStart: Int, selEnd: Int): Edit =
    rewriteLines(text, selStart, selEnd) { line ->
        val block = parseLine(line)
        if (block.kind == BlockKind.SceneBreak || block.kind == BlockKind.Image) line
        else stripInlineKeepingNotes(lineContent(line))
    }

private fun stripInlineKeepingNotes(text: String): String {
    val marks = findInlineMarks(text).filter { it.kind != MarkKind.Footnote }
    if (marks.isEmpty()) return text
    val hidden = BooleanArray(text.length)
    for (m in marks) {
        for (i in m.start until m.contentStart) hidden[i] = true
        for (i in m.contentEnd until m.end) hidden[i] = true
    }
    return buildString { text.forEachIndexed { i, c -> if (!hidden[i]) append(c) } }
}

/** Põe [line] num parágrafo próprio no lugar da seleção (quebra de cena, imagem); o cursor vai para a linha seguinte. */
fun insertStandaloneLine(text: String, selStart: Int, selEnd: Int, line: String): Edit {
    val s = minOf(selStart, selEnd)
    val e = maxOf(selStart, selEnd)
    val before = text.substring(0, s)
    val after = text.substring(e)
    val lead = if (before.isEmpty() || before.endsWith("\n")) "" else "\n"
    val trail = if (after.startsWith("\n")) "" else "\n"
    val out = before + lead + line + trail + after
    val cursor = before.length + lead.length + line.length + 1
    return Edit(out, cursor.coerceAtMost(out.length))
}

/** Insere a chamada `[^id]` logo depois da seleção (antes de espaços finais). */
fun insertFootnoteRef(text: String, selStart: Int, selEnd: Int, id: String): Edit {
    var at = maxOf(selStart, selEnd)
    while (at > minOf(selStart, selEnd) && text[at - 1].isWhitespace()) at--
    val ref = "${MarkKind.Footnote.open}$id${MarkKind.Footnote.close}"
    return Edit(text.substring(0, at) + ref + text.substring(at), at + ref.length)
}

/** Próximo id livre de nota: um número maior que todos os usados. */
fun nextFootnoteId(body: String, existing: Collection<String>): String {
    val used = (footnoteIds(body) + existing).mapNotNull { it.toIntOrNull() }
    return ((used.maxOrNull() ?: 0) + 1).toString()
}

/**
 * O campo de texto pode mandar a tecla seguinte antes de receber o texto já corrigido (digitação rápida):
 * a mudança chega sobre [fieldBase] (o que o campo conhecia), não sobre [ours] (o texto corrigido).
 * Se a mudança aconteceu depois de [baseCursor] (onde a correção terminou), aplica a mesma mudança em [ours].
 * Devolve o texto resultante e o deslocamento a somar nas posições do campo, ou `null` se não der para rebasear.
 */
fun rebaseFieldEdit(fieldBase: String, fieldNew: String, ours: String, baseCursor: Int): Pair<String, Int>? {
    var prefix = 0
    val maxPrefix = minOf(fieldBase.length, fieldNew.length)
    while (prefix < maxPrefix && fieldBase[prefix] == fieldNew[prefix]) prefix++
    var suffix = 0
    while (suffix < fieldBase.length - prefix && suffix < fieldNew.length - prefix &&
        fieldBase[fieldBase.length - 1 - suffix] == fieldNew[fieldNew.length - 1 - suffix]
    ) suffix++
    if (prefix < baseCursor) return null
    val delta = ours.length - fieldBase.length
    val removedEnd = fieldBase.length - suffix
    val oursStart = prefix + delta
    val oursEnd = removedEnd + delta
    if (oursStart < 0 || oursEnd > ours.length || oursStart > oursEnd) return null
    return ours.substring(0, oursStart) + fieldNew.substring(prefix, fieldNew.length - suffix) + ours.substring(oursEnd) to delta
}

/**
 * Regras aplicadas enquanto se digita um caractere (só quando o texto ganhou exatamente um caractere no cursor):
 * - Enter num item de lista ou citação continua o bloco; Enter num item vazio encerra o bloco.
 * - Com [smartTypography]: `--` vira travessão, `...` vira reticências, aspas retas viram curvas,
 *   e a primeira letra de um parágrafo novo fica maiúscula.
 * Devolve `null` quando nenhuma regra se aplica.
 */
fun applyTypingRules(oldText: String, newText: String, cursor: Int, smartTypography: Boolean): Edit? {
    if (newText.length != oldText.length + 1 || cursor < 1 || cursor > newText.length) return null
    val p = cursor - 1
    if (newText.substring(0, p) != oldText.substring(0, p) || newText.substring(cursor) != oldText.substring(p)) return null
    val ch = newText[p]
    val lineStart = lineStartOf(newText, p)

    if (ch == '\n') {
        val prevLine = newText.substring(lineStart, p)
        for (kind in listOf(BlockKind.ListItem, BlockKind.Quote)) {
            val prefix = prefixOf(kind)
            if (prevLine == prefix) {
                // Item vazio: sai da lista e fica num parágrafo comum vazio.
                return Edit(newText.removeRange(lineStart, p + 1), lineStart)
            }
            if (prevLine.startsWith(prefix) && !isSceneBreakLine(prevLine)) {
                return Edit(newText.substring(0, cursor) + prefix + newText.substring(cursor), cursor + prefix.length)
            }
        }
        return null
    }
    if (!smartTypography) return null

    val prev = if (p > 0) newText[p - 1] else '\n'
    when {
        ch == '-' && prev == '-' && (p < 2 || newText[p - 2] != '-') ->
            return Edit(newText.replaceRange(p - 1, p + 1, "—"), p)
        ch == '.' && p >= 2 && newText.substring(p - 2, p) == ".." ->
            return Edit(newText.replaceRange(p - 2, p + 1, "…"), p - 1)
        ch == '"' -> {
            val opening = prev.isWhitespace() || prev in "([{—–“‘"
            return Edit(newText.replaceRange(p, p + 1, if (opening) "“" else "”"), cursor)
        }
        ch == '\'' -> {
            val opening = prev.isWhitespace() || prev in "([{—–“"
            return Edit(newText.replaceRange(p, p + 1, if (opening) "‘" else "’"), cursor)
        }
        ch.isLowerCase() -> {
            val before = newText.substring(lineStart, p)
            val restOfLineEmpty = lineEndOf(newText, cursor) == cursor
            val atParagraphStart = before.isEmpty() || BLOCK_PREFIXES.any { it.first == before } || before == CENTER_OPEN
            if (atParagraphStart && restOfLineEmpty) return Edit(newText.replaceRange(p, p + 1, ch.uppercaseChar().toString()), cursor)
        }
    }
    return null
}
