package com.cuboide.escriba.core.universe

import com.cuboide.escriba.core.model.EntryBlock
import com.cuboide.escriba.core.model.EntryKind
import com.cuboide.escriba.core.model.Universe
import com.cuboide.escriba.core.model.UniverseEntry

const val UNIVERSE_SCHEMA_VERSION = 2

/**
 * Leva fichas da versão 1 (resumo + anotações) para blocos. Personagem: tudo vai para "Notas" (o resumo como
 * primeira linha). Outros tipos: o resumo vira "Descrição" e as anotações, "Notas". Rodar de novo não muda nada.
 */
fun Universe.migrated(): Universe {
    if (schemaVersion >= UNIVERSE_SCHEMA_VERSION) return this
    return Universe(entries.map { it.migrated() }, UNIVERSE_SCHEMA_VERSION)
}

fun UniverseEntry.migrated(): UniverseEntry {
    if (summary.isBlank() && notes.isBlank()) return copy(summary = "", notes = "")
    val added = buildList {
        if (kind == EntryKind.Personagem) {
            val text = listOf(summary.trim(), notes.trim()).filter { it.isNotEmpty() }.joinToString("\n\n")
            add(EntryBlock("$id-notas", EntrySchemas.NOTES, "Notas", text))
        } else {
            val main = if (kind == EntryKind.Lore) "conteudo" else EntrySchemas.DESCRIPTION
            if (summary.isNotBlank()) add(EntryBlock("$id-$main", main, schema.block(main)?.title.orEmpty(), summary.trim()))
            if (notes.isNotBlank()) add(EntryBlock("$id-notas", EntrySchemas.NOTES, "Notas", notes.trim()))
        }
    }
    val merged = blocks + added.filter { a -> blocks.none { it.key == a.key } }
    return copy(summary = "", notes = "", blocks = merged)
}
