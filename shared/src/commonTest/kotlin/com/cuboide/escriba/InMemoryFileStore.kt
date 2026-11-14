package com.cuboide.escriba

import com.cuboide.escriba.core.storage.FileStore

class InMemoryFileStore : FileStore {
    val files = linkedMapOf<String, ByteArray>()
    private val dirs = mutableSetOf<String>()

    private fun norm(p: String) = p.replace('\\', '/').trimEnd('/')

    override fun exists(path: String) = norm(path).let { it in files || isDirectory(it) }

    override fun isDirectory(path: String): Boolean {
        val p = norm(path)
        return p in dirs || files.keys.any { it.startsWith("$p/") }
    }

    override fun readText(path: String) = files[norm(path)]?.decodeToString()

    override fun readBytes(path: String) = files[norm(path)]?.copyOf()

    override fun writeText(path: String, text: String) = writeBytes(path, text.encodeToByteArray())

    /** Quantas das próximas gravações falham (disco cheio, pasta sumiu). */
    var failWrites = 0

    override fun writeBytes(path: String, bytes: ByteArray) {
        if (failWrites > 0) {
            failWrites--
            throw IllegalStateException("não foi possível gravar $path")
        }
        val p = norm(path)
        mkdirs(p.substringBeforeLast('/'))
        files[p] = bytes
    }

    override fun list(dir: String): List<String> {
        val p = norm(dir) + "/"
        return (files.keys + dirs).filter { it.startsWith(p) }
            .map { it.removePrefix(p).substringBefore('/') }
            .filter { it.isNotEmpty() }
            .distinct().sorted()
    }

    override fun mkdirs(path: String) {
        var p = norm(path)
        while (p.isNotEmpty() && dirs.add(p)) p = p.substringBeforeLast('/', "")
    }

    override fun deleteRecursively(path: String) {
        val p = norm(path)
        files.keys.removeAll { it == p || it.startsWith("$p/") }
        dirs.removeAll { it == p || it.startsWith("$p/") }
    }

    override fun move(from: String, to: String) {
        val f = norm(from)
        val t = norm(to)
        check(!exists(t)) { "destino já existe: $t" }
        files.keys.filter { it == f || it.startsWith("$f/") }.forEach { files[t + it.removePrefix(f)] = files.remove(it)!! }
        dirs.filter { it == f || it.startsWith("$f/") }.forEach { dirs.remove(it); dirs.add(t + it.removePrefix(f)) }
        mkdirs(t.substringBeforeLast('/'))
    }
}

/** Relógio e gerador de ids determinísticos para os testes. */
class TestClock(var now: Long = 1_700_000_000_000L) {
    private var n = 0
    fun id() = "id${++n}"
}
