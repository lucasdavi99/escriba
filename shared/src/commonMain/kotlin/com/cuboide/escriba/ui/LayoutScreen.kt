package com.cuboide.escriba.ui

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.core.layout.BleedPreset
import com.cuboide.escriba.core.layout.BodyAlign
import com.cuboide.escriba.core.layout.COVER_BACKGROUNDS
import com.cuboide.escriba.core.layout.COVER_INKS
import com.cuboide.escriba.core.layout.ChapterNumberStyle
import com.cuboide.escriba.core.layout.CoverLayout
import com.cuboide.escriba.core.layout.FolioPosition
import com.cuboide.escriba.core.layout.HeadContent
import com.cuboide.escriba.core.layout.IsbnCheck
import com.cuboide.escriba.core.layout.LaidOutBook
import com.cuboide.escriba.core.layout.LaidPage
import com.cuboide.escriba.core.layout.LayoutTheme
import com.cuboide.escriba.core.layout.snapshot
import com.cuboide.escriba.core.layout.ptToMm
import com.cuboide.escriba.core.layout.SpineDirection
import com.cuboide.escriba.core.layout.MmOffset
import com.cuboide.escriba.core.layout.EbookCoverSize
import com.cuboide.escriba.core.layout.CoverGradient
import com.cuboide.escriba.core.layout.COVER_GRADIENTS
import com.cuboide.escriba.core.layout.printSpec
import com.cuboide.escriba.core.layout.light
import com.cuboide.escriba.core.layout.checkLayout
import com.cuboide.escriba.core.layout.CheckLight
import com.cuboide.escriba.core.layout.CheckLevel
import com.cuboide.escriba.core.layout.CheckItem
import com.cuboide.escriba.core.layout.OpeningFolio
import com.cuboide.escriba.core.layout.HeadPosition
import com.cuboide.escriba.core.layout.HeadInk
import com.cuboide.escriba.core.layout.FolioPages
import com.cuboide.escriba.core.layout.ChapterHead
import com.cuboide.escriba.core.layout.MarginEdge
import com.cuboide.escriba.core.layout.OpeningSide
import com.cuboide.escriba.core.layout.PageKind
import com.cuboide.escriba.core.layout.PaperStock
import com.cuboide.escriba.core.layout.TrimSize
import com.cuboide.escriba.core.layout.checkIsbn
import com.cuboide.escriba.platform.layoutFontFamilies
import com.cuboide.escriba.platform.decodeImageBitmap
import com.cuboide.escriba.platform.renderCover
import com.cuboide.escriba.platform.renderLayoutPage
import com.cuboide.escriba.platform.systemFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private fun fmt(v: Float, decimals: Int = 1): String {
    val factor = if (decimals == 0) 1f else if (decimals == 1) 10f else 100f
    val r = (v * factor).roundToInt() / factor
    val s = if (r == r.toInt().toFloat()) r.toInt().toString() else r.toString()
    return s.replace('.', ',')
}

/** Páginas abertas: a página 1 sozinha à direita, depois pares (2–3), (4–5)… */
private fun spreads(pages: List<LaidPage>): List<Pair<LaidPage?, LaidPage?>> {
    if (pages.isEmpty()) return emptyList()
    return listOf(null to pages.first()) + pages.drop(1).chunked(2).map { it[0] to it.getOrNull(1) }
}

private fun spreadIndexOf(pageNumber: Int) = pageNumber / 2

private enum class LayoutTab(val label: String) { Miolo("Miolo"), Capa("Capa") }

@Composable
fun LayoutScreen(c: EscribaController) {
    val book = c.book ?: return
    val laid = c.layout
    val s = c.layoutSettings
    // Zoom em dp por ponto tipográfico; null = ajustar à largura da área (padrão).
    var zoomSetting by remember { mutableStateOf<Float?>(null) }
    var lastFitZoom by remember { mutableStateOf(0.75f) }
    var guides by remember { mutableStateOf(true) }
    // Página clicada: o painel mostra a mancha dela (global ou própria).
    var selectedPage by remember(book.path) { mutableStateOf<Int?>(null) }
    var tab by remember { mutableStateOf(LayoutTab.Miolo) }
    var show3d by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // A capa depende do miolo (lombada) e das próprias configurações; espera a digitação assentar.
    val cover by produceState<CoverLayout?>(null, tab, laid, s.cover, s.front.isbn) {
        if (tab != LayoutTab.Capa || laid == null) return@produceState
        delay(120)
        value = withContext(Dispatchers.Default) { c.coverFor(laid, s) }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(start = Space.page, end = Space.xl, top = 36.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Diagramação", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.width(28.dp))
                    TabSwitch(tab) { tab = it }
                }
                Spacer(Modifier.height(4.dp))
                val summary = when {
                    laid == null -> "Diagramando “${book.manifest.title}”…"
                    tab == LayoutTab.Capa -> cover?.let { cv ->
                        val g = cv.geometry
                        buildString {
                            append("Capa aberta ${fmt(g.widthMm)} × ${fmt(g.heightMm)} mm · lombada ${fmt(g.spineMm)} mm (${laid.pages.size} págs)")
                            if (g.flapMm > 0f) append(" · orelhas ${fmt(g.flapMm)} mm")
                            if (g.bleedMm > 0f) append(" · sangria ${fmt(g.bleedMm)} mm")
                        }
                    } ?: "Montando a capa…"
                    else -> buildString {
                        append("${s.page.trim.label} · ${laid.pages.size} páginas · lombada ≈ ${fmt(laid.spineMm)} mm")
                        append(" · ${laid.bodyFamily} ${fmt(s.body.sizePt)} pt")
                        if (s.page.bleed != BleedPreset.Sem) append(" · sangria ${fmt(s.page.bleed.mm)} mm")
                    }
                }
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = LocalPalette.current.muted)
            }
            if (tab == LayoutTab.Capa) {
                IconAction(Icons.Outlined.ViewInAr, "Livro 3D: o livro pronto, nas medidas reais", onClick = { show3d = !show3d }, selected = show3d)
                IconAction(Icons.Outlined.GridOn, "Mostrar corte, dobras e área segura", onClick = { guides = !guides }, selected = guides)
                Spacer(Modifier.width(8.dp))
                EbookCoverButton(enabled = laid != null && !c.layoutBusy) { c.exportEbookCover(it) }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { c.exportCoverPdf() }, enabled = cover?.canExport == true && !c.layoutBusy) {
                    Icon(Icons.Outlined.PictureAsPdf, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Gerar PDF da capa")
                }
            } else {
                ThemePicker(s.theme) { c.applyLayoutTheme(it) }
                Spacer(Modifier.width(12.dp))
                val shownZoom = zoomSetting ?: lastFitZoom
                // Zoom e guias numa pílula só.
                FloatingPanel {
                    Row(Modifier.padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconAction(Icons.Outlined.ZoomOut, "Diminuir", onClick = { zoomSetting = (shownZoom - 0.1f).coerceAtLeast(0.3f) })
                        Text("${(shownZoom * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
                        IconAction(Icons.Outlined.ZoomIn, "Aumentar", onClick = { zoomSetting = (shownZoom + 0.1f).coerceAtMost(2.5f) })
                        IconAction(Icons.Outlined.FitScreen, "Ajustar à largura", onClick = { zoomSetting = null }, selected = zoomSetting == null)
                        Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(20.dp).background(LocalPalette.current.hairline))
                        IconAction(Icons.Outlined.GridOn, "Mostrar e ajustar as margens (arraste a mancha na página)", onClick = { guides = !guides }, selected = guides)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Button(onClick = { c.exportLayoutPdf() }, enabled = laid != null && !c.layoutBusy) {
                    Icon(Icons.Outlined.PictureAsPdf, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Gerar PDF")
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(3.dp)) {
            if (c.layoutBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        c.layoutGate?.let { LayoutGateDialog(c, it) }
        if (tab == LayoutTab.Capa) {
            CoverWorkspace(c, laid, cover, guides, show3d)
            return@Column
        }
        Row(Modifier.fillMaxSize()) {
            BookIndex(c, laid, Modifier.width(232.dp).fillMaxHeight()) { page ->
                scope.launch { listState.animateScrollToItem(spreadIndexOf(page)) }
            }
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            BoxWithConstraints(Modifier.weight(1f).fillMaxHeight().background(LocalPalette.current.desk)) {
                val rulers = if (guides) 2 * RULER_SIZE.value else 0f
                val fit = laid?.let { ((maxWidth.value - 64f - rulers) / (2 * it.widthPt + 2f)).coerceIn(0.3f, 2.5f) } ?: 0.75f
                lastFitZoom = fit
                val zoom = zoomSetting ?: fit
                val hScroll = rememberScrollState()
                when {
                    laid == null && c.layoutError != null -> EmptyState(Icons.Outlined.WarningAmber, "Não deu para diagramar", c.layoutError ?: "")
                    laid == null -> EmptyState(Icons.Outlined.PictureAsPdf, "Diagramando…", "Na primeira vez o Escriba lê as fontes instaladas; leva alguns segundos.")
                    else -> LazyColumn(
                        state = listState,
                        // Mais largo que a área quando o zoom passa da tela: rola na horizontal.
                        modifier = Modifier.fillMaxHeight().horizontalScroll(hScroll)
                            .width(maxOf(maxWidth, (laid.widthPt * 2 * zoom + 64 + rulers).dp)),
                        contentPadding = PaddingValues(28.dp),
                        verticalArrangement = Arrangement.spacedBy(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        itemsIndexed(spreads(laid.pages)) { _, (left, right) ->
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                PageSlot(c, laid, left, zoom, guides, rulerOnLeft = true, selectedPage) { selectedPage = it }
                                PageSlot(c, laid, right, zoom, guides, rulerOnLeft = false, selectedPage) { selectedPage = it }
                            }
                        }
                    }
                }
                if (laid != null) {
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(listState),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 4.dp),
                    )
                }
            }
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingsPanel(c, selectedPage?.takeIf { p -> laid != null && p <= laid.pages.size }, { selectedPage = null }, Modifier.width(340.dp).fillMaxHeight())
        }
    }
}

@Composable
private fun PageSlot(
    c: EscribaController,
    laid: LaidOutBook,
    page: LaidPage?,
    zoom: Float,
    guides: Boolean,
    rulerOnLeft: Boolean,
    selectedPage: Int?,
    onSelect: (Int) -> Unit,
) {
    val w = (laid.widthPt * zoom).dp
    val h = (laid.heightPt * zoom).dp
    val trim = laid.settings.page.trim
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (page == null) {
            Spacer(Modifier.size(w + if (guides) RULER_SIZE else 0.dp, h + if (guides) RULER_SIZE else 0.dp))
            Spacer(Modifier.height(20.dp))
            return@Column
        }
        val pageSetup = c.layoutSettings.page
        val margins = pageSetup.marginsFor(page.number)
        val own = pageSetup.hasOwnMargins(page.number)
        val left = if (page.recto) margins.innerMm else margins.outerMm
        val right = if (page.recto) margins.outerMm else margins.innerMm
        val rulerW = if (guides) RULER_SIZE else 0.dp
        if (guides) {
            Row {
                if (rulerOnLeft) Spacer(Modifier.width(rulerW))
                MmRuler(trim.widthMm, zoom, horizontal = true, marksMm = listOf(left, trim.widthMm - right))
                if (!rulerOnLeft) Spacer(Modifier.width(rulerW))
            }
        }
        Row {
            if (guides && rulerOnLeft) MmRuler(trim.heightMm, zoom, horizontal = false, marksMm = listOf(margins.topMm, trim.heightMm - margins.bottomMm))
            val density = LocalDensity.current.density
            // A prévia sai sem guias: a mancha é desenhada por cima, editável, sem refazer a imagem a cada arraste.
            val bitmap by produceState<ImageBitmap?>(null, laid, page, zoom) {
                value = withContext(Dispatchers.Default) { renderLayoutPage(laid, page, zoom * density, false, c::layoutImage) }
            }
            val paper = Color(laid.settings.page.paper.tint.toInt())
            val selected = selectedPage == page.number
            Box(
                Modifier.size(w, h).shadow(6.dp).background(paper)
                    .then(if (selected) Modifier.border(2.dp, LocalPalette.current.accent) else Modifier)
                    .clickable { onSelect(page.number) },
            ) {
                bitmap?.let { Image(it, contentDescription = "Página ${page.number}", modifier = Modifier.fillMaxSize()) }
                if (guides && page.kind != PageKind.Branca) {
                    MarginOverlay(
                        margins = margins, trim = trim, recto = page.recto, zoom = zoom, selected = selected, own = own,
                        onSelect = { onSelect(page.number) },
                        onCommit = { m -> c.updateLayout { it.copy(page = it.page.withBox(page.number, m)) } },
                    )
                }
            }
            if (guides && !rulerOnLeft) MmRuler(trim.heightMm, zoom, horizontal = false, marksMm = listOf(margins.topMm, trim.heightMm - margins.bottomMm))
        }
        Text(
            "${page.number}" + (if (page.folio != page.number.toString()) " (nº ${page.folio})" else "") +
                (if (page.kind != PageKind.Texto) " · ${page.kind.label.lowercase()}" else "") + if (own) " · mancha própria" else "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.height(20.dp).padding(top = 4.dp),
        )
    }
}

@Composable
private fun ThemePicker(current: LayoutTheme, onPick: (LayoutTheme) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) { Text("Tema: ${current.label}") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            LayoutTheme.entries.forEach { t ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(t.label, fontWeight = if (t == current) FontWeight.Bold else FontWeight.Normal)
                            Text(t.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    onClick = { open = false; onPick(t) },
                )
            }
        }
    }
}

@Composable
private fun BookIndex(c: EscribaController, laid: LaidOutBook?, modifier: Modifier, onJump: (Int) -> Unit) {
    Column(modifier.background(MaterialTheme.colorScheme.surfaceContainerLow).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Estrutura", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (laid == null) {
            Text("Calculando páginas…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
        // Pré-textuais (antes do primeiro capítulo) e capítulos, com a página onde começam.
        val front = laid.pages.takeWhile { it.chapterId == null || it.kind == PageKind.Sumario || it.kind == PageKind.Branca }
            .filter { it.kind != PageKind.Branca }
        front.forEach { p -> IndexRow(p.kind.label, p.number) { onJump(p.number) } }
        if (front.isNotEmpty()) HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
        laid.toc.forEach { e -> IndexRow(e.label, e.page) { onJump(e.page) } }
        Spacer(Modifier.height(16.dp))
        LayoutChecks(c, laid, onJump)
    }
}

/** Cor do semáforo da conferência, a partir dos tons de estado do tema. */
@Composable
private fun lightColor(light: CheckLight): Color = when (light) {
    CheckLight.Vermelho -> LocalPalette.current.danger
    CheckLight.Amarelo -> LocalPalette.current.warning
    CheckLight.Verde -> LocalPalette.current.success
}

/** Semáforo da conferência, com consertos de um clique, e a ficha técnica para orçamento. */
@Composable
private fun LayoutChecks(c: EscribaController, laid: LaidOutBook, onJump: (Int) -> Unit) {
    val checks = remember(laid) { checkLayout(laid) }
    val light = checks.light()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(lightColor(light)))
        Spacer(Modifier.width(8.dp))
        Overline("Conferência")
    }
    Text(light.label.replaceFirstChar(Char::uppercase), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))
    checks.sortedBy { it.level }.forEach { item ->
        Row(Modifier.padding(vertical = 4.dp)) {
            Icon(
                if (item.level == CheckLevel.Critico) Icons.Outlined.ErrorOutline else Icons.Outlined.WarningAmber, null, Modifier.size(14.dp),
                tint = if (item.level == CheckLevel.Critico) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
            )
            Spacer(Modifier.width(6.dp))
            Column {
                Text(item.message, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    item.page?.let { pg -> TextButton(onClick = { onJump(pg) }, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("pág. $pg", style = MaterialTheme.typography.labelSmall) } }
                    item.fix?.let { fix -> TextButton(onClick = { c.applyLayoutFix(fix) }, contentPadding = PaddingValues(horizontal = 6.dp)) { Text(fix.label, style = MaterialTheme.typography.labelSmall) } }
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    var specOpen by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { specOpen = !specOpen }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("FICHA TÉCNICA PARA ORÇAMENTO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Icon(if (specOpen) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, Modifier.size(16.dp))
    }
    if (specOpen) {
        Text(remember(laid) { printSpec(laid) }, style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { c.copyPrintSpec() }, modifier = Modifier.padding(top = 6.dp)) { Text("Copiar a ficha") }
        Text(
            "Faça a capa por último: a lombada depende do número final de páginas. E peça uma prova (boneco) à gráfica antes da tiragem.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun LayoutGateDialog(c: EscribaController, items: List<CheckItem>) {
    AlertDialog(
        onDismissRequest = { c.dismissLayoutGate() },
        title = { Text(if (items.size == 1) "Antes do PDF: 1 ponto sairia errado no impresso" else "Antes do PDF: ${items.size} pontos sairiam errados no impresso") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.forEach { Text("• ${it.message}", style = MaterialTheme.typography.bodyMedium) }
                Text("A conferência fica na coluna da esquerda, com os consertos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button(onClick = { c.dismissLayoutGate() }) { Text("Ver e corrigir") } },
        dismissButton = { TextButton(onClick = { c.exportLayoutPdf(force = true) }) { Text("Gerar assim mesmo") } },
    )
}

@Composable
private fun IndexRow(label: String, page: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text("$page", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------- Capa

@Composable
private fun TabSwitch(current: LayoutTab, onPick: (LayoutTab) -> Unit) {
    SegmentedControl(LayoutTab.entries.map { it.label }, LayoutTab.entries.indexOf(current), { onPick(LayoutTab.entries[it]) })
}

@Composable
private fun CoverWorkspace(c: EscribaController, laid: LaidOutBook?, cover: CoverLayout?, guides: Boolean, show3d: Boolean) {
    var dragMode by remember { mutableStateOf(CoverDragMode.Pecas) }
    Row(Modifier.fillMaxSize()) {
        CoverChecks(cover, Modifier.width(232.dp).fillMaxHeight())
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight().background(LocalPalette.current.desk), contentAlignment = Alignment.Center) {
            when {
                laid == null && c.layoutError != null -> EmptyState(Icons.Outlined.WarningAmber, "Não deu para diagramar", c.layoutError ?: "")
                laid == null || cover == null -> EmptyState(Icons.Outlined.PictureAsPdf, "Montando a capa…", "A lombada sai do número de páginas do miolo.")
                show3d -> Cover3DView(c, cover, minOf(maxWidth, maxHeight) - 48.dp)
                else -> {
                    val g = cover.geometry
                    val wPt = g.widthPt + 2 * g.bleedPt
                    val hPt = g.heightPt + 2 * g.bleedPt
                    val zoom = minOf((maxWidth.value - 64f) / wPt, (maxHeight.value - 64f) / hPt).coerceIn(0.1f, 2.5f)
                    val density = LocalDensity.current.density
                    val bitmap by produceState<ImageBitmap?>(null, cover, zoom, guides) {
                        value = withContext(Dispatchers.Default) { renderCover(cover, zoom * density, guides, c::layoutImage) }
                    }
                    Box(Modifier.size((wPt * zoom).dp, (hPt * zoom).dp).shadow(8.dp).background(Color(cover.background.toInt()))) {
                        bitmap?.let { Image(it, contentDescription = "Capa aberta", modifier = Modifier.fillMaxSize()) }
                        CoverDragLayer(
                            cover, zoom, dragMode,
                            onPiece = { p, dx, dy ->
                                c.updateCover { cv -> cv.copy(offsets = cv.offsets + (p to ((cv.offsets[p] ?: MmOffset()) + MmOffset(dx, dy)))) }
                            },
                            onOverlay = { id, dx, dy -> c.updateCoverOverlay(id) { it.copy(xMm = it.xMm + dx, yMm = it.yMm + dy) } },
                            onArt = { dx, dy ->
                                // Parte de onde a arte está de fato (o motor limita o arraste), para não "prender" no limite.
                                val img = cover.image ?: return@CoverDragLayer
                                val ex = ptToMm(img.x + img.width / 2 - g.widthPt / 2)
                                val ey = ptToMm(img.y + img.height / 2 - g.heightPt / 2)
                                c.updateCover { it.copy(artOffsetXMm = ex + dx, artOffsetYMm = ey + dy) }
                            },
                        )
                    }
                }
            }
        }
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        CoverSettingsPanel(c, laid, cover, dragMode, { dragMode = it }, Modifier.width(340.dp).fillMaxHeight())
    }
}

/** O livro pronto em 3D; ao montar a pele, grava também a vitrine da Biblioteca. */
@Composable
private fun Cover3DView(c: EscribaController, cover: CoverLayout, side: androidx.compose.ui.unit.Dp) {
    val skin by produceState<ImageBitmap?>(null, cover) {
        delay(150)
        val png = withContext(Dispatchers.Default) { c.coverSkinPng(cover) }
        c.storeCoverSnapshot(cover, png)
        value = withContext(Dispatchers.Default) { decodeImageBitmap(png) }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val image = skin
        if (image == null) {
            Box(Modifier.size(side), contentAlignment = Alignment.Center) { Text("Montando o livro 3D…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            Book3D(image, cover.geometry.snapshot(), Modifier.size(side))
        }
        Text(
            "Arraste para girar · duplo clique volta ao ângulo inicial · lombada de ${fmt(cover.geometry.spineMm)} mm",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CoverChecks(cover: CoverLayout?, modifier: Modifier) {
    Column(modifier.background(MaterialTheme.colorScheme.surfaceContainerLow).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Medidas", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (cover == null) {
            Text("Calculando…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
        val g = cover.geometry
        MeasureRow("Capa aberta", "${fmt(g.widthMm)} × ${fmt(g.heightMm)} mm")
        if (g.bleedMm > 0f) MeasureRow("Com sangria", "${fmt(g.widthMm + 2 * g.bleedMm)} × ${fmt(g.heightMm + 2 * g.bleedMm)} mm")
        g.zones.forEach { z -> MeasureRow(z.panel.label, "${fmt(z.widthMm)} mm") }
        cover.image?.let { MeasureRow("Arte", "${it.ppi.roundToInt()} ppi") }
        Spacer(Modifier.height(16.dp))
        Text("CONFERÊNCIA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (cover.issues.isEmpty()) {
            Row(Modifier.padding(vertical = 4.dp)) {
                Icon(Icons.Outlined.CheckCircle, null, Modifier.size(14.dp), tint = LocalPalette.current.accent)
                Spacer(Modifier.width(6.dp))
                Text("Tudo certo para gerar o PDF.", style = MaterialTheme.typography.bodySmall)
            }
        }
        cover.issues.sortedByDescending { it.blocking }.forEach { issue ->
            Row(Modifier.padding(vertical = 4.dp)) {
                Icon(
                    if (issue.blocking) Icons.Outlined.ErrorOutline else Icons.Outlined.WarningAmber, null, Modifier.size(14.dp),
                    tint = if (issue.blocking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                )
                Spacer(Modifier.width(6.dp))
                Text(issue.message, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (!cover.canExport) {
            Text(
                "Os itens em vermelho impedem gerar o PDF.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun MeasureRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CoverSettingsPanel(
    c: EscribaController,
    laid: LaidOutBook?,
    cover: CoverLayout?,
    dragMode: CoverDragMode,
    onDragMode: (CoverDragMode) -> Unit,
    modifier: Modifier,
) {
    val s = c.layoutSettings
    val cv = s.cover
    val families by produceState(emptyList<String>()) { value = withContext(Dispatchers.Default) { layoutFontFamilies() } }
    Column(modifier.background(MaterialTheme.colorScheme.surfaceContainerLow).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Group("Formato") {
            Text(
                "Formato do miolo: ${s.page.trim.label}. A lombada vem do número de páginas e do papel.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Choice("Papel do miolo", s.page.paper, PaperStock.entries, { it.label }) { v -> c.updateLayout { it.copy(page = it.page.copy(paper = v)) } }
            NumberField("Orelhas (mm, 0 = sem)", cv.flapMm, Modifier.fillMaxWidth()) { v -> c.updateCover { it.copy(flapMm = v.coerceIn(0f, 150f)) } }
            Choice("Sangria", cv.bleed, BleedPreset.entries, { it.label }) { v -> c.updateCover { it.copy(bleed = v) } }
            SwitchLine("Marcas de corte e dobra", cv.marks) { v -> c.updateCover { it.copy(marks = v) } }
        }
        Group("Arte") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { c.importCoverImage() }) {
                    Icon(Icons.Outlined.Image, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (cv.image.isBlank()) "Importar imagem…" else "Trocar imagem…")
                }
                if (cv.image.isNotBlank()) TextButton(onClick = { c.updateCover { it.copy(image = "") } }) { Text("Remover") }
            }
            val ppi = cover?.image?.ppi
            val neededPx = cover?.geometry?.let { fmt((it.widthMm + 2 * it.bleedMm) / 25.4f * 300f, 0) } ?: "…"
            Text(
                when {
                    cv.image.isBlank() -> "A imagem cobre a capa aberta inteira, sangria incluída. Para 300 ppi, ela precisa ter cerca de $neededPx pixels de largura."
                    ppi != null -> "Resolução no tamanho final: ${ppi.roundToInt()} ppi."
                    else -> "Lendo a imagem…"
                },
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Segmented("Arrastar na prévia", CoverDragMode.entries, dragMode, { it.label }) { onDragMode(it) }
            if (cv.image.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    NumberField("Tamanho da arte (%)", cv.artZoom * 100f, Modifier.weight(1f), decimals = 0) { v -> c.updateCover { it.copy(artZoom = (v / 100f).coerceIn(0.2f, 4f)) } }
                    TextButton(onClick = { c.updateCover { it.copy(artZoom = 1f, artOffsetXMm = 0f, artOffsetYMm = 0f) } }) { Text("Reenquadrar") }
                }
                Text(
                    "100% cobre a capa aberta até a sangria. Abaixo disso sobra uma borda em volta; acima, a arte amplia e dá para escolher o pedaço arrastando “A arte”.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Segmented("Fundo", listOf(false, true), cv.gradient != null, { if (it) "Degradê" else "Cor" }) { v ->
                c.updateCover { it.copy(gradient = if (v) it.gradient ?: COVER_GRADIENTS.first().second else null) }
            }
            val gradient = cv.gradient
            if (gradient == null) {
                Swatches("Cor de fundo", COVER_BACKGROUNDS, cv.background) { v -> c.updateCover { it.copy(background = v) } }
            } else {
                GradientSwatches(gradient) { v -> c.updateCover { it.copy(gradient = v) } }
                SwitchLine("Radial (luz no centro)", gradient.radial) { v -> c.updateCover { it.copy(gradient = gradient.copy(radial = v)) } }
                if (!gradient.radial) {
                    NumberField("Ângulo (°)", gradient.angleDeg, Modifier.fillMaxWidth(), decimals = 0) { v -> c.updateCover { it.copy(gradient = gradient.copy(angleDeg = ((v % 360f) + 360f) % 360f)) } }
                }
                Text("Com arte, o degradê fica por baixo dela (aparece na borda, se houver).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Swatches("Cor do texto", COVER_INKS, cv.ink) { v -> c.updateCover { it.copy(ink = v) } }
        }
        Group("Imagens sobre a capa") {
            OutlinedButton(onClick = { c.addCoverOverlay() }) {
                Icon(Icons.Outlined.Image, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("＋ Imagem (logo, selo)…")
            }
            if (cv.overlays.isEmpty()) {
                Text(
                    "Logo da editora, selo de prêmio… A imagem entra na contracapa; arraste-a na prévia para posicionar.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            cv.overlays.forEach { o ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(o.path.substringAfterLast('/'), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    key(o.id) { NumberField("Largura (mm)", o.widthMm, Modifier.width(110.dp)) { v -> c.updateCoverOverlay(o.id) { it.copy(widthMm = v.coerceIn(3f, 300f)) } } }
                    TextButton(onClick = { c.removeCoverOverlay(o.id) }) { Text("Remover") }
                }
            }
        }
        Group("Textos") {
            SwitchLine("Título na capa", cv.frontTitle) { v -> c.updateCover { it.copy(frontTitle = v) } }
            SwitchLine("Autor na capa", cv.frontAuthor) { v -> c.updateCover { it.copy(frontAuthor = v) } }
            Text(
                "Desligue se a imagem já traz o título ou o autor desenhado.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextLine("Título (vazio = do livro)", cv.title) { v -> c.updateCover { it.copy(title = v) } }
            TextLine("Autor (vazio = do livro)", cv.author) { v -> c.updateCover { it.copy(author = v) } }
            FontChoice("Fonte do título", cv.titleFont, "Do miolo (${laid?.titleFamily ?: "…"})", families) { v -> c.updateCover { it.copy(titleFont = v) } }
            NumberField("Título (pt)", cv.titleSizePt, Modifier.fillMaxWidth()) { v -> c.updateCover { it.copy(titleSizePt = v.coerceIn(10f, 120f)) } }
            SwitchLine("Título em caixa alta", cv.titleUppercase) { v -> c.updateCover { it.copy(titleUppercase = v) } }
            SwitchLine("Título e autor na lombada", cv.spineText) { v -> c.updateCover { it.copy(spineText = v) } }
            if (cv.spineText) {
                Segmented("Sentido na lombada", SpineDirection.entries, cv.spineDirection, { if (it == SpineDirection.Descendente) "Desce ↓" else "Sobe ↑" }) { v -> c.updateCover { it.copy(spineDirection = v) } }
                Text(cv.spineDirection.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (cv.offsets.isNotEmpty()) {
                TextButton(onClick = { c.updateCover { it.copy(offsets = emptyMap()) } }) { Text("Voltar os textos ao lugar automático") }
            }
            TextLine("Texto da contracapa", cv.backText, singleLine = false) { v -> c.updateCover { it.copy(backText = v) } }
        }
        Group("Código de barras") {
            SwitchLine("Código de barras do ISBN", cv.barcode) { v -> c.updateCover { it.copy(barcode = v) } }
            TextLine("ISBN", s.front.isbn) { v -> c.updateLayout { it.copy(front = it.front.copy(isbn = v)) } }
            val check = remember(s.front.isbn) { checkIsbn(s.front.isbn) }
            Text(
                when (check) {
                    IsbnCheck.Empty -> "O mesmo ISBN da página de créditos. Aceita 13 dígitos ou 10 (formato antigo)."
                    is IsbnCheck.Valid -> "Válido · EAN ${check.ean}"
                    is IsbnCheck.Invalid -> check.reason
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (check is IsbnCheck.Invalid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GradientSwatches(value: CoverGradient, onPick: (CoverGradient) -> Unit) {
    Column {
        Text("Degradê", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            COVER_GRADIENTS.forEach { (name, grad) ->
                val selected = grad.from == value.from && grad.to == value.to
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape)
                            .border(if (selected) 3.dp else 1.dp, if (selected) LocalPalette.current.accent else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .background(Brush.linearGradient(listOf(Color(grad.from.toInt()), Color(grad.to.toInt()))))
                            .clickable { onPick(grad.copy(angleDeg = value.angleDeg, radial = value.radial)) },
                    )
                    Text(name, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun EbookCoverButton(enabled: Boolean, onPick: (EbookCoverSize) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, enabled = enabled) { Text("Capa do e-book (JPEG)") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            EbookCoverSize.entries.forEach { size ->
                DropdownMenuItem(text = { Text(size.label) }, onClick = { open = false; onPick(size) })
            }
        }
    }
}

@Composable
private fun Swatches(label: String, colors: List<Long>, value: Long, onPick: (Long) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            colors.forEach { argb ->
                val selected = argb == value
                Box(
                    Modifier.size(28.dp).clip(CircleShape)
                        .border(if (selected) 3.dp else 1.dp, if (selected) LocalPalette.current.accent else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .padding(if (selected) 4.dp else 0.dp).clip(CircleShape)
                        .background(Color(argb.toInt()))
                        .clickable { onPick(argb) },
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Painel de ajustes

@Composable
private fun SettingsPanel(c: EscribaController, selectedPage: Int?, onCloseSelection: () -> Unit, modifier: Modifier) {
    val s = c.layoutSettings
    val families by produceState(emptyList<String>()) { value = withContext(Dispatchers.Default) { layoutFontFamilies() } }
    Column(modifier.background(MaterialTheme.colorScheme.surfaceContainerLow).verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (selectedPage != null) PageBoxGroup(c, selectedPage, onCloseSelection)
        Group("Página") {
            Choice("Formato", s.page.trim, TrimSize.entries, { it.label }) { v -> c.updateLayout { it.copy(page = it.page.copy(trim = v)) } }
            Choice("Papel", s.page.paper, PaperStock.entries, { it.label }) { v -> c.updateLayout { it.copy(page = it.page.copy(paper = v)) } }
            Choice("Sangria", s.page.bleed, BleedPreset.entries, { it.label }) { v -> c.updateLayout { it.copy(page = it.page.copy(bleed = v)) } }
            SwitchLine("Marcas de corte", s.page.cropMarks) { v -> c.updateLayout { it.copy(page = it.page.copy(cropMarks = v)) } }
            Text("Margens do livro (mm)", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
            Text(
                "Também dá para arrastar as bordas da mancha na própria página (com as margens à mostra).",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Superior", s.page.marginTopMm, Modifier.weight(1f)) { v -> c.updateLayout { it.copy(page = it.page.copy(marginTopMm = v.coerceIn(5f, 60f))) } }
                NumberField("Inferior", s.page.marginBottomMm, Modifier.weight(1f)) { v -> c.updateLayout { it.copy(page = it.page.copy(marginBottomMm = v.coerceIn(5f, 60f))) } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Interna", s.page.marginInnerMm, Modifier.weight(1f)) { v -> c.updateLayout { it.copy(page = it.page.copy(marginInnerMm = v.coerceIn(5f, 60f))) } }
                NumberField("Externa", s.page.marginOuterMm, Modifier.weight(1f)) { v -> c.updateLayout { it.copy(page = it.page.copy(marginOuterMm = v.coerceIn(5f, 60f))) } }
            }
        }
        Group("Texto") {
            FontChoice("Fonte", s.body.fontFamily, "Do tema (${c.layout?.bodyFamily ?: "…"})", families) { v -> c.updateLayout { it.copy(body = it.body.copy(fontFamily = v)) } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Corpo (pt)", s.body.sizePt, Modifier.weight(1f)) { v -> c.updateLayout { it.copy(body = it.body.copy(sizePt = v.coerceIn(7f, 18f))) } }
                NumberField("Entrelinha", s.body.leading, Modifier.weight(1f), decimals = 2) { v -> c.updateLayout { it.copy(body = it.body.copy(leading = v.coerceIn(1f, 2.2f))) } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Recuo (em)", s.body.indentEm, Modifier.weight(1f)) { v -> c.updateLayout { it.copy(body = it.body.copy(indentEm = v.coerceIn(0f, 5f))) } }
                NumberField("Entre ¶ (linhas)", s.body.paragraphSpacingLines, Modifier.weight(1f)) { v -> c.updateLayout { it.copy(body = it.body.copy(paragraphSpacingLines = v.coerceIn(0f, 2f))) } }
            }
            Segmented("Alinhamento", BodyAlign.entries, s.body.align, { it.label }) { v -> c.updateLayout { it.copy(body = it.body.copy(align = v)) } }
            SwitchLine("1º parágrafo sem recuo", s.body.firstParagraphNoIndent) { v -> c.updateLayout { it.copy(body = it.body.copy(firstParagraphNoIndent = v)) } }
            SwitchLine("Hifenizar palavras", s.body.hyphenate) { v -> c.updateLayout { it.copy(body = it.body.copy(hyphenate = v)) } }
            SwitchLine("Composição fina (espaços parelhos)", s.body.fineComposition) { v -> c.updateLayout { it.copy(body = it.body.copy(fineComposition = v)) } }
            Choice("Viúvas e órfãs", s.body.widowOrphanLines, listOf(1, 2, 3), { if (it == 1) "Permitir" else "Mínimo de $it linhas" }) { v -> c.updateLayout { it.copy(body = it.body.copy(widowOrphanLines = v)) } }
        }
        Group("Capítulos") {
            Segmented("Abre em", OpeningSide.entries, s.opening.side, { it.label.removePrefix("Página ").replaceFirstChar(Char::uppercase) }) { v -> c.updateLayout { it.copy(opening = it.opening.copy(side = v)) } }
            Choice("Numeração", s.opening.numberStyle, ChapterNumberStyle.entries, { it.label }) { v -> c.updateLayout { it.copy(opening = it.opening.copy(numberStyle = v)) } }
            FontChoice("Fonte do título", s.opening.titleFont, "Do tema (${c.layout?.titleFamily ?: "…"})", families) { v -> c.updateLayout { it.copy(opening = it.opening.copy(titleFont = v)) } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Título (pt)", s.opening.titleSizePt, Modifier.weight(1f)) { v -> c.updateLayout { it.copy(opening = it.opening.copy(titleSizePt = v.coerceIn(10f, 48f))) } }
                NumberField("Descida (mm)", s.opening.sinkMm, Modifier.weight(1f)) { v -> c.updateLayout { it.copy(opening = it.opening.copy(sinkMm = v.coerceIn(0f, 120f))) } }
            }
            Choice("Letra capitular", s.opening.dropCapLines, listOf(0, 2, 3, 4), { if (it == 0) "Sem capitular" else "$it linhas" }) { v -> c.updateLayout { it.copy(opening = it.opening.copy(dropCapLines = v)) } }
            SwitchLine("Título em caixa alta", s.opening.titleUppercase) { v -> c.updateLayout { it.copy(opening = it.opening.copy(titleUppercase = v)) } }
            SwitchLine("Ornamento sob o título", s.opening.ornament) { v -> c.updateLayout { it.copy(opening = it.opening.copy(ornament = v)) } }
            TextLine("Quebra de cena", s.sceneBreak) { v -> c.updateLayout { it.copy(sceneBreak = v.ifBlank { "*   *   *" }) } }
        }
        Group("Cabeçalho e número", initiallyOpen = false) {
            Choice("Página esquerda", s.heads.verso, HeadContent.entries, { it.label }) { v -> c.updateLayout { it.copy(heads = it.heads.copy(verso = v)) } }
            Choice("Página direita", s.heads.recto, HeadContent.entries, { it.label }) { v -> c.updateLayout { it.copy(heads = it.heads.copy(recto = v)) } }
            Choice("Número da página", s.heads.folio, FolioPosition.entries, { it.label }) { v -> c.updateLayout { it.copy(heads = it.heads.copy(folio = v)) } }
            NumberField("Tamanho (pt)", s.heads.sizePt, Modifier.fillMaxWidth()) { v -> c.updateLayout { it.copy(heads = it.heads.copy(sizePt = v.coerceIn(6f, 14f))) } }
            SwitchLine("Cabeçalho em caixa alta", s.heads.uppercase) { v -> c.updateLayout { it.copy(heads = it.heads.copy(uppercase = v)) } }
            Segmented("Onde fica o cabeçalho", HeadPosition.entries, s.heads.position, { it.label.removePrefix("Canto de ").replaceFirstChar(Char::uppercase) }) { v -> c.updateLayout { it.copy(heads = it.heads.copy(position = v)) } }
            Segmented("Tinta do cabeçalho", HeadInk.entries, s.heads.ink, { it.label }) { v -> c.updateLayout { it.copy(heads = it.heads.copy(ink = v)) } }
            NumberField("Subir o cabeçalho (mm; negativo desce)", s.heads.raiseMm, Modifier.fillMaxWidth()) { v -> c.updateLayout { it.copy(heads = it.heads.copy(raiseMm = v.coerceIn(-10f, 10f))) } }
            SwitchLine("Cabeçalho também na abertura de capítulo", s.heads.onOpening) { v -> c.updateLayout { it.copy(heads = it.heads.copy(onOpening = v)) } }
            Choice("Número em", s.heads.folioPages, FolioPages.entries, { it.label }) { v -> c.updateLayout { it.copy(heads = it.heads.copy(folioPages = v)) } }
            Choice("Número na abertura de capítulo", s.heads.openingFolio, OpeningFolio.entries, { it.label }) { v -> c.updateLayout { it.copy(heads = it.heads.copy(openingFolio = v)) } }
            SwitchLine("Pré-textuais em romanos (i, ii…)", s.heads.romanFrontMatter) { v -> c.updateLayout { it.copy(heads = it.heads.copy(romanFrontMatter = v)) } }
            NumberField(
                if (s.heads.romanFrontMatter) "O 1º capítulo começa na página" else "A numeração começa em",
                s.heads.startAt.toFloat(), Modifier.fillMaxWidth(), decimals = 0,
            ) { v -> c.updateLayout { it.copy(heads = it.heads.copy(startAt = v.toInt().coerceIn(1, 9999))) } }
            Text(
                "Páginas em branco e as pré-textuais (rosto, créditos, sumário) nunca mostram número, mas contam. Para tirar o número ou o cabeçalho de uma página só, clique nela.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Group("Pré-textuais", initiallyOpen = false) {
            SwitchLine("Falsa folha de rosto", s.front.halfTitle) { v -> c.updateLayout { it.copy(front = it.front.copy(halfTitle = v)) } }
            SwitchLine("Folha de rosto", s.front.titlePage) { v -> c.updateLayout { it.copy(front = it.front.copy(titlePage = v)) } }
            SwitchLine("Página de créditos", s.front.credits) { v -> c.updateLayout { it.copy(front = it.front.copy(credits = v)) } }
            SwitchLine("Sumário", s.front.toc) { v -> c.updateLayout { it.copy(front = it.front.copy(toc = v)) } }
            TextLine("Editora", s.front.publisher) { v -> c.updateLayout { it.copy(front = it.front.copy(publisher = v)) } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextLine("Ano", s.front.year, Modifier.weight(0.4f)) { v -> c.updateLayout { it.copy(front = it.front.copy(year = v)) } }
                TextLine("ISBN", s.front.isbn, Modifier.weight(0.6f)) { v -> c.updateLayout { it.copy(front = it.front.copy(isbn = v)) } }
            }
            TextLine("Créditos (revisão, capa, ficha…)", s.front.creditsText, singleLine = false) { v -> c.updateLayout { it.copy(front = it.front.copy(creditsText = v)) } }
        }
        Text(
            "Dedicatória, epígrafe, prólogo, epílogo e outras partes vêm do tipo de cada capítulo no Manuscrito.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

/** Mancha da página clicada: segue a global ou tem a sua; dá para promovê-la a padrão do livro. */
@Composable
private fun PageBoxGroup(c: EscribaController, pageNumber: Int, onClose: () -> Unit) {
    val setup = c.layoutSettings.page
    val own = setup.hasOwnMargins(pageNumber)
    val m = setup.marginsFor(pageNumber)
    val facing = if (pageNumber % 2 == 0) pageNumber + 1 else pageNumber - 1
    val laid = c.layout
    Group("Caixa da escrita · página $pageNumber") {
        Segmented("Mancha desta página", listOf(false, true), own, { if (it) "Só desta página" else "Global" }) { v ->
            c.updateLayout { it.copy(page = if (v) it.page.ownMargins(pageNumber) else it.page.followGlobal(pageNumber)) }
        }
        fun set(edge: MarginEdge, v: Float) = c.updateLayout { it.copy(page = it.page.withBox(pageNumber, m.with(edge, v))) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Superior", m.topMm, Modifier.weight(1f)) { v -> set(MarginEdge.Top, v) }
            NumberField("Inferior", m.bottomMm, Modifier.weight(1f)) { v -> set(MarginEdge.Bottom, v) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Interna", m.innerMm, Modifier.weight(1f)) { v -> set(MarginEdge.Inner, v) }
            NumberField("Externa", m.outerMm, Modifier.weight(1f)) { v -> set(MarginEdge.Outer, v) }
        }
        val textW = setup.trim.widthMm - m.innerMm - m.outerMm
        val textH = setup.trim.heightMm - m.topMm - m.bottomMm
        val mirror = laid != null && facing in 1..laid.pages.size && setup.marginsFor(facing).sameAs(m)
        Text(
            "Mancha ${fmt(textW)} × ${fmt(textH)} mm" + if (mirror) " · igual à página ao lado ✓" else "",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            if (own) "Só esta página usa estas margens; o resto do livro segue a global. Se o livro repaginar, a exceção continua na página $pageNumber."
            else "Esta página segue a mancha do livro inteiro: mudar aqui (ou arrastar na página) muda todas.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (own) OutlinedButton(onClick = { c.updateLayout { it.copy(page = it.page.makeGlobal(pageNumber)) } }) { Text("Definir como global") }
            TextButton(onClick = onClose) { Text("Fechar") }
        }
    }
    val heads = c.layoutSettings.heads
    val laidPage = laid?.pages?.getOrNull(pageNumber - 1)
    Group("Cabeçalho e número · página $pageNumber") {
        laidPage?.let { pg ->
            Text(
                if (pg.folio != pg.number.toString()) "Esta página conta como “${pg.folio}”." else "Página ${pg.folio}.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SwitchLine("Sem cabeçalho nesta página", pageNumber in heads.hiddenHeads) { v ->
            c.updateLayout { it.copy(heads = it.heads.copy(hiddenHeads = if (v) it.heads.hiddenHeads + pageNumber else it.heads.hiddenHeads - pageNumber)) }
        }
        SwitchLine("Sem número nesta página (continua contando)", pageNumber in heads.hiddenFolios) { v ->
            c.updateLayout { it.copy(heads = it.heads.copy(hiddenFolios = if (v) it.heads.hiddenFolios + pageNumber else it.heads.hiddenFolios - pageNumber)) }
        }
        val chapterId = laidPage?.chapterId
        if (chapterId != null) {
            val own = heads.chapterHeads[chapterId] ?: ChapterHead()
            fun setHead(h: ChapterHead) = c.updateLayout { st ->
                val map = if (h.verso.isBlank() && h.recto.isBlank()) st.heads.chapterHeads - chapterId else st.heads.chapterHeads + (chapterId to h)
                st.copy(heads = st.heads.copy(chapterHeads = map))
            }
            Text("Cabeçalho deste capítulo (vazio = padrão do livro)", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
            key(chapterId) {
                TextLine("Página esquerda", own.verso) { v -> setHead((c.layoutSettings.heads.chapterHeads[chapterId] ?: ChapterHead()).copy(verso = v)) }
                TextLine("Página direita", own.recto) { v -> setHead((c.layoutSettings.heads.chapterHeads[chapterId] ?: ChapterHead()).copy(recto = v)) }
            }
        }
    }
}

@Composable
private fun Group(title: String, initiallyOpen: Boolean = true, content: @Composable () -> Unit) {
    var open by remember { mutableStateOf(initiallyOpen) }
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(Radius.control).clickable { open = !open }.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Overline(title, Modifier.weight(1f))
            Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, Modifier.size(16.dp), tint = LocalPalette.current.muted)
        }
        if (open) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
        HorizontalDivider(Modifier.padding(top = 12.dp), color = LocalPalette.current.hairline)
    }
}

@Composable
private fun <T> Choice(label: String, value: T, options: List<T>, text: (T) -> String, onPick: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)).clickable { open = true },
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text(value), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Outlined.ExpandMore, null, Modifier.size(18.dp))
                }
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { o ->
                    DropdownMenuItem(
                        text = { Text(text(o), fontWeight = if (o == value) FontWeight.Bold else FontWeight.Normal) },
                        onClick = { open = false; onPick(o) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FontChoice(label: String, value: String, defaultLabel: String, families: List<String>, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)).clickable { open = true },
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    val family = remember(value) { if (value.isBlank()) null else systemFontFamily(listOf(value)) }
                    Text(value.ifBlank { defaultLabel }, fontFamily = family, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Outlined.ExpandMore, null, Modifier.size(18.dp))
                }
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DropdownMenuItem(text = { Text(defaultLabel) }, onClick = { open = false; onPick("") })
                if (families.isEmpty()) DropdownMenuItem(text = { Text("Lendo fontes instaladas…") }, onClick = {}, enabled = false)
                families.forEach { name ->
                    val family = remember(name) { systemFontFamily(listOf(name)) }
                    DropdownMenuItem(text = { Text(name, fontFamily = family) }, onClick = { open = false; onPick(name) })
                }
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: Float, modifier: Modifier, decimals: Int = 1, onValue: (Float) -> Unit) {
    var text by remember { mutableStateOf(fmt(value, decimals)) }
    var focused by remember { mutableStateOf(false) }
    val current by rememberUpdatedState(value)
    // Acompanha mudanças de fora (arraste, tema) só sem foco: digitando "25", o "2" intermediário não vira o mínimo.
    LaunchedEffect(value) { if (!focused) text = fmt(value, decimals) }
    OutlinedTextField(
        value = text,
        onValueChange = { t ->
            text = t
            t.replace(',', '.').toFloatOrNull()?.let(onValue)
        },
        label = { Text(label, maxLines = 1) },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        modifier = modifier.onFocusChanged { f ->
            if (focused && !f.isFocused) text = fmt(current, decimals)
            focused = f.isFocused
        },
    )
}

@Composable
private fun TextLine(label: String, value: String, modifier: Modifier = Modifier.fillMaxWidth(), singleLine: Boolean = true, onValue: (String) -> Unit) {
    var text by remember { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; onValue(it) },
        label = { Text(label, maxLines = 1) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        shape = RoundedCornerShape(10.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        modifier = modifier,
    )
}

@Composable
private fun SwitchLine(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) }) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun <T> Segmented(label: String, options: List<T>, value: T, text: (T) -> String, onPick: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = LocalPalette.current.muted)
        SegmentedControl(options.map(text), options.indexOf(value), { onPick(options[it]) }, Modifier.fillMaxWidth(), dense = true, equal = true)
    }
}
