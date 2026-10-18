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
| 17 | Contradições | `manuscrito/contradicoes` | ❌ | Fatos das fichas (idade, cor dos olhos…) comparados com o texto: regras simples e depois IA | G |
| 18 | Resumo de capítulo | `chapter-summary-core.js` | ❌ | Via IA (item 56) ou resumo por extração | P (com IA) |

### Planejamento

| # | Funcionalidade | Evidência | Escriba | O que é preciso | Esforço |
|---|---|---|---|---|---|
| 19 | Roteiro de cenas com modelos | `outline:*` | 🟡 2 modelos | O Autor tem 10 (Save the Cat, Jornada do Herói, Story Circle, Sete Pontos, Kishōtenketsu, Fichtean, Romance, Mistério, Snowflake, Três Atos) e permite salvar e apagar modelos próprios | P |
| 20 | Quadro de enredo livre (canvas, ligações, minimapa) | `renderer/enredo/` (23 arquivos, 270 KB) | ❌ | Canvas com zoom e arrasto em Compose, cartões posicionados, setas, minimapa, gravação em `enredo.json` | G |
| 21 | Linha do tempo e análise de arco | `enredo-timeline.js`, `enredo-arc-analysis.js` | ❌ | Eixo temporal das cenas e curva de tensão por cena | M |
| 22 | Agenda e prazos ("Norte") | `norte:*` | ❌ | Calendário com marcos e prazos, ligado às metas | M |
| 23 | Novels (publicação seriada) | `novels:*` | ❌ | Episódios com data de publicação a partir dos capítulos | M |

### Universo

| # | Funcionalidade | Evidência | Escriba | O que é preciso | Esforço |
|---|---|---|---|---|---|
| 24 | Fichas por tipo | `universe:*` | ✅ | — | — |
| 25 | Onde a ficha aparece | `universe:fichas-do-livro` | ✅ | — | — |
| 26 | Relações entre fichas (grafo) | `universe-links-*.js` | ❌ | Lista `relations` (de, para, tipo) e grafo com layout de forças em Canvas | G |
| 27 | Retratos e imagens nas fichas | `universe-portrait-core.js` | ❌ | Imagem por ficha, recorte circular | P |
| 28 | Universo compartilhado entre livros | `universe:link-project`, `unificar:*` | ❌ | Universo como entidade própria, referenciada por vários livros | M |
| 29 | Árvore genealógica | `ferramentas/arvore-core.js`, `lentes/familia` | ❌ | Vem das relações do item 26; desenho em camadas | M |
| 30 | Calendário do mundo e linha do tempo | `calendario-mundo-core.js`, `linha-do-tempo-core.js` | ❌ | Calendário configurável (meses e dias próprios) e eventos | M |
| 31 | Idiomas inventados | `idiomas-*.js` | ❌ | Dicionário do idioma, regras de fonologia e gerador de palavras | M |
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
| 44 | PDF simples (manuscrito) | `export:pdf` | ❌ | Apache PDFBox (Apache 2.0) com página, margens, fonte embutida e títulos | M |
| 45 | Diagramação profissional | `renderer/diagramacao/` (87 módulos, 1,8 MB) e `services/diagramacao/` | ❌ | Motor de paginação próprio:<br>• quebra de linha Knuth-Plass<br>• hifenização Liang com os padrões TeX `hyph-pt` (conferir a licença)<br>• viúvas e órfãs, capitulares, ornamentos, cabeçalhos e numeração<br>• folha de rosto, sumário e índice<br>• PDF/X com `OutputIntent` ICC no PDFBox (o perfil ECI PSO Uncoated pode ser redistribuído) | GG |
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
| Enredo | Quadro livre infinito: cartões, personagens/lugares, imagens, textos, ligações com legenda de cores, arranjo automático, vistas "Linha do tempo" e "Família" | ❌ | G |
| Universo | Retratos, relações, calendário do mundo (modelos de meses e semanas), idiomas inventados (escrita própria, tradutor, nomes com significado), Acervo de universos compartilhados entre livros, Unificar livros | Só fichas com resumo, notas e etiquetas | M–G cada |
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
