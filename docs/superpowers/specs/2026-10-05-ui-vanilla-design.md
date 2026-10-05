# UI Vanilla (estilo Conquistas) — Design

**Data:** 2026-10-05 · **Escopo:** telas do cliente do Vanilla+ Talents (talentos, escolha de classe, confirmação de troca).
**Fora do escopo:** regras, efeitos, rede (nenhum pacote novo), prévia do valor do próximo nível de cada nó.

## Objetivo

O problema apontado pelo José é **visual cru**: as telas não têm moldura nem fundo próprio e os nós parecem quadrados soltos. Meta: telas com cara de parte do Minecraft, no estilo da tela de Conquistas, mantendo o comportamento atual.

Critério de sucesso: em jogo, a tela de talentos e a de classe parecem telas vanilla (moldura em relevo, abas, molduras de conquista, fundo de bloco) e continuam funcionando como hoje (comprar, converter, converter tudo, trocar classe).

## Decisões do brainstorming

| Tema | Decisão |
|---|---|
| Direção visual | Vanilla, estilo Conquistas |
| Layout da árvore | Árvore à esquerda + painel de detalhes do nó à direita |
| Compra | Clique seleciona; botão "Comprar" no painel; **clique duplo compra direto** |
| Estado dos nós | Molduras de conquista: cinza (normal), dourada (completo), escurecido (bloqueado), fio verde (disponível), `n/max` + barrinha (progresso), moldura de desafio no capstone |
| Fundo | Textura de bloco temática por árvore |
| Escolha de classe | Lista à esquerda + painel de detalhes; confirmação da troca dentro do painel (2 cliques) |
| Renderização | Sprites e texturas vanilla para nós, abas e fundo; moldura da janela desenhada em código |

## 1. Tela de talentos

**Janela:** centralizada, ~400×225 px de GUI, moldura em relevo cinza (como inventário vanilla), título "Talentos — <árvore>". Encolhe até o mínimo do jogo (320×240 px de GUI).

**Abas** acima da janela (sprites `advancements/tab_above_*`): Comum (ícone escudo) e Classe (ícone do nó raiz da classe atual; sem classe, a aba abre a escolha de classe).

**Área da árvore** (esquerda, painel afundado):
- Fundo de bloco repetido por árvore: Comum = pedra, Minerador = deepslate, Produtor = terra arada, Desbravador = grama (lateral), Guerreiro = blackstone polido, Arqueiro = tábuas de carvalho. Bordas levemente escurecidas.
- Conexões: branca com contorno preto quando o pré-requisito está atendido; cinza escuro quando não.
- Nós: moldura de conquista vanilla com o ícone do item no centro.
  - bloqueado: `task_frame_unobtained` escurecido;
  - disponível: `task_frame_unobtained` + contorno verde de 1 px;
  - em progresso: `task_frame_unobtained` + contador `n/max` + barrinha de progresso;
  - completo: `task_frame_obtained` (dourada) + `n/max`;
  - capstone: `challenge_frame_unobtained` / `challenge_frame_obtained` com os mesmos indicadores.
  - selecionado: contorno amarelo.
- Se a árvore não couber na área: arrastar com o botão esquerdo move a vista (limitado às bordas da árvore). Se couber, fica centralizada e não arrasta.
- Passar o mouse num nó mostra só o nome em tooltip curto.

**Painel de detalhes** (direita, largura fixa ~110 px):
- Com nó selecionado: nome (amarelo), descrição, `Nível n/max`, requisitos (✔ verde / ✘ vermelho com `n/exigido`), custo (1 PT), botão **Comprar (1 PT)**. Botão desabilitado quando não dá para comprar, com o motivo acima: "Sem PT", "Requisito faltando", "Nível máximo", "Escolha uma classe", "Outra classe".
- Sem seleção: resumo da árvore (PT gastos, nós completos / total, PT para completar).

**Rodapé** dentro da janela: barra de XP, "Converter 5 níveis → 1 PT", "Converter tudo (N PT)", "Trocar classe" (só na aba Classe com classe escolhida).

**Interação:** clique seleciona; clique duplo compra (envia `C2SBuyNode`); depois da compra o nó segue selecionado e o painel atualiza quando chega o `S2CSyncPlayer`. Sem atualização otimista.

**Sem classe** na aba Classe: área da árvore mostra "Nenhuma classe escolhida" e um botão "Escolher classe".

## 2. Tela de classes

Substitui `ClassSelectScreen` e `ConfirmRespecScreen` (removidas).

**Janela:** mesma moldura, ~300×180 px de GUI, título "Classes".

**Lista à esquerda:** 5 classes, cada uma com ícone (o do nó raiz) em moldura e nome. A atual aparece "(atual)" e não é selecionável.

**Painel à direita:** nome, descrição curta (chave nova `vanillatalents.class.<id>.desc`), raiz e capstone (ícone + nome), "N nós · M PT para completar".
- Sem classe: "A primeira escolha é gratuita" + botão **Escolher <classe>** (envia na hora).
- Com classe: prévia da troca — taxa (níveis), PT gastos que serão zerados, PT devolvidos, "A Árvore Comum não é afetada" — e botão vermelho **Trocar para <classe> (−N níveis)**. Primeiro clique arma ("Clique de novo para confirmar", ~3 s); segundo clique envia `C2SChangeClass`. Se faltarem níveis: botão desabilitado + motivo em vermelho. Taxa 0 (nada gasto) mostra "Troca gratuita".
- Depois de enviar: volta para a tela de talentos na aba Classe.

## 3. Arquitetura

| Unidade | Responsabilidade |
|---|---|
| `client/ui/VanillaGui` | Desenho da moldura em relevo, painel afundado, fundo de bloco repetido; constantes com os ids dos sprites vanilla (único lugar que os conhece). |
| `client/ui/TreeView` | Desenha a árvore, hit-test de nós, arrastar para mover. |
| `client/ui/NodeDetailPanel` | Painel do nó selecionado ou resumo; estado do botão Comprar. |
| `client/TalentScreen` | Orquestra abas, título, rodapé, seleção e clique duplo. |
| `client/ClassScreen` | Lista + painel + confirmação em 2 cliques. |
| `core/TalentScreenModel` (estendido) | Lógica pura: motivo do bloqueio (a partir de `PurchaseResult`), resumo da árvore, resumo da classe, layout/limites do arrastar, mapa árvore → fundo. |
| `core/TwoStepConfirm` | Máquina de estados pura: ocioso → armado (até um tick limite) → confirmado; expira sozinho. |

**Fluxo de dados:** inalterado. O motivo do bloqueio é calculado no cliente com `TalentRules.canPurchase` (mesma regra do servidor, sobre `ClientTalentState` + registro do cliente). O servidor continua sendo a autoridade.

**Traduções novas** (pt_br e en_us): motivos de bloqueio, textos do painel e do resumo, rótulos da tela de classes, 5 descrições de classe.

## 4. Erros e casos-limite

- Sprite ausente (resource pack): o jogo usa a textura "faltando"; nada trava.
- Registro vazio antes do sync: "Carregando talentos…".
- Nó selecionado que deixa de existir após `/reload`: seleção limpa.
- Tela menor que a janela padrão: janela encolhe até 320×240; árvore passa a ser arrastável.

## 5. Testes

- **JUnit (core):** motivo para cada `PurchaseResult`; resumo da árvore e da classe com os números reais dos JSON (12 nós; 37/36/35/34/35/37 PT); `TwoStepConfirm` (armar, confirmar dentro do prazo, expirar); limites do arrastar (árvore que cabe = sem deslocamento; que não cabe = limitado às bordas); mapa de fundos cobre as 6 árvores.
- **Regras de código existentes:** `LangKeysTest` (passa a incluir as 5 descrições de classe), `InputConstantsUsageTest`, `EventSubscriberRulesTest`.
- **Manual (José, em jogo):** checklist no plano — abas, fundos temáticos, molduras por estado, seleção, clique duplo, motivo do bloqueio, Converter/Converter tudo, tela de classes nos dois casos (sem classe / com classe), confirmação em 2 cliques, tela pequena (escala de GUI máxima) com arrastar.
