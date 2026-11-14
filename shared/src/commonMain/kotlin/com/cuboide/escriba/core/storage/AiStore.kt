package com.cuboide.escriba.core.storage

import com.cuboide.escriba.core.ai.AiConfig

/** Conexão com a IA (provedor, chave, modelo), num arquivo só dela na pasta de dados: nunca vai para o livro. */
class AiStore(private val fs: FileStore, private val file: String) {
    var current: AiConfig = load()
        private set

    fun update(transform: (AiConfig) -> AiConfig) {
        current = transform(current)
        fs.writeText(file, EscribaJson.encodeToString(AiConfig.serializer(), current))
    }

    private fun load(): AiConfig =
        fs.readText(file)?.let {
            runCatching { EscribaJson.decodeFromString(AiConfig.serializer(), it) }.getOrNull()
        } ?: AiConfig()
}
