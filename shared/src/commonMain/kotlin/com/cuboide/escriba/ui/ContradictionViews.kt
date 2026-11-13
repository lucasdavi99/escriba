package com.cuboide.escriba.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.core.model.UniverseEntry
import com.cuboide.escriba.core.universe.BookCheck
import com.cuboide.escriba.core.universe.Contradiction
import com.cuboide.escriba.core.universe.FactKind
import com.cuboide.escriba.core.universe.SealStatus
import com.cuboide.escriba.core.universe.relationIssues
import com.cuboide.escriba.core.universe.sealSuggestions
import com.cuboide.escriba.core.universe.sealText

// ---------- Manuscrito ----------

/** Bloco do inspetor: liga a conferência, mostra as contradições do capítulo e abre o relatório do livro. */
@Composable
fun ContradictionsBlock(c: EscribaController) {
    val scheme = MaterialTheme.colorScheme
    val on = c.settings.checkContradictions
    val check = c.bookCheck
    var report by remember { mutableStateOf<BookCheck?>(null) }
    var showOthers by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("CONTRADIÇÕES", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Switch(on, { c.setCheckContradictions(it) }, modifier = Modifier.semantics { contentDescription = "Conferir contradições" })
        }
        if (!on) {
            Text(
                "Desligada. Ligue para sublinhar no texto o que contradiz os fatos selados das fichas ou o próprio livro.",
                style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant,
            )
        } else if (check != null) {
            val here = check.contradictions.filter { it.spot.chapterId == c.currentChapterId }
            val others = check.contradictions - here.toSet()
            if (here.isEmpty()) Text("Nada se contradiz neste capítulo.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            here.forEach { ContradictionRow(c, it) }
            if (others.isNotEmpty()) {
                Text(
                    (if (showOthers) "▾ " else "▸ ") + "Nos outros capítulos: ${others.size}",
                    style = MaterialTheme.typography.bodySmall, color = scheme.primary,
                    modifier = Modifier.clickable { showOthers = !showOthers },
                )
                if (showOthers) others.forEach { ContradictionRow(c, it, chapterTitle = c.book?.manifest?.chapters?.firstOrNull { ch -> ch.id == it.spot.chapterId }?.title) }
            }
        }
        TextButton(onClick = { report = c.runCheck() }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            Text("Relatório do livro inteiro")
        }
    }
    report?.let { BookReportDialog(c, it) { report = null } }
}

@Composable
private fun ContradictionRow(c: EscribaController, item: Contradiction, chapterTitle: String? = null) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { c.openSpot(item.spot) },
        shape = RoundedCornerShape(8.dp), color = scheme.errorContainer.copy(alpha = 0.45f),
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (chapterTitle != null) Text(chapterTitle, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            Text(item.message, style = MaterialTheme.typography.bodySmall)
            item.source?.let { src ->
                Text(
                    "Onde foi dito", style = MaterialTheme.typography.labelSmall, color = scheme.primary,
                    modifier = Modifier.clickable { c.openSpot(src) },
                )
            }
        }
    }
}

/** Relatório do livro inteiro: fatos selados, contradições, relações, fichas a selar e idades citadas. */
@Composable
fun BookReportDialog(c: EscribaController, check: BookCheck, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val chapters = c.book?.manifest?.chapters.orEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Relatório do livro") },
        text = {
            Column(Modifier.width(520.dp).heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Fatos com selo: ${check.sealedFacts}, em ${check.sealedEntries} de ${check.characters} ficha(s) de personagem.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (check.contradictions.isEmpty() && check.relations.isEmpty()) {
                    Text("Nada se contradiz — nem nos fatos selados das fichas, nem no próprio texto.", style = MaterialTheme.typography.bodyMedium, color = scheme.primary)
                }
                if (check.contradictions.isNotEmpty()) {
                    ReportTitle("Contradições (${check.contradictions.size})")
                    check.contradictions.groupBy { it.spot.chapterId }.forEach { (id, list) ->
                        Text(chapters.firstOrNull { it.id == id }?.title ?: "Capítulo", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                        list.forEach { item -> ContradictionRow(c, item) }
                    }
                }
                if (check.relations.isNotEmpty()) {
                    ReportTitle("Aliados e inimigos")
                    check.relations.forEach { r ->
                        Text("• ${r.message}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.clickable { onDismiss(); c.openUniverseEntry(r.entryId) })
                    }
                }
                if (check.unsealed.isNotEmpty()) {
                    ReportTitle("Fichas que já dizem coisas ainda sem selo")
                    Text(
                        "Abra a ficha no Universo e use “Selar na ficha”: daí em diante o Manuscrito confere esses fatos.",
                        style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant,
                    )
                    check.unsealed.forEach { e ->
                        Text("• ${e.name}", style = MaterialTheme.typography.bodySmall, color = scheme.primary, modifier = Modifier.clickable { onDismiss(); c.openUniverseEntry(e.id) })
                    }
                }
                if (check.ages.isNotEmpty()) {
                    ReportTitle("Idades que o texto dá")
                    Text("Só para você conferir: idade não é acusada, porque personagem envelhece.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    check.ages.forEach { a ->
                        val title = chapters.firstOrNull { it.id == a.spot.chapterId }?.title.orEmpty()
                        Text("• ${a.entryName}: ${a.age} anos ($title)", style = MaterialTheme.typography.bodySmall, modifier = Modifier.clickable { onDismiss(); c.openSpot(a.spot) })
                    }
                }
                if (!c.settings.checkContradictions) {
                    HorizontalDivider()
                    Text("A conferência continua desligada: o texto não fica sublinhado.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
    )
}

@Composable
private fun ReportTitle(text: String) {
    Text(text.uppercase(), Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelMedium, color = LocalPalette.current.accent, fontWeight = FontWeight.SemiBold)
}

// ---------- ficha ----------

/**
 * "Para conferir" na ficha de personagem: o que a descrição física já diz e ainda não está selado (com conflitos
 * quando o atributo diz outra coisa), as relações que não batem e as contradições do texto com esta ficha.
 */
@Composable
fun SealCheckCard(c: EscribaController, entry: UniverseEntry) {
    val scheme = MaterialTheme.colorScheme
    val suggestions = remember(entry) { sealSuggestions(entry) }
    val relations = remember(c.book?.universe) { relationIssues(c.book?.universe?.entries.orEmpty()).filter { it.entryId == entry.id } }
    val contradictions = c.bookCheck?.contradictions.orEmpty().filter { it.entryId == entry.id }
    if (suggestions.isEmpty() && relations.isEmpty() && contradictions.isEmpty()) return
    var replace by remember(entry.id) { mutableStateOf(emptySet<FactKind>()) }
    Surface(shape = RoundedCornerShape(14.dp), color = scheme.surface, border = BorderStroke(1.dp, scheme.outlineVariant)) {
        Column(Modifier.padding(14.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("PARA CONFERIR", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            if (suggestions.isNotEmpty()) {
                Text("A descrição física já diz", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                suggestions.forEach { s ->
                    Column {
                        Text("${s.kind.label}: ${sealText(s.values)}", style = MaterialTheme.typography.bodySmall)
                        when (s.status) {
                            SealStatus.Conflito -> Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(s.kind in replace, { on -> replace = if (on) replace + s.kind else replace - s.kind })
                                Text("A ficha diz “${s.current}”. Usar a descrição", style = MaterialTheme.typography.labelSmall, color = scheme.error)
                            }
                            SealStatus.Igual -> Text("igual à ficha, falta selar", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                            SealStatus.Novo -> Text("ainda não está na ficha", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                        }
                    }
                }
                Button(onClick = { c.sealFromDescription(entry.id, replace); replace = emptySet() }) { Text("Selar na ficha") }
                Text(
                    "Copia para os atributos com selo. Daí em diante o Manuscrito confere esses fatos.",
                    style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant,
                )
            }
            if (relations.isNotEmpty()) {
                if (suggestions.isNotEmpty()) HorizontalDivider()
                Text("Aliados e inimigos", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                relations.forEach { Text("• ${it.message}", style = MaterialTheme.typography.bodySmall, color = scheme.error) }
            }
            if (contradictions.isNotEmpty()) {
                if (suggestions.isNotEmpty() || relations.isNotEmpty()) HorizontalDivider()
                Text("No texto (${contradictions.size})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                contradictions.take(5).forEach { item ->
                    Text(
                        "• ${item.message}", style = MaterialTheme.typography.bodySmall, color = scheme.error,
                        modifier = Modifier.clickable { c.openSpot(item.spot) },
                    )
                }
            }
            Spacer(Modifier.width(0.dp))
        }
    }
}
