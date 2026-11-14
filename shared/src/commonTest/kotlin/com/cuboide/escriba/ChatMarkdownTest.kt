package com.cuboide.escriba

import com.cuboide.escriba.core.ai.chaptersNamedIn
import com.cuboide.escriba.core.model.ChapterKind
import com.cuboide.escriba.core.model.ChapterMeta
import com.cuboide.escriba.core.text.MdBlock
import com.cuboide.escriba.core.text.MdRun
import com.cuboide.escriba.core.text.chatMarkdownToManuscript
import com.cuboide.escriba.core.text.parseChatMarkdown
import com.cuboide.escriba.core.text.parseInlineMarkdown
import kotlin.test.Test
import kotlin.test.assertEquals

class ChatMarkdownTest {
    @Test
    fun blocksOfATypicalAnswer() {
        val md = """
            No contexto tenho o **Prólogo** inteiro.

            ---

            ### Análise do **Prólogo**

            **O que funciona:**
            * **Ritmo:** a abertura prende.
            * **Gancho:** a frase *"Mesmo tendo entregado tudo"* planta um mistério.
              * *"pós vida"* → **pós-vida**.
            1. Primeiro
            2) Segundo
            > Uma citação
            ```
            código cru **sem** ênfase
            ```
        """.trimIndent()
        val blocks = parseChatMarkdown(md)
        assertEquals(
            listOf(
                MdBlock.Paragraph("No contexto tenho o **Prólogo** inteiro."),
                MdBlock.Rule,
                MdBlock.Heading(3, "Análise do **Prólogo**"),
                MdBlock.Paragraph("**O que funciona:**"),
                MdBlock.ListItem("**Ritmo:** a abertura prende.", 0, "•"),
                MdBlock.ListItem("**Gancho:** a frase *\"Mesmo tendo entregado tudo\"* planta um mistério.", 0, "•"),
                MdBlock.ListItem("*\"pós vida\"* → **pós-vida**.", 1, "•"),
                MdBlock.ListItem("Primeiro", 0, "1."),
                MdBlock.ListItem("Segundo", 0, "2."),
                MdBlock.Quote("Uma citação"),
                MdBlock.Code("código cru **sem** ênfase"),
            ),
            blocks,
        )
    }

    @Test
    fun inlineEmphasis() {
        assertEquals(
            listOf(MdRun("o correto é "), MdRun("pós-vida", bold = true), MdRun(".")),
            parseInlineMarkdown("o correto é **pós-vida**."),
        )
        assertEquals(
            listOf(MdRun("a "), MdRun("\"Eu morri?\"", italic = true), MdRun(" e "), MdRun("x", bold = true, italic = true)),
            parseInlineMarkdown("a *\"Eu morri?\"* e ***x***"),
        )
        // Negrito com itálico dentro.
        assertEquals(
            listOf(MdRun("Nota ", bold = true), MdRun("aqui", bold = true, italic = true)),
            parseInlineMarkdown("**Nota *aqui***"),
        )
        assertEquals(listOf(MdRun("use "), MdRun("@", code = true)), parseInlineMarkdown("use `@`"))
        // Asterisco solto e sublinhado no meio da palavra ficam literais.
        assertEquals(listOf(MdRun("2 * 3 * 4 e nome_de_arquivo")), parseInlineMarkdown("2 * 3 * 4 e nome_de_arquivo"))
    }

    @Test
    fun answerBecomesManuscriptText() {
        val md = "### Título\n\nUm parágrafo\nquebrado.\n\n---\n\n- item **forte**\n1. numerado\n> citação"
        assertEquals(
            "### Título\nUm parágrafo quebrado.\n* item **forte**\n* 1. numerado\n> citação",
            chatMarkdownToManuscript(md),
        )
    }

    @Test
    fun chaptersCitedInTheRequest() {
        val chapters = listOf(
            ChapterMeta("p", "Recomeço", kind = ChapterKind.Prologo),
            ChapterMeta("c1", "Capítulo 1"),
            ChapterMeta("c2", "Novo Dia"),
            ChapterMeta("e", "Fim", kind = ChapterKind.Epilogo),
        )
        assertEquals(listOf("c2"), chaptersNamedIn("Analise o capítulo 2 por inteiro", chapters))
        assertEquals(listOf("c1", "c2"), chaptersNamedIn("compare o cap. 1 com o cap 2", chapters))
        assertEquals(listOf("p"), chaptersNamedIn("O que acha do prologo?", chapters))
        assertEquals(listOf("c2"), chaptersNamedIn("revise \"Novo dia\"", chapters))
        assertEquals(listOf("e"), chaptersNamedIn("melhore o epílogo", chapters))
        assertEquals(emptyList(), chaptersNamedIn("capítulo 9 não existe; capitulação também não", chapters))
    }
}
