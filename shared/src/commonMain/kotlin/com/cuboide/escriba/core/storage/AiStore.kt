package com.cuboide.escriba.core.storage

import com.cuboide.escriba.core.ai.AiConfig
import com.cuboide.escriba.core.ai.withUpgradedModes

/** Conexão com a IA (provedor, chave, modelo), num arquivo só dela na pasta de dados: nunca vai para o livro. */
class AiStore(private val fs: FileStore, private val file: String) {
    var current: AiConfig = load()
        private set

    fun update(transform: (AiConfig) -> AiConfig) {
        current = transform(current)
        fs.writeText(file, EscribaJson.encodeToString(AiConfig.serializer(), current))
    }

    /** Lê a configuração e atualiza os modos de fábrica que o autor não editou (ver `withUpgradedModes`). */
    private fun load(): AiConfig =
        (fs.readText(file)?.let {
            runCatching { EscribaJson.decodeFromString(AiConfig.serializer(), it) }.getOrNull()
        } ?: AiConfig()).withUpgradedModes()
}
