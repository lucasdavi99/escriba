package com.cuboide.escriba.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cuboide.escriba.core.board.Board
import com.cuboide.escriba.core.board.BoardNode
import com.cuboide.escriba.core.board.ImageFrame
import com.cuboide.escriba.core.board.NodeKind
import com.cuboide.escriba.core.board.NodeSizes
import com.cuboide.escriba.core.board.PlotBoards
import com.cuboide.escriba.core.board.Pt
import com.cuboide.escriba.core.board.Viewport
import com.cuboide.escriba.core.board.addBoard
import com.cuboide.escriba.core.board.addLegendColor
import com.cuboide.escriba.core.board.addNode
import com.cuboide.escriba.core.board.arrange
import com.cuboide.escriba.core.board.connect
import com.cuboide.escriba.core.board.duplicate
import com.cuboide.escriba.core.board.edge
import com.cuboide.escriba.core.board.moveNodes
import com.cuboide.escriba.core.board.nextBoardName
import com.cuboide.escriba.core.board.node
import com.cuboide.escriba.core.board.removeBoard
import com.cuboide.escriba.core.board.removeEdge
import com.cuboide.escriba.core.board.removeLegend
import com.cuboide.escriba.core.board.removeNodes
import com.cuboide.escriba.core.board.renameBoard
import com.cuboide.escriba.core.board.renameLegend
import com.cuboide.escriba.core.board.resize
import com.cuboide.escriba.core.board.updateActive
import com.cuboide.escriba.core.board.updateEdge
import com.cuboide.escriba.core.board.updateNode
import com.cuboide.escriba.core.universe.digest
import com.cuboide.escriba.platform.imagePixelSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** O que o Enredo mostra: o mapa livre ou a grade de cenas do roteiro. */
enum class EnredoView(val label: String) { Mapa("Mapa"), Roteiro("Roteiro") }

/**
 * Estado e ações do mapa de enredo (`enredo.json`) do livro aberto: seleção, desfazer/refazer e gravação com atraso
 * curto. Os dados ficam em [OpenBook.boards]; tudo o que muda passa por [edit].
 */
class PlotMapController internal constructor(
    private val owner: EscribaController,
    private val services: EscribaServices,
    private val scope: CoroutineScope,
) {
    val boards: PlotBoards? get() = owner.book?.boards
    val board: Board? get() = boards?.active

    /** Nós selecionados. Uma ligação selecionada exclui nós selecionados e vice-versa. */
    var selection by mutableStateOf<Set<String>>(emptySet()); private set
    var selectedEdge by mutableStateOf<String?>(null); private set
    /** Nó cujo texto está sendo editado no próprio quadro. */
    var editingId by mutableStateOf<String?>(null); private set
    /** Nó cuja imagem está sendo enquadrada (arrastar move, roda amplia). */
    var framingId by mutableStateOf<String?>(null); private set

    private val undoStack = ArrayDeque<PlotBoards>()
    private val redoStack = ArrayDeque<PlotBoards>()
    var canUndo by mutableStateOf(false); private set
    var canRedo by mutableStateOf(false); private set
    private var saveJob: Job? = null
    private var dirty = false

    // ---------- núcleo ----------

    /** Aplica a mudança, guarda o estado anterior para desfazer (se [undoable]) e agenda a gravação. */
    fun edit(undoable: Boolean = true, transform: (PlotBoards) -> PlotBoards) {
        val b = owner.book ?: return
        val before = b.boards
        val after = transform(before)
        if (after == before) return
        if (undoable) pushUndo(before)
        b.boards = after
        scheduleSave()
    }

    private fun editBoard(undoable: Boolean = true, transform: (Board) -> Board) = edit(undoable) { it.updateActive(transform) }

    private fun pushUndo(state: PlotBoards) {
        undoStack.addLast(state)
        while (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        redoStack.clear()
        syncHistory()
    }

    private fun syncHistory() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    fun undo() = step(undoStack, redoStack)

    fun redo() = step(redoStack, undoStack)

    private fun step(from: ArrayDeque<PlotBoards>, to: ArrayDeque<PlotBoards>) {
        val b = owner.book ?: return
        // Pontos de partida de edições que não mudaram nada (ex.: abrir e fechar um texto) não contam como passo.
        while (from.lastOrNull()?.sameContent(b.boards) == true) from.removeLast()
        val target = from.removeLastOrNull() ?: return syncHistory()
        to.addLast(b.boards)
        // Desfazer muda o conteúdo, não o quadro que se está olhando (se ele ainda existir).
        val keep = b.boards.activeId
        b.boards = if (target.boards.any { it.id == keep }) target.copy(activeId = keep) else target
        pruneSelection()
        syncHistory()
        scheduleSave()
    }

    /** Mesmo conteúdo, ignorando o quadro aberto e a posição da vista. */
    private fun PlotBoards.sameContent(other: PlotBoards): Boolean =
        legend == other.legend && boards.map { it.copy(viewport = Viewport()) } == other.boards.map { it.copy(viewport = Viewport()) }

    private fun pruneSelection() {
        val bd = board
        selection = selection.filter { bd?.node(it) != null }.toSet()
        if (editingId != null && bd?.node(editingId!!) == null) editingId = null
        if (framingId != null && bd?.node(framingId!!)?.image == null) framingId = null
        if (selectedEdge != null && bd?.edge(selectedEdge!!) == null) selectedEdge = null
    }

    private fun scheduleSave() {
        dirty = true
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(SAVE_DELAY_MS)
            flush()
        }
    }

    /** Grava já o que estiver pendente (ao sair da tela, trocar ou fechar o livro). */
    fun flush() {
        saveJob?.cancel()
        saveJob = null
        if (!dirty) return
        val b = owner.book ?: return
        dirty = false
        services.books.saveBoards(b.path, b.boards)
    }

    /** Chamado quando o livro aberto muda: zera histórico e seleção. */
    internal fun reset() {
        saveJob?.cancel()
        saveJob = null
        dirty = false
        undoStack.clear()
        redoStack.clear()
        syncHistory()
        selection = emptySet()
        selectedEdge = null
        editingId = null
        framingId = null
    }

    // ---------- seleção ----------

    fun select(id: String, additive: Boolean = false) {
        selectedEdge = null
        if (editingId != id) editingId = null
        if (framingId != id) framingId = null
        selection = when {
            !additive -> setOf(id)
            id in selection -> selection - id
            else -> selection + id
        }
    }

    fun selectEdge(id: String) {
        selection = emptySet()
        editingId = null
        framingId = null
        selectedEdge = id
    }

    fun clearSelection() {
        selection = emptySet()
        selectedEdge = null
        editingId = null
        framingId = null
    }

    // ---------- nós ----------

    private fun newNode(kind: NodeKind, center: Pt, width: Float, height: Float, title: String = "", image: String? = null) =
        BoardNode(
            id = services.newId(), kind = kind,
            x = center.x - width / 2, y = center.y - height / 2, width = width, height = height,
            title = title, image = image,
        )

    /** Cria um cartão centrado em [center] e o seleciona. Devolve a id. */
    fun addCard(center: Pt): String {
        val node = newNode(NodeKind.Cartao, center, NodeSizes.CARD_W, NodeSizes.CARD_H)
        editBoard { it.addNode(node) }
        select(node.id)
        editingId = node.id
        return node.id
    }

    fun addText(center: Pt): String {
        val node = newNode(NodeKind.Texto, center, NodeSizes.TEXT_W, NodeSizes.TEXT_H)
        editBoard { it.addNode(node) }
        select(node.id)
        editingId = node.id
        return node.id
    }

    /** Escolhe uma imagem e cria um nó de imagem com a proporção dela. */
    fun addImage(center: Pt) {
        scope.launch {
            val (relative, size) = pickImage("Imagem para o mapa") ?: return@launch
            val w = NodeSizes.IMAGE_W
            val h = (w * size.second / size.first.coerceAtLeast(1)).coerceIn(60f, 900f)
            val node = newNode(NodeKind.Imagem, center, w, h, image = relative)
            editBoard { it.addNode(node) }
            select(node.id)
        }
    }

    /** Escolhe a imagem do topo de um cartão (ou troca a de um nó de imagem). */
    fun chooseNodeImage(id: String) {
        scope.launch {
            val (relative, _) = pickImage("Imagem do cartão") ?: return@launch
            editBoard { bd ->
                bd.updateNode(id) { n ->
                    val grow = n.kind == NodeKind.Cartao && n.image == null && n.height < NodeSizes.CARD_WITH_IMAGE_H
                    n.copy(image = relative, height = if (grow) NodeSizes.CARD_WITH_IMAGE_H else n.height, frame = ImageFrame())
                }
            }
        }
    }

    fun removeNodeImage(id: String) {
        if (framingId == id) framingId = null
        editBoard { bd -> bd.updateNode(id) { it.copy(image = null, frame = ImageFrame()) } }
    }

    // ---------- enquadramento da imagem ----------

    /** Entra no modo de ajustar a imagem do nó; tudo o que se ajustar até sair vira um passo só de desfazer. */
    fun startFraming(id: String) {
        if (board?.node(id)?.image == null) return
        checkpoint()
        select(id)
        editingId = null
        framingId = id
    }

    fun stopFraming() {
        framingId = null
    }

    /** Enquadramento ao vivo (durante arraste e roda), fora do desfazer: o passo foi guardado em [startFraming]. */
    fun setFrame(id: String, frame: ImageFrame) = editBoard(undoable = false) { bd -> bd.updateNode(id) { it.copy(frame = frame) } }

    /** Altura da imagem no cartão, como fração da altura do cartão. */
    fun setImageRatio(id: String, ratio: Float) = editBoard(undoable = false) { bd ->
        bd.updateNode(id) { it.copy(imageRatio = ratio.coerceIn(ImageFrame.MIN_CARD_RATIO, ImageFrame.MAX_CARD_RATIO)) }
    }

    fun resetFrame(id: String) = editBoard(undoable = framingId != id) { bd ->
        bd.updateNode(id) { it.copy(frame = ImageFrame(), imageRatio = ImageFrame.DEFAULT_CARD_RATIO) }
    }

    /** Nó de imagem: altura volta a seguir a proporção da imagem, que aparece inteira. */
    fun fitNodeToImage(id: String) {
        val b = owner.book ?: return
        val node = board?.node(id) ?: return
        val bytes = node.image?.let { services.books.readAsset(b.path, it) } ?: return
        val (w, h) = imagePixelSize(bytes) ?: return
        editBoard { bd -> bd.updateNode(id) { it.copy(height = it.width * h / w.coerceAtLeast(1), frame = ImageFrame()) } }
    }

    private suspend fun pickImage(title: String): Pair<String, Pair<Int, Int>>? = owner.pickAndImportImage(title)

    fun moveNodes(ids: Set<String>, dx: Float, dy: Float) = editBoard { it.moveNodes(ids, dx, dy) }

    fun resizeNode(id: String, width: Float, height: Float) = editBoard { it.resize(id, width, height) }

    /** Guarda o estado atual no desfazer antes de uma série de mudanças sem desfazer próprio (digitação). */
    fun checkpoint() {
        owner.book?.let { pushUndo(it.boards) }
    }

    /** Começa a editar o texto do nó: um único passo de desfazer cobre toda a digitação. */
    fun startEditing(id: String) {
        if (board?.node(id) == null || editingId == id) return
        checkpoint()
        select(id)
        framingId = null
        editingId = id
    }

    fun stopEditing() {
        editingId = null
    }

    fun setNodeText(id: String, title: String, body: String) =
        editBoard(undoable = false) { bd -> bd.updateNode(id) { it.copy(title = title, body = body) } }

    /** Cor dos nós selecionados ou da ligação selecionada. */
    fun setSelectionColor(color: String?) {
        val edgeId = selectedEdge
        if (edgeId != null) editBoard { it.updateEdge(edgeId) { e -> e.copy(color = color) } }
        else if (selection.isNotEmpty()) editBoard { bd -> selection.fold(bd) { acc, id -> acc.updateNode(id) { it.copy(color = color) } } }
    }

    fun duplicateSelection() {
        if (selection.isEmpty()) return
        var created = emptySet<String>()
        editBoard { bd -> bd.duplicate(selection, services.newId).also { created = it.second }.first }
        if (created.isNotEmpty()) { selection = created; selectedEdge = null }
    }

    fun deleteSelection() {
        val edgeId = selectedEdge
        when {
            edgeId != null -> editBoard { it.removeEdge(edgeId) }
            selection.isNotEmpty() -> editBoard { it.removeNodes(selection) }
            else -> return
        }
        clearSelection()
    }

    fun deleteNode(id: String) {
        editBoard { it.removeNodes(setOf(id)) }
        selection = selection - id
    }

    /** Vincula (ou desvincula, com `null`) uma ficha do Universo; título e texto vazios vêm da ficha. */
    fun linkEntry(nodeId: String, entryId: String?) {
        val entry = entryId?.let { id -> owner.book?.universe?.entries?.firstOrNull { it.id == id } }
        editBoard { bd ->
            bd.updateNode(nodeId) { n ->
                if (entry == null) n.copy(entryId = null)
                else n.copy(
                    entryId = entry.id,
                    title = n.title.ifBlank { entry.name },
                    body = n.body.ifBlank { entry.digest() },
                )
            }
        }
    }

    // ---------- ligações ----------

    /** Liga dois nós; devolve falso se a ligação já existia ou não faz sentido. */
    fun connect(from: String, to: String): Boolean {
        val bd = board ?: return false
        val id = services.newId()
        val updated = bd.connect(from, to, id) ?: return false
        editBoard { updated }
        selectEdge(id)
        return true
    }

    /** Solta uma ligação no vazio: cria ali um cartão já ligado ao nó de origem. */
    fun connectToNewCard(from: String, center: Pt) {
        val bd = board ?: return
        if (bd.node(from) == null) return
        val node = newNode(NodeKind.Cartao, center, NodeSizes.CARD_W, NodeSizes.CARD_H)
        val edgeId = services.newId()
        editBoard { it.addNode(node).connect(from, node.id, edgeId) ?: it }
        select(node.id)
        editingId = node.id
    }

    fun setEdgeLabel(id: String, label: String, undoable: Boolean = true) =
        editBoard(undoable) { bd -> bd.updateEdge(id) { it.copy(label = label) } }

    // ---------- quadro inteiro ----------

    fun arrange() = editBoard { it.arrange() }

    /** Pan e zoom: gravados, mas fora do desfazer. */
    fun setViewport(viewport: Viewport) = editBoard(undoable = false) { it.copy(viewport = viewport) }

    fun selectBoard(id: String) {
        if (boards?.activeId == id) return
        clearSelection()
        edit(undoable = false) { it.copy(activeId = id) }
    }

    fun addBoard() {
        clearSelection()
        edit { it.addBoard(services.newId(), it.nextBoardName()) }
    }

    fun renameBoard(id: String, name: String) = edit { it.renameBoard(id, name) }

    fun removeBoard(id: String) {
        clearSelection()
        edit { bs ->
            val rest = bs.removeBoard(id)
            if (rest.boards.isEmpty()) rest.addBoard(services.newId(), "Quadro 1") else rest
        }
    }

    // ---------- legenda ----------

    fun addLegendColor() = edit { it.addLegendColor() }

    fun renameLegend(index: Int, name: String) = edit(undoable = false) { it.renameLegend(index, name) }

    fun removeLegend(index: Int) = edit { it.removeLegend(index) }

    companion object {
        const val SAVE_DELAY_MS = 400L
        const val MAX_HISTORY = 100
    }
}
