package com.cuboide.escriba

import com.cuboide.escriba.core.layout.BleedPreset
import com.cuboide.escriba.core.layout.LayoutSettings
import com.cuboide.escriba.core.layout.LayoutTheme
import com.cuboide.escriba.core.layout.Typesetter
import com.cuboide.escriba.core.layout.TypesetChapter
import com.cuboide.escriba.core.layout.TypesetSource
import com.cuboide.escriba.core.layout.mmToPt
import com.cuboide.escriba.core.model.ChapterKind
import com.cuboide.escriba.platform.layoutMeasurer
import com.cuboide.escriba.platform.writeLayoutPdf
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal val sampleSource = TypesetSource(
    title = "O Farol de Areia",
    author = "Marina Duarte",
    chapters = listOf(
        TypesetChapter("ded", ChapterKind.Dedicatoria, "Dedicatória", null, listOf("Para quem ainda conta os degraus.")),
        TypesetChapter(
            "c1", ChapterKind.Capitulo, "A areia no farol", 1,
            buildList {
                add("Naquela manhã o farol amanheceu coberto de areia. Ninguém na vila sabia explicar como o vento tinha subido tão alto, nem por que as gaivotas haviam desaparecido do cais.")
                add("Lia subiu os degraus contando cada um, como fazia o avô. Seu Bento dizia que eram cento e doze, mas naquele dia ela contou cento e treze.[^n1]")
                add("— Vô, tem um degrau a mais — gritou, a voz batendo nas paredes curvas da torre.")
                repeat(14) { add("Ninguém respondeu. Lá embaixo, o mar recuava em silêncio, deixando à mostra pedras que *ninguém* via havia gerações, cobertas de conchas e de uma ferrugem antiga que parecia **escrita**. A menina desceu correndo, tropeçou na areia fina e caiu de joelhos diante da primeira pedra, onde alguém tinha gravado uma data impossível.") }
                add("* * *")
                repeat(6) { add("A maré voltou ao anoitecer, mais devagar do que o costume, e trouxe consigo um cheiro de ferro e de chuva distante. Seu Bento, sentado na varanda, fingia não perceber a inquietação da neta.") }
            },
            mapOf("n1" to "Cento e doze era também o número de casas da vila em 1920."),
        ),
        TypesetChapter("c2", ChapterKind.Capitulo, "A cidade submersa", 2, List(10) { "O mar recuou trezentos metros durante a noite, e a vila inteira desceu à praia para ver o que a água escondia havia tanto tempo." }),
        TypesetChapter("epi", ChapterKind.Epilogo, "Epílogo", null, listOf("Anos depois, ainda contavam a história.")),
    ),
)

class LayoutPdfTest {
    @Test
    fun writesPdfWithEmbeddedFontsAndTrimBox() {
        val settings = LayoutTheme.Fantasia.apply(LayoutSettings()).let { it.copy(page = it.page.copy(bleed = BleedPreset.Nacional, cropMarks = true)) }
        val book = Typesetter(settings, layoutMeasurer()).typeset(sampleSource)
        val out = File(System.getProperty("java.io.tmpdir"), "escriba-layout-test.pdf")
        writeLayoutPdf(book, out.path) { null }
        Loader.loadPDF(out).use { doc ->
            assertEquals(book.pages.size, doc.numberOfPages)
            assertEquals(0, doc.numberOfPages % 2)
            val page = doc.getPage(book.pages.indexOfFirst { it.chapterId == "c1" })
            assertTrue(abs(page.trimBox.width - book.widthPt) < 0.5f)
            assertTrue(page.mediaBox.width > page.trimBox.width + 2 * mmToPt(5f))
            val fonts = page.resources.fontNames.map { page.resources.getFont(it) }
            assertTrue(fonts.isNotEmpty() && fonts.all { it.isEmbedded }, "fontes: ${fonts.map { it.name }}")
            assertEquals(4, doc.documentCatalog.documentOutline.children().count())
            // Rasteriza do próprio PDF para inspeção manual (opcional).
            System.getProperty("escriba.layoutPreviewDir")?.let { dir ->
                val renderer = PDFRenderer(doc)
                for (i in 0 until minOf(doc.numberOfPages, 14)) {
                    ImageIO.write(renderer.renderImageWithDPI(i, 110f), "png", File(dir, "pdf-p${i + 1}.png"))
                }
            }
        }
    }
}
