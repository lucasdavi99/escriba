package com.cuboide.escriba

import com.cuboide.escriba.app.EscribaController
import com.cuboide.escriba.app.EscribaServices
import com.cuboide.escriba.core.language.Concepts
import com.cuboide.escriba.core.language.LangStyle
import com.cuboide.escriba.core.language.Language
import com.cuboide.escriba.core.language.WordOrder
import com.cuboide.escriba.core.language.adaptForeignName
import com.cuboide.escriba.core.language.fnv1a64
import com.cuboide.escriba.core.language.form
import com.cuboide.escriba.core.language.history
import com.cuboide.escriba.core.language.meaningfulNames
import com.cuboide.escriba.core.language.translate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class LanguageTest {
    private val seed = fnv1a64("Livro de teste")

    @Test
    fun sameSeedSameLanguageAndGoldenOutput() {
        val a = Language(seed, LangStyle.Rio)
        val b = Language(seed, LangStyle.Rio)
        assertEquals(a.name, b.name)
        assertEquals(Concepts.all.map { a.protoWord(it.lemma) }, Concepts.all.map { b.protoWord(it.lemma) })
        assertEquals(a.grammar, b.grammar)
        // Saída travada: se o gerador mudar, as línguas dos livros mudariam junto. Mudou de propósito?
        // Então aumente Language.GENERATOR_VERSION e atualize estes valores.
        assertEquals("Ralame", a.name)
        assertEquals("PNYY-8KNS", a.code)
        assertEquals("lisi", a.protoWord("mar"))
        assertEquals("ora", a.protoWord("agua"))
        assertEquals(WordOrder.VSO, a.grammar.order)
        // Outra semente, outra língua.
        assertNotEquals(a.name, Language(seed + 1, LangStyle.Rio).name)
    }

    @Test
    fun wordsDoNotDependOnWhatWasAskedBefore() {
        val a = Language(seed, LangStyle.Pedra)
        val before = a.protoWord("lobo")
        repeat(50) { a.protoWord("coisa$it") }
        translate(a, a.changes.varieties(a.name).first(), "O dragão cantou nas montanhas.")
        assertEquals(before, Language(seed, LangStyle.Pedra).protoWord("lobo"))
        assertEquals(before, a.protoWord("lobo"))
    }

    @Test
    fun wordsUseOnlyTheLanguageSounds() {
        for (style in LangStyle.entries) {
            for (s in listOf(seed, 42L, -7L)) {
                val lang = Language(s, style)
                val allowed = (lang.consonants + lang.vowels).flatMap { it.toList() }.toSet()
                for (c in Concepts.all) {
                    val w = lang.protoWord(c.lemma)
                    assertTrue(w.isNotEmpty() && w.all { it in allowed }, "$style ${c.lemma}=$w")
                    assertFalse(Regex("[aeiou]{3}").containsMatchIn(w), "$style ${c.lemma}=$w (três vogais)")
                    assertEquals(w, lang.syllabify(w).joinToString(""), "$style sílabas de $w")
                }
            }
        }
    }

    @Test
    fun translatesWithGrammar() {
        val lang = Language(seed, LangStyle.Rio)
        val now = lang.changes.varieties(lang.name).first { it.id == "atual" }
        val t = translate(lang, now, "A rainha viu o mar.")
        assertEquals(listOf("rainha", "mar", "ver"), t.glosses.map { it.lemma }.sortedBy { listOf("rainha", "mar", "ver").indexOf(it) })
        val verb = t.glosses.first { it.lemma == "ver" }
        assertTrue("passado" in verb.meaning)
        // Ordem VSO: o verbo vem primeiro.
        assertEquals("ver", t.glosses.first().lemma)
        assertTrue(t.sentence.endsWith("."))
        assertTrue(t.newLemmas.isEmpty())

        val neg = translate(lang, now, "O lobo não temia a noite fria, e Helena cantou nas montanhas.")
        val temer = neg.glosses.first { it.lemma == "temer" }
        assertTrue("(não)" in temer.meaning && "(passado)" in temer.meaning)
        assertTrue(neg.glosses.any { it.meaning.startsWith("montanha") && it.isNew && "(plural)" in it.meaning && "(em)" in it.meaning })
        assertEquals(adaptForeignName(lang, "Helena"), neg.glosses.first { it.meaning == "Helena" }.word.replaceFirstChar { it.uppercase() })
        assertEquals(setOf("montanha"), neg.newLemmas)
        // Duas orações: o "e" vira uma palavra da língua entre elas.
        assertTrue(neg.glosses.any { it.lemma == "e" })
        // Palavra cunhada guardada continua a mesma.
        val kept = translate(lang, now, "Montanha.", mapOf("montanha" to "zuzu"))
        assertEquals(Language.tidy(now.form("zuzu", lang)), kept.glosses.single().word)
    }

    @Test
    fun namesForeignNamesAndVarieties() {
        val lang = Language(seed, LangStyle.Pedra)
        val people = meaningfulNames(lang, 0, places = false)
        assertEquals(8, people.size)
        assertEquals(people.size, people.map { it.name }.toSet().size)
        assertTrue(people.all { n -> n.name.any { it.lowercaseChar() !in "aeiou" } && " de " in n.meaning })
        assertNotEquals(people, meaningfulNames(lang, 1, places = false)) // "Outros nomes"
        assertEquals(people, meaningfulNames(lang, 0, places = false))

        val adapted = adaptForeignName(lang, "Christopher").lowercase()
        val allowed = (lang.consonants + lang.vowels).flatMap { it.toList() }.toSet()
        assertTrue(adapted.all { it in allowed }, adapted)

        val vars = lang.changes.varieties(lang.name)
        val proto = vars.first { it.id == "proto" }
        assertTrue(proto.history("kotrada").isEmpty())
        val familiar = vars.first { it.id == "familiar" }
        assertTrue(lang.syllabify(familiar.form("kotradakun", lang)).size < lang.syllabify(vars.first { it.id == "atual" }.form("kotradakun", lang)).size + 1)
        assertEquals(10, vars.size)
    }
}

class LanguageControllerTest {
    private val fs = InMemoryFileStore()
    private val clock = TestClock()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val services = EscribaServices(fs, "/lib", "/data", { clock.now }, clock::id, { "2026-11-12" })
    private val c = EscribaController(services, scope)

    @AfterTest
    fun tearDown() = scope.cancel()

    @Test
    fun bookLanguageIsSavedRerolledAndRemembersCoinedWords() {
        c.createBook("Livro de teste", "")
        c.createLanguage(LangStyle.Rio)
        val first = c.bookLanguage!!
        assertEquals("Ralame", first.name) // a semente vem do título
        c.setLanguageStyle(LangStyle.Pedra)
        assertEquals(seedOf(), fnv1a64("Livro de teste"))
        c.rerollLanguage()
        assertNotEquals(fnv1a64("Livro de teste"), seedOf())
        c.previousLanguage()
        assertEquals(fnv1a64("Livro de teste"), seedOf())

        val lang = c.bookLanguage!!
        val t = translate(lang, lang.changes.varieties(lang.name)[2], "A montanha dorme.")
        c.keepCoined(t)
        val path = c.book!!.path
        c.closeBook()
        c.openBook(path)
        assertEquals(LangStyle.Pedra, c.book!!.language?.style)
        assertEquals(setOf("montanha"), c.book!!.language?.coined?.keys)
    }

    private fun seedOf() = c.book!!.language!!.seed
}
