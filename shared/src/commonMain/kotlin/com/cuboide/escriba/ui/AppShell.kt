package com.cuboide.escriba.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuOpen
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.History
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        Section.Lixeira -> Icons.Outlined.DeleteOutline
        Section.Preferencias -> Icons.Outlined.Settings
    }

private val groups = listOf(
    null to listOf(Section.Biblioteca),
    "Escrever" to listOf(Section.Manuscrito, Section.Enredo, Section.Universo),
    "Acompanhar" to listOf(Section.Metas, Section.Versoes, Section.Busca),
    "Entregar" to listOf(Section.Exportar),
)

@Composable
fun AppShell(c: EscribaController) {
    Row(Modifier.fillMaxSize()) {
        if (!c.focusMode) NavRail(c)
        Box(Modifier.weight(1f).fillMaxHeight()) {
            when (c.section) {
                Section.Biblioteca -> LibraryScreen(c)
                Section.Manuscrito -> ManuscriptScreen(c)
                Section.Enredo -> OutlineBoard(c, Modifier.fillMaxSize())
                Section.Universo -> UniverseScreen(c)
                Section.Metas -> GoalsScreen(c)
                Section.Versoes -> VersionsScreen(c)
                Section.Busca -> SearchScreen(c)
                Section.Exportar -> ExportScreen(c)
                Section.Lixeira -> TrashScreen(c)
                Section.Preferencias -> SettingsScreen(c)
            }
        }
    }
}

@Composable
private fun NavRail(c: EscribaController) {
    val palette = LocalPalette.current
    val expanded = c.settings.navExpanded
    val width by animateDpAsState(if (expanded) 228.dp else 72.dp)
    Column(
        Modifier.width(width).fillMaxHeight().background(palette.navBackground).padding(vertical = 14.dp),
    ) {
        // Marca
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center,
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(palette.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("E", color = palette.accent, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
            if (expanded) {
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Escriba", color = palette.navSelectedContent, style = MaterialTheme.typography.titleLarge)
                    val book = c.book
                    Text(
                        book?.manifest?.title ?: "Nenhum livro aberto",
                        color = palette.navContent,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Column(Modifier.weight(1f)) {
            groups.forEach { (label, sections) ->
                if (label != null) {
                    if (expanded) {
                        Text(
                            label.uppercase(),
                            color = palette.navContent.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            letterSpacing = 1.4.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 28.dp, top = 14.dp, bottom = 4.dp),
                        )
                    } else {
                        Spacer(Modifier.height(14.dp))
                    }
                }
                sections.forEach { NavItem(c, it, expanded) }
            }
        }
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

@Composable
private fun NavItem(c: EscribaController, section: Section, expanded: Boolean, badge: String? = null) {
    NavButton(
        icon = section.icon,
        label = section.label,
        expanded = expanded,
        selected = c.section == section,
        enabled = !section.needsBook || c.book != null,
        badge = badge,
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
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    val content = if (selected) palette.navSelectedContent else palette.navContent
    Row(
        Modifier
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) palette.navSelected else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.35f)
            .padding(horizontal = if (expanded) 12.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center,
    ) {
        if (expanded) {
            Box(
                Modifier.width(3.dp).height(18.dp).clip(RoundedCornerShape(2.dp))
                    .background(if (selected) palette.accent else Color.Transparent),
            )
            Spacer(Modifier.width(9.dp))
        }
        Box {
            Icon(icon, label, tint = content, modifier = Modifier.size(20.dp))
            if (badge != null && !expanded) {
                Box(Modifier.align(Alignment.TopEnd).size(7.dp).clip(CircleShape).background(palette.accent))
            }
        }
        if (expanded) {
            Spacer(Modifier.width(14.dp))
            Text(
                label,
                color = content,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            if (badge != null) {
                Text(
                    badge,
                    color = palette.navBackground,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(palette.accent).padding(horizontal = 7.dp, vertical = 1.dp),
                )
            }
        }
    }
}
