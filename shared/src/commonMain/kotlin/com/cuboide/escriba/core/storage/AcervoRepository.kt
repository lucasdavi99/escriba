package com.cuboide.escriba.core.storage

import com.cuboide.escriba.core.model.Universe
import kotlinx.serialization.Serializable

/**
 * Universo guardado fora dos livros, no Acervo. [saga]: universo de uma saga, compartilhado pelos livros em
 * [bookPaths] (cada livro guarda uma cópia igual em `universe.json`, então continua completo sozinho).
 */
@Serializable
data class AcervoMeta(
    val id: String,
    val name: String,
    val description: String = "",
    val createdAt: Long = 0,
    /** Livro de onde as fichas vieram ("Importar do projeto atual"). */
    val sourceTitle: String? = null,
    val saga: Boolean = false,
    val bookPaths: List<String> = emptyList(),
)

/** Universo do Acervo encontrado no disco. */
data class AcervoSummary(val path: String, val meta: AcervoMeta, val entries: Int, val byKind: Map<com.cuboide.escriba.core.model.EntryKind, Int>)

/**
 * Acervo de universos em `<biblioteca>/Acervo/<nome>.universo/`, com `acervo.json`, `universe.json` e `assets/`.
 * Fichas e imagens usam os mesmos arquivos de um livro, então as funções de [BookRepository] que recebem um
 * caminho (universo, imagens) servem aqui também.
 */
class AcervoRepository(
    private val fs: FileStore,
    private val books: BookRepository,
    private val now: () -> Long,
    private val newId: () -> String,
) {
    val dir: String get() = joinPath(books.libraryDir, ACERVO_DIR)

    fun list(): List<AcervoSummary> {
        if (!fs.exists(dir)) return emptyList()
        return fs.list(dir).filter { it.endsWith(EXTENSION) }.mapNotNull { name ->
            val path = joinPath(dir, name)
            val meta = loadMeta(path) ?: return@mapNotNull null
            val entries = books.loadUniverse(path).entries
            AcervoSummary(path, meta, entries.size, entries.groupingBy { it.kind }.eachCount())
        }.sortedBy { it.meta.name.lowercase() }
    }

    fun create(name: String, description: String = "", universe: Universe = Universe(), sourceTitle: String? = null, saga: Boolean = false, bookPaths: List<String> = emptyList()): AcervoSummary {
        fs.mkdirs(dir)
        val base = safeFileName(name.ifBlank { "Universo" }, "Universo")
        var path = joinPath(dir, "$base$EXTENSION")
        var n = 2
        while (fs.exists(path)) path = joinPath(dir, "$base ($n)$EXTENSION").also { n++ }
        fs.mkdirs(path)
        val meta = AcervoMeta(newId(), name.trim().ifBlank { "Universo" }, description.trim(), now(), sourceTitle, saga, bookPaths)
        saveMeta(path, meta)
        books.saveUniverse(path, universe)
        return AcervoSummary(path, meta, universe.entries.size, universe.entries.groupingBy { it.kind }.eachCount())
    }

    fun loadMeta(path: String): AcervoMeta? =
        fs.readText(joinPath(path, META))?.let { runCatching { EscribaJson.decodeFromString(AcervoMeta.serializer(), it) }.getOrNull() }

    fun saveMeta(path: String, meta: AcervoMeta) {
        fs.writeText(joinPath(path, META), EscribaJson.encodeToString(AcervoMeta.serializer(), meta))
    }

    /** Pasta do universo de saga com essa id, se existir. */
    fun findSaga(sagaId: String): String? = list().firstOrNull { it.meta.saga && it.meta.id == sagaId }?.path

    companion object {
        const val ACERVO_DIR = "Acervo"
        const val EXTENSION = ".universo"
        const val META = "acervo.json"
    }
}
