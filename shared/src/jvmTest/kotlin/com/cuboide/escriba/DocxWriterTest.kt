package com.cuboide.escriba

import com.cuboide.escriba.core.export.ExportChapter
import com.cuboide.escriba.core.export.ExportDocument
import com.cuboide.escriba.platform.buildDocx
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import javax.imageio.ImageIO
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertTrue

class DocxWriterTest {
    private fun parts(bytes: ByteArray): Map<String, ByteArray> {
        val out = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                out[entry.name] = zip.readBytes()
            }
        }
        return out
    }

    private fun Map<String, ByteArray>.text(name: String) = getValue(name).decodeToString()

    private fun assertValidXml(parts: Map<String, ByteArray>) {
        val parser = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        parts.filterKeys { it.endsWith(".xml") || it.endsWith(".rels") }.values.forEach { parser.parse(ByteArrayInputStream(it)) }
    }

    @Test
    fun sceneBreakIsCenteredParagraphAndXmlIsValid() {
        val doc = ExportDocument("Livro", "Ana", "", listOf(ExportChapter("Um", listOf("Antes.", "* * *", "nome_de_arquivo"))))
        val parts = parts(buildDocx(doc))
        val body = parts.text("word/document.xml")
        assertTrue("""<w:pStyle w:val="SceneBreak"/></w:pPr><w:r><w:t xml:space="preserve">* * *</w:t>""" in body)
        assertTrue("nome_de_arquivo" in body)
        assertTrue("<w:i/>" !in body)
        assertTrue("""w:styleId="SceneBreak"""" in parts.text("word/styles.xml"))
        assertValidXml(parts)
    }

    @Test
    fun blocksMarksFootnotesAndImages() {
        val png = ByteArrayOutputStream().also { ImageIO.write(BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB), "png", it) }.toByteArray()
        val doc = ExportDocument(
            "Livro", "", "",
            listOf(
                ExportChapter(
                    "Um",
                    listOf("# Título", "### Chamada", "* item", "> citação", "->Fim<-", "Rei [Dom]{.smallcaps} x^2^ H~2~O[^a].", "![Mapa](assets/m.png)"),
                    footnotes = mapOf("a" to "Nota do rei."),
                ),
            ),
            images = mapOf("assets/m.png" to png),
        )
        val parts = parts(buildDocx(doc))
        val body = parts.text("word/document.xml")
        listOf("Heading2", "Kicker", "ListBullet", "Quote", "Centered", "Figure", "Caption").forEach { style ->
            assertTrue("""<w:pStyle w:val="$style"/>""" in body, "estilo $style")
            assertTrue("""w:styleId="$style"""" in parts.text("word/styles.xml"), "definição de $style")
        }
        assertTrue("<w:smallCaps/>" in body)
        assertTrue("""<w:vertAlign w:val="superscript"/>""" in body && """<w:vertAlign w:val="subscript"/>""" in body)
        assertTrue("""<w:footnoteReference w:id="1"/>""" in body)
        assertTrue("Nota do rei." in parts.text("word/footnotes.xml"))
        assertTrue("footnotes.xml" in parts.text("word/_rels/document.xml.rels"))
        // Imagem embutida com o tamanho em EMU (40 × 20 px).
        assertTrue("word/media/image1.png" in parts.keys)
        assertTrue("""<wp:extent cx="381000" cy="190500"/>""" in body)
        assertTrue("""Extension="png"""" in parts.text("[Content_Types].xml"))
        assertTrue("{.smallcaps}" !in body && "[^a]" !in body)
        assertValidXml(parts)
    }
}
