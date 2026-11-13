package com.cuboide.escriba

import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.EscribaServices
import com.cuboide.escriba.core.model.EntryImage
import com.cuboide.escriba.core.model.EntryKind
import com.cuboide.escriba.core.model.Universe
import com.cuboide.escriba.core.model.UniverseEntry
import com.cuboide.escriba.core.storage.TrashKind
import com.cuboide.escriba.core.storage.joinPath
import com.cuboide.escriba.core.universe.BookUniverse
import com.cuboide.escriba.core.universe.EntrySchemas
import com.cuboide.escriba.core.universe.aliases
import com.cuboide.escriba.core.universe.attr
import com.cuboide.escriba.core.universe.blockText
import com.cuboide.escriba.core.universe.copyEntries
import com.cuboide.escriba.core.universe.mergeSaga
import com.cuboide.escriba.core.universe.withAttr
import com.cuboide.escriba.core.universe.withBlock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SagaMergeTest {
    private var n = 0
    private val id = { "n${++n}" }

    @Test
    fun repeatedEntriesAreJoinedWithoutLosingAnything() {
        val mara1 = UniverseEntry("m1", EntryKind.Personagem, "Mara", subtitle = "Capitã")
            .withAttr("olhos", "verdes", id).withAttr(EntrySchemas.ALIASES, "a Capitã", id)
            .withBlock("aparencia", "Alta.", id)
        val mara2 = UniverseEntry("m2", EntryKind.Personagem, "MARA", subtitle = "Almirante")
            .withAttr("olhos", "verdes", id).withAttr("cabelo", "ruivo", id).withAttr(EntrySchemas.ALIASES, "Marinha, a capitã", id)
            .withBlock("aparencia", "Alta e magra.", id).withBlock("personalidade", "Calada.", id)
            .copy(tags = listOf("mar"), portrait = EntryImage("assets/m2.png"))
        val vale = UniverseEntry("v", EntryKind.Local, "Vale")
        val result = mergeSaga(
            listOf(BookUniverse("/a", "Livro Um", Universe(listOf(mara1))), BookUniverse("/b", "Livro Dois", Universe(listOf(mara2, vale)))),
            id,
        )
        assertEquals(1, result.joined)
        assertEquals(mapOf("m2" to "m1"), result.idMap)
        val (mara, local) = result.universe.entries
        assertEquals("m1", mara.id)
        assertEquals("Capitã", mara.subtitle)
        assertEquals("ruivo", mara.attr("cabelo"))                     // faltava: veio do outro livro
        assertEquals(listOf("a Capitã", "Marinha"), mara.aliases)      // apelidos se somam, sem repetir
        assertEquals("Calada.", mara.blockText("personalidade"))
        assertEquals("Alta.", mara.blockText("aparencia"))             // a base fica…
        val note = mara.blocks.single { it.key == null }               // …e o diferente vai para "Em Livro Dois"
        assertEquals("Em Livro Dois", note.title)
        assertTrue("Almirante" in note.text && "Alta e magra." in note.text)
        assertEquals(listOf("mar"), mara.tags)
        assertEquals("assets/m2.png", mara.portrait?.path)
        assertEquals("Vale", local.name)
    }

    @Test
    fun copiesGetNewIdsAndTheirImages() {
        val stored = mutableMapOf("assets/a.png" to byteArrayOf(1))
        val e = UniverseEntry("x", EntryKind.Personagem, "Mara", portrait = EntryImage("assets/a.png"), gallery = listOf(EntryImage("assets/a.png"), EntryImage("assets/sumiu.png")))
        var imported = 0
        val copy = copyEntries(listOf(e), { stored[it] }, { _, ext -> "assets/novo${++imported}.$ext" }, id).single()
        assertNotEquals("x", copy.id)
        assertEquals("assets/novo1.png", copy.portrait?.path)
        assertEquals(listOf("assets/novo1.png"), copy.gallery.map { it.path }) // mesma imagem: um arquivo só; a que sumiu cai
        assertEquals(1, imported)
    }
}

class AcervoControllerTest {
    private val fs = InMemoryFileStore()
    private val clock = TestClock()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val services = EscribaServices(fs, "/lib", "/data", { clock.now }, clock::id, { "2026-11-12" })
    private val c = EscribaController(services, scope)

    @AfterTest
    fun tearDown() = scope.cancel()

    @Test
    fun acervoUniversesLiveOutsideBooks() {
        c.createBook("Livro", "")
        val book = c.book!!.path
        val img = services.books.importAsset(book, byteArrayOf(9, 9), "png")
        c.saveEntry(c.createEntry(EntryKind.Personagem).copy(name = "Mara", portrait = EntryImage(img)))

        val acervo = c.importBookToAcervo()!!
        assertEquals(1, c.acervoList.single().entries)
        c.openAcervoUniverse(acervo.path)
        val copy = c.universeEntries.single()
        assertNotEquals(img, copy.portrait?.path)
        assertTrue(fs.exists(joinPath(acervo.path, copy.portrait!!.path)))

        // Editar no Acervo não mexe no livro.
        c.saveEntry(copy.copy(name = "Mara do Acervo"))
        c.saveEntry(c.createEntry(EntryKind.Local).copy(name = "Vale"))
        assertEquals(listOf("Mara"), services.books.loadUniverse(book).entries.map { it.name })
        assertEquals(listOf("Mara do Acervo", "Vale"), services.books.loadUniverse(acervo.path).entries.map { it.name })

        // Lixeira de fichas do Acervo: vai e volta para o Acervo.
        c.trashEntry(c.universeEntries.first { it.name == "Vale" })
        assertEquals(listOf("Vale"), c.entryTrash.map { it.title })
        c.restoreFromTrash(c.entryTrash.single())
        assertTrue(services.books.loadUniverse(acervo.path).entries.any { it.name == "Vale" })

        // Voltar ao livro e trazer fichas do Acervo (com a imagem).
        c.closeAcervoUniverse()
        c.bringEntries(acervo.path, services.books.loadUniverse(acervo.path).entries.map { it.id }.toSet())
        assertEquals(listOf("Mara", "Mara do Acervo", "Vale"), c.book!!.universe.entries.map { it.name })
        val brought = c.book!!.universe.entries.first { it.name == "Mara do Acervo" }
        assertTrue(fs.exists(joinPath(book, brought.portrait!!.path)))

        // O universo inteiro vai para a lixeira e volta.
        c.trashAcervoUniverse(c.acervoList.single())
        assertTrue(c.acervoList.isEmpty())
        val item = c.trashItems.first { it.kind == TrashKind.Universo }
        c.restoreFromTrash(item)
        c.refreshAcervo()
        assertEquals(listOf("Livro — universo"), c.acervoList.map { it.meta.name })
    }

    @Test
    fun unifiedBooksShareEntriesAndStayInSync() {
        // Livro Dois: Mara com cabelo, uma cena com ela e um cartão vinculado.
        c.createBook("Livro Dois", "")
        val two = c.book!!.path
        val img = services.books.importAsset(two, byteArrayOf(7), "png")
        val maraTwo = c.createEntry(EntryKind.Personagem).copy(name = "Mara", portrait = EntryImage(img)).withAttr("cabelo", "ruivo", c::newId)
        c.saveEntry(maraTwo)
        c.saveBeat(c.newBeat().copy(title = "Cena", characterIds = listOf(maraTwo.id)))
        // Livro Um: outra Mara (será a base) e o Tobias.
        c.createBook("Livro Um", "")
        val one = c.book!!.path
        val maraOne = c.createEntry(EntryKind.Personagem).copy(name = "Mara").withAttr("olhos", "verdes", c::newId)
        c.saveEntry(maraOne)
        c.saveEntry(c.createEntry(EntryKind.Personagem).copy(name = "Tobias"))

        c.unifyBooks(listOf(two), "A Travessia")

        val u1 = services.books.loadUniverse(one).entries
        val u2 = services.books.loadUniverse(two).entries
        assertEquals(u1, u2)
        assertEquals(listOf("Mara", "Tobias"), u1.map { it.name })
        val mara = u1.first()
        assertEquals(maraOne.id, mara.id)
        assertEquals("ruivo", mara.attr("cabelo"))
        assertEquals("verdes", mara.attr("olhos"))
        assertTrue(fs.exists(joinPath(one, img)))                                   // a imagem do outro livro veio junto
        assertEquals(listOf(maraOne.id), services.books.loadOutline(two).beats.single().characterIds) // cena aponta para a ficha que ficou
        assertEquals("A Travessia", services.books.loadManifest(two).sagaName)
        assertTrue(fs.list("/data/backups").single().startsWith("saga-"))        // cópia de segurança antes de juntar
        val saga = c.acervoList.single { it.meta.saga }
        assertEquals(setOf(one, two), saga.meta.bookPaths.toSet())

        // Mudou num livro, muda nos outros e na saga.
        c.saveEntry(c.book!!.universe.entries.first().copy(subtitle = "Capitã"))
        assertEquals("Capitã", services.books.loadUniverse(two).entries.first().subtitle)
        assertEquals("Capitã", services.books.loadUniverse(saga.path).entries.first().subtitle)

        // Fora da saga, deixa de mudar junto.
        c.leaveSaga()
        assertNull(c.book!!.manifest.sagaId)
        c.saveEntry(c.book!!.universe.entries.first().copy(subtitle = "Só no Livro Um"))
        assertEquals("Capitã", services.books.loadUniverse(two).entries.first().subtitle)
    }
}
