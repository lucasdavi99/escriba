# Diagramação do Autor — análise completa das funcionalidades

Levantamento feito em 30/10/2026 sobre o **Autor 1.1.26**, instalado em `%LOCALAPPDATA%\Programs\Autor`.
Ele amplia o resumo de [diagramacao-autor.md](diagramacao-autor.md) e vai do miolo até a capa, a capa dura,
os marcadores e o livro 3D.

## Como foi feito (e o que não foi feito)

- **Fontes usadas:**
  - **Inventário de interface:** o `app.asar` foi extraído numa pasta temporária. Dele saíram as cerca de 1.630
    chaves de interface em português ligadas à diagramação (`dgm.*`, capa montada `cm.*`, começo e fim do livro
    `cf.*`), os nomes dos ~100 módulos de `renderer/diagramacao` e `services/diagramacao`, e as notas de versão.
  - **Mensagens de validação:** as mensagens de erro dos módulos do motor mostram que regra cada módulo confere.
- **Clean-room:** o código do Autor vem ofuscado e proíbe derivação e reimplementação (Termos, cláusula 11).
  Este documento **descreve comportamento com palavras próprias**. Ele não traz código, textos de interface
  copiados, artes nem valores de tabela do Autor. Medidas de mercado (formatos KDP, sangria de 3,2 mm, padrão
  GS1 do código de barras) são públicas e não vêm do Autor.
- **Limite:** o inventário mostra *o que existe*. Como cada recurso se comporta em detalhe só se confirma usando
  o app. Os itens marcados com *(motor)* aparecem como módulo do motor sem controle visível na interface; podem
  ser internos ou ainda não estar expostos.

## Visão geral

A seção Diagramação abre com uma escolha entre dois modos, e dá para voltar a ela a qualquer hora:

- **Diagramar Livro:** o miolo (páginas, tipografia, notas), o PDF e o e-book.
- **Diagramar Capa:** a capa aberta na medida da gráfica, com a lombada vinda do miolo. Tem abas para
  **Impressa**, **Capa dura**, **Ebook** e **Marcadores**, mais o **Livro 3D** e o painel **Montar capa**.

A diagramação é uma **cópia montável** do manuscrito: o que se muda nela não altera o texto original. Ela faz
parte de um plano pago (há bloqueio por plano).

---

## 1. Miolo

### 1.1 Fluxo de trabalho

| Recurso | O que faz |
|---|---|
| Trazer livro | Importa o manuscrito. Numa reimportação, **preserva** a montagem (caixas, títulos soltos, objetos) e **re-ancora** os elementos presos a trechos de texto. O que não consegue re-ancorar (quebras de cena, blocos de arte, inserções, alinhamentos) é listado para revisão. |
| Salvar e salvamento automático | Salva no projeto e avisa quando o salvamento automático falha, por exemplo por bloqueio de pasta do Windows. |
| Desfazer e refazer | Histórico com o nome de cada ação. |
| Recomeçar montagem | Apaga todos os ajustes manuais e reflui o texto limpo, mantendo formato, papel, sangria e margens. Dá para desfazer. |
| Temas | Presets do livro inteiro (fonte do corpo e do título, cabeçalho, fólio, capitular). Trocar de tema reflui tudo, e os ajustes por página continuam por cima. |
| Exportar e importar tema | Grava o visual num arquivo próprio para reusar em outro livro. Pergunta se a arte da capitular vai junto. |
| Minhas fontes | Importa `.ttf`/`.otf`. A fonte aparece em todos os seletores e fica embutida no projeto e no PDF. |
| Bloco auxiliar | Área de transferência de parágrafos: copiar, recortar e colar entre caixas e páginas. |

### 1.2 Página, papel e lombada

- **Formato:** presets nacionais (romance, grande, bolso, didático/álbum), KDP e **formato personalizado**.
- **Papel:** cada papel tem espessura (para a lombada) e cor (o tom do creme aparece na prévia).
- **Sangria:**
  - Presets nacional, KDP e sem sangria, mais um valor **personalizado em mm**.
  - Troca sozinha para a sangria de KDP quando se escolhe um formato KDP, e para a nacional num formato nacional.
- **Lombada:**
  - Janela com páginas × espessura e a **largura da capa aberta**.
  - Aceita um **caliper manual** (a espessura exata que a gráfica informar) e tem um botão para voltar ao valor
    do papel.
- **Visualização:** páginas abertas (spread) ou uma a uma, zoom e ajustar à largura.
- Nas notas de versão, o Autor admite que já calculou a lombada com o **dobro** da espessura real. Isso reforça
  que a espessura deve ser **por página**, e não por folha.

### 1.3 Edição direta na página (o "clicar e arrastar")

É a maior diferença para o Escriba de hoje: quase tudo se ajusta na própria página.

| Recurso | O que faz |
|---|---|
| Caixa da escrita (mancha) | **Global** ou **só desta página**. Na página, alças arrastáveis mudam as margens (superior, inferior, dentro, fora). O inspetor mostra as mesmas medidas em mm. |
| Mover a coluna | Arrastar a coluna de texto inteira, ou uma parte da caixa, dentro da página. |
| Travar largura ou altura | Trava cada dimensão da caixa para não desconfigurar sem querer. |
| Indicador de espelho | Mostra quando a mancha está igual à da página ao lado. |
| Definir como global | Promove a mancha desta página a padrão do livro e limpa as exceções. |
| Tipografia por página | Clicar numa página permite dar a ela corpo, entrelinha etc. próprios, com botão para voltar ao padrão. |
| Dividir caixa | Seleciona parágrafos e divide a caixa (até 3 por página). Também cria uma caixa livre a partir de uma seleção. |
| Texto que não coube | Aviso na caixa, com a sugestão de mover o excedente (copiar, recortar, colar). |
| Réguas em mm | Réguas no palco (`canvas-ruler`). |
| Guias livres | Guias verticais e horizontais que não imprimem; botão para limpar. |
| Guias inteligentes | Ímã para borda e centro da página, do conteúdo e de outros objetos (Alt liga o ímã). |
| Quebra manual | "Quebrar página aqui", inserir página em branco antes de um parágrafo, remover página inserida. |
| Edição de texto | Editor de texto do capítulo dentro da diagramação, separado das caixas soltas. |

### 1.4 Tipografia do corpo (livro inteiro)

- **Medidas:** fonte, corpo, entrelinha, alinhamento, recuo da 1ª linha em em, espaço entre parágrafos e após o
  título (em linhas).
- **Primeiro parágrafo sem recuo:** opcional, desligado por padrão.
- **Hifenização:** ligada por padrão, só no corpo.
- **Composição fina:** composição do parágrafo inteiro, que também aperta espaços; desligada por padrão.

### 1.5 Estilos de parágrafo e de trecho

- **Estilos de parágrafo editáveis:** mudar um estilo muda todos os parágrafos que o usam.
  - **Estilos:** corpo, primeiro parágrafo, epígrafe, citação, verso, onomatopeia, subtítulo interno e bloco de
    cena (local, data e hora), além de mensagem enviada e recebida.
  - **Propriedades:** fonte, versalete, espaço entre letras, tinta (% de preto) e recuo de bloco.
  - **Regra de pé de página:** subtítulo e bloco de cena nunca ficam sozinhos no pé da página.
- **Detectar estilos:** reconhece onomatopeias, subtítulos e blocos de cena no texto e aplica os estilos
  (desfazível).
- **Estilo de trecho:** negrito, itálico, versalete, sobrescrito, subscrito, fonte só do trecho, tinta (inclusive
  branco vazado) e espaço entre letras.
- **Alinhamento:** por parágrafo selecionado, e "centralizar este parágrafo" para local/data ou epígrafe.
- **Conversa por mensagens:** transforma parágrafos `Nome: fala` em balões, com as falas de uma pessoa à direita
  e as outras à esquerda. Aparece na tela, no PDF e no e-book.
- *(motor)* Versos com numeração a cada N e recuo de continuação; tabelas com cabeçalho repetido e pesos de
  coluna; listas com níveis.

### 1.6 Capítulos e aberturas

- **Painel de capítulos:** página de início, ir até ela, **mudar a ordem de impressão**, editar texto e contagem
  de capítulos e páginas.
- **Por capítulo:**
  - **Soltar título:** o título vira uma caixa livre, para arrastar e estilizar.
  - **Ocultar título:** para quando a arte da abertura já traz o título.
  - **Aplicar um estilo de abertura** e **realinhar a abertura**, que recoloca título, epígrafe, fundo e capitular
    nas posições calibradas.
  - **Travar a abertura:** as peças não se movem ao arrastar.
  - **Capitular:** ligar ou desligar só neste capítulo, ou usar uma fonte própria nele.
- **Onde abre:** padrão do livro (direita, esquerda ou próxima página), com **exceção por capítulo**.
- **Padrão de título:** grava a métrica de um título solto como padrão; os próximos saem iguais.
- **Capitular:**
  - **Arte atrás da letra:** uma moldura ilustrada, a "capitular iluminada".
  - **Por parágrafo, no menu de contexto:** pôr a capitular aqui, tirar deste parágrafo ou voltar ao automático.
  - *(motor)* Trata travessão de diálogo e conflito com contorno de arte.

### 1.7 Cabeçalho e número de página

- **Cabeçalho:**
  - **Texto por capítulo:** página esquerda e direita, mais o **número do capítulo fixo**, útil em projeto
    parcial. O título do livro do cabeçalho é editável.
  - **Posição:** canto de fora, centro, canto de dentro, sempre à esquerda ou sempre à direita.
  - **Visual:** estilos prontos, tinta (preto ou dois cinzas), subir ou descer em passos de 0,5 mm.
  - **Exceções:** clicar no cabeçalho de uma página tira **só dele**. Opção de cabeçalho também na abertura de
    capítulo.
- **Número de página (fólio):**
  - **Onde aparece:** todas as páginas, só ímpares ou só pares.
  - **Na abertura:** número centrado no pé, sem número (mas contando) ou normal.
  - **Contagem:** onde começa a numeração e **romanos nas pré-textuais**. Folhas escolhidas ficam **sem número**,
    mas contam.
  - **Arte do fólio:**
    - Uma imagem por lado (esquerda e direita), arrastável com o número.
    - Um botão centra o número no anel da arte.
    - Ajustes de tamanho, negrito, itálico, opacidade do número e da arte e escorrido.
    - Opção de deixar o enfeite passar do corte.
  - **Ocultar só nesta página:** o cabeçalho e o fólio.

### 1.8 Começo e fim do livro

- **Montar pelo guia:**
  - **Páginas de antes da história:** falsa folha de rosto, folha de rosto, créditos, ficha catalográfica (CIP),
    dedicatória, epígrafe, elogios, frontispício, aviso ao leitor, nota do tradutor, personagens e sumário.
  - **Páginas de depois:** agradecimentos, sobre o autor, do mesmo autor, perguntas para clube de leitura,
    bibliografia, colofão e pedido de avaliação.
  - **Preenchimento:** cada página nasce preenchida com título, autor e ano.
- **Ordem do guia:** avisa quando uma página está fora da ordem habitual e tem **"arrumar a ordem"**. Também
  avisa quando a mesma página existe no manuscrito e na diagramação.
- **Nova seção:** pré ou pós-textual, só na diagramação, sem número e que sobrevive à reimportação.
- **Parte do livro:** prólogo, epílogo, interlúdio, posicionada antes ou depois de um capítulo.
- **Sumário automático:** cria e atualiza com as páginas reais.
- *(motor)* Índice remissivo com ordenação em português, listas de ilustrações e de tabelas, referências
  cruzadas do tipo "ver pág. N" e "ver capítulo".

### 1.9 Objetos, imagens e arte

- **Caixa de texto livre:**
  - **Texto:** fonte, corpo, entrelinha, alinhamento horizontal e vertical, espaço entre letras, tinta (K%).
  - **Caixa:** moldura (espessura, canto arredondado, sólida ou tracejada, tinta), preenchimento, respiro interno.
  - **Posição:** posição e tamanho em mm, centralizar na página.
  - **Estilos de objeto prontos e "meus estilos"** salvos, mais um **padrão global de caixas**: definir uma caixa
    como padrão aplica tamanho e aparência a todas, e cada caixa pode voltar ao global.
- **Imagem livre (PNG):**
  - **Visual:** opacidade e **escorrido** (bordas que se dissolvem no papel), por borda e com desfoque em mm.
  - **Recorte** por alças.
  - **Camada:** ordem frente e trás, **atrás do texto** (fundo), **repetir em todas as páginas** (página-mestra).
  - **Ajustes rápidos:** enquadrar na proporção da arte, cobrir a página com sangria, caber nas margens.
- **Contorno do texto:**
  - **Modos:** contornar a caixa, contornar a **forma** (pelo recorte transparente), atrás do texto ou na frente.
  - **Ajustes:** texto dos dois lados quando a arte está no meio e respiro em mm.
  - **Comportamento:** funciona enquanto se arrasta. Não se aplica na página de abertura.
- **Imagem de página cheia:**
  - **Enquadrar:** preencher a folha ou mostrar a arte inteira; arrastar, aproximar, afastar, centralizar.
  - **Medidas:** X, Y e largura em mm, com 0 na linha de corte.
  - **Avisos:** mostra quanto é cortado em cada lado, a faixa de sangria e "falta arte" quando sobraria papel em
    branco.
- **Imagem dupla:**
  - Atravessa a calha e se prende ao centro, ao verso ou à frente.
  - Avisa se deixa fresta branca na lombada.
- **Arte entre parágrafos:**
  - Elemento que abre espaço no texto e reflui (ampulheta, ilustração).
  - Ajustes: respiro em linhas, −/+ tamanho e recorte.
- **Biblioteca de artes embutida:** cerca de 700 imagens com busca.
  - Aberturas simples e duplas por gênero (fantasia, gótico, cyberpunk, náutico…), cada uma em vários traços.
  - Botânicos, cantos e molduras, divisores e filetes, emblemas, fleurões e sinais.
- **Enfeites vetoriais:** filetes, fleurões e anéis, sem imagem externa.
- **Vínculos:** aponta uma pasta de artes do autor uma vez e insere cada arte por **papel**:
  - página inteira (empurra o texto) ou fundo de página inteira;
  - objeto solto atrás do texto ou enfeite solto;
  - arte de rodapé;
  - **página-número do capítulo** (arte com o numeral grande por cima);
  - **quebra de cena com bloco local/data/hora**.
- **Camadas:** lista frente→trás da página, com nomear, ocultar (não imprime) e travar.
- **Quebra de cena:** texto, marca própria, ornamento ou imagem sua aplicada a **todas** as quebras do livro,
  com largura em mm.
- **E-book:** legenda e descrição para leitor de tela (texto alternativo) em cada imagem.

### 1.10 Notas de rodapé

- **Tipos:** nota **numerada** ou **anotação por símbolo**.
- **Criar:** clicando na palavra que a nota comenta, com o editor da nota abrindo no pé.
- **Organizar:** remover renumera. Uma nota pode **virar caixa livre** e ser **travada**: as próximas nascem na
  mesma posição.
- *(motor)* Numeração contínua ou por capítulo, nota no pé ou no fim.

### 1.11 Motor e microtipografia *(motor)*

- **Quebra e hifenização:** quebra de linha Knuth-Plass com caixas, colas e penalidades, e hifenização em
  português com exceções.
- **Ajustes ópticos:** margens ópticas, pontuação pendurada, kerning e espaço entre letras por contexto,
  expansão e contração leve de glifos, ligaduras e versalete (sintético ou OpenType).
- **Grade e limpeza:** grade de linha de base e limpeza tipográfica de aspas, travessões e espaços.
- **Medição:** o Autor garante que a tela é **exatamente** o PDF. Ele espera a fonte carregar antes de medir e
  repete a repaginação até as caixas soltas pararem de mudar de folha.

### 1.12 Mostrar (só na tela)

Guias, cabeçalho e número, letra grande, grade de base e "papel branco" (ver a página sem o tom creme).

### 1.13 Conferência do miolo

- **Semáforo antes de exportar:** vermelho (sai errado no impresso), amarelo (pendência) e verde.
  - **O que confere:** sumário com página velha, margem da lombada, texto perto do corte, arte que não chega à
    sangria, imagem de baixa resolução e fonte ausente.
  - **Consertos de um clique:** alargar a medianiz (repagina e avisa que a lombada mudou), criar ou atualizar o
    sumário e ir até a página.
  - **Extras:** dicas para o livro "parecer de editora" e passo a passo antes da gráfica (prova, capa por último).
- **Ficha técnica para orçamento:**
  - **Conteúdo:** formato, páginas (completando para cadernos de 16), papel, cores do miolo, lombada, sangria e
    capa aberta.
  - Botão de **copiar** para mandar à gráfica.
- **Portão antes do PDF:** lista os pontos vermelhos, com "ver e corrigir" ou "gerar assim mesmo".
- **Termo de responsabilidade:** o autor confirma que fará a conferência final; o botão só libera após alguns
  segundos.
- **Conferência do PDF pronto:** depois de gerar, o app **lê o próprio PDF**.
  - **O que procura:** texto além do corte, linha raspada por recorte, texto do livro ausente ou duplicado, caixa
    que não saiu inteira e sumário divergente.
  - **Resultado:** relatório copiável, com "salvar assim mesmo" ou "não salvar".

### 1.14 Saídas do miolo

- **Destinos do PDF:**
  - **Sob demanda:** RGB, para KDP e similares.
  - **Gráfica:** CMYK, PDF/X com perfil ICC embutido, conversão própria com **preto só em K** no miolo P&B e
    limite de cobertura de tinta.
  - **Gráfica colorida.**
  - **P&B forçado.**
  - Quando a conversão não é possível, oferece RGB ou P&B.
- **Estrutura do PDF:** metadados XMP, **rótulos de página** (romanos), marcadores e marcação de acessibilidade
  (PDF/UA, com aviso de figura sem texto alternativo).
- *(motor)* **Imposição e cadernos** de 8, 16 ou 32 páginas, costurado ou colado, e marcas de impressão.
- **Cópia para leitor beta:**
  - **Carimbo:** nome e CPF do leitor (validado) em todas as páginas, posição arrastável, assinatura do autor,
    fonte e tamanho.
  - **Leitores salvos** para as próximas cópias.
- **E-book (EPUB para Kindle):**
  - **Estilo do e-book:**
    - letra do livro ou do leitor;
    - parágrafos com recuo ou espaço;
    - alinhamento;
    - início de capítulo (capitular, versalete nas primeiras palavras ou simples);
    - quebra de cena (ornamento do livro ou filete);
    - espaço antes do título;
    - qualidade das imagens;
    - sumário clicável.
  - **Páginas:**
    - página de rosto e copyright (ou texto livre);
    - dedicatória;
    - sobre o autor;
    - **pedido de avaliação** logo após a história, com link;
    - **outros livros do autor**, com links.
  - **Metadados:** sinopse e autor, com aviso quando falta o autor.
  - **Arquivo:**
    - opção de embutir as fontes;
    - imagens redimensionadas para a tela, respeitando o recorte;
    - aviso de peso em MB (a loja cobra a entrega por MB).
  - **Conferência do EPUB** antes de exportar.
- **Prévia Kindle:**
  - Aparelhos: Paperwhite, Scribe e celular.
  - Temas de leitura: branco, sépia, verde e escuro.
  - Tamanho da letra, entrelinha e margens com 3 opções, ir para capítulo e contagem de páginas.

---

## 2. Capa

### 2.1 Capa impressa (brochura)

- **Medidas:**
  - Formato, **lombada vinda do miolo** (com o número de páginas), orelha e unidade em mm ou cm.
  - Legenda de corte, dobra, sangria e zona do ISBN.
- **Arte da capa:**
  - **PDF é o recomendado:** vai inteiro para o export, sem perda. PNG e JPG também servem. PDF pesado vira um
    cartão na prévia, mas sai em qualidade máxima.
  - **Ajustes:** arrastar para posicionar e Ctrl+rolagem para zoom.
  - **Tamanho da arte em %:**
    - 100% cobre tudo, até a sangria.
    - **Abaixo de 100%** sobra uma **borda branca de corte** (mostrada em mm).
    - Acima de 100% amplia.
  - Botão de reenquadrar e centralizar.
- **Imagens por cima:** logo, selo e código ISBN pronto, com botão para **encaixar na zona do ISBN** da
  contracapa.
- **Saída:** Gráfica (PDF/X em CMYK) ou Sob demanda (RGB). Se a arte não converte para CMYK, sai em RGB com
  aviso.
- **Conferência capa × miolo:**
  - **Diferenças:** formato, papel, sangria e **lombada fora da paginação** (com a diferença em mm e "usar a
    lombada do miolo").
  - **Outros avisos:** miolo ímpar, marcas que não cabem na sangria, sem miolo diagramado (a lombada é palpite) e
    **orelha com saída sob demanda** (POD não imprime orelha).
  - **Níveis:** crítico ("impede a gráfica") e aviso dispensável.
  - Confirmação para exportar assim mesmo.

### 2.2 Montar capa (sem arte pronta)

- **Começo:** escolhe-se um **estilo** (clássico, romance, suspense, sombrio, leve). O app preenche com título,
  autor, sinopse e ISBN do livro.
- **Elementos (papéis):**
  - **Textos:** título, subtítulo, autor, título e autor na lombada, sinopse, sobre o autor, texto da orelha da
    capa e da contracapa e texto livre.
  - **Peças:** código de barras, **foto do autor**, **ilustração** e **imagem de fundo de um painel**.
- **Painéis:** capa, contracapa, lombada e as duas orelhas. Cada elemento pertence a um painel; se o painel não
  existir (sem orelhas), avisa.
- **Fundo por painel:**
  - Nenhum, cor ou **degradê linear ou radial** com várias cores e ângulo.
  - "Usar este fundo em todos os painéis".
  - **Degradês por gênero:** a capa ganha o degradê e o resto a cor mais forte dele.
- **Texto:**
  - **Letra:** fonte, peso, corpo, entrelinha, espaço entre letras, itálico, caixa alta e cor.
  - **Posição:** alinhamento horizontal e vertical na caixa, centralizar no painel.
  - **Ajuste:** diminuir o corpo se não couber, hifenizar e "usar o texto do livro".
- **Sentido na lombada:** de cima para baixo (Brasil, EUA, Reino Unido), de baixo para cima (França, Alemanha,
  Itália) ou em pé (lombada larga).
- **Manipulação:** arrastar na capa, alça para redimensionar e setas que empurram 0,5 mm (5 mm com Shift).
  Desfazer e refazer próprios.
- **Imagens:**
  - **Foto do autor:** moldura redonda, de cantos arredondados ou reta.
  - **Ilustração:** pintar a silhueta numa cor (exige PNG transparente).
  - **Fundo de painel:** imagem de fundo de um painel só.
  - **Colar:** colar da área de transferência e escolher o papel da imagem.
  - **Ordem:** trazer à frente e mandar para trás.
  - Há um limite de imagens.
- **Código de barras:**
  - **Origem:** ISBN de 13 dígitos (com ou sem hífens), vindo do livro.
  - **Validação:** dígito verificador conferido; se inválido, nenhuma barra é gerada.
  - **Tamanho e cor:** **tamanho de 80% a 100%** (GS1), barras em preto puro sobre painel branco.
  - **Conferência de leitura:** o app decodifica o que gerou ("o leitor lê …").
- **Avisos ao vivo:**
  - **Texto:** não cabe, corpo reduzido para caber, perto do corte ou da dobra, **contraste baixo com o fundo**
    (com a razão), lombada estreita para o texto e **poucas páginas para texto na lombada**.
  - **Código de barras:** ISBN vazio ou inválido, código ilegível e código fora da área segura.
  - **Peças:** **sobreposição** entre elementos e imagem abaixo de 300 dpi no tamanho usado.
- **Convivência com arte:** a arte pronta continua valendo; o fundo montado fica por baixo e os textos por cima.

### 2.3 Capa dura

- **Folha própria:** não é a brochura esticada.
- **Medidas:**
  - **Lombada do case** = miolo + duas chapas de papelão.
  - **Praça:** a tampa passa do miolo na cabeça, no pé e no corte.
  - **Virada:** a sobra que dobra para dentro da tampa.
  - **Quatro vincos**, mais marcas de dobra e de corte.
- **Encadernação:** presets (gráfica nacional, KDP) que mudam a virada e o tamanho da folha.
- **Zonas:** faixas mostram o que some na virada e o que aparece na praça.
- **Partir da brochura:** traz a arte da capa impressa como ponto de partida e mostra **quantos mm faltam** em
  cada borda.
- **Exportação:** PDF da folha. Avisa se as marcas não couberam na virada.

### 2.4 Capa de e-book

- **Arte:** solta-se uma imagem ou **usa-se a frente da capa impressa**, recortada automaticamente.
- **Prévia:** área segura marcada.
- **Tamanho:** padrão KDP (1600 × 2560 px) ou proporção igual à do livro impresso.
- **Saída:** JPEG em alta, que também vai embutido no EPUB.

### 2.5 Marcadores (marca-páginas)

- **Cartão:**
  - **Tamanho:** padrões BR e US, ou personalizado.
  - **Sangria:** Brasil (3 mm), EUA (0,125 in) ou sem.
  - **Furo para cordão** opcional, com validação de que o furo cabe.
  - Guias e margem de segurança.
- **Lados:** **frente e verso** compostos à parte, cada um com arte arrastável e zoom, ou "usar a frente da
  capa".
- **Conjunto:** vários marcadores, com navegação e adicionar ou remover. Lados vazios não são exportados.
- **Saída:** PNG a 300 dpi por lado, ou **PDF de gráfica** com uma página por lado ou **vários cartões por
  folha**, com marcas de corte e do furo.

### 2.6 Livro 3D

- **Modelo:** o livro nas **medidas reais**, com capa, lombada e contracapa a partir da arte impressa.
- **Interação:** arrastar gira, Shift+arrastar move e duplo clique centraliza.
- **Alternância brochura ↔ capa dura:** praça, grossura da chapa, vinco e cabeceado; vale só na prévia.
- **Onde aparece:** ao lado da edição da capa (também na capa dura) e como **vitrine na Biblioteca**. Sem capa
  diagramada, a Biblioteca mostra só o convite "Diagramar Capa".
- Acompanha o tamanho do monitor.

---

## 3. Comparação com o Escriba (30/10/2026)

Legenda: ✅ tem · 🟡 parcial · ❌ não tem.

| Área | Autor | Escriba |
|---|---|---|
| Formatos, papéis, sangria | Presets, personalizado, sangria em mm, troca automática para KDP | 🟡 presets; sem formato nem sangria personalizados |
| Lombada | Calculada, caliper manual, capa aberta | 🟡 calculada (páginas × papel); sem caliper manual |
| Margens | Numéricas e **arrastando na página**, mancha por página, travas, "definir global" | ✅ arrastando bordas ou a mancha inteira na página, mancha só da página, "definir como global" (sem travas) |
| Réguas, guias, guias inteligentes | ✅ | 🟡 réguas em mm com as margens marcadas; sem guias livres nem ímã |
| Tipografia do corpo | ✅ | ✅ (com viúvas e órfãs configuráveis) |
| Tipografia por página | ✅ | ❌ |
| Estilos de parágrafo editáveis e detecção | ✅ | 🟡 títulos internos, citação, listas e quebra de cena vindos da marcação; estilos fixos |
| Estilo de trecho (tinta, fonte, espaçamento) | ✅ | 🟡 negrito, itálico, versalete e sobrescrito vindos do manuscrito |
| Conversa por mensagens | ✅ | ❌ |
| Abertura de capítulo | Por capítulo, títulos soltos, travar, arte | 🟡 só global: lado, descida, número, ornamento, capitular |
| Capitular | Por capítulo e parágrafo, fonte própria, arte atrás | 🟡 global, N linhas |
| Cabeçalho | Texto e posição por capítulo, estilos, tirar por página | ✅ conteúdo por lado, texto próprio por capítulo, posição (fora, centro, dentro), tinta, subir e descer, na abertura, tirar por página |
| Fólio | Política por página, romanos, arte por lado, ocultar por página | 🟡 posição, ímpares ou pares, número na abertura, romanos nas pré-textuais, início da numeração, ocultar por página, rótulos no PDF; sem arte de fólio |
| Pré e pós-textuais | Guia completo com modelos e ordem | 🟡 falsa folha de rosto, rosto, créditos, sumário e partes pelo tipo do capítulo |
| Objetos livres (texto, imagem, camadas) | ✅ | ❌ |
| Contorno de texto | ✅ | ❌ |
| Imagem de página cheia e dupla | ✅ | ❌ (imagem só no fluxo, com legenda) |
| Biblioteca de artes, enfeites, vínculos | ✅ | ❌ (só o losango de ornamento) |
| Notas de rodapé | Numeradas e por símbolo, caixa livre | ✅ numeradas no pé |
| Microtipografia (margens ópticas, grade de base) | ✅ *(motor)* | ❌ (Knuth-Plass e hifenização ✅) |
| Conferência do miolo | Semáforo, consertos, ficha técnica, leitura do PDF pronto | 🟡 semáforo com consertos de um clique (medianiz, margens no corte, exceções antigas), portão antes do PDF e ficha técnica copiável; sem leitura do PDF pronto |
| PDF | RGB, CMYK/PDF-X, P&B, XMP, rótulos, marcadores, PDF/UA | 🟡 RGB com fontes embutidas, marcadores, TrimBox e BleedBox |
| Cópia para leitor beta | ✅ | ❌ |
| EPUB e prévia Kindle | ✅ | ❌ |
| Capa impressa com arte | ✅ (PDF vetorial, zoom e borda, imagens por cima) | ✅ arte com tamanho em %, borda abaixo de 100% e arraste do enquadramento; imagens soltas (logo, selo) arrastáveis; sem arte em PDF vetorial |
| Montar capa (textos e fundos editáveis) | ✅ | 🟡 peças de texto arrastáveis na prévia (título, autor, lombada, sinopse, código), degradê linear ou radial, sentido na lombada, cor de texto; sem foto do autor, textos de orelha nem estilos prontos |
| Código de barras ISBN | ✅ (tamanho GS1, conferência de leitura) | ✅ (EAN-13 validado, tamanho fixo) |
| Conferência capa × miolo | ✅ | ✅ bloqueia sem arte, ISBN inválido e páginas ímpares; avisa capa gerada com lombada antiga, orelha em POD, peça perto do corte ou sobreposta e pouco contraste |
| Capa dura | ✅ | ❌ |
| Capa de e-book | ✅ | ✅ frente da capa em JPEG, na proporção do livro ou 1600 × 2560 (Kindle) |
| Marcadores | ✅ | ❌ |
| Livro 3D | ✅ | 🟡 livro em perspectiva nas medidas reais (capa, lombada, contracapa, bloco de páginas), arrastar gira e o duplo clique volta; vitrine na Biblioteca; sem capa dura nem deslocar o livro |

## 4. Sugestão de ordem para o Escriba

A ordem pesa valor para quem publica e esforço no código atual (motor em `core/layout`, PDFBox e prévia Java2D).

1. ~~**Margens na própria página.**~~ Entregue em 30/10/2026: alças nas bordas, mover a mancha inteira, réguas em
   mm, mancha só da página e "definir como global". Faltam guias livres, ímã e travas de largura e altura.
2. ~~**Fólio e cabeçalho completos.**~~ Entregue em 30/10/2026. Falta a arte do fólio por lado.
3. ~~**Conferência do miolo em semáforo**~~ com consertos de um clique, portão antes do PDF e ficha técnica:
   entregue em 30/10/2026. Falta conferir o PDF já gerado.
4. ~~**Capa, segunda rodada**~~ entregue em 30/10/2026. Falta arte em PDF vetorial, foto do autor e textos
   de orelha. O que estava planejado:
   - **Arte:** zoom e posição da arte, borda de corte abaixo de 100% e imagens extras (logo, selo).
   - **Montar capa:** peças de texto arrastáveis, fundos em degradê e sentido do texto na lombada.
   - **Conferência:** capa × miolo (lombada salva *versus* atual) e contraste do texto.
   - **Saídas:** capa de e-book exportada da frente.
5. ~~**Livro 3D**~~ entregue em 30/10/2026: perspectiva própria (homografia por face) na aba Capa e vitrine na
   Biblioteca, gravada ao ver o livro 3D ou ao gerar o PDF da capa. Falta a versão capa dura.
6. **Aberturas por capítulo e objetos livres** (texto e imagem com camadas), seguidos de imagem de página cheia
   e contorno de texto.
7. **Começo e fim pelo guia,** com modelos próprios de texto.
8. **EPUB** com a estrutura do livro (sumário, rosto, créditos, imagens com descrição).
9. **Capa dura e marcadores.** Reaproveitam a geometria da capa (`CoverGeometry` + zonas) com virada, praça e
   vincos.
10. **CMYK, PDF/X e imposição.** Exigem gestão de cor e perfil ICC; ficam por último.

## 5. Lições das notas de versão do Autor

- **Lombada:** a espessura do papel deve ser **por página**, e não por folha. O Autor já errou isso (lombada quase
  dupla). O Escriba usa `PaperStock.mmPerPage` por página; manter um teste com um valor conhecido.
- **Repaginação iterativa:** caixas presas a texto mudam o fluxo, que muda as folhas. É preciso repetir até
  estabilizar, senão títulos e caixas saem por cima do texto.
- **Medir depois da fonte carregar:** medir antes deu texto cortado no PDF. No Escriba a medição e o PDF usam o
  mesmo arquivo de fonte, o que evita esse problema.
- **Conferir o arquivo final,** e não só o modelo: ler o PDF gerado pegou defeitos que a conferência prévia não
  via.
- **Impressão sob demanda (KDP/Ingram) não imprime orelha;** avisar quando houver orelha com saída POD.
- **Imagens no e-book:** reduzir para a tela e aplicar o recorte, porque a loja cobra a entrega por MB.
