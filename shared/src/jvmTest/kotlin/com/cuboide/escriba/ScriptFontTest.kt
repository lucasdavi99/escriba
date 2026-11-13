package com.cuboide.escriba

import com.cuboide.escriba.core.language.LangStyle
import com.cuboide.escriba.core.language.Language
import com.cuboide.escriba.core.language.Script
import com.cuboide.escriba.core.language.fnv1a64
import com.cuboide.escriba.core.language.fontBytes
import org.apache.fontbox.ttf.TTFParser
import org.apache.pdfbox.io.RandomAccessReadBuffer
import java.awt.Font
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A fonte gerada abre no FontBox (que o PDF usa) e no Java (prévia e medição), com um glifo por som. */
class ScriptFontTest {
    @Test
    fun generatedFontIsValidEverywhere() {
        for (style in LangStyle.entries) {
            val script = Script(Language(fnv1a64("Livro de teste"), style))
            val bytes = script.fontBytes()
            val ttf = TTFParser().parse(RandomAccessReadBuffer(bytes))
            assertEquals(script.glyphs.size + 2, ttf.numberOfGlyphs, "$style")
            val cmap = ttf.unicodeCmapLookup!!
            script.glyphs.forEach { g -> assertTrue(cmap.getGlyphId(g.codepoint) > 1, "$style ${g.unit}") }
            assertEquals(1, cmap.getGlyphId(0x20))
            ttf.close()

            val awt = Font.createFont(Font.TRUETYPE_FONT, ByteArrayInputStream(bytes))
            script.glyphs.forEach { g -> assertTrue(awt.canDisplay(g.codepoint), "$style java ${g.unit}") }
            assertTrue(awt.getFamily().endsWith("Escrita"), awt.getFamily())

            // Ida e volta pela escrita.
            val word = Language(fnv1a64("Livro de teste"), style).protoWord("estrela")
            val written = script.transliterate(word)
            assertTrue(written.all { Script.isScriptChar(it.code) }, "$style $word -> $written")
            assertEquals(word, script.romanize(written))
        }
    }

    @Test
    fun lettersAreDistinct() {
        val script = Script(Language(fnv1a64("Livro de teste"), LangStyle.Pedra))
        val shapes = script.glyphs.map { it.strokes }
        assertEquals(shapes.size, shapes.toSet().size)
    }
}

/** Texto na escrita inventada sai no PDF com a fonte gerada embutida (e não como "?"). */
class ScriptPdfTest {
    @Test
    fun scriptTextIsEmbeddedInThePdf() {
        val script = Script(Language(fnv1a64("Livro de teste"), LangStyle.Pedra))
        val fontFile = java.io.File.createTempFile("escrita-teste", ".ttf").apply { writeBytes(script.fontBytes()); deleteOnExit() }
        com.cuboide.escriba.platform.registerScriptFont(fontFile.path)
        try {
            val written = script.transliterate(Language(fnv1a64("Livro de teste"), LangStyle.Pedra).protoWord("estrela"))
            val source = com.cuboide.escriba.core.layout.TypesetSource(
                title = "Teste", author = "",
                chapters = listOf(
                    com.cuboide.escriba.core.layout.TypesetChapter(
                        "c1", com.cuboide.escriba.core.model.ChapterKind.Capitulo, "Um", 1,
                        listOf("A inscrição dizia: $written, e ninguém soube ler."),
                    ),
                ),
            )
            val book = com.cuboide.escriba.core.layout.Typesetter(com.cuboide.escriba.core.layout.LayoutSettings(), com.cuboide.escriba.platform.layoutMeasurer()).typeset(source)
            val out = java.io.File(System.getProperty("java.io.tmpdir"), "escriba-escrita-test.pdf")
            com.cuboide.escriba.platform.writeLayoutPdf(book, out.path) { null }
            org.apache.pdfbox.Loader.loadPDF(out).use { doc ->
                val page = doc.getPage(book.pages.indexOfFirst { it.chapterId == "c1" })
                val fonts = page.resources.fontNames.map { page.resources.getFont(it) }
                assertTrue(fonts.any { "Escrita" in it.name }, "fontes: ${fonts.map { it.name }}")
                assertTrue(fonts.all { it.isEmbedded })
                val text = org.apache.pdfbox.text.PDFTextStripper().getText(doc)
                assertTrue(written in text, "texto do PDF: $text")
                System.getProperty("escriba.layoutPreviewDir")?.let { dir ->
                    javax.imageio.ImageIO.write(org.apache.pdfbox.rendering.PDFRenderer(doc).renderImageWithDPI(book.pages.indexOfFirst { it.chapterId == "c1" }, 140f), "png", java.io.File(dir, "pdf-escrita.png"))
                }
            }
        } finally {
            com.cuboide.escriba.platform.registerScriptFont(null)
        }
    }
}
