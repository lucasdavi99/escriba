package com.cuboide.escriba.core.export

import com.cuboide.escriba.core.model.BookManifest
import com.cuboide.escriba.core.model.ChapterMeta
import com.cuboide.escriba.core.text.BlockKind
import com.cuboide.escriba.core.text.LineBlock
import com.cuboide.escriba.core.text.MarkKind
import com.cuboide.escriba.core.text.findInlineMarks
import com.cuboide.escriba.core.text.footnoteIds
import com.cuboide.escriba.core.text.isSceneBreakLine
import com.cuboide.escriba.core.text.parseLine
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

enum class ExportFormat(val label: String, val extension: String) {
    Markdown("Markdown (.md)", "md"),
    Texto("Texto puro (.txt)", "txt"),
    Html("Página web (.html)", "html"),
    Docx("Word (.docx)", "docx"),
}

/** Capítulo a exportar: parágrafos na marcação do manuscrito e o texto das notas de rodapé por id. */
data class ExportChapter(
    val title: String,
    val paragraphs: List<String>,
    val footnotes: Map<String, String> = emptyMap(),
) {
    /** Número de cada nota, na ordem em que a chamada aparece no capítulo (1, 2, 3…). */
    val footnoteNumbers: Map<String, Int> by lazy {
        footnoteIds(paragraphs.joinToString("\n")).withIndex().associate { (i, id) -> id to i + 1 }
    }
}

data class ExportDocument(
    val title: String,
    val author: String,
    val synopsis: String,
    val chapters: List<ExportChapter>,
    /** Imagens do livro pelo caminho usado no texto (`assets/…`). As que faltam saem só com a legenda. */
    val images: Map<String, ByteArray> = emptyMap(),
)

data class InlineSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val smallCaps: Boolean = false,
    val superscript: Boolean = false,
    val subscript: Boolean = false,
    /** Chamada de nota de rodapé (o texto do span fica vazio). */
    val footnote: String? = null,
)

fun buildExportDocument(manifest: BookManifest, chapters: List<Pair<ChapterMeta, String>>): ExportDocument =
    ExportDocument(
        title = manifest.title,
        author = manifest.author,
        synopsis = manifest.synopsis,
        chapters = chapters.map { (meta, body) -> ExportChapter(meta.title, splitParagraphs(body), meta.footnotes) },
    )

/** Caminhos de todas as imagens citadas no livro. */
fun imagePaths(doc: ExportDocument): Set<String> =
    doc.chapters.flatMap { it.paragraphs }.map(::parseLine).filter { it.kind == BlockKind.Image }.map { it.imagePath }.toSet()

/** Cada linha não vazia vira um parágrafo (o editor usa uma linha por parágrafo). */
fun splitParagraphs(body: String): List<String> =
    body.replace("\r\n", "\n").split('\n').map { it.trim() }.filter { it.isNotEmpty() }

/** Parágrafo que só tem asteriscos (`* * *`, `***`) ou `#`: a quebra de cena inserida pelo editor. */
fun isSceneBreak(paragraph: String): Boolean = isSceneBreakLine(paragraph)

/** Como a quebra de cena aparece nos arquivos exportados (centralizada no HTML e no DOCX). */
const val SCENE_BREAK_TEXT = "* * *"

/** Parágrafo classificado com seu conteúdo (sem prefixo de bloco nem marcadores de centralização). */
data class ExportBlock(val block: LineBlock, val content: String)

fun exportBlock(paragraph: String): ExportBlock {
    val block = parseLine(paragraph)
    val content = when (block.kind) {
        BlockKind.SceneBreak -> SCENE_BREAK_TEXT
        BlockKind.Image -> block.imageCaption
        else -> paragraph.substring(block.contentStart, block.contentEnd)
    }
    return ExportBlock(block, content)
}

/**
 * Trechos com formatação de um parágrafo (sem prefixo de bloco): negrito, itálico, versalete, sobrescrito,
 * subscrito e chamadas de nota. Marcadores somem; marcas aninhadas (itálico dentro de negrito) somam.
 */
fun parseInline(paragraph: String): List<InlineSpan> {
    val marks = findInlineMarks(paragraph)
    if (marks.isEmpty()) return if (paragraph.isEmpty()) emptyList() else listOf(InlineSpan(paragraph))
    val styled = marks.filter { it.kind != MarkKind.Footnote }
    val notes = marks.filter { it.kind == MarkKind.Footnote }.associateBy { it.start }
    val spans = ArrayList<InlineSpan>()
    val text = StringBuilder()
    var style = InlineSpan("")
    fun flush() {
        if (text.isNotEmpty()) spans += style.copy(text = text.toString())
        text.clear()
    }
    var i = 0
    while (i < paragraph.length) {
        val note = notes[i]
        if (note != null) {
            flush()
            spans += InlineSpan("", footnote = paragraph.substring(note.contentStart, note.contentEnd))
            i = note.end
            continue
        }
        if (styled.any { i in it.start until it.contentStart || i in it.contentEnd until it.end }) {
            i++
            continue
        }
        val active = styled.filter { i in it.contentStart until it.contentEnd }.map { it.kind }.toSet()
        val current = InlineSpan(
            "",
            bold = MarkKind.Bold in active,
            italic = MarkKind.Italic in active,
            smallCaps = MarkKind.SmallCaps in active,
            superscript = MarkKind.Superscript in active,
            subscript = MarkKind.Subscript in active,
        )
        if (current != style) {
            flush()
            style = current
        }
        text.append(paragraph[i])
        i++
    }
    flush()
    return spans
}

fun plainText(paragraph: String): String = parseInline(paragraph).joinToString("") { it.text }

// ---------------------------------------------------------------- Markdown

/**
 * O manuscrito já é Markdown: os títulos internos descem dois níveis (o livro usa `#` e os capítulos `##`),
 * a centralização vira parágrafo comum e as notas ganham ids únicos no documento, com o texto no fim do capítulo.
 */
fun exportMarkdown(doc: ExportDocument): String = buildString {
    append("# ").append(doc.title).append("\n\n")
    if (doc.author.isNotBlank()) append("*por ").append(doc.author).append("*\n\n")
    if (doc.synopsis.isNotBlank()) append("> ").append(doc.synopsis.replace("\n", "\n> ")).append("\n\n")
    doc.chapters.forEachIndexed { c, chapter ->
        append("## ").append(chapter.title).append("\n\n")
        fun notes(s: String) = s.replace(FOOTNOTE_REF) { m -> "[^c${c + 1}-${m.groupValues[1]}]" }
        var previous: BlockKind? = null
        for (p in chapter.paragraphs) {
            val (block, content) = exportBlock(p)
            // Itens de lista seguidos ficam juntos; os demais parágrafos são separados por linha em branco.
            if (previous == BlockKind.ListItem && block.kind == BlockKind.ListItem) setLength(length - 1)
            when (block.kind) {
                BlockKind.Heading1 -> append("### ").append(notes(content))
                BlockKind.Heading2 -> append("#### ").append(notes(content))
                BlockKind.Heading3 -> append("##### ").append(notes(content))
                BlockKind.Paragraph -> append(notes(content))
                else -> append(notes(p))
            }
            append("\n\n")
            previous = block.kind
        }
        val numbers = chapter.footnoteNumbers
        numbers.keys.forEach { id -> append("[^c${c + 1}-$id]: ").append(chapter.footnotes[id].orEmpty().replace("\n", " ")).append('\n') }
        if (numbers.isNotEmpty()) append('\n')
    }
}.trimEnd() + "\n"

private val FOOTNOTE_REF = Regex("\\[\\^([^\\]\\s]+)]")

// ---------------------------------------------------------------- Texto puro

fun exportPlainText(doc: ExportDocument): String = buildString {
    append(doc.title.uppercase()).append('\n')
    if (doc.author.isNotBlank()) append(doc.author).append('\n')
    append('\n')
    doc.chapters.forEachIndexed { i, chapter ->
        append("\n").append(chapter.title).append("\n")
        append("—".repeat(minOf(40, maxOf(3, chapter.title.length)))).append("\n\n")
        val numbers = chapter.footnoteNumbers
        fun inline(s: String) = parseInline(s).joinToString("") { if (it.footnote != null) "[${numbers[it.footnote]}]" else it.text }
        for (p in chapter.paragraphs) {
            val (block, content) = exportBlock(p)
            val line = when (block.kind) {
                BlockKind.SceneBreak -> SCENE_BREAK_TEXT
                BlockKind.Image -> "[Imagem: ${content.ifBlank { block.imagePath }}]"
                BlockKind.Heading1, BlockKind.Heading3 -> inline(content).uppercase()
                BlockKind.ListItem -> "• " + inline(content)
                BlockKind.Quote -> "    " + inline(content)
                else -> inline(content)
            }
            append(line).append("\n\n")
        }
        if (numbers.isNotEmpty()) {
            append("Notas\n\n")
            numbers.forEach { (id, n) -> append("[$n] ").append(chapter.footnotes[id].orEmpty()).append('\n') }
            append('\n')
        }
        if (i < doc.chapters.lastIndex) append("\n")
    }
}.trimEnd() + "\n"

// ---------------------------------------------------------------- HTML

private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

private fun imageMime(path: String) = when (path.substringAfterLast('.').lowercase()) {
    "jpg", "jpeg" -> "image/jpeg"
    "gif" -> "image/gif"
    "webp" -> "image/webp"
    else -> "image/png"
}

/** Página única e autossuficiente: imagens embutidas, notas no fim de cada capítulo com links de ida e volta. */
@OptIn(ExperimentalEncodingApi::class)
fun exportHtml(doc: ExportDocument): String = buildString {
    append("<!doctype html>\n<html lang=\"pt-BR\">\n<head>\n<meta charset=\"utf-8\">\n")
    append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
    append("<title>").append(esc(doc.title)).append("</title>\n")
    append(
        "<style>body{max-width:38em;margin:3em auto;padding:0 1em;font:1.1em/1.6 Georgia,serif;color:#222}" +
            "h1{text-align:center}h2{margin-top:3em;text-align:center}.autor{text-align:center;font-style:italic}" +
            "p{text-indent:1.5em;margin:0}p.cena{text-indent:0;text-align:center;margin:1em 0}" +
            "p.centro{text-indent:0;text-align:center}p.chamada{text-indent:0;margin-top:1.5em;font-size:.8em;letter-spacing:.15em;text-transform:uppercase}" +
            "h3,h4{margin:1.2em 0 .4em}blockquote{margin:.8em 2em;font-style:italic}blockquote p{text-indent:0}" +
            "ul{margin:.4em 0 .4em 1.5em}.versalete{font-variant:small-caps}figure{margin:1.5em 0;text-align:center}" +
            "figure img{max-width:100%}figcaption{font-size:.9em;font-style:italic}" +
            "ol.notas{font-size:.85em;margin-top:2em;border-top:1px solid #ccc;padding-top:.5em}sup a{text-decoration:none}</style>\n",
    )
    append("</head>\n<body>\n<h1>").append(esc(doc.title)).append("</h1>\n")
    if (doc.author.isNotBlank()) append("<p class=\"autor\">").append(esc(doc.author)).append("</p>\n")
    doc.chapters.forEachIndexed { c, chapter ->
        val numbers = chapter.footnoteNumbers
        fun inline(p: String) = parseInline(p).joinToString("") { span ->
            if (span.footnote != null) {
                val n = numbers[span.footnote]
                return@joinToString "<sup id=\"ref-${c + 1}-$n\"><a href=\"#nota-${c + 1}-$n\">$n</a></sup>"
            }
            var t = esc(span.text)
            if (span.smallCaps) t = "<span class=\"versalete\">$t</span>"
            if (span.superscript) t = "<sup>$t</sup>"
            if (span.subscript) t = "<sub>$t</sub>"
            if (span.italic) t = "<em>$t</em>"
            if (span.bold) t = "<strong>$t</strong>"
            t
        }
        append("<h2>").append(esc(chapter.title)).append("</h2>\n")
        var inList = false
        for (p in chapter.paragraphs) {
            val (block, content) = exportBlock(p)
            if (inList && block.kind != BlockKind.ListItem) {
                append("</ul>\n")
                inList = false
            }
            when (block.kind) {
                BlockKind.SceneBreak -> append("<p class=\"cena\">").append(SCENE_BREAK_TEXT).append("</p>\n")
                BlockKind.Heading1 -> append("<h3>").append(inline(content)).append("</h3>\n")
                BlockKind.Heading2 -> append("<h4>").append(inline(content)).append("</h4>\n")
                BlockKind.Heading3 -> append("<p class=\"chamada\">").append(inline(content)).append("</p>\n")
                BlockKind.Quote -> append("<blockquote><p>").append(inline(content)).append("</p></blockquote>\n")
                BlockKind.ListItem -> {
                    if (!inList) append("<ul>\n")
                    inList = true
                    append("<li>").append(inline(content)).append("</li>\n")
                }
                BlockKind.Image -> {
                    append("<figure>")
                    doc.images[block.imagePath]?.let { bytes ->
                        append("<img src=\"data:").append(imageMime(block.imagePath)).append(";base64,")
                            .append(Base64.encode(bytes)).append("\" alt=\"").append(esc(content)).append("\">")
                    }
                    if (content.isNotBlank()) append("<figcaption>").append(esc(content)).append("</figcaption>")
                    append("</figure>\n")
                }
                BlockKind.Paragraph -> {
                    append(if (block.centered) "<p class=\"centro\">" else "<p>").append(inline(content)).append("</p>\n")
                }
            }
        }
        if (inList) append("</ul>\n")
        if (numbers.isNotEmpty()) {
            append("<ol class=\"notas\">\n")
            numbers.forEach { (id, n) ->
                append("<li id=\"nota-${c + 1}-$n\">").append(esc(chapter.footnotes[id].orEmpty()))
                    .append(" <a href=\"#ref-${c + 1}-$n\">↩</a></li>\n")
            }
            append("</ol>\n")
        }
    }
    append("</body>\n</html>\n")
}
