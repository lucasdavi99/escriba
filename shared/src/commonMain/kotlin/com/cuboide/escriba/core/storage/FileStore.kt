package com.cuboide.escriba.core.storage

import kotlinx.serialization.json.Json

/**
 * Acesso mínimo a arquivos. Caminhos usam `/` como separador em todas as plataformas.
 * [writeText] e [writeBytes] devem ser atômicos: um arquivo nunca fica pela metade.
 */
interface FileStore {
    fun exists(path: String): Boolean
    fun isDirectory(path: String): Boolean
    fun readText(path: String): String?
    fun readBytes(path: String): ByteArray?
    fun writeText(path: String, text: String)
    fun writeBytes(path: String, bytes: ByteArray)
    /** Nomes (não caminhos) das entradas diretas de [dir]; vazio se não existir. */
    fun list(dir: String): List<String>
    fun mkdirs(path: String)
    fun deleteRecursively(path: String)
    fun move(from: String, to: String)
}

fun joinPath(vararg parts: String): String =
    parts.filter { it.isNotEmpty() }
        .mapIndexed { i, p -> if (i == 0) p.trimEnd('/', '\\') else p.trim('/', '\\') }
        .joinToString("/")

fun fileName(path: String): String = path.trimEnd('/', '\\').substringAfterLast('/').substringAfterLast('\\')

/** Remove caracteres proibidos em nomes de arquivo no Windows/macOS/Linux. */
fun safeFileName(name: String, fallback: String): String {
    val cleaned = name.replace(Regex("[<>:\"/\\\\|?*\\u0000-\\u001F]"), "_").trim().trimEnd('.')
    return cleaned.ifEmpty { fallback }.take(120)
}

val EscribaJson = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}
