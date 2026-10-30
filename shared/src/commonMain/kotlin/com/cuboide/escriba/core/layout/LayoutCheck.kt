package com.cuboide.escriba.core.layout

import kotlin.math.ceil

/** Gravidade de um ponto da conferência: [Critico] sai errado no impresso; [Aviso] é pendência para revisar. */
enum class CheckLevel { Critico, Aviso }

/** Conserto de um clique que a tela oferece ao lado do ponto. */
enum class CheckFix(val label: String) {
    AlargarMedianiz("Alargar a margem da lombada"),
    AumentarMargens("Aumentar as margens"),
    LimparExcecoes("Limpar as exceções"),
}

data class CheckItem(val level: CheckLevel, val message: String, val page: Int? = null, val fix: CheckFix? = null)

/** Resultado do semáforo: vermelho se houver crítico, amarelo se houver aviso, verde se nada. */
enum class CheckLight(val label: String) { Vermelho("tem coisa que sairia errada no impresso"), Amarelo("pendências para revisar"), Verde("pronto para gerar o PDF") }

fun List<CheckItem>.light(): CheckLight = when {
    any { it.level == CheckLevel.Critico } -> CheckLight.Vermelho
    isNotEmpty() -> CheckLight.Amarelo
    else -> CheckLight.Verde
}

/**
 * Medianiz mínima recomendada pelo número de páginas: livro grosso curva mais perto da lombada e "engole" o texto.
 * Faixas da orientação pública da impressão sob demanda (KDP) para a margem interna, arredondadas para cima
 * no passo de 0,5 mm das margens.
 */
fun recommendedInnerMm(pages: Int): Float = when {
    pages <= 150 -> 10f
    pages <= 300 -> 13f
    pages <= 500 -> 16f
    pages <= 700 -> 19.5f
    else -> 22.5f
}

/** Margem mínima nas bordas cortadas: com sangria a guilhotina pode variar mais, então pede-se mais respiro. */
fun minimumEdgeMm(withBleed: Boolean): Float = if (withBleed) 10f else 6.5f


/**
 * Conferência do miolo antes do PDF. Junta os avisos da diagramação (imagens, fontes) com regras de gráfica:
 * medianiz para a grossura do livro, margens nas bordas cortadas, limites de páginas e exceções esquecidas.
 */
fun checkLayout(book: LaidOutBook): List<CheckItem> {
    val out = ArrayList<CheckItem>()
    val page = book.settings.page
    val heads = book.settings.heads
    val n = book.pages.size

    // Avisos que o motor já produz (o texto deles é do próprio Escriba).
    for (w in book.warnings) {
        when {
            "não encontrada" in w -> out += CheckItem(CheckLevel.Critico, "$w — sai um espaço vazio no lugar.")
            "baixa resolução" in w -> out += CheckItem(CheckLevel.Aviso, "$w — no papel ela sai serrilhada.")
            "não está instalada" in w -> out += CheckItem(CheckLevel.Aviso, w)
            // A página em branco final é normal: livro impresso fecha em número par.
        }
    }
    if (n % 2 == 1) out += CheckItem(CheckLevel.Critico, "O miolo tem $n páginas: livro impresso fecha em número par.")

    val inner = recommendedInnerMm(n)
    val narrowInner = (listOf(page.globalMargins) + page.pageMargins.values).minOf { it.innerMm }
    if (narrowInner + 0.05f < inner) {
        out += CheckItem(
            CheckLevel.Aviso,
            "Margem da lombada de ${formatMm(narrowInner)} mm para $n páginas: o texto some na dobra. O recomendado é ao menos ${formatMm(inner)} mm.",
            fix = CheckFix.AlargarMedianiz,
        )
    }
    val minEdge = minimumEdgeMm(page.bleed != BleedPreset.Sem)
    val tight = (listOf(0 to page.globalMargins) + page.pageMargins.entries.map { it.key to it.value })
        .firstOrNull { (_, m) -> minOf(m.outerMm, m.topMm, m.bottomMm) + 0.05f < minEdge }
    if (tight != null) {
        val where = if (tight.first == 0) "As margens do livro" else "A mancha da página ${tight.first}"
        out += CheckItem(
            CheckLevel.Critico,
            "$where ficam a menos de ${formatMm(minEdge)} mm do corte: a guilhotina pode raspar o texto.",
            page = tight.first.takeIf { it > 0 },
            fix = CheckFix.AumentarMargens,
        )
    }
    // A impressão sob demanda da Amazon aceita de 24 a 828 páginas no miolo P&B.
    if (page.trim in POD_TRIMS) {
        if (n < 24) out += CheckItem(CheckLevel.Critico, "Com $n páginas o livro fica abaixo do mínimo da impressão sob demanda (24).")
        if (n > 828) out += CheckItem(CheckLevel.Critico, "Com $n páginas o livro passa do máximo da impressão sob demanda (828).")
    }
    if (book.spineMm < MIN_SPINE_TEXT_MM) {
        out += CheckItem(CheckLevel.Aviso, "Lombada de ${formatMm(book.spineMm)} mm: estreita demais para levar título na capa.")
    }
    book.settings.cover.lastExport?.let { last ->
        if (kotlin.math.abs(last.spineMm - book.spineMm) > 0.25f) {
            out += CheckItem(
                CheckLevel.Aviso,
                "A capa foi gerada para ${last.pages} páginas (lombada de ${formatMm(last.spineMm)} mm); o miolo agora tem $n (${formatMm(book.spineMm)} mm). Gere a capa de novo.",
            )
        }
    }
    // Exceções presas a páginas que não existem mais (o livro encolheu depois de ajustadas).
    val stale = (page.pageMargins.keys + heads.hiddenFolios + heads.hiddenHeads).filter { it > n }.toSortedSet()
    if (stale.isNotEmpty()) {
        out += CheckItem(CheckLevel.Aviso, "Há ajustes em páginas que não existem mais (${stale.joinToString(", ")}).", fix = CheckFix.LimparExcecoes)
    }
    return out
}

/** Aplica o conserto de um clique às configurações. */
fun applyFix(settings: LayoutSettings, fix: CheckFix, pages: Int): LayoutSettings {
    val page = settings.page
    return when (fix) {
        CheckFix.AlargarMedianiz -> {
            val inner = recommendedInnerMm(pages)
            settings.copy(
                page = page.copy(
                    marginInnerMm = maxOf(page.marginInnerMm, inner),
                    pageMargins = page.pageMargins.mapValues { (_, m) -> m.copy(innerMm = maxOf(m.innerMm, inner)).fitIn(page.trim) },
                ),
            )
        }
        CheckFix.AumentarMargens -> {
            val min = minimumEdgeMm(page.bleed != BleedPreset.Sem)
            fun PageMargins.raised() = copy(outerMm = maxOf(outerMm, min), topMm = maxOf(topMm, min), bottomMm = maxOf(bottomMm, min)).fitIn(page.trim)
            val g = page.globalMargins.raised()
            settings.copy(
                page = page.copy(
                    marginTopMm = g.topMm, marginBottomMm = g.bottomMm, marginOuterMm = g.outerMm,
                    pageMargins = page.pageMargins.mapValues { (_, m) -> m.raised() },
                ),
            )
        }
        CheckFix.LimparExcecoes -> settings.copy(
            page = page.copy(pageMargins = page.pageMargins.filterKeys { it <= pages }),
            heads = settings.heads.copy(
                hiddenFolios = settings.heads.hiddenFolios.filter { it <= pages }.toSet(),
                hiddenHeads = settings.heads.hiddenHeads.filter { it <= pages }.toSet(),
            ),
        )
    }
}

/** Páginas arredondadas para cadernos de 16, como a gráfica costuma imprimir. */
fun signaturePages(pages: Int, perSignature: Int = 16): Int = (ceil(pages / perSignature.toDouble()) * perSignature).toInt()

/** Ficha técnica para pedir orçamento à gráfica (texto para colar no e-mail ou na mensagem). */
fun printSpec(book: LaidOutBook): String {
    val s = book.settings
    val n = book.pages.size
    val cover = coverGeometry(book, s.cover)
    val coverW = cover.widthMm + 2 * cover.bleedMm
    val coverH = cover.heightMm + 2 * cover.bleedMm
    return buildString {
        appendLine("Ficha técnica — ${book.title.ifBlank { "livro" }}")
        appendLine("Formato fechado: ${formatMm(s.page.trim.widthMm)} × ${formatMm(s.page.trim.heightMm)} mm")
        val sig = signaturePages(n)
        appendLine("Miolo: $n páginas" + if (sig != n) " (em cadernos de 16: $sig)" else "")
        appendLine("Papel do miolo: ${s.page.paper.label}")
        appendLine("Impressão do miolo: preto (1 cor)")
        appendLine("Lombada: ${formatMm(book.spineMm)} mm ($n págs × ${s.page.paper.mmPerPage.toString().replace('.', ',')} mm)")
        appendLine("Sangria do miolo: " + if (s.page.bleed == BleedPreset.Sem) "sem sangria" else "${formatMm(s.page.bleed.mm)} mm")
        if (cover.flapMm > 0f) appendLine("Orelhas: ${formatMm(cover.flapMm)} mm cada")
        appendLine("Capa aberta (com sangria): ${formatMm(coverW)} × ${formatMm(coverH)} mm")
        if (s.front.isbn.isNotBlank()) appendLine("ISBN: ${s.front.isbn.trim()}")
        append("Arquivos: PDF do miolo e PDF da capa")
    }
}
