package com.cuboide.escriba.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuOpen
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ImportContacts
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Publish
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.Section

val Section.icon: ImageVector
    get() = when (this) {
        Section.Biblioteca -> Icons.Outlined.LocalLibrary
        Section.Manuscrito -> Icons.Outlined.EditNote
        Section.Enredo -> Icons.Outlined.Timeline
        Section.Universo -> Icons.Outlined.Public
        Section.Metas -> Icons.Outlined.Insights
        Section.Versoes -> Icons.Outlined.History
        Section.Busca -> Icons.Outlined.Search
        Section.Exportar -> Icons.Outlined.Publish
        Section.Diagramacao -> Icons.Outlined.ImportContacts
        Section.Lixeira -> Icons.Outlined.DeleteOutline
        Section.Preferencias -> Icons.Outlined.Settings
    }

/** Os três lugares onde o livro é escrito. */
private val primary = listOf(Section.Manuscrito, Section.Enredo, Section.Universo)

/** Acompanhar e entregar: ficam num grupo recolhível. */
private val tools = listOf(Section.Metas, Section.Versoes, Section.Busca, Section.Diagramacao, Section.Exportar)

@Composable
fun AppShell(c: EscribaController) {
    Row(Modifier.fillMaxSize()) {
        if (!c.focusMode) {
            NavRail(c)
            VHairline()
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            Crossfade(c.section, animationSpec = tween(Motion.PANEL)) { section ->
                Box(Modifier.fillMaxSize()) {
                    when (section) {
                        Section.Biblioteca -> LibraryScreen(c)
                        Section.Manuscrito -> ManuscriptScreen(c)
                        Section.Enredo -> OutlineBoard(c, Modifier.fillMaxSize())
                        Section.Universo -> UniverseScreen(c)
                        Section.Metas -> GoalsScreen(c)
                        Section.Versoes -> VersionsScreen(c)
                        Section.Busca -> SearchScreen(c)
                        Section.Exportar -> ExportScreen(c)
                        Section.Diagramacao -> LayoutScreen(c)
                        Section.Lixeira -> TrashScreen(c)
                        Section.Preferencias -> SettingsScreen(c)
                    }
                }
            }
        }
    }
}

@Composable
private fun NavRail(c: EscribaController) {
    val palette = LocalPalette.current
    val expanded = c.settings.navExpanded
    val width by animateDpAsState(if (expanded) 232.dp else 64.dp, tween(Motion.PANEL))
    val book = c.book
    Column(Modifier.width(width).fillMaxHeight().background(palette.navBackground).padding(vertical = 18.dp)) {
        // Marca.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = if (expanded) 22.dp else 0.dp).height(36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center,
        ) {
            if (expanded) {
                Text("Escriba", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal), color = palette.navSelectedContent)
                Spacer(Modifier.width(3.dp))
                Box(Modifier.padding(top = 10.dp).size(5.dp).clip(CircleShape).background(palette.accent))
            } else {
                Text("E", style = MaterialTheme.typography.headlineSmall, color = palette.navSelectedContent)
            }
        }
        Spacer(Modifier.height(20.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            NavItem(c, Section.Biblioteca, expanded)
            if (book != null) {
                Spacer(Modifier.height(18.dp))
                BookBlock(c, expanded)
                Spacer(Modifier.height(6.dp))
                primary.forEach { NavItem(c, it, expanded) }
                Spacer(Modifier.height(14.dp))
                ToolsGroup(c, expanded)
            }
        }
        Hairline(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        NavItem(c, Section.Lixeira, expanded, badge = c.trashItems.size.takeIf { it > 0 }?.toString())
        NavItem(c, Section.Preferencias, expanded)
        NavButton(
            icon = if (expanded) Icons.AutoMirrored.Outlined.MenuOpen else Icons.Outlined.Menu,
            label = "Recolher menu",
            expanded = expanded,
            selected = false,
            enabled = true,
            onClick = { c.updateSettings { it.copy(navExpanded = !expanded) } },
        )
    }
}

/** O livro aberto: miniatura da capa e título, abrindo o grupo dos lugares de escrita. */
@Composable
private fun BookBlock(c: EscribaController, expanded: Boolean) {
    val palette = LocalPalette.current
    val m = c.book?.manifest ?: return
    if (!expanded) {
        Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
            WithTooltip(m.title) { BookCover(m.title, m.author, Modifier.size(26.dp, 36.dp), compact = true) }
        }
        return
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        BookCover(m.title, m.author, Modifier.size(28.dp, 40.dp), compact = true)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                m.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
                color = palette.navSelectedContent,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (m.author.isNotBlank()) {
                Text(m.author, style = MaterialTheme.typography.labelSmall, color = palette.navContent, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ToolsGroup(c: EscribaController, expanded: Boolean) {
    val palette = LocalPalette.current
    val current = c.section in tools
    val open = !expanded || c.settings.navToolsExpanded || current
    val rotation by animateFloatAsState(if (open) 0f else -90f, tween(Motion.PANEL))
    if (expanded) {
        Row(
            Modifier.padding(horizontal = 12.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .clickable(enabled = !current) { c.updateSettings { it.copy(navToolsExpanded = !it.navToolsExpanded) } }
                .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Overline("Ferramentas", Modifier.weight(1f), color = palette.navContent.copy(alpha = 0.75f))
            Icon(Icons.Outlined.ExpandMore, if (open) "Recolher ferramentas" else "Mostrar ferramentas", Modifier.size(16.dp).rotate(rotation), tint = palette.navContent)
        }
    } else {
        Hairline(Modifier.padding(horizontal = 18.dp, vertical = 6.dp))
    }
    AnimatedVisibility(open, enter = expandVertically(tween(Motion.PANEL)) + fadeIn(), exit = shrinkVertically(tween(Motion.PANEL)) + fadeOut()) {
        Column { tools.forEach { NavItem(c, it, expanded, compact = true) } }
    }
}

@Composable
private fun NavItem(c: EscribaController, section: Section, expanded: Boolean, badge: String? = null, compact: Boolean = false) {
    NavButton(
        icon = section.icon,
        label = section.label,
        expanded = expanded,
        selected = c.section == section,
        enabled = !section.needsBook || c.book != null,
        badge = badge,
        compact = compact,
        onClick = { c.goTo(section) },
    )
}

@Composable
private fun NavButton(
    icon: ImageVector,
    label: String,
    expanded: Boolean,
    selected: Boolean,
    enabled: Boolean,
    badge: String? = null,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(
        when {
            selected -> palette.navSelected
            hovered && enabled -> palette.navSelected.copy(alpha = 0.55f)
            else -> Color.Transparent
        },
        tween(Motion.QUICK),
    )
    val content = if (selected || hovered) palette.navSelectedContent else palette.navContent
    val row = @Composable {
        Row(
            Modifier
                .padding(horizontal = 12.dp, vertical = 1.dp)
                .fillMaxWidth()
                .height(if (compact) 34.dp else 38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bg)
                .hoverable(interaction)
                .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick)
                .alpha(if (enabled) 1f else 0.35f)
                .padding(horizontal = if (expanded) 10.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center,
        ) {
            Box {
                Icon(icon, if (expanded) null else label, tint = if (selected) palette.accent else content, modifier = Modifier.size(if (compact) 17.dp else 19.dp))
                if (badge != null && !expanded) {
                    Box(Modifier.align(Alignment.TopEnd).size(6.dp).clip(CircleShape).background(palette.accent))
                }
            }
            if (expanded) {
                Spacer(Modifier.width(12.dp))
                Text(
                    label,
                    color = content,
                    style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else if (compact) FontWeight.Normal else FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                if (badge != null) {
                    Text(badge, color = palette.muted, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
    if (expanded) row() else WithTooltip(label) { row() }
}
