# UI Vanilla (estilo Conquistas) — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesenhar a tela de talentos e a de classes no estilo da tela de Conquistas vanilla, sem mudar regras nem rede.

**Architecture:** Lógica nova (motivo de bloqueio, resumos, limites do arrastar, fundo por árvore, confirmação em 2 cliques) vai para `core/` com JUnit. O cliente ganha três peças de UI (`VanillaGui`, `TreeView`, `NodeDetailPanel`) e duas telas (`TalentScreen` reescrita, `ClassScreen` nova, que substitui `ClassSelectScreen` + `ConfirmRespecScreen`). Sprites e texturas são os vanilla; a moldura da janela é desenhada em código.

**Tech Stack:** Java 25, Forge 66.0.9 / MC 26.3, `GuiGraphicsExtractor` (`blitSprite`, `blit`, `fill`, `text`), JUnit 5.

**Spec:** `docs/superpowers/specs/2026-10-05-ui-vanilla-design.md`

## Global Constraints

- Comandos: `.\gradlew.bat` (Windows). Branch: `feat/vanilla-talents`.
- Nenhum pacote de rede novo; o cliente só envia `C2SBuyNode`, `C2SChangeClass`, `C2SConvertXp`; sem atualização otimista.
- `core/` não importa `net.minecraft` nem `net.minecraftforge`.
- Botões do mouse só via `InputConstants.MOUSE_BUTTON_*` (`InputConstantsUsageTest`).
- Toda chave de tradução literal no código existe em `pt_br.json` e `en_us.json` (`LangKeysTest`).
- Sprites vanilla (ids exatos): `advancements/task_frame_unobtained`, `advancements/task_frame_obtained`, `advancements/challenge_frame_unobtained`, `advancements/challenge_frame_obtained`, `advancements/tab_above_left`, `advancements/tab_above_left_selected`, `advancements/tab_above_middle`, `advancements/tab_above_middle_selected`. Moldura de nó 26×26; aba 28×32, passo `(28+4)×índice`.
- Desenho de sprite: `g.blitSprite(RenderPipelines.GUI_TEXTURED, id, x, y, w, h)`; fundo de bloco: `g.blit(RenderPipelines.GUI_TEXTURED, textura, x, y, 0, 0, 16, 16, 16, 16)` em ladrilhos de 16 px.
- Fundos por árvore (textura): common `textures/block/stone.png`, miner `textures/block/deepslate.png`, farmer `textures/block/farmland.png`, explorer `textures/block/grass_block_side.png`, warrior `textures/block/polished_blackstone.png`, archer `textures/block/oak_planks.png`.
- Janela de talentos ~400×225 px de GUI, encolhe até caber em 320×240; painel de detalhes ~110 px de largura. Janela de classes ~300×180.
- Cores do relevo vanilla: fundo `#C6C6C6`, borda clara `#FFFFFF`, borda escura `#555555`, painel afundado `#8B8B8B` com bordas `#373737`/`#FFFFFF`, contorno externo `#000000`.

## Review Focus

1. Nó selecionado que some após `/reload` (ou árvore trocada por respec): a seleção é limpa, nada quebra — `TalentScreen.onSync` revalida (Task 3).
2. Escala de GUI máxima (tela 320×240): a árvore não cabe e passa a ser arrastável, sem sair das bordas — `clampScroll` (Task 1).
3. Segundo clique de confirmação depois do prazo: re-arma em vez de confirmar; nunca envia duas trocas — `TwoStepConfirm` (Task 1).
4. Clique duplo em nó que não pode ser comprado: não envia pacote — `TalentScreen` usa `canPurchase` antes de enviar (Task 3).
5. Tela de classes aberta quando chega um sync (classe mudou): lista e painel se refazem — `ClassScreen.onSync` (Task 4).

---

### Task 1: Modelo da UI no `core/`

**Files:**
- Modify: `src/main/java/com/seunome/vanillatalents/core/TalentScreenModel.java`
- Create: `src/main/java/com/seunome/vanillatalents/core/TwoStepConfirm.java`
- Test: `src/test/java/com/seunome/vanillatalents/core/TalentScreenModelTest.java`, `src/test/java/com/seunome/vanillatalents/core/TwoStepConfirmTest.java`, `src/test/java/com/seunome/vanillatalents/data/DesignDocConsistencyTest.java`

**Interfaces:**
- Produces (em `TalentScreenModel`):
  - `static String blockReasonKey(PurchaseResult r)` → `null` para `OK`; `UNKNOWN_NODE`→`"gui.vanillatalents.reason.unknown"`, `NO_CLASS_SELECTED`→`".no_class"`, `WRONG_CLASS`→`".wrong_class"`, `MAXED`→`".maxed"`, `NOT_ENOUGH_POINTS`→`".no_points"`, `PREREQUISITE_NOT_MET`→`".prerequisite"` (todas com o prefixo `gui.vanillatalents.reason`).
  - `record TreeSummary(int spent, int maxedNodes, int nodeCount, int totalPoints)` + `static TreeSummary treeSummary(SkillView v, TalentRegistry r, TreeCategory tree)` (spent = soma dos níveis efetivos da árvore).
  - `record ClassSummary(String rootId, String capstoneId, int nodeCount, int totalPoints)` + `static ClassSummary classSummary(TalentRegistry r, TreeCategory tree)`: raiz = primeiro nó sem pré-requisitos na ordem de `r.tree(tree)`; capstone = último de `r.tree(tree)`; árvore vazia → ids `null`, contagens 0.
  - `static int clampScroll(int contentSize, int viewSize, int scroll)`: se `contentSize <= viewSize` devolve 0; senão limita a `[viewSize - contentSize, 0]`.
  - `static String backgroundTexture(TreeCategory tree)`: caminho conforme Global Constraints.
- Produces (`TwoStepConfirm`): `TwoStepConfirm(int windowTicks)`, `boolean click(long tick)` (1º clique arma e devolve false; 2º dentro de `windowTicks` devolve true e desarma; depois do prazo re-arma e devolve false), `boolean isArmed(long tick)`, `void reset()`.

- [ ] **Step 1: Testes que falham**
  - `blockReasonKey`: `OK` → `null`; cada um dos 6 outros valores → a chave exata acima.
  - `treeSummary` com `TalentRulesTest.REG`/`FakeView` (`common_health`=5, `common_saturation`=2): `spent == 7`, `maxedNodes == 1`, `nodeCount == 5`, `totalPoints == 17` (5+3+3+5+1).
  - `classSummary` no registro de `TalentRulesTest.REG` para MINER: `rootId == "miner_haste"`, `capstoneId == "miner_darkvision"`, `nodeCount == 2`, `totalPoints == 6`; árvore sem nós (WARRIOR) → `rootId == null`, `nodeCount == 0`.
  - `clampScroll(200, 300, -50) == 0`; `clampScroll(400, 300, -150) == -100`; `clampScroll(400, 300, 20) == 0`; `clampScroll(400, 300, -60) == -60`.
  - `backgroundTexture` devolve as 6 texturas exatas e nenhuma árvore fica sem.
  - `TwoStepConfirmTest` (janela 60): `click(0) == false`, `isArmed(10) == true`, `click(30) == true`, `isArmed(31) == false`; `click(0); click(61) == false` e `isArmed(61) == true`; `reset()` desarma.
  - Em `DesignDocConsistencyTest` (registro real): `classSummary(MINER)` = `miner_haste`, `miner_vein`, 12, 36; `classSummary(ARCHER).totalPoints == 37`; `classSummary(COMMON).capstoneId == "common_second_wind"`.
- [ ] **Step 2:** `.\gradlew.bat test` → FAIL (símbolos inexistentes).
- [ ] **Step 3:** Implementar as interfaces acima.
- [ ] **Step 4:** `.\gradlew.bat test` → PASS (toda a suíte).
- [ ] **Step 5:** Commit `feat(core): modelo da UI (motivos, resumos, arrastar, fundos, confirmação)`.

### Task 2: `VanillaGui` e `TreeView`

**Files:**
- Create: `src/main/java/com/seunome/vanillatalents/client/ui/VanillaGui.java`, `src/main/java/com/seunome/vanillatalents/client/ui/TreeView.java`

**Interfaces:**
- Consumes: `TalentScreenModel.clampScroll`, `backgroundTexture` (Task 1); `TalentRules.nodeState`, `effectiveLevel`; `TalentRegistries.client()`; `ClientTalentState.data()`.
- Produces:
  - `VanillaGui.raisedPanel(GuiGraphicsExtractor g, int x, int y, int w, int h)` (janela em relevo + contorno preto), `VanillaGui.insetPanel(g, x, y, w, h)` (painel afundado), `VanillaGui.tiled(g, Identifier texture, int x, int y, int w, int h, int offsetX, int offsetY)` (ladrilhos de 16 px com scissor), constantes `Identifier` dos sprites das Global Constraints e `VanillaGui.frameSprite(boolean capstone, boolean obtained)`.
  - `TreeView(int x, int y, int w, int h)` com `setTree(TreeCategory tree)` (zera o deslocamento), `render(GuiGraphicsExtractor g, @Nullable String selectedId, int mouseX, int mouseY)`, `@Nullable TalentNode nodeAt(double mouseX, double mouseY)`, `boolean contains(double x, double y)`, `void drag(double dx, double dy)` (aplica `clampScroll` por eixo).
- Comportamento de `render` (spec §1): fundo `tiled` com a textura da árvore + borda escurecida (gradiente para `#00000066`); conexões em L como hoje (branca com contorno preto se atendido, `#555555` se não); nó = `blitSprite` de `frameSprite` 26×26 + `g.item(icon)` em (+5,+5); bloqueado = overlay `#99000000`; disponível = contorno verde `#FF55FF55` de 1 px; progresso/completo = texto `n/max` no canto inferior direito; progresso = barrinha 2 px `#FF55FF55` proporcional; selecionado = contorno `#FFFFFF55` de 2 px. Grade: célula 32×36; conteúdo centralizado quando cabe.

- [ ] **Step 1:** Implementar as duas classes (sem lógica nova além das funções da Task 1; nada aqui é testável sem o jogo).
- [ ] **Step 2:** `.\gradlew.bat build` → `BUILD SUCCESSFUL` (inclui `InputConstantsUsageTest`, `LangKeysTest`).
- [ ] **Step 3:** Commit `feat(client): VanillaGui e TreeView`.

### Task 3: `NodeDetailPanel` e `TalentScreen` reescrita

**Files:**
- Create: `src/main/java/com/seunome/vanillatalents/client/ui/NodeDetailPanel.java`
- Modify: `src/main/java/com/seunome/vanillatalents/client/TalentScreen.java` (reescrita), `src/main/resources/assets/vanillatalents/lang/pt_br.json`, `en_us.json`

**Interfaces:**
- Consumes: Tasks 1–2; `TalentRules.canPurchase`; `TalentScreenModel.unmetPrerequisites`, `blockReasonKey`, `treeSummary`; pacotes `C2SBuyNode`, `C2SConvertXp(boolean)`.
- Produces:
  - `NodeDetailPanel(int x, int y, int w, int h)`: `render(g, @Nullable TalentNode selected, TreeCategory tree)`; `Button buyButton()` (criado pela tela, posicionado no rodapé do painel; `active` = `canPurchase == OK`).
  - `TalentScreen` mantém `onSync()` (chamado por `ClientTalentState.notifyScreen`) e passa a ter `void openTab(boolean classTab)`.
- Comportamento (spec §1): abas acima da janela com `tab_above_left[_selected]` (Comum, ícone escudo `minecraft:shield`) e `tab_above_middle[_selected]` (Classe, ícone do nó raiz da classe; sem classe → `minecraft:compass`); título `gui.vanillatalents.title.tree` = "Talentos — %s"; à direita do título "PT %s · Nível %s"; rodapé com barra de XP (`player.experienceProgress`), "Converter", "Converter tudo (N PT)", "Trocar classe" (só aba Classe com classe); passar o mouse num nó = tooltip só com o nome; clique esquerdo = seleciona (`TreeView.nodeAt`), e a seleção continua depois da compra; `doubleClick` em nó com `canPurchase == OK` envia `C2SBuyNode`; arrastar com o esquerdo dentro do `TreeView` chama `drag` (via `mouseDragged`); `onSync` limpa a seleção se o nó sumiu do registro ou da árvore visível; aba Classe sem classe mostra "Nenhuma classe escolhida" + botão "Escolher classe" (abre `ClassScreen`, Task 4 — até lá mantém `ClassSelectScreen`).
- Chaves novas (pt_br; en_us traduzido): `gui.vanillatalents.title.tree` "Talentos — %s"; `gui.vanillatalents.status` "PT %s · Nível %s"; `gui.vanillatalents.reason.unknown` "Nó desconhecido", `.no_class` "Escolha uma classe", `.wrong_class` "Outra classe", `.maxed` "Nível máximo", `.no_points` "Sem PT", `.prerequisite` "Requisito faltando"; `gui.vanillatalents.detail.requirements` "Requisitos:"; `gui.vanillatalents.detail.met` "✔ %s %s/%s"; `gui.vanillatalents.detail.unmet` "✘ %s %s/%s"; `gui.vanillatalents.detail.cost` "Custo: 1 PT"; `gui.vanillatalents.buy` "Comprar (1 PT)"; `gui.vanillatalents.summary.spent` "PT gastos: %s"; `gui.vanillatalents.summary.nodes` "Nós completos: %s/%s"; `gui.vanillatalents.summary.total` "PT para completar: %s"; `gui.vanillatalents.summary.hint` "Clique num nó para ver os detalhes. Clique duplo compra.".

- [ ] **Step 1:** Adicionar as chaves e rodar `.\gradlew.bat test --tests '*LangKeysTest'` depois de escrever o código → deve passar só quando todas existirem (RED antes de adicioná-las).
- [ ] **Step 2:** Implementar `NodeDetailPanel` e reescrever `TalentScreen`.
- [ ] **Step 3:** `.\gradlew.bat build` → `BUILD SUCCESSFUL`.
- [ ] **Step 4:** Commit `feat(client): tela de talentos no estilo Conquistas`.

### Task 4: `ClassScreen` (substitui escolha + confirmação)

**Files:**
- Create: `src/main/java/com/seunome/vanillatalents/client/ClassScreen.java`
- Delete: `src/main/java/com/seunome/vanillatalents/client/ClassSelectScreen.java`, `src/main/java/com/seunome/vanillatalents/client/ConfirmRespecScreen.java`
- Modify: `TalentScreen.java` (abre `ClassScreen`), `ClientTalentState.java` (`notifyScreen` também chama `ClassScreen.onSync`), `LangKeysTest.java` (inclui `vanillatalents.class.<id>.desc`), `pt_br.json`, `en_us.json`

**Interfaces:**
- Consumes: Task 1 (`classSummary`, `TwoStepConfirm`, `respecPreview`), `VanillaGui`, `ClientTalentState.economy()`, `C2SChangeClass`.
- Produces: `ClassScreen(Screen parent)`, `void onSync()`.
- Comportamento (spec §2): lista de 5 classes (ícone = ícone do nó raiz, via `classSummary`); atual = "%s (atual)" e não selecionável; painel com nome, `vanillatalents.class.<id>.desc`, raiz e capstone (ícone + nome traduzido), "%s nós · %s PT para completar"; sem classe: "A primeira escolha é gratuita" + "Escolher %s" (envia e volta à aba Classe); com classe: prévia (taxa, PT zerados, PT devolvidos, Comum não afetada) + botão vermelho "Trocar para %s (−%s níveis)" ou "Trocar para %s (gratuito)" se taxa 0; `TwoStepConfirm` com janela de 60 ticks (texto "Clique de novo para confirmar" enquanto armado); sem níveis para a taxa: botão desabilitado + `gui.vanillatalents.respec.not_enough` em vermelho; ao confirmar envia `C2SChangeClass` e volta à `TalentScreen` com `openTab(true)`; trocar a seleção chama `reset()`.
- Chaves novas: `gui.vanillatalents.classes.title` "Classes"; `.current` "%s (atual)"; `.root` "Raiz: %s"; `.capstone` "Capstone: %s"; `.size` "%s nós · %s PT para completar"; `.choose` "Escolher %s"; `.change` "Trocar para %s (−%s níveis)"; `.change_free` "Trocar para %s (gratuito)"; `.confirm` "Clique de novo para confirmar"; `.fee` "Taxa: %s níveis"; `.reset` "PT gastos que serão zerados: %s"; `.refund` "PT devolvidos: %s"; `.common_safe` "A Árvore Comum não é afetada."; `gui.vanillatalents.back` "Voltar"; descrições `vanillatalents.class.miner.desc` "Subterrâneo e extração: quebra mais rápida, minérios extras e visão no escuro.", `farmer.desc` "Agricultura e pecuária: colheita em área, drops extras de animais e uma aura que acelera plantações.", `explorer.desc` "Exploração e mobilidade: mais velocidade, menos dano de queda e menos fome ao correr.", `warrior.desc` "Combate corpo a corpo: mais dano com espadas e machados, repulsão e resistência.", `archer.desc` "Combate à distância: puxada e recarga mais rápidas, dano extra em projéteis e flechas poupadas.".

- [ ] **Step 1:** Em `LangKeysTest.keysInCode`, adicionar `vanillatalents.class.<id>.desc` para as 5 classes → `.\gradlew.bat test --tests '*LangKeysTest'` FAIL.
- [ ] **Step 2:** Adicionar as chaves; implementar `ClassScreen`; remover as duas telas antigas e seus usos.
- [ ] **Step 3:** `.\gradlew.bat build` → `BUILD SUCCESSFUL`.
- [ ] **Step 4:** Commit `feat(client): tela de classes com detalhes e confirmação em 2 cliques`.

### Task 5: Verificação no jogo e registro

**Files:**
- Modify: `docs/superpowers/plans/2026-10-05-vanilla-talents-registro.md`

- [ ] **Step 1:** `.\gradlew.bat runClient` até a tela de título; o log não tem `Failed to register`, `Crash report` nem exceção com `vanillatalents`. Fechar o cliente.
- [ ] **Step 2:** Registrar no arquivo de registro as decisões tomadas e o checklist manual para o José: abas e fundos temáticos nas 6 árvores; molduras por estado (bloqueado, disponível, progresso, completo, capstone); seleção + painel + motivo do bloqueio; clique duplo compra; Converter / Converter tudo; tela de classes sem classe e com classe; confirmação em 2 cliques e expiração; escala de GUI máxima com arrastar.
- [ ] **Step 3:** Commit `docs: registro da UI vanilla`.
