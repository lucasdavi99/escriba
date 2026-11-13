package com.cuboide.escriba.core.storage

import com.cuboide.escriba.core.model.BOOK_EXTENSION
import com.cuboide.escriba.core.model.Beat
import com.cuboide.escriba.core.model.ChapterMeta
import com.cuboide.escriba.core.model.UniverseEntry
import com.cuboide.escriba.core.outline.insertAt
import kotlinx.serialization.Serializable

@Serializable
enum class TrashKind(val label: String) { Livro("Livro"), Capitulo("Capítulo"), Ficha("Ficha"), Cena("Cena"), Universo("Universo") }

@Serializable
data class TrashItem(
    val id: String,
    val kind: TrashKind,
    val title: String,
    val deletedAt: Long,
    /** Livro de origem (para capítulos, fichas e cenas). */
    val bookPath: String? = null,
    val bookTitle: String? = null,
    val chapter: ChapterMeta? = null,
    val chapterBody: String? = null,
    val entry: UniverseEntry? = null,
    val beat: Beat? = null,
    /** Posição do cartão no roteiro quando foi apagado. */
    val beatIndex: Int? = null,
    /** Posição do capítulo no livro quando foi apagado. */
    val chapterIndex: Int? = null,
)

sealed interface RestoreOutcome {
    data class Restored(val bookPath: String) : RestoreOutcome
    data class OriginMissing(val bookTitle: String?) : RestoreOutcome
}

/**
 * Lixeira com retenção: nada é apagado na hora. Itens vencidos são removidos por [purgeExpired].
 * Layout: `<trashDir>/<id>/item.json` e, para livros, `<trashDir>/<id>/book.escriba/`.
 */
class TrashStore(
    private val fs: FileStore,
    private val trashDir: String,
    private val books: BookRepository,
    private val now: () -> Long,
    private val newId: () -> String,
) {
    var retentionDays: Int = DEFAULT_RETENTION_DAYS
        set(value) { field = value.coerceIn(MIN_RETENTION_DAYS, MAX_RETENTION_DAYS) }

    fun list(): List<TrashItem> {
        fs.mkdirs(trashDir)
        return fs.list(trashDir).mapNotNull { readItem(it) }.sortedByDescending { it.deletedAt }
    }

    fun trashBook(bookPath: String, title: String): TrashItem {
        val item = TrashItem(newId(), TrashKind.Livro, title, now(), bookPath = bookPath, bookTitle = title)
        val dir = itemDir(item.id)
        fs.mkdirs(dir)
        fs.move(bookPath, joinPath(dir, "book$BOOK_EXTENSION"))
        writeItem(item)
        return item
    }

    /** Pasta inteira para a lixeira (universo do Acervo). Volta para o mesmo lugar, ou com outro nome se ele foi ocupado. */
    fun trashFolder(path: String, title: String, kind: TrashKind): TrashItem {
        val item = TrashItem(newId(), kind, title, now(), bookPath = path, bookTitle = title)
        val dir = itemDir(item.id)
        fs.mkdirs(dir)
        fs.move(path, joinPath(dir, FOLDER))
        writeItem(item)
        return item
    }

    fun trashChapter(bookPath: String, bookTitle: String, chapter: ChapterMeta, body: String, index: Int? = null): TrashItem {
        val item = TrashItem(
            newId(), TrashKind.Capitulo, chapter.title, now(),
            bookPath = bookPath, bookTitle = bookTitle, chapter = chapter, chapterBody = body, chapterIndex = index,
        )
        writeItem(item)
        return item
    }

    fun trashEntry(bookPath: String, bookTitle: String, entry: UniverseEntry): TrashItem {
        val item = TrashItem(
            newId(), TrashKind.Ficha, entry.name, now(),
            bookPath = bookPath, bookTitle = bookTitle, entry = entry,
        )
        writeItem(item)
        return item
    }

    fun trashBeat(bookPath: String, bookTitle: String, beat: Beat, index: Int): TrashItem {
        val item = TrashItem(
            newId(), TrashKind.Cena, beat.title, now(),
            bookPath = bookPath, bookTitle = bookTitle, beat = beat, beatIndex = index,
        )
        writeItem(item)
        return item
    }

    fun restore(item: TrashItem): RestoreOutcome {
        val outcome = when (item.kind) {
            TrashKind.Livro -> {
                val target = books.uniqueBookPath(item.title)
                fs.move(joinPath(itemDir(item.id), "book$BOOK_EXTENSION"), target)
                RestoreOutcome.Restored(target)
            }
            TrashKind.Capitulo -> {
                val bookPath = item.bookPath
                if (bookPath == null || !fs.exists(joinPath(bookPath, BookRepository.MANIFEST))) {
                    return RestoreOutcome.OriginMissing(item.bookTitle)
                }
                val chapter = item.chapter!!
                val manifest = books.loadManifest(bookPath)
                val meta = if (manifest.chapters.any { it.id == chapter.id }) chapter.copy(id = newId()) else chapter
                books.writeChapter(bookPath, meta.id, item.chapterBody.orEmpty())
                // Volta para onde estava (ou para o fim, se o livro encolheu).
                val chapters = manifest.chapters.toMutableList()
                chapters.add((item.chapterIndex ?: chapters.size).coerceIn(0, chapters.size), meta)
                books.saveManifest(bookPath, manifest.copy(chapters = chapters, modifiedAt = now()))
                RestoreOutcome.Restored(bookPath)
            }
            TrashKind.Universo -> {
                val original = item.bookPath ?: return RestoreOutcome.OriginMissing(item.title)
                var target = original
                var n = 2
                while (fs.exists(target)) target = original.removeSuffix(AcervoRepository.EXTENSION) + " ($n)" + AcervoRepository.EXTENSION.also { n++ }
                fs.move(joinPath(itemDir(item.id), FOLDER), target)
                RestoreOutcome.Restored(target)
            }
            TrashKind.Ficha -> {
                val bookPath = item.bookPath
                // A ficha pode ser de um livro ou de um universo do Acervo.
                if (bookPath == null || (!fs.exists(joinPath(bookPath, BookRepository.MANIFEST)) && !fs.exists(joinPath(bookPath, AcervoRepository.META)))) {
                    return RestoreOutcome.OriginMissing(item.bookTitle)
                }
                val universe = books.loadUniverse(bookPath)
                val entry = item.entry!!
                val restored = if (universe.entries.any { it.id == entry.id }) entry.copy(id = newId()) else entry
                books.saveUniverse(bookPath, universe.copy(entries = universe.entries + restored))
                RestoreOutcome.Restored(bookPath)
            }
            TrashKind.Cena -> {
                val bookPath = item.bookPath
                if (bookPath == null || !fs.exists(joinPath(bookPath, BookRepository.MANIFEST))) {
                    return RestoreOutcome.OriginMissing(item.bookTitle)
                }
                val outline = books.loadOutline(bookPath)
                val beat = item.beat!!
                val restored = if (outline.beats.any { it.id == beat.id }) beat.copy(id = newId()) else beat
                books.saveOutline(bookPath, outline.insertAt(restored, item.beatIndex ?: outline.beats.size))
                RestoreOutcome.Restored(bookPath)
            }
        }
        fs.deleteRecursively(itemDir(item.id))
        return outcome
    }

    /** Exclusão definitiva de um item que já está na lixeira (com as versões salvas do capítulo, se for um). */
    fun deleteForever(item: TrashItem) {
        val chapter = item.chapter
        val bookPath = item.bookPath
        if (item.kind == TrashKind.Capitulo && chapter != null && bookPath != null && fs.exists(joinPath(bookPath, BookRepository.MANIFEST))) {
            val stillInBook = runCatching { books.loadManifest(bookPath).chapters.any { it.id == chapter.id } }.getOrDefault(true)
            if (!stillInBook) fs.deleteRecursively(joinPath(bookPath, BookRepository.SNAPSHOTS_DIR, safeFileName(chapter.id, "capitulo")))
        }
        fs.deleteRecursively(itemDir(item.id))
    }

    /**
     * Restaura vários itens: livros primeiro (para capítulos e fichas acharem a origem) e depois do último apagado
     * para o primeiro, desfazendo as exclusões em ordem inversa para cada capítulo voltar à posição certa.
     * Devolve quantos voltaram.
     */
    fun restoreAll(items: List<TrashItem>): Int =
        items.sortedWith(compareBy<TrashItem> { it.kind != TrashKind.Livro }.thenByDescending { it.deletedAt })
            .count { restore(it) is RestoreOutcome.Restored }

    fun daysLeft(item: TrashItem): Int {
        val elapsedDays = ((now() - item.deletedAt) / DAY_MS).toInt()
        return (retentionDays - elapsedDays).coerceAtLeast(0)
    }

    /** Remove itens com mais de [retentionDays] dias. Devolve quantos foram removidos. */
    fun purgeExpired(): Int {
        val limit = now() - retentionDays * DAY_MS
        val expired = list().filter { it.deletedAt < limit }
        expired.forEach { deleteForever(it) }
        return expired.size
    }

    private val FOLDER = "pasta"

    private fun itemDir(id: String) = joinPath(trashDir, safeFileName(id, "item"))

    private fun writeItem(item: TrashItem) {
        fs.mkdirs(itemDir(item.id))
        fs.writeText(joinPath(itemDir(item.id), ITEM_FILE), EscribaJson.encodeToString(TrashItem.serializer(), item))
    }

    private fun readItem(dirName: String): TrashItem? =
        fs.readText(joinPath(trashDir, dirName, ITEM_FILE))?.let {
            runCatching { EscribaJson.decodeFromString(TrashItem.serializer(), it) }.getOrNull()
        }

    companion object {
        const val DEFAULT_RETENTION_DAYS = 60
        const val MIN_RETENTION_DAYS = 1
        const val MAX_RETENTION_DAYS = 365
        const val DAY_MS = 24L * 60 * 60 * 1000
        private const val ITEM_FILE = "item.json"
    }
}
