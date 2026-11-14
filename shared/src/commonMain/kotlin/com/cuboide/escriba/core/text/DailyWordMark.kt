package com.cuboide.escriba.core.text

/**
 * A marca do dia: a maior contagem de palavras de cada capítulo já registrada hoje. Só o que passa dela vira
 * palavras escritas, então apagar e reescrever, ou desfazer e refazer, não conta o mesmo texto duas vezes.
 * Vale para todos os capítulos do livro e sobrevive à troca de capítulo; vira à meia-noite ([today]).
 */
class DailyWordMark(private val today: () -> String) {
    private val marks = mutableMapOf<String, Int>()
    private var markDay = ""

    /**
     * Edição do teclado levou o capítulo de [from] para [to] palavras. Devolve as palavras escritas (o que passou
     * da marca) e sobe a marca. Sem marca hoje, ela começa em [from], o texto que já existia.
     */
    fun wrote(chapterId: String, from: Int, to: Int): Int {
        val mark = markOf(chapterId, from)
        if (to <= mark) return 0
        marks[chapterId] = to
        return to - mark
    }

    /** Edição do app levou o capítulo de [from] para [to] palavras: a marca sobe junto, sem contar nada. */
    fun appEdited(chapterId: String, from: Int, to: Int) {
        marks[chapterId] = maxOf(markOf(chapterId, from), to)
    }

    private var lastActivityAt: Long? = null
    private var pendingActiveMs = 0L

    /** Houve uma edição em [at]. Tempo ativo = soma dos intervalos curtos entre edições (pausas longas não contam). */
    fun activity(at: Long) {
        lastActivityAt?.let { pendingActiveMs += (at - it).coerceIn(0, ACTIVE_GAP_MS) }
        lastActivityAt = at
    }

    /** Segundos ativos acumulados desde a última chamada; a fração de segundo fica para a próxima. */
    fun takeActiveSeconds(): Long {
        val seconds = pendingActiveMs / 1000
        pendingActiveMs %= 1000
        return seconds
    }

    private fun markOf(chapterId: String, from: Int): Int {
        val day = today()
        if (day != markDay) {
            marks.clear()
            markDay = day
        }
        return marks.getOrPut(chapterId) { from }
    }

    companion object {
        const val ACTIVE_GAP_MS = 30_000L
    }
}
