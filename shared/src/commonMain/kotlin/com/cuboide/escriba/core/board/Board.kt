package com.cuboide.escriba.core.board

import kotlinx.serialization.Serializable

/** Tipo de nó do mapa de enredo. */
@Serializable
enum class NodeKind(val label: String) {
    Cartao("Cartão"),
    Imagem("Imagem"),
    Texto("Texto"),
}

/**
 * Nó do mapa. Posição e tamanho em unidades do quadro (dp com zoom de 100 %).
 * Em [NodeKind.Imagem] o [title] é a legenda; em [NodeKind.Texto] é o próprio texto.
 */
@Serializable
data class BoardNode(
    val id: String,
    val kind: NodeKind,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val title: String = "",
    val body: String = "",
    /** Caminho relativo da imagem em `assets/` do livro. */
    val image: String? = null,
    /** Cor da paleta ("#RRGGBB"), ou `null` para neutro. */
    val color: String? = null,
    /** Ficha do Universo vinculada ao cartão. */
    val entryId: String? = null,
    /** Como a imagem é recortada e posicionada dentro do nó. */
    val frame: ImageFrame = ImageFrame(),
    /** Cartão: fração da altura ocupada pela imagem. */
    val imageRatio: Float = ImageFrame.DEFAULT_CARD_RATIO,
) {
    val right: Float get() = x + width
    val bottom: Float get() = y + height
    val centerX: Float get() = x + width / 2
    val centerY: Float get() = y + height / 2
}

/** Lado do nó onde a ligação se prende. */
@Serializable
enum class Side { Topo, Direita, Base, Esquerda }

/** Ligação entre dois nós; sem lados fixos, o desenho escolhe os lados mais próximos. */
@Serializable
data class BoardEdge(
    val id: String,
    val from: String,
    val to: String,
    val fromSide: Side? = null,
    val toSide: Side? = null,
    val color: String? = null,
    val label: String = "",
)

/** Onde a vista do quadro estava: deslocamento em dp de tela e zoom. */
@Serializable
data class Viewport(val x: Float = 0f, val y: Float = 0f, val zoom: Float = 1f)

@Serializable
data class Board(
    val id: String,
    val name: String,
    val nodes: List<BoardNode> = emptyList(),
    val edges: List<BoardEdge> = emptyList(),
    val viewport: Viewport = Viewport(),
)

/** Significado de uma cor de ligação (ex.: vermelho = Amor). */
@Serializable
data class LegendItem(val color: String, val name: String)

/** Todos os mapas de enredo do livro (`enredo.json`). A legenda vale para todos os quadros. */
@Serializable
data class PlotBoards(
    val boards: List<Board> = emptyList(),
    val legend: List<LegendItem> = DEFAULT_LEGEND,
    val activeId: String? = null,
) {
    val active: Board? get() = boards.firstOrNull { it.id == activeId } ?: boards.firstOrNull()
}

/** Paleta fixa do mapa, em duas fileiras como na barra de seleção. */
val BOARD_COLORS: List<String> = listOf(
    "#D9B44A", "#D9605A", "#E0884A", "#C9A227", "#9DBA4E", "#5DAE7B", "#3FA59C", "#4BA3C7", "#5B8DEF", "#7A7FE0",
    "#9C6ADE", "#C266C9", "#E06A9E", "#B8494F", "#B07A4F", "#8C8C8C", "#C9C4B8", "#6E5B45", "#3E6B5A", "#40506E",
)

val DEFAULT_LEGEND: List<LegendItem> = listOf(
    LegendItem("#D9605A", "Amor"),
    LegendItem("#E0884A", "Inimizade"),
    LegendItem("#D9B44A", "Lealdade"),
    LegendItem("#9DBA4E", "Família"),
    LegendItem("#8C8C8C", "Rivalidade"),
)

/** Tamanho inicial e mínimo de cada tipo de nó. */
object NodeSizes {
    const val CARD_W = 240f
    const val CARD_H = 150f
    const val CARD_WITH_IMAGE_H = 280f
    const val IMAGE_W = 260f
    const val TEXT_W = 280f
    const val TEXT_H = 64f
    fun min(kind: NodeKind): Pair<Float, Float> = when (kind) {
        NodeKind.Cartao -> 140f to 80f
        NodeKind.Imagem -> 60f to 60f
        NodeKind.Texto -> 80f to 36f
    }
}
