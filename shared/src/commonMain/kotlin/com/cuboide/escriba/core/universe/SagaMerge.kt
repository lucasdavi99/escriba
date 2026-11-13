package com.cuboide.escriba.core.universe

import com.cuboide.escriba.core.model.EntryAttribute
import com.cuboide.escriba.core.model.EntryBlock
import com.cuboide.escriba.core.model.Universe
import com.cuboide.escriba.core.model.UniverseEntry

/** Universo de um livro que vai entrar na saga. */
data class BookUniverse(val path: String, val title: String, val universe: Universe)

/**
 * Resultado da fusão: o universo comum, as ids antigas que viraram outra ([idMap], para corrigir cenas e cartões)
 * e quantas fichas repetidas foram juntadas.
 */
data class SagaMerge(val universe: Universe, val idMap: Map<String, String>, val joined: Int)

/**
 * Junta os universos dos livros de uma saga. Fichas do mesmo tipo e com o mesmo nome (sem diferenciar acentos)
 * viram uma só: a do primeiro livro é a base; o que ela não tem vem das outras; o que é diferente vai para uma
 * seção "Em ‹livro›" da ficha. Apelidos, etiquetas e imagens se somam. Nada se perde.
 */
fun mergeSaga(books: List<BookUniverse>, newId: () -> String): SagaMerge {
    val merged = mutableListOf<UniverseEntry>()
    val idMap = HashMap<String, String>()
    var joined = 0
    for ((i, book) in books.withIndex()) {
        for (e in book.universe.entries) {
            val match = merged.indexOfFirst { it.kind == e.kind && fold(it.name.trim()) == fold(e.name.trim()) && it.name.isNotBlank() }
            when {
                i == 0 || match < 0 -> {
                    val id = if (merged.any { it.id == e.id }) newId() else e.id
                    if (id != e.id) idMap[e.id] = id
                    merged += e.copy(id = id)
                }
                else -> {
                    idMap[e.id] = merged[match].id
                    merged[match] = mergeInto(merged[match], e, book.title, newId)
                    joined++
                }
            }
        }
    }
    return SagaMerge(Universe(merged, UNIVERSE_SCHEMA_VERSION), idMap, joined)
}

private fun same(a: String, b: String) = fold(a.trim()) == fold(b.trim())

private fun mergeInto(base: UniverseEntry, other: UniverseEntry, bookTitle: String, newId: () -> String): UniverseEntry {
    val notes = mutableListOf<String>()
    var e = base

    if (e.subtitle.isBlank()) e = e.copy(subtitle = other.subtitle)
    else if (other.subtitle.isNotBlank() && !same(e.subtitle, other.subtitle)) notes += "${e.schema.subtitleLabel}: ${other.subtitle}"

    // Atributos padrão: preenche o que falta; apelidos se somam; o resto que diverge vira nota.
    for (a in other.attributes) {
        if (a.value.isBlank()) continue
        if (a.key == null) {
            if (e.attributes.none { it.key == null && same(it.label, a.label) && same(it.value, a.value) }) e = e.copy(attributes = e.attributes + a.copy(id = newId()))
            continue
        }
        val mine = e.attributes.firstOrNull { it.key == a.key }
        when {
            mine == null || mine.value.isBlank() -> e = e.copy(attributes = e.attributes.filterNot { it.key == a.key } + a.copy(id = mine?.id ?: newId()))
            a.key == EntrySchemas.ALIASES -> {
                val all = (mine.value.split(',', ';') + a.value.split(',', ';')).map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { fold(it) }
                e = e.copy(attributes = e.attributes.map { if (it.key == a.key) it.copy(value = all.joinToString(", ")) else it })
            }
            !same(mine.value, a.value) -> notes += "${a.label.ifBlank { e.schema.attribute(a.key)?.label.orEmpty() }}: ${a.value}"
        }
    }

    // Blocos: vazio recebe o texto; texto diferente vira nota com o título do bloco.
    for (b in other.blocks) {
        if (b.text.isBlank()) continue
        if (b.key == null) {
            if (e.blocks.none { it.key == null && same(it.title, b.title) && same(it.text, b.text) }) e = e.copy(blocks = e.blocks + b.copy(id = newId()))
            continue
        }
        val mine = e.blocks.firstOrNull { it.key == b.key }
        when {
            mine == null || mine.text.isBlank() -> e = e.copy(blocks = e.blocks.filterNot { it.key == b.key } + b.copy(id = mine?.id ?: newId()))
            !same(mine.text, b.text) -> notes += "${b.title.ifBlank { e.schema.block(b.key)?.title.orEmpty() }}:\n${b.text}"
        }
    }

    e = e.copy(
        tags = (e.tags + other.tags).distinctBy { fold(it) },
        summary = e.summary.ifBlank { other.summary },
        portrait = e.portrait ?: other.portrait,
        gallery = (e.gallery + listOfNotNull(other.portrait.takeIf { e.portrait != null && it?.path != e.portrait?.path }) + other.gallery)
            .distinctBy { it.path },
    )
    if (notes.isNotEmpty()) {
        val title = "Em $bookTitle"
        val text = notes.joinToString("\n\n")
        val existing = e.blocks.firstOrNull { it.key == null && it.title == title }
        e = if (existing != null) e.copy(blocks = e.blocks.map { if (it.id == existing.id) it.copy(text = it.text + "\n\n" + text) else it })
        else e.copy(blocks = e.blocks + EntryBlock(newId(), null, title, text))
    }
    return e
}

/** Caminhos de todas as imagens usadas pelas fichas. */
fun Universe.imagePaths(): Set<String> = entries.flatMap { e -> listOfNotNull(e.portrait?.path) + e.gallery.map { it.path } }.toSet()

@Suppress("unused")
private fun EntryAttribute.isAlias() = key == EntrySchemas.ALIASES
