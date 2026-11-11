package com.cuboide.escriba.core.board

/** Operações puras sobre um quadro. Nenhuma delas grava nada. */

fun Board.node(id: String): BoardNode? = nodes.firstOrNull { it.id == id }

fun Board.edge(id: String): BoardEdge? = edges.firstOrNull { it.id == id }

fun Board.addNode(node: BoardNode): Board = copy(nodes = nodes + node)

fun Board.updateNode(id: String, transform: (BoardNode) -> BoardNode): Board =
    copy(nodes = nodes.map { if (it.id == id) transform(it) else it })

fun Board.moveNodes(ids: Set<String>, dx: Float, dy: Float): Board =
    if (dx == 0f && dy == 0f) this else copy(nodes = nodes.map { if (it.id in ids) it.copy(x = it.x + dx, y = it.y + dy) else it })

/** Novo tamanho, respeitando o mínimo do tipo. */
fun Board.resize(id: String, width: Float, height: Float): Board = updateNode(id) {
    val (minW, minH) = NodeSizes.min(it.kind)
    it.copy(width = width.coerceAtLeast(minW), height = height.coerceAtLeast(minH))
}

/** Remove os nós e todas as ligações que tocam neles. */
fun Board.removeNodes(ids: Set<String>): Board =
    copy(nodes = nodes.filterNot { it.id in ids }, edges = edges.filterNot { it.from in ids || it.to in ids })

/** Cópias dos nós, deslocadas; ligações entre os nós copiados também são copiadas. Devolve o quadro e os novos ids. */
fun Board.duplicate(ids: Set<String>, newId: () -> String, offset: Float = 24f): Pair<Board, Set<String>> {
    val originals = nodes.filter { it.id in ids }
    if (originals.isEmpty()) return this to emptySet()
    val map = originals.associate { it.id to newId() }
    val copies = originals.map { it.copy(id = map.getValue(it.id), x = it.x + offset, y = it.y + offset) }
    val edgeCopies = edges.filter { it.from in map && it.to in map }
        .map { it.copy(id = newId(), from = map.getValue(it.from), to = map.getValue(it.to)) }
    return copy(nodes = nodes + copies, edges = edges + edgeCopies) to map.values.toSet()
}

/** Liga dois nós. Recusa (devolve `null`) auto-ligação, nó inexistente e ligação repetida (em qualquer sentido). */
fun Board.connect(from: String, to: String, id: String, color: String? = null): Board? {
    if (from == to || node(from) == null || node(to) == null) return null
    if (edges.any { (it.from == from && it.to == to) || (it.from == to && it.to == from) }) return null
    return copy(edges = edges + BoardEdge(id = id, from = from, to = to, color = color))
}

fun Board.updateEdge(id: String, transform: (BoardEdge) -> BoardEdge): Board =
    copy(edges = edges.map { if (it.id == id) transform(it) else it })

fun Board.removeEdge(id: String): Board = copy(edges = edges.filterNot { it.id == id })

/** Retângulo que envolve todos os nós, ou `null` num quadro vazio. */
fun Board.bounds(): Rect? {
    if (nodes.isEmpty()) return null
    val l = nodes.minOf { it.x }
    val t = nodes.minOf { it.y }
    return Rect(l, t, nodes.maxOf { it.right } - l, nodes.maxOf { it.bottom } - t)
}

// ---- quadros e legenda ----

fun PlotBoards.updateActive(transform: (Board) -> Board): PlotBoards {
    val current = active ?: return this
    val updated = transform(current)
    return if (updated == current) this else copy(boards = boards.map { if (it.id == current.id) updated else it })
}

fun PlotBoards.addBoard(id: String, name: String): PlotBoards =
    copy(boards = boards + Board(id = id, name = name), activeId = id)

fun PlotBoards.renameBoard(id: String, name: String): PlotBoards =
    copy(boards = boards.map { if (it.id == id) it.copy(name = name.trim().ifEmpty { it.name }) else it })

/** Remove o quadro; o ativo passa para o vizinho. */
fun PlotBoards.removeBoard(id: String): PlotBoards {
    val index = boards.indexOfFirst { it.id == id }
    if (index < 0) return this
    val rest = boards.filterNot { it.id == id }
    val nextActive = if (activeId == id || activeId == null) rest.getOrNull(index.coerceAtMost(rest.size - 1))?.id else activeId
    return copy(boards = rest, activeId = nextActive)
}

/** Garante ao menos um quadro (livros novos ou antigos sem `enredo.json`). */
fun PlotBoards.ensureBoard(id: () -> String, name: String): PlotBoards =
    if (boards.isEmpty()) addBoard(id(), name) else if (active?.id != activeId) copy(activeId = active?.id) else this

/** Próximo nome livre "Quadro N". */
fun PlotBoards.nextBoardName(): String {
    var n = boards.size + 1
    while (boards.any { it.name == "Quadro $n" }) n++
    return "Quadro $n"
}

fun PlotBoards.addLegendColor(): PlotBoards {
    val used = legend.map { it.color }.toSet()
    val color = BOARD_COLORS.firstOrNull { it !in used } ?: BOARD_COLORS[legend.size % BOARD_COLORS.size]
    return copy(legend = legend + LegendItem(color, "Nova relação"))
}

fun PlotBoards.renameLegend(index: Int, name: String): PlotBoards =
    copy(legend = legend.mapIndexed { i, item -> if (i == index) item.copy(name = name) else item })

fun PlotBoards.removeLegend(index: Int): PlotBoards = copy(legend = legend.filterIndexed { i, _ -> i != index })

fun PlotBoards.legendName(color: String?): String? = color?.let { c -> legend.firstOrNull { it.color.equals(c, true) }?.name }
