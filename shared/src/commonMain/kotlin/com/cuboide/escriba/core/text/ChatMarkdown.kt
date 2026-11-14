package com.cuboide.escriba.core.text

/**
 * Markdown simples das respostas da IA (títulos, listas, citações, código, linhas e ênfase), para mostrar formatado
 * no chat. Não é o Markdown completo: só o que os modelos costumam usar.
 */
sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    /** [marker] é "•" ou o número ("1."); [depth] começa em 0. */
    data class ListItem(val text: String, val depth: Int, val marker: String) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Code(val text: String) : MdBlock
    data object Rule : MdBlock
}

/** Pedaço de texto com a ênfase que ele tem. */
data class MdRun(val text: String, val bold: Boolean = false, val italic: Boolean = false, val code: Boolean = false)

private val HEADING = Regex("^\\s{0,3}(#{1,6})\\s+(.*?)\\s*#*\\s*$")
private val RULE = Regex("^\\s{0,3}([-*_])(\\s*\\1){2,}\\s*$")
private val LIST = Regex("^(\\s*)([*+-]|\\d{1,3}[.)])\\s+(.*)$")
private val QUOTE = Regex("^\\s{0,3}>\\s?(.*)$")
private val FENCE = Regex("^\\s{0,3}(```|~~~)")

fun parseChatMarkdown(source: String): List<MdBlock> {
    val out = ArrayList<MdBlock>()
    val para = ArrayList<String>()
    val quote = ArrayList<String>()
    var code: StringBuilder? = null

    fun flushPara() {
        if (para.isNotEmpty()) out += MdBlock.Paragraph(para.joinToString("\n"))
        para.clear()
    }
    fun flushQuote() {
        if (quote.isNotEmpty()) out += MdBlock.Quote(quote.joinToString("\n"))
        quote.clear()
    }

    for (raw in source.replace("\r\n", "\n").split('\n')) {
        val line = raw.trimEnd()
        if (code != null) {
            if (FENCE.containsMatchIn(line)) {
                out += MdBlock.Code(code.toString().trimEnd('\n'))
                code = null
            } else {
                code.append(raw).append('\n')
            }
            continue
        }
        if (FENCE.containsMatchIn(line)) {
            flushPara(); flushQuote()
            code = StringBuilder()
            continue
        }
        if (line.isBlank()) {
            flushPara(); flushQuote()
            continue
        }
        val quoteMatch = QUOTE.matchEntire(line)
        if (quoteMatch != null) {
            flushPara()
            quote += quoteMatch.groupValues[1]
            continue
        }
        flushQuote()
        if (RULE.matches(line)) {
            flushPara()
            out += MdBlock.Rule
            continue
        }
        val heading = HEADING.matchEntire(line)
        if (heading != null) {
            flushPara()
            out += MdBlock.Heading(heading.groupValues[1].length, heading.groupValues[2])
            continue
        }
        val item = LIST.matchEntire(line)
        if (item != null) {
            flushPara()
            val indent = item.groupValues[1].replace("\t", "    ").length
            val marker = item.groupValues[2].let { if (it[0].isDigit()) it.dropLast(1) + "." else "•" }
            out += MdBlock.ListItem(item.groupValues[3], (indent / 2).coerceAtMost(3), marker)
            continue
        }
        // Linha recuada logo depois de um item continua o item.
        val last = out.lastOrNull()
        if (para.isEmpty() && last is MdBlock.ListItem && raw.startsWith(" ")) {
            out[out.lastIndex] = last.copy(text = last.text + " " + line.trim())
            continue
        }
        para += line.trim()
    }
    code?.let { out += MdBlock.Code(it.toString().trimEnd('\n')) }
    flushPara(); flushQuote()
    return out
}

private val INLINE = Regex(
    "`([^`\\n]+)`" +
        "|\\*\\*\\*(?=\\S)(.+?)(?<=\\S)\\*\\*\\*" +
        // O fecho do negrito não pode ser seguido de outro `*`: em "**a *b***" o itálico fecha antes.
        "|\\*\\*(?=\\S)(.+?)(?<=\\S)\\*\\*(?!\\*)" +
        "|(?<![\\p{L}\\p{N}_])__(?=\\S)(.+?)(?<=\\S)__(?![\\p{L}\\p{N}_])" +
        "|(?<![*\\p{L}\\p{N}])\\*(?=[^\\s*])(.+?)(?<=[^\\s*])\\*(?![*\\p{L}\\p{N}])" +
        "|(?<![\\p{L}\\p{N}_])_(?=\\S)(.+?)(?<=\\S)_(?![\\p{L}\\p{N}_])",
)

/** Ênfase dentro de uma linha: `código`, ***negrito itálico***, **negrito**, *itálico* (e as formas com `_`). */
fun parseInlineMarkdown(text: String, bold: Boolean = false, italic: Boolean = false): List<MdRun> {
    val out = ArrayList<MdRun>()
    var pos = 0
    for (m in INLINE.findAll(text)) {
        if (m.range.first > pos) out += MdRun(text.substring(pos, m.range.first), bold, italic)
        val g = m.groupValues
        when {
            g[1].isNotEmpty() -> out += MdRun(g[1], bold, italic, code = true)
            g[2].isNotEmpty() -> out += parseInlineMarkdown(g[2], bold = true, italic = true)
            g[3].isNotEmpty() -> out += parseInlineMarkdown(g[3], bold = true, italic = italic)
            g[4].isNotEmpty() -> out += parseInlineMarkdown(g[4], bold = true, italic = italic)
            g[5].isNotEmpty() -> out += parseInlineMarkdown(g[5], bold = bold, italic = true)
            g[6].isNotEmpty() -> out += parseInlineMarkdown(g[6], bold = bold, italic = true)
        }
        pos = m.range.last + 1
    }
    if (pos < text.length) out += MdRun(text.substring(pos), bold, italic)
    return out
}

/**
 * A resposta da IA na marcação do Manuscrito (um parágrafo por linha): títulos e itens de lista viram os do
 * Manuscrito, linhas horizontais e cercas de código somem, e negrito/itálico ficam como estão (a sintaxe é a mesma).
 */
fun chatMarkdownToManuscript(source: String): String =
    parseChatMarkdown(source).mapNotNull { b ->
        when (b) {
            is MdBlock.Heading -> "#".repeat(b.level.coerceIn(1, 3)) + " " + b.text
            is MdBlock.Paragraph -> b.text.lines().joinToString(" ") { it.trim() }
            is MdBlock.ListItem -> "* " + (if (b.marker != "•") b.marker + " " else "") + b.text
            is MdBlock.Quote -> b.text.lines().joinToString(" ") { it.trim() }.let { "> $it" }
            is MdBlock.Code -> b.text.lines().joinToString(" ") { it.trim() }
            MdBlock.Rule -> null
        }
    }.joinToString("\n")
