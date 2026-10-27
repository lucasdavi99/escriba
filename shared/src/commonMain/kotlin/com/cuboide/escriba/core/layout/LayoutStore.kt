package com.cuboide.escriba.core.layout

import com.cuboide.escriba.core.storage.EscribaJson
import com.cuboide.escriba.core.storage.FileStore
import com.cuboide.escriba.core.storage.joinPath

/** Configurações de diagramação ficam no próprio livro (`layout.json`), junto do manuscrito. */
const val LAYOUT_FILE = "layout.json"

fun loadLayoutSettings(fs: FileStore, bookPath: String): LayoutSettings =
    fs.readText(joinPath(bookPath, LAYOUT_FILE))?.let {
        runCatching { EscribaJson.decodeFromString(LayoutSettings.serializer(), it) }.getOrNull()
    } ?: LayoutSettings()

fun saveLayoutSettings(fs: FileStore, bookPath: String, settings: LayoutSettings) {
    fs.writeText(joinPath(bookPath, LAYOUT_FILE), EscribaJson.encodeToString(LayoutSettings.serializer(), settings))
}
