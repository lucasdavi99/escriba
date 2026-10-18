package com.cuboide.escriba.platform

import com.cuboide.escriba.core.export.ExportDocument
import com.cuboide.escriba.core.export.InlineSpan
import com.cuboide.escriba.core.export.SCENE_BREAK_TEXT
import com.cuboide.escriba.core.export.exportBlock
import com.cuboide.escriba.core.export.parseInline
import com.cuboide.escriba.core.text.BlockKind
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO

/**
 * Gera um .docx (Office Open XML) válido, sem dependências externas: estilos de título, lista, citação e
 * quebra de cena, formatação de caractere, imagens embutidas e notas de rodapé no pé da página.
 */
internal object DocxWriter {
    private class Image(val relId: String, val target: String, val widthEmu: Long, val heightEmu: Long)

    private class Footnote(val id: Int, val text: String)

    fun write(doc: ExportDocument): ByteArray {
        val images = LinkedHashMap<String, Image>()
        val imageBytes = LinkedHashMap<String, ByteArray>()
        val footnotes = ArrayList<Footnote>()
        val body = documentXml(doc, images, imageBytes, footnotes)

        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.put("[Content_Types].xml", contentTypes(imageBytes.keys))
            zip.put("_rels/.rels", ROOT_RELS)
            zip.put("word/_rels/document.xml.rels", documentRels(images.values))
            zip.put("word/styles.xml", STYLES)
            zip.put("word/footnotes.xml", footnotesXml(footnotes))
            zip.put("word/document.xml", body)
            imageBytes.forEach { (target, bytes) -> zip.putBytes("word/$target", bytes) }
        }
        return out.toByteArray()
    }

    private fun documentXml(
        doc: ExportDocument,
        images: MutableMap<String, Image>,
        imageBytes: MutableMap<String, ByteArray>,
        footnotes: MutableList<Footnote>,
    ): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<w:document xmlns:w="$W" xmlns:r="$R" xmlns:wp="$WP" xmlns:a="$A" xmlns:pic="$PIC"><w:body>""")
        append(paragraph("Title", doc.title))
        if (doc.author.isNotBlank()) append(paragraph("Subtitle", doc.author))
        if (doc.synopsis.isNotBlank()) append(paragraph("Normal", doc.synopsis))
        var drawingId = 1
        for (chapter in doc.chapters) {
            append("""<w:p><w:pPr><w:pStyle w:val="Heading1"/><w:pageBreakBefore/></w:pPr>""")
            append(run(chapter.title)).append("</w:p>")
            // Ids de nota são únicos no documento; o Word numera as notas em sequência.
            val noteIds = HashMap<String, Int>()
            fun spans(text: String) = buildString {
                for (span in parseInline(text)) {
                    val note = span.footnote
                    if (note != null) {
                        val id = noteIds.getOrPut(note) {
                            (footnotes.size + 1).also { footnotes += Footnote(it, chapter.footnotes[note].orEmpty()) }
                        }
                        append("""<w:r><w:rPr><w:rStyle w:val="FootnoteReference"/></w:rPr><w:footnoteReference w:id="$id"/></w:r>""")
                    } else {
                        append(run(span))
                    }
                }
            }
            for (p in chapter.paragraphs) {
                val (block, content) = exportBlock(p)
                when (block.kind) {
                    BlockKind.SceneBreak -> append(paragraph("SceneBreak", SCENE_BREAK_TEXT))
                    BlockKind.Image -> {
                        val bytes = doc.images[block.imagePath]
                        val image = bytes?.let { registerImage(block.imagePath, it, images, imageBytes) }
                        if (image != null) {
                            append("""<w:p><w:pPr><w:pStyle w:val="Figure"/></w:pPr><w:r>""")
                            append(drawing(image, drawingId++, content))
                            append("</w:r></w:p>")
                        }
                        if (content.isNotBlank()) append(paragraph("Caption", content))
                    }
                    else -> {
                        val style = when (block.kind) {
                            BlockKind.Heading1 -> "Heading2"
                            BlockKind.Heading2 -> "Heading3"
                            BlockKind.Heading3 -> "Kicker"
                            BlockKind.ListItem -> "ListBullet"
                            BlockKind.Quote -> "Quote"
                            else -> if (block.centered) "Centered" else "BodyText"
                        }
                        append("""<w:p><w:pPr><w:pStyle w:val="$style"/></w:pPr>""")
                        if (block.kind == BlockKind.ListItem) append(run("•\t"))
                        append(spans(content))
                        append("</w:p>")
                    }
                }
            }
        }
        append(
            """<w:sectPr><w:pgSz w:w="8391" w:h="11906"/>""" +
                """<w:pgMar w:top="1134" w:right="1021" w:bottom="1134" w:left="1247" w:header="567" w:footer="567" w:gutter="0"/></w:sectPr>""",
        )
        append("</w:body></w:document>")
    }

    /** Lê o tamanho da imagem e a limita à largura útil da página A5. Formatos que o Word não lê ficam de fora. */
    private fun registerImage(path: String, bytes: ByteArray, images: MutableMap<String, Image>, imageBytes: MutableMap<String, ByteArray>): Image? {
        images[path]?.let { return it }
        val ext = path.substringAfterLast('.').lowercase().let { if (it == "jpeg") "jpg" else it }
        if (ext !in setOf("png", "jpg", "gif")) return null
        val buffered = runCatching { ImageIO.read(ByteArrayInputStream(bytes)) }.getOrNull() ?: return null
        val maxWidth = (8391L - 1247 - 1021) * EMU_PER_TWIP
        var width = buffered.width * EMU_PER_PIXEL
        var height = buffered.height * EMU_PER_PIXEL
        if (width > maxWidth) {
            height = height * maxWidth / width
            width = maxWidth
        }
        val index = images.size + 1
        val image = Image("rIdImg$index", "media/image$index.$ext", width, height)
        images[path] = image
        imageBytes[image.target] = bytes
        return image
    }

    private fun drawing(image: Image, id: Int, caption: String): String {
        val name = escape(caption.ifBlank { "Imagem $id" })
        return """<w:drawing><wp:inline distT="0" distB="0" distL="0" distR="0"><wp:extent cx="${image.widthEmu}" cy="${image.heightEmu}"/>""" +
            """<wp:docPr id="$id" name="$name"/><a:graphic><a:graphicData uri="$PIC">""" +
            """<pic:pic><pic:nvPicPr><pic:cNvPr id="$id" name="$name"/><pic:cNvPicPr/></pic:nvPicPr>""" +
            """<pic:blipFill><a:blip r:embed="${image.relId}"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>""" +
            """<pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="${image.widthEmu}" cy="${image.heightEmu}"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr>""" +
            """</pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing>"""
    }

    private fun footnotesXml(notes: List<Footnote>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:footnotes xmlns:w="$W">""")
        append("""<w:footnote w:type="separator" w:id="-1"><w:p><w:r><w:separator/></w:r></w:p></w:footnote>""")
        append("""<w:footnote w:type="continuationSeparator" w:id="0"><w:p><w:r><w:continuationSeparator/></w:r></w:p></w:footnote>""")
        for (note in notes) {
            append("""<w:footnote w:id="${note.id}"><w:p><w:pPr><w:pStyle w:val="FootnoteText"/></w:pPr>""")
            append("""<w:r><w:rPr><w:rStyle w:val="FootnoteReference"/></w:rPr><w:footnoteRef/></w:r>""")
            append(run(" " + note.text)).append("</w:p></w:footnote>")
        }
        append("</w:footnotes>")
    }

    private fun paragraph(style: String, text: String) =
        """<w:p><w:pPr><w:pStyle w:val="$style"/></w:pPr>${run(text)}</w:p>"""

    private fun run(text: String) = run(InlineSpan(text))

    private fun run(span: InlineSpan): String {
        val props = buildString {
            if (span.bold) append("<w:b/>")
            if (span.italic) append("<w:i/>")
            if (span.smallCaps) append("<w:smallCaps/>")
            if (span.superscript) append("""<w:vertAlign w:val="superscript"/>""")
            if (span.subscript) append("""<w:vertAlign w:val="subscript"/>""")
        }
        val rPr = if (props.isEmpty()) "" else "<w:rPr>$props</w:rPr>"
        // Tabulação vira <w:tab/>; o resto vai como texto.
        val parts = span.text.split('\t')
        return parts.mapIndexed { i, part ->
            (if (i > 0) "<w:r>$rPr<w:tab/></w:r>" else "") +
                (if (part.isEmpty()) "" else """<w:r>$rPr<w:t xml:space="preserve">${escape(part)}</w:t></w:r>""")
        }.joinToString("")
    }

    private fun escape(s: String) = buildString(s.length) {
        for (c in s) when {
            c == '&' -> append("&amp;")
            c == '<' -> append("&lt;")
            c == '>' -> append("&gt;")
            c == '"' -> append("&quot;")
            c.code < 0x20 && c != '\t' -> Unit // caracteres de controle são inválidos em XML
            else -> append(c)
        }
    }

    private fun ZipOutputStream.put(name: String, content: String) = putBytes(name, content.toByteArray(Charsets.UTF_8))

    private fun ZipOutputStream.putBytes(name: String, bytes: ByteArray) {
        putNextEntry(ZipEntry(name))
        write(bytes)
        closeEntry()
    }

    private fun contentTypes(imageTargets: Collection<String>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        imageTargets.map { it.substringAfterLast('.') }.distinct().forEach { ext ->
            val mime = when (ext) { "jpg" -> "image/jpeg"; "gif" -> "image/gif"; else -> "image/png" }
            append("""<Default Extension="$ext" ContentType="$mime"/>""")
        }
        append("""<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>""")
        append("""<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>""")
        append("""<Override PartName="/word/footnotes.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footnotes+xml"/>""")
        append("</Types>")
    }

    private fun documentRels(images: Collection<Image>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        append("""<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
        append("""<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/footnotes" Target="footnotes.xml"/>""")
        images.forEach { append("""<Relationship Id="${it.relId}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="${it.target}"/>""") }
        append("</Relationships>")
    }

    private const val W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
    private const val R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val WP = "http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
    private const val A = "http://schemas.openxmlformats.org/drawingml/2006/main"
    private const val PIC = "http://schemas.openxmlformats.org/drawingml/2006/picture"
    private const val EMU_PER_TWIP = 635L
    private const val EMU_PER_PIXEL = 9525L

    private const val ROOT_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

    private const val STYLES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii="Georgia" w:hAnsi="Georgia" w:cs="Georgia"/><w:sz w:val="22"/><w:lang w:val="pt-BR"/></w:rPr></w:rPrDefault></w:docDefaults>
<w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/><w:pPr><w:spacing w:after="120" w:line="300" w:lineRule="auto"/></w:pPr></w:style>
<w:style w:type="paragraph" w:styleId="BodyText"><w:name w:val="Body Text"/><w:basedOn w:val="Normal"/><w:pPr><w:spacing w:after="0" w:line="324" w:lineRule="auto"/><w:ind w:firstLine="425"/><w:jc w:val="both"/></w:pPr></w:style>
<w:style w:type="paragraph" w:styleId="Centered"><w:name w:val="Centered"/><w:basedOn w:val="BodyText"/><w:pPr><w:ind w:firstLine="0"/><w:jc w:val="center"/></w:pPr></w:style>
<w:style w:type="paragraph" w:styleId="SceneBreak"><w:name w:val="Scene Break"/><w:basedOn w:val="Normal"/><w:next w:val="BodyText"/><w:pPr><w:spacing w:before="240" w:after="240"/><w:jc w:val="center"/></w:pPr></w:style>
<w:style w:type="paragraph" w:styleId="ListBullet"><w:name w:val="List Bullet"/><w:basedOn w:val="Normal"/><w:pPr><w:spacing w:after="60"/><w:tabs><w:tab w:val="left" w:pos="567"/></w:tabs><w:ind w:left="567" w:hanging="283"/></w:pPr></w:style>
<w:style w:type="paragraph" w:styleId="Quote"><w:name w:val="Quote"/><w:basedOn w:val="Normal"/><w:pPr><w:spacing w:before="120" w:after="120"/><w:ind w:left="720" w:right="720"/><w:jc w:val="both"/></w:pPr><w:rPr><w:i/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Figure"><w:name w:val="Figure"/><w:basedOn w:val="Normal"/><w:pPr><w:keepNext/><w:spacing w:before="240" w:after="60"/><w:jc w:val="center"/></w:pPr></w:style>
<w:style w:type="paragraph" w:styleId="Caption"><w:name w:val="caption"/><w:basedOn w:val="Normal"/><w:pPr><w:spacing w:after="240"/><w:jc w:val="center"/></w:pPr><w:rPr><w:i/><w:sz w:val="18"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Title"><w:name w:val="Title"/><w:basedOn w:val="Normal"/><w:pPr><w:spacing w:before="2400" w:after="240"/><w:jc w:val="center"/></w:pPr><w:rPr><w:sz w:val="52"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Subtitle"><w:name w:val="Subtitle"/><w:basedOn w:val="Normal"/><w:pPr><w:jc w:val="center"/></w:pPr><w:rPr><w:i/><w:sz w:val="28"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/><w:basedOn w:val="Normal"/><w:next w:val="BodyText"/><w:pPr><w:keepNext/><w:spacing w:before="1440" w:after="480"/><w:jc w:val="center"/><w:outlineLvl w:val="0"/></w:pPr><w:rPr><w:sz w:val="34"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Heading2"><w:name w:val="heading 2"/><w:basedOn w:val="Normal"/><w:next w:val="BodyText"/><w:pPr><w:keepNext/><w:spacing w:before="360" w:after="120"/><w:outlineLvl w:val="1"/></w:pPr><w:rPr><w:sz w:val="30"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Heading3"><w:name w:val="heading 3"/><w:basedOn w:val="Normal"/><w:next w:val="BodyText"/><w:pPr><w:keepNext/><w:spacing w:before="240" w:after="120"/><w:outlineLvl w:val="2"/></w:pPr><w:rPr><w:b/><w:sz w:val="24"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Kicker"><w:name w:val="Kicker"/><w:basedOn w:val="Normal"/><w:next w:val="Heading2"/><w:pPr><w:keepNext/><w:spacing w:before="360" w:after="0"/></w:pPr><w:rPr><w:caps/><w:spacing w:val="40"/><w:sz w:val="17"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="FootnoteText"><w:name w:val="footnote text"/><w:basedOn w:val="Normal"/><w:pPr><w:spacing w:after="0" w:line="240" w:lineRule="auto"/></w:pPr><w:rPr><w:sz w:val="18"/></w:rPr></w:style>
<w:style w:type="character" w:styleId="FootnoteReference"><w:name w:val="footnote reference"/><w:rPr><w:vertAlign w:val="superscript"/></w:rPr></w:style>
</w:styles>"""
}
