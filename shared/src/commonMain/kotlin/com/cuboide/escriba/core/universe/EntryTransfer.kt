package com.cuboide.escriba.core.universe

import com.cuboide.escriba.core.model.EntryImage
import com.cuboide.escriba.core.model.EntryKind
import com.cuboide.escriba.core.model.UniverseEntry

/**
 * Leva fichas de um universo para outro (livro ↔ Acervo): cada cópia ganha id nova e as imagens são copiadas
 * para `assets/` do destino, com o caminho novo. [readAsset] lê do universo de origem; [importAsset] grava no
 * destino e devolve o caminho relativo.
 */
fun copyEntries(
    entries: List<UniverseEntry>,
    readAsset: (String) -> ByteArray?,
    importAsset: (ByteArray, String) -> String,
    newId: () -> String,
): List<UniverseEntry> {
    val copied = HashMap<String, String>() // a mesma imagem em duas fichas vira um arquivo só
    fun copy(image: EntryImage): EntryImage? {
        copied[image.path]?.let { return image.copy(path = it) }
        val bytes = readAsset(image.path) ?: return null
        val path = importAsset(bytes, image.path.substringAfterLast('.', "png"))
        copied[image.path] = path
        return image.copy(path = path)
    }
    return entries.map { e ->
        e.copy(
            id = newId(),
            portrait = e.portrait?.let(::copy),
            gallery = e.gallery.mapNotNull(::copy),
            blocks = e.blocks.map { it.copy(id = newId()) },
            attributes = e.attributes.map { it.copy(id = newId()) },
        )
    }
}

/** Já existe no destino uma ficha do mesmo tipo com esse nome (sem diferenciar acentos)? */
fun List<UniverseEntry>.hasSameName(entry: UniverseEntry): Boolean =
    any { it.kind == entry.kind && fold(it.name.trim()) == fold(entry.name.trim()) }

/** Contagem por tipo, na ordem dos tipos ("2 personagens · 1 local"). */
fun List<UniverseEntry>.countLabel(): String =
    EntryKind.entries.mapNotNull { k -> count { it.kind == k }.takeIf { it > 0 }?.let { "$it ${if (it == 1) k.label.lowercase() else k.plural.lowercase()}" } }
        .joinToString(" · ").ifEmpty { "nenhuma ficha" }
