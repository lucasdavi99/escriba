package com.cuboide.escriba.core.storage

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeId(val label: String, val description: String) {
    Papel("Papel", "Claro, tinta e terracota"),
    Sepia("Sépia", "Claro e quente"),
    Tinta("Tinta", "Grafite e âmbar"),
    Floresta("Floresta", "Escuro e esverdeado"),
}

/** Alinhamento do texto do manuscrito (parágrafos centralizados com `->…<-` não mudam). */
@Serializable
enum class EditorAlign(val label: String) { Esquerda("Esquerda"), Justificado("Justificado"), Centro("Centro"), Direita("Direita") }

/** Atalhos de espaçamento entre parágrafos (o valor fino fica em [AppSettings.paragraphSpacing]). */
enum class ParagraphSpacing(val label: String, val em: Float) { Compacto("Compacto", 0f), Normal("Normal", 0.5f), Arejado("Arejado", 1f) }

@Serializable
data class AppSettings(
    val theme: ThemeId = ThemeId.Papel,
    val editorFont: String = "",
    val editorFontSize: Int = 19,
    val editorWidth: Int = 720,
    val lineSpacing: Float = 1.7f,
    val firstLineIndent: Boolean = true,
    /** Tamanho do recuo de primeira linha, em em. */
    val indentSize: Float = 1.6f,
    val textAlign: EditorAlign = EditorAlign.Esquerda,
    /** Espaço extra entre parágrafos no editor, em em (0 = colados, como num livro com recuo). */
    val paragraphSpacing: Float = 0f,
    /** Travessão, reticências, aspas curvas e maiúscula no começo do parágrafo enquanto se digita. */
    val smartTypography: Boolean = true,
    val navExpanded: Boolean = true,
    /** Grupo "Ferramentas" (Metas, Versões, Busca, Diagramação, Exportar) aberto no menu lateral. */
    val navToolsExpanded: Boolean = false,
    val trashRetentionDays: Int = TrashStore.DEFAULT_RETENTION_DAYS,
    /** Conferir contradições do texto com as fichas enquanto se escreve (marcas no Manuscrito). */
    val checkContradictions: Boolean = false,
    val lastBookPath: String? = null,
)

class SettingsStore(private val fs: FileStore, private val file: String) {
    var current: AppSettings = load()
        private set

    fun update(transform: (AppSettings) -> AppSettings) {
        current = transform(current)
        fs.writeText(file, EscribaJson.encodeToString(AppSettings.serializer(), current))
    }

    private fun load(): AppSettings =
        fs.readText(file)?.let {
            runCatching { EscribaJson.decodeFromString(AppSettings.serializer(), it) }.getOrNull()
        } ?: AppSettings()
}
