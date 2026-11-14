package com.cuboide.escriba.core.ai

import com.cuboide.escriba.core.model.BookManifest
import com.cuboide.escriba.core.model.Outline
import com.cuboide.escriba.core.model.Universe
import com.cuboide.escriba.core.model.UniverseEntry
import com.cuboide.escriba.core.model.chapterNumbers
import com.cuboide.escriba.core.text.stripMarkup
import com.cuboide.escriba.core.model.EntryAttribute
import com.cuboide.escriba.core.model.EntryBlock
import com.cuboide.escriba.core.universe.schema
import com.cuboide.escriba.core.universe.fold
import com.cuboide.escriba.core.universe.findMentions
import com.cuboide.escriba.core.universe.searchTerms

/** Tudo o que o assistente pode consultar de um livro. [chapterBodies] tem o texto cru (com marcação) de cada capítulo. */
class BookContext(
    val manifest: BookManifest,
    val chapterBodies: Map<String, String>,
    val universe: Universe,
    val outline: Outline,
    val currentChapterId: String? = null,
)

/**
 * O que o autor pede ao assistente, em três camadas: [general] vale para tudo, [book] para este livro e [mode] para
 * esta conversa. Entram depois das regras fixas e mandam no estilo e no foco da resposta.
 */
data class AuthorInstructions(val general: String = "", val book: String = "", val mode: AiMode? = null)

/** Prompt de sistema pronto, com a lista do que foi anexado. [cutChapters]: capítulos encurtados no modo livro inteiro. */
class BuiltContext(val system: String, val used: List<ContextItem>, val tokens: Int, val cutChapters: Int = 0)

object ContextBuilder {
    private const val PIECE_CHARS = 800
    private const val CURRENT_CHAPTER_BOOST = 1.6
    private const val NAME_IN_QUERY_BOOST = 6.0
    private const val MENTIONED_IN_CHAPTER_BOOST = 1.5

    /**
     * Monta o contexto da pergunta [query] dentro de [budgetTokens]. Ordem de prioridade: o que o autor apontou ([refs]),
     * a estrutura do livro (sinopse e roteiro) e, com o que sobrar, os trechos mais relevantes ao pedido.
     * [others] são os outros livros que o autor pode apontar com `@`, pelo caminho.
     *
     * Com [fullBook] ("Livro inteiro" ligado), no lugar da busca entram todas as fichas e todos os capítulos, na ordem
     * do livro; se não couberem, os capítulos são encurtados por igual (os curtos vão inteiros).
     */
    fun build(
        book: BookContext,
        query: String,
        refs: List<AiRef>,
        budgetTokens: Int,
        others: Map<String, BookContext> = emptyMap(),
        fullBook: Boolean = false,
        instructions: AuthorInstructions = AuthorInstructions(),
    ): BuiltContext {
        val budgetChars = budgetTokens.coerceAtLeast(500) * 4
        val used = ArrayList<ContextItem>()
        val blocks = ArrayList<String>()
        var chars = 0
        val skipSources = HashSet<String>()

        fun add(label: String, ref: AiRef?, text: String): Boolean {
            if (text.isBlank()) return false
            val room = budgetChars - chars
            if (room <= 200) return false
            val body = if (text.length > room) text.take(room - 40).trimEnd() + "\n[…cortado por limite de contexto]" else text
            blocks += "### $label\n$body"
            used += ContextItem(label, ref)
            chars += body.length + label.length + 8
            return true
        }

        // 1) O que o autor apontou. Um trecho ocupa até metade do orçamento; um capítulo inteiro, até três quartos.
        val half = budgetChars / 2
        // Vários capítulos inteiros dividem esses três quartos.
        val chapterMax = budgetChars * 3 / 4 / refs.distinct().count { it is AiRef.Chapter }.coerceAtLeast(1)
        for (ref in refs.distinct()) {
            when (ref) {
                is AiRef.Selection -> add("Trecho selecionado", ref, cap(ref.text, half))
                is AiRef.Chapter -> {
                    if (book.manifest.chapters.none { it.id == ref.id }) continue
                    val text = stripMarkup(book.chapterBodies[ref.id].orEmpty())
                    if (add(chapterLabel(book, ref.id) + " (inteiro)", ref, cap(text, chapterMax))) skipSources += "c:${ref.id}"
                }
                is AiRef.Beat -> {
                    val beat = book.outline.beats.firstOrNull { it.id == ref.id } ?: continue
                    add("Cena: ${beat.title}", ref, beatText(book, beat.id))
                }
                is AiRef.Entry -> {
                    val entry = book.universe.entries.firstOrNull { it.id == ref.id } ?: continue
                    if (add(entryLabel(entry), ref, entryText(entry))) skipSources += "e:${entry.id}"
                }
                AiRef.Synopsis -> add("Sinopse", ref, book.manifest.synopsis)
                AiRef.Outline -> add("Enredo (roteiro completo)", ref, outlineText(book, withIds = false))
                is AiRef.Book -> {
                    val other = others[ref.path] ?: continue
                    add("Outro livro: ${other.manifest.title}", ref, otherBookText(other))
                }
            }
        }

        // 2) Estrutura fixa do livro.
        val m = book.manifest
        val header = buildString {
            append("Título: ${m.title}")
            if (m.author.isNotBlank()) append("\nAutor: ${m.author}")
            if (m.genre.isNotBlank()) append("\nGênero: ${m.genre}")
            if (m.synopsis.isNotBlank() && AiRef.Synopsis !in refs) append("\nSinopse: ${m.synopsis.trim()}")
        }
        val outline = if (AiRef.Outline !in refs) outlineText(book, withIds = false).take(budgetChars / 5) else ""
        val fixed = header + if (outline.isNotBlank()) "\n\nRoteiro (cenas na ordem da história):\n$outline" else ""
        val fixedBlock = "### Livro\n$fixed"
        blocks.add(0, fixedBlock)
        chars += fixedBlock.length

        // 3a) Livro inteiro: todas as fichas e todos os capítulos, em vez da busca.
        var cutChapters = 0
        if (fullBook) {
            for (entry in book.universe.entries) {
                if ("e:${entry.id}" !in skipSources) add(entryLabel(entry), AiRef.Entry(entry.id), entryText(entry))
            }
            val rest = book.manifest.chapters.filter { "c:${it.id}" !in skipSources }
                .map { it to stripMarkup(book.chapterBodies[it.id].orEmpty()).trim() }
                .filter { it.second.isNotEmpty() }
            val room = budgetChars - chars - rest.sumOf { chapterLabel(book, it.first.id).length + 40 }
            val allowance = fairShares(rest.map { it.second.length }, room.coerceAtLeast(0))
            // A divisão já respeita o orçamento: cada capítulo entra direto com a parte dele (sem a folga mínima de [add]).
            rest.forEachIndexed { i, (meta, text) ->
                val whole = text.length <= allowance[i]
                if (!whole) cutChapters++
                if (allowance[i] <= 0) return@forEachIndexed
                val label = chapterLabel(book, meta.id) + if (whole) " (inteiro)" else " (encurtado)"
                val body = cap(text, allowance[i])
                blocks += "### $label\n$body"
                used += ContextItem(label, AiRef.Chapter(meta.id))
                chars += body.length + label.length + 8
            }
        }

        // 3b) Trechos relevantes ao pedido, até encher o orçamento.
        val pieces = if (fullBook) emptyList() else buildPieces(book).filter { it.sourceKey !in skipSources }
        val scores = scorePieces(pieces, query).toMutableList()
        val queryFolded = fold(query)
        val currentMentions = currentChapterEntryIds(book)
        pieces.forEachIndexed { i, p ->
            var s = scores[i]
            if (p.sourceKey.startsWith("e:")) {
                val id = p.sourceKey.removePrefix("e:")
                val entry = book.universe.entries.firstOrNull { it.id == id }
                if (entry != null && entry.searchTerms().any { queryFolded.contains(fold(it)) }) s += NAME_IN_QUERY_BOOST
                if (id in currentMentions) s += MENTIONED_IN_CHAPTER_BOOST
            }
            if (p.sourceKey == "c:${book.currentChapterId}" && s > 0) s *= CURRENT_CHAPTER_BOOST
            scores[i] = s
        }
        // Passagens de capítulos e cenas só entram se baterem com pelo menos duas palavras do pedido (ou a única que
        // ele tem): uma palavra solta coincidindo por acaso puxava trechos de capítulos que o autor nem citou.
        val matched = matchedTermCounts(pieces, query)
        val needed = searchTokens(query).distinct().size.coerceAtMost(2)
        val ranked = pieces.indices
            .filter { scores[it] > 0 }
            .filter { pieces[it].sourceKey.startsWith("e:") || (needed > 0 && matched[it] >= needed) }
            .sortedByDescending { scores[it] }
        val addedFromEntries = HashSet<String>()
        for (i in ranked) {
            val p = pieces[i]
            if (p.sourceKey.startsWith("e:") && !addedFromEntries.add(p.sourceKey)) continue
            if (budgetChars - chars <= 400) break
            add(p.label, p.ref, p.text)
        }

        val system = buildString {
            appendLine("Você é um assistente de escrita dentro do Escriba, um editor de livros. Ajuda o autor a ter ideias, melhorar o texto e manter a história coerente.")
            appendLine("Regras:")
            appendLine("- Responda em português do Brasil, a menos que o autor peça outro idioma.")
            appendLine("- Use o contexto do livro abaixo. As fichas do Universo e o Enredo são a verdade da história: não os contradiga.")
            appendLine("- Se faltar informação no contexto, diga o que falta em vez de inventar fatos do livro.")
            appendLine("- Se precisar de um capítulo, cena ou ficha que não está no contexto, diga ao autor para citá-lo pelo nome ou apontá-lo com @ no pedido. Nunca peça para ele colar o texto.")
            appendLine("- Itens marcados \"(inteiro)\" são o texto completo; \"(passagem N)\" são só trechos achados por busca.")
            appendLine("- Pode usar Markdown simples na resposta (títulos, listas, **negrito**, *itálico*).")
            appendLine("- Quando propuser texto para o livro, entregue-o pronto para colar, no estilo e na voz do autor, sem explicações no meio dele.")
            appendLine("- Você só sugere: quem altera o livro é o autor.")
            if (fullBook) {
                val focus = refs.filterIsInstance<AiRef.Chapter>().firstOrNull()?.let { chapterLabel(book, it.id) }
                appendLine()
                appendLine("Modo livro inteiro: o autor ligou a leitura do livro todo. Você recebeu todos os capítulos e fichas" +
                    (if (cutChapters > 0) " (os marcados \"(encurtado)\" foram cortados por limite de contexto)." else "."))
                if (focus != null) appendLine("Capítulo em foco: $focus.")
                appendLine("Ao avaliar um capítulo, compare-o com o livro todo:")
                appendLine("- repetições: cenas, informações, explicações, frases, imagens e estruturas que já apareceram em outros capítulos;")
                appendLine("- coerência: enredo, personagens (comportamento, conhecimento, aparência), tom, ponto de vista e linha do tempo;")
                appendLine("- função: o que o capítulo acrescenta à história e se ele avança o enredo ou só repete o que o leitor já sabe.")
                appendLine("Cite o capítulo e o trecho sempre que apontar algo.")
            }
            val general = instructions.general.trim()
            val bookRules = instructions.book.trim()
            val mode = instructions.mode?.takeIf { it.instructions.isNotBlank() }
            if (general.isNotEmpty() || bookRules.isNotEmpty() || mode != null) {
                appendLine()
                appendLine("# Instruções do autor")
                appendLine("Siga estas instruções: elas mandam no estilo e no foco da resposta. Só não valem para contradizer as fichas e o enredo do livro.")
                if (general.isNotEmpty()) { appendLine(); appendLine("## Como o autor quer ser ajudado"); appendLine(general) }
                if (bookRules.isNotEmpty()) { appendLine(); appendLine("## Sobre este livro"); appendLine(bookRules) }
                if (mode != null) { appendLine(); appendLine("## Seu papel nesta conversa: ${mode.name}"); appendLine(mode.instructions.trim()) }
            }
            appendLine()
            appendLine("# Contexto do livro")
            append(blocks.joinToString("\n\n"))
        }
        return BuiltContext(system, used, estimateTokens(system), cutChapters)
    }

    /**
     * Divide [room] caracteres entre textos de tamanhos [lengths]: os que cabem na parte igual vão inteiros e o que
     * sobra deles é repartido entre os maiores. Devolve quanto cada um pode ocupar, na mesma ordem.
     */
    internal fun fairShares(lengths: List<Int>, room: Int): List<Int> {
        val out = IntArray(lengths.size)
        var left = room
        val order = lengths.indices.sortedBy { lengths[it] }
        order.forEachIndexed { k, i ->
            val share = left / (order.size - k)
            out[i] = minOf(lengths[i], share)
            left -= out[i]
        }
        return out.toList()
    }

    private const val CUT_MARK = "\n[…cortado por limite de contexto]"

    private fun cap(text: String, max: Int): String = if (text.length <= max) text else text.take(max).trimEnd() + CUT_MARK

    // --- trechos ---

    internal fun buildPieces(book: BookContext): List<ContextPiece> = buildList {
        for (meta in book.manifest.chapters) {
            val text = stripMarkup(book.chapterBodies[meta.id].orEmpty())
            if (text.isBlank()) continue
            val label = chapterLabel(book, meta.id)
            val paragraphs = text.split('\n').filter { it.isNotBlank() }
            val buf = StringBuilder()
            var part = 1
            fun flush() {
                if (buf.isEmpty()) return
                add(ContextPiece("$label (passagem $part)", buf.toString().trim(), AiRef.Chapter(meta.id), "c:${meta.id}"))
                buf.clear()
                part++
            }
            for (p in paragraphs) {
                if (buf.isNotEmpty() && buf.length + p.length > PIECE_CHARS) flush()
                buf.append(p).append('\n')
            }
            flush()
        }
        for (beat in book.outline.beats) {
            val text = beatText(book, beat.id)
            if (text.isNotBlank()) add(ContextPiece("Cena: ${beat.title}", text, AiRef.Beat(beat.id), "b:${beat.id}"))
        }
        for (entry in book.universe.entries) {
            add(ContextPiece(entryLabel(entry), entryText(entry), AiRef.Entry(entry.id), "e:${entry.id}"))
        }
    }

    private fun chapterLabel(book: BookContext, id: String): String {
        val meta = book.manifest.chapters.firstOrNull { it.id == id } ?: return "Capítulo"
        val n = chapterNumbers(book.manifest.chapters)[id]
        // Prólogo, epílogo, piloto…: o tipo vai no rótulo para a IA saber que parte do livro é aquela.
        return when {
            n != null -> "Capítulo $n — ${meta.title}"
            meta.title.isBlank() || meta.title.equals(meta.kind.label, ignoreCase = true) -> meta.kind.label
            else -> "${meta.kind.label} — ${meta.title}"
        }
    }

    private fun entryLabel(e: UniverseEntry) = "${e.kind.label}: ${e.name}"

    /** A ficha como texto. Fatos "selados" vão marcados, porque a IA não pode contradizê-los. */
    internal fun entryText(e: UniverseEntry): String = buildString {
        if (e.subtitle.isNotBlank()) appendLine(e.subtitle.trim())
        e.attributes.filter { it.value.isNotBlank() }.forEach { a ->
            val label = attributeLabelFor(e, a)
            appendLine("$label: ${a.value.trim()}${if (a.sealed) " (fato fixo, não pode mudar)" else ""}")
        }
        e.blocks.filter { it.text.isNotBlank() }.forEach { b ->
            appendLine("${blockTitleFor(e, b)}: ${stripMarkup(b.text).trim()}")
        }
        if (e.blocks.isEmpty() && e.summary.isNotBlank()) appendLine(e.summary.trim())
    }.trim()

    private fun attributeLabelFor(e: UniverseEntry, a: EntryAttribute): String =
        a.label.ifBlank { e.schema.attributes.firstOrNull { it.key == a.key }?.label ?: a.key.orEmpty() }

    private fun blockTitleFor(e: UniverseEntry, b: EntryBlock): String =
        b.title.ifBlank { e.schema.blocks.firstOrNull { it.key == b.key }?.title ?: b.key.orEmpty() }

    private fun beatText(book: BookContext, beatId: String): String {
        val b = book.outline.beats.firstOrNull { it.id == beatId } ?: return ""
        val chars = b.characterIds.mapNotNull { id -> book.universe.entries.firstOrNull { it.id == id }?.name }
        return buildString {
            if (b.act.isNotBlank()) appendLine("Etapa: ${b.act}")
            appendLine("Situação: ${b.status.label}")
            if (b.whenText.isNotBlank()) appendLine("Quando: ${b.whenText}")
            if (chars.isNotEmpty()) appendLine("Personagens: ${chars.joinToString(", ")}")
            if (b.summary.isNotBlank()) appendLine(b.summary.trim())
        }.trim()
    }

    private fun outlineText(book: BookContext, withIds: Boolean): String =
        book.outline.beats.joinToString("\n") { b ->
            val act = if (b.act.isNotBlank()) "[${b.act}] " else ""
            val summary = if (b.summary.isNotBlank()) ": ${b.summary.trim().replace('\n', ' ')}" else ""
            "- $act${b.title}$summary" + if (withIds) " (${b.id})" else ""
        }

    private fun otherBookText(other: BookContext): String = buildString {
        appendLine("Título: ${other.manifest.title}")
        if (other.manifest.genre.isNotBlank()) appendLine("Gênero: ${other.manifest.genre}")
        if (other.manifest.synopsis.isNotBlank()) appendLine("Sinopse: ${other.manifest.synopsis.trim()}")
        val outline = outlineText(other, withIds = false)
        if (outline.isNotBlank()) appendLine("Roteiro:\n$outline")
        val names = other.universe.entries.joinToString("; ") { "${it.name} (${it.kind.label})" }
        if (names.isNotBlank()) appendLine("Fichas: $names")
    }.trim()

    private fun currentChapterEntryIds(book: BookContext): Set<String> {
        val id = book.currentChapterId ?: return emptySet()
        val text = book.chapterBodies[id].orEmpty()
        if (text.isBlank()) return emptySet()
        return book.universe.entries.filter { findMentions(text, it.searchTerms()).isNotEmpty() }.map { it.id }.toSet()
    }
}
