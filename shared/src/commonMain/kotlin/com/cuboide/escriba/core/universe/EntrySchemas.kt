package com.cuboide.escriba.core.universe

import com.cuboide.escriba.core.model.EntryAttribute
import com.cuboide.escriba.core.model.EntryBlock
import com.cuboide.escriba.core.model.EntryKind
import com.cuboide.escriba.core.model.UniverseEntry

/** Para que serve um atributo, independente do rótulo (o código nunca compara rótulos). */
enum class AttrRole { None, Aliases, Group, Born, Died, Age, Gender }

/** Atributo padrão de um tipo de ficha. [sealable]: pode virar fato conferido no Manuscrito. */
data class AttrSpec(
    val key: String,
    val label: String,
    val placeholder: String = "",
    val role: AttrRole = AttrRole.None,
    val sealable: Boolean = false,
)

/** Bloco padrão de um tipo de ficha. [wide]: ocupa a largura toda; [list]: um item por linha (aliados, inimigos). */
data class BlockSpec(val key: String, val title: String, val placeholder: String = "", val wide: Boolean = false, val list: Boolean = false)

/** Como é a ficha de cada tipo: rótulos do título e do subtítulo, blocos e atributos padrão. */
data class EntrySchema(
    val kind: EntryKind,
    val titleLabel: String,
    val subtitleLabel: String,
    val blocks: List<BlockSpec>,
    val attributes: List<AttrSpec>,
) {
    fun block(key: String?): BlockSpec? = blocks.firstOrNull { it.key == key }
    fun attribute(key: String?): AttrSpec? = attributes.firstOrNull { it.key == key }
}

object EntrySchemas {
    const val NOTES = "notas"
    const val DESCRIPTION = "descricao"
    const val ALIASES = "apelidos"
    /** Frase marcante do personagem: aparece sob o retrato, não na grade de blocos. */
    const val QUOTE = "frase"

    private val personagem = EntrySchema(
        EntryKind.Personagem, "Nome", "Papel na história",
        blocks = listOf(
            BlockSpec("aparencia", "Descrição física", "Como a pessoa é por fora…"),
            BlockSpec("personalidade", "Personalidade", "Temperamento, manias, medos…"),
            BlockSpec("arco", "Arco do personagem", "Quem é no começo e quem é no fim…", wide = true),
            BlockSpec("aliados", "Aliados", "Um por linha…", list = true),
            BlockSpec("inimigos", "Inimigos", "Um por linha…", list = true),
            BlockSpec(NOTES, "Notas", "Notas…", wide = true),
            BlockSpec(QUOTE, "Frase marcante", "Algo que só essa pessoa diria…", wide = true),
        ),
        attributes = listOf(
            AttrSpec(ALIASES, "Apelidos", "separe por vírgula", AttrRole.Aliases),
            AttrSpec("grupo", "Grupo / subdivisão", "ex.: Casa de Ardel", AttrRole.Group, sealable = true),
            AttrSpec("idade", "Idade", "—", AttrRole.Age),
            AttrSpec("nasceu", "Nasceu", "ex.: 3 de março, ano 12", AttrRole.Born),
            AttrSpec("morreu", "Morreu", "em branco = está vivo", AttrRole.Died),
            AttrSpec("genero", "Ele ou ela", "ele, ela…", AttrRole.Gender, sealable = true),
            AttrSpec("olhos", "Olhos", "ex.: castanhos", sealable = true),
            AttrSpec("cabelo", "Cabelo", "ex.: preto e curto", sealable = true),
            AttrSpec("mao", "Mão", "canhota ou destra", sealable = true),
            AttrSpec("marca", "Marca", "ex.: cicatriz no queixo", sealable = true),
            AttrSpec("porte", "Altura e porte", "ex.: baixo e forte", sealable = true),
            AttrSpec("pele", "Pele", "ex.: sardenta", sealable = true),
            AttrSpec("corpo", "Corpo", "ex.: quatro braços", sealable = true),
            AttrSpec("especie", "Espécie / raça", "—", sealable = true),
        ),
    )

    private val local = EntrySchema(
        EntryKind.Local, "Nome", "Tipo (cidade, floresta…)",
        blocks = listOf(
            BlockSpec(DESCRIPTION, "Descrição", "Como é o lugar…"),
            BlockSpec(NOTES, "Notas", "Notas…"),
        ),
        attributes = listOf(
            AttrSpec(ALIASES, "Outros nomes", "separe por vírgula", AttrRole.Aliases),
            AttrSpec("regiao", "Grupo / subdivisão", "ex.: Reino do Sul", AttrRole.Group),
            AttrSpec("clima", "Clima / atmosfera"),
            AttrSpec("habitantes", "Habitantes"),
        ),
    )

    private val grupo = EntrySchema(
        EntryKind.Grupo, "Nome", "Tipo (facção, guilda…)",
        blocks = listOf(
            BlockSpec("objetivo", "Objetivo", "O que o grupo quer…"),
            BlockSpec(DESCRIPTION, "Descrição", "Como o grupo é…"),
            BlockSpec(NOTES, "Notas", "Notas…", wide = true),
        ),
        attributes = listOf(
            AttrSpec(ALIASES, "Outros nomes", "separe por vírgula", AttrRole.Aliases),
            AttrSpec("alianca", "Reino / aliança", "ex.: Liga das Ilhas", AttrRole.Group),
            AttrSpec("membros", "Membros-chave"),
        ),
    )

    private val lore = EntrySchema(
        EntryKind.Lore, "Título", "Categoria (magia, história…)",
        blocks = listOf(
            BlockSpec("conteudo", "Conteúdo", "O que se sabe…"),
            BlockSpec(NOTES, "Notas", "Notas…"),
        ),
        attributes = listOf(
            AttrSpec(ALIASES, "Outros nomes", "separe por vírgula", AttrRole.Aliases),
            AttrSpec("grupo", "Grupo / subdivisão", "ex.: Magia antiga", AttrRole.Group),
        ),
    )

    fun of(kind: EntryKind): EntrySchema = when (kind) {
        EntryKind.Personagem -> personagem
        EntryKind.Local -> local
        EntryKind.Grupo -> grupo
        EntryKind.Lore -> lore
    }
}

val UniverseEntry.schema: EntrySchema get() = EntrySchemas.of(kind)

/** Valor do atributo padrão [key] (vazio se a ficha ainda não o preencheu). */
fun UniverseEntry.attr(key: String): String = attributes.firstOrNull { it.key == key }?.value.orEmpty()

/** Texto do bloco padrão [key]. */
fun UniverseEntry.blockText(key: String): String = blocks.firstOrNull { it.key == key }?.text.orEmpty()

/** Valor do primeiro atributo com o papel [role] no esquema do tipo. */
fun UniverseEntry.attrByRole(role: AttrRole): String =
    schema.attributes.firstOrNull { it.role == role }?.let { attr(it.key) }.orEmpty()

/** Apelidos ou outros nomes, separados por vírgula, ponto e vírgula ou linha. */
val UniverseEntry.aliases: List<String>
    get() = attrByRole(AttrRole.Aliases).split(',', ';', '\n').map { it.trim() }.filter { it.isNotEmpty() }

/** Grava [value] no atributo padrão [key], criando-o se preciso; vazio e sem selo some da lista. */
fun UniverseEntry.withAttr(key: String, value: String, newId: () -> String): UniverseEntry {
    val spec = schema.attribute(key)
    val existing = attributes.firstOrNull { it.key == key }
    val updated = when {
        existing != null -> attributes.map { if (it.key == key) it.copy(value = value) else it }
        value.isEmpty() -> return this
        else -> attributes + EntryAttribute(newId(), key, spec?.label.orEmpty(), value)
    }
    return copy(attributes = updated.filterNot { it.key != null && it.value.isEmpty() && !it.sealed })
}

/** Grava o texto do bloco padrão [key], criando-o se preciso; bloco padrão vazio some da lista. */
fun UniverseEntry.withBlock(key: String, text: String, newId: () -> String): UniverseEntry {
    val spec = schema.block(key)
    val updated = when {
        blocks.any { it.key == key } -> blocks.map { if (it.key == key) it.copy(text = text) else it }
        text.isEmpty() -> return this
        else -> blocks + EntryBlock(newId(), key, spec?.title.orEmpty(), text)
    }
    return copy(blocks = updated.filterNot { it.key != null && it.text.isEmpty() })
}
