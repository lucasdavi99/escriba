package com.cuboide.escriba.core.ai

/** Pedidos prontos do assistente. O texto vira o prompt; o autor pode completá-lo antes de enviar. */
enum class AiTask(val label: String, val prompt: String, val needsSelection: Boolean = false) {
    Ideias("Ideias", "Me dê ideias para o que pode acontecer a seguir, coerentes com o enredo e o universo. Liste 5, cada uma com uma frase de justificativa."),
    Melhorar(
        "Melhorar trecho",
        "Melhore o trecho selecionado: clareza, ritmo e força das imagens, mantendo a minha voz, os fatos e o sentido. Devolva só a versão melhorada.",
        needsSelection = true,
    ),
    Continuar("Continuar", "Continue o texto a partir de onde o capítulo atual termina, no mesmo estilo e voz. Escreva cerca de 300 palavras."),
    Coerencia(
        "Conferir coerência",
        "Confira o capítulo atual contra as fichas do Universo e o Enredo. Liste, com o trecho exato, tudo o que contradiz uma ficha, uma data ou uma cena. Se nada contradiz, diga isso.",
    ),
    Corrigir(
        "Corrigir texto",
        "Corrija ortografia, gramática e pontuação do trecho selecionado sem mudar o estilo. Devolva o texto corrigido e, depois, uma lista curta do que mudou.",
        needsSelection = true,
    ),
    Resumir("Resumir capítulo", "Resuma o capítulo atual em até 5 frases, destacando o que muda para os personagens."),
}
