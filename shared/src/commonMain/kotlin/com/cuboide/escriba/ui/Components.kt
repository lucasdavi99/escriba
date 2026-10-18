package com.cuboide.escriba.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Cabeçalho padrão de cada seção: título em fonte de exibição, subtítulo e ações à direita. */
@Composable
fun SectionPage(
    title: String,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(start = 36.dp, end = 28.dp, top = 28.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineMedium)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 36.dp).padding(bottom = 24.dp), content = content)
    }
}

/** Cartão de estatística com ícone. */
@Composable
fun StatTile(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier, detail: String? = null) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 0.dp) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleLarge)
                if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, action: @Composable () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        action()
    }
}

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
) {
    WithTooltip(tooltip) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.focusProperties { canFocus = takesFocus },
            colors = if (selected) IconButtonDefaults.filledTonalIconButtonColors() else IconButtonDefaults.iconButtonColors(),
        ) {
            Icon(icon, tooltip, tint = if (tint == Color.Unspecified) androidx.compose.material3.LocalContentColor.current else tint)
        }
    }
}

/** Capa gerada a partir do título: gradiente estável por livro, título em fonte de exibição. */
@Composable
fun BookCover(title: String, author: String, modifier: Modifier = Modifier, compact: Boolean = false) {
    val hue = ((title.hashCode().toLong() and 0xffff) % 360).toFloat()
    val top = Color.hsl(hue, 0.32f, 0.30f)
    val bottom = Color.hsl((hue + 28f) % 360f, 0.40f, 0.18f)
    val gold = Color(0xFFE6C88F)
    Box(
        modifier
            .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 10.dp, bottomEnd = 10.dp))
            .background(Brush.verticalGradient(listOf(top, bottom))),
    ) {
        // Lombada
        Box(Modifier.width(10.dp).fillMaxHeight().background(Color.Black.copy(alpha = 0.22f)))
        val frame = if (compact) 8.dp else 14.dp
        Box(
            Modifier.fillMaxSize().padding(start = 10.dp + frame, end = frame, top = frame, bottom = frame)
                .border(1.dp, gold.copy(alpha = 0.45f), RoundedCornerShape(4.dp)),
        )
        Column(
            Modifier.fillMaxSize().padding(start = 14.dp + frame, end = frame + 4.dp, top = frame + 12.dp, bottom = frame + 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("✦", color = gold.copy(alpha = 0.8f), fontSize = 14.sp)
            Text(
                title,
                color = Color(0xFFF7EEDC),
                style = if (compact) MaterialTheme.typography.titleMedium.copy(fontFamily = MaterialTheme.typography.titleLarge.fontFamily) else MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                author.ifBlank { " " }.uppercase(),
                color = gold,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Chip de status com cor. */
@Composable
fun StatusPill(text: String, color: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.16f)).padding(horizontal = 8.dp, vertical = 2.dp),
    ) { Text(text, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold) }
}
