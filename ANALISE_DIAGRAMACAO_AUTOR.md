# Análise da diagramação do Autor × Escriba

Levantamento de 27/10/2026 sobre o **Autor 1.1.26** (`%LOCALAPPDATA%\Programs\Autor`, `app.asar` SHA-256
`67870045e15433a0d886fc356b3a7ea685836d9c5405dda5bfb69a89010d1341`) comparado ao **Escriba** no estado atual da
árvore de trabalho (branch `main`, commit `09ab89e` **mais** o trabalho de capa ainda não commitado: `Cover.kt`,
`Isbn.kt`, `CoverRendering.kt`, `LayoutScreen.kt` e testes).

> **Licença.** Todo arquivo JavaScript do Autor declara que o software não é licenciado para "derivação,
> reimplementação". Esta análise registra **comportamentos observáveis e requisitos funcionais**. Não copia código,
> textos, arte, valores internos nem o formato de arquivo do Autor. As recomendações descrevem o que o Escriba deve
> fazer, e não como o Autor faz por dentro.

Legenda de evidência usada em todo o documento:

| Marca | Significado |
|---|---|
| **[UI]** | Observado diretamente na interface do Autor, com a interação executada |
| **[DADOS]** | Lido no `diagramacao.json` ou nos arquivos do projeto do Autor (só leitura, a partir de uma cópia de backup) |
| **[REA]** | Obtido pela análise estática com o REA (`rea-agents` 6.5.0) |
| **[CÓDIGO]** | Confirmado lendo o código-fonte do Escriba |
| **[TESTE]** | Confirmado por teste automatizado executado nesta análise |
| **[INFERÊNCIA]** | Conclusão técnica ainda não confirmada; precisa de validação |

---

## 1. Resumo executivo

**Onde o Escriba está.** O motor de miolo do Escriba é sólido: Knuth-Plass, hifenização PT-BR, viúvas e órfãs, notas
no pé, aberturas de capítulo, sumário em duas passagens, prévia idêntica ao PDF e fontes embutidas
[CÓDIGO][TESTE]. A capa (ainda não commitada) já calcula lombada, orelhas, sangria e marcas de corte e dobra,
gera o código de barras EAN-13 a partir do ISBN e bloqueia a exportação quando falta algo essencial. Nisso ela vai
além do Autor, que pede uma imagem pronta do código de barras [UI][CÓDIGO].

**A diferença principal não está no motor, e sim no modelo de edição.** O Autor é um editor de páginas: clica-se na
página, aparece a caixa de texto com alças e medidas em mm, e cada ajuste pode valer para o livro inteiro **ou só
para aquela página**. Esses ajustes ficam ancorados ao conteúdo (história → parágrafo → linha), há objetos livres
(texto, imagem, enfeite) com camadas e contorno, uma barra de ações por parágrafo, menu de contexto com estilos de
parágrafo e desfazer com histórico nomeado [UI][DADOS]. O Escriba é um **formulário de configurações globais com
uma prévia estática**. As páginas são bitmaps sem nenhuma interação, não existe ajuste por página nem desfazer na
diagramação [CÓDIGO].

**Riscos confirmados no Escriba (P0):**

1. **Os campos numéricos da Diagramação não aceitam a digitação normal.** O campo reformata e limita o valor a cada
   tecla. Depois de selecionar tudo e digitar "12" numa margem (mínimo 5), o valor vira 52. Digitar "10" no corpo
   (mínimo 7) dá 18. O mesmo acontece no corpo do título da capa [CÓDIGO]. A causa está em
   `LayoutScreen.kt:664-678`.
2. **Um `layout.json` ilegível é trocado em silêncio pelas configurações padrão**, e a próxima alteração grava
   esse padrão por cima do arquivo do usuário [CÓDIGO], em `LayoutStore.kt:10-13`.
3. **O PDF pode sair de uma diagramação antiga.** Nos 350 ms entre uma alteração e o início do recálculo, o botão
   "Gerar PDF" continua ativo e exporta a diagramação anterior. A capa também usa esse miolo antigo para a lombada
   [CÓDIGO], em `EscribaController.kt:957-994` e `1026-1041`.

**Oportunidades de maior retorno, em ordem:** corrigir os P0; desfazer/refazer na diagramação; espessura do papel
editável e mais papéis; conferência da medianiz pelo número de páginas; enquadramento da arte da capa (arrastar e
zoom); manipulação direta das margens globais na prévia. Só depois disso vale o passo estrutural: ajustes por página
ancorados ao conteúdo, objetos livres e estilos de parágrafo. CMYK/PDF-X, capa dura, capa de ebook e o mockup 3D
completam o conjunto.

**Gargalo arquitetural.** O `LaidOutBook` é uma saída descartável, sem identidade estável de parágrafo ou linha, e o
`LayoutSettings` é só global. Antes de qualquer edição na página que precise sobreviver a mudanças no texto, é
preciso criar um **modelo de ajustes ancorados** e uma **camada de coordenadas pt↔tela** sobre a prévia (seção 12).

---

## 2. Escopo e metodologia

### 2.1 Métodos

| Método | O que foi feito |
|---|---|
| Interface do Autor | O Autor já estava aberto no projeto real "The Witcher - New Dawn" (1 capítulo, 12 páginas, capa com arte). Foram abertos todos os menus da barra do miolo, os diálogos de Lombada, Numeração, Conferência, Nova seção e Arte da letra, o menu de contexto, a barra flutuante por parágrafo, os painéis de objeto de texto e de estilos, as quatro abas da capa, "Montar capa" e a visualização 3D (girar, centralizar, brochura/capa dura) |
| Testes destrutivos controlados | Inserir objeto de texto, arrastar o objeto e arrastar a borda inferior da caixa de texto, **desfazendo tudo em seguida** |
| Dados do Autor | Backup completo da pasta do projeto antes de qualquer interação. Leitura da árvore de chaves de `diagramacao.json` e de `cover-mockup/meta.json`. Comparação do arquivo vivo com o backup depois de cada teste |
| REA | `analyze-javascript-application` com nove módulos selecionados do `app.asar` instalado (seção 11) |
| Código do Escriba | Leitura de `LayoutSettings.kt`, `Cover.kt`, `Isbn.kt`, `Typesetter.kt` (estrutura), `LayoutStore.kt`, `LayoutScreen.kt`, `LayoutRendering.kt`, `CoverRendering.kt`, `Layout.jvm.kt` e das funções de diagramação e capa do `EscribaController.kt` |
| Testes do Escriba | `./gradlew :shared:jvmTest :desktopApp:test`: **109 testes, 0 falhas** (94 em `shared`, 15 em `desktopApp`) |

### 2.2 Restrições e incidentes

- **Plano do Autor limitado a 1 projeto.** Não deu para criar um livro de teste ("Limite de projetos — 1 de 1").
  Toda a inspeção foi feita no livro real, com backup prévio e verificação por diff.
- **Incidente corrigido.** Uma rolagem do mouse sobre o painel lateral caiu num campo numérico e alterou o "Espaço
  entre ¶" da página 1 de 0,4 para 0,35. O Autor salvou isso sozinho. O valor foi restaurado pelo próprio campo, e o
  `diagramacao.json.bak` (cópia interna do Autor) foi restaurado do backup. A verificação final por diff deu
  `diagramacao.json`, `.bak` e `project.json` **idênticos ao backup**.
  - Lição para quem repetir: no Autor, a roda do mouse sobre campos numéricos muda o valor e salva na hora. Role
    pela área da página, nunca pelo painel.
- **Ações evitadas para não alterar dados reais:**
  - Aplicar um estilo de "Montar capa": o editor de capa não tem desfazer.
  - Arrastar ou "Enquadrar" a arte da capa: isso mudaria o enquadramento salvo.
  - "Trazer livro": reimportaria o manuscrito.
  - Aplicar tema ou "Recomeçar montagem".
  - Inserir seções.
- **Exportação de PDF do Autor não concluída.** Antes de exportar, o Autor exige marcar "Compreendo que a
  conferência final é de minha responsabilidade. Aceito." Esse aceite não foi dado em nome do usuário, então os PDFs
  do Autor (perfis CMYK, caixas, marcas) **não foram inspecionados**.
- **Prévia Kindle não abriu** nenhuma janela visível no clique (não verificado).
- **Escriba sem execução interativa.** O app não foi aberto. Os fluxos foram verificados pelo código e pelos testes
  automatizados, inclusive `CoverTabTest`, que monta a aba de capa em Compose.

### 2.3 Cobertura

| Área do Autor | Inspecionada | Observação |
|---|---|---|
| Entrada da diagramação (Diagramar Livro / Diagramar Capa) | ✅ | |
| Barra do miolo: Modos, Trazer livro, Salvar, ↶ ↷, Tema, Página, Mostrar, Inserir, Numeração, Exportar, Zoom, Ajustar, Conferência, Gerar PDF | ✅ menus abertos | Gerar PDF e Trazer livro não foram executados |
| Painel "Tipografia do corpo" (livro inteiro) e "Só na página" | ✅ | |
| Caixa da escrita: alças, medidas, arrastar a borda | ✅ testado e desfeito | |
| Objeto de texto: inserir, arrastar, painel completo | ✅ testado e desfeito | Moldura, preenchimento e contorno não foram aplicados |
| Barra flutuante por parágrafo (6 ações) | ✅ dicas lidas | Ações não executadas |
| Menu de contexto do texto | ✅ | Estilos não aplicados |
| Lista de capítulos (Editar, Soltar, título, Abertura, Simples) | ✅ dicas lidas | |
| Nova seção / Montar pelo guia | ✅ | Nada criado |
| Estilos do parágrafo, Arte da letra, Camadas | ✅ abertos | |
| Conferência, ficha técnica e passo a passo | ✅ | |
| Capa Impressa, Capa dura, Ebook, Marcadores | ✅ | Arte, imagens e montagem não foram aplicadas |
| Montar capa | ✅ painel | Estilo não aplicado |
| Livro 3D | ✅ girar, centralizar, brochura/capa dura | A roda do mouse não mostrou zoom visível |
| Exportar PDF (miolo e capa), EPUB, cópia para leitor | ⚠️ | Bloqueado pelo aceite de responsabilidade; não verificado |
| Prévia Kindle | ❌ | Não abriu |
| Guias manuais, imagem dupla, soltar título, ± página, sumário auto | ⚠️ só o rótulo no menu | Não executados |

---

## 3. Inventário de funcionalidades do Autor

Os IDs (M = miolo, C = capa, 3D, E = exportação) são usados na matriz da seção 9.

### 3.1 Miolo

| ID | Funcionalidade | Como funciona | Evid. |
|---|---|---|---|
| M01 | Modos | "‹ Modos" volta à escolha Diagramar Livro / Diagramar Capa | UI |
| M02 | Trazer livro | Reimporta o manuscrito. A diagramação guarda uma **cópia** do texto (`stories`, `source.snapshotAt`), e o que foi feito em "Nova seção" sobrevive à reimportação | UI, DADOS |
| M03 | Salvar + salvamento automático | Botão Salvar. Mesmo sem clicar, as mudanças são gravadas (visto no incidente). O arquivo anterior fica em `diagramacao.json.bak` | UI, DADOS |
| M04 | Desfazer/refazer com histórico | ↶ ↷ na barra. Cada desfazer mostra um aviso com o nome da ação ("Desfeito: Mover objeto.", "Desfeito: Nova caixa de texto.") | UI |
| M05 | Temas de miolo | Menu Tema: escolher tema, exportar e importar tema, "Minhas fontes…", "Recomeçar montagem" | UI |
| M06 | Página: tamanho, papel, sangria | Tamanho (ex.: 14 × 21 cm romance nacional), papel (ex.: Pólen Soft 80 g creme, alto bulk), sangria (Nacional 5 mm), "Ver" (Páginas abertas / Uma a uma) | UI |
| M07 | Lombada com espessura editável | Diálogo "Lombada do livro": 0,7 mm = 12 págs × 0,056 mm/pág. Campo "Espessura por página (mm) — se a gráfica informar o valor exato", botão "Usar o valor do papel" e "Capa aberta (contracapa + lombada + capa + sangria): 290,7 × 220,0 mm" | UI |
| M08 | Mostrar | Liga e desliga Guias, Cabeçalho e nº, Letra grande (capitular), Grade de base, Papel branco; "Arte da letra…" | UI |
| M09 | Arte da letra (capitular) | Escolher uma imagem de arte para a capitular e a "Fonte da letra grande" (padrão: a do corpo) | UI |
| M10 | Inserir | Nova seção, Sumário auto, Texto, Nota de rodapé, Imagem, Enfeite, Camadas, Imagem dupla, Soltar título, + Página, − Página, Guia vertical, Guia horizontal, Limpar guias | UI |
| M11 | Nova seção / Montar pelo guia | Seção livre com título, no começo (pré-textual) ou no fim (pós-textual), que sobrevive ao "Trazer livro". "Parte do livro" (Prólogo, Epílogo, Interlúdio…). Guia **antes da história**: falsa folha de rosto, folha de rosto, créditos, ficha catalográfica, dedicatória, epígrafe, sumário, aviso ao leitor, elogios, frontispício, nota do tradutor, personagens. **Depois da história**: agradecimentos, sobre o autor, do mesmo autor, colofão e mais 3. As páginas já nascem com título, autor e ano do livro | UI |
| M12 | Tipografia do corpo (livro inteiro) | Fonte, corpo (pt), entrelinha, alinhamento, recuo da 1ª linha (em), espaço entre ¶ e espaço após o título (em linhas), 1º parágrafo sem recuo, hifenizar, composição fina | UI |
| M13 | Tipografia "só nesta página" | O mesmo conjunto aplicado a uma página, com "↺" para voltar ao padrão. Fica salvo como `bodyTypography[]`, com `physicalPage` e uma **âncora** (`storyIndex`, `paragraphIndex`, `line`) | UI, DADOS |
| M14 | Caixa da escrita Global / Só desta página | Margens Sup., Inf., Dentro e Fora em mm. Alterna entre Global e "Só desta página"; "★ Definir como global" promove o ajuste da página. Salvo como `bodyFrames[]` com margens e âncora | UI, DADOS |
| M15 | **Manipulação direta da caixa da escrita** | Um clique no texto mostra a caixa com 8 alças, um rótulo "L 128,9 × A 180,0 mm", uma alça de arrasto no topo e as travas "L travar" e "A travar". Arrastar a borda inferior mudou só Inf. de 15 para 24 mm, refluiu o texto **na hora** e levou o livro de 12 para 13 páginas. Desfeito com ↶ | UI |
| M16 | Opções da página | "Ocultar cabeçalho nesta página", "Ocultar a numeração nesta página" | UI |
| M17 | Cabeçalho por capítulo | Título do livro (cabeçalho); para cada capítulo, texto da página esquerda e da direita (o padrão é o título do livro / do capítulo) e nº do capítulo (automático). "Estilo do cabeçalho (todo o livro)" | UI |
| M18 | Abertura de capítulo | "Onde cada capítulo abre": Direita / Esquerda / Próxima página, como padrão do livro. Um clique numa abertura muda só aquele capítulo | UI |
| M19 | Lista de capítulos | Página inicial de cada capítulo; ↑ ↓; Editar (texto do capítulo); Soltar (título vira objeto livre); ocultar título ("você pôs arte com o título embutido"); presets "Abertura" e "Simples" ("título solto e centralizado no alto") | UI |
| M20 | Barra flutuante por parágrafo | Ao lado do parágrafo, 6 ações: quebrar a página aqui (empurra o resto), inserir página em branco antes (para imagem de página inteira), soltar o parágrafo como caixa livre, quebra de cena depois, elemento de imagem depois (ampulheta/ilustração), centralizar o parágrafo (local/data, epígrafe) | UI |
| M21 | Menu de contexto do texto | Copiar, Recortar, Apagar; Alinhar à esquerda, Centralizar, Justificar; nota numerada ou nota com símbolo; "Pôr a capitular AQUI"; estilo do parágrafo: Corpo, Primeiro parágrafo, Epígrafe, Citação, Verso, Onomatopeia, Subtítulo, Bloco de cena, Mensagem recebida, Mensagem enviada | UI |
| M22 | Estilos de parágrafo editáveis | "Editar um estilo muda TODOS os parágrafos que o usam." "Detectar estilos no texto". Cada estilo tem alinhar, recuo, fonte… (ex.: "Primeiro parágrafo: abre o capítulo/cena sem recuo") | UI |
| M23 | Objeto de texto | Texto; estilo rápido (aplicar ou salvar estilo com nome). Tipografia: fonte, corpo, entrelinha, alinhamento horizontal e vertical, espaço entre letras (em), tinta (K%). Posição e tamanho X/Y/L/A em mm, "Centralizar na página", "Ocupa as duas páginas". Camada: contornar a caixa, contornar a forma, atrás do texto, na frente do texto, ⬇ Atrás, ⬆ Frente. Caixa: moldura (espessura, canto, estilo, tinta K%), preenchimento (K%), respiro interno nos 4 lados. "Padrões globais (livro inteiro)" | UI |
| M24 | Arrastar objeto | Arrastar move o objeto e atualiza X/Y em mm na hora (28 → 38,28 mm) | UI |
| M25 | Camadas | "Camadas — página N (frente → trás)": lista os objetos da página | UI |
| M26 | Guias e réguas | Réguas em mm nos dois eixos; guias manuais vertical e horizontal; limpar guias. No modelo: `guides[]`, `objectStyles[]`, `pageControls[]`, `assets[]` | UI, DADOS |
| M27 | Numeração (fólio) | Mostrar o número em (Todas as páginas…); na abertura de capítulo (Número normal…); "Cabeçalho também na abertura"; "A numeração começa em"; "Páginas em romano (i, ii…)" para os pré-textuais. Páginas em branco e de imagem cheia não mostram número, mas contam | UI |
| M28 | Fonte e arte do número | Fonte, tamanho, negrito, itálico, opacidade do número e da arte, "escorrido" da arte (topo, base, esquerda, direita), desfoque, "o enfeite pode passar do corte"; arte separada para a página esquerda e a direita, com posição em mm e "Centrar" | UI |
| M29 | Zoom | Controle deslizante com valor em %, mais "Ajustar" | UI |
| M30 | Conferência pré-impressão | Lista "tem coisa saindo ERRADA no impresso" com link para a página. Ex.: "Margem do lado da lombada com 3,8 mm (página 1): num livro de 12 páginas… O mínimo é 9,5 mm" (**mínimo varia com o número de páginas**). Dicas: falta a página de créditos; "12 aspas retas" | UI |
| M31 | Ficha técnica para a gráfica | Texto copiável com formato, número de páginas (cadernos de 16), papel, lombada, sangria e capa aberta | UI |
| M32 | Passo a passo para a gráfica | 6 passos: zerar o vermelho, imprimir 2 páginas em tamanho real, pedir prova, capa por último, mandar a ficha, perfil KDP × gráfica nacional | UI |
| M33 | Notas da conversão | O modelo tem `footnotes[]` e `notasDoManuscrito` (criadas, atualizadas, removidas, sem lugar), um relatório de sincronização das notas | DADOS |

### 3.2 Capa

| ID | Funcionalidade | Como funciona | Evid. |
|---|---|---|---|
| C01 | Abas | Impressa, Capa dura, Ebook, Marcadores | UI |
| C02 | Formato e unidade | Formato (14 × 21 cm…) e Unidade (mm…) | UI |
| C03 | Lombada casada com o miolo | Campo da lombada (0,7) e aviso "Lombada casada com o miolo: 0,7 mm para 12 páginas" | UI |
| C04 | Orelha | Campo Orelha (mm) | UI |
| C05 | Guias e legenda | Guias liga/desliga; legenda corte, dobra, sangria e ISBN (zona tracejada no pé da contracapa); rótulos CONTRACAPA e CAPA; ornamentos na lombada | UI |
| C06 | Arte da capa | "Arquivo de capa…": **PDF é o formato recomendado** (o que sai do Photoshop/Illustrator); depois se **arrasta** para posicionar e usa **Ctrl+roda** para o zoom. A arte cobre a capa aberta inteira. No modelo: `artSource` (`cover-art/original.png`), `artUrl` (raster derivado) e `artFit` {scale, offsetX, offsetY} | UI, DADOS |
| C07 | Enquadrar e escala | "Enquadrar" (recentraliza a arte), escala com −/+ e campo em %, "Remover arte" | UI |
| C08 | + Imagem | "Importe uma imagem (logo, selo, código ISBN…) e posicione por cima da capa." O ISBN se encaixa na zona da contracapa. **Não foi visto nenhum gerador de código de barras na interface** | UI |
| C09 | Montar capa | 5 estilos (Clássico, Romance, Suspense, Sombrio, Leve). "O app preenche com o título, o autor, a sinopse e o ISBN… depois você muda o que quiser e arrasta cada peça na própria capa." A arte pronta continua por baixo e os textos ficam por cima | UI |
| C10 | Saída | Gráfica (CMYK) ou Sob demanda (RGB) | UI |
| C11 | Exportar capa (PDF) | Diálogo de salvar (nome sugerido "…-capa-cmyk"), depois o aceite obrigatório (botão liberado após 2 s) | UI |
| C12 | Painel lateral do livro | Miniatura, título, "140 × 210 mm · 12 pág · lombada 5,7 mm · capa dura", seletor BROCHURA / CAPA DURA, link "Diagramar a capa dura →" | UI |
| C13 | Capa dura | Encadernação (Amazon KDP capa dura, IngramSpark case laminate, Gráfica nacional papelão 2,5 mm). Lombada 0,7 vira **5,7 mm no case**. Zonas, "Arte da capa dura…", "Partir da brochura", Saída. Explica a **virada** (some para dentro da tampa) e a **praça** (sobra da tampa). Folha de 337,7 × 246 mm, virada de 15 mm | UI |
| C14 | Capa de ebook | Tamanho Kindle/KDP 1600 × 2560 ou "Igual ao livro impresso"; área segura; "Arte do ebook…"; "Usar a frente da capa impressa"; exporta JPEG | UI |
| C15 | Marcadores | Tamanho (5 × 15 cm Brasil…), sangria Brasil 3 mm, furo para o cordão, guias, vários marcadores (1/1, +), frente e verso, "Arte do lado…", "Usar a frente da capa", PDF "Uma por lado", saída CMYK, Exportar PNG, PDF de gráfica | UI |

### 3.3 Visualização 3D

| ID | Funcionalidade | Como funciona | Evid. |
|---|---|---|---|
| 3D1 | Acesso | Botão "Livro 3D" no editor de capa abre uma sobreposição sobre a tela. Há também uma miniatura do livro no painel lateral | UI |
| 3D2 | Modelo | Bloco com capa, lombada e quarta capa com a arte e o bloco de páginas brancas visível na lateral | UI |
| 3D3 | Controles | "Arraste = girar · Shift+arraste = mover · duplo clique = centralizar". O giro horizontal mostra a quarta capa. O arrasto vertical inclinou muito pouco e a roda do mouse não deu zoom visível | UI |
| 3D4 | Brochura × capa dura | "Ver livro em Capa dura" troca o modelo. Sem arte de capa dura, o livro aparece todo branco | UI |
| 3D5 | Origem das texturas | `cover-mockup/` guarda `front.jpg`, `back.jpg`, `spine.jpg` e `meta.json` {trim, spineMm, pages}: o 3D usa texturas pré-renderizadas da capa | DADOS |
| 3D6 | Inconsistência | O 3D mostrou "lombada 1 mm · 12 pág." enquanto o editor mostrava 0,7 mm. O `meta.json` tinha `spineMm: 1, pages: 3`, ou seja, texturas de quando o livro tinha 3 páginas | UI, DADOS |
| 3D7 | Módulos | `book3d-core.js` (14 funções) e `book3d-realistic.js` (14 funções, 395 chamadas) | REA |

Fluxo completo observado: Diagramação → Diagramar Capa → (arte e imagens) → Livro 3D (sobreposição) → girar e
centralizar → alternar brochura/capa dura → fechar (✕) e voltar ao editor sem perder o estado.

### 3.4 Exportação

| ID | Funcionalidade | Como funciona | Evid. |
|---|---|---|---|
| E01 | Exportar (miolo) | E-book, Prévia Kindle, Cópia para leitor, PDF com perfis **Sob demanda / Gráfica (CMYK) / Gráfica colorida** | UI |
| E02 | Aceite antes da gráfica | Modal "Antes de mandar para a gráfica": confira o PDF, peça um boneco; o aceite só libera após 2 s | UI |
| E03 | Conferência do arquivo | `services/diagramacao/conferencia-impressao.js` importa `pdf-lib` (57 funções): indica que o Autor relê o PDF gerado | REA (inferência) |
| E04 | PDF/X, ICC, marcas, imposição | Nomes de módulos: `pdfx-output-intent`, `icc-transform`, `color-management`, `printer-marks`, `imposition`, `signatures`, `pdf-page-labels`, `pdfua-tags`, `pdf-metadata-xmp`, `pdf-konly` | Inventário de módulos (sem verificar o comportamento) |

---

## 4. Editor de miolo: análise detalhada

### 4.1 Como o Autor organiza a edição

- **Dois níveis de escopo sempre visíveis.** O painel direito mostra "Tipografia do corpo — LIVRO INTEIRO" em cima e
  "SÓ NA PÁGINA N" embaixo, cada um com um rótulo de escopo e "↺" para voltar ao padrão [UI]. A mesma ideia vale
  para a "Caixa da escrita (Global | Só desta página)" e para a abertura de capítulo, que tem padrão do livro e
  exceção por capítulo.
- **Os ajustes por página ficam presos ao conteúdo, não ao número da página.** `bodyFrames[0]` guarda
  `physicalPage: 1`, as margens e `anchor {storyIndex: 0, paragraphIndex: 0, line: 0}` [DADOS]. Quando o texto
  reflui, o ajuste acompanha o trecho. O conteúdo é uma cópia (`stories`) atualizada por "Trazer livro"; por isso a
  âncora por índice de parágrafo continua válida entre reimportações.
- **Manipulação direta.** A caixa de texto tem 8 alças, medidas em mm ao vivo e travas de largura e altura. Arrastar
  muda a margem do escopo atual e refaz a paginação na hora (12 → 13 páginas no teste) [UI].
- **Objetos livres.** Caixas de texto, imagens e enfeites com posição em mm, camadas, contorno do texto (pela caixa
  ou pela forma), moldura e preenchimento em K% (tinta de impressão) [UI].
- **Ações por parágrafo sem sair da página.** A barra flutuante e o menu de contexto aplicam quebras, centralização,
  estilos, notas e capitular [UI].
- **Conferência com links.** Cada problema aponta para a página ("pág. 1") [UI].

### 4.2 Medidas e regras observadas

| Regra | Valor observado | Evid. |
|---|---|---|
| Espessura do Pólen Soft 80 g | 0,056 mm por página (editável) | UI |
| Lombada | nº de páginas × espessura (12 × 0,056 ≈ 0,7 mm) | UI |
| Capa aberta | 2 × largura + lombada + 2 × sangria (2 × 140 + 0,7 + 2 × 5 = 290,7 mm; altura 210 + 10 = 220) | UI |
| Margem mínima do lado da lombada | 9,5 mm para 12 páginas; cresce com o número de páginas (a regra exata não foi extraída) | UI |
| Cadernos | Miolo em cadernos de 16; a gráfica completa até 16 | UI |
| Capa dura (papelão 2,5 mm) | lombada do case = lombada do miolo + 5 mm (0,7 → 5,7); virada de 15 mm | UI |
| Capa de ebook | 1600 × 2560 px (KDP) | UI |
| Marcador | 5 × 15 cm, sangria 3 mm | UI |

### 4.3 O que não foi verificado no miolo

A grade de base, as guias manuais, a imagem dupla, ± página, o sumário automático e "Soltar título" foram vistos só
como itens de menu, sem execução. O comportamento de contorno do texto (pela caixa ou pela forma) também não foi
executado.

---

## 5. Editor de capas: análise detalhada

- **Geometria.** Capa aberta = contracapa + lombada + capa (+ orelhas) + sangria, com a lombada "casada" ao miolo
  [UI]. A capa dura tem geometria própria, com papelão, virada e praça [UI].
- **Arte.** A capa é pensada para receber a arte **pronta**, de preferência em PDF vindo de um editor gráfico. O
  usuário enquadra a arte arrastando e com Ctrl+roda, e pode recentralizar ("Enquadrar") ou ajustar a escala em %
  [UI]. O modelo guarda o arquivo original e o enquadramento (`artFit`) separados [DADOS].
- **Textos.** Na aba Impressa, a barra não tem ferramenta de texto. Os textos vêm de "Montar capa", com 5 estilos
  preenchidos com título, autor, sinopse e ISBN, cujas peças se arrastam sobre a capa [UI]. Fonte, tamanho e
  alinhamento dessas peças **não foram verificados**, porque aplicar um estilo alteraria a capa real e o editor de
  capa não tem desfazer.
- **ISBN.** Há uma zona tracejada reservada na contracapa. O código entra como **imagem importada**, por "+ Imagem"
  [UI]. Existe um módulo `isbn-ean13.js` no pacote, mas nenhuma interface que gere o código foi vista.
- **Saída.** CMYK para a gráfica ou RGB para impressão sob demanda [UI].
- **Restrições.** O botão de exportar exige o aceite de responsabilidade [UI]. A conferência da capa bloqueia a
  exportação quando falta a arte ou quando o miolo tem número ímpar de páginas (registrado no levantamento de
  26/10, em `docs/diagramacao-autor.md`).

---

## 6. Visualização 3D: análise

- **Acesso e estado.** Botão "Livro 3D" que abre uma sobreposição modal. Ao fechar, o editor continua no mesmo
  estado (a aba, a arte e a posição foram preservadas) [UI].
- **Representação.** Frente, lombada e quarta capa com a arte; bloco de páginas brancas na lateral [UI]. As texturas
  vêm de imagens pré-renderizadas em `cover-mockup/` [DADOS].
- **Controles.** Girar arrastando (eixo vertical bem responsivo), mover com Shift+arraste, centralizar com duplo
  clique. A inclinação vertical foi mínima e o zoom pela roda não foi perceptível [UI]. Não há opções de fundo,
  iluminação, exportar imagem ou compartilhar [UI].
- **Formatos.** Brochura e capa dura. Sem arte de capa dura, o modelo aparece branco [UI].
- **Inconsistência.** Lombada de 1 mm no 3D contra 0,7 mm no editor, e `meta.json` com `pages: 3`: as texturas não
  foram regeneradas depois que o miolo mudou de tamanho [UI][DADOS].
- **Para o Escriba.** O valor está no "ver o livro de verdade" antes de imprimir. O risco que o Autor mostra (texturas
  velhas) se evita gerando as faces a partir do `CoverLayout` atual, sem cache em disco.

---

## 7. Importação, exportação e impressão

| Aspecto | Autor | Escriba |
|---|---|---|
| Origem do conteúdo | Cópia do manuscrito na diagramação, reimportada sob demanda ("Trazer livro") [UI][DADOS] | Lê os capítulos **ao vivo** a cada nova diagramação; não há o que reimportar [CÓDIGO] |
| PDF do miolo | Perfis Sob demanda, Gráfica (CMYK), Gráfica colorida [UI] | Um perfil RGB; fontes embutidas como subconjunto; TrimBox e BleedBox; marcas de corte opcionais; marcadores do PDF por capítulo; metadados básicos [CÓDIGO] |
| PDF da capa | CMYK ou RGB [UI] | RGB; TrimBox e BleedBox; marcas de corte e de dobra [CÓDIGO][TESTE] |
| PDF/X, ICC | Módulos presentes [inventário] | Ausente |
| Formato da arte da capa | PDF (recomendado) e imagens [UI] | Só PNG e JPG (`COVER_IMAGE_EXTENSIONS`) [CÓDIGO] |
| Ebook | EPUB, prévia Kindle, cópia para leitor, capa JPEG 1600 × 2560 [UI] | Ausente (o Exportar geral tem MD, TXT, HTML e DOCX) |
| Alertas antes de exportar | Conferência com links para a página e aceite obrigatório [UI] | Avisos no índice ("Conferência"); a capa bloqueia o botão quando há pendência [CÓDIGO] |
| Fontes ausentes | Não verificado | Aviso: "A fonte X não está instalada; usando Y" [CÓDIGO] |
| Imagens de baixa resolução | Módulo `image-preflight` [inventário] | Aviso abaixo de 200 ppi no miolo e de 300 ppi na capa, com o tamanho em pixels necessário [CÓDIGO][TESTE] |
| Prévia × PDF | Não verificado (PDF não exportado) | Mesmas fontes e posições; diferença pequena nas imagens da prévia, que são desenhadas em coordenadas inteiras de ponto (`LayoutRendering.kt:230`) [CÓDIGO] |

---

## 8. Inventário da implementação atual do Escriba

| Responsabilidade | Onde | Situação |
|---|---|---|
| Modelo de configurações | `shared/src/commonMain/kotlin/com/cuboide/escriba/core/layout/LayoutSettings.kt`: `TrimSize` (9 formatos), `PaperStock` (6 papéis, mm/pág **fixo**), `BleedPreset` (0 / 3,175 / 5 mm), `PageSetup` (margens), `BodyTypography`, `ChapterOpening`, `RunningHeads`, `FrontMatter`, `LayoutTheme` (5 temas) | Só global; nada por página ou capítulo |
| Persistência | `core/layout/LayoutStore.kt`: `layout.json` dentro do livro, lido e gravado inteiro | Uma falha de leitura cai nos padrões (P0) |
| Paginação | `core/layout/Typesetter.kt` (781 linhas): `Typesetter`, duas passagens (`Pass`), aberturas, pré-textuais, sumário, notas, viúvas e órfãs, capitular, imagens | Funciona [TESTE: `LayoutTest`, 12 testes] |
| Quebra de linha e hifenização | `core/layout/LineBreaker.kt`, `core/layout/Hyphenation.kt` | Funciona [TESTE] |
| Saída da paginação | `LaidOutBook` → `LaidPage(items: TextItem, RuleItem, DiamondItem, ImageItem)` em pontos | Sem identidade de parágrafo ou linha |
| Capa | `core/layout/Cover.kt`: `CoverSettings`, `CoverGeometry` (zonas, dobras), `layoutCover` (arte em "cover" centralizada, textos em posições fixas, lombada vertical, sinopse, código de barras, conferência), `coverMarks` | Funciona, não commitado [TESTE: `CoverTest`, 11; `CoverPdfTest`, 1; `CoverTabTest`, 1] |
| ISBN e EAN-13 | `core/layout/Isbn.kt`: validação de ISBN-10 e 13, conversão 10 → 13, módulos EAN-13 | Funciona [TESTE] |
| Fontes | `platform/LayoutFonts.kt`: catálogo das fontes do sistema, medição AWT, fallback por cobertura de glifos | Só fontes do sistema |
| Prévia e PDF do miolo | `platform/LayoutRendering.kt`: `LayoutPdfWriter` (PDFBox) e `LayoutPainter` (Java2D → bitmap) | Funciona [TESTE: `LayoutPdfTest`] |
| Prévia e PDF da capa | `platform/CoverRendering.kt` | Funciona [TESTE] |
| Ponte com a plataforma | `platform/Platform.kt` (expect) e `platform/Layout.jvm.kt` (actual) | — |
| Tela | `ui/LayoutScreen.kt` (726 linhas): abas Miolo e Capa, prévia em spreads numa `LazyColumn`, zoom ±/ajustar, guias, índice da estrutura com saltos, painel de ajustes em grupos recolhíveis, painel da capa | **Sem interação na página**; sem desfazer |
| Controlador | `app/EscribaController.kt:942-1041`: `updateLayout`, `applyLayoutTheme`, `requestLayout` (atraso de 350 ms, cancelável), `exportLayoutPdf`, `updateCover`, `importCoverImage`, `coverFor`, `exportCoverPdf` | Ver P0-3 |
| Visualização 3D | — | Ausente |

---

## 9. Matriz comparativa

Situações: **Impl.** implementada · **Parcial** parcialmente implementada · **Limit.** implementada com limitações ·
**Ausente** · **N/V** não verificada · **N/A** não aplicável. Complexidade: **B** baixa · **M** média · **A** alta.

### 9.1 Miolo

| Categoria | Funcionalidade do Autor | Comportamento observado | Situação | Implementação existente | Lacuna | Melhoria necessária | Evidência | Prior. | Compl. |
|---|---|---|---|---|---|---|---|---|---|
| Entrada de dados | Campos numéricos (M12/M14) | Digitação livre; valida ao confirmar | **Limit.** (defeito) | `LayoutScreen.kt:664` `NumberField` | O campo limita e reformata a cada tecla: "12" numa margem vira 52, "10" no corpo vira 18 | Guardar o texto digitado sem limitar; aplicar e limitar no Enter, ao perder o foco ou depois de uma pausa; mostrar erro quando estiver fora do intervalo | CÓDIGO | **P0** | B: mudança local no componente |
| Persistência | Salvar com `.bak` (M03) | Salva sozinho e mantém `.bak` | **Limit.** | `LayoutStore.kt` | Uma leitura que falha cai nos padrões e a próxima gravação apaga o arquivo do usuário | Ao falhar: renomear para `layout.json.corrompido-<data>`, avisar e não sobrescrever; manter `.bak`; ler cada campo com tolerância | CÓDIGO | **P0** | B |
| Exportação | Gerar PDF do estado atual | — | **Limit.** (defeito) | `EscribaController.kt:983` | O botão fica ativo nos 350 ms antes do recálculo e exporta a diagramação anterior | Exportar depois de esperar a diagramação pendente (`layoutJob.join()` ou recalcular na hora); desativar o botão enquanto houver recálculo pendente | CÓDIGO | **P0** | B |
| Histórico | Desfazer/refazer nomeado (M04) | ↶ ↷ e aviso com o nome da ação | **Ausente** | O desfazer existe só no editor de texto | Aplicar um tema troca todas as configurações sem volta | Pilha de `LayoutSettings` (e depois dos ajustes ancorados) com rótulo; Ctrl+Z/Ctrl+Y na Diagramação; aviso "Desfeito: …" | CÓDIGO | **P1** | B–M: o estado é imutável (`data class`), o que facilita |
| Página | Tamanho, papel, sangria (M06) | 13 formatos, 8 papéis, sangria personalizada | **Limit.** | `TrimSize` (9), `PaperStock` (6), `BleedPreset` (3) | Faltam 21 × 28, 7 × 10, 8,5 × 11, 6,14 × 9,21, Couché 115 g, KDP Premium Color, sangria personalizada e formato personalizado | Ampliar os presets e permitir largura, altura e sangria personalizadas | UI, CÓDIGO | P2 | B |
| Página | Espessura por página editável (M07) | Campo mm/pág e "Usar o valor do papel" | **Ausente** | `PaperStock.mmPerPage` fixo | A lombada não pode seguir o valor da gráfica | `PageSetup.mmPerPageOverride: Float?`; usar em `LaidOutBook.spineMm`; diálogo com a conta e a capa aberta | UI, CÓDIGO | **P1** | B |
| Página | Ver uma a uma (M06) | Páginas abertas ou uma a uma | **Parcial** | Só spreads (`spreads()`) | Não há modo de página única | Alternar spread / página única na prévia | UI, CÓDIGO | P3 | B |
| Visualização | Mostrar (M08) | Guias, cabeçalho, capitular, grade de base, papel branco | **Parcial** | Botão de guias (margens tracejadas) | Faltam grade de base, ocultar cabeçalho e papel branco | Opções de visualização (não persistidas no livro) | UI, CÓDIGO | P3 | B (grade: M) |
| Visualização | Zoom (M29) | Controle deslizante e Ajustar | **Impl.** | ± e "Ajustar à largura" | — | — | CÓDIGO | — | — |
| Visualização | Réguas em mm (M26) | Réguas nos dois eixos | **Ausente** | — | Não há referência métrica na prévia | Réguas sobre a área da prévia usando a escala pt → dp | UI | P2 | M |
| Manipulação direta | Caixa da escrita com alças (M15) | Arrastar as bordas muda as margens; medidas ao vivo; reflui | **Ausente** | A prévia é um bitmap (`PageSlot`) sem tratamento de ponteiro | Margem só por campo numérico | Camada sobre a página com 8 alças, conversão dp↔mm, arrasto com encaixe (0,5 mm), rótulo L × A; no começo, só para margens **globais** | UI, CÓDIGO | **P1** | M: as posições já estão em pt, falta a camada de ponteiro e o desempenho no arrasto |
| Escopo | Margens só desta página (M14) | Global / Só desta página, "Definir como global" | **Ausente** | Só global | Não há exceção por página | Ajustes ancorados (seção 12, B1); pilha de margens por página no `Typesetter` | UI, DADOS, CÓDIGO | P2 | A: exige modelo de âncoras |
| Escopo | Tipografia só desta página (M13) | O mesmo conjunto por página, com ↺ | **Ausente** | — | — | Igual ao anterior | UI, DADOS | P2 | A |
| Escopo | Abertura por capítulo (M18) | Padrão do livro mais exceção por capítulo | **Parcial** | `ChapterOpening.side` global | Não há exceção por capítulo | `chapterOverrides[chapterId]` (lado, ocultar título, preset) | UI, CÓDIGO | P2 | M |
| Capítulos | Lista com início, Soltar, ocultar título, presets (M19) | — | **Parcial** | `BookIndex` com a página de cada capítulo e salto | Faltam ocultar o título, presets por capítulo e soltar o título | Ações por capítulo no índice | UI, CÓDIGO | P2 | M |
| Parágrafo | Barra flutuante (M20) | Quebrar aqui, página em branco antes, soltar, quebra de cena, ornamento, centralizar | **Parcial** | Quebra de cena e centralização vêm da marcação do manuscrito | Não dá para agir na página da diagramação | Ajustes ancorados por parágrafo (quebra forçada, página em branco antes, centralizar) sem mexer no manuscrito | UI, CÓDIGO | P2 | A: depende de B1 e B2 |
| Parágrafo | Menu de contexto (M21) | Alinhamento, notas, capitular aqui, estilos | **Ausente** | — | — | Menu de contexto sobre a página, ligado ao parágrafo sob o ponteiro | UI | P2 | M |
| Parágrafo | Estilos editáveis (M22) | 10+ estilos; editar um muda todos; detectar no texto | **Parcial** | `BlockKind` fixos (H1–H3, citação, lista, centralizado) com tipografia fixa | Não há estilos nomeados nem editáveis (verso, epígrafe, mensagem…) | `paragraphStyles` em `LayoutSettings` e atribuição por marcação ou âncora | UI, CÓDIGO | P2 | M–A |
| Objetos | Objeto de texto, imagem, enfeite (M23–M25) | X/Y/L/A em mm, camadas, contorno, moldura, K% | **Ausente** | O `ImageItem` vem só do manuscrito | Não há objetos livres | Camada `freeObjects` por página (ancorada); itens extras no `LaidPage`; contorno simples (pela caixa) primeiro | UI, DADOS | P2 | A |
| Guias | Guias manuais e inteligentes (M26) | Inserir guia vertical/horizontal, limpar; `smart-guides` | **Ausente** | — | — | Guias em mm por livro; encaixe dos objetos | UI, REA | P3 | M |
| Seções | Nova seção / Montar pelo guia (M11) | ~19 tipos pré e pós-textuais | **Parcial** | Falsa folha, rosto, créditos, sumário (`FrontMatter`); dedicatória, epígrafe, prólogo… pelo tipo do capítulo | Faltam ficha catalográfica, aviso ao leitor, elogios, frontispício, personagens, agradecimentos, sobre o autor, colofão… | Mais `ChapterKind` e modelos pós-textuais; texto inicial com título, autor e ano | UI, CÓDIGO | P2 | M |
| Numeração | Fólio (M27) | Começa em N; romanos nos pré-textuais; número na abertura; cabeçalho na abertura | **Parcial** | `FolioPosition` (4); regra fixa "sem fólio na abertura e em branco" | Faltam início, romanos e opção na abertura | Ampliar `RunningHeads` | UI, CÓDIGO | P2 | B–M |
| Numeração | Fonte e arte do número (M28) | Fonte, negrito, itálico, opacidade, arte por lado | **Parcial** | Só o tamanho e a caixa alta do cabeçalho | — | Fonte, estilo e cor do fólio; a arte do fólio fica para P3 | UI | P3 | M |
| Cabeçalho | Por capítulo e ocultar na página (M16, M17) | — | **Parcial** | `RunningHeads` global | — | Exceções por capítulo e por página (B1) | UI | P2 | M |
| Capitular | Arte e fonte da capitular (M09) | — | **Parcial** | `dropCapLines` (0, 2, 3, 4) | Sem fonte própria nem arte | Fonte da capitular; a arte fica para P3 | UI, CÓDIGO | P3 | B |
| Temas | Exportar e importar tema, minhas fontes (M05) | — | **Parcial** | 5 temas fixos; fontes do sistema | Não dá para salvar e reaproveitar temas nem usar fontes fora do sistema | Exportar e importar `LayoutSettings` sem os dados do livro; pasta de fontes do usuário (`.ttf/.otf`) | UI, CÓDIGO | P3 | B / M |
| Conferência | Medianiz mínima pelo nº de páginas (M30) | Erro com link para a página | **Ausente** | Avisos de fonte, imagem e páginas ímpares | Não valida a margem interna nem a soma das margens | Regra própria de medianiz mínima por faixa de páginas (definida com gráficas e documentada), mais "área de texto ≥ X mm"; lista com link | UI, CÓDIGO | **P1** | B |
| Conferência | Créditos ausentes, aspas retas (M30) | Dicas | **Ausente** | — | — | Conferência de acabamento: aspas retas, "--", espaços duplos (o editor já corrige ao digitar), créditos desligados ou sem ISBN | UI | P2 | B |
| Conferência | Ficha técnica e passo a passo (M31, M32) | Texto copiável | **Ausente** | O resumo mostra formato, páginas e lombada | — | Botão "Copiar ficha técnica" com os dados do `LaidOutBook` e da capa | UI | P2 | B |
| Notas | Nota numerada ou com símbolo (M21) | — | **Parcial** | Notas numeradas vindas do manuscrito | Sem símbolo (*, †) | Estilo de chamada por livro | UI, CÓDIGO | P3 | B |

### 9.2 Capa

| Categoria | Funcionalidade do Autor | Comportamento observado | Situação | Implementação existente | Lacuna | Melhoria necessária | Evidência | Prior. | Compl. |
|---|---|---|---|---|---|---|---|---|---|
| Geometria | Capa aberta, lombada casada, orelhas, sangria (C02–C05) | — | **Impl.** | `CoverGeometry`, `coverGeometry()` | Unidade só em mm | Unidade in (P3) | UI, CÓDIGO, TESTE | — | — |
| Guias | Corte, dobra, sangria, ISBN (C05) | Legenda | **Impl.** | `CoverPainter.guides`; área segura de 10 mm | Não há legenda na tela | Legenda das linhas | UI, CÓDIGO | P3 | B |
| Arte | Arte com arrasto, Ctrl+roda, Enquadrar, escala (C06, C07) | Enquadramento livre e salvo | **Limit.** | `layoutCover`: "cover" centralizado, sem ajuste | O usuário não escolhe o recorte da arte | `CoverSettings.artFit {scale, offsetX, offsetY}`; arrastar e rodar sobre a prévia; botão recentralizar; o mínimo de ppi recalculado pela escala | UI, DADOS, CÓDIGO | **P1** | M |
| Arte | Arte em PDF (C06) | PDF recomendado | **Ausente** | Só PNG e JPG | Quem tem a capa em PDF precisa rasterizar | Aceitar PDF de 1 página: incorporar como Form XObject no PDF (PDFBox `LayerUtility`) e rasterizar para a prévia | UI, CÓDIGO | P2 | M |
| Elementos | + Imagem (logo, selo) com posição (C08) | — | **Ausente** | — | — | `coverImages[]` com posição e tamanho em mm e encaixe na zona do ISBN | UI | P2 | M |
| ISBN | Código de barras | Imagem importada pelo usuário | **Impl. (superior)** | `Isbn.kt` gera e valida o EAN-13 | — | Manter; opcionalmente permitir importar a imagem | UI, CÓDIGO, TESTE | — | — |
| Textos | Montar capa: 5 estilos, peças arrastáveis (C09) | Título, autor, sinopse e ISBN preenchidos | **Parcial** | Textos automáticos em posições fixas; fonte, tamanho e caixa alta do título; 6 cores de fundo e 3 de texto | Não se move nada; não há estilos prontos; sem fonte e cor por peça | `coverPieces` com posição em mm e estilo; 3 a 5 estilos próprios; arrasto na prévia | UI, CÓDIGO | P2 | M–A |
| Saída | CMYK ou RGB (C10) | — | **Ausente** (só RGB) | `CoverPdfWriter` | A gráfica nacional pede CMYK | Ver o item CMYK/PDF-X (E) | UI, CÓDIGO | P2 | A |
| Conferência | Bloqueio por arte ausente ou páginas ímpares | — | **Impl. (superior)** | `CoverIssue`: arte, ISBN, ppi, lombada estreita, sinopse que não cabe | — | Acrescentar "texto fora da área segura" quando houver peças móveis | CÓDIGO, TESTE | — | — |
| Formatos | Capa dura (C13) | Papelão, virada, praça, lombada do case | **Ausente** | — | — | `CoverGeometry` com espessura de papelão, virada e articulação; perfis KDP, Ingram e nacional (valores próprios, confirmados com as gráficas) | UI | P3 | M |
| Formatos | Capa de ebook (C14) | JPEG 1600 × 2560 ou do livro | **Ausente** | — | — | Recortar a zona "Capa" do `CoverLayout` e exportar JPEG | UI | P2 | B |
| Formatos | Marcadores (C15) | Frente e verso, furo, vários, PNG ou PDF | **Ausente** | — | — | Nova aba; pode reaproveitar a geometria e o desenho da capa | UI | P3 | M |
| Exportação | Aceite antes da gráfica (C11) | Modal de 2 s | **N/A** | — | Escolha de produto, não de técnica | Opcional: lembrete com a lista de verificação | UI | P3 | B |

### 9.3 Visualização 3D

| Categoria | Funcionalidade do Autor | Comportamento observado | Situação | Implementação existente | Lacuna | Melhoria necessária | Evidência | Prior. | Compl. |
|---|---|---|---|---|---|---|---|---|---|
| 3D | Livro 3D (3D1–3D4) | Sobreposição; girar, mover, centralizar; brochura e capa dura | **Ausente** | — | — | Cena 3D com 6 faces (capa, quarta, lombada, 3 faces de páginas) gerada do `CoverLayout` atual; girar arrastando, roda para zoom, duplo clique para centralizar; exportar PNG | UI, DADOS, REA | P2 | M: uma caixa com projeção em perspectiva cabe em Compose/Skia; iluminação realista seria A |
| 3D | Texturas sempre atuais | O Autor mostrou texturas velhas | — | — | — | Gerar as texturas na hora a partir do `CoverLayout` (sem cache em disco) | UI, DADOS | P2 | B |

### 9.4 Exportação

| Categoria | Funcionalidade do Autor | Comportamento observado | Situação | Implementação existente | Lacuna | Melhoria necessária | Evidência | Prior. | Compl. |
|---|---|---|---|---|---|---|---|---|---|
| PDF | Perfis Sob demanda, CMYK e colorida (E01) | — | **Parcial** | PDF RGB com fontes embutidas, TrimBox, BleedBox, marcas e marcadores | Sem CMYK, sem OutputIntent, sem PDF/X | Perfil "Gráfica": texto em K puro (0/0/0/100), imagens convertidas com um perfil ICC de licença livre, OutputIntent, PDF/X-1a ou X-4, conferido com o preflight do Acrobat ou com veraPDF | UI, CÓDIGO | P2 | A |
| PDF | Conferência do arquivo gerado (E03) | Relê o PDF com `pdf-lib` | **Ausente** | O `LayoutPdfTest` confere no teste | — | Depois de exportar, reabrir o PDF e conferir páginas, caixas e fontes embutidas | REA | P3 | B |
| Ebook | EPUB, Kindle, cópia para leitor (E01) | — | **Ausente** | O Exportar geral tem DOCX e HTML | — | EPUB 3 (já previsto em `docs/comparacao-escriba-autor.md` #46) | UI | P2 | M |

---

## 10. Usabilidade e experiência de edição

### 10.1 Fluxos equivalentes passo a passo

| Tarefa | Autor | Escriba |
|---|---|---|
| Mudar a margem inferior do livro inteiro | 2 ações: clicar no texto e arrastar a borda (medida ao vivo). Ou 3: clicar, escolher "Global" e digitar | 3 ações: o grupo "Página" já vem aberto; clicar no campo "Inferior", selecionar e digitar. **Mas por causa do defeito P0-1**, digitar "12" resulta em 52; é preciso editar dígito por dígito |
| Mudar a margem só da página 37 | 3 ações: navegar, clicar no texto, arrastar | **Impossível** |
| Desfazer uma mudança errada | 1 ação: ↶, com aviso do que foi desfeito | **Impossível**: refazer à mão. Aplicar um tema troca tudo sem volta |
| Ver onde está o problema da conferência | 1 clique: link "pág. 1" | Ler o aviso e procurar a página no índice |
| Ajustar o recorte da arte da capa | Arrastar e Ctrl+roda | **Impossível**: refazer a imagem fora do app |
| Pôr o título da capa noutra posição | Montar capa e arrastar a peça | **Impossível**: posição fixa (terço superior) |
| Ver o livro em 3D | 1 clique | **Inexistente** |
| Mudar o corpo do texto para 10 pt | Digitar "10" | Digitar "10" dá **18** (P0-1) |

### 10.2 Observações

- **Descoberta.** No Escriba tudo fica num único painel com grupos recolhíveis, o que facilita achar as coisas. O
  Autor espalha em 7 menus, um painel e menus de contexto, mas oferece a ação **onde o usuário está olhando**: na
  página, no parágrafo, no capítulo.
- **Feedback.** O Autor mostra medidas em mm sobre a página e avisos com o nome de cada ação. O Escriba mostra uma
  barra de progresso e o resumo "formato · páginas · lombada · fonte", que já ajuda a entender o efeito de cada
  ajuste.
- **Prevenção de erro.** O Escriba limita os valores, mas os limites estragam a digitação (P0-1). O Autor permitiu
  3,8 mm de medianiz, mas acusa o erro na conferência. No Escriba, um tema aplicado por engano apaga a configuração
  sem chance de desfazer.
- **Proteção contra alteração acidental.** Um achado sobre o próprio Autor: a roda do mouse sobre um campo numérico
  altera e salva (vivido nesta análise). O Escriba usa `OutlinedTextField` comum, sem esse risco.
- **Teclado.** O Escriba tem atalhos globais, mas nenhum na Diagramação (sem Ctrl+Z, sem setas para mover).
- **Desempenho.** O Autor refluiu 12 páginas sem atraso visível. Em livros grandes, nenhum dos dois foi medido. No
  Escriba, cada ajuste refaz as duas passagens do livro inteiro, com 350 ms de atraso [CÓDIGO]. Para arrastar
  margens ao vivo, é preciso medir antes (seção 12, B5).
- **Consistência entre capa, miolo e 3D.** No Escriba, o papel aparece nas duas abas e a lombada vem do miolo, sem
  passo de "casar" [CÓDIGO]. No Autor, o 3D mostrou lombada e páginas desatualizadas [UI][DADOS].

---

## 11. Engenharia reversa com o REA

### 11.1 Procedimento

- **Ferramenta.** `rea-agents` 6.5.0, no cache do npx (`%LOCALAPPDATA%\npm-cache\_npx\af752b90495482d7`), rodando
  com o Node portátil 24.22.0 da sessão anterior. Nada foi baixado.
- **Comando:**
  ```
  rea analyze-javascript-application <app.asar> --source-path renderer/diagramacao/{cover-geometry,body-frame,book3d-core,book3d-realistic,cover-preflight,smart-guides,spread-geometry,case-binding}.js --source-path services/diagramacao/conferencia-impressao.js --format json --full-output
  ```
- **Por que esses módulos.** São os responsáveis pelas regras que a interface não explica: o mínimo da medianiz, a
  geometria da capa e da capa dura, o 3D e a conferência do arquivo.
- **Evidence:** `ev_02ba2f81c22cc47e70a5c4ac1b623c30e5b26f2d4c14c54fe962e1a42e8f52c6`; subject `app.asar` sha256
  `67870045…d1341`.
- **Reaproveitado da análise anterior:** o fluxo de extração do ASAR e a cópia extraída da 1.1.26, de mesma versão
  que a instalada (registrados em `docs/engenharia-reversa-autor.md` e `docs/diagramacao-autor.md`).

### 11.2 Resultados

| Pergunta | Resultado | Conclusão sustentada |
|---|---|---|
| Os módulos de capa, 3D e caixa da escrita existem e que tamanho têm? | Funções e chamadas por módulo: `body-frame` 31/437, `smart-guides` 25/369, `spread-geometry` 21/221, `case-binding` 22/169, `cover-preflight` 16/197, `cover-geometry` 10/130, `book3d-core` 14/83, `book3d-realistic` 14/395, `conferencia-impressao` 57/279 | A caixa da escrita e as guias inteligentes são os módulos mais densos da interação. O 3D "realista" concentra o desenho (395 chamadas) |
| Do que dependem? | `conferencia-impressao` importa `pdf-lib`; os outros `require` são expressões ofuscadas (`require(W(0x1da))`) sem resolução estática | A conferência do arquivo provavelmente relê o PDF gerado (inferência) |
| Dá para extrair a regra da medianiz mínima ou da lombada? | Não. Literais e nomes ofuscados; o REA recuperou só 11 literais sem valor de negócio | **Regras e constantes do Autor não são recuperáveis** sem executar o código, o que também não seria apropriado pela licença |
| Superfície IPC desses módulos | 0 operações IPC (são módulos do renderer) | Ajustes de página e capa são calculados no renderer; a persistência passa pela camada de serviço (análise anterior: 17 canais `diagramacao:*`) |

### 11.3 Limitações e impacto

- **Cobertura parcial.** Os outros 3.273 arquivos ficaram fora da seleção, e o REA não executa código. Um
  relacionamento ausente não prova que ele não exista.
- **Impacto no plano.** O REA confirmou *onde* o Autor investe (caixa da escrita, guias e 3D), mas não *quanto*
  valem as regras. As regras do Escriba (medianiz mínima por número de páginas, espessuras, capa dura) precisam vir
  de **fontes próprias**: tabelas das gráficas, a especificação pública do KDP e testes de impressão. O plano trata
  isso como pré-requisito.

---

## 12. Lacunas técnicas, riscos e problemas arquiteturais

### 12.1 Problemas confirmados

| # | Problema | Onde | Efeito |
|---|---|---|---|
| P0-1 | O `NumberField` usa `remember(value)` e chama `onValue` a cada tecla com limites | `ui/LayoutScreen.kt:664-678`, campos com `coerceIn` em `:525-566` e `:469` | Valores com o 1º dígito abaixo do mínimo ficam impossíveis de digitar após "selecionar tudo": margens (5–60), corpo (7–18), título do capítulo (10–48), tamanho do cabeçalho (6–14), título da capa (10–120). Cada tecla também grava o `layout.json` |
| P0-2 | `loadLayoutSettings` faz `runCatching{…}.getOrNull() ?: LayoutSettings()` | `core/layout/LayoutStore.kt:10-13` | Um arquivo com valor de enum desconhecido (vindo de versão nova ou antiga do app) ou JSON inválido volta ao padrão, e a próxima mudança **sobrescreve** o arquivo do usuário |
| P0-3 | A exportação usa o `layout` em memória; `layoutBusy` só liga depois do atraso | `app/EscribaController.kt:957-994`, `1026-1041` | O PDF do miolo e a lombada da capa podem sair da diagramação anterior se o usuário exportar logo depois de ajustar |
| L-1 | Imagens da prévia desenhadas com coordenadas inteiras de ponto | `platform/LayoutRendering.kt:230` | Diferença de até 1 pt (0,35 mm) entre a prévia e o PDF nas imagens |
| L-2 | `TextLine` usa `remember { mutableStateOf(value) }` sem chave | `ui/LayoutScreen.kt:681` | Se o valor mudar por fora (um futuro desfazer, por exemplo), o campo continua mostrando o texto antigo. Vira problema assim que existir desfazer (P1) |
| L-3 | Nenhuma validação da área de texto | `Typesetter` (`textW = width - inner - outer`) | Margens que somam a largura do formato (ex.: 12 × 18 com 60 + 60 mm) deixam a área de texto com largura ≤ 0. Ver H-2 |

### 12.2 Gargalos estruturais

| ID | Gargalo | Situação atual | O que bloqueia | Recomendação |
|---|---|---|---|---|
| **B1** | Modelo de ajustes ancorados | `LayoutSettings` só global; o manuscrito é lido ao vivo (bom para manter tudo em dia) e os parágrafos não têm identidade estável | Margens e tipografia por página, quebras por parágrafo, objetos livres, cabeçalho por página, exceções por capítulo | Introduzir `LayoutOverrides` em `layout.json`, ancorado em `chapterId` + índice do parágrafo + um **hash curto do texto**, para reencontrar o parágrafo quando o texto muda. Quando a âncora se perde, mostrar o ajuste como "órfão" na conferência e não descartá-lo em silêncio. As exceções por capítulo precisam só de `chapterId`, que já é estável, e podem vir antes |
| **B2** | Camada de coordenadas e interação | Prévia rasterizada por página; o `LaidPage` não diz qual parágrafo gerou cada linha | Manipulação direta, menu de contexto, barra por parágrafo, objetos | Acrescentar `sourceRef` (capítulo e parágrafo) ao `TextItem` e um `PageOverlay` Composable com escala pt→dp e testes de clique. As posições já estão em pt, o que favorece (confirmado) |
| **B3** | Histórico da diagramação | Não existe; `updateLayout` grava direto | Desfazer, arrastar com segurança, temas reversíveis | `LayoutHistory` (pilha de `LayoutSettings` e dos ajustes) no controlador, com agrupamento de arrastos |
| **B4** | Cor | Tudo em RGB (PDFBox, `argbColor`) | CMYK da capa e do miolo, PDF/X | Uma camada de cor no escritor de PDF (DeviceCMYK para textos e fundos; imagens convertidas por ICC; OutputIntent). Conferir a licença do perfil escolhido |
| **B5** | Desempenho da paginação | Livro inteiro, 2 passagens, a cada mudança (350 ms de atraso) | Arrastar margens com refluxo ao vivo | **Medir** primeiro (H-1). Se for lento: no arrasto, refazer só o capítulo atual e mostrar o resultado provisório; refazer o livro inteiro ao soltar |
| **B6** | Robustez da persistência | Leitura tudo-ou-nada | Evolução do esquema (ajustes, objetos) | Campo `schemaVersion` e migrações; leitura tolerante por campo; backup antes de migrar |

### 12.3 Hipóteses a validar

- **H-1.** Quanto tempo leva a paginação de um livro de 300 a 400 páginas? Medir `layoutBook` com um manuscrito
  sintético. Isso decide como fazer o arrasto ao vivo (B5).
- **H-2.** Com a área de texto ≤ 0, o `LineBreaker` termina (uma palavra por linha, com o texto vazando) ou entra em
  laço? O teste `greedyBreaksOverflowingWordOnItsOwnLine` sugere que termina, mas a largura zero não foi testada.
- **H-3.** Âncora por índice com hash resiste às edições comuns (inserir ou apagar parágrafos)? Validar com um teste
  de propriedade que edite o manuscrito aleatoriamente.
- **H-4.** O PDFBox incorpora PDF de capa (Form XObject) mantendo fontes e vetores? Fazer uma prova de conceito com
  um PDF do Illustrator e outro do Affinity.

---

## 13. Plano de evolução priorizado

As etapas respeitam as dependências: primeiro os defeitos, depois a infraestrutura (histórico, coordenadas, âncoras)
e só então os recursos que dependem dela.

### Etapa 0: P0 (integridade e precisão)

#### P0-1. Campos numéricos que aceitam digitação
1. **Problema:** o campo limita e reformata a cada tecla, e valores comuns ficam impossíveis de digitar (seção 12, P0-1).
2. **Funcionalidade:** um campo numérico com texto local, que só confirma o valor no Enter, ao perder o foco ou
   depois de 600 ms sem digitar.
3. **Comportamento esperado:** o usuário seleciona tudo e digita "12" ou "10,5"; o valor aplicado é 12 ou 10,5. Se
   ficar fora do intervalo, a borda fica vermelha e aparece "entre 5 e 60 mm", sem substituir o que foi digitado.
4. **Componentes:** `ui/LayoutScreen.kt` (`NumberField`, todos os usos), com chance de reaproveitar em outras telas.
5. **Dependências:** nenhuma.
6. **Complexidade:** baixa (um componente).
7. **Critérios de aceitação:** "12" numa margem, "10" no corpo e "34" no título da capa resultam exatamente nesses
   valores; "3" numa margem mostra o erro e não muda o livro; digitar não grava o `layout.json` a cada tecla.
8. **Riscos:** perder um valor se o usuário fechar a tela antes de confirmar. Mitigação: confirmar ao sair da tela
   ou da composição.
9. **Pré-requisitos:** nenhum.
10. **Validação:** teste de UI em `desktopApp/src/test` (modelo `CoverTabTest`) que digita sequências e confere
    `layoutSettings`.

#### P0-2. Leitura segura do `layout.json`
1. **Problema:** uma falha de leitura cai no padrão e sobrescreve o arquivo depois.
2. **Funcionalidade:** leitura tolerante, com preservação do arquivo original.
3. **Comportamento esperado:** se a leitura falhar, o Escriba renomeia o arquivo para
   `layout.json.ilegivel-<data>`, avisa "As configurações de diagramação não puderam ser lidas; uma cópia foi
   guardada" e começa com o padrão.
4. **Componentes:** `core/layout/LayoutStore.kt`, `EscribaJson` (avaliar `coerceInputValues = true` só para a
   diagramação), `EscribaController.openBook`.
5. **Dependências:** nenhuma.
6. **Complexidade:** baixa.
7. **Critérios de aceitação:** um `layout.json` com `"trim": "Inexistente"` continua no disco (renomeado) depois de
   abrir e mudar uma configuração; campos desconhecidos são ignorados sem perder os conhecidos.
8. **Riscos:** com `coerceInputValues`, um valor inválido vira o padrão sem aviso. Mitigação: comparar antes e
   depois e avisar.
9. **Pré-requisitos:** nenhum.
10. **Validação:** testes em `StorageTest` com o `InMemoryFileStore`.

#### P0-3. Exportar sempre o estado atual
1. **Problema:** exportar durante o atraso de 350 ms usa a diagramação anterior.
2. **Funcionalidade:** a exportação espera o recálculo pendente ou recalcula na hora.
3. **Comportamento esperado:** mudar a margem e clicar em "Gerar PDF" logo em seguida gera o PDF com a margem nova;
   o botão mostra "Atualizando…" enquanto isso.
4. **Componentes:** `EscribaController.requestLayout`, `exportLayoutPdf`, `exportCoverPdf`; `LayoutScreen` (estado
   do botão).
5. **Dependências:** nenhuma.
6. **Complexidade:** baixa.
7. **Critérios de aceitação:** um teste que chama `updateLayout` e depois exporta, sem esperar, gera um PDF com o
   número de páginas da configuração nova; a capa usa a lombada do miolo novo.
8. **Riscos:** espera longa em livros grandes. Mitigação: barra de progresso.
9. **Pré-requisitos:** nenhum.
10. **Validação:** teste no `ControllerTest` com um medidor falso.

### Etapa 1: P1 (base competitiva, sem mudar o modelo)

#### P1-1. Desfazer e refazer na diagramação
1. **Problema:** não há como voltar atrás; um tema aplicado por engano apaga tudo.
2. **Funcionalidade:** histórico de `LayoutSettings` com rótulos.
3. **Comportamento esperado:** Ctrl+Z e Ctrl+Y (e botões ↶ ↷ na barra) desfazem e refazem; aparece "Desfeito: tema
   Clássico"; a digitação num campo conta como uma entrada só.
4. **Componentes:** `EscribaController` (nova `LayoutHistory`), `LayoutScreen` (barra e atalhos), `TextLine` (deve
   reagir a mudanças vindas de fora, L-2).
5. **Dependências:** P0-1, para não registrar uma entrada por tecla.
6. **Complexidade:** baixa a média (o estado já é imutável).
7. **Critérios de aceitação:** aplicar um tema e desfazer restaura o `LayoutSettings` anterior idêntico (`==`);
   50 passos de histórico; ao trocar de livro, o histórico zera.
8. **Riscos:** consumo de memória (pequeno; cada estado tem poucos KB).
9. **Pré-requisitos:** P0-1.
10. **Validação:** testes unitários do histórico e um teste de UI com Ctrl+Z.

#### P1-2. Espessura do papel editável e mais papéis e formatos
1. **Problema:** a lombada não pode seguir o valor informado pela gráfica; faltam papéis e formatos comuns.
2. **Funcionalidade:** espessura por página personalizada, mais papéis, formato e sangria personalizados.
3. **Comportamento esperado:** diálogo "Lombada" mostrando "N págs × E mm/pág = L mm", um campo para a espessura e
   "Usar o valor do papel"; a capa reflete na hora.
4. **Componentes:** `LayoutSettings.kt` (`PageSetup.mmPerPageOverride`, formato e sangria personalizados),
   `Typesetter.LaidOutBook.spineMm`, `LayoutScreen`.
5. **Dependências:** P0-1.
6. **Complexidade:** baixa.
7. **Critérios de aceitação:** com 0,07 mm/pág e 200 páginas, a lombada é 14 mm no miolo e na capa; um formato de
   150 × 220 mm gera um PDF com TrimBox de 425,2 × 623,6 pt.
8. **Riscos:** valores sem fonte confiável. Mitigação: documentar a origem de cada preset (ficha da gráfica ou
   KDP).
9. **Pré-requisitos:** levantar as espessuras com gráficas nacionais (fonte própria).
10. **Validação:** `CoverTest` e `LayoutTest` com a espessura personalizada.

#### P1-3. Conferência da área de texto e da medianiz
1. **Problema:** o Escriba não avisa sobre medianiz pequena para a espessura do livro, nem sobre margens impossíveis.
2. **Funcionalidade:** novas verificações na lista "Conferência", com link para a página.
3. **Comportamento esperado:** "Margem interna de 8 mm: para 320 páginas o mínimo recomendado é X mm" (erro);
   "Área de texto com menos de 60 mm de largura" (aviso); clicar leva à página.
4. **Componentes:** `Typesetter` (`passWarnings` vira um tipo estruturado com a página), `LayoutScreen.BookIndex`.
5. **Dependências:** nenhuma.
6. **Complexidade:** baixa.
7. **Critérios de aceitação:** as regras ficam em uma tabela própria, documentada; um teste para cada faixa; a
   largura ≤ 0 é bloqueada antes de paginar (resolve H-2).
8. **Riscos:** regra conservadora demais. Mitigação: aviso em vez de erro onde houver dúvida.
9. **Pré-requisitos:** tabela de medianiz definida com uma fonte própria (gráficas e guias públicos de produção
   editorial).
10. **Validação:** testes no `LayoutTest`.

#### P1-4. Enquadramento da arte da capa
1. **Problema:** a arte é sempre centralizada e preenche a capa; o usuário não escolhe o recorte.
2. **Funcionalidade:** escala e deslocamento salvos, com arrasto e roda sobre a prévia.
3. **Comportamento esperado:** arrastar move a arte; Ctrl+roda dá zoom em torno do ponteiro; "Recentralizar"
   volta ao padrão; o ppi exibido acompanha a escala; a arte nunca deixa sangria descoberta (encaixe automático, ou
   aviso bloqueante se ficar faixa sem arte).
4. **Componentes:** `Cover.kt` (`CoverSettings.artFit`, `layoutCover`), `LayoutScreen.CoverWorkspace` (ponteiro),
   `CoverRendering.kt`.
5. **Dependências:** P1-1, para desfazer o enquadramento.
6. **Complexidade:** média (gestos e conversão de coordenadas).
7. **Critérios de aceitação:** prévia e PDF com o mesmo recorte (comparação de pixels com tolerância); escala de
   150 % reduz o ppi a 2/3; sangria descoberta gera um item bloqueante.
8. **Riscos:** gestos conflitando com a rolagem. Mitigação: Ctrl obrigatório para o zoom, como no Autor.
9. **Pré-requisitos:** P1-1.
10. **Validação:** `CoverTest` (geometria) e `CoverPdfTest` (recorte no PDF).

#### P1-5. Manipulação direta das margens globais
1. **Problema:** ajustar margens só por campo, sem ver o efeito sobre a página.
2. **Funcionalidade:** sobreposição na página com a caixa de texto, alças e medidas, começando pelas margens
   **globais**.
3. **Comportamento esperado:** passar o mouse sobre a página mostra a caixa tracejada; arrastar uma borda mostra
   "Inf. 24 mm · A 171 mm"; ao soltar, o livro é refeito; o arrasto encaixa em 0,5 mm; Shift+arrasto aplica nas
   duas páginas espelhadas (interna e externa); Esc cancela.
4. **Componentes:** `LayoutScreen.PageSlot` (novo `PageOverlay`), conversão pt↔dp, `EscribaController.updateLayout`.
5. **Dependências:** P1-1 (desfazer) e a medição H-1.
6. **Complexidade:** média.
7. **Critérios de aceitação:** depois do arrasto, o valor no painel e no `layout.json` é o mesmo exibido no rótulo;
   o PDF tem a área de texto na nova posição (±0,1 mm); um arrasto vira uma entrada só no histórico.
8. **Riscos:** se cada movimento repaginar o livro, fica lento. Mitigação: durante o arrasto, mostrar só a caixa e
   repaginar ao soltar (B5).
9. **Pré-requisitos:** P0-1, P1-1, H-1.
10. **Validação:** teste de UI com arrasto sintético (`performMouseInput`) e conferência de `layoutSettings`.

### Etapa 2: P2 (modelo ancorado e recursos de editor)

Cada item tem os 10 campos de forma condensada.

| ID | Problema → funcionalidade | Comportamento esperado | Componentes | Dependências / pré-requisitos | Compl. | Critérios de aceitação | Riscos | Validação |
|---|---|---|---|---|---|---|---|---|
| P2-1 | Sem exceções por página → **modelo `LayoutOverrides` ancorado** (B1, B6) | Margens e tipografia "só desta página", com ↺ e "Definir como global"; ajustes órfãos aparecem na conferência | `LayoutSettings.kt` (novas classes), `LayoutStore` (`schemaVersion`, migração), `Typesetter` (margens por página) | P0-2, P1-1, H-3 | A: muda o núcleo da paginação | Um ajuste na página onde começa o parágrafo P continua com P depois de inserir 3 parágrafos antes; o padrão global continua idêntico ao atual sem ajustes | Âncoras quebradas por edição pesada | Teste de propriedade (H-3) e testes de regressão do `LayoutTest` |
| P2-2 | Sem ligação entre linha e parágrafo → **`sourceRef` em `TextItem`** e testes de clique (B2) | Clicar numa linha seleciona o parágrafo de origem | `Typesetter` (emissão), `LayoutScreen` | P1-5 | M | Clicar em qualquer linha devolve o capítulo e o parágrafo certos | Aumento de memória | Teste unitário de mapeamento |
| P2-3 | Ações só no manuscrito → **barra por parágrafo e menu de contexto** (quebrar a página aqui, página em branco antes, centralizar, estilo, capitular aqui) | Ícones ao lado do parágrafo sob o ponteiro e menu do botão direito; a ação vira um ajuste ancorado e não mexe no `.md` | `LayoutScreen`, `Typesetter` | P2-1, P2-2 | M | "Quebrar aqui" leva o parágrafo para o topo da página seguinte; desfazer volta | Divergência entre o manuscrito e o livro | `LayoutTest` |
| P2-4 | Estilos fixos → **estilos de parágrafo nomeados** (verso, epígrafe, mensagem…) | Editar um estilo muda todos os parágrafos que o usam | `LayoutSettings.paragraphStyles`, `Typesetter`, marcação opcional no manuscrito | P2-3 | M–A | Mudar o recuo de "Verso" altera só os parágrafos com esse estilo | Explosão de opções | `LayoutTest` |
| P2-5 | Sem exceção por capítulo → **ajustes por capítulo** (lado de abertura, ocultar título, preset, cabeçalho) | Ações na lista de capítulos | `LayoutSettings`, `Typesetter`, `BookIndex` | P1-1 (só precisa de `chapterId`) | M | Capítulo X abre à esquerda e os outros seguem o padrão | Baixo | `LayoutTest` |
| P2-6 | Fólio limitado → **numeração: início, romanos nos pré-textuais, número e cabeçalho na abertura, fonte e estilo do fólio** | Igual ao diálogo do Autor | `RunningHeads`, `Typesetter.furniture` | P0-1 | B–M | Pré-textuais i, ii…; o miolo começa em 1 (ou N) | Sumário com numeração errada | `LayoutTest` (sumário em duas passagens) |
| P2-7 | Poucos pré e pós-textuais → **modelos de seção** (ficha catalográfica, agradecimentos, sobre o autor, colofão…) | Nascem com título, autor e ano | `ChapterKind`, `Typesetter.frontMatter`, Manuscrito | — | M | Cada modelo gera a página com o tipo certo e a posição certa (antes ou depois) | Baixo | `LayoutTest` |
| P2-8 | Sem objetos livres → **objetos de texto, imagem e enfeite** com X/Y/L/A em mm, camadas e contorno pela caixa | Inserir, arrastar, redimensionar, painel em mm | `LayoutOverrides.objects`, `Typesetter` (exclusão de área), prévia e PDF | P2-1, P2-2 | A | O texto contorna a caixa; o PDF tem o objeto na posição exata (±0,1 mm) | Interação com notas e viúvas | `LayoutTest` e `LayoutPdfTest` |
| P2-9 | Textos da capa fixos → **peças da capa móveis e estilos de montagem** | Arrastar título, autor e sinopse; 3 a 5 estilos próprios | `Cover.kt` (`coverPieces`), `CoverWorkspace` | P1-4 | M–A | Peças fora da área segura geram aviso; o PDF bate com a prévia | Repetir o visual do Autor; usar estilos próprios | `CoverTest` |
| P2-10 | **Imagens extras na capa** (logo, selo) | "+ Imagem" com posição e tamanho em mm | `Cover.kt`, `CoverRendering.kt` | P2-9 | M | A imagem respeita a sangria e o ppi é conferido | Baixo | `CoverPdfTest` |
| P2-11 | **Arte de capa em PDF** | Importar PDF de 1 página | `CoverPdfWriter` (Form XObject), prévia rasterizada | H-4 | M | O PDF final mantém vetores e fontes da arte | PDFs com transparência | Teste com PDFs de exemplo |
| P2-12 | **Capa de ebook (JPEG)** | Exportar a zona "Capa" em 1600 × 2560 ou na proporção do livro | `CoverRendering` | P1-4 | B | JPEG com o tamanho certo e sem sangria | Baixo | Teste de dimensões |
| P2-13 | **Mockup 3D** | Botão "Livro 3D": girar arrastando, zoom pela roda, duplo clique centraliza, exportar PNG; faces geradas do `CoverLayout` atual | Nova `Book3DView` (Compose/Skia com projeção), `CoverRendering` (texturas) | P1-4 | M | A lombada do 3D é igual à da capa (sem cache velho); 60 fps ao girar num livro de 400 páginas | Desempenho do Skia em máquinas fracas | Teste de geometria das faces e teste visual manual |
| P2-14 | **Conferência de acabamento e ficha técnica** | Aspas retas, "--", créditos sem ISBN; "Copiar ficha técnica" | `Typesetter`/conferência, `LayoutScreen` | P1-3 | B | Cada regra com teste; a ficha bate com o `LaidOutBook` | Falsos positivos | `LayoutTest` |
| P2-15 | **PDF para gráfica: CMYK e PDF/X** (B4) | Perfil "Gráfica (CMYK)" no miolo e na capa | `LayoutPdfWriter`, `CoverPdfWriter`, nova camada de cor | Perfil ICC com licença livre | A | Passa no preflight PDF/X-1a (Acrobat ou veraPDF); textos em K 100 % | Cores das imagens mudam na conversão | Teste e validação externa |
| P2-16 | **EPUB 3** | Exportar EPUB a partir dos capítulos | `core/export` | — | M | Passa no EPUBCheck | Baixo | Teste e EPUBCheck |

### Etapa 3: P3 (refinamentos)

| ID | Melhoria | Componentes | Dependências | Compl. | Critério de aceitação | Risco | Validação |
|---|---|---|---|---|---|---|---|
| P3-1 | Ver página única; réguas em mm; grade de base; ocultar cabeçalho na prévia | `LayoutScreen` | P1-5 (escala) | B–M | As réguas batem com o PDF (±0,2 mm) | Baixo | Teste de UI |
| P3-2 | Guias manuais e encaixe | `LayoutOverrides.guides` | P2-8 | M | Objeto encaixa a menos de 1 mm da guia | Baixo | Teste unitário |
| P3-3 | Capa dura (papelão, virada, articulação) | `CoverGeometry` | P1-2 | M | Lombada do case = miolo + 2 × papelão (valores próprios por gráfica) | Valores errados | `CoverTest` |
| P3-4 | Marcadores de página | Nova aba | P2-10 | M | PDF frente e verso com sangria de 3 mm | Baixo | `CoverPdfTest` |
| P3-5 | Exportar e importar tema; pasta de fontes do usuário | `LayoutTheme`, `LayoutFonts` | — | B / M | Tema importado reproduz o miolo sem trazer os dados do livro | Licença das fontes | Teste |
| P3-6 | Arte do fólio e da capitular | `Typesetter`, renderização | P2-6 | M | Arte nas duas páginas, espelhada | Baixo | `LayoutPdfTest` |
| P3-7 | Reler o PDF exportado e conferir | `platform` | — | B | Avisa se faltar fonte embutida | Baixo | Teste |
| P3-8 | Imagens da prévia em coordenadas fracionárias (L-1) | `LayoutRendering.kt:230` | — | B | Diferença prévia × PDF abaixo de 0,1 pt | Nenhum | Comparação de imagem |

---

## 14. Recomendações finais

1. **Primeiro a Etapa 0** (P0-1 a P0-3). São três correções pequenas que afetam funções já entregues: um usuário
   hoje não consegue digitar o corpo 10, pode perder o `layout.json` e pode mandar à gráfica um PDF que não
   corresponde ao que vê.
2. **Depois desfazer (P1-1) e a espessura personalizada (P1-2).** São baratos e dão segurança e precisão; o
   desfazer é pré-requisito de toda edição direta.
3. **Conferência da medianiz (P1-3)** antes de qualquer edição visual: evita o erro mais caro (livro impresso com
   texto afundado na dobra), e o Autor mostra que é uma checagem de alto valor.
4. **Enquadramento da arte (P1-4) e margens por arrasto (P1-5)** são o salto de experiência mais visível e não
   exigem mudar o modelo de dados: usam só as configurações globais.
5. **Só então o modelo ancorado (P2-1, P2-2).** Ele destrava metade da Etapa 2. Antes, medir o desempenho (H-1) e
   provar a âncora (H-3), porque o Escriba lê o manuscrito ao vivo, diferente da cópia reimportável do Autor. Essa é
   uma vantagem de sincronização que não deve ser perdida.
6. **O 3D (P2-13) é barato e muito visível** depois do P1-4. Gerar as texturas do `CoverLayout` atual evita a
   inconsistência observada no Autor.
7. **Manter as vantagens do Escriba:** código de barras EAN-13 gerado e ISBN validado, conferência da capa mais
   detalhada (ppi necessário, lombada estreita, sinopse que não cabe) e manuscrito sempre sincronizado.

Uma reescrita **não** se justifica. O motor (`Typesetter`, `LineBreaker`, PDF e prévia em pt) é a base certa; as
lacunas são de camadas acima dele.

---

## 15. Apêndice de evidências

### 15.1 Caminhos de navegação no Autor

| Evidência | Caminho |
|---|---|
| Tela do miolo | Menu lateral → Diagramação → Diagramar Livro |
| Tema / Página / Mostrar / Inserir / Numeração / Exportar | Botões da barra superior do miolo |
| Lombada | Página → "Lombada…" |
| Fólio | Numeração → "Onde aparece · romano (i, ii)…" / "Fonte e tamanho do número…" / "Arte da página esquerda…" (abre o seletor de arquivos) |
| Conferência | Ícone de escudo, à esquerda de "Gerar PDF"; seções "Ficha técnica…" e "Passo a passo…" |
| Objeto de texto | Inserir → Texto (depois desfeito) |
| Caixa da escrita | Clicar no texto da página → alças; arrastar a alça inferior (depois desfeito) |
| Barra por parágrafo | Passar o mouse sobre os ícones à esquerda do parágrafo selecionado |
| Menu de contexto | Botão direito sobre o texto |
| Nova seção | Inserir → Nova seção → "Mais páginas do guia" |
| Estilos | Painel direito → "Estilos do parágrafo" |
| Arte da letra | Mostrar → "Arte da letra…" |
| Capa | Modos → Diagramar Capa → abas Impressa, Capa dura, Ebook, Marcadores; "Montar capa"; dicas de "Arquivo de capa…", "Enquadrar", "+ Imagem" |
| 3D | Diagramar Capa → "Livro 3D" → arrastar, duplo clique, "Ver livro em Capa dura" / "Ver livro em Brochura", ✕ |
| Limite de projetos | Biblioteca → "+ Novo projeto" → "Limite de projetos 1 de 1" |
| Aceite de exportação | Diagramar Capa → "Exportar capa (PDF)" → salvar → modal "Antes de mandar para a gráfica" (cancelado) |

As capturas de tela foram feitas durante a sessão e **não foram gravadas no repositório**, já que a única alteração
autorizada é este relatório.

### 15.2 Dados do Autor consultados (só leitura)

- `Documents\Projetos Autor\The Witcher - New Dawn.autor\diagramacao.json`, chaves de primeiro nível: `schemaVersion`
  (2), `source` {projectPath, snapshotAt}, `spec` {trimId, stockId, bleedId, standard, trimMm, bleedMm, margins,
  spineMm, flapMm, pageCountMeasured…}, `settings`, `stories[]` {chapterId, title, paragraphs[] {type, text,
  inlineMarks}}, `pages[]`, `guides[]`, `assets[]`, `objectStyles[]`, `pageControls[]`, `bodyFrames[]`
  {physicalPage, margins, anchor}, `bodyTypography[]` {physicalPage, typography, anchor}, `footnotes[]`, `cover`
  {flapMm, showGuides, artUrl, artSource, images[], artFit}, `notasDoManuscrito`.
- `cover-mockup\meta.json`: `{version 2, trimWMm 140, trimHMm 210, spineMm 1, pages 3, files {front, back, spine}}`.
- Backup e verificação: cópia integral em scratchpad antes da sessão; diff final sem diferenças.

### 15.3 Arquivos do Escriba examinados

- `shared/src/commonMain/kotlin/com/cuboide/escriba/core/layout/LayoutSettings.kt`
- `shared/src/commonMain/kotlin/com/cuboide/escriba/core/layout/LayoutStore.kt`
- `shared/src/commonMain/kotlin/com/cuboide/escriba/core/layout/Typesetter.kt`
- `shared/src/commonMain/kotlin/com/cuboide/escriba/core/layout/Cover.kt` (não commitado)
- `shared/src/commonMain/kotlin/com/cuboide/escriba/core/layout/Isbn.kt` (não commitado)
- `shared/src/commonMain/kotlin/com/cuboide/escriba/ui/LayoutScreen.kt`
- `shared/src/commonMain/kotlin/com/cuboide/escriba/app/EscribaController.kt` (linhas 942-1041, 1083-1091)
- `shared/src/commonMain/kotlin/com/cuboide/escriba/core/storage/FileStore.kt` (`EscribaJson`)
- `shared/src/jvmMain/kotlin/com/cuboide/escriba/platform/LayoutRendering.kt`
- `shared/src/jvmMain/kotlin/com/cuboide/escriba/platform/CoverRendering.kt` (não commitado)
- `shared/src/jvmMain/kotlin/com/cuboide/escriba/platform/Layout.jvm.kt`

### 15.4 Testes executados

`JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew :shared:jvmTest :desktopApp:test` deu
**BUILD SUCCESSFUL**:

| Suíte | Testes | Falhas |
|---|---|---|
| ControllerTest | 11 | 0 |
| CoverTest | 11 | 0 |
| CoverPdfTest | 1 | 0 |
| LayoutTest | 12 | 0 |
| LayoutPdfTest | 1 | 0 |
| DocxWriterTest, ExportTest, FormattingTest, MarkupTest, OutlineTest, PolishTest, StorageTest, TextToolsTest | 58 | 0 |
| CoverTabTest, EditorClickTest, LongChapterClickTest, ManuscriptLayoutTest, NarrowWindowTest (desktop) | 15 | 0 |
| **Total** | **109** | **0** |

Nenhum teste cobre os defeitos P0-1 a P0-3. Todos foram confirmados pela leitura do código, e os critérios de
aceitação da seção 13 já descrevem o teste que deve ser escrito para cada um.

### 15.5 REA

- Saída completa (88 MB) e resumo ficaram no scratchpad da sessão, fora do repositório.
- Evidence `ev_02ba2f81c22cc47e70a5c4ac1b623c30e5b26f2d4c14c54fe962e1a42e8f52c6`.
- Análises anteriores: `docs/engenharia-reversa-autor.md` (1.1.25, evidence `ev_43cfbf…`) e
  `docs/diagramacao-autor.md` (1.1.26, inventário de módulos).
