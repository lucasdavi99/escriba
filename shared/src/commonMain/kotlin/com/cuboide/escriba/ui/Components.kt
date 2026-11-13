package com.cuboide.escriba.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- tokens

/** Escala de espaçamento: todo respiro da interface sai daqui. */
object Space {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val xxxl = 64.dp

    /** Margem lateral das páginas. */
    val page = 48.dp
}

/** Os três raios da interface. */
object Radius {
    val control = RoundedCornerShape(6.dp)
    val surface = RoundedCornerShape(10.dp)
    val sheet = RoundedCornerShape(16.dp)
}

object Motion {
    const val QUICK = 120
    const val PANEL = 220
}

// ---------------------------------------------------------------- página

/** Cabeçalho de página: título em fonte de exibição, subtítulo discreto e ações à direita. */
@Composable
fun PageHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().padding(start = Space.page, end = Space.xl, top = 36.dp, bottom = Space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.lg)) {
                Text(title, style = MaterialTheme.typography.displaySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                leading?.invoke()
            }
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = LocalPalette.current.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** Página padrão de cada seção: [PageHeader] e o conteúdo com as mesmas margens. */
@Composable
fun SectionPage(
    title: String,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        PageHeader(title, subtitle, actions = actions)
        Column(Modifier.fillMaxSize().padding(horizontal = Space.page).padding(bottom = Space.lg), content = content)
    }
}

// ---------------------------------------------------------------- tipografia e divisões

/** O único estilo de rótulo pequeno: caixa alta espaçada, discreta (ou no acento). */
@Composable
fun Overline(text: String, modifier: Modifier = Modifier, accent: Boolean = false, color: Color = Color.Unspecified) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.09.em),
        color = when {
            color != Color.Unspecified -> color
            accent -> LocalPalette.current.accent
            else -> LocalPalette.current.muted
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(LocalPalette.current.hairline))
}

@Composable
fun VHairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxHeight().width(1.dp).background(LocalPalette.current.hairline))
}

/** Número em destaque com rótulo discreto ao lado, numa linha. */
@Composable
fun InlineStat(value: String, label: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = LocalPalette.current.muted, modifier = Modifier.padding(bottom = 3.dp))
    }
}

/** Ponto colorido de status. */
@Composable
fun StatusDot(color: Color, size: Dp = 7.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}

// ---------------------------------------------------------------- navegação

data class TabItem(val label: String, val count: Int? = null, val icon: ImageVector? = null, val description: String? = null)

/**
 * Abas de texto com sublinhado no acento. [groups] separa conjuntos de abas por um fio vertical.
 * Devolve o índice global da aba clicada.
 */
@Composable
fun QuietTabs(
    tabs: List<TabItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    groupBreaks: Set<Int> = emptySet(),
) {
    val palette = LocalPalette.current
    Box(modifier) {
        Hairline(Modifier.align(Alignment.BottomStart))
        Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { i, tab ->
                if (i in groupBreaks) {
                    Box(Modifier.padding(horizontal = Space.sm).height(16.dp).width(1.dp).background(palette.hairline))
                }
                val isSel = i == selected
                val interaction = remember { MutableInteractionSource() }
                val hovered by interaction.collectIsHoveredAsState()
                val content by animateColorAsState(
                    when {
                        isSel -> MaterialTheme.colorScheme.onSurface
                        hovered -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        else -> palette.muted
                    },
                    tween(Motion.QUICK),
                )
                val underline by animateFloatAsState(if (isSel) 1f else 0f, tween(Motion.PANEL))
                Column(
                    Modifier
                        .width(IntrinsicSize.Max)
                        .clip(Radius.control)
                        .hoverable(interaction)
                        .clickable(interactionSource = interaction, indication = null) { onSelect(i) }
                        .then(if (tab.description != null) Modifier.semantics { contentDescription = tab.description } else Modifier)
                        .padding(horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (tab.icon != null) {
                            Icon(tab.icon, null, Modifier.size(16.dp), tint = content)
                            Spacer(Modifier.width(7.dp))
                        }
                        Text(
                            tab.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                            color = content,
                            maxLines = 1,
                            softWrap = false,
                        )
                        if (tab.count != null) {
                            Spacer(Modifier.width(6.dp))
                            Text("${tab.count}", style = MaterialTheme.typography.labelMedium, color = palette.muted)
                        }
                    }
                    Box(
                        Modifier.height(2.dp).fillMaxWidth(underline.coerceAtLeast(0.001f))
                            .clip(RoundedCornerShape(1.dp)).background(palette.accent.copy(alpha = underline)),
                    )
                }
            }
        }
    }
}

/** Controle segmentado: uma trilha suave e o segmento escolhido em relevo. */
@Composable
fun SegmentedControl(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    dots: List<Color?>? = null,
    dense: Boolean = false,
    /** Segmentos de largura igual, ocupando a largura toda. */
    equal: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val palette = LocalPalette.current
    Row(
        modifier.clip(Radius.surface).background(scheme.surfaceContainer).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { i, label ->
            val isSel = i == selected
            val bg by animateColorAsState(if (isSel) scheme.surfaceContainerLowest else Color.Transparent, tween(Motion.QUICK))
            Row(
                Modifier
                    .then(if (equal) Modifier.weight(1f) else Modifier)
                    .clip(RoundedCornerShape(7.dp))
                    .background(bg)
                    .then(if (isSel) Modifier.border(1.dp, palette.hairline, RoundedCornerShape(7.dp)) else Modifier)
                    .clickable { onSelect(i) }
                    .padding(horizontal = if (dense) 8.dp else 12.dp, vertical = if (dense) 5.dp else 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                dots?.getOrNull(i)?.let { StatusDot(it, 6.dp); Spacer(Modifier.width(6.dp)) }
                Text(
                    label,
                    style = if (dense) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                    color = if (isSel) scheme.onSurface else palette.muted,
                    fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- ações

/** Mostra [tooltip] ao passar o mouse sobre [content]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithTooltip(tooltip: String, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState(),
        content = content,
    )
}

/** Botão de ícone com dica ao passar o mouse. */
@Composable
fun IconAction(
    icon: ImageVector,
    tooltip: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    selected: Boolean = false,
    tint: Color = Color.Unspecified,
    /** `false` em barras de formatação: o clique não tira o foco do texto que está sendo editado. */
    takesFocus: Boolean = true,
    size: Dp = 36.dp,
) {
    val palette = LocalPalette.current
    WithTooltip(tooltip) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(size).focusProperties { canFocus = takesFocus },
            shape = Radius.control,
            colors = if (selected) {
                IconButtonDefaults.iconButtonColors(containerColor = palette.accentSoft, contentColor = palette.accent)
            } else {
                IconButtonDefaults.iconButtonColors()
            },
        ) {
            Icon(icon, tooltip, Modifier.size(19.dp), tint = if (tint == Color.Unspecified) LocalContentColor.current else tint)
        }
    }
}

/** Menu "⋯" padronizado: o conteúdo recebe uma função para fechar o menu. */
@Composable
fun MoreMenu(
    tooltip: String,
    icon: ImageVector = Icons.Outlined.MoreHoriz,
    size: Dp = 36.dp,
    content: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconAction(icon, tooltip, onClick = { open = true }, size = size)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) { content { open = false } }
    }
}

/** Superfície flutuante (barras de ferramentas, legendas, popovers): fio fino e sombra difusa. */
@Composable
fun FloatingPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier,
        shape = Radius.surface,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, LocalPalette.current.hairline),
        shadowElevation = 6.dp,
        content = content,
    )
}

/** Seção com título em [Overline] que abre e fecha com animação. */
@Composable
fun CollapsibleSection(
    title: String,
    modifier: Modifier = Modifier,
    initiallyOpen: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by remember(title) { mutableStateOf(initiallyOpen) }
    val rotation by animateFloatAsState(if (open) 0f else -90f, tween(Motion.PANEL))
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().clip(Radius.control).clickable { open = !open }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Overline(title, Modifier.weight(1f))
            trailing?.invoke()
            Icon(Icons.Outlined.ExpandMore, null, Modifier.size(16.dp).rotate(rotation), tint = LocalPalette.current.muted)
        }
        AnimatedVisibility(open, enter = expandVertically(tween(Motion.PANEL)) + fadeIn(), exit = shrinkVertically(tween(Motion.PANEL)) + fadeOut()) {
            Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}

// ---------------------------------------------------------------- estados

/** Estado vazio: ícone num halo suave, título em fonte de exibição e uma ação. */
@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, action: @Composable () -> Unit = {}) {
    val palette = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(vertical = Space.xxl, horizontal = Space.lg), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(palette.accentSoft, radius = size.minDimension / 2 * 0.78f)
                drawCircle(palette.accent.copy(alpha = 0.22f), radius = size.minDimension / 2 - 1f, style = Stroke(1f))
                drawCircle(palette.accent.copy(alpha = 0.55f), radius = 2.5f, center = Offset(size.width * 0.86f, size.height * 0.2f))
            }
            Icon(icon, null, Modifier.size(30.dp), tint = palette.accent)
        }
        Spacer(Modifier.height(Space.md))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, color = palette.muted, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(420.dp))
        Spacer(Modifier.height(Space.lg))
        action()
    }
}

// ---------------------------------------------------------------- livro

/** Capa gerada a partir do título: tecido liso num tom estável por livro, título em fonte de exibição. */
@Composable
fun BookCover(title: String, author: String, modifier: Modifier = Modifier, compact: Boolean = false) {
    val hue = ((title.hashCode().toLong() and 0xffff) % 360).toFloat()
    val base = Color.hsl(hue, 0.22f, 0.34f)
    val deep = Color.hsl(hue, 0.26f, 0.24f)
    val text = Color(0xFFF6F1E7)
    val shape = RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 6.dp, bottomEnd = 6.dp)
    Box(modifier.clip(shape).background(Brush.linearGradient(listOf(base, deep)))) {
        // Lombada e vinco.
        Box(Modifier.width(if (compact) 4.dp else 8.dp).fillMaxHeight().background(Color.Black.copy(alpha = 0.18f)))
        Box(Modifier.padding(start = if (compact) 6.dp else 11.dp).width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.10f)))
        if (compact) {
            Text(
                title.take(1).uppercase(),
                Modifier.align(Alignment.Center),
                color = text,
                style = MaterialTheme.typography.titleLarge,
            )
        } else {
            Column(
                Modifier.fillMaxSize().padding(start = 26.dp, end = 18.dp, top = 28.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    title,
                    color = text,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal, lineHeight = 27.sp),
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
                Column {
                    Box(Modifier.width(22.dp).height(1.dp).background(text.copy(alpha = 0.5f)))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        author.ifBlank { " " },
                        color = text.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Chip de status com cor. */
@Composable
fun StatusPill(text: String, color: Color) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.12f)).padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(color, 5.dp)
        Spacer(Modifier.width(5.dp))
        Text(text, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

/** Linha clicável com hover suave (listas tipográficas sem caixas). */
@Composable
fun HoverRow(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.(hovered: Boolean) -> Unit,
) {
    val palette = LocalPalette.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(
        when {
            selected -> palette.navSelected
            hovered -> palette.navSelected.copy(alpha = 0.5f)
            else -> Color.Transparent
        },
        tween(Motion.QUICK),
    )
    Row(
        modifier.clip(Radius.surface).background(bg).hoverable(interaction).clickable(interactionSource = interaction, indication = null, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) { content(hovered) }
}
