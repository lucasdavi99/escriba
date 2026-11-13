package com.cuboide.escriba.core.language

/** Classe da palavra: decide a ordem na frase e quais afixos ela recebe. */
enum class WordClass { Nome, Verbo, Adjetivo, Pronome, Numero, Outro }

/** Conceito básico do léxico, pelo lema em português. */
data class Concept(val lemma: String, val cls: WordClass, val short: Boolean = false)

/**
 * Lista própria de conceitos que toda língua gerada já tem (o resto é cunhado quando aparece numa frase).
 * [short]: palavras muito usadas, que ficam com uma sílaba só ou duas.
 */
object Concepts {
    private fun n(vararg w: String) = w.map { Concept(it, WordClass.Nome) }
    private fun v(vararg w: String) = w.map { Concept(it, WordClass.Verbo) }
    private fun a(vararg w: String) = w.map { Concept(it, WordClass.Adjetivo) }
    private fun s(cls: WordClass, vararg w: String) = w.map { Concept(it, cls, short = true) }

    val all: List<Concept> = listOf(
        // Natureza
        n("agua", "fogo", "terra", "vento", "pedra", "mar", "rio", "lago", "monte", "vale", "ilha", "ceu", "sol", "lua",
            "estrela", "noite", "dia", "luz", "sombra", "nevoa", "chuva", "neve", "gelo", "tempestade", "trovao", "arvore",
            "floresta", "flor", "folha", "raiz", "semente", "campo", "caminho", "praia", "onda", "areia", "deserto", "cinza"),
        // Seres e pessoas
        n("lobo", "corvo", "cavalo", "peixe", "serpente", "dragao", "urso", "passaro", "cervo", "gato", "cao",
            "rei", "rainha", "principe", "princesa", "guerreiro", "bruxa", "mago", "sacerdote", "mae", "pai", "filho", "filha",
            "irmao", "irma", "amigo", "inimigo", "povo", "estrangeiro", "crianca", "velho", "homem", "mulher", "deus", "deusa",
            "capitao", "marinheiro", "ladrao", "ferreiro"),
        // Corpo e coisas
        n("olho", "mao", "coracao", "sangue", "osso", "voz", "nome", "alma", "sonho", "morte", "vida", "guerra", "paz",
            "casa", "porta", "torre", "castelo", "cidade", "porto", "barco", "navio", "espada", "escudo", "lanca", "coroa",
            "ouro", "prata", "ferro", "pao", "vinho", "livro", "cancao", "historia", "segredo", "promessa", "lei", "festa",
            "ano", "tempo", "palavra", "lingua", "fim", "inicio"),
        // Ações
        v("ver", "ouvir", "falar", "dizer", "ir", "vir", "andar", "correr", "voar", "nadar", "cair", "subir", "comer",
            "beber", "dormir", "sonhar", "morrer", "viver", "nascer", "amar", "odiar", "temer", "querer", "saber", "lembrar",
            "esquecer", "dar", "tomar", "ter", "fazer", "lutar", "matar", "proteger", "cantar", "chorar", "rir", "abrir",
            "fechar", "chamar", "esperar", "encontrar", "perder", "guardar", "queimar", "construir", "partir", "voltar"),
        // Qualidades
        a("grande", "pequeno", "alto", "baixo", "longo", "curto", "velho", "novo", "antigo", "jovem", "forte", "fraco",
            "rapido", "lento", "bom", "mau", "belo", "feio", "claro", "escuro", "frio", "quente", "branco", "negro",
            "vermelho", "verde", "azul", "dourado", "sagrado", "proibido", "perdido", "eterno", "livre", "triste", "alegre",
            "cheio", "vazio", "profundo", "distante", "silencioso"),
        // Palavras curtas
        s(WordClass.Pronome, "eu", "tu", "ele", "ela", "nos", "vos", "eles", "elas", "voce", "isto", "aquilo", "quem", "que"),
        s(WordClass.Numero, "um", "dois", "tres", "quatro", "cinco", "seis", "sete", "oito", "nove", "dez", "cem", "mil"),
        s(WordClass.Outro, "e", "ou", "mas", "com", "sem", "para", "sobre", "sob", "entre", "ate", "sempre", "nunca",
            "aqui", "ali", "agora", "depois", "antes", "muito", "pouco", "sim"),
    ).flatten()

    private val byLemma = all.associateBy { it.lemma }

    fun find(lemma: String): Concept? = byLemma[lemma]

    /** Conceitos bons para nomes de pessoas e lugares (substantivos e qualidades com imagem forte). */
    val forNames: List<String> = listOf(
        "agua", "fogo", "pedra", "mar", "rio", "lago", "monte", "vale", "ilha", "ceu", "sol", "lua", "estrela", "noite",
        "luz", "sombra", "nevoa", "chuva", "neve", "trovao", "arvore", "flor", "raiz", "onda", "lobo", "corvo", "cervo",
        "rei", "rainha", "mae", "pai", "coracao", "sangue", "voz", "sonho", "espada", "coroa", "ouro", "prata", "ferro",
        "cancao", "promessa", "festa", "forte", "claro", "escuro", "frio", "vermelho", "dourado", "sagrado", "livre", "eterno",
    )

    /** Palavras mostradas em "A mesma palavra em cada canto". */
    val showcase: List<String> = listOf(
        "agua", "fogo", "terra", "vento", "pedra", "mar", "rio", "lago", "monte", "ilha", "ceu", "sol", "lua", "estrela",
        "noite", "dia", "luz", "sombra", "nevoa", "rei", "rainha", "mae", "pai", "casa", "porto", "espada", "coroa", "nome",
    )
}

/** Lema sem acento do jeito que aparece para o leitor (água, coração…). */
val LEMMA_DISPLAY: Map<String, String> = mapOf(
    "agua" to "água", "ceu" to "céu", "nevoa" to "névoa", "trovao" to "trovão", "arvore" to "árvore", "dragao" to "dragão",
    "passaro" to "pássaro", "principe" to "príncipe", "mae" to "mãe", "irmao" to "irmão", "irma" to "irmã", "crianca" to "criança",
    "coracao" to "coração", "lanca" to "lança", "pao" to "pão", "cancao" to "canção", "historia" to "história", "lingua" to "língua",
    "inicio" to "início", "lancar" to "lançar", "rapido" to "rápido", "capitao" to "capitão", "ladrao" to "ladrão", "cao" to "cão",
    "tres" to "três", "nos" to "nós", "vos" to "vós", "voce" to "você", "ate" to "até", "proibido" to "proibido",
)

fun displayLemma(lemma: String): String = LEMMA_DISPLAY[lemma] ?: lemma
