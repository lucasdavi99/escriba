package com.cuboide.escriba

import com.cuboide.escriba.core.layout.BleedPreset
import com.cuboide.escriba.core.layout.CheckFix
import com.cuboide.escriba.core.layout.CheckLevel
import com.cuboide.escriba.core.layout.CheckLight
import com.cuboide.escriba.core.layout.LaidOutBook
import com.cuboide.escriba.core.layout.LaidPage
import com.cuboide.escriba.core.layout.LayoutSettings
import com.cuboide.escriba.core.layout.PageKind
import com.cuboide.escriba.core.layout.PageMargins
import com.cuboide.escriba.core.layout.TrimSize
import com.cuboide.escriba.core.layout.applyFix
import com.cuboide.escriba.core.layout.checkLayout
import com.cuboide.escriba.core.layout.light
import com.cuboide.escriba.core.layout.mmToPt
import com.cuboide.escriba.core.layout.printSpec
import com.cuboide.escriba.core.layout.recommendedInnerMm
import com.cuboide.escriba.core.layout.signaturePages
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun book(pages: Int, settings: LayoutSettings = LayoutSettings(), warnings: List<String> = emptyList()) = LaidOutBook(
    settings = settings,
    widthPt = mmToPt(settings.page.trim.widthMm),
    heightPt = mmToPt(settings.page.trim.heightMm),
    pages = List(pages) { LaidPage(it + 1, PageKind.Texto, null, emptyList()) },
    toc = emptyList(),
    warnings = warnings,
    bodyFamily = "Mono",
    titleFamily = "Mono",
    title = "O Farol de Areia",
    author = "Marina Duarte",
)

class LayoutCheckTest {
    @Test
    fun defaultBookIsGreenOnceTheSpineIsWideEnough() {
        val checks = checkLayout(book(200))
        assertEquals(CheckLight.Verde, checks.light(), checks.toString())
        // Livro fino: só o aviso da lombada, sem nada crítico.
        val thin = checkLayout(book(40))
        assertEquals(CheckLight.Amarelo, thin.light())
        assertTrue(thin.single().message.contains("Lombada"))
    }

    @Test
    fun gutterGrowsWithThePageCountAndTheFixWidensIt() {
        val base = LayoutSettings()
        val settings = base.copy(page = base.page.copy(marginInnerMm = 10f).ownMargins(7, PageMargins(18f, 21f, 10f, 15f)))
        val b = book(420, settings)
        val item = checkLayout(b).single { it.fix == CheckFix.AlargarMedianiz }
        assertEquals(CheckLevel.Aviso, item.level)
        val fixed = applyFix(settings, CheckFix.AlargarMedianiz, 420)
        assertEquals(recommendedInnerMm(420), fixed.page.marginInnerMm)
        assertEquals(recommendedInnerMm(420), fixed.page.marginsFor(7).innerMm, 0.5f)
        assertTrue(checkLayout(b.copy(settings = fixed)).none { it.fix == CheckFix.AlargarMedianiz })
    }

    @Test
    fun marginsTooCloseToTheCutAreCritical() {
        val base = LayoutSettings()
        val settings = base.copy(page = base.page.copy(marginOuterMm = 5f, bleed = BleedPreset.Nacional))
        val checks = checkLayout(book(200, settings))
        val item = checks.single { it.fix == CheckFix.AumentarMargens }
        assertEquals(CheckLevel.Critico, item.level)
        assertEquals(CheckLight.Vermelho, checks.light())
        val fixed = applyFix(settings, CheckFix.AumentarMargens, 200)
        assertTrue(checkLayout(book(200, fixed)).none { it.level == CheckLevel.Critico })
        // Mancha própria apertada aponta a página.
        val own = base.copy(page = base.page.ownMargins(9, PageMargins(4f, 21f, 20f, 15f)))
        assertEquals(9, checkLayout(book(200, own)).single { it.fix == CheckFix.AumentarMargens }.page)
    }

    @Test
    fun staleExceptionsMissingImagesAndPodLimits() {
        val base = LayoutSettings()
        val stale = base.copy(heads = base.heads.copy(hiddenFolios = setOf(3, 999)), page = base.page.ownMargins(500))
        val item = checkLayout(book(200, stale)).single { it.fix == CheckFix.LimparExcecoes }
        assertTrue("500" in item.message && "999" in item.message)
        val cleaned = applyFix(stale, CheckFix.LimparExcecoes, 200)
        assertEquals(setOf(3), cleaned.heads.hiddenFolios)
        assertTrue(cleaned.page.pageMargins.isEmpty())

        val missing = checkLayout(book(200, warnings = listOf("Imagem não encontrada: assets/x.png")))
        assertEquals(CheckLevel.Critico, missing.single().level)

        val kdp = base.copy(page = base.page.copy(trim = TrimSize.In6x9))
        assertTrue(checkLayout(book(12, kdp)).any { it.level == CheckLevel.Critico && "24" in it.message })
        assertFalse(checkLayout(book(12)).any { "24" in it.message })
    }

    @Test
    fun printSpecHasWhatThePrinterAsksFor() {
        val base = LayoutSettings()
        val settings = base.copy(cover = base.cover.copy(flapMm = 80f), front = base.front.copy(isbn = "978-85-0000-000-3"))
        val spec = printSpec(book(250, settings))
        assertEquals(256, signaturePages(250))
        assertTrue("140 × 210 mm" in spec, spec)
        assertTrue("250 páginas (em cadernos de 16: 256)" in spec, spec)
        assertTrue("Lombada: 15 mm" in spec, spec)
        assertTrue("Orelhas: 80 mm" in spec, spec)
        // Capa aberta: 2 × 140 + 15 + 2 × 80 = 455 mm, mais 5 mm de sangria de cada lado.
        assertTrue("Capa aberta (com sangria): 465 × 220 mm" in spec, spec)
        assertTrue("ISBN: 978-85-0000-000-3" in spec, spec)
    }
}
