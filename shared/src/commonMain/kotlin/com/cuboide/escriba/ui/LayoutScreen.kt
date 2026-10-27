package com.cuboide.escriba.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
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
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.core.layout.BleedPreset
import com.cuboide.escriba.core.layout.BodyAlign
import com.cuboide.escriba.core.layout.ChapterNumberStyle
import com.cuboide.escriba.core.layout.FolioPosition
import com.cuboide.escriba.core.layout.HeadContent
import com.cuboide.escriba.core.layout.LaidOutBook
import com.cuboide.escriba.core.layout.LaidPage
import com.cuboide.escriba.core.layout.LayoutTheme
import com.cuboide.escriba.core.layout.OpeningSide
import com.cuboide.escriba.core.layout.PageKind
import com.cuboide.escriba.core.layout.PaperStock
import com.cuboide.escriba.core.layout.TrimSize
import com.cuboide.escriba.platform.layoutFontFamilies
import com.cuboide.escriba.platform.renderLayoutPage
import com.cuboide.escriba.platform.systemFontFamily
import kotlinx.coroutines.Dispatchers
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

@Composable
fun LayoutScreen(c: EscribaController) {
    val book = c.book ?: return
    val laid = c.layout
    val s = c.layoutSettings
    // Zoom em dp por ponto tipográfico; null = ajustar à largura da área (padrão).
    var zoomSetting by remember { mutableStateOf<Float?>(null) }
    var lastFitZoom by remember { mutableStateOf(0.75f) }
    var guides by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(start = 28.dp, end = 20.dp, top = 20.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Diagramação", style = MaterialTheme.typography.headlineMedium)
                val summary = if (laid == null) "Diagramando “${book.manifest.title}”…" else buildString {
                    append("${s.page.trim.label} · ${laid.pages.size} páginas · lombada ≈ ${fmt(laid.spineMm)} mm")
                    append(" · ${laid.bodyFamily} ${fmt(s.body.sizePt)} pt")
                    if (s.page.bleed != BleedPreset.Sem) append(" · sangria ${fmt(s.page.bleed.mm)} mm")
                }
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ThemePicker(s.theme) { c.applyLayoutTheme(it) }
            Spacer(Modifier.width(12.dp))
            val shownZoom = zoomSetting ?: lastFitZoom
            IconAction(Icons.Outlined.ZoomOut, "Diminuir", onClick = { zoomSetting = (shownZoom - 0.1f).coerceAtLeast(0.3f) })
            Text("${(shownZoom * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(44.dp))
            IconAction(Icons.Outlined.ZoomIn, "Aumentar", onClick = { zoomSetting = (shownZoom + 0.1f).coerceAtMost(2.5f) })
            IconAction(Icons.Outlined.FitScreen, "Ajustar à largura", onClick = { zoomSetting = null }, selected = zoomSetting == null)
            IconAction(Icons.Outlined.GridOn, "Mostrar margens", onClick = { guides = !guides }, selected = guides)
            Spacer(Modifier.width(8.dp))
            Button(onClick = { c.exportLayoutPdf() }, enabled = laid != null && !c.layoutBusy) {
                Icon(Icons.Outlined.PictureAsPdf, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Gerar PDF")
            }
        }
        Box(Modifier.fillMaxWidth().height(3.dp)) {
            if (c.layoutBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        Row(Modifier.fillMaxSize()) {
            BookIndex(c, laid, Modifier.width(232.dp).fillMaxHeight()) { page ->
                scope.launch { listState.animateScrollToItem(spreadIndexOf(page)) }
            }
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            BoxWithConstraints(Modifier.weight(1f).fillMaxHeight().background(LocalPalette.current.desk)) {
                val fit = laid?.let { ((maxWidth.value - 64f) / (2 * it.widthPt + 2f)).coerceIn(0.3f, 2.5f) } ?: 0.75f
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
                            .width(maxOf(maxWidth, (laid.widthPt * 2 * zoom + 64).dp)),
                        contentPadding = PaddingValues(28.dp),
                        verticalArrangement = Arrangement.spacedBy(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        itemsIndexed(spreads(laid.pages)) { _, (left, right) ->
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                PageSlot(c, laid, left, zoom, guides)
                                PageSlot(c, laid, right, zoom, guides)
                            }
                        }
                    }
                }
            }
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingsPanel(c, Modifier.width(340.dp).fillMaxHeight())
        }
    }
}

@Composable
private fun PageSlot(c: EscribaController, laid: LaidOutBook, page: LaidPage?, zoom: Float, guides: Boolean) {
    val w = (laid.widthPt * zoom).dp
    val h = (laid.heightPt * zoom).dp
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (page == null) {
            Spacer(Modifier.size(w, h))
            Spacer(Modifier.height(20.dp))
            return@Column
        }
        val density = LocalDensity.current.density
        val bitmap by produceState<ImageBitmap?>(null, laid, page, zoom, guides) {
            value = withContext(Dispatchers.Default) { renderLayoutPage(laid, page, zoom * density, guides, c::layoutImage) }
        }
        val paper = Color(laid.settings.page.paper.tint.toInt())
        Box(Modifier.size(w, h).shadow(6.dp).background(paper)) {
            bitmap?.let { Image(it, contentDescription = "Página ${page.number}", modifier = Modifier.fillMaxSize()) }
        }
        Text(
            "${page.number}" + if (page.kind != PageKind.Texto) " · ${page.kind.label.lowercase()}" else "",
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
        if (laid.warnings.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("CONFERÊNCIA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            laid.warnings.forEach { w ->
                Row(Modifier.padding(vertical = 4.dp)) {
                    Icon(Icons.Outlined.WarningAmber, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(6.dp))
                    Text(w, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
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

// ---------------------------------------------------------------- Painel de ajustes

@Composable
private fun SettingsPanel(c: EscribaController, modifier: Modifier) {
    val s = c.layoutSettings
    val families by produceState(emptyList<String>()) { value = withContext(Dispatchers.Default) { layoutFontFamilies() } }
    Column(modifier.background(MaterialTheme.colorScheme.surfaceContainerLow).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Group("Página") {
            Choice("Formato", s.page.trim, TrimSize.entries, { it.label }) { v -> c.updateLayout { it.copy(page = it.page.copy(trim = v)) } }
            Choice("Papel", s.page.paper, PaperStock.entries, { it.label }) { v -> c.updateLayout { it.copy(page = it.page.copy(paper = v)) } }
            Choice("Sangria", s.page.bleed, BleedPreset.entries, { it.label }) { v -> c.updateLayout { it.copy(page = it.page.copy(bleed = v)) } }
            SwitchLine("Marcas de corte", s.page.cropMarks) { v -> c.updateLayout { it.copy(page = it.page.copy(cropMarks = v)) } }
            Text("Margens (mm)", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
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

@Composable
private fun Group(title: String, initiallyOpen: Boolean = true, content: @Composable () -> Unit) {
    var open by remember { mutableStateOf(initiallyOpen) }
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { open = !open }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = LocalPalette.current.accent, modifier = Modifier.weight(1f))
            Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, Modifier.size(18.dp))
        }
        if (open) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
        HorizontalDivider(Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
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
    var text by remember(value) { mutableStateOf(fmt(value, decimals)) }
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
        modifier = modifier,
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
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
        ) {
            options.forEach { o ->
                val selected = o == value
                Box(
                    Modifier.weight(1f).background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable { onPick(o) }.padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text(o), style = MaterialTheme.typography.labelMedium, maxLines = 1,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
