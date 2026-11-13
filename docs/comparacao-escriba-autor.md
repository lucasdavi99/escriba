# Escriba × Autor: comparação e caminho até a paridade

Este documento compara o **Escriba** (este projeto, Kotlin Multiplatform + Compose Desktop) com o **Autor 1.1.25**
(app Electron instalado em `%LOCALAPPDATA%\Programs\Autor`). As evidências do Autor vêm da análise com o REA,
detalhada em [engenharia-reversa-autor.md](engenharia-reversa-autor.md).

> "Igual" aqui significa **paridade de funcionalidades**. Não significa mesmo visual, mesmos textos, mesma
> arte nem mesmo formato de arquivo. O código do Autor declara que a licença proíbe "derivação, reimplementação",
> então tudo abaixo deve ser construído do zero, com dados e arte de fontes licenciadas.

## 1. Tamanho de cada app

| | Autor | Escriba |
|---|---|---|
| Código próprio + dados | ≈ 18,7 MB (JS/CSS/HTML minificados, dicionários, 3.000+ desenhos de mapa) | ≈ 246 KB (5.179 linhas de Kotlin) |
| Maiores módulos | IA ≈ 5,1 MB (com léxicos) · autocorreção 3,4 MB · revisor 2,5 MB · diagramação 1,8 MB · idiomas 1 MB · `app.js` 580 KB · sinônimos 545 KB · mapas 400 KB (+2.722 arquivos de arte) · ferramentas do mundo 330 KB · enredo 270 KB | controlador 605 linhas · Manuscrito 571 · telas 427 · Enredo 409 |
| Seções no menu | 16 (Biblioteca, Manuscrito, Universo, Roteiro, Enredo, Novels, Diagramação, Música, Progresso, Agenda, Mapas, Publicar, Paleta, Ritual, Lixeira, Configurações) | 10 (Biblioteca, Manuscrito, Enredo, Universo, Metas, Versões, Busca, Exportar, Lixeira, Preferências) |
| Canais IPC (funções do sistema) | 277 | não se aplica (sem processos separados) |

## 2. Funcionalidade por funcionalidade

Legenda: ✅ tem · 🟡 tem parte · ❌ não tem. O esforço supõe uma pessoa:
**P** até 3 dias, **M** 1 a 2 semanas, **G** 3 a 6 semanas, **GG** mais de 2 meses.

### Escrita

| # | Funcionalidade do Autor | Evidência | Escriba | O que é preciso | Esforço |
|---|---|---|---|---|---|
| 1 | Capítulos com status e reordenação | `project:*`, `chStatusBtn` | ✅ | — | — |
| 2 | Salvamento automático e gravação segura | `project:save`, `gravar-seguro.js` | ✅ | — | — |
| 3 | Editor que mostra a formatação (não `**`) | `services/document-model/*`, `paragraph-style.js` | ✅ | Marcadores somem fora do cursor; arquivo continua Markdown | — |
| 4 | Títulos H1–H3, listas, citação, alinhamento, espaçamento entre parágrafos | barra `data-writing-action` | ✅ | — | — |
| 5 | Imagens no texto | `project:import-image`, `fotos-do-livro.js` | 🟡 entra no livro e na exportação | Falta mostrar a imagem dentro do editor (hoje aparece a legenda) | P |
| 6 | Partes do livro (prólogo, dedicatória, epílogo…) | `renderer/partes/` | ✅ | — | — |
| 7 | Notas do capítulo e notas de rodapé | `manuscrito/notas`, `diagramacao/footnotes.js` | ✅ | Notas de rodapé e blocos de notas | — |
| 8 | Modo foco (inclusive destacar o parágrafo atual) | `renderer/foco/`, `foco-paragrafo-core.js` | ✅ | — | — |
| 9 | Localizar e substituir | `find*`, `replaceAll`, `deleteAll` | ✅ | — | — |
| 10 | Paleta de comandos | `btnCommandPalette` | ❌ | Diálogo com busca sobre ações, seções, capítulos e fichas (`Ctrl+K`) | P |
| 11 | Ritual de escrita (sessões, sprints) | `btnSessionDrawer` | ❌ | Cronômetro, meta da sessão e registro em `ProductivityStore` (já existe) | P |
| 12 | Player de música | `music:*`, `music-player.js` | ❌ | Biblioteca de faixas locais e reprodução de MP3/OGG com uma lib Java LGPL/Apache (evitar vlcj, que é GPL) | M |

### Revisão de texto (PT-BR)

| # | Funcionalidade | Evidência | Escriba | O que é preciso | Esforço |
|---|---|---|---|---|---|
| 13 | Ortografia, gramática e autocorreção | `revisor-ortografia.js`, `autocorrect-data.js` (3,4 MB) | ❌ | **LanguageTool** (LGPL 2.1, roda local na JVM, tem pt-BR) ou Hunspell com o dicionário VERO pt-BR. Sublinhado no editor e sugestões em menu | M–G |
| 14 | Sinônimos | `renderer/sinonimos/` (545 KB) | ❌ | Tesauro pt-BR com licença livre (por exemplo, o do LibreOffice; conferir a licença), indexado em SQLite ou JSON | M |
| 15 | Repetições e ecos de palavras | `repetition:analyze`, `ecos-universais` | ❌ | Detector próprio: mesma palavra ou radical a menos de N palavras de distância, ignorando palavras comuns | M |
| 16 | Legibilidade | `legibilidade.css` | ❌ | Índice Flesch adaptado ao português (Martins et al.), frases longas e advérbios em excesso | P |
| 17 | Contradições | `manuscrito/contradicoes` | ❌ | Fatos "selados" das fichas (olhos, cabelo, mão…) comparados com o texto, por capítulo e em relatório do livro. Detalhes em 9.3 | G |
| 18 | Resumo de capítulo | `chapter-summary-core.js` | ❌ | Via IA (item 56) ou resumo por extração | P (com IA) |

### Planejamento

| # | Funcionalidade | Evidência | Escriba | O que é preciso | Esforço |
|---|---|---|---|---|---|
| 19 | Roteiro de cenas com modelos | `outline:*` | 🟡 2 modelos | O Autor tem 10 (Save the Cat, Jornada do Herói, Story Circle, Sete Pontos, Kishōtenketsu, Fichtean, Romance, Mistério, Snowflake, Três Atos) e permite salvar e apagar modelos próprios | P |
| 20 | Quadro de enredo livre (canvas, ligações, minimapa) | `renderer/enredo/` (23 arquivos, 270 KB) | 🟡 | Feito: aba **Mapa** do Enredo (`PlotCanvas.kt`, `core/board/`, `enredo.json`) com zoom e arrasto, cartões com imagem, nós de imagem e de texto, ligações curvas com cor, rótulo e legenda, vínculo com fichas, vários quadros, desfazer e "Arranjar". Falta: minimapa | P |
| 21 | Linha do tempo e análise de arco | `enredo-timeline.js`, `enredo-arc-analysis.js` | ❌ | Eixo temporal das cenas e curva de tensão por cena | M |
| 22 | Agenda e prazos ("Norte") | `norte:*` | ❌ | Calendário com marcos e prazos, ligado às metas | M |
| 23 | Novels (publicação seriada) | `novels:*` | ❌ | Episódios com data de publicação a partir dos capítulos | M |

### Universo

| # | Funcionalidade | Evidência | Escriba | O que é preciso | Esforço |
|---|---|---|---|---|---|
| 24 | Fichas por tipo | `universe:*` | ✅ (11/11) | Ficha por tipo com subtítulo, blocos de texto rico, atributos padrão e livres e seções próprias (U1, U3) | — |
| 25 | Onde a ficha aparece | `universe:fichas-do-livro` | ✅ (11/11) | Contagem por capítulo, apelidos, sem acentos, abre o capítulo na citação (U5) | — |
| 26 | Relações entre fichas (grafo) | `universe-links-*.js` | ❌ | Lista `relations` (de, para, tipo) e grafo com layout de forças em Canvas | G |
| 27 | Retratos e imagens nas fichas | `universe-portrait-core.js` | ✅ (11/11) | Retrato com enquadramento e galeria por ficha (U2) | — |
| 28 | Universo compartilhado entre livros | `universe:link-project`, `unificar:*` | ❌ | Acervo de universos e unificação de livros da saga (U8, U9) | M–G |
| 29 | Árvore genealógica | `ferramentas/arvore-core.js`, `lentes/familia` | ❌ | Vem das relações do item 26; desenho em camadas | M |
| 30 | Calendário do mundo e linha do tempo | `calendario-mundo-core.js`, `linha-do-tempo-core.js` | ❌ | Meses, semana, eras, festas e estações; luas, sol e constelações; astrolábio e céu animado; datas do Enredo e idades (9.4; U10 a U13) | M–G |
| 31 | Idiomas inventados | `idiomas-*.js` | ❌ | Gerador por semente e estilo, gramática, tradutor, variedades, nomes e alfabeto próprio como fonte (9.5; U14 a U16) | G–GG |
| 32 | Mapas | `renderer/mapas/` (2.722 arquivos) | ❌ | Editor em Canvas com camadas, terrenos e carimbos. A arte precisa ser própria ou CC0 (por exemplo, Kenney). É o item mais caro em arte | GG |

### Progresso e dados

| # | Funcionalidade | Evidência | Escriba | O que é preciso | Esforço |
|---|---|---|---|---|---|
| 33 | Metas diária e do livro | `project:set-book-goal` | ✅ | — | — |
| 34 | Mapa de calor, sequência, tempo ativo | `productivity:*` | ✅ | — | — |
| 35 | Histórico de atividade | `activity:*` | ❌ | Registro de eventos (criou, apagou, exportou) | P |
| 36 | Versões com comparação e restauração | `snapshot:*` | ✅ | — | — |
| 37 | Lixeira com retenção | `trash:*` | ✅ | — | — |
| 38 | Busca | `search:*` | 🟡 só no livro aberto | Índice com todos os livros da biblioteca | P |
| 39 | Backup local automático | `local-backup:*` (a cada 7 dias) | ❌ | Zip agendado e restauração como livro novo | M |
| 40 | Backup em pasta de nuvem ("Arca") | `arca:*`, `cloud-detect.js` | ❌ | Detectar as pastas do Google Drive, OneDrive e Dropbox e copiar os zips para lá | P |
| 41 | Recuperação após falha | `project:crash-*` | ❌ | Rascunho `.recovery` a cada edição e oferta de restaurar ao abrir | P |
| 42 | Sincronização com conta | `nuvem:*`, `sincronia/motor.cjs` | ❌ | Precisa de servidor próprio (contas, armazenamento, resolução de conflitos). É outro produto | GG |

### Saída (exportar, diagramar, importar)

| # | Funcionalidade | Evidência | Escriba | O que é preciso | Esforço |
|---|---|---|---|---|---|
| 43 | DOCX e TXT | `export:*` | ✅ (e também MD e HTML) | — | — |
| 44 | PDF simples (manuscrito) | `export:pdf` | ✅ (26/10) | Coberto pelo PDF do miolo da Diagramação (item 45) | — |
| 45 | Diagramação profissional | `renderer/diagramacao/` (87 módulos, 1,8 MB) e `services/diagramacao/` | 🟡 miolo (26/10) | **Feito:** formatos e papéis, margens espelhadas, sangria e marcas de corte, Knuth-Plass, hifenização PT-BR por regras de silabação, viúvas e órfãs, aberturas (lado, descida, numeração, ornamento, capitular), cabeçalhos e fólios, pré-textuais e sumário, notas no pé, imagens, 5 temas, prévia idêntica ao PDF (PDFBox, fontes embutidas). **Falta:** PDF/X CMYK com ICC, grade de base, margens ópticas, objetos livres na página, estilos por parágrafo, índice remissivo. Detalhes em [diagramacao-autor.md](diagramacao-autor.md) | G (restante) |
| 46 | EPUB e Kindle | `diagramacao:export-epub`, `kindle-export.js` | ❌ | EPUB 3: zip com `mimetype`, OPF, NAV e XHTML por capítulo, mais CSS. O KDP aceita EPUB, o que cobre o Kindle | M |
| 47 | Capa (montagem, lombada, ISBN) e mockup 3D | `cover-*.js`, `isbn-ean13.js`, `book3d-*.js` | ❌ | Calculadora de lombada por número de páginas e papel, código de barras EAN-13, PDF da capa e mockup com `graphicsLayer(rotationY)` | G |
| 48 | Importar DOCX e TXT | `import:docx`, `import:txt` | ❌ | Ler `word/document.xml`, separar capítulos por estilo de título ou quebra de página | M |

### Aplicativo

| # | Funcionalidade | Evidência | Escriba | O que é preciso | Esforço |
|---|---|---|---|---|---|
| 49 | Temas | skins (`assets/themes`, 12+) | 🟡 4 temas | Mais paletas: é só dado | P |
| 50 | Menu lateral | `#sidebar` | ✅ | — | — |
| 51 | Interface em pt, en e es | `renderer/locales/` | ❌ | Mover os textos para Compose Resources (`values/`, `values-en/`, `values-es/`) | M |
| 52 | Boas-vindas e tutoriais | `onboarding/`, `tutoriais/` | ❌ | Fluxo de primeira execução e dicas contextuais | P |
| 53 | Instalador e atualização automática | `electron-updater`, `app-update.yml` | 🟡 MSI pelo Compose | Assinar o instalador e verificar atualizações (Hydraulic Conveyor ou endpoint próprio) | M |
| 54 | Perfil do autor | `perfil:*` | ❌ | Nome, foto e bio usados na exportação | P |
| 55 | Estado da IA e escolha de modelo | `ai-health:*`, `ai-catalog:*` | ❌ | Vem junto com o item 56 | — |
| 56 | Assistente de IA (correção, chat, rascunhos, glossário, guia de estilo, contexto do livro) | `services/ai/*` (Ollama, OpenAI, Anthropic, Gemini) | ❌ | Cliente HTTP (Ktor) com provedores plugáveis: Claude pela API da Anthropic e modelos locais pelo Ollama. Montagem de contexto com fichas e resumo, chave guardada no cofre do sistema | G |
| 57 | Licença, ativação, preços, Portal | `licenca:*`, `pricing:*`, `portal:*` | — | Decisão comercial sua, não é funcionalidade de escrita | — |

## 3. Placar

Considerando 55 funcionalidades (o item 57 fica de fora):

- **Por contagem (18/10/2026):** 15 completas e 6 parciais, ou cerca de **33%**. A contagem anterior estava inflada: tinha 12 completas, e o roteiro de cenas (item 19) caiu para parcial ao comparar os modelos.
- **Ajuste (11/11/2026):** depois do levantamento do Universo (seção 9), os itens 24 e 25 caíram de completos para
  parciais. No mesmo dia, a Fase 1 do Universo (9.6) completou os itens 24, 25 e 27.
- **Por esforço:** bem menos, entre 5% e 10%. O que falta inclui as partes mais caras do Autor: diagramação, revisão
  PT-BR com dicionários, mapas com arte, IA e sincronização.

## 4. Roteiro até a paridade

A ordem respeita as dependências. O **modelo de documento em blocos** (item 3) vem cedo porque editor rico,
imagens, notas, PDF, EPUB e diagramação dependem dele.

| Fase | Conteúdo | Itens | Duração estimada |
|---|---|---|---|
| 1. Fundação | Backup e recuperação, modelo em blocos com editor rico, importar DOCX/TXT, partes, notas, imagens | 39, 40, 41, 3, 4, 48, 6, 7, 5 | 6–8 semanas |
| 2. Revisão PT-BR | LanguageTool, repetições/ecos, legibilidade, sinônimos | 13, 15, 16, 14 | 4–6 semanas |
| 3. Publicação | EPUB, PDF simples, capa com ISBN e mockup, diagramação profissional | 46, 44, 47, 45 | 10–16 semanas |
| 4. Mundo | Relações e grafo, árvore, calendário e linha do tempo, imagens, universo compartilhado, quadro de enredo, idiomas | 26, 29, 30, 27, 28, 20, 21, 31 | 8–12 semanas |
| 5. Assistente e hábitos | IA (com resumo e contradições), ritual e sprints, paleta de comandos, agenda, histórico | 56, 18, 17, 11, 10, 22, 35 | 6–8 semanas |
| 6. Acabamento | Idiomas da interface, boas-vindas, perfil, temas, instalador e atualização, música, novels, busca global | 51, 52, 54, 49, 53, 12, 23, 38 | 4–6 semanas |
| 7. Opcional | Mapas, sincronização com conta | 32, 42 | 3+ meses cada |

**Total sem a fase 7: cerca de 9 a 14 meses para uma pessoa em tempo integral.** As estimativas são
aproximadas e não incluem a arte (ornamentos, carimbos de mapa) nem testes com usuários.

## 5. O que muda na arquitetura do Escriba

- **Documento em blocos** no lugar de `chapters/<id>.md`, com migração automática dos livros existentes.
- **Ktor client** para IA e verificação de atualizações.
- **SQLite** (por exemplo, SQLDelight) para o índice de busca, o tesauro e o dicionário. Arquivos JSON não escalam para isso.
- **Motor de paginação** separado da interface, em `commonMain`, com saída para PDF (PDFBox na JVM).
- **Dados com licença conferida:** dicionário (VERO/LanguageTool), tesauro, padrões de hifenização, perfis ICC (baixados da ECI),
  arte CC0 ou própria. **Nada extraído do `app.asar` do Autor.**

## 6. Onde o Escriba já se diferencia

- Exporta Markdown e HTML, que o Autor não oferece.
- Lógica testada (23 testes) e independente de plataforma, então pode virar app Android ou iOS reaproveitando o `commonMain`.
- O inspetor mostra quais fichas do Universo aparecem no capítulo, enquanto você escreve.
- Versões automáticas antes de substituições em massa e restaurações.

## 7. Manuscrito: paridade com o Autor (17/10/2026)

Levantamento feito usando os dois apps lado a lado (barra superior, barra flutuante, menus de capítulo, painel
lateral e barra de status do Autor 1.1.25). ✅ tem · ❌ não tem.

| Funcionalidade do Autor | Escriba | Observação |
|---|---|---|
| Texto formatado na tela (WYSIWYG) | ✅ | Marcadores somem fora do cursor e reaparecem apagados ao editar o trecho |
| Negrito, itálico, versalete, sobrescrito, subscrito | ✅ | Barra, barra flutuante e Ctrl+B/Ctrl+I; ligam e desligam |
| Título (H1), subtítulo (H2), chamada (H3), lista, citação, limpar | ✅ | Lista continua no Enter e termina com Enter num item vazio |
| Centralizar parágrafo; alinhamento geral (esq., just., centro, dir.) | ✅ | |
| Aspas, travessão, quebra de cena (ornamento centralizado) | ✅ | |
| Inserir imagem | ✅ | Copiada para `assets/` do livro; sai embutida no HTML e no DOCX |
| Nota de rodapé | ✅ | Notas no inspetor; DOCX com notas de rodapé reais |
| Medidas: fonte, entrelinha, largura, recuo e tamanho do recuo | ✅ | "Restaurar medidas padrão" faz o papel do Reset |
| Desfazer / refazer | ✅ | Pilha própria: desfaz também formatação e substituições |
| Título do capítulo editável na folha; Enter leva ao texto | ✅ | |
| Partes do livro fora da numeração (prólogo, dedicatória, epílogo…) | ✅ | Botão "Nova parte" e "Tipo" no menu do capítulo |
| Status do capítulo | ✅ | Escriba tem também "Ideia" |
| Modo foco com destaque do parágrafo atual | ✅ | |
| Localizar e substituir, apagar todas, no capítulo ou no livro | ✅ | |
| Blocos de notas ao lado do texto | ✅ | Valem para o livro todo |
| Barra de status: palavras, parágrafos, página x de y, leitura, notas, meta | ✅ | Páginas estimadas a 250 palavras |
| Criar ficha no Universo a partir da seleção | ✅ | |
| Correções ao digitar: `--` → —, `...` → …, maiúscula no início | ✅ | Escriba faz também aspas curvas; opção desligável |
| Autocorreção de acentos e revisão ortográfica PT-BR, sinônimos | ❌ | Precisa de dicionário e tesauro com licença livre (VERO/LanguageTool) |
| Espaçamento entre parágrafos (Compacto/Normal/Arejado) | ✅ | Com ajuste fino de 0 a 2 em, no menu de medidas |
| Reordenar capítulos arrastando | ❌ | Por enquanto pelo menu (mover para cima/baixo) |
| Correção IA, resumo do capítulo, beta-reader | ❌ | Dependem de um provedor de IA (item 56) |
| Enviar trecho para Novel | ❌ | O Escriba não tem a seção Novels (item 23) |

## 8. Nova revisão do app inteiro (18/10/2026)

Os dois apps foram percorridos seção por seção de novo (no Autor, só olhando, sem editar o livro). A tabela da
seção 2 foi atualizada. Abaixo, o que ainda falta, em ordem de prioridade para quem escreve.

### 8.1 Riscos e defeitos no que o Escriba já tem

| Problema | Onde | Situação |
|---|---|---|
| **Sem backup automático nem recuperação após falha.** Se o disco ou a pasta do livro der problema, não há cópia. O Autor faz backup a cada 7 dias e tem a "Arca" | itens 39–41 | ❌ pendente |
| Versões e Busca mostram a marcação crua (`**`, `[^1]`, `{.smallcaps}`) desde que o editor passou a esconder os marcadores | `BookScreens.kt` (diff e trechos da busca) | ✅ corrigido: a busca procura no texto visível e Versões compara o texto visível, com aviso quando só a formatação mudou |
| Capítulo restaurado da lixeira volta no fim da lista, não na posição original (cenas do Enredo já voltam no lugar) | `TrashStore.restore` | ✅ corrigido; "Restaurar todos" desfaz as exclusões em ordem inversa |
| Versões do capítulo ficam órfãs na pasta quando ele é excluído de vez da lixeira | `TrashStore.deleteForever` | ✅ corrigido |
| O app não reabre o último livro (a preferência `lastBookPath` é gravada, mas nunca lida) | `EscribaController` | ✅ corrigido: abre no último livro; esquece se ele for para a lixeira |
| Diálogos sem foco automático e sem Enter para confirmar (novo livro, renomear, ficha nova do Universo) | `Dialogs.kt`, `UniverseScreen.kt` | ✅ corrigido (foco, texto selecionado ao renomear, Enter confirma) |
| Lixeira sem filtro por tipo, "restaurar todos" e "esvaziar" | `AppScreens.kt` | ✅ corrigido: filtro por tipo, restaurar todos e esvaziar com confirmação |
| Mapa de calor sem rótulos de mês e dia da semana | `BookScreens.kt` | ✅ corrigido: semanas de domingo a sábado, meses no topo, dias ao lado e dica com data e palavras |

### 8.2 Funcionalidades do Autor que faltam, por seção

| Seção do Autor | O que tem lá | Escriba | Esforço |
|---|---|---|---|
| Biblioteca | Capa do livro, livros fixados, "inspiração do dia", atividade recente, abrir livro de qualquer pasta | Só a estante e os números | P–M |
| Manuscrito | Revisão ortográfica, autocorreção de acentos, sinônimos, arrastar capítulos | Ver seção 7 (o resto já tem) | M–G |
| Roteiro | 10 modelos de estrutura, salvar modelo próprio, vista em lista e em mural | 2 modelos, só quadro por ato | P |
| Enredo | Quadro livre infinito: cartões, personagens/lugares, imagens, textos, ligações com legenda de cores, arranjo automático, vistas "Linha do tempo" e "Família" | 🟡 tudo menos as vistas "Linha do tempo" e "Família" e o minimapa | M |
| Universo | Fichas com atributos, selo e retrato, calendário do mundo, idiomas inventados, Acervo e Unificar livros. Levantamento completo na seção 9 | Só fichas com resumo, notas e etiquetas | M–G cada |
| Novels | Trechos do manuscrito viram capítulos de uma novel, prontos para colar em plataformas | ❌ | M |
| Diagramação | Livro em páginas (formato, lombada, tema, numeração, tipografia do corpo, hifenização, capítulo abrindo em página ímpar), "Gerar PDF", e montagem de capa | ❌ | GG |
| Música | Biblioteca de faixas, álbuns e playlists para ouvir enquanto escreve | ❌ | M |
| Progresso | Palavras por hora, maior sequência, média por dia ativo, páginas aproximadas, capítulo mais longo, "quando você escreve" (dia × hora) | 🟡 meta, sequência, melhor dia, mapa de calor | P |
| Agenda | Data de lançamento, listas de hoje, amanhã e "no horizonte", calendário | ❌ | M |
| Mapas | Editor de mapas com pincéis, carimbos, estradas, rótulos, molduras, grades e exportação | ❌ | GG (arte própria ou CC0) |
| Publicar | Portal do Escritor (precisa de conta no serviço deles) | fora de escopo | — |
| Comando | Paleta de comandos (capítulos, fichas, cenas e ações) | ❌ | P |
| Sessão | Painel da sessão de escrita: meta, cronômetro, capítulo ativo, números do livro e do mundo | ❌ | P |
| Configurações | Atalhos configuráveis, perfil (nome, pseudônimo, foto), idioma, modo conforto, animações, autocorreção, intervalo de backup, chaves de IA | Tema, fonte, editor e dias na lixeira | P–M |
| IA | Correção, resumo de capítulo, beta-reader, chat (Claude, OpenAI, Gemini, Ollama) | ❌ | G |

### 8.3 Ordem sugerida

1. **Segurança dos dados:** backup automático em zip (com intervalo configurável), cópia opcional para pasta do
   OneDrive/Google Drive e recuperação após falha.
2. **Acabamento do que já existe:** Versões e Busca sem marcação crua, posição do capítulo restaurado, reabrir o
   último livro, foco e Enter nos diálogos, lixeira completa, rótulos do mapa de calor.
3. **Ganhos rápidos:** paleta de comandos (`Ctrl+K`), painel de sessão com cronômetro, mais modelos de roteiro e
   modelos próprios, mais estatísticas no Progresso, capa e livros fixados na Biblioteca, perfil do autor.
4. **Revisão PT-BR:** ortografia com sugestões, acentos automáticos e sinônimos (proposta já aberta em outra sessão).
5. **Saída:** EPUB e PDF simples; depois importação de DOCX/TXT.
6. **Mundo:** retratos e relações nas fichas, calendário do mundo, linha do tempo e árvore genealógica, universo
   compartilhado entre livros.
7. **Grandes módulos:** quadro de enredo livre, Novels, Agenda, Música, idiomas inventados, diagramação com PDF
   para gráfica, mapas e IA.

## 9. Universo: paridade com o Autor (11/11/2026)

Levantamento feito usando o Autor 1.1.25 no livro do usuário, com fichas de teste criadas e removidas em seguida.
Os comportamentos que não deu para disparar na tela (sem outra ficha ou porque o clique não tem volta) foram
confirmados pelos textos da interface no pacote do app, só para entender o comportamento. Nada disso deve ser copiado.

O Universo do Escriba hoje tem quatro tipos de ficha (Personagens, Locais, Grupos, Lore). Cada ficha tem nome,
resumo, anotações, etiquetas e "aparece em", e a tela tem um filtro por nome e etiqueta.

### 9.1 Estrutura

- O livro tem seis abas: **Personagens, Locais, Grupos, Lore, Calendário e Idiomas**. Calendário e Idiomas são do livro;
  as outras quatro também existem nos universos do Acervo.
- **Acervo**: universos independentes de livro, que podem servir vários livros.
  - "Novo universo" pede nome e descrição.
  - **"Importar do projeto atual"** copia na hora todas as fichas do livro para um universo novo.
  - **"Trazer de um livro"** mostra as fichas agrupadas por tipo, com caixa de seleção e busca. Avisa quando já existe
    ficha com o mesmo nome, e a cópia leva as imagens junto.
- **Unificar livros**: livros da mesma saga passam a usar as mesmas fichas, e o nome da saga é opcional. Fichas de
  mesmo nome viram uma só, e o que era diferente vai para uma seção "Em ‹outro livro›". Antes de juntar, o app guarda
  uma cópia das fichas de cada livro.
- **Lista**:
  - Cards com o retrato, ou as iniciais quando não há retrato, e um trecho da descrição; o × aparece ao passar o mouse.
  - A busca encontra por nome, apelido e texto da ficha.
  - Dentro da ficha, uma faixa no topo mostra as fichas do mesmo tipo, com um botão "+ Novo".
- **Lixeira das fichas**, uma por livro, aberta pelo botão "Lixeira (n)" no topo do Universo. Os itens mostram tipo,
  data e dias restantes, e podem ser restaurados ou apagados de vez. Guarda por 60 dias e vai junto com o livro no backup.
- Apagar um universo do Acervo pede confirmação e manda o universo para a lixeira geral, por 60 dias.

### 9.2 Fichas

Todas as fichas têm o mesmo esqueleto:

- **Retrato**: entra sem tela de recorte. Depois dá para preencher a moldura ou mostrar a imagem inteira, aproximar e
  afastar (botões ou roda do mouse) e voltar ao enquadramento.
- **Galeria**: cada imagem tem "Tornar retrato" e "Remover".
- **Blocos de texto rico**: cada um abre um editor em janela com negrito, itálico, sublinhado, listas e título.
- **"+ Nova seção"**: blocos com título e texto definidos pelo autor, que podem ser apagados.
- **Atributos**: campos curtos, alguns já definidos para o tipo de ficha. "+ Atributo" acrescenta um campo livre, com
  rótulo e valor.
- **Painel "No manuscrito"** (veja 9.3).

| Tipo | Título e subtítulo | Blocos | Atributos |
|---|---|---|---|
| Personagem | Nome · Papel na história | Descrição física, Personalidade, Arco do personagem, Aliados, Inimigos, Notas; Frase marcante | Apelidos, Grupo/subdivisão, Idade, Nasceu, Morreu (em branco = vivo), Ele ou ela, Olhos, Cabelo, Mão, Marca, Altura e porte, Pele, Corpo, Espécie/raça |
| Local | Nome · Tipo (cidade, floresta…) | Descrição, Notas | Outros nomes, Grupo/subdivisão, Clima/atmosfera, Habitantes |
| Grupo | Nome · Tipo (facção, guilda…) | Objetivo, Descrição, Notas | Outros nomes, Reino/aliança, Membros-chave |
| Lore | Título · Categoria (magia, história…) | Conteúdo, Notas | Outros nomes, Grupo/subdivisão |

Observações:

- **Idade** é um campo de texto livre. A ficha não calcula a idade a partir de "Nasceu": a conta é feita na Linha do
  tempo do Enredo, a partir do "presente da história" do calendário.
- **Aliados** e **Inimigos** são texto livre, um nome por linha. O campo não sugere nem liga fichas.

### 9.3 Selo, "No manuscrito" e contradições

- **Selo.** Os atributos de aparência e identidade têm um marcador (selo): Grupo, Ele ou ela, Olhos, Cabelo, Mão,
  Marca, Altura e porte, Pele, Corpo e Espécie. O que estiver nesses campos vira "fato selado", e o Manuscrito confere
  esses fatos contra o texto.
- **"Para conferir"**:
  - O app lê a Descrição física e propõe os fatos que encontrou, com um botão **"Selar na ficha"** que copia esses
    fatos para os campos com selo.
  - Testado com "Alto e magro, olhos azuis e cabelo ruivo, canhoto". O app propôs "cabelo ruivo / azul" e "alto e
    magro", ou seja, juntou a cor dos olhos à do cabelo e ignorou "canhoto".
  - Também não avisou que o campo Olhos já dizia "verdes". A ideia é boa, mas a extração é frágil.
- **"No manuscrito"**:
  - Mostra "Citado em N de M capítulos", com o número de ocorrências por capítulo e link para abrir cada um.
  - Procura pelo nome e pelos apelidos ou "outros nomes", e diz em quais termos procurou.
  - Avisa que nome escrito não quer dizer presença na cena.
  - Também aponta quando o mesmo nome está em Aliados e em Inimigos, e quando a relação está desencontrada entre duas
    fichas (aliado numa, inimigo na outra).
- **Contradições** ficam no Manuscrito:
  - Podem ser ligadas e desligadas, e mostram o que há "neste capítulo" e "nos outros capítulos".
  - "Onde foi dito" leva ao trecho que contradiz.
  - O "Relatório do livro inteiro" serve para conferir antes de publicar. Ele conta os fatos selados por ficha, lista
    as fichas que ainda têm fatos sem selo e mostra as idades que o texto menciona sem acusá-las, porque o personagem
    envelhece.

### 9.4 Calendário do mundo

- **Começo**: são três modelos.
  - Um exemplo completo, com meses e semana próprios, duas luas, duas eras, festas e constelações.
  - "Como o nosso", com 12 meses, semana de 7 dias, ano bissexto e uma lua.
  - "Em branco", com 12 meses de 30 dias.
  - Sem calendário próprio, o livro usa datas simples ("ano 12", "3 de março, 12").
- **Estrutura editável**:

  | Bloco | O que se define |
  |---|---|
  | Calendário | Nome |
  | Meses | Nome e número de dias; pode haver um mês curto de dias "fora do ano" |
  | Dias da semana | Lista livre de nomes |
  | Luas | Cor, nome e ciclo em dias |
  | Estações | Cor, nome, mês e dia de início |
  | Festas | Nome, mês, dia e descrição |
  | Eras | Nome, sigla e ano inicial |
  | Constelações | Cor, nome e o dia em que ficam no alto à meia-noite |
  | Sol | Mês e dia do dia mais longo; horas de luz no dia mais longo e no mais curto; horas do dia; temporada e janela de eclipses |
  | Presente da história | A data de onde a Linha do tempo parte e a idade das fichas é contada |

  Renomear um mês não perde datas, porque o "Quando" dos cartões do Enredo guarda o texto. Apagar um mês deixa as datas
  dele sem lugar, e a Linha do tempo aponta isso.
- **Tela principal**:
  - **Astrolábio**: um disco com os meses no anel de fora e as estações numa faixa colorida, mostrando as luas na fase
    do dia, a festa e a era. Clicar num mês leva a data até ele.
  - **Céu da cena**: hora com um cursor de 0 a 24h e atalhos (meia-noite, nascer, meio-dia, pôr do sol). Mostra luas e
    constelações nomeadas na posição certa. "Passar o dia" anima o céu: estrelas e luas andam, o sol nasce e aparecem
    partículas da estação.
  - **Resumo do dia**: horas de luz e a diferença para ontem, fase e % de iluminação de cada lua, e o dia da estação.
  - **Ano**: navegação ‹ ano › e "Ir ao presente". O gráfico "Estações e luz" mostra o ano inteiro (dia claro,
    crepúsculo e noite), com luas cheias e eclipses, e diz quando começa a próxima estação.
  - **Grade do mês**:
    - Usa a semana própria e mostra as fases das luas em cada dia.
    - Marca festas e cartões do Enredo naquele dia.
    - Ao clicar num dia, mostra estação, festa, sol e eclipses, com o botão "Fazer deste dia o presente da história".
  - **Painel das luas**: ciclo, fase, % iluminada, próxima cheia e próxima nova, régua de fases e aviso de "todas as
    luas cheias juntas".
  - **Rodapé**: uma linha de resumo (meses, dias, semana, luas, estações, festas, eras, constelações, sol e eclipses)
    e a legenda dos símbolos.
- **O que o calendário não faz**: não mostra aniversários das fichas.

### 9.5 Idiomas do mundo

- **Geração**:
  - A língua é gerada a partir do nome do livro e de um **estilo**. São seis estilos (fluido, áspero, solar, nórdico,
    insular e ritual), e cada um muda os sons e o clima da língua.
  - O mesmo livro sempre gera a mesma língua, e o número da língua aparece na tela.
  - "Sortear outra língua" sorteia outra semente, e "Voltar à anterior" desfaz um passo. Tudo fica salvo no livro.
- **A língua**: nome, assinatura na escrita própria, consoantes e vogais.
- **Gramática**: ordem da frase, posição do adjetivo, acento tônico, sufixos de plural, passado, futuro, negação e
  honorífico, "lugar de…" e direção da escrita. Também há regras como repetir a primeira sílaba.
- **Tradutor**:
  - Recebe uma frase em português e devolve a frase na escrita da língua, mais a transliteração.
  - Mostra um glossário palavra por palavra: palavra, significado e pronúncia, com a sílaba tônica marcada.
  - Palavras que a língua ainda não tem são criadas na hora e marcadas como "palavra nova".
  - Funciona em todas as variedades.
  - Tem dois botões de cópia: "na escrita da língua" (o texto aparece com o alfabeto inventado no Manuscrito, na
    Diagramação e no PDF) e "em letras nossas".
- **Nomes com significado**: listas de pessoas (com gênero) e de lugares, com pronúncia e sentido. "Outros nomes"
  gera mais.
- **Nome de fora**: adapta um nome estrangeiro aos sons da língua e mostra como fica na escrita.
- **A mesma palavra em cada canto**: para uma lista fixa de palavras básicas (água, fogo, mar, sol, noite…), mostra a
  evolução no tempo (proto → antigo → moderno, explicando cada regra sonora), nas regiões, nas classes sociais (corte,
  porto) e nas situações (formal, íntimo).
- **A escrita**: um alfabeto próprio, com um glifo por som.

### 9.6 O que falta no Escriba

| # | Funcionalidade | O que é preciso | Esforço |
|---|---|---|---|
| U1 | Ficha estruturada por tipo | Campos de título/subtítulo, blocos e atributos padrão por tipo (tabela 9.2), atributos livres e seções próprias. Migrar `summary`/`notes` para os blocos | M |
| U2 | Retrato e galeria | Imagens em `assets/` do livro, enquadramento (zoom e posição), "tornar retrato", retrato no card | P–M |
| U3 | Blocos com texto rico | Reaproveitar o editor do Manuscrito (Markdown) num diálogo | P |
| U4 | Busca por apelido e por texto da ficha | Incluir apelidos/outros nomes e o conteúdo dos blocos no filtro | P |
| U5 | "No manuscrito" completo | Contagem por capítulo, procurar também apelidos, abrir o capítulo | P |
| U6 | Selo e contradições | Atributos com selo; extração de fatos da descrição física; verificação no Manuscrito (por capítulo e relatório do livro); aviso de aliados e inimigos incoerentes | G |
| U7 | Lixeira dentro do Universo | Atalho para as fichas da lixeira atual, que já guarda fichas | P |
| U8 | Acervo de universos | Universo fora do livro, importar do livro e trazer fichas escolhidas | M |
| U9 | Unificar livros (saga) | Fichas compartilhadas por vários livros, com fusão por nome e cópia de segurança antes | M–G |
| U10 | Calendário do mundo: núcleo | Meses, semana, eras, festas, estações, "presente da história", grade do mês e três modelos | M |
| U11 | Calendário: astronomia | Luas com fases, sol e duração do dia, eclipses, constelações | M |
| U12 | Calendário: visual | Astrolábio, céu animado e gráfico do ano | G |
| U13 | Datas ligadas | "Quando" das cenas do Enredo no calendário, idade das fichas na Linha do tempo (item 21) | M |
| U14 | Idiomas: gerador | Semente e estilo, fonologia, gramática, léxico gerado, nomes com significado, nome de fora | G |
| U15 | Idiomas: tradutor e variedades | Tradução palavra a palavra com gramática, palavras novas, evolução por tempo, região, classe e registro | G |
| U16 | Idiomas: escrita própria | Alfabeto gerado como fonte que apareça no editor, na Diagramação e no PDF | G |

**Situação (11/11/2026):** Fase 1 entregue: **U1, U2, U3, U4, U5 e U7** prontos.
- Fichas antigas viram blocos ao abrir o livro (`core/universe/EntryMigration.kt`).
- O esquema de cada tipo está em `core/universe/EntrySchemas.kt`.
- As menções ficam em `core/universe/Mentions.kt`.
- A tela nova está em `ui/UniverseScreen.kt`.
- Testes: `UniverseTest.kt` (shared) e `UniverseScreenTest.kt` (desktopApp).

Calendário e Idiomas aparecem como abas desativadas até as fases 2 e 5.

**Situação (11/11/2026, mais tarde):** Fase 2a entregue: **U10** e **U13** (só a parte das cenas).
- Calendário do livro em `calendar.json`, com:
  - meses de tamanhos diferentes e meses fora da semana;
  - ano bissexto configurável;
  - semana com nomes, eras, estações e festas;
  - o presente da história.
- O cálculo das datas está em `core/calendar/CalendarMath.kt` e a leitura de datas digitadas, em `DateParser.kt`.
- Três modelos: um exemplo inventado (Aldamar), "Como o nosso" e "Em branco".
- A aba Calendário do Universo tem a grade do mês, o painel do dia e o editor da estrutura.
- As cenas do Roteiro ganham "Quando". A data é guardada pela id do mês, então renomear um mês não quebra nada.
- O Enredo ganha a vista "Linha do tempo", com:
  - a ordem por data e os intervalos entre as cenas;
  - o marcador do presente;
  - a idade de cada personagem a partir de "Nasceu" e "Morreu";
  - listas de cenas sem data e de datas com problema.
- Falta: dar data também aos cartões do Mapa. As luas, o sol e os eclipses ficam para a Fase 2b.

**Situação (12/11/2026):** Fase 2b entregue: **U11** (sem as constelações) e o gráfico do ano de **U12**.
- O cálculo está em `core/calendar/Astronomy.kt`:
  - Luas com ciclo e uma lua nova de referência: fase, % iluminada, próxima cheia e próxima nova, e o dia em que todas ficam cheias juntas.
  - Sol: luz do dia numa curva entre o dia mais longo e o mais curto, com nascer, pôr e a diferença para ontem.
  - Eclipses: do sol na lua nova e da lua na cheia, só dentro das temporadas de eclipse.
- "Como o nosso" usa valores reais. Nos testes, acerta, com folga de 1 dia, as luas de 2026 e os eclipses de 2025 e 2026.
- A tela mostra:
  - as fases na grade do mês e o eclipse marcado no dia;
  - o céu no painel do dia;
  - o gráfico "Estações e luz" do ano, que é clicável;
  - luas, sol e eclipses no editor da estrutura.
- Ficam de fora, como decidido: o astrolábio, o céu animado e as constelações.

**Situação (12/11/2026, mais tarde):** Fase 3 entregue: **U6**.
- Os atributos de aparência ganham um **cadeado** (selo).
- O extrator de fatos (`core/universe/FactExtractor.kt`) lê olhos, cabelo (cor e forma separadas), pele, altura e corpo, mão e marcas:
  - divide o texto em orações, então "olhos azuis e cabelo ruivo" não se mistura como no Autor;
  - normaliza gênero e número;
  - no texto do livro, só aceita altura e corpo depois de verbo de estado ("era alto", não "falou alto").
- **"Para conferir"** na ficha:
  - mostra o que a descrição física diz e marca os conflitos com os atributos;
  - "Selar na ficha" copia e sela, e nos conflitos você escolhe qual vale;
  - avisa quando aliados e inimigos não batem entre fichas;
  - lista as contradições do texto com aquela ficha.
- O detector fica em `core/universe/Contradictions.kt`.
  - O que ele confere:
    - o texto contra os fatos selados;
    - o texto contra ele mesmo, com "Onde foi dito" levando à primeira vez;
  - Como ele lê o texto:
    - atribui cada fato ao nome mais próximo, e a "ele/ela" quando a frase não tem nome;
    - ignora o que vem negado (não, nem, sem…);
    - lista as idades citadas sem acusá-las.
- No Manuscrito:
  - a conferência fica desligada até você ligar;
  - os trechos ficam sublinhados em vermelho;
  - o inspetor mostra "neste capítulo" e "nos outros";
  - há um "Relatório do livro inteiro" com fatos selados, contradições, relações, fichas a selar e idades.

**Situação (12/11/2026, mais tarde):** Fase 4 entregue: **U8** e **U9**.
- **Acervo:** universos fora de qualquer livro, em `<biblioteca>/Acervo/<nome>.universo/` (com `acervo.json`, `universe.json` e `assets/`). Tem:
  - "Novo universo" e "Importar do livro atual", que copia as fichas com as imagens;
  - a mesma ficha completa para editar (sem calendário nem idiomas, que são do livro);
  - lixeira de fichas;
  - apagar o universo inteiro, que vai para a lixeira e pode ser restaurado.
- **Trazer fichas**, no livro e no Acervo: escolhe a origem (outro livro ou universo do Acervo), mostra as fichas por tipo com busca e o aviso "já há uma ficha com este nome aqui", e copia com as imagens.
- **Unificar livros**, a saga:
  - **Antes de juntar:** guarda uma cópia do `universe.json` de cada livro em `<dados>/backups/saga-<data>/`.
  - **A junção:** fichas do mesmo tipo e nome viram uma só.
    - A base é a do livro aberto e o que falta vem dos outros.
    - Apelidos, etiquetas e imagens se somam.
    - O que difere vai para uma seção "Em ‹livro›".
    - Cenas e cartões passam a apontar para a ficha que ficou.
  - **Depois:** o universo da saga fica no Acervo. Mudar uma ficha em qualquer livro, ou no universo da saga, grava em todos, e as imagens são copiadas.
  - **Saída:** "Tirar este livro da saga" deixa uma cópia independente das fichas no livro.

**Situação (12/11/2026, mais tarde):** Fase 5 entregue: **U14** e **U15**.
- **Gerador** (`core/language/`):
  - O sorteio é reproduzível (SplitMix64). A semente vem do título do livro, e cada palavra tem a sua própria sequência de sorteio.
  - Há 6 estilos inventados para o Escriba: Rio, Pedra, Areia, Neve, Coral e Oráculo.
  - A língua sai com sons, moldes de sílaba, gramática (ordem da frase, adjetivo, acento, afixos de plural, passado, futuro, negação, honorífico e lugar, direção da escrita) e uma lista própria de cerca de 250 conceitos.
  - A saída fica travada por `GENERATOR_VERSION` e por um teste com valores fixos.
- **Tradutor:**
  - lematiza o português (verbos regulares e irregulares, plural e feminino);
  - divide a frase em orações e põe cada uma na ordem da língua;
  - aplica os afixos;
  - adapta nomes próprios;
  - cunha as palavras que faltam e as marca como "palavra nova", que entram no dicionário do livro quando a tradução é copiada.
- **Variedades:** 10 delas (proto, antigo, de hoje, três regiões, nobreza, mercado, cerimoniosa e familiar), feitas de regras de som sorteadas, cada uma com uma explicação.
- **Também na aba:**
  - nomes com significado para pessoas e lugares, com "Outros nomes";
  - "Nome de fora";
  - "A mesma palavra, em cada canto", com a história da palavra regra por regra;
  - dicionário;
  - "Sortear outra língua" e "Voltar à anterior".
- **Falta:** a escrita própria da língua, que é a Fase 6.

Ordem sugerida: **U1, U3, U4, U5, U2** (a ficha fica no nível do Autor); depois **U10 e U13** (calendário útil para a
história); depois **U8 e U6**; e, por último, **U11, U12, U9 e U14–U16** (os mais caros e menos essenciais).
