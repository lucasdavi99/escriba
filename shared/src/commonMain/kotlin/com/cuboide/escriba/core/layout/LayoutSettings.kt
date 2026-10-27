package com.cuboide.escriba.core.layout

import kotlinx.serialization.Serializable

const val PT_PER_MM = 72f / 25.4f

fun mmToPt(mm: Float) = mm * PT_PER_MM
fun ptToMm(pt: Float) = pt / PT_PER_MM

/** Formatos de miolo usados pelas gráficas brasileiras e pela impressão sob demanda (KDP). */
@Serializable
enum class TrimSize(val label: String, val widthMm: Float, val heightMm: Float) {
    Br14x21("14 × 21 cm (romance)", 140f, 210f),
    Br16x23("16 × 23 cm", 160f, 230f),
    Br155x23("15,5 × 23 cm", 155f, 230f),
    Bolso12x18("12 × 18 cm (bolso)", 120f, 180f),
    A5("A5 (14,8 × 21 cm)", 148f, 210f),
    In5x8("5 × 8 in (KDP)", 127f, 203.2f),
    In525x8("5,25 × 8 in (KDP)", 133.35f, 203.2f),
    In55x85("5,5 × 8,5 in (KDP)", 139.7f, 215.9f),
    In6x9("6 × 9 in (KDP)", 152.4f, 228.6f),
}

/**
 * Papel do miolo com a espessura aproximada **por página** (meia folha), usada para estimar a lombada.
 * Os valores do KDP seguem a fórmula publicada pela Amazon; os nacionais são médias e devem ser
 * confirmados com a gráfica.
 */
@Serializable
enum class PaperStock(val label: String, val mmPerPage: Float, val tint: Long) {
    Polen80("Pólen 80 g (creme)", 0.060f, 0xFFF6EEDCL),
    PolenBold90("Pólen Bold 90 g (creme, encorpado)", 0.072f, 0xFFF4EAD3L),
    Offset75("Offset 75 g (branco)", 0.045f, 0xFFFFFFFFL),
    Offset90("Offset 90 g (branco)", 0.052f, 0xFFFFFFFFL),
    KdpBranco("KDP branco", 0.0572f, 0xFFFFFFFFL),
    KdpCreme("KDP creme", 0.0635f, 0xFFF7EFDFL),
}

@Serializable
enum class BleedPreset(val label: String, val mm: Float) {
    Sem("Sem sangria", 0f),
    Kdp("KDP (3,2 mm)", 3.175f),
    Nacional("Gráfica nacional (5 mm)", 5f),
}

@Serializable
data class PageSetup(
    val trim: TrimSize = TrimSize.Br14x21,
    val paper: PaperStock = PaperStock.Polen80,
    val bleed: BleedPreset = BleedPreset.Sem,
    /** Marcas de corte fora da sangria (para a gráfica). */
    val cropMarks: Boolean = false,
    val marginTopMm: Float = 18f,
    val marginBottomMm: Float = 21f,
    /** Margem do lado da lombada (medianiz). */
    val marginInnerMm: Float = 20f,
    val marginOuterMm: Float = 15f,
)

@Serializable
enum class BodyAlign(val label: String) { Justificado("Justificado"), Esquerda("Esquerda") }

@Serializable
data class BodyTypography(
    /** Família da fonte do texto; vazio usa a do tema. */
    val fontFamily: String = "",
    val sizePt: Float = 11f,
    /** Entrelinha como múltiplo do corpo (1,35 = 11 pt sobre 14,85 pt). */
    val leading: Float = 1.38f,
    val align: BodyAlign = BodyAlign.Justificado,
    val indentEm: Float = 1.5f,
    /** Sem recuo no 1º parágrafo após título ou quebra de cena. */
    val firstParagraphNoIndent: Boolean = true,
    /** Espaço extra entre parágrafos, em linhas. */
    val paragraphSpacingLines: Float = 0f,
    val hyphenate: Boolean = true,
    /** Composição do parágrafo inteiro (Knuth-Plass): espaços mais parelhos que a quebra linha a linha. */
    val fineComposition: Boolean = true,
    /** Mínimo de linhas de um parágrafo no pé (órfã) e no topo (viúva) da página. */
    val widowOrphanLines: Int = 2,
)

@Serializable
enum class OpeningSide(val label: String) { Direita("Página direita"), Qualquer("Próxima página"), Esquerda("Página esquerda") }

@Serializable
enum class ChapterNumberStyle(val label: String) {
    Nenhum("Sem número"),
    Arabico("Capítulo 1"),
    Romano("Capítulo I"),
    Extenso("Capítulo um"),
    SoNumero("1"),
}

@Serializable
data class ChapterOpening(
    val side: OpeningSide = OpeningSide.Direita,
    /** Distância do topo da área de texto até o título. */
    val sinkMm: Float = 40f,
    val numberStyle: ChapterNumberStyle = ChapterNumberStyle.Arabico,
    /** Família do título; vazio usa a do texto. */
    val titleFont: String = "",
    val titleSizePt: Float = 20f,
    val titleUppercase: Boolean = false,
    val ornament: Boolean = true,
    /** Linhas da letra capitular (0 = sem capitular). */
    val dropCapLines: Int = 0,
)

@Serializable
enum class HeadContent(val label: String) { Nada("Nada"), Livro("Título do livro"), Autor("Autor"), Capitulo("Título do capítulo") }

@Serializable
enum class FolioPosition(val label: String) {
    RodapeCentro("Rodapé, centro"),
    RodapeExterno("Rodapé, lado externo"),
    TopoExterno("Cabeçalho, lado externo"),
    Nenhum("Sem número"),
}

@Serializable
data class RunningHeads(
    /** Página esquerda (par). */
    val verso: HeadContent = HeadContent.Autor,
    /** Página direita (ímpar). */
    val recto: HeadContent = HeadContent.Livro,
    val folio: FolioPosition = FolioPosition.RodapeCentro,
    val sizePt: Float = 8.5f,
    val uppercase: Boolean = true,
)

@Serializable
data class FrontMatter(
    val halfTitle: Boolean = true,
    val titlePage: Boolean = true,
    val credits: Boolean = true,
    val publisher: String = "",
    val isbn: String = "",
    val year: String = "",
    /** Texto livre da página de créditos (ficha catalográfica, revisão, capa…). */
    val creditsText: String = "",
    val toc: Boolean = true,
)

@Serializable
data class LayoutSettings(
    val theme: LayoutTheme = LayoutTheme.Classico,
    val page: PageSetup = PageSetup(),
    val body: BodyTypography = BodyTypography(),
    val opening: ChapterOpening = ChapterOpening(),
    val heads: RunningHeads = RunningHeads(),
    val front: FrontMatter = FrontMatter(),
    val sceneBreak: String = "*   *   *",
)

/** Estilos prontos de miolo. Cada um sugere fontes em ordem de preferência (a primeira instalada vence). */
@Serializable
enum class LayoutTheme(val label: String, val description: String, val bodyFonts: List<String>, val titleFonts: List<String>) {
    Classico(
        "Clássico", "Serifada tradicional, título centralizado com ornamento",
        listOf("Palatino Linotype", "Book Antiqua", "Georgia"), listOf("Palatino Linotype", "Georgia"),
    ),
    Contemporaneo(
        "Contemporâneo", "Texto arejado, título grande alinhado, sem ornamento",
        listOf("Georgia", "Cambria", "Constantia"), listOf("Segoe UI", "Calibri", "Arial"),
    ),
    Fantasia(
        "Fantasia", "Título em caixa alta, capitular de 3 linhas, abertura profunda",
        listOf("Book Antiqua", "Palatino Linotype", "Georgia"), listOf("Constantia", "Book Antiqua", "Georgia"),
    ),
    Bolso(
        "Bolso", "Formato 12 × 18, corpo menor e margens econômicas",
        listOf("Georgia", "Cambria"), listOf("Georgia", "Cambria"),
    ),
    Elegante(
        "Elegante", "Margens generosas, título em versal pequena, entrelinha ampla",
        listOf("Garamond", "Palatino Linotype", "Georgia"), listOf("Garamond", "Palatino Linotype", "Georgia"),
    ),
    ;

    /** Configurações do tema, preservando dados do livro (créditos, ISBN) de [base]. */
    fun apply(base: LayoutSettings): LayoutSettings {
        val keep = base.front
        return when (this) {
            Classico -> LayoutSettings(theme = this, front = keep)
            Contemporaneo -> LayoutSettings(
                theme = this, front = keep,
                body = BodyTypography(sizePt = 11f, leading = 1.45f, indentEm = 1.2f),
                opening = ChapterOpening(side = OpeningSide.Qualquer, sinkMm = 30f, numberStyle = ChapterNumberStyle.SoNumero, titleSizePt = 24f, ornament = false),
                heads = RunningHeads(verso = HeadContent.Livro, recto = HeadContent.Capitulo, folio = FolioPosition.RodapeExterno, uppercase = false),
                sceneBreak = "•",
            )
            Fantasia -> LayoutSettings(
                theme = this, front = keep,
                body = BodyTypography(sizePt = 11f, leading = 1.38f),
                opening = ChapterOpening(sinkMm = 50f, numberStyle = ChapterNumberStyle.Extenso, titleSizePt = 19f, titleUppercase = true, dropCapLines = 3),
                heads = RunningHeads(verso = HeadContent.Autor, recto = HeadContent.Capitulo),
                sceneBreak = "~   ✦   ~",
            )
            Bolso -> LayoutSettings(
                theme = this, front = keep,
                page = PageSetup(trim = TrimSize.Bolso12x18, paper = PaperStock.Offset75, marginTopMm = 14f, marginBottomMm = 17f, marginInnerMm = 16f, marginOuterMm = 11f),
                body = BodyTypography(sizePt = 10f, leading = 1.32f, indentEm = 1.3f),
                opening = ChapterOpening(side = OpeningSide.Qualquer, sinkMm = 25f, titleSizePt = 16f),
                heads = RunningHeads(sizePt = 7.5f),
            )
            Elegante -> LayoutSettings(
                theme = this, front = keep,
                page = PageSetup(marginTopMm = 22f, marginBottomMm = 26f, marginInnerMm = 22f, marginOuterMm = 18f),
                body = BodyTypography(sizePt = 11.5f, leading = 1.5f, indentEm = 1.8f),
                opening = ChapterOpening(sinkMm = 55f, numberStyle = ChapterNumberStyle.Romano, titleSizePt = 17f, titleUppercase = true),
                heads = RunningHeads(verso = HeadContent.Autor, recto = HeadContent.Capitulo, folio = FolioPosition.RodapeExterno),
            )
        }
    }
}
