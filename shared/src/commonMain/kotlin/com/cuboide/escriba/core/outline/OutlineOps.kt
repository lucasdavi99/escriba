package com.cuboide.escriba.core.outline

import com.cuboide.escriba.core.model.Beat
import com.cuboide.escriba.core.model.BeatStatus
import com.cuboide.escriba.core.model.Outline

/** Insere [beat] ou substitui o cartão de mesmo id, mantendo a posição. */
fun Outline.upsert(beat: Beat): Outline =
    if (beats.any { it.id == beat.id }) copy(beats = beats.map { if (it.id == beat.id) beat else it })
    else copy(beats = beats + beat)

/** Move o cartão [delta] posições; fora dos limites não faz nada. */
fun Outline.move(id: String, delta: Int): Outline {
    val from = beats.indexOfFirst { it.id == id }
    return if (from < 0) this else moveTo(id, from + delta)
}

/** Move o cartão para a posição [index] (na lista final); fora dos limites não faz nada. */
fun Outline.moveTo(id: String, index: Int): Outline {
    val list = beats.toMutableList()
    val from = list.indexOfFirst { it.id == id }
    if (from < 0 || index !in list.indices || from == index) return this
    list.add(index, list.removeAt(from))
    return copy(beats = list)
}

fun Outline.remove(id: String): Outline = copy(beats = beats.filterNot { it.id == id })

/** Insere na posição [index], limitada ao tamanho atual (útil ao restaurar da lixeira). */
fun Outline.insertAt(beat: Beat, index: Int): Outline {
    val list = beats.toMutableList()
    list.add(index.coerceIn(0, list.size), beat)
    return copy(beats = list)
}

/** Acrescenta ao fim do roteiro um cartão por etapa do modelo. Cartões existentes não mudam. */
fun Outline.applyTemplate(template: StructureTemplate, newId: () -> String, now: Long): Outline =
    copy(
        beats = beats + template.steps.map { step ->
            Beat(id = newId(), title = step.title, summary = step.hint, act = step.act, modifiedAt = now)
        },
    )

data class BeatFilter(val status: BeatStatus? = null, val characterId: String? = null, val query: String = "") {
    fun matches(beat: Beat): Boolean =
        (status == null || beat.status == status) &&
            (characterId == null || characterId in beat.characterIds) &&
            (query.isBlank() || beat.title.contains(query, true) || beat.summary.contains(query, true))
}

/** Uma etapa do roteiro: cartões consecutivos com o mesmo [Beat.act]. */
data class BeatGroup(val act: String, val beats: List<Beat>)

/** Agrupa cartões consecutivos pela etapa, preservando a ordem do roteiro. */
fun groupByAct(beats: List<Beat>): List<BeatGroup> {
    val groups = mutableListOf<BeatGroup>()
    for (beat in beats) {
        val last = groups.lastOrNull()
        if (last != null && last.act == beat.act) groups[groups.lastIndex] = last.copy(beats = last.beats + beat)
        else groups += BeatGroup(beat.act, listOf(beat))
    }
    return groups
}

/** Quantos cartões há em cada status, na ordem do enum. */
fun Outline.statusCounts(): List<Pair<BeatStatus, Int>> =
    BeatStatus.entries.map { s -> s to beats.count { it.status == s } }
