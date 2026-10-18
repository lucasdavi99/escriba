# Engenharia reversa do Autor → Escriba

Relatório da análise do app **Autor 1.1.25** ("O Portal do Escritor") instalado em
`%LOCALAPPDATA%\Programs\Autor`, feita com o [REA](https://github.com/morluto/rea) (`rea-agents` 6.5.0),
e de como as funcionalidades equivalentes foram implementadas no Escriba (KMP / Compose Desktop).

> **Aviso de licença encontrado no próprio código.** Todo arquivo JavaScript do Autor começa com
> um aviso de software proprietário, com © 2026 Krigor Roncoleta Gonçalez. Ele afirma que o software
> "não [é] licenciado para redistribuição, derivação, reimplementação ou treinamento de modelo" e
> remete aos Termos (theautor.com/termos, cláusula 11). Por isso o Escriba foi escrito em modo
> *clean-room*: só se usaram evidências de **arquitetura e comportamento** (o que o app faz, como
> organiza os dados). **Nenhum código, texto, arte, fonte, dicionário nem formato de arquivo do Autor
> foi copiado.** Antes de distribuir ou vender o Escriba, leia esses Termos ou consulte um advogado.

## 1. Como a análise foi feita

| Passo | Ferramenta | Resultado |
|---|---|---|
| Identificar o tipo de app | listagem da pasta de instalação | Electron (`Autor.exe` + `resources/app.asar`, `LICENSE.electron.txt`, `chrome_*.pak`) |
| Extrair o pacote | leitor de ASAR em Python (formato: header JSON + blobs) | 7.054 arquivos, sem nada fora do pacote (*unpacked*) |
| Reconstruir o grafo da aplicação | `rea analyze-javascript-application app.asar --source-path …` (19 módulos centrais) | grafo com 10.977 nós e 11.377 arestas; grafo semântico com 72.665 nós |
| Ler os módulos de interesse | `grep` direcionado em `services/*.js` | layout de pastas, retenção da lixeira, tipos de ficha |

Identidade da evidência REA:

- `app.asar` SHA-256 `8e16cae033b587cf8666c0ca47136af4a98fb37c89245f59e91fac38f46b29de`
- evidence id `ev_43cfbf25499e155bb48cb308511d2c5246658c9b37ecf970a2a3e127d776dc52`

## 2. Evidências

### 2.1 Pacote e dependências (`package.json`)

`"name": "autor"`, `"version": "1.1.25"`, `"main": "electron.js"`. Dependências relevantes:
`docx` (exportar Word), `mammoth` (importar Word), `pdf-lib` + `@pdf-lib/fontkit` + `pdfjs-dist`
(diagramação e PDF), `archiver` (backup zip), `electron-updater`, `@sentry/electron`, `music-metadata`
(player de música).

### 2.2 Superfície Electron (REA `summary`)

| Métrica | Valor |
|---|---|
| `BrowserWindow`s | 3, com `contextIsolation: true`, `nodeIntegration: false` e `sandbox: true` |
| Preload | 1 (`preload.js`) |
| APIs do `contextBridge` | 2: `autorAPI` (278 membros) e `autorRuntimeFlags` |
| Operações IPC | 296, sendo 277 canais literais, 260 envios do renderer e 36 *listeners* |
| Handlers no main pareados pelo REA | 0. Os handlers provavelmente são registrados de forma indireta; isso não foi investigado |

### 2.3 Canais IPC por domínio (REA, nós `ipc-channel`)

Entre parênteses está o número de canais de cada domínio. Os que foram para o Escriba estão em **negrito**.

- **project (22)**: create, load, save, save-sync, rename, set-author, set-book-goal, set-cover, recent-list, crash-save…
- **snapshot (6)**: create, list, get, remove, diff-against, diff-between
- **trash (13)**: chapter, entry, project, outline, universe, beat, list, restore, restore-swap, hard-delete, purge-expired, get/set-retention
- **productivity (5)**: daily, heatmap, summary, active-time, clear
- **search (7)**: index-project, query, reindex-stale…
- **universe (14)**: create, add-entry, update-entry, delete-entry, fichas-do-livro…
- **export (3)**: docx, pdf, txt
- **settings (3)**: get, set, setMany
- outline (13), enredo (1), novels (1), norte (3): roteiro/*beats*, quadro de enredo e metas do livro
- diagramacao (17), dialog (8): diagramação gráfica, capa, EPUB, PDF/X e marcador de página
- ai\* (≈50): correção, chat, catálogo de modelos, Ollama/OpenAI/Anthropic/Gemini
- licenca (13), entitlements (5), pricing (1): licenciamento comercial
- nuvem (9), arca (10), local-backup (4): sincronia e backups
- music (11), mapas (1), vinculos (4), perfil (4), unificar (4)…

### 2.4 Modelo de dados no disco (`services/project-service.js`)

- Cada livro é uma pasta `Nome.autor/` com `project.json` (manifesto), `manuscript/<id>.md` (um
  arquivo Markdown por capítulo), `universe/{personagens,locais,grupos,lore}.json`,
  `outline/beats.json`, `enredo/boards.json`, `novels/novels.json` e `assets/images/`.
- O manifesto guarda id (UUID), nome, datas, sinopse, gênero, lista de capítulos, sessões e uma meta.
- A gravação é segura (`gravar-seguro.js`): escreve num arquivo temporário, faz `flush` e só então
  troca pelo arquivo final. Se a gravação falhar, o manifesto é recuperado na abertura.
- As versões ficam em `_snapshots/<capítulo>/…md`, dentro do livro.

### 2.5 Comportamentos observados

- **Lixeira** (`trash-service.js`): retenção padrão de 60 dias, configurável de 1 a 365. Os tipos são
  `project`, `chapter`, `entry`, `outline`, `universe` e `beat`. Ao restaurar um livro com nome
  repetido, ele volta como "(restaurado)".
- **Produtividade** (`productivity-service.js`): registro diário com `words`, `seconds`,
  `secondsActive` e `sessions`, mais *heatmap*.
- **Busca** (`search-index-service.js`): índice por projeto, com tipos `chapter`, `entry`, `beat` e
  `project-meta`.
- **Exportação** (`export-service.js`): títulos "Capítulo N — …", `**negrito**`/`*itálico*`, imagens com legenda e versaletes.
- **Backup local**: a cada 7 dias, em `Documents/Autor Backups`.
- **Preferências** (`settings-service.js`): `settings.json` em `userData`.

## 3. Mapeamento Autor → Escriba

| Funcionalidade (evidência) | Escriba | Onde |
|---|---|---|
| Biblioteca de livros, criar e abrir (`project:*`) | pasta própria `Nome.escriba/` com `book.json`, `chapters/<id>.md` e `universe.json` | [BookRepository.kt](../shared/src/commonMain/kotlin/com/cuboide/escriba/core/storage/BookRepository.kt) |
| Gravação segura (`gravar-seguro`) | grava em `.tmp`, faz `fsync` e um `ATOMIC_MOVE` | [JvmFileStore.kt](../shared/src/jvmMain/kotlin/com/cuboide/escriba/platform/JvmFileStore.kt) |
| Capítulos: renomear, reordenar, status | menu de cada capítulo; status Ideia, Rascunho, Revisão e Final | [WorkspaceScreen.kt](../shared/src/commonMain/kotlin/com/cuboide/escriba/ui/WorkspaceScreen.kt) |
| Salvamento automático (`project:save`) | *debounce* de 800 ms, `Ctrl+S` e salvamento ao trocar de capítulo ou fechar o livro | [EscribaController.kt](../shared/src/commonMain/kotlin/com/cuboide/escriba/app/EscribaController.kt) |
| Versões e diff (`snapshot:*`) | versão manual e versões automáticas antes de substituir ou restaurar; diff palavra a palavra (LCS) | [SnapshotStore.kt](../shared/src/commonMain/kotlin/com/cuboide/escriba/core/storage/SnapshotStore.kt), [WordDiff.kt](../shared/src/commonMain/kotlin/com/cuboide/escriba/core/text/WordDiff.kt) |
| Lixeira com retenção (`trash:*`) | livros, capítulos e fichas; padrão de 60 dias (1 a 365); restaurar e excluir definitivamente | [TrashStore.kt](../shared/src/commonMain/kotlin/com/cuboide/escriba/core/storage/TrashStore.kt) |
| Produtividade (`productivity:*`) | palavras e tempo ativo por dia, sequência de dias, melhor dia, *heatmap* de 30 dias | [ProductivityStore.kt](../shared/src/commonMain/kotlin/com/cuboide/escriba/core/storage/ProductivityStore.kt) |
| Metas (`set-book-goal`) | meta diária e meta do livro, com barras de progresso | painel Metas |
| Universo (`universe:*`) | fichas de Personagens, Locais, Grupos e Lore, com resumo, notas e etiquetas | painel Universo |
| Localizar e substituir | diferencia maiúsculas, palavra inteira e regex (`$1`); substitui no capítulo ou no livro | [FindReplace.kt](../shared/src/commonMain/kotlin/com/cuboide/escriba/core/text/FindReplace.kt) |
| Busca no livro (`search:*`) | busca em todos os capítulos, com trecho e destaque (`Ctrl+Shift+F`) | [BookSearch.kt](../shared/src/commonMain/kotlin/com/cuboide/escriba/core/text/BookSearch.kt) |
| Exportação (`export:*`) | Markdown, TXT, HTML e DOCX (OOXML gerado sem dependências) | [Exporters.kt](../shared/src/commonMain/kotlin/com/cuboide/escriba/core/export/Exporters.kt), [DocxWriter.kt](../shared/src/jvmMain/kotlin/com/cuboide/escriba/platform/DocxWriter.kt) |
| Preferências (`settings:*`) | tema escuro, fonte e largura do editor, retenção da lixeira | [SettingsStore.kt](../shared/src/commonMain/kotlin/com/cuboide/escriba/core/storage/SettingsStore.kt) |

A lógica fica em `commonMain` e não depende de plataforma. O que é específico do sistema (arquivos,
UUID, datas, DOCX, diálogo "Salvar como") é um `expect`, implementado em `jvmMain`. Para levar o app a
Android ou iOS, basta implementar esses `actual`.

## 4. Fora do escopo desta versão

Diagramação gráfica (PDF/X, EPUB, capas, ornamentos), quadro de enredo e roteiro, mapas, assistentes de
IA, sincronia na nuvem, licenciamento, player de música e importação de DOCX.

## 5. Verificação

- `./gradlew :shared:jvmTest`: 15 testes passando, cobrindo contagem de palavras, localizar e
  substituir, diff, datas, busca, repositório, lixeira (ida e volta, origem ausente, expiração),
  versões, produtividade e Markdown.
- As telas foram renderizadas fora da tela com `ImageComposeScene` (imagens em `docs/screenshots/`). O
  `.docx` gerado foi aberto como zip, e cada parte XML foi validada com um parser.
- O que não foi testado: uso interativo da janela real (digitar, menus e o diálogo nativo de salvar) e
  abrir o `.docx` no Word ou no LibreOffice.

## 6. O que ainda falta para igualar o Autor

O levantamento vem do menu lateral do Autor (`index.html`, `nav-btn data-s=…`), dos canais IPC (seção 2.3) e das
pastas de `renderer/` e `services/`. Em cada área, **✓** marca o que o Escriba já tem.

| Área | Autor tem | Escriba hoje |
|---|---|---|
| Editor | editor rico (WYSIWYG) com títulos H1–H3, listas, citação em bloco, alinhamento, espaçamento entre parágrafos, imagem no texto, notas de rodapé e "Partes" agrupando capítulos | ✓ negrito/itálico/aspas/travessão/quebra de cena, fonte, entrelinha, largura, recuo, modo foco. ✗ Marcação aparece como texto (`**`); sem imagens, listas, partes e notas |
| Revisão de texto | autocorreção PT-BR (dicionário de 3,4 MB), revisor, sinônimos, detector de repetições, legibilidade, "ecos" de palavras, resumo de capítulo | ✗ nada disso |
| Planejamento | **Roteiro** (beats) e **Enredo** como quadro livre com ligações, minimapa, linha do tempo, análise de arco, consistência, importação de Markdown e "lentes" | ✓ quadro de cenas por ato com modelos. ✗ quadro livre, ligações, linha do tempo e análises |
| Universo | relações entre fichas (grafo), retratos/imagens, universo compartilhado entre livros, árvore genealógica, calendário do mundo, linha do tempo, detector de contradições, idiomas inventados | ✓ fichas com resumo/notas/etiquetas e "aparece em". ✗ todo o resto |
| Mapas | editor de mapas com milhares de carimbos e terrenos, exporta PNG | ✗ |
| Agenda | "Norte": agenda e prazos com sinais do projeto | ✗ |
| Ritual de escrita | sessões/sprints com cronômetro, player de música, paleta de comandos | ✗ (só modo foco) |
| Novels | publicação seriada (vários "novels" por livro) | ✗ |
| Diagramação | livro impresso: PDF e PDF/X CMYK com perfil ICC, hifenização PT-BR, capitulares, ornamentos, kerning, margens ópticas, imposição, folha de rosto, índice remissivo, capa com lombada, ISBN/EAN-13, marcador, mockup 3D | ✗ |
| Exportar / importar | DOCX, PDF, TXT, EPUB, Kindle (com prévia); importa DOCX/TXT; localiza livros no disco | ✓ MD, TXT, HTML, DOCX. ✗ PDF, EPUB, Kindle, importação |
| IA | correção, chat, rascunhos múltiplos, prompts próprios, glossário, guia de estilo, contexto do livro; Ollama local, OpenAI, Anthropic, Gemini | ✗ |
| Segurança dos dados | backup local automático (7 dias), "Arca" (cópia para pastas de nuvem), recuperação após falha, sincronização com conta | ✓ gravação atômica, versões, lixeira. ✗ backup automático, recuperação, nuvem |
| App | atualização automática, perfil com foto, 12+ temas, vários idiomas, tutoriais, avisos, histórico de atividade | ✓ 4 temas e menu recolhível. ✗ o restante |
| Comercial | licença, ativação, preços, Portal do Escritor | fora de escopo |

Ordem sugerida, pelo valor para quem escreve:

1. Backup automático e recuperação após falha.
2. Editor rico, para o texto aparecer formatado e não com `**`, mais importação de DOCX.
3. Exportar PDF e EPUB.
4. Revisão PT-BR: repetições, legibilidade e sinônimos.
5. Universo: relações, linha do tempo e árvore genealógica.
6. Partes e notas de rodapé.
7. Sessões de escrita com cronômetro.
8. IA opcional.
9. Mapas e agenda.
