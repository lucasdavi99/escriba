package com.cuboide.escriba

import com.cuboide.escriba.core.model.EntryAttribute
import com.cuboide.escriba.core.model.EntryKind
import com.cuboide.escriba.core.model.UniverseEntry
import com.cuboide.escriba.core.universe.ChapterText
import com.cuboide.escriba.core.universe.FactKind
import com.cuboide.escriba.core.universe.SealStatus
import com.cuboide.escriba.core.universe.attributeTerms
import com.cuboide.escriba.core.universe.checkBook
import com.cuboide.escriba.core.universe.extractFacts
import com.cuboide.escriba.core.universe.relationIssues
import com.cuboide.escriba.core.universe.sealSuggestions
import com.cuboide.escriba.core.universe.withAttr
import com.cuboide.escriba.core.universe.withBlock
import kotlin.test.Test
import kotlinx.coroutines.cancel
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContradictionTest {
    private var n = 0
    private val id = { "id${++n}" }
    private fun facts(text: String, narrative: Boolean = false) = extractFacts(text, narrative).map { it.kind to it.value }

    @Test
    fun extractsFactsWithoutMixingClauses() {
        // A frase em que o Autor junta tudo ("cabelo ruivo / azul") e esquece "canhoto".
        assertEquals(
            listOf(FactKind.Porte to "alto", FactKind.Porte to "magro", FactKind.Olhos to "azul", FactKind.Cabelo to "ruivo", FactKind.Mao to "canhoto"),
            facts("Alto e magro, olhos azuis e cabelo ruivo, canhoto."),
        )
        assertEquals(listOf(FactKind.Cabelo to "loiro", FactKind.Cabelo to "cacheado"), facts("Cabelos loiros cacheados"))
        assertEquals(listOf(FactKind.Pele to "sardenta"), facts("a pele era sardenta"))
        assertEquals(listOf(FactKind.Olhos to "cinza"), facts("De olhos cinzentos."))
        assertEquals(listOf(FactKind.Cabelo to "careca"), facts("Careca desde os vinte."))
        assertEquals(FactKind.Marca to "cicatriz no queixo", facts("Tem uma cicatriz no queixo.").single())
        // No texto do livro, altura e corpo só com verbo de estado: "falou alto" não é altura.
        assertTrue(facts("Ela falou alto.", narrative = true).isEmpty())
        assertEquals(listOf(FactKind.Porte to "baixo"), facts("Ele era muito baixo.", narrative = true))
    }

    @Test
    fun attributeValuesAreReadTheSameWay() {
        assertEquals(setOf("verde"), attributeTerms(FactKind.Olhos, "Verdes"))
        assertEquals(setOf("ruivo", "curto"), attributeTerms(FactKind.Cabelo, "ruivo e curto"))
        assertEquals(setOf("canhoto"), attributeTerms(FactKind.Mao, "canhota"))
        assertEquals(setOf("alto", "magro"), attributeTerms(FactKind.Porte, "alto e magro"))
    }

    private fun mara(sealEyes: String? = "verdes"): UniverseEntry {
        var e = UniverseEntry("mara", EntryKind.Personagem, "Mara").withAttr("apelidos", "a Capitã", id)
        if (sealEyes != null) e = e.copy(attributes = e.attributes + EntryAttribute("o", "olhos", "Olhos", sealEyes, sealed = true))
        return e
    }

    @Test
    fun sealedFactsAreCheckedAgainstTheText() {
        val tobias = UniverseEntry("tob", EntryKind.Personagem, "Tobias")
        val chapters = listOf(
            ChapterText("c1", "Um", "Mara abriu os olhos verdes. Tobias tinha olhos azuis."),
            ChapterText("c2", "Dois", "A Capitã piscou os olhos azuis. Ela não tinha olhos castanhos."),
        )
        val check = checkBook(listOf(mara(), tobias), chapters)
        val c = check.contradictions.single() // Tobias não tem selo e a negação é ignorada
        assertEquals("mara", c.entryId)
        assertEquals("c2", c.spot.chapterId)
        assertEquals("azul", c.said)
        assertEquals("verdes", c.expected)
        assertNull(c.source)
        assertEquals("olhos azuis", chapters[1].text.substring(c.spot.start, c.spot.end))
        assertEquals(1, check.sealedFacts)
        assertEquals(1, check.sealedEntries)
    }

    @Test
    fun withoutSealTheTextIsCheckedAgainstItself() {
        val chapters = listOf(
            ChapterText("c1", "Um", "Mara prendeu o cabelo ruivo. Mara era canhota."),
            ChapterText("c2", "Dois", "O vento soltou o cabelo curto de Mara. Mais tarde, ela soltou o cabelo loiro. Mara era destra."),
        )
        val check = checkBook(listOf(mara(sealEyes = null)), chapters)
        // "curto" é forma, não cor: não contradiz "ruivo". "loiro" contradiz (pelo pronome "ela"). "destra" contradiz "canhota".
        assertEquals(listOf("loiro" to "ruivo", "destro" to "canhoto"), check.contradictions.map { it.said to it.expected })
        check.contradictions.forEach { assertEquals("c1", it.source?.chapterId) }
    }

    @Test
    fun agesAreListedNotAccused() {
        val check = checkBook(listOf(mara()), listOf(ChapterText("c1", "Um", "Mara tinha 34 anos. Anos depois, Mara fez quarenta e dois anos.")))
        assertEquals(listOf(34, 42), check.ages.map { it.age })
        assertTrue(check.contradictions.isEmpty())
    }

    @Test
    fun alliesAndEnemiesMustAgree() {
        val a = UniverseEntry("a", EntryKind.Personagem, "Mara").withBlock("aliados", "Tobias\nOdete", id).withBlock("inimigos", "Odete", id)
        val b = UniverseEntry("b", EntryKind.Personagem, "Tobias").withBlock("inimigos", "Mara", id)
        val issues = relationIssues(listOf(a, b)).map { it.message }
        assertEquals(
            listOf("Odete está em Aliados e em Inimigos de Mara.", "Na ficha de Mara, Tobias é aliado; na ficha de Tobias, Mara é inimigo."),
            issues,
        )
    }

    @Test
    fun physicalDescriptionSuggestsSeals() {
        val e = UniverseEntry("e", EntryKind.Personagem, "Mara")
            .withBlock("aparencia", "Alta, olhos azuis e cabelo ruivo; canhota.", id)
            .withAttr("olhos", "verdes", id)
            .withAttr("cabelo", "ruivo", id)
        val s = sealSuggestions(e).associateBy { it.kind }
        assertEquals(SealStatus.Conflito, s.getValue(FactKind.Olhos).status)
        assertEquals(SealStatus.Igual, s.getValue(FactKind.Cabelo).status)
        assertEquals(SealStatus.Novo, s.getValue(FactKind.Mao).status)
        assertEquals(listOf("alto"), s.getValue(FactKind.Porte).values)
    }
}

class ContradictionControllerTest {
    private val fs = InMemoryFileStore()
    private val clock = TestClock()
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
    private val services = com.cuboide.escriba.app.EscribaServices(fs, "/lib", "/data", { clock.now }, clock::id, { "2026-11-12" })
    private val c = com.cuboide.escriba.app.EscribaController(services, scope)

    @kotlin.test.AfterTest
    fun tearDown() = scope.cancel()

    @Test
    fun sealingFromTheDescriptionAndCheckingTheBook() {
        c.createBook("Livro", "")
        c.onEditorChange(androidx.compose.ui.text.input.TextFieldValue("Mara abriu os olhos azuis. Ela era canhota."))
        c.flushSave()
        val mara = c.createEntry(EntryKind.Personagem).copy(name = "Mara")
            .withBlock("aparencia", "Olhos verdes, cabelo ruivo; canhota.", c::newId)
            .withAttr("olhos", "castanhos", c::newId)
        c.saveEntry(mara)

        // Olhos: conflito (a ficha diz castanhos). Sem escolher, só cabelo e mão entram e são selados.
        c.sealFromDescription(mara.id)
        var saved = c.book!!.universe.entries.single()
        assertEquals(listOf("cabelo", "mao"), saved.attributes.filter { it.sealed }.mapNotNull { it.key }.sorted())
        assertEquals("castanhos", saved.attributes.first { it.key == "olhos" }.value)
        // Escolhendo ficar com a descrição, olhos vira "verde" e é selado.
        c.sealFromDescription(mara.id, setOf(FactKind.Olhos))
        saved = c.book!!.universe.entries.single()
        assertEquals("verde", saved.attributes.first { it.key == "olhos" }.value)
        assertTrue(saved.attributes.first { it.key == "olhos" }.sealed)

        // Desligada: nada sublinhado. Ligada: o texto diz azuis e a ficha diz verde.
        assertTrue(c.currentIssues.isEmpty())
        c.setCheckContradictions(true)
        assertEquals(1, c.bookCheck!!.contradictions.size)
        val issue = c.currentIssues.single()
        assertEquals("olhos azuis", c.editor.text.substring(issue.start, issue.end))
        // Tirar o selo dos olhos: não há mais contra o que conferir.
        c.toggleSeal(mara.id, "olhos")
        assertTrue(c.runCheck()!!.contradictions.isEmpty())
        c.setCheckContradictions(false)
        assertNull(c.bookCheck)
    }
}
