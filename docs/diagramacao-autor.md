# Diagramação: o que o Autor faz e o plano do Escriba

Levantamento feito em 26/10/2026 sobre o **Autor 1.1.26**, por duas fontes:

- **Navegação na tela** do app instalado, só para observar.
- **Inventário do `app.asar`** extraído: nomes de módulos e textos visíveis. O código do Autor agora vem ofuscado e
  proíbe reimplementação, por isso nada foi copiado. O Escriba implementa o mesmo **comportamento** com código,
  algoritmos e valores próprios.

> A análise completa, do miolo à capa dura, marcadores e livro 3D, está em
> [diagramacao-autor-analise-completa.md](diagramacao-autor-analise-completa.md).

## 1. O que o Autor oferece

### Entrada

A seção "Diagramação" abre com duas opções:

- **Diagramar Livro**: o miolo (capítulos, páginas, tipografia, notas) e a exportação de ebook.
- **Diagramar Capa**: capa aberta com contracapa, lombada e capa, na medida da gráfica, com sangria e marcas de corte.

### Miolo (tela observada)

| Área | O que tem |
|---|---|
| Barra | Trazer livro (reimporta o manuscrito) · Salvar · desfazer/refazer · **Tema** · **Página** · **Mostrar** · **Inserir** · **Numeração** · **Exportar** · zoom · Ajustar · conferência (escudo) · **Gerar PDF** · resumo `140 × 210 mm · lombada 0,2 mm (3 págs) · sangria 5 mm` |
| Tema (presets de miolo) | Clássico, Miolo V2, Romance Literário, Fantasia Épica, Contemporâneo, Elegante, Bolso Econômico; exportar/importar tema; minhas fontes; recomeçar montagem |
| Página | **Tamanho:** 14×21 (romance nacional), 16×23, 15,5×23, 12,5×18 (pocket), A5, 21×28, 6×9 in, 5×8, 5,5×8,5, 5,25×8, 6,14×9,21, 7×10, 8,5×11 (KDP US)<br>**Papel:** Pólen Soft 80 g, Pólen Bold 90 g, Offset 75/90 g, Couché 115 g, KDP White/Cream/Premium Color<br>**Sangria:** Nacional 5 mm, KDP 3,2 mm, sem, personalizada<br>**Ver:** páginas abertas (spread)<br>**Lombada…** |
| Mostrar | Guias, cabeçalho e número, letra grande (capitular), grade de base, arte da letra, papel branco |
| Inserir | Nova seção, sumário automático, texto, nota de rodapé, imagem, enfeite, camadas, imagem dupla, soltar título, ± página, guias, limpar guias |
| Numeração | Onde aparece (com romanos i, ii… nos pré-textuais), fonte e tamanho, arte das páginas esquerda/direita |
| Exportar | E-book, prévia Kindle, cópia para leitor, PDF "sob demanda" |
| Painel "Tipografia do corpo" | Fonte, corpo (pt), entrelinha, alinhamento, recuo da 1ª linha (em), espaço entre parágrafos e após o título (em linhas), 1º parágrafo sem recuo, hifenizar, composição fina (espaço parelho entre palavras) |
| Onde o capítulo abre | Direita · Esquerda · Próxima página (padrão do livro, com exceção por capítulo) |
| Lista de capítulos | Página inicial de cada um; editar, soltar título (vira objeto livre), estilos de abertura, travar |
| Objeto de texto | Fonte, corpo, entrelinha, alinhamento horizontal e vertical, espaço entre letras, tinta (K%), posição e tamanho em mm, centralizar na página |
| Visual | Páginas lado a lado com réguas em mm, margens e área de texto tracejadas, número de página |

### Capa

- **Abas:** Impressa, Capa dura, Ebook, Marcadores.
- **Formato e medidas:** unidade, lombada calculada do miolo (pelo número de páginas e pelo papel) e orelhas.
- **Montagem:** arquivo de capa, imagens (logo, selo, ISBN), montar capa.
- **Saída:** Gráfica (CMYK), Livro 3D e Exportar capa (PDF).
- **Conferência que bloqueia a exportação:** por exemplo, "sem arte" ou "o miolo tem 3 páginas (ímpar), e todo livro impresso fecha em número par".

### Motor (pelos nomes dos módulos)

| Parte | Módulos |
|---|---|
| Composição de linhas | Knuth-Plass com caixas, colas e penalidades (`line-break`, `paragraph-composer`) |
| Hifenização PT-BR | `hyphenation-ptbr` |
| Microtipografia | `microtype`, `optical-margins`, `hanging-punctuation`, `kerning-tracking`, `ligatures-ptbr`, `small-caps` |
| Página e abertura | `grid-baseline`, `drop-cap` (com tratamento de travessão), `page-composer`, `running-heads`, `front-matter` |
| Pré-textuais | Falsa folha de rosto, folha de rosto, créditos e ficha catalográfica, dedicatória, epígrafe, sumário, prefácio, introdução, listas de ilustrações e tabelas |
| Pós-textuais | Apêndices, glossário, bibliografia, notas, sobre o autor, colofão |
| PDF | `pdf-print-html` + `pdf-lib-assembly`, XMP, rótulos de página, marcadores, PDF/UA, PDF/X com perfil ICC FOGRA39/47, marcas de impressão, imposição, cadernos |
| Capa e outros | `cover-geometry`, `isbn-ean13`, `case-binding`, `book3d-*`, `bookmark-*`, `preflight-report` |
| Ebook | EPUB, Kindle |

## 2. O que o Escriba implementa nesta etapa (miolo e PDF) — entregue em 26/10/2026

| Recurso | Como |
|---|---|
| Formato, papel e sangria | Presets de formatos padrão do mercado (nacionais e KDP), papéis com espessura para calcular a lombada, sangria com marcas de corte opcionais |
| Margens espelhadas | Superior, inferior, interna (medianiz) e externa, em mm |
| Tipografia do corpo | Fonte do sistema (TrueType), corpo, entrelinha, alinhamento, recuo, 1º parágrafo sem recuo, espaço entre parágrafos |
| Composição fina | **Knuth-Plass** (algoritmo público de 1981) com recuo para quebra simples; hifenização PT-BR por **regras de divisão silábica próprias** |
| Viúvas e órfãs | Mínimo de linhas no pé e no topo da página |
| Abertura de capítulo | Página direita, esquerda ou qualquer; descida do título; número (arábico, romano ou por extenso); ornamento; capitular de N linhas |
| Cabeçalhos e fólios | Título do livro ou autor na página par, capítulo na ímpar; número no pé (centro ou externo) ou no topo; sem fólio nas aberturas e páginas em branco |
| Pré-textuais | Falsa folha de rosto, folha de rosto, créditos, sumário automático, partes do livro (dedicatória, prólogo, epílogo…) |
| Conteúdo | Títulos internos, citações, listas, quebras de cena, imagens com legenda, **notas de rodapé no pé da página** |
| Temas próprios | Clássico, Contemporâneo, Fantasia, Bolso, Elegante (valores próprios) |
| Saída | Prévia em spread idêntica ao PDF (mesmas fontes e posições) e PDF com fontes embutidas (Apache PDFBox) |
| Conferência | Contagem de páginas ímpar, fontes ausentes, imagens de baixa resolução |

**Próximas etapas:** capa (lombada, orelhas, ISBN EAN-13, mockup 3D), EPUB/Kindle, PDF/X com CMYK, objetos livres
na página, grade de base e margens ópticas.

## 3. Onde está no código

| Parte | Arquivo |
|---|---|
| Configurações, presets e temas | `shared/src/commonMain/.../core/layout/LayoutSettings.kt` (gravadas no livro em `layout.json`) |
| Hifenização PT-BR | `core/layout/Hyphenation.kt` |
| Knuth-Plass, quebra simples e montagem das linhas | `core/layout/LineBreaker.kt` |
| Paginação (aberturas, pré-textuais, sumário em duas passagens, notas, viúvas e órfãs, capitular, imagens) | `core/layout/Typesetter.kt` |
| Fontes do sistema e medição (mesmos arquivos do PDF) | `shared/src/jvmMain/.../platform/LayoutFonts.kt` |
| PDF (PDFBox) e prévia (Java2D) | `platform/LayoutRendering.kt` |
| Tela | `ui/LayoutScreen.kt` (menu lateral → Entregar → Diagramação) |
| Testes | `commonTest/LayoutTest.kt` (motor, sem fontes) e `jvmTest/LayoutPdfTest.kt` (PDF real com fontes embutidas) |

Para conferir as páginas do PDF de teste como imagens:

```
./gradlew :shared:jvmTest --tests com.cuboide.escriba.LayoutPdfTest -Pescriba.layoutPreviewDir=<pasta>
```
