package com.cuboide.escriba.core.layout

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// ---------------------------------------------------------------- Configuração

/** Sentido do texto na lombada. */
@Serializable
enum class SpineDirection(val label: String) {
    Descendente("De cima para baixo (Brasil, EUA, Reino Unido)"),
    Ascendente("De baixo para cima (França, Alemanha, Itália)"),
}

/** Fundo em degradê de duas cores: linear no ângulo dado (0° = da esquerda para a direita) ou radial (luz no centro). */
@Serializable
data class CoverGradient(val from: Long, val to: Long, val angleDeg: Float = 90f, val radial: Boolean = false)

/** Peças de texto da capa que podem ser arrastadas. */
@Serializable
enum class CoverPiece(val label: String) {
    Titulo("Título"),
    Autor("Autor"),
    LombadaTitulo("Lombada · título"),
    LombadaAutor("Lombada · autor"),
    Sinopse("Sinopse"),
    CodigoBarras("Código de barras"),
}

@Serializable
data class MmOffset(val x: Float = 0f, val y: Float = 0f) {
    operator fun plus(o: MmOffset) = MmOffset(x + o.x, y + o.y)
}

/** Imagem solta por cima da capa (logo da editora, selo): canto superior esquerdo e largura em mm, a partir do corte. */
@Serializable
data class CoverOverlay(val id: String, val path: String, val xMm: Float, val yMm: Float, val widthMm: Float)

/** Medida da lombada quando o PDF da capa foi gerado (para avisar se o miolo mudou depois). */
@Serializable
data class CoverExportRecord(val pages: Int, val spineMm: Float)

@Serializable
data class CoverSettings(
    /** Largura de cada orelha (0 = sem orelhas). */
    val flapMm: Float = 0f,
    val bleed: BleedPreset = BleedPreset.Nacional,
    /** Marcas de corte nos cantos e de dobra na lombada e nas orelhas. */
    val marks: Boolean = true,
    /** Arte de fundo (caminho relativo, em `assets/`), sobre a capa aberta inteira com a sangria. */
    val image: String = "",
    /** Tamanho da arte: 1 cobre tudo (sangra); abaixo de 1 sobra uma borda; acima amplia para enquadrar. */
    val artZoom: Float = 1f,
    /** Deslocamento do centro da arte em relação ao centro da folha, em mm. */
    val artOffsetXMm: Float = 0f,
    val artOffsetYMm: Float = 0f,
    /** Cor de fundo (ARGB) visível onde não há arte. */
    val background: Long = 0xFF2E3B4EL,
    /** Degradê no lugar da cor de fundo (`null` = cor lisa). */
    val gradient: CoverGradient? = null,
    /** Cor dos textos (ARGB). */
    val ink: Long = 0xFFFFFFFFL,
    /** Título e autor da capa; vazio usa os do livro. */
    val title: String = "",
    val author: String = "",
    /** Família do título; vazio usa a dos títulos do miolo. */
    val titleFont: String = "",
    val titleSizePt: Float = 34f,
    val titleUppercase: Boolean = false,
    /** Desligue quando a arte já traz o título (ou o autor) desenhado, para não sobrepor. A lombada tem chave própria. */
    val frontTitle: Boolean = true,
    val frontAuthor: Boolean = true,
    val spineText: Boolean = true,
    val spineDirection: SpineDirection = SpineDirection.Descendente,
    /** Texto da contracapa (sinopse); quebras de linha separam parágrafos. */
    val backText: String = "",
    val barcode: Boolean = true,
    /** Quanto cada peça foi arrastada a partir do lugar automático. */
    val offsets: Map<CoverPiece, MmOffset> = emptyMap(),
    val overlays: List<CoverOverlay> = emptyList(),
    val lastExport: CoverExportRecord? = null,
)

/** Cores prontas para fundo e texto da capa (ARGB). */
val COVER_BACKGROUNDS = listOf(0xFF2E3B4EL, 0xFF1C1A18L, 0xFF6B2D2DL, 0xFF2F4A3AL, 0xFFE9E2D3L, 0xFFFFFFFFL)
val COVER_INKS = listOf(0xFFFFFFFFL, 0xFF1C1A18L, 0xFFE8C66AL)

/** Degradês prontos (paleta própria do Escriba), com um nome que sugere o clima. */
val COVER_GRADIENTS = listOf(
    "Noite" to CoverGradient(0xFF0F1A2EL, 0xFF35507AL, 100f),
    "Brasa" to CoverGradient(0xFF3A0E0EL, 0xFFB5482AL, 120f),
    "Mata" to CoverGradient(0xFF0E2A1FL, 0xFF3F7A5CL, 90f),
    "Aurora" to CoverGradient(0xFF2B1846L, 0xFFC0607EL, 135f),
    "Areia" to CoverGradient(0xFFEFE3C8L, 0xFFC79A5BL, 90f),
    "Farol" to CoverGradient(0xFFF4E7B5L, 0xFF1D3557L, 0f, radial = true),
)

/** Lombada abaixo disso não leva texto (as gráficas não garantem o registro). */
const val MIN_SPINE_TEXT_MM = 6f
/** Resolução mínima da arte no tamanho final de impressão. */
const val MIN_COVER_PPI = 300f
/** Distância segura das dobras e do corte para textos. */
const val COVER_SAFE_MM = 10f
/** Contraste mínimo entre texto e fundo (razão WCAG para texto grande). */
const val MIN_COVER_CONTRAST = 3f

// ---------------------------------------------------------------- Geometria

enum class CoverPanel(val label: String) { OrelhaVerso("Orelha da contracapa"), Contracapa("Contracapa"), Lombada("Lombada"), Capa("Capa"), OrelhaCapa("Orelha da capa") }

data class CoverZone(val panel: CoverPanel, val xMm: Float, val widthMm: Float) {
    val endMm get() = xMm + widthMm
}

/**
 * Capa aberta, da esquerda para a direita: orelha, contracapa, lombada, capa, orelha.
 * Medidas em mm a partir do canto superior esquerdo do formato final (sem sangria).
 */
data class CoverGeometry(
    val trimWidthMm: Float,
    val trimHeightMm: Float,
    val spineMm: Float,
    val flapMm: Float = 0f,
    val bleedMm: Float = 0f,
    /** Área fora da sangria reservada às marcas (0 = sem marcas). */
    val slugMm: Float = 0f,
) {
    val widthMm get() = 2 * trimWidthMm + spineMm + 2 * flapMm
    val heightMm get() = trimHeightMm

    val zones: List<CoverZone>
        get() = buildList {
            var x = 0f
            fun add(p: CoverPanel, w: Float) { add(CoverZone(p, x, w)); x += w }
            if (flapMm > 0f) add(CoverPanel.OrelhaVerso, flapMm)
            add(CoverPanel.Contracapa, trimWidthMm)
            add(CoverPanel.Lombada, spineMm)
            add(CoverPanel.Capa, trimWidthMm)
            if (flapMm > 0f) add(CoverPanel.OrelhaCapa, flapMm)
        }

    fun zone(panel: CoverPanel): CoverZone? = zones.firstOrNull { it.panel == panel }

    /** Posições das dobras (entre painéis). */
    val foldsMm: List<Float> get() = zones.dropLast(1).map { it.endMm }

    val widthPt get() = mmToPt(widthMm)
    val heightPt get() = mmToPt(heightMm)
    val bleedPt get() = mmToPt(bleedMm)
    val slugPt get() = mmToPt(slugMm)
}

fun coverGeometry(book: LaidOutBook, cover: CoverSettings): CoverGeometry = CoverGeometry(
    trimWidthMm = book.settings.page.trim.widthMm,
    trimHeightMm = book.settings.page.trim.heightMm,
    spineMm = book.spineMm,
    flapMm = cover.flapMm.coerceAtLeast(0f),
    bleedMm = cover.bleed.mm,
    slugMm = if (cover.marks) 10f else 0f,
)

// ---------------------------------------------------------------- Saída

/** Elementos da capa em pontos, a partir do canto superior esquerdo do formato final; podem cair na sangria. */
sealed interface CoverItem

/** Giro do texto: na lombada ele desce (pé das letras para a contracapa) ou sobe (pé para a capa). */
enum class TextTurn { Nenhum, Desce, Sobe }

/** Texto com a linha de base em [y]. Girado, [x] é a linha de base e [y] o começo do texto. */
data class CoverText(
    val x: Float, val y: Float, val text: String, val font: FontSpec, val sizePt: Float, val color: Long,
    val turn: TextTurn = TextTurn.Nenhum, val trackingEm: Float = 0f, val piece: CoverPiece? = null,
) : CoverItem {
    val vertical get() = turn != TextTurn.Nenhum
}

data class CoverRect(val x: Float, val y: Float, val width: Float, val height: Float, val color: Long, val piece: CoverPiece? = null) : CoverItem

data class CoverLine(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val widthPt: Float = 0.25f)

/** Retângulo em pontos (mesma origem dos itens). */
data class PtRect(val x: Float, val y: Float, val width: Float, val height: Float) {
    val right get() = x + width
    val bottom get() = y + height
    fun intersects(o: PtRect) = x < o.right && o.x < right && y < o.bottom && o.y < bottom
    fun inside(o: PtRect) = x >= o.x - 0.01f && y >= o.y - 0.01f && right <= o.right + 0.01f && bottom <= o.bottom + 0.01f
    fun union(o: PtRect): PtRect {
        val nx = min(x, o.x)
        val ny = min(y, o.y)
        return PtRect(nx, ny, max(right, o.right) - nx, max(bottom, o.bottom) - ny)
    }
}

/**
 * Imagem desenhada em [x], [y], [width], [height]. A arte de fundo ([clip]) é recortada na área com sangria;
 * as imagens soltas ([overlayId]) não.
 */
data class CoverImage(
    val path: String, val x: Float, val y: Float, val width: Float, val height: Float, val ppi: Float,
    val clip: Boolean = true, val overlayId: String? = null,
)

data class CoverIssue(val message: String, val blocking: Boolean)

data class CoverLayout(
    val geometry: CoverGeometry,
    val background: Long,
    val image: CoverImage?,
    val items: List<CoverItem>,
    /** Marcas de corte e dobra, fora da sangria (só no PDF). */
    val marks: List<CoverLine>,
    val issues: List<CoverIssue>,
    val title: String,
    val author: String,
    val gradient: CoverGradient? = null,
    /** Imagens soltas por cima da arte (logo, selo). */
    val overlays: List<CoverImage> = emptyList(),
    /** Caixa de cada peça de texto, para arrastar na tela e conferir. */
    val pieces: Map<CoverPiece, PtRect> = emptyMap(),
) {
    val canExport get() = issues.none { it.blocking }
}

// ---------------------------------------------------------------- Montagem

/** Formatos da impressão sob demanda (KDP), que não imprime orelhas. */
internal val POD_TRIMS = setOf(TrimSize.In5x8, TrimSize.In525x8, TrimSize.In55x85, TrimSize.In6x9)

/**
 * Monta a capa aberta para o miolo [book]. [imagePx] é o tamanho em pixels da arte de fundo
 * (`null` se não houver ou não puder ser lida); [overlayPx] o das imagens soltas.
 */
fun layoutCover(
    book: LaidOutBook,
    cover: CoverSettings,
    isbn: String,
    measurer: GlyphMeasurer,
    imagePx: Pair<Int, Int>?,
    overlayPx: (String) -> Pair<Int, Int>? = { null },
): CoverLayout {
    val g = coverGeometry(book, cover)
    val issues = ArrayList<CoverIssue>()
    val items = ArrayList<CoverItem>()
    val bounds = HashMap<CoverPiece, PtRect>()
    val safe = mmToPt(COVER_SAFE_MM)
    val title = cover.title.ifBlank { book.title }.trim()
    val author = cover.author.ifBlank { book.author }.trim()
    val titleFamily = cover.titleFont.takeIf { it.isNotBlank() && measurer.hasFamily(it) } ?: book.titleFamily
    val bodyFamily = book.bodyFamily
    fun zoneX(p: CoverPanel) = mmToPt(g.zone(p)!!.xMm)
    val panelW = mmToPt(g.trimWidthMm)
    val h = g.heightPt

    fun boxOf(t: CoverText): PtRect {
        val w = measurer.width(t.text, t.font, t.sizePt, t.trackingEm)
        val s = t.sizePt
        return when (t.turn) {
            TextTurn.Nenhum -> PtRect(t.x, t.y - s * 0.8f, w, s)
            TextTurn.Desce -> PtRect(t.x - s * 0.2f, t.y, s, w)
            TextTurn.Sobe -> PtRect(t.x - s * 0.8f, t.y - w, s, w)
        }
    }

    /** Acrescenta os itens de uma peça já deslocados pelo arraste, e guarda a caixa dela. */
    fun addPiece(piece: CoverPiece, pieceItems: List<CoverItem>, lockX: Boolean = false) {
        if (pieceItems.isEmpty()) return
        val off = cover.offsets[piece] ?: MmOffset()
        val dx = if (lockX) 0f else mmToPt(off.x)
        val dy = mmToPt(off.y)
        var box: PtRect? = null
        for (item in pieceItems) {
            val moved = item.offset(dx, dy)
            items += moved
            val b = when (moved) {
                is CoverText -> boxOf(moved)
                is CoverRect -> PtRect(moved.x, moved.y, moved.width, moved.height)
            }
            box = box?.union(b) ?: b
        }
        bounds[piece] = box!!
    }

    // Conferência do miolo.
    if (book.pages.size % 2 == 1) issues += CoverIssue("O miolo tem ${book.pages.size} páginas: um livro impresso fecha em número par.", true)
    cover.lastExport?.let { last ->
        if (abs(last.spineMm - g.spineMm) > 0.25f) {
            issues += CoverIssue(
                "O PDF da capa já gerado tem lombada de ${formatMm(last.spineMm)} mm (${last.pages} páginas); o miolo agora pede ${formatMm(g.spineMm)} mm (${book.pages.size} páginas). Gere a capa de novo.",
                false,
            )
        }
    }
    if (cover.flapMm > 0f && book.settings.page.trim in POD_TRIMS) {
        issues += CoverIssue("Orelhas num formato de impressão sob demanda: a KDP não imprime orelha.", false)
    }

    // Arte de fundo, preenchendo a capa aberta com a sangria (ou menor, deixando borda).
    val areaW = g.widthPt + 2 * g.bleedPt
    val areaH = h + 2 * g.bleedPt
    var image: CoverImage? = null
    if (cover.image.isBlank()) {
        if (cover.gradient == null) issues += CoverIssue("Sem arte de capa: importe uma imagem de fundo ou escolha um degradê.", true)
    } else if (imagePx == null || imagePx.first <= 0 || imagePx.second <= 0) {
        issues += CoverIssue("A arte de capa não foi encontrada ou não pôde ser lida.", true)
    } else {
        val zoom = cover.artZoom.coerceIn(0.2f, 4f)
        val scale = max(areaW / imagePx.first, areaH / imagePx.second) * zoom // pt por pixel
        val w = imagePx.first * scale
        val ih = imagePx.second * scale
        // Acima de 100% a arte não pode deixar vão; abaixo, não pode sair da folha.
        val slackX = abs(w - areaW) / 2
        val slackY = abs(ih - areaH) / 2
        val dx = mmToPt(cover.artOffsetXMm).coerceIn(-slackX, slackX)
        val dy = mmToPt(cover.artOffsetYMm).coerceIn(-slackY, slackY)
        val ppi = 72f / scale
        image = CoverImage(cover.image, -g.bleedPt + (areaW - w) / 2 + dx, -g.bleedPt + (areaH - ih) / 2 + dy, w, ih, ppi)
        if (ppi < MIN_COVER_PPI) {
            val needW = (areaW / 72f * MIN_COVER_PPI / zoom).toInt()
            val needH = (areaH / 72f * MIN_COVER_PPI / zoom).toInt()
            issues += CoverIssue("A arte fica com ${ppi.toInt()} ppi no tamanho final (o mínimo para impressão é ${MIN_COVER_PPI.toInt()}); use uma imagem de pelo menos $needW × $needH pixels.", false)
        }
        if (w < areaW - 0.5f || ih < areaH - 0.5f) {
            val border = min(areaW - w, areaH - ih).coerceAtLeast(0f) / 2 - g.bleedPt
            issues += CoverIssue(
                if (border > 0f) "A arte não chega ao corte: sobra uma borda de cerca de ${formatMm(ptToMm(border))} mm em volta (de propósito? volte a 100% para sangrar)."
                else "A arte não cobre toda a sangria: a guilhotina pode deixar um fio de papel na borda.",
                false,
            )
        }
    }

    // Imagens soltas (logo, selo).
    val overlays = cover.overlays.mapNotNull { o ->
        val px = overlayPx(o.path)
        if (px == null || px.first <= 0) {
            issues += CoverIssue("Uma imagem solta (${o.path.substringAfterLast('/')}) não foi encontrada.", false)
            return@mapNotNull null
        }
        val w = mmToPt(o.widthMm)
        val ih = w * px.second / px.first
        val ppi = px.first / (o.widthMm / 25.4f)
        if (ppi < MIN_COVER_PPI) issues += CoverIssue("Imagem solta com ${ppi.toInt()} ppi no tamanho usado (o bom é 300): diminua a imagem ou use uma maior.", false)
        CoverImage(o.path, mmToPt(o.xMm), mmToPt(o.yMm), w, ih, ppi, clip = false, overlayId = o.id)
    }

    // Capa: título no terço superior, autor perto do pé.
    run {
        val x0 = zoneX(CoverPanel.Capa)
        val maxW = panelW - 2 * safe
        val shown = if (cover.titleUppercase) title.uppercase() else title
        val font = FontSpec(titleFamily, bold = true)
        var size = cover.titleSizePt.coerceIn(10f, 120f)
        var lines = wrapText(shown, font, size, maxW, measurer)
        while (size > 12f && lines.any { measurer.width(it, font, size) > maxW }) {
            size *= 0.92f
            lines = wrapText(shown, font, size, maxW, measurer)
        }
        val leading = size * 1.12f
        var y = h * 0.24f + size
        if (cover.frontTitle) {
            addPiece(CoverPiece.Titulo, lines.map { line ->
                CoverText(x0 + (panelW - measurer.width(line, font, size)) / 2, y, line, font, size, cover.ink, piece = CoverPiece.Titulo).also { y += leading }
            })
        }
        if (cover.frontAuthor && author.isNotEmpty()) {
            val aFont = FontSpec(titleFamily)
            val aSize = (size * 0.48f).coerceIn(10f, 22f)
            val aLines = wrapText(author, aFont, aSize, maxW, measurer)
            var ay = h - safe - mmToPt(8f) - (aLines.size - 1) * aSize * 1.2f
            addPiece(CoverPiece.Autor, aLines.map { line ->
                CoverText(x0 + (panelW - measurer.width(line, aFont, aSize)) / 2, ay, line, aFont, aSize, cover.ink, trackingEm = 0.04f, piece = CoverPiece.Autor)
                    .also { ay += aSize * 1.2f }
            })
        }
    }

    // Lombada: título e autor girados, no sentido escolhido.
    if (cover.spineText) {
        if (g.spineMm < MIN_SPINE_TEXT_MM) {
            issues += CoverIssue("Lombada de ${formatMm(g.spineMm)} mm: estreita demais para texto (mínimo ${MIN_SPINE_TEXT_MM.toInt()} mm); sai sem título.", false)
        } else {
            val spineW = mmToPt(g.spineMm)
            val cx = zoneX(CoverPanel.Lombada) + spineW / 2
            val inset = mmToPt(12f)
            val avail = h - 2 * inset
            val tFont = FontSpec(titleFamily, bold = true)
            val aFont = FontSpec(titleFamily)
            val gap = mmToPt(10f)
            var size = min(spineW * 0.5f, 16f)
            fun total(s: Float) = measurer.width(title, tFont, s) + if (author.isEmpty()) 0f else gap + measurer.width(author, aFont, s * 0.85f)
            while (size > 5f && total(size) > avail) size *= 0.92f
            if (total(size) > avail) {
                issues += CoverIssue("O título não cabe na lombada.", false)
            } else {
                val aSize = size * 0.85f
                val aW = measurer.width(author, aFont, aSize)
                val tW = measurer.width(title, tFont, size)
                if (cover.spineDirection == SpineDirection.Descendente) {
                    // Desce: a linha de base fica à esquerda do centro e as letras crescem para a capa.
                    addPiece(CoverPiece.LombadaTitulo, listOf(CoverText(cx - size * 0.35f, inset, title, tFont, size, cover.ink, TextTurn.Desce, piece = CoverPiece.LombadaTitulo)), lockX = true)
                    if (author.isNotEmpty()) addPiece(CoverPiece.LombadaAutor, listOf(CoverText(cx - aSize * 0.35f, h - inset - aW, author, aFont, aSize, cover.ink, TextTurn.Desce, piece = CoverPiece.LombadaAutor)), lockX = true)
                } else {
                    // Sobe: lido de baixo para cima; o título fica no alto da lombada (onde termina a leitura dele).
                    addPiece(CoverPiece.LombadaTitulo, listOf(CoverText(cx + size * 0.35f, inset + tW, title, tFont, size, cover.ink, TextTurn.Sobe, piece = CoverPiece.LombadaTitulo)), lockX = true)
                    if (author.isNotEmpty()) addPiece(CoverPiece.LombadaAutor, listOf(CoverText(cx + aSize * 0.35f, h - inset, author, aFont, aSize, cover.ink, TextTurn.Sobe, piece = CoverPiece.LombadaAutor)), lockX = true)
                }
            }
        }
    }

    // Código de barras no canto inferior externo da contracapa.
    val backX = zoneX(CoverPanel.Contracapa)
    var barcodeTop = h - safe
    if (cover.barcode) {
        when (val check = checkIsbn(isbn)) {
            IsbnCheck.Empty -> issues += CoverIssue("Sem ISBN para o código de barras: preencha o ISBN ou desligue o código.", true)
            is IsbnCheck.Invalid -> issues += CoverIssue("ISBN inválido: ${check.reason}", true)
            is IsbnCheck.Valid -> {
                val box = barcodeItems(check.ean, isbn.trim(), bodyFamily, measurer)
                val bx = backX + panelW - safe - box.width
                val by = h - safe - box.height
                barcodeTop = by + mmToPt(cover.offsets[CoverPiece.CodigoBarras]?.y ?: 0f)
                addPiece(CoverPiece.CodigoBarras, box.items.map { it.offset(bx, by).withPiece(CoverPiece.CodigoBarras) })
            }
        }
    } else {
        (checkIsbn(isbn) as? IsbnCheck.Invalid)?.let { issues += CoverIssue("ISBN inválido: ${it.reason}", true) }
    }

    // Contracapa: sinopse alinhada à esquerda, a partir do alto (até onde o código de barras deixa, se não foi arrastada).
    val back = cover.backText.trim()
    if (back.isNotEmpty()) {
        val font = FontSpec(bodyFamily)
        val size = 10.5f
        val leading = size * 1.45f
        val maxW = panelW - 2 * safe
        val moved = cover.offsets[CoverPiece.Sinopse] != null
        val limit = if (moved) h - safe else barcodeTop - mmToPt(6f)
        var y = mmToPt(28f)
        var overflow = false
        val lines = ArrayList<CoverItem>()
        for (para in back.split('\n').map { it.trim() }) {
            if (para.isEmpty()) { y += leading * 0.6f; continue }
            for (line in wrapText(para, font, size, maxW, measurer)) {
                if (y > limit) { overflow = true; break }
                lines += CoverText(backX + safe, y, line, font, size, cover.ink, piece = CoverPiece.Sinopse)
                y += leading
            }
            if (overflow) break
            y += leading * 0.4f
        }
        addPiece(CoverPiece.Sinopse, lines)
        if (overflow) issues += CoverIssue("O texto da contracapa não cabe: encurte a sinopse.", false)
    }

    // Peças fora da área segura do painel delas, ou umas sobre as outras.
    val safeRects = mapOf(
        CoverPanel.Capa to g.zone(CoverPanel.Capa)!!,
        CoverPanel.Contracapa to g.zone(CoverPanel.Contracapa)!!,
        CoverPanel.Lombada to g.zone(CoverPanel.Lombada)!!,
    ).mapValues { (panel, z) ->
        if (panel == CoverPanel.Lombada) PtRect(mmToPt(z.xMm) + 1f, mmToPt(6f), mmToPt(z.widthMm) - 2f, h - mmToPt(12f))
        else PtRect(mmToPt(z.xMm) + safe * 0.5f, safe * 0.5f, mmToPt(z.widthMm) - safe, h - safe)
    }
    fun panelOf(p: CoverPiece) = when (p) {
        CoverPiece.Titulo, CoverPiece.Autor -> CoverPanel.Capa
        CoverPiece.LombadaTitulo, CoverPiece.LombadaAutor -> CoverPanel.Lombada
        CoverPiece.Sinopse, CoverPiece.CodigoBarras -> CoverPanel.Contracapa
    }
    for ((piece, box) in bounds) {
        if (!box.inside(safeRects.getValue(panelOf(piece)))) issues += CoverIssue("${piece.label}: está perto do corte ou da dobra. Traga para dentro da área segura.", false)
    }
    val placed = bounds.entries.sortedBy { it.key.ordinal }
    for (i in placed.indices) for (j in i + 1 until placed.size) {
        if (placed[i].value.intersects(placed[j].value)) {
            issues += CoverIssue("${placed[j].key.label}: está por cima de “${placed[i].key.label}”. Afaste um do outro.", false)
        }
    }

    // Contraste do texto com o fundo liso ou o degradê (com arte, a imagem decide: confira a prévia).
    if (image == null) {
        val backgrounds = cover.gradient?.let { listOf(it.from, it.to) } ?: listOf(cover.background)
        val worst = backgrounds.minOf { contrastRatio(cover.ink, it) }
        if (worst < MIN_COVER_CONTRAST && bounds.isNotEmpty()) {
            issues += CoverIssue("Pouco contraste entre o texto e o fundo (${formatRatio(worst)}:1): o texto pode sumir na impressão.", false)
        }
    }

    return CoverLayout(
        g, cover.background, image, items, if (cover.marks) coverMarks(g) else emptyList(), issues, title, author,
        gradient = cover.gradient, overlays = overlays, pieces = bounds,
    )
}

/** Razão de contraste WCAG entre duas cores ARGB (1 a 21). */
fun contrastRatio(a: Long, b: Long): Float {
    fun lum(c: Long): Float {
        fun ch(v: Long): Float {
            val s = v / 255f
            return if (s <= 0.03928f) s / 12.92f else ((s + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
        }
        return 0.2126f * ch((c shr 16) and 0xFF) + 0.7152f * ch((c shr 8) and 0xFF) + 0.0722f * ch(c and 0xFF)
    }
    val la = lum(a)
    val lb = lum(b)
    return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
}

private fun formatRatio(v: Float) = (kotlin.math.round(v * 10f) / 10f).toString().replace('.', ',')

private fun CoverItem.offset(dx: Float, dy: Float): CoverItem = when (this) {
    is CoverText -> copy(x = x + dx, y = y + dy)
    is CoverRect -> copy(x = x + dx, y = y + dy)
}

private fun CoverItem.withPiece(p: CoverPiece): CoverItem = when (this) {
    is CoverText -> copy(piece = p)
    is CoverRect -> copy(piece = p)
}

private class Box(val width: Float, val height: Float, val items: List<CoverItem>)

/**
 * Código EAN-13 com o ISBN legível em cima e os dígitos embaixo, sobre fundo branco.
 * Módulo de 0,264 mm (80% do nominal), comum em livros.
 */
private fun barcodeItems(ean: String, typed: String, family: String, measurer: GlyphMeasurer): Box {
    val m = mmToPt(0.33f * 0.8f)
    val pad = mmToPt(1.5f)
    val quietL = 11 * m
    val quietR = 7 * m
    val barH = mmToPt(18f)
    val guardExtra = 5 * m
    val labelSize = 8f
    val digitSize = 8.5f
    val font = FontSpec(family)
    val black = 0xFF000000L
    // Mantém os hífens digitados quando o número é o mesmo do código (ISBN-10 aparece já convertido).
    val typedClean = typed.removePrefix("ISBN").removePrefix("isbn").trim().removePrefix(":").trim()
    val labelText = "ISBN " + if (typedClean.filter(Char::isDigit) == ean) typedClean else ean
    val barsTop = pad + labelSize * 1.3f
    val labelW = measurer.width(labelText, font, labelSize)
    val codeW = quietL + EAN13_MODULES * m + quietR
    val width = max(codeW, labelW + 2 * pad)
    val height = barsTop + barH + digitSize + pad
    // Início das barras, com a zona de silêncio à esquerda (onde fica o 1º dígito).
    val x0 = (width - codeW) / 2 + quietL
    val out = ArrayList<CoverItem>()
    out += CoverRect(0f, 0f, width, height, 0xFFFFFFFFL)
    out += CoverText((width - labelW) / 2, pad + labelSize, labelText, font, labelSize, black)
    val modules = ean13Modules(ean)
    var i = 0
    while (i < EAN13_MODULES) {
        if (!modules[i]) { i++; continue }
        var j = i
        while (j + 1 < EAN13_MODULES && modules[j + 1] && ean13IsGuard(j + 1) == ean13IsGuard(i)) j++
        val long = ean13IsGuard(i)
        out += CoverRect(x0 + i * m, barsTop, (j - i + 1) * m, barH - (if (long) 0f else guardExtra), black)
        i = j + 1
    }
    val digitY = barsTop + barH - guardExtra + digitSize
    fun digit(d: Char, centerX: Float) {
        val s = d.toString()
        out += CoverText(centerX - measurer.width(s, font, digitSize) / 2, digitY, s, font, digitSize, black)
    }
    digit(ean[0], x0 - 4 * m)
    for (k in 0 until 6) digit(ean[1 + k], x0 + (3 + 7 * k + 3.5f) * m)
    for (k in 0 until 6) digit(ean[7 + k], x0 + (50 + 7 * k + 3.5f) * m)
    return Box(width, height, out)
}

/** Marcas de corte nos cantos e de dobra no alto e no pé, fora da sangria. */
private fun coverMarks(g: CoverGeometry): List<CoverLine> {
    val gap = g.bleedPt + 2f
    val len = g.slugPt - 3f
    if (len <= 0f) return emptyList()
    val w = g.widthPt
    val h = g.heightPt
    val out = ArrayList<CoverLine>()
    for (x in listOf(0f, w)) for (y in listOf(0f, h)) {
        val dx = if (x == 0f) -1 else 1
        val dy = if (y == 0f) -1 else 1
        out += CoverLine(x + dx * gap, y, x + dx * (gap + len), y)
        out += CoverLine(x, y + dy * gap, x, y + dy * (gap + len))
    }
    for (f in g.foldsMm.map { mmToPt(it) }) {
        out += CoverLine(f, -gap, f, -gap - len)
        out += CoverLine(f, h + gap, f, h + gap + len)
    }
    return out
}

// ---------------------------------------------------------------- E-book

/** Tamanho da capa de e-book exportada (só a frente). */
enum class EbookCoverSize(val label: String) {
    Livro("Proporção do livro impresso (1600 px de largura)"),
    Kindle("Kindle/KDP — 1600 × 2560 px"),
}

/** Pixels da capa de e-book para a frente de [g]. */
fun ebookCoverPixels(size: EbookCoverSize, g: CoverGeometry): Pair<Int, Int> = when (size) {
    EbookCoverSize.Livro -> 1600 to (1600f * g.trimHeightMm / g.trimWidthMm).toInt()
    EbookCoverSize.Kindle -> 1600 to 2560
}

/** Quebra gulosa por palavras; uma palavra mais larga que [maxWidth] fica sozinha na linha. */
internal fun wrapText(text: String, font: FontSpec, sizePt: Float, maxWidth: Float, measurer: GlyphMeasurer): List<String> {
    val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
    val lines = ArrayList<String>()
    var current = ""
    for (word in words) {
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (current.isNotEmpty() && measurer.width(candidate, font, sizePt) > maxWidth) {
            lines += current
            current = word
        } else current = candidate
    }
    if (current.isNotEmpty()) lines += current
    return lines
}

/** Medida com uma casa decimal e vírgula (12,5). */
internal fun formatMm(v: Float): String {
    val r = kotlin.math.round(v * 10f) / 10f
    return (if (r == r.toInt().toFloat()) r.toInt().toString() else r.toString()).replace('.', ',')
}
