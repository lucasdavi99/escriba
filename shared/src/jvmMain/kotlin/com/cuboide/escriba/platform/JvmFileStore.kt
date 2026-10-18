package com.cuboide.escriba.platform

import com.cuboide.escriba.core.storage.FileStore
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class JvmFileStore : FileStore {
    override fun exists(path: String) = File(path).exists()

    override fun isDirectory(path: String) = File(path).isDirectory

    override fun readText(path: String): String? =
        File(path).takeIf { it.isFile }?.readText(Charsets.UTF_8)

    override fun readBytes(path: String): ByteArray? = File(path).takeIf { it.isFile }?.readBytes()

    override fun writeText(path: String, text: String) = writeBytes(path, text.toByteArray(Charsets.UTF_8))

    /** Grava num temporário ao lado do destino, força para o disco e troca de uma vez. */
    override fun writeBytes(path: String, bytes: ByteArray) {
        val target = File(path)
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, ".${target.name}.${System.nanoTime()}.tmp")
        try {
            tmp.outputStream().use { out ->
                out.write(bytes)
                out.flush()
                out.fd.sync()
            }
            try {
                Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            tmp.delete()
        }
    }

    override fun list(dir: String): List<String> =
        File(dir).list()?.filterNot { it.startsWith(".") && it.endsWith(".tmp") }?.sorted() ?: emptyList()

    override fun mkdirs(path: String) {
        File(path).mkdirs()
    }

    override fun deleteRecursively(path: String) {
        File(path).deleteRecursively()
    }

    override fun move(from: String, to: String) {
        val target = File(to)
        target.parentFile?.mkdirs()
        Files.move(File(from).toPath(), target.toPath())
    }
}
