package com.cuboide.escriba.platform

import com.cuboide.escriba.core.export.ExportDocument
import com.cuboide.escriba.core.storage.FileStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.swing.SwingUtilities

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun newId(): String = UUID.randomUUID().toString()

actual fun localDayKey(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().toString()

private val dateTimeFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"))

actual fun formatDateTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(dateTimeFormat)

private val home: String get() = System.getProperty("user.home")

actual fun defaultLibraryDir(): String = File(home, "Documents/Escriba").path

actual fun appDataDir(): String {
    val os = System.getProperty("os.name").lowercase()
    val base = when {
        os.contains("win") -> System.getenv("APPDATA") ?: File(home, "AppData/Roaming").path
        os.contains("mac") -> File(home, "Library/Application Support").path
        else -> System.getenv("XDG_DATA_HOME") ?: File(home, ".local/share").path
    }
    return File(base, "Escriba").path
}

actual fun platformFileStore(): FileStore = JvmFileStore()

actual fun buildDocx(document: ExportDocument): ByteArray = DocxWriter.write(document)

actual suspend fun chooseSaveFile(suggestedName: String, extension: String): String? = withContext(Dispatchers.Default) {
    var result: String? = null
    SwingUtilities.invokeAndWait {
        val dialog = FileDialog(null as Frame?, "Exportar", FileDialog.SAVE)
        dialog.file = "$suggestedName.$extension"
        dialog.isVisible = true
        val dir = dialog.directory
        val file = dialog.file
        if (dir != null && file != null) {
            result = File(dir, if (file.endsWith(".$extension", ignoreCase = true)) file else "$file.$extension").path
        }
    }
    result
}

actual suspend fun chooseOpenFile(title: String, extensions: List<String>): String? = withContext(Dispatchers.Default) {
    var result: String? = null
    SwingUtilities.invokeAndWait {
        val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
        dialog.setFilenameFilter { _, name -> extensions.any { name.endsWith(".$it", ignoreCase = true) } }
        // No Windows o filtro acima não vale; o padrão "*.png;*.jpg" faz o papel dele.
        dialog.file = extensions.joinToString(";") { "*.$it" }
        dialog.isVisible = true
        val dir = dialog.directory
        val file = dialog.file
        if (dir != null && file != null && extensions.any { file.endsWith(".$it", ignoreCase = true) }) result = File(dir, file).path
    }
    result
}

actual fun revealInFileManager(path: String) {
    val file = File(path)
    runCatching {
        val desktop = Desktop.getDesktop()
        if (desktop.isSupported(Desktop.Action.BROWSE_FILE_DIR)) desktop.browseFileDirectory(file)
        else desktop.open(if (file.isDirectory) file else file.parentFile)
    }
}
