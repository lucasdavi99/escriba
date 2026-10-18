package com.cuboide.escriba.core.storage

import com.cuboide.escriba.core.text.DayKey
import kotlinx.serialization.Serializable

@Serializable
data class DayStat(val words: Int = 0, val activeSeconds: Long = 0, val sessions: Int = 0)

@Serializable
data class ProductivityData(val days: Map<String, DayStat> = emptyMap())

data class ProductivitySummary(
    val today: DayStat,
    val streakDays: Int,
    val bestDay: Pair<String, Int>?,
    val totalWords: Int,
    /** Últimos N dias, do mais antigo ao mais recente (inclui dias sem escrita). */
    val recent: List<Pair<String, DayStat>>,
)

/** Histórico diário de palavras escritas e tempo ativo, compartilhado por todos os livros. */
class ProductivityStore(private val fs: FileStore, private val file: String) {
    private var data: ProductivityData = load()

    fun record(day: String, wordsAdded: Int, activeSeconds: Long, newSession: Boolean = false) {
        if (wordsAdded <= 0 && activeSeconds <= 0 && !newSession) return
        val current = data.days[day] ?: DayStat()
        val updated = current.copy(
            words = current.words + wordsAdded.coerceAtLeast(0),
            activeSeconds = current.activeSeconds + activeSeconds.coerceAtLeast(0),
            sessions = current.sessions + if (newSession) 1 else 0,
        )
        data = data.copy(days = data.days + (day to updated))
        fs.writeText(file, EscribaJson.encodeToString(ProductivityData.serializer(), data))
    }

    fun summary(today: String, recentDays: Int = 30): ProductivitySummary {
        val days = data.days
        // A sequência continua viva se hoje ainda não teve escrita mas ontem teve.
        var cursor = if ((days[today]?.words ?: 0) > 0) today else DayKey.plusDays(today, -1)
        var streak = 0
        while ((days[cursor]?.words ?: 0) > 0) {
            streak++
            cursor = DayKey.plusDays(cursor, -1)
        }
        val recent = (recentDays - 1 downTo 0).map { back ->
            val key = DayKey.plusDays(today, -back.toLong())
            key to (days[key] ?: DayStat())
        }
        return ProductivitySummary(
            today = days[today] ?: DayStat(),
            streakDays = streak,
            bestDay = days.maxByOrNull { it.value.words }?.takeIf { it.value.words > 0 }?.let { it.key to it.value.words },
            totalWords = days.values.sumOf { it.words },
            recent = recent,
        )
    }

    fun clear() {
        data = ProductivityData()
        fs.writeText(file, EscribaJson.encodeToString(ProductivityData.serializer(), data))
    }

    private fun load(): ProductivityData =
        fs.readText(file)?.let {
            runCatching { EscribaJson.decodeFromString(ProductivityData.serializer(), it) }.getOrNull()
        } ?: ProductivityData()
}
