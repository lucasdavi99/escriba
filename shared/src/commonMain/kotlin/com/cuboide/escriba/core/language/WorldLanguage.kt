package com.cuboide.escriba.core.language

import kotlinx.serialization.Serializable

/**
 * O que fica gravado da língua do livro (`language.json`): a semente e o estilo bastam para gerar tudo de novo.
 * [previousSeeds]: sementes anteriores, para "Voltar à anterior". [coined]: palavras cunhadas em traduções
 * usadas (lema → forma antiga), que ficam iguais mesmo se o gerador mudar em versões futuras.
 */
@Serializable
data class WorldLanguage(
    val seed: Long,
    val style: LangStyle,
    val generatorVersion: Int = Language.GENERATOR_VERSION,
    val previousSeeds: List<Long> = emptyList(),
    val coined: Map<String, String> = emptyMap(),
) {
    fun build(): Language = Language(seed, style)
}

/** Entrada do dicionário da língua: palavra antiga, o lema em português e se foi cunhada numa tradução. */
data class DictionaryEntry(val word: String, val meaning: String, val coined: Boolean)

/** Dicionário: os conceitos básicos e as palavras cunhadas, em ordem alfabética do português. */
fun Language.dictionary(coined: Map<String, String>): List<DictionaryEntry> =
    (Concepts.all.map { DictionaryEntry(protoWord(it.lemma), displayLemma(it.lemma), false) } +
        coined.map { (lemma, word) -> DictionaryEntry(word, displayLemma(lemma), true) })
        .sortedBy { it.meaning }
