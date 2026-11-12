package com.cuboide.escriba.core.model

import kotlinx.serialization.Serializable

const val BOOK_EXTENSION = ".escriba"

@Serializable
data class BookManifest(
    val formatVersion: Int = 1,
    val id: String,
    val title: String,
    val author: String = "",
    val synopsis: String = "",
    val genre: String = "",
    val createdAt: Long,
    val modifiedAt: Long,
    val chapters: List<ChapterMeta> = emptyList(),
    val dailyGoal: Int = 1000,
    val bookGoal: Int = 50_000,
) {
    val totalWords: Int get() = chapters.sumOf { it.words }
}

@Serializable
data class ChapterMeta(
    val id: String,
    val title: String,
    val status: ChapterStatus = ChapterStatus.Rascunho,
    val words: Int = 0,
    val modifiedAt: Long = 0,
    val kind: ChapterKind = ChapterKind.Capitulo,
    /** Texto das notas de rodapé, pela id usada na chamada `[^id]` do texto. */
    val footnotes: Map<String, String> = emptyMap(),
)

enum class BookPartGroup(val label: String) { Antes("Antes da história"), Entre("Entre capítulos"), Depois("Depois da história") }

/** Capítulo numerado ou outra parte do livro (pré e pós-textuais), que não entra na numeração. */
@Serializable
enum class ChapterKind(val label: String, val group: BookPartGroup?) {
    Capitulo("Capítulo", null),
    Prologo("Prólogo", BookPartGroup.Antes),
    Prefacio("Prefácio", BookPartGroup.Antes),
    Apresentacao("Apresentação", BookPartGroup.Antes),
    NotaDoAutor("Nota do autor", BookPartGroup.Antes),
    AvisoAoLeitor("Aviso ao leitor", BookPartGroup.Antes),
    Dedicatoria("Dedicatória", BookPartGroup.Antes),
    Epigrafe("Epígrafe", BookPartGroup.Antes),
    Interludio("Interlúdio", BookPartGroup.Entre),
    Epilogo("Epílogo", BookPartGroup.Depois),
    Posfacio("Posfácio", BookPartGroup.Depois),
    Agradecimentos("Agradecimentos", BookPartGroup.Depois),
    SobreOAutor("Sobre o autor", BookPartGroup.Depois),
    Glossario("Glossário", BookPartGroup.Depois),
}

/** Número de cada capítulo numerado (partes como prólogo e epílogo ficam com `null`). */
fun chapterNumbers(chapters: List<ChapterMeta>): Map<String, Int> {
    var n = 0
    return chapters.filter { it.kind == ChapterKind.Capitulo }.associate { it.id to ++n }
}

/** Bloco de anotações livres do livro, mostrado ao lado do texto. */
@Serializable
data class NoteBlock(val id: String, val title: String = "", val body: String = "")

@Serializable
data class BookNotes(val blocks: List<NoteBlock> = emptyList())

@Serializable
enum class ChapterStatus(val label: String) {
    Ideia("Ideia"),
    Rascunho("Rascunho"),
    Revisao("Revisão"),
    Final("Final"),
}

@Serializable
enum class EntryKind(val label: String, val plural: String) {
    Personagem("Personagem", "Personagens"),
    Local("Local", "Locais"),
    Grupo("Grupo", "Grupos"),
    Lore("Lore", "Lore"),
}

/**
 * Bloco de texto da ficha (Descrição física, Personalidade, Notas…). [key] liga o bloco ao esquema do tipo
 * (ver `core/universe/EntrySchemas.kt`); `null` é uma seção criada pelo autor. O texto usa a marcação do Manuscrito.
 */
@Serializable
data class EntryBlock(val id: String, val key: String? = null, val title: String = "", val text: String = "")

/** Campo curto da ficha (Olhos, Clima…). [key] liga ao esquema; `null` é um atributo livre com rótulo próprio. */
@Serializable
data class EntryAttribute(
    val id: String,
    val key: String? = null,
    val label: String = "",
    val value: String = "",
    /** Fato "selado": o Manuscrito confere o texto contra ele (fase de contradições). */
    val sealed: Boolean = false,
)

/** Imagem da ficha guardada em `assets/` do livro, com o enquadramento escolhido. */
@Serializable
data class EntryImage(val path: String, val frame: com.cuboide.escriba.core.board.ImageFrame = com.cuboide.escriba.core.board.ImageFrame())

@Serializable
data class UniverseEntry(
    val id: String,
    val kind: EntryKind,
    val name: String,
    /** Campos da versão 1 das fichas; ao abrir o livro viram blocos (ver `migrateUniverse`). */
    val summary: String = "",
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val modifiedAt: Long = 0,
    /** Papel na história, tipo de lugar, tipo de grupo, categoria de lore… */
    val subtitle: String = "",
    val blocks: List<EntryBlock> = emptyList(),
    val attributes: List<EntryAttribute> = emptyList(),
    val portrait: EntryImage? = null,
    val gallery: List<EntryImage> = emptyList(),
)

@Serializable
data class Universe(val entries: List<UniverseEntry> = emptyList(), val schemaVersion: Int = 1)

@Serializable
enum class BeatStatus(val label: String) {
    Planejada("Planejada"),
    Escrevendo("Escrevendo"),
    Escrita("Escrita"),
    Revisada("Revisada"),
}

/** Cartão de cena do roteiro. */
@Serializable
data class Beat(
    val id: String,
    val title: String,
    val summary: String = "",
    val status: BeatStatus = BeatStatus.Planejada,
    /** Etapa da estrutura (ex.: "Ato I"); cartões consecutivos com a mesma etapa ficam agrupados. */
    val act: String = "",
    val chapterId: String? = null,
    /** Ids das fichas de personagem do Universo que aparecem na cena. */
    val characterIds: List<String> = emptyList(),
    val modifiedAt: Long = 0,
    /** Quando a cena acontece, como o autor escreveu ("12 de Bruma, 347 E.C."). */
    val whenText: String = "",
    /** A mesma data lida no calendário do livro; `null` se não foi informada ou não pôde ser lida. */
    val date: com.cuboide.escriba.core.calendar.WorldDate? = null,
)

/** Roteiro do livro: a ordem da lista é a ordem das cenas. */
@Serializable
data class Outline(val beats: List<Beat> = emptyList())

/** Livro encontrado na biblioteca, sem o texto dos capítulos. */
data class BookSummary(val path: String, val manifest: BookManifest)
