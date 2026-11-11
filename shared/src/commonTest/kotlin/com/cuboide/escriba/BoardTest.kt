package com.cuboide.escriba

import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.EscribaServices
import com.cuboide.escriba.core.board.Board
import com.cuboide.escriba.core.board.BoardNode
import com.cuboide.escriba.core.board.NodeKind
import com.cuboide.escriba.core.board.PlotBoards
import com.cuboide.escriba.core.board.Pt
import com.cuboide.escriba.core.board.Side
import com.cuboide.escriba.core.board.arrange
import com.cuboide.escriba.core.board.autoSides
import com.cuboide.escriba.core.board.connect
import com.cuboide.escriba.core.board.curveOf
import com.cuboide.escriba.core.board.duplicate
import com.cuboide.escriba.core.board.hitEdge
import com.cuboide.escriba.core.board.nodeAt
import com.cuboide.escriba.core.board.rect
import com.cuboide.escriba.core.board.removeBoard
import com.cuboide.escriba.core.board.removeNodes
import com.cuboide.escriba.core.board.resize
import com.cuboide.escriba.core.model.EntryKind
import com.cuboide.escriba.core.storage.BookRepository
import com.cuboide.escriba.core.storage.joinPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BoardTest {
    private fun card(id: String, x: Float, y: Float, w: Float = 200f, h: Float = 120f) =
        BoardNode(id, NodeKind.Cartao, x, y, w, h, title = id)

    private fun board(vararg nodes: BoardNode) = Board("b", "Quadro", nodes.toList())

    @Test
    fun removingNodeTakesItsEdges() {
        val b = board(card("a", 0f, 0f), card("b", 400f, 0f), card("c", 800f, 0f))
            .connect("a", "b", "e1")!!.connect("b", "c", "e2")!!
        val after = b.removeNodes(setOf("b"))
        assertEquals(listOf("a", "c"), after.nodes.map { it.id })
        assertTrue(after.edges.isEmpty())
    }

    @Test
    fun connectRefusesSelfDuplicatesAndMissingNodes() {
        val b = board(card("a", 0f, 0f), card("b", 400f, 0f)).connect("a", "b", "e1")!!
        assertNull(b.connect("a", "a", "x"))
        assertNull(b.connect("a", "b", "x"))
        assertNull(b.connect("b", "a", "x"), "a mesma ligação no outro sentido também é repetida")
        assertNull(b.connect("a", "zz", "x"))
    }

    @Test
    fun duplicateCopiesNodesAndInnerEdgesOnly() {
        var n = 0
        val b = board(card("a", 0f, 0f), card("b", 400f, 0f), card("c", 0f, 400f))
            .connect("a", "b", "e1")!!.connect("a", "c", "e2")!!
        val (after, created) = b.duplicate(setOf("a", "b"), { "n${++n}" })
        assertEquals(5, after.nodes.size)
        assertEquals(2, created.size)
        val copyA = after.nodes.first { it.id in created && it.title == "a" }
        assertEquals(24f, copyA.x)
        // Só a ligação a–b (as duas pontas copiadas) foi copiada.
        assertEquals(3, after.edges.size)
        assertTrue(after.edges.any { it.from in created && it.to in created })
    }

    @Test
    fun resizeRespectsMinimum() {
        val b = board(card("a", 0f, 0f)).resize("a", 10f, 10f)
        assertTrue(b.nodes[0].width >= 140f && b.nodes[0].height >= 80f)
    }

    @Test
    fun anchorsPickFacingSides() {
        val a = card("a", 0f, 0f)
        assertEquals(Side.Direita to Side.Esquerda, autoSides(a, card("b", 500f, 20f)))
        assertEquals(Side.Esquerda to Side.Direita, autoSides(a, card("b", -500f, 0f)))
        assertEquals(Side.Base to Side.Topo, autoSides(a, card("b", 30f, 400f)))
        assertEquals(Side.Topo to Side.Base, autoSides(a, card("b", 0f, -400f)))
    }

    @Test
    fun edgeHitTestFollowsTheCurve() {
        val b = board(card("a", 0f, 0f), card("b", 500f, 0f)).connect("a", "b", "e1")!!
        val mid = b.curveOf(b.edges[0])!!.mid
        assertEquals("e1", b.hitEdge(mid, 6f)?.id)
        assertEquals("e1", b.hitEdge(Pt(mid.x, mid.y + 4f), 6f)?.id)
        assertNull(b.hitEdge(Pt(mid.x, mid.y + 80f), 6f))
        assertEquals("b", b.nodeAt(Pt(510f, 10f))?.id)
        assertNull(b.nodeAt(Pt(510f, 10f), except = "b"))
    }

    @Test
    fun arrangeMakesLayersWithoutOverlap() {
        // a → b → c, a → d; e solto. Todos começam empilhados no mesmo lugar.
        val b = board(card("a", 0f, 0f), card("b", 5f, 5f), card("c", 10f, 10f), card("d", 15f, 15f), card("e", 20f, 20f))
            .connect("a", "b", "1")!!.connect("b", "c", "2")!!.connect("a", "d", "3")!!
        val after = b.arrange()
        val byId = after.nodes.associateBy { it.id }
        for (i in after.nodes.indices) for (j in i + 1 until after.nodes.size) {
            assertFalse(after.nodes[i].rect.overlaps(after.nodes[j].rect), "${after.nodes[i].id} sobre ${after.nodes[j].id}")
        }
        assertTrue(byId.getValue("a").x < byId.getValue("b").x)
        assertTrue(byId.getValue("b").x < byId.getValue("c").x)
        assertEquals(byId.getValue("b").x, byId.getValue("d").x, "b e d estão na mesma camada")
        assertTrue(byId.getValue("e").y > byId.getValue("c").bottom, "nós soltos vão para baixo")
    }

    @Test
    fun removingActiveBoardActivatesNeighbour() {
        val bs = PlotBoards(listOf(Board("1", "A"), Board("2", "B"), Board("3", "C")), activeId = "2")
        assertEquals("3", bs.removeBoard("2").activeId)
        assertEquals("2", bs.removeBoard("3").activeId)
    }
}

class PlotMapControllerTest {
    private val fs = InMemoryFileStore()
    private val clock = TestClock()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val services = EscribaServices(fs, "/lib", "/data", { clock.now }, clock::id, { "2026-11-11" })
    private val c = EscribaController(services, scope)

    @AfterTest
    fun tearDown() = scope.cancel()

    private fun onDisk(): PlotBoards = services.books.loadBoards(c.book!!.path)

    @Test
    fun newBookStartsWithOneBoardAndSavesEdits() {
        c.createBook("Livro", "")
        assertEquals(1, c.map.boards!!.boards.size)
        assertEquals("Quadro 1", c.map.board!!.name)
        val a = c.map.addCard(Pt(0f, 0f))
        c.map.setNodeText(a, "Cena do baile", "Ela chega tarde.")
        c.map.flush()
        val saved = onDisk().active!!
        assertEquals("Cena do baile", saved.nodes.single().title)
        assertEquals("Ela chega tarde.", saved.nodes.single().body)
    }

    @Test
    fun undoRedoAndTextEditIsOneStep() {
        c.createBook("Livro", "")
        val a = c.map.addCard(Pt(0f, 0f))
        val b = c.map.addCard(Pt(500f, 0f))
        assertTrue(c.map.connect(a, b))
        assertFalse(c.map.connect(b, a))
        c.map.startEditing(a)
        c.map.setNodeText(a, "A", "")
        c.map.setNodeText(a, "Ab", "")
        c.map.setNodeText(a, "Abc", "")
        c.map.stopEditing()

        c.map.undo() // toda a digitação
        assertEquals("", c.map.board!!.nodes.first { it.id == a }.title)
        c.map.undo() // a ligação
        assertTrue(c.map.board!!.edges.isEmpty())
        c.map.redo()
        assertEquals(1, c.map.board!!.edges.size)
        c.map.redo()
        assertEquals("Abc", c.map.board!!.nodes.first { it.id == a }.title)
        assertFalse(c.map.canRedo)
    }

    @Test
    fun deleteSelectionAndConnectToNewCard() {
        c.createBook("Livro", "")
        val a = c.map.addCard(Pt(0f, 0f))
        c.map.connectToNewCard(a, Pt(600f, 0f))
        assertEquals(2, c.map.board!!.nodes.size)
        assertEquals(1, c.map.board!!.edges.size)
        val created = c.map.selection.single()
        assertEquals(created, c.map.editingId, "o cartão novo já abre para escrever")
        c.map.deleteSelection()
        assertEquals(listOf(a), c.map.board!!.nodes.map { it.id })
        assertTrue(c.map.board!!.edges.isEmpty())
    }

    @Test
    fun linkingEntryFillsBlankCardAndOpensUniverse() {
        c.createBook("Livro", "")
        val entry = c.createEntry(EntryKind.Personagem)
        c.saveEntry(entry.copy(name = "Fenalia", summary = "Rainha de Cerulean"))
        val a = c.map.addCard(Pt(0f, 0f))
        c.map.linkEntry(a, entry.id)
        val node = c.map.board!!.nodes.single()
        assertEquals("Fenalia", node.title)
        assertEquals("Rainha de Cerulean", node.body)
        assertEquals(entry.id, node.entryId)

        c.openUniverseEntry(entry.id)
        assertEquals(com.cuboide.escriba.app.Section.Universo, c.section)
        assertEquals(entry.id, c.universeFocus)
    }

    @Test
    fun boardsSurviveReopenAndBrokenFileLoadsEmpty() {
        c.createBook("Livro", "")
        val path = c.book!!.path
        c.map.addBoard()
        c.map.addText(Pt(0f, 0f))
        c.map.addLegendColor()
        c.closeBook()
        c.openBook(path)
        assertEquals(2, c.map.boards!!.boards.size)
        assertEquals(6, c.map.boards!!.legend.size)
        assertEquals(NodeKind.Texto, c.map.board!!.nodes.single().kind)
        assertFalse(c.map.canUndo, "histórico zera ao reabrir")

        fs.writeText(joinPath(path, BookRepository.ENREDO), "{ quebrado")
        assertNotNull(services.books.loadBoards(path))
        c.closeBook()
        c.openBook(path)
        assertEquals(1, c.map.boards!!.boards.size, "arquivo corrompido: começa com um quadro vazio")
    }
}
