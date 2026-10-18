package com.cuboide.escriba.core.storage

import kotlinx.serialization.Serializable

@Serializable
data class Snapshot(
    val id: String,
    val chapterId: String,
    val createdAt: Long,
    val label: String,
    val words: Int,
    val body: String,
)

/** Versões de capítulos em `snapshots/<chapterId>/<createdAt>-<id>.json` dentro do livro. */
class SnapshotStore(
    private val fs: FileStore,
    private val now: () -> Long,
    private val newId: () -> String,
    private val maxPerChapter: Int = 200,
) {
    fun create(bookPath: String, chapterId: String, body: String, label: String, words: Int): Snapshot {
        val snapshot = Snapshot(newId(), chapterId, now(), label, words, body)
        val dir = dir(bookPath, chapterId)
        fs.mkdirs(dir)
        fs.writeText(joinPath(dir, fileNameOf(snapshot)), EscribaJson.encodeToString(Snapshot.serializer(), snapshot))
        prune(bookPath, chapterId)
        return snapshot
    }

    /** Mais recentes primeiro. */
    fun list(bookPath: String, chapterId: String): List<Snapshot> {
        val dir = dir(bookPath, chapterId)
        return fs.list(dir)
            .filter { it.endsWith(".json") }
            .mapNotNull { name ->
                fs.readText(joinPath(dir, name))?.let {
                    runCatching { EscribaJson.decodeFromString(Snapshot.serializer(), it) }.getOrNull()
                }
            }
            .sortedByDescending { it.createdAt }
    }

    fun remove(bookPath: String, snapshot: Snapshot) {
        fs.deleteRecursively(joinPath(dir(bookPath, snapshot.chapterId), fileNameOf(snapshot)))
    }

    fun removeAllFor(bookPath: String, chapterId: String) {
        fs.deleteRecursively(dir(bookPath, chapterId))
    }

    private fun prune(bookPath: String, chapterId: String) {
        list(bookPath, chapterId).drop(maxPerChapter).forEach { remove(bookPath, it) }
    }

    private fun dir(bookPath: String, chapterId: String) =
        joinPath(bookPath, BookRepository.SNAPSHOTS_DIR, safeFileName(chapterId, "capitulo"))

    private fun fileNameOf(s: Snapshot) = "${s.createdAt}-${safeFileName(s.id, "v")}.json"
}
