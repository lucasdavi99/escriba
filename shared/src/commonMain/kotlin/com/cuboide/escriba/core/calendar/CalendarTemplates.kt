package com.cuboide.escriba.core.calendar

/** Pontos de partida para o calendário do livro. Nomes e festas do exemplo foram inventados para o Escriba. */
enum class CalendarTemplate(val title: String, val description: String) {
    Exemplo("Aldamar (exemplo)", "9 meses de 40 dias e 5 dias fora do ano, semana de 5 dias, duas eras e festas."),
    ComoONosso("Como o nosso", "12 meses, semana de 7 dias e ano bissexto."),
    EmBranco("Em branco", "12 meses de 30 dias para você dar nome."),
}

object CalendarTemplates {
    fun build(template: CalendarTemplate): WorldCalendar = when (template) {
        CalendarTemplate.Exemplo -> aldamar()
        CalendarTemplate.ComoONosso -> gregorian()
        CalendarTemplate.EmBranco -> blank()
    }

    /** Calendário usado quando o livro não tem um: datas como "3 de março, 12" continuam valendo. */
    val implicit: WorldCalendar by lazy { gregorian() }

    private fun gregorian(): WorldCalendar {
        val names = listOf("janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro")
        val days = listOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val months = names.mapIndexed { i, n -> CalMonth("m${i + 1}", n.replaceFirstChar { it.uppercase() }, days[i]) }
        val base = WorldCalendar(
            name = "Calendário comum",
            months = months,
            weekdays = listOf("Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"),
            leap = LeapRule("m2", every = 4, skipEvery = 100, keepEvery = 400),
            seasons = listOf(
                Season("verao", "Verão", "#E0A33A", "m12", 21),
                Season("outono", "Outono", "#B8643A", "m3", 20),
                Season("inverno", "Inverno", "#5B86B0", "m6", 21),
                Season("primavera", "Primavera", "#6FA35B", "m9", 22),
            ),
            festivals = listOf(
                Festival("ano-novo", "Ano-novo", "m1", 1),
                Festival("natal", "Natal", "m12", 25),
            ),
        )
        // No calendário gregoriano, 1º de janeiro do ano 1 foi uma segunda-feira.
        val firstOfYearOne = base.weekdayOf(WorldDate(1, "m1", 1)) ?: 0
        return base.copy(weekdayOffset = (1 - firstOfYearOne).mod(7))
    }

    private fun blank(): WorldCalendar = WorldCalendar(
        name = "Calendário do mundo",
        months = (1..12).map { CalMonth("m$it", "Mês $it", 30) },
        weekdays = (1..7).map { "${it}º dia" },
    )

    private fun aldamar(): WorldCalendar {
        val names = listOf("Primeira Chuva", "Verdejo", "Abelha", "Sol Alto", "Poeira", "Trigo", "Folha Rubra", "Vento Norte", "Lareira")
        val months = names.mapIndexed { i, n -> CalMonth("a${i + 1}", n, 40) } + CalMonth("a0", "Dias da Lua Velha", 5, outsideWeek = true)
        return WorldCalendar(
            name = "Calendário de Aldamar",
            months = months,
            weekdays = listOf("Pedra", "Água", "Fogo", "Vento", "Semente"),
            eras = listOf(Era("torres", "Era das Torres", "E.T.", 1), Era("ponte", "Era da Ponte", "E.P.", 500)),
            seasons = listOf(
                Season("primavera", "Primavera", "#6FA35B", "a1", 1),
                Season("verao", "Verão", "#E0A33A", "a4", 1),
                Season("outono", "Outono", "#B8643A", "a7", 1),
                Season("inverno", "Inverno", "#5B86B0", "a9", 1),
            ),
            festivals = listOf(
                Festival("chuva", "Festa da Primeira Chuva", "a1", 1, "O ano começa: as portas ficam abertas até a chuva chegar."),
                Festival("trigo", "Dia do Trigo", "a6", 20, "Pão novo em todas as mesas; ninguém paga pela farinha."),
                Festival("ventos", "Feira dos Ventos", "a8", 10, "Pipas no porto e apostas sobre o inverno."),
                Festival("lareira", "Vigília da Lareira", "a9", 40, "A noite mais longa: as histórias antigas são contadas de novo."),
                Festival("lua-velha", "Dias da Lua Velha", "a0", 1, "Cinco dias que não pertencem a nenhum mês. Não se assina contrato."),
            ),
            present = WorldDate(1111, "a3", 7),
        )
    }
}
