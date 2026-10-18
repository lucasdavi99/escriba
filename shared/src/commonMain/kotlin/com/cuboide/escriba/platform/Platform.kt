package com.cuboide.escriba.platform

import com.cuboide.escriba.core.export.ExportDocument
import com.cuboide.escriba.core.storage.FileStore

/** Serviços que dependem do sistema operacional. Cada alvo KMP fornece sua implementação. */
expect fun currentTimeMillis(): Long

expect fun newId(): String

/** Dia local (fuso do sistema) no formato `yyyy-MM-dd`. */
expect fun localDayKey(epochMillis: Long): String

expect fun formatDateTime(epochMillis: Long): String

/** Pasta padrão onde os livros (`*.escriba`) ficam. */
expect fun defaultLibraryDir(): String

/** Pasta de dados do app (lixeira, produtividade, preferências). */
expect fun appDataDir(): String

expect fun platformFileStore(): FileStore

expect fun buildDocx(document: ExportDocument): ByteArray

/** Abre o diálogo nativo de "Salvar como"; devolve o caminho escolhido ou `null`. */
expect suspend fun chooseSaveFile(suggestedName: String, extension: String): String?

/** Abre o diálogo nativo de "Abrir" filtrando por [extensions]; devolve o caminho escolhido ou `null`. */
expect suspend fun chooseOpenFile(title: String, extensions: List<String>): String?

expect fun revealInFileManager(path: String)

/** Primeira fonte instalada da lista, com regular/itálico/negrito; `null` se nenhuma existir. */
expect fun systemFontFamily(candidates: List<String>): androidx.compose.ui.text.font.FontFamily?

/** Fontes serifadas instaladas que fazem sentido para o texto do livro. */
expect fun installedBookFonts(): List<String>
