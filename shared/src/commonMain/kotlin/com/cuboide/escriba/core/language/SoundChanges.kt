package com.cuboide.escriba.core.language

/** Mudança de som com a explicação que aparece para o autor. */
data class SoundRule(val id: String, val explanation: String, private val pattern: Regex, private val replacement: String) {
    fun apply(word: String): String {
        // "LONG": a vogal longa vira a vogal simples (aa → a).
        val out = if (replacement == "LONG") pattern.replace(word) { it.value.take(1) } else pattern.replace(word, replacement)
        return if (out.length < 2) word else Language.tidy(out)
    }
}

private const val V = "[aeiou]"

/** Regras possíveis; cada língua sorteia quais usa e em que ordem. */
val SOUND_RULES: List<SoundRule> = listOf(
    SoundRule("p-f", "p entre vogais virou f", Regex("(?<=$V)p(?=$V)"), "f"),
    SoundRule("t-d", "t entre vogais ficou sonoro: d", Regex("(?<=$V)t(?![h])(?=$V)"), "d"),
    SoundRule("k-h", "k entre vogais se desfez em h", Regex("(?<=$V)k(?![h])(?=$V)"), "h"),
    SoundRule("final", "a vogal final, átona, caiu", Regex("(?<=[^aeiou])[aeiou]$"), ""),
    SoundRule("ki-chi", "k antes de i e e virou ch", Regex("k(?![h])(?=[ie])"), "ch"),
    SoundRule("cr", "o r dos encontros sumiu (kr → k)", Regex("(?<=[kgtdbpf])r"), ""),
    SoundRule("h-ini", "o h do começo emudeceu", Regex("^h(?=$V)"), ""),
    SoundRule("o-u", "o final fechou em u", Regex("o$"), "u"),
    SoundRule("l-r", "l entre vogais virou r", Regex("(?<=$V)l(?=$V)"), "r"),
    SoundRule("s-h", "s no fim virou um sopro (h)", Regex("s$"), "h"),
    SoundRule("ee-i", "ee e oo fecharam em i e u", Regex("ee"), "i"),
    SoundRule("n-nh", "n antes de i palatalizou: nh", Regex("n(?![h])(?=i)"), "nh"),
    SoundRule("ai-e", "o ditongo ai virou e", Regex("ai"), "e"),
    SoundRule("d-th", "d entre vogais amoleceu em th", Regex("(?<=$V)d(?=$V)"), "th"),
    SoundRule("a-e", "o a final enfraqueceu em e", Regex("a$"), "e"),
    SoundRule("i-e", "i entre consoantes abriu em e", Regex("(?<=[^aeiou])i(?=[^aeiou])"), "e"),
    SoundRule("r-fim", "o r do fim caiu", Regex("(?<=$V)r$"), ""),
    SoundRule("n-m", "n no fim fechou em m", Regex("(?<=$V)n$"), "m"),
    SoundRule("longa", "a vogal longa encurtou", Regex("(aa|ee|ii|oo|uu)"), "LONG"),
    SoundRule("u-o", "u virou o antes de consoante final", Regex("u(?=[^aeiou]$)"), "o"),
)

/** Grupo de variedades, como aparece na tela. */
enum class VarietyAxis(val label: String) { Tempo("No tempo"), Regioes("Nas regiões"), Sociedade("Na sociedade"), Situacao("Na situação") }

/**
 * Variedade da língua: a forma de hoje é a antiga com as regras de [steps] aplicadas em ordem. [clip]: fala
 * íntima, que encurta palavras longas; [honorific]: fala cerimoniosa, que põe o honorífico nos nomes.
 */
data class Variety(
    val id: String,
    val name: String,
    val axis: VarietyAxis,
    val description: String,
    val steps: List<SoundRule>,
    val clip: Boolean = false,
    val honorific: Boolean = false,
)

/** As regras sorteadas para uma língua e as variedades montadas com elas. */
class SoundChanges(val toOld: List<SoundRule>, val toNow: List<SoundRule>, val regional: List<SoundRule>, val reduced: List<SoundRule>) {
    fun varieties(lang: String): List<Variety> {
        val old = toOld
        val now = toOld + toNow
        return listOf(
            Variety("proto", "Proto-$lang", VarietyAxis.Tempo, "a língua reconstruída, antes de tudo", emptyList()),
            Variety("antigo", "$lang antigo", VarietyAxis.Tempo, "a dos documentos e dos templos", old),
            Variety("atual", "$lang de hoje", VarietyAxis.Tempo, "a que se fala na capital", now),
            Variety("norte", "Do Norte", VarietyAxis.Regioes, "a de hoje com um traço a mais", now + regional.take(1)),
            Variety("litoral", "Do Litoral", VarietyAxis.Regioes, "a de hoje com outro traço", now + regional.drop(1).take(1)),
            Variety("serras", "Das Serras", VarietyAxis.Regioes, "parou no tempo: quase a antiga", old + regional.drop(2).take(1)),
            Variety("nobreza", "Da Nobreza", VarietyAxis.Sociedade, "conservadora: guarda a forma antiga", old),
            Variety("mercado", "Do Mercado", VarietyAxis.Sociedade, "rápida: gasta os sons", now + reduced),
            Variety("cerimonia", "Cerimoniosa", VarietyAxis.Situacao, "a de hoje, com honorífico", now, honorific = true),
            Variety("familiar", "Familiar", VarietyAxis.Situacao, "a de hoje encurtada, entre íntimos", now, clip = true),
        )
    }

    companion object {
        fun generate(seed: Long): SoundChanges {
            val r = Rng.stream(seed, "mudancas:${Language.GENERATOR_VERSION}")
            val pool = SOUND_RULES.toMutableList()
            fun take(n: Int) = List(n) { pool.removeAt(r.nextInt(pool.size)) }
            val reducers = SOUND_RULES.filter { it.id in setOf("final", "cr", "h-ini", "k-h") }
            return SoundChanges(take(4), take(4), take(3), listOf(r.pick(reducers)))
        }
    }
}

/** Uma etapa da história da palavra: a regra e a forma depois dela. */
data class Step(val rule: SoundRule, val result: String)

/** Aplica as regras da variedade e devolve cada etapa que mudou a palavra. */
fun Variety.history(proto: String): List<Step> {
    var w = proto
    val out = mutableListOf<Step>()
    for (r in steps) {
        val next = r.apply(w)
        if (next != w) out += Step(r, next)
        w = next
    }
    return out
}

/** Forma da palavra nesta variedade (sem afixos). */
fun Variety.form(proto: String, lang: Language): String {
    var w = history(proto).lastOrNull()?.result ?: proto
    if (clip) {
        val s = lang.syllabify(w)
        if (s.size >= 3) w = s.dropLast(1).joinToString("")
    }
    return w
}
