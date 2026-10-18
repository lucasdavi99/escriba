package com.cuboide.escriba.core.outline

data class TemplateStep(val act: String, val title: String, val hint: String)

data class StructureTemplate(val id: String, val name: String, val description: String, val steps: List<TemplateStep>)

/** Modelos de estrutura narrativa oferecidos no painel de Enredo. */
object StructureTemplates {
    val ThreeActs = StructureTemplate(
        id = "tres-atos",
        name = "Três atos",
        description = "Apresentação, confronto e resolução, com duas viradas e um ponto médio.",
        steps = listOf(
            TemplateStep("Ato I · Apresentação", "Rotina do protagonista", "Quem é, o que deseja e o que lhe falta antes de tudo mudar."),
            TemplateStep("Ato I · Apresentação", "Acontecimento que desequilibra", "Um fato tira a personagem da rotina e cria o problema da história."),
            TemplateStep("Ato I · Apresentação", "Primeira virada", "A personagem decide agir e atravessa um ponto sem volta."),
            TemplateStep("Ato II · Confronto", "Terreno desconhecido", "Primeiras tentativas, aliados e adversários; as regras do novo cenário."),
            TemplateStep("Ato II · Confronto", "Ponto médio", "Uma descoberta ou vitória aparente muda o objetivo ou o tamanho do risco."),
            TemplateStep("Ato II · Confronto", "O cerco se fecha", "Os obstáculos crescem e o plano começa a falhar."),
            TemplateStep("Ato II · Confronto", "Segunda virada", "O pior momento: algo é perdido e a personagem precisa mudar para seguir."),
            TemplateStep("Ato III · Resolução", "Confronto final", "O conflito central é enfrentado de frente, com tudo o que foi aprendido."),
            TemplateStep("Ato III · Resolução", "Novo equilíbrio", "As consequências e o que mudou na personagem e no mundo dela."),
        ),
    )

    val HeroJourney = StructureTemplate(
        id = "jornada-heroi",
        name = "Jornada do herói",
        description = "Doze etapas de partida, iniciação e retorno, no estilo do monomito.",
        steps = listOf(
            TemplateStep("Partida", "Mundo comum", "O cotidiano do herói, para que o leitor sinta o contraste com o que virá."),
            TemplateStep("Partida", "Chamado à aventura", "Surge um convite, ameaça ou oportunidade que exige uma resposta."),
            TemplateStep("Partida", "Recusa do chamado", "Medo, dever ou dúvida fazem o herói hesitar."),
            TemplateStep("Partida", "Encontro com o mentor", "Alguém oferece conselho, treino ou um objeto que dá coragem."),
            TemplateStep("Partida", "Travessia do limiar", "O herói deixa o mundo conhecido e entra no desconhecido."),
            TemplateStep("Iniciação", "Testes, aliados e inimigos", "Desafios menores mostram quem ajuda, quem atrapalha e como as coisas funcionam ali."),
            TemplateStep("Iniciação", "Aproximação", "Preparação e tensão antes do maior perigo."),
            TemplateStep("Iniciação", "Provação", "O teste central: o herói encara a morte, real ou simbólica."),
            TemplateStep("Iniciação", "Recompensa", "O que foi conquistado ao sobreviver — um objeto, um saber, uma reconciliação."),
            TemplateStep("Retorno", "Caminho de volta", "As forças contrárias reagem; a volta tem um preço."),
            TemplateStep("Retorno", "Ressurreição", "Um último teste prova que o herói mudou de verdade."),
            TemplateStep("Retorno", "Retorno transformado", "O herói volta para casa trazendo algo que muda o mundo comum."),
        ),
    )

    val all: List<StructureTemplate> = listOf(ThreeActs, HeroJourney)
}
