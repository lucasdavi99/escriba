package com.cuboide.escriba.core.text

/**
 * Marcação do manuscrito. O capítulo continua sendo texto puro com um parágrafo por linha, numa
 * sintaxe compatível com Markdown/Pandoc, para que busca, versões e exportação leiam o mesmo arquivo:
 *
 * ```
 * # Título  ## Subtítulo  ### Chamada     títulos dentro do capítulo
 * * item                                  lista (não "- ", que muita gente usa como travessão de diálogo)
 * > trecho                                citação em bloco
 * ->texto<-                               parágrafo centralizado
 * * * *                                   quebra de cena (também *** ou #)
 * ![legenda](assets/foto.png)             imagem
 * **negrito**  *itálico*  _itálico_
 * [texto]{.smallcaps}                     versalete
 * ^sobrescrito^  ~subscrito~
 * [^1]                                    nota de rodapé (o texto fica em ChapterMeta.footnotes)
 * ```
 */
enum class BlockKind { Paragraph, Heading1, Heading2, Heading3, ListItem, Quote, SceneBreak, Image }

/** Um parágrafo (linha) já classificado. Offsets relativos ao início da linha. */
data class LineBlock(
    val kind: BlockKind,
    val contentStart: Int,
    val contentEnd: Int,
    val centered: Boolean = false,
    val imageCaption: String = "",
    val imagePath: String = "",
)

private val SCENE_BREAK_LINE = Regex("(\\*\\s*){3,}|#")
private val IMAGE_LINE = Regex("!\\[([^\\]\\n]*)]\\(([^)\\n]+)\\)\\s*")

fun isSceneBreakLine(line: String): Boolean = SCENE_BREAK_LINE.matches(line.trim())

/** Prefixos de bloco, do mais longo para o mais curto (para "### " não virar "# "). */
val BLOCK_PREFIXES = listOf("### " to BlockKind.Heading3, "## " to BlockKind.Heading2, "# " to BlockKind.Heading1, "* " to BlockKind.ListItem, "> " to BlockKind.Quote)

fun prefixOf(kind: BlockKind): String = BLOCK_PREFIXES.firstOrNull { it.second == kind }?.first.orEmpty()

const val CENTER_OPEN = "->"
const val CENTER_CLOSE = "<-"

fun parseLine(line: String): LineBlock {
    if (isSceneBreakLine(line)) return LineBlock(BlockKind.SceneBreak, 0, line.length)
    IMAGE_LINE.matchEntire(line)?.let { m ->
        return LineBlock(BlockKind.Image, 0, line.length, imageCaption = m.groupValues[1], imagePath = m.groupValues[2].trim())
    }
    for ((prefix, kind) in BLOCK_PREFIXES) {
        if (line.startsWith(prefix)) return LineBlock(kind, prefix.length, line.length)
    }
    if (line.length >= CENTER_OPEN.length + CENTER_CLOSE.length && line.startsWith(CENTER_OPEN) && line.endsWith(CENTER_CLOSE)) {
        return LineBlock(BlockKind.Paragraph, CENTER_OPEN.length, line.length - CENTER_CLOSE.length, centered = true)
    }
    return LineBlock(BlockKind.Paragraph, 0, line.length)
}

/** Linha do texto com sua posição absoluta. */
data class TextLine(val start: Int, val end: Int, val block: LineBlock)

fun parseLines(text: String): List<TextLine> {
    val out = ArrayList<TextLine>()
    var start = 0
    while (start <= text.length) {
        val nl = text.indexOf('\n', start).let { if (it < 0) text.length else it }
        out += TextLine(start, nl, parseLine(text.substring(start, nl)))
        start = nl + 1
    }
    return out
}

enum class MarkKind(val open: String, val close: String) {
    Bold("**", "**"),
    Italic("*", "*"),
    SmallCaps("[", "]{.smallcaps}"),
    Superscript("^", "^"),
    Subscript("~", "~"),
    Footnote("[^", "]"),
}

/** Trecho marcado: [start, end) inclui os marcadores; [contentStart, contentEnd) é o conteúdo. */
data class InlineMark(val kind: MarkKind, val start: Int, val end: Int, val contentStart: Int, val contentEnd: Int)

/**
 * Como no Markdown, o marcador de abertura não pode vir seguido de espaço nem o de fechamento precedido
 * de espaço (assim "2 * 3 * 4" fica literal), e `_` só conta fora de palavras ("nome_de_arquivo" fica literal).
 */
private val INLINE = Regex(
    "\\[\\^([^\\]\\s]+)]" +                                                // 1 nota
        "|\\[([^\\]\\n]+?)]\\{\\.smallcaps}" +                            // 2 versalete
        "|\\*\\*\\*(?=[^\\s*])(.+?)(?<=[^\\s*])\\*\\*\\*" +               // 3 negrito + itálico
        "|\\*\\*(?=\\S)(.+?)(?<=\\S)\\*\\*" +                             // 4 negrito
        "|(?<!\\*)\\*(?=[^\\s*])(.+?)(?<=[^\\s*])\\*(?!\\*)" +             // 5 itálico *
        "|(?<![\\p{L}\\p{N}_])_(?=\\S)(.+?)(?<=\\S)_(?![\\p{L}\\p{N}_])" + // 6 itálico _
        "|\\^([^\\s^\\[\\]]+)\\^" +                                       // 7 sobrescrito
        "|~([^\\s~]+)~",                                                  // 8 subscrito
)

/** Marcas de [text] entre [from] e [to], inclusive as aninhadas (ex.: itálico dentro de negrito). */
fun findInlineMarks(text: String, from: Int = 0, to: Int = text.length): List<InlineMark> {
    val out = ArrayList<InlineMark>()
    collectMarks(text, from, to, out)
    return out
}

private fun collectMarks(text: String, from: Int, to: Int, out: MutableList<InlineMark>) {
    if (from >= to) return
    val region = text.substring(from, to)
    for (m in INLINE.findAll(region)) {
        val group = (1..8).first { m.groups[it] != null }
        val kind = when (group) {
            1 -> MarkKind.Footnote
            2 -> MarkKind.SmallCaps
            3, 4 -> MarkKind.Bold
            5, 6 -> MarkKind.Italic
            7 -> MarkKind.Superscript
            else -> MarkKind.Subscript
        }
        // Em ***x*** o negrito envolve "*x*", e a recursão abaixo encontra o itálico.
        val content = if (group == 3) (m.range.first + 2)..(m.range.last - 2) else m.groups[group]!!.range
        val mark = InlineMark(kind, from + m.range.first, from + m.range.last + 1, from + content.first, from + content.last + 1)
        out += mark
        if (kind == MarkKind.Bold || kind == MarkKind.Italic || kind == MarkKind.SmallCaps) {
            collectMarks(text, mark.contentStart, mark.contentEnd, out)
        }
    }
}

/** Texto visível de uma linha, sem nenhum marcador (o que o leitor lê). Notas e imagens somem. */
fun plainLine(line: String): String {
    val block = parseLine(line)
    return when (block.kind) {
        BlockKind.SceneBreak -> ""
        BlockKind.Image -> block.imageCaption
        else -> stripInline(line.substring(block.contentStart, block.contentEnd))
    }
}

fun stripInline(text: String): String {
    val marks = findInlineMarks(text)
    if (marks.isEmpty()) return text
    val hidden = BooleanArray(text.length)
    for (m in marks) {
        if (m.kind == MarkKind.Footnote) {
            for (i in m.start until m.end) hidden[i] = true
        } else {
            for (i in m.start until m.contentStart) hidden[i] = true
            for (i in m.contentEnd until m.end) hidden[i] = true
        }
    }
    return buildString { text.forEachIndexed { i, c -> if (!hidden[i]) append(c) } }
}

/** Capítulo inteiro sem marcação, um parágrafo por linha. */
fun stripMarkup(body: String): String = body.split('\n').joinToString("\n") { plainLine(it) }

/**
 * Texto que o leitor vê ([text], igual a [stripMarkup]) e, para cada caractere dele, a posição no texto
 * guardado ([rawIndex]). Serve para buscar no que aparece na tela e voltar ao lugar certo do arquivo.
 */
class VisibleText(val text: String, val rawIndex: IntArray) {
    /** Posição no texto guardado logo depois do caractere visível [end] - 1 (fim exclusivo de um trecho). */
    fun rawEnd(end: Int): Int = if (end <= 0) 0 else rawIndex[end - 1] + 1
}

fun visibleText(body: String): VisibleText {
    val hidden = BooleanArray(body.length)
    for (line in parseLines(body)) {
        val b = line.block
        when (b.kind) {
            BlockKind.SceneBreak -> for (i in line.start until line.end) hidden[i] = true
            BlockKind.Image -> {
                val captionStart = line.start + body.substring(line.start, line.end).indexOf("![") + 2
                for (i in line.start until line.end) hidden[i] = i !in captionStart until captionStart + b.imageCaption.length
            }
            else -> {
                for (i in line.start until line.start + b.contentStart) hidden[i] = true
                for (i in line.start + b.contentEnd until line.end) hidden[i] = true
                for (m in findInlineMarks(body, line.start + b.contentStart, line.start + b.contentEnd)) {
                    if (m.kind == MarkKind.Footnote) {
                        for (i in m.start until m.end) hidden[i] = true
                    } else {
                        for (i in m.start until m.contentStart) hidden[i] = true
                        for (i in m.contentEnd until m.end) hidden[i] = true
                    }
                }
            }
        }
    }
    val text = StringBuilder(body.length)
    val map = IntArray(body.length)
    var n = 0
    body.forEachIndexed { i, ch ->
        if (!hidden[i]) {
            text.append(ch)
            map[n++] = i
        }
    }
    return VisibleText(text.toString(), map.copyOf(n))
}

/** Palavras do capítulo como o leitor as vê (marcadores, notas e caminhos de imagem não contam). */
fun countManuscriptWords(body: String): Int = countWords(stripMarkup(body))

/** Ids das notas de rodapé na ordem em que aparecem no texto. */
fun footnoteIds(body: String): List<String> =
    findInlineMarks(body).filter { it.kind == MarkKind.Footnote }.sortedBy { it.start }
        .map { body.substring(it.contentStart, it.contentEnd) }.distinct()
