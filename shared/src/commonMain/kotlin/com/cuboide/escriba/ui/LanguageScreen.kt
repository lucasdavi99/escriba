package com.cuboide.escriba.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.core.language.Concepts
import com.cuboide.escriba.core.language.LangStyle
import com.cuboide.escriba.core.language.Language
import com.cuboide.escriba.core.language.VarietyAxis
import com.cuboide.escriba.core.language.adaptForeignName
import com.cuboide.escriba.core.language.dictionary
import com.cuboide.escriba.core.language.displayLemma
import com.cuboide.escriba.core.language.form
import com.cuboide.escriba.core.language.history
import com.cuboide.escriba.core.language.meaningfulNames
import com.cuboide.escriba.core.language.translate
import com.cuboide.escriba.core.universe.fold
import com.cuboide.escriba.platform.copyToClipboard

/** Aba Idiomas do Universo: a língua inventada do livro. */
@Composable
fun LanguageScreen(c: EscribaController) {
    val book = c.book ?: return
    val wl = book.language
    val lang = c.bookLanguage
    if (wl == null || lang == null) {
        StylePicker(c)
        return
    }
    val scriptFamily = rememberScriptFamily(c)
    Row(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(Modifier.width(380.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LanguageCard(c, lang, wl.previousSeeds.isNotEmpty(), scriptFamily)
            SoundsCard(lang)
            GrammarCard(lang)
            ForeignNameCard(lang)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TranslatorCard(c, lang, wl.coined, scriptFamily)
            ScriptCard(c, lang, scriptFamily)
            NamesCard(lang)
            SameWordCard(lang)
            DictionaryCard(lang, wl.coined)
        }
    }
}

@Composable
private fun Card(title: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = LocalPalette.current.accent, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                trailing()
            }
            content()
        }
    }
}

@Composable
private fun Small(text: String) = Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StylePicker(c: EscribaController) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.Translate, null, Modifier.size(48.dp), tint = LocalPalette.current.accent)
        Spacer(Modifier.height(12.dp))
        Text("A língua do seu mundo", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            "Escolha o clima dos sons. O Escriba sorteia o resto: sons, palavras, gramática e as variedades da língua. " +
                "O sorteio parte do título do livro, então este livro tem sempre a mesma língua, e outro livro tem outra.",
            textAlign = TextAlign.Center, modifier = Modifier.width(620.dp), color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        FlowRow(Modifier.width(760.dp), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LangStyle.entries.forEach { s ->
                Surface(
                    Modifier.width(230.dp).clip(RoundedCornerShape(14.dp)).clickable { c.createLanguage(s) }.semantics { contentDescription = "Estilo ${s.label}" },
                    shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(s.label.uppercase(), style = MaterialTheme.typography.labelMedium, color = LocalPalette.current.accent, fontWeight = FontWeight.SemiBold)
                        Text(s.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguageCard(c: EscribaController, lang: Language, canGoBack: Boolean, scriptFamily: androidx.compose.ui.text.font.FontFamily?) {
    Card("A língua") {
        Text("LÍNGUA Nº ${lang.code}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(lang.name, style = MaterialTheme.typography.displaySmall, fontFamily = LocalFonts.current.display, modifier = Modifier.semantics { contentDescription = "Nome da língua" })
        if (scriptFamily != null) {
            Text(c.toScript(lang.name.lowercase()), fontFamily = scriptFamily, fontSize = 30.sp, color = LocalPalette.current.accent, modifier = Modifier.semantics { contentDescription = "Nome na escrita da língua" })
        }
        Text(lang.pronunciation(lang.name.lowercase()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${lang.style.label}: ${lang.style.description}", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LangStyle.entries.forEach { s -> FilterChip(s == lang.style, onClick = { c.setLanguageStyle(s) }, label = { Text(s.label) }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { c.rerollLanguage() }) { Text("Sortear outra língua") }
            if (canGoBack) TextButton(onClick = { c.previousLanguage() }) { Text("Voltar à anterior") }
        }
        Small("O número é a semente: só este livro tem esta língua. Trocar o estilo mantém o sorteio e muda o clima dos sons.")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SoundsCard(lang: Language) {
    Card("Sons") {
        Small("Consoantes (${lang.consonants.size})")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { lang.consonants.forEach { Chip(it) } }
        Small("Vogais (${lang.vowels.size})")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { lang.vowels.forEach { Chip(it) } }
    }
}

@Composable
private fun Chip(text: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun GrammarCard(lang: Language) {
    val g = lang.grammar
    Card("Gramática") {
        Rule("Ordem da frase", g.order.label)
        Rule("Adjetivo", if (g.adjectiveAfter) "depois do nome" else "antes do nome")
        Rule("Acento", g.stress.label)
        Rule("Plural", if (g.pluralReduplicates || g.plural == null) "repete a primeira sílaba" else g.plural.label)
        Rule("Passado", g.past.label)
        Rule("Futuro", g.future.label)
        Rule("Negação", g.negation.label)
        Rule("Honorífico", g.honorific.label)
        Rule("Lugar (em…)", g.locative.label)
        Rule("Nomes femininos", "terminam em -${g.feminine}")
        Rule("Escrita", g.direction.label)
    }
}

@Composable
private fun Rule(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun TranslatorCard(c: EscribaController, lang: Language, coined: Map<String, String>, scriptFamily: androidx.compose.ui.text.font.FontFamily?) {
    val varieties = remember(lang) { lang.changes.varieties(lang.name) }
    var text by remember { mutableStateOf("A rainha viu o mar.") }
    var varietyId by remember(lang) { mutableStateOf("atual") }
    var menu by remember { mutableStateOf(false) }
    val variety = varieties.firstOrNull { it.id == varietyId } ?: varieties[2]
    val t = remember(lang, variety, text, coined) { translate(lang, variety, text, coined) }
    val scheme = MaterialTheme.colorScheme
    Card("Tradutor") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(text, { text = it }, label = { Text("Frase em português") }, singleLine = true, modifier = Modifier.weight(1f))
            Box {
                OutlinedButton(onClick = { menu = true }) { Text(variety.name + " ▾") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    varieties.forEach { v -> DropdownMenuItem(text = { Text("${v.name} · ${v.axis.label.lowercase()}") }, onClick = { menu = false; varietyId = v.id }) }
                }
            }
        }
        Surface(shape = RoundedCornerShape(12.dp), color = scheme.surfaceContainerHigh) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (scriptFamily != null && t.sentence.isNotBlank()) {
                    Text(
                        c.toScript(t.sentence), fontFamily = scriptFamily, fontSize = 34.sp, textAlign = TextAlign.Center,
                        color = scheme.onSurface, modifier = Modifier.semantics { contentDescription = "Tradução na escrita" },
                    )
                }
                Text(
                    t.sentence.ifBlank { "Escreva uma frase simples em português." },
                    Modifier.semantics { contentDescription = "Tradução" },
                    style = MaterialTheme.typography.headlineSmall, fontFamily = LocalFonts.current.display, textAlign = TextAlign.Center,
                )
            }
        }
        if (t.glosses.isNotEmpty()) {
            Row(Modifier.fillMaxWidth()) {
                Text("PALAVRA", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                Text("QUER DIZER", Modifier.weight(1.3f), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                Text("COMO SE FALA", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            }
            t.glosses.forEach { g ->
                Row(Modifier.fillMaxWidth()) {
                    Text(g.word, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(g.meaning + if (g.isNew) " · palavra nova" else "", Modifier.weight(1.3f), style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = if (g.isNew) LocalPalette.current.accent else scheme.onSurface)
                    Text(g.pronunciation, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = t.sentence.isNotBlank(), onClick = {
                copyToClipboard(c.toScript(t.sentence)); c.keepCoined(t)
                c.message = "Copiado na escrita da língua. Cole no Manuscrito: aparece com o alfabeto na tela, na Diagramação e no PDF."
            }) { Text("Copiar na escrita da língua") }
            OutlinedButton(enabled = t.sentence.isNotBlank(), onClick = { copyToClipboard(t.sentence); c.keepCoined(t); c.message = "Tradução copiada em letras nossas." }) { Text("Copiar em letras nossas") }
        }
        Small("Palavras que a língua ainda não tinha são criadas na hora e ficam no dicionário quando você copia a tradução.")
    }
}

@Composable
private fun NamesCard(lang: Language) {
    var batch by remember(lang) { mutableStateOf(0) }
    val people = remember(lang, batch) { meaningfulNames(lang, batch, places = false) }
    val places = remember(lang, batch) { meaningfulNames(lang, batch, places = true) }
    Card("Nomes com significado", trailing = { TextButton(onClick = { batch++ }) { Text("Outros nomes") } }) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Small("Pessoas")
                people.forEach { n -> NameRow(n.name, (if (n.feminine == true) "fem. · " else "masc. · ") + n.meaning, n.pronunciation) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Small("Lugares")
                places.forEach { n -> NameRow(n.name, n.meaning, n.pronunciation) }
            }
        }
    }
}

@Composable
private fun NameRow(name: String, meaning: String, pronunciation: String) {
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(name, style = MaterialTheme.typography.titleMedium, fontFamily = LocalFonts.current.display, modifier = Modifier.weight(1f))
            Text(pronunciation, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("— $meaning", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ForeignNameCard(lang: Language) {
    var name by remember { mutableStateOf("Helena") }
    val adapted = remember(lang, name) { adaptForeignName(lang, name) }
    Card("Nome de fora") {
        Small("Como este povo diz o nome de um estrangeiro: os sons que a língua não tem viram os mais parecidos.")
        OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (adapted.isNotBlank()) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$name → ", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(adapted, style = MaterialTheme.typography.headlineSmall, fontFamily = LocalFonts.current.display, modifier = Modifier.semantics { contentDescription = "Nome adaptado" })
            }
            Text(lang.pronunciation(adapted.lowercase()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SameWordCard(lang: Language) {
    val varieties = remember(lang) { lang.changes.varieties(lang.name) }
    var lemma by remember { mutableStateOf("mar") }
    var menu by remember { mutableStateOf(false) }
    val proto = lang.protoWord(lemma)
    val now = varieties.first { it.id == "atual" }
    Card("A mesma palavra, em cada canto") {
        Box {
            OutlinedButton(onClick = { menu = true }) { Text(displayLemma(lemma) + " ▾") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                Concepts.showcase.forEach { l -> DropdownMenuItem(text = { Text(displayLemma(l)) }, onClick = { menu = false; lemma = l }) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            // História da palavra até a forma de hoje, regra por regra.
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(proto, style = MaterialTheme.typography.titleMedium, fontFamily = LocalFonts.current.display)
                Small("a forma mais antiga")
                val steps = now.history(proto)
                if (steps.isEmpty()) Small("Esta palavra atravessou o tempo sem mudar até a capital; os falares abaixo ainda podem mexer nela.")
                steps.forEach { s ->
                    Text(s.result, style = MaterialTheme.typography.titleSmall, fontFamily = LocalFonts.current.display)
                    Text(s.rule.explanation, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                VarietyAxis.entries.forEach { axis ->
                    Text(axis.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = LocalPalette.current.accent, modifier = Modifier.padding(top = 4.dp))
                    varieties.filter { it.axis == axis }.forEach { v ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(v.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text(v.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            val w = v.form(proto, lang).let { if (v.honorific) lang.grammar.honorific.apply(it) else it }
                            Text(w.uppercase(), style = MaterialTheme.typography.titleSmall, fontFamily = LocalFonts.current.display, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DictionaryCard(lang: Language, coined: Map<String, String>) {
    var open by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val entries = remember(lang, coined) { lang.dictionary(coined) }
    Card("Dicionário (${entries.size})", trailing = { TextButton(onClick = { open = !open }) { Text(if (open) "Fechar" else "Abrir") } }) {
        if (!open) {
            Small("Palavras básicas da língua e as que você criou nas traduções.")
            return@Card
        }
        OutlinedTextField(query, { query = it }, placeholder = { Text("Procurar em português ou na língua…") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        val q = fold(query.trim())
        Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            entries.filter { q.isEmpty() || fold(it.meaning).contains(q) || it.word.contains(q) }.forEach { e ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(e.meaning, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Text(e.word + if (e.coined) " ·" else "", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Família da escrita inventada do livro aberto (recriada só quando a fonte muda). */
@Composable
fun rememberScriptFamily(c: EscribaController): androidx.compose.ui.text.font.FontFamily? {
    val info = c.scriptFont
    return remember(info?.id) { info?.let { com.cuboide.escriba.platform.scriptFontFamily(it.bytes, it.id) } }
}

/** "A escrita": o alfabeto da língua, uma letra por som, com a direção da escrita. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScriptCard(c: EscribaController, lang: Language, scriptFamily: androidx.compose.ui.text.font.FontFamily?) {
    val script = c.bookScript ?: return
    Card("A escrita") {
        Small("Uma letra por som, escrita ${lang.grammar.direction.label}. No Manuscrito, o texto colado na escrita aparece com estas letras; DOCX, HTML e texto saem em letras nossas.")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            script.glyphs.forEach { g ->
                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Column(Modifier.width(64.dp).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(g.codepoint.toChar().toString(), fontFamily = scriptFamily, fontSize = 30.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(g.unit, style = MaterialTheme.typography.labelMedium, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
