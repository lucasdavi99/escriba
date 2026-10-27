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

/** Medidor de texto da diagramação (mesmas fontes do PDF e da prévia). */
expect fun layoutMeasurer(): com.cuboide.escriba.core.layout.GlyphMeasurer

/** Famílias de fonte que podem ir para o PDF da diagramação. Pode demorar na primeira chamada (lê as fontes do sistema). */
expect fun layoutFontFamilies(): List<String>

expect fun imagePixelSize(bytes: ByteArray): Pair<Int, Int>?

/** Uma página diagramada como imagem, para a prévia. [pxPerPt] = pixels por ponto tipográfico. */
expect fun renderLayoutPage(
    book: com.cuboide.escriba.core.layout.LaidOutBook,
    page: com.cuboide.escriba.core.layout.LaidPage,
    pxPerPt: Float,
    showGuides: Boolean,
    images: (String) -> ByteArray?,
): androidx.compose.ui.graphics.ImageBitmap

/** Grava o PDF do miolo em [path]. */
expect fun writeLayoutPdf(book: com.cuboide.escriba.core.layout.LaidOutBook, path: String, images: (String) -> ByteArray?)
