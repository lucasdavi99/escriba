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

/** Vitrine da Biblioteca: a pele do livro 3D (contracapa, lombada e capa) e as medidas dela, guardadas no livro. */
const val COVER_SNAPSHOT_IMAGE = "capa-vitrine.png"
const val COVER_SNAPSHOT_FILE = "capa-vitrine.json"

fun saveCoverSnapshot(fs: FileStore, bookPath: String, snapshot: CoverSnapshot, png: ByteArray) {
    fs.writeBytes(joinPath(bookPath, COVER_SNAPSHOT_IMAGE), png)
    fs.writeText(joinPath(bookPath, COVER_SNAPSHOT_FILE), EscribaJson.encodeToString(CoverSnapshot.serializer(), snapshot))
}

fun loadCoverSnapshot(fs: FileStore, bookPath: String): Pair<CoverSnapshot, ByteArray>? {
    val json = fs.readText(joinPath(bookPath, COVER_SNAPSHOT_FILE)) ?: return null
    val snapshot = runCatching { EscribaJson.decodeFromString(CoverSnapshot.serializer(), json) }.getOrNull() ?: return null
    val png = fs.readBytes(joinPath(bookPath, COVER_SNAPSHOT_IMAGE)) ?: return null
    return snapshot to png
}
