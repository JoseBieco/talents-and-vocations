# Vanilla+ Talents — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar o sistema completo de talentos do mod (conversão de XP em PT, Árvore Comum + 5 classes com 72 nós, respec, GUI e todos os efeitos) em Forge 66 / Minecraft 26.3.

**Architecture:** Um núcleo em Java puro (`core/`, sem imports de `net.minecraft`) concentra modelo, validação, regras de compra/respec/custo e todas as fórmulas numéricas, testado com JUnit. Em volta dele ficam camadas finas de Forge: capability (persistência), reload listener de JSON (definições data-driven), rede (SimpleChannel), GUI (Screen) e handlers de evento por árvore, que só leem o nível do nó, chamam a fórmula do `core` e aplicam o resultado. Os nós que o Forge não expõe por evento usam um conjunto pequeno e fechado de Mixins.

**Tech Stack:** Java 25, Forge 66.0.9 (MC 26.3), EventBus 7 (`net.minecraftforge.eventbus.api.listener.SubscribeEvent`, `XxxEvent.BUS`), Mixin (somente os alvos listados na Task 9), JUnit 5, Gradle 9 (wrapper do projeto).

**Spec:** `gdd_vanilla_talents.md`, `plano_de_a_o_implementa_o.md` e `docs/arvores/*.md` (README + 6 árvores). Em caso de conflito, `docs/arvores/` vence o plano de ação (é mais recente e detalha os valores).

---

## Decisões assumidas (configuráveis)

O design deixou pontos em aberto. Para não travar a implementação, cada um vira opção do `Config` com o default abaixo. **O José deve revisar estes defaults antes da Task 17.**

| Ponto em aberto | Default implementado | Chave de config |
|---|---|---|
| Modelo de custo do PT (`README.md`, risco transversal) | `LEVELS`, 5 níveis (como no GDD). Alternativa `POINTS` com 100 pontos de XP. | `costMode`, `costLevels`, `costPoints` |
| Taxa de respec (GDD §4 não define valor) | 10 níveis; a **primeira** escolha de classe é gratuita | `respecFeeLevels` |
| Retorno do respec | 25%, arredondado **para baixo** | `respecRefundPercent` |
| `miner_fortune` em minérios de ferro/ouro/cobre | Sim (vale para toda a tag `c:ores`) | — |
| Dano do Guerreiro | +0,5 por ponto (valor em JSON) | — |
| Bônus de atributo (GDD §5.3 cita `EntityAttributeModificationEvent`) | Esse evento registra atributos por **tipo** de entidade; bônus por jogador usam `AttributeModifier` na instância do jogador (`AttributeSync`, Task 9) | — |
| Varredura (`warrior_sweep`) | Reinterpretado como +0,15 no atributo `SWEEPING_DAMAGE_RATIO` por nível (o vanilla não separa o dano de varredura num evento). **Atualizar `04_guerreiro.md`** na Task 15. | — |

## Como executar

Entregar ao Claude Code, a partir da raiz do projeto:

> Leia `docs/superpowers/plans/2026-10-05-vanilla-talents-implementacao.md` e as specs que ele cita. Execute o plano task por task com `superpowers:subagent-driven-development`, na ordem. Pare ao final de cada **Fase** e me mostre o resultado dos testes e do checklist manual antes de seguir. Não altere valores de balanceamento fora dos JSON.

As Fases 0–6 entregam um mod jogável (MVP). As Fases 7–8 podem ser feitas uma árvore por sessão.

## Global Constraints

- Comandos escritos como `./gradlew`; no Windows (ambiente do José) usar `.\gradlew.bat`.

- MOD_ID `vanillatalents`; pacote `com.seunome.vanillatalents`; Minecraft `26.3`, Forge `66.0.9`, Java 25.
- Conversão: **1 PT = 5 níveis de XP** (default; ver tabela acima). Sem level cap; tudo é 100% completável.
- Regra de pré-requisito: nível exigido ≥ `ceil(maxLevel_do_pré-requisito / 2)` e ≤ `maxLevel`. Pré-requisitos múltiplos são E lógico.
- Apenas uma classe ativa. Classes válidas: `miner`, `farmer`, `explorer`, `warrior`, `archer`; sem classe = `"none"`.
- **Todo ID da Árvore Comum começa com `common_`**; todo ID de classe começa com `<classId>_`. O respec apaga só nós que não começam com `common_`.
- Respec zera a árvore da classe e devolve `floor(gastos × 25%)` PT. A Árvore Comum nunca é afetada.
- O servidor é a única autoridade: o cliente só envia intenções (pacotes C2S) e nunca altera XP, pontos ou níveis localmente.
- Árvores **não** ficam no código: nós em `src/main/resources/data/vanillatalents/skills/<arvore>/<id>.json`. Números de balanceamento ficam no campo `values` do JSON, nunca como constante em handler.
- Efeitos multiplicativos com encantamentos vanilla, salvo exceções explícitas (Arqueiro: bônus de dano do mod **aditivos entre si**). Tetos: redução de queda do mod ≤ 60%; recarga da besta ≥ 8 ticks.
- Sem habilidades ativas e sem tecla além da que abre a GUI (padrão `K`).
- `core/` não pode importar nada de `net.minecraft` nem de `net.minecraftforge` (é o que permite testar com JUnit).
- Textos visíveis via chaves de tradução `talent.vanillatalents.<id>.name` / `.desc`, com `pt_br.json` e `en_us.json`.

## Review Focus

1. **Morte, respawn e troca de dimensão:** o jogador mantém pontos, níveis e bônus de atributo; a vida máxima extra não some nem é aplicada duas vezes (modificadores com `Identifier` fixo por nó, operação de substituir, nunca somar). Coberto na Task 9 (`AttributeSync` idempotente) e no checklist da Task 18.
2. **Datapack alterado depois que o jogador já investiu** (nó removido ou `maxLevel` reduzido): nada quebra; níveis de IDs desconhecidos ficam no NBT, mas não têm efeito, e o nível efetivo é limitado ao novo máximo. Teste `effectiveLevel_*` na Task 2.
3. **Pacotes inválidos ou dessincronizados** (comprar nó de outra classe, sem pontos, com pré-requisito faltando, converter com menos de 5 níveis, classe inexistente): o servidor rejeita sem alterar estado e reenvia o sync. Testes de `canPurchase` / `canAfford` / `validateChange` nas Tasks 2 e 3.
4. **Efeitos que disparam o próprio evento** (Veio, Foice Larga, Golpe Amplo, Colheita Perpétua) não podem entrar em loop nem aplicar bônus em cascata. `RecursionGuard` com teste na Task 9.
5. **Respec degenerado** (escolher a mesma classe, 0 pontos gastos, sem níveis para a taxa): nenhuma mudança de estado e nenhuma cobrança. Teste `validateChange_*` na Task 3.

---

## Estado atual do código (lido em 2026-10-05)

O projeto **não compila** em Forge 66 / MC 26.3. Problemas confirmados nas fontes do Forge em `.gradle/mavenizer/.../forge-26.3-66.0.9-sources.jar`:

| Arquivo | Problema |
|---|---|
| `event/ModEvents.java`, `event/PlayerEvents.java` | Usam `net.minecraftforge.eventbus.api.SubscribeEvent` (EventBus 6). O correto é `net.minecraftforge.eventbus.api.listener.SubscribeEvent`. |
| `event/ModEvents.java` | `RegisterCapabilitiesEvent` não existe mais; usar `@AutoRegisterCapability` em `PlayerSkillData`. |
| `event/PlayerEvents.java` | `ResourceLocation` foi renomeado para `net.minecraft.resources.Identifier`. `AttachCapabilitiesEvent<Entity>` virou `AttachCapabilitiesEvent.Entities`. `PlayerEvent.Clone` é um `record` (`isWasDeath()`, `getOriginal()`, `getEntity()`). |
| `capability/PlayerSkillData.java`, `PlayerSkillProvider.java` | `INBTSerializable` agora é `serializeNBT(HolderLookup.Provider)` / `deserializeNBT(HolderLookup.Provider, T)`. `CompoundTag.getString/getInt/getCompound` retornam `Optional`; usar `getStringOr`, `getIntOr`, `getCompoundOrEmpty`. |
| `VanillaTalents.java`, `Config.java` | Ainda contêm o código de exemplo do MDK (bloco, item, aba criativa, "magic number"). |
| raiz do projeto | Não é um repositório git; os passos de commit exigem `git init` (Task 0). |

APIs do Forge 66 confirmadas e usadas no plano: `PlayerEvent.BreakSpeed`, `BlockEvent.BreakEvent` (com XP), `BlockEvent.FarmlandTrampleEvent`, `LivingHurtEvent`, `LivingDamageEvent`, `LivingFallEvent`, `LivingDropsEvent`, `LivingDeathEvent`, `LivingEntityUseItemEvent.Tick/Finish`, `LivingEquipmentChangeEvent`, `LivingEvent.LivingJumpEvent`, `MobEffectEvent.Added`, `BabyEntitySpawnEvent`, `BonemealEvent`, `CriticalHitEvent`, `AttackEntityEvent`, `ArrowLooseEvent`, `ProjectileImpactEvent`, `EntityJoinLevelEvent`, `EntityMountEvent`, `TickEvent.PlayerTickEvent.Post`, `PlayerEvent.PlayerLoggedInEvent/PlayerRespawnEvent/PlayerChangedDimensionEvent/Clone`, `OnDatapackSyncEvent`, `AddReloadListenerEvent`, `RegisterKeyMappingsEvent`, `MovementInputUpdateEvent` (cliente), `IGlobalLootModifier`, `ChannelBuilder`/`SimpleChannel`/`PacketDistributor`, `Tags.Blocks.ORES` (`c:ores`), `ForgeMod.SWIM_SPEED`, `SimpleJsonResourceReloadListener(Codec, FileToIdConverter)`.

Atributos vanilla usados: `MAX_HEALTH`, `ARMOR`, `OXYGEN_BONUS`, `SUBMERGED_MINING_SPEED`, `BURNING_TIME`, `MOVEMENT_SPEED`, `STEP_HEIGHT`, `MOVEMENT_EFFICIENCY`, `SAFE_FALL_DISTANCE`, `ATTACK_SPEED`, `ATTACK_KNOCKBACK`, `KNOCKBACK_RESISTANCE`, `SWEEPING_DAMAGE_RATIO`.

Ponto sem evento no Forge: exaustão de fome (regeneração, corrida, pulo), consumo de durabilidade e duração do escudo desativado. Esses usam Mixin (Task 9).

---

## Estrutura de arquivos

```
src/main/java/com/seunome/vanillatalents/
  VanillaTalents.java            # bootstrap: registra config, rede, GLM; sem código de exemplo
  Config.java                    # opções da tabela "Decisões assumidas" + tetos
  core/                          # Java puro, testado com JUnit
    TreeCategory.java            # enum common/miner/farmer/explorer/warrior/archer
    Prerequisite.java  GridPos.java  TalentNode.java
    TalentRegistry.java          # coleção imutável + validação
    SkillView.java               # leitura do estado do jogador
    PurchaseResult.java  NodeState.java  TalentRules.java
    CostMode.java  XpCostRules.java  RespecRules.java  RespecCheck.java
    RecursionGuard.java
    formula/ CommonFormulas, MinerFormulas, FarmerFormulas,
             ExplorerFormulas, WarriorFormulas, ArcherFormulas
  capability/  PlayerSkillData.java  PlayerSkillProvider.java  SkillAccess.java
  data/        TalentNodeCodec.java  TalentDataLoader.java  TalentRegistries.java
  network/     ModNetwork.java  C2SConvertXp  C2SBuyNode  C2SChangeClass
               S2CSyncPlayer  S2CSyncDefinitions  S2CProspectorHighlight
  server/      TalentActions.java      # lógica de servidor dos 3 pacotes C2S
  effect/      AttributeSync.java  Cooldowns (dentro de PlayerSkillData)
               CommonEffects  MinerEffects  FarmerEffects
               ExplorerEffects  WarriorEffects  ArcherEffects
               loot/ TalentLootModifier.java  (GLM único com "kind")
               hooks/ DurabilityHooks  ExhaustionHooks  ShieldHooks
  mixin/       FoodDataMixin  ServerPlayerMixin  ItemStackMixin  PlayerMixin
  client/      KeyBindings  ClientTalentState  TalentScreen  ClassSelectScreen
               ConfirmRespecScreen  ClientEffects (MovementInputUpdate, partículas)
src/main/resources/
  META-INF/mods.toml  vanillatalents.mixins.json
  data/vanillatalents/skills/{common,miner,farmer,explorer,warrior,archer}/*.json
  data/vanillatalents/tags/block/dense_stone.json
  data/vanillatalents/tags/entity_type/livestock.json
  data/vanillatalents/loot_modifiers/*.json  + data/forge/loot_modifiers/global_loot_modifiers.json
  assets/vanillatalents/lang/{pt_br,en_us}.json
  assets/vanillatalents/textures/gui/talent_screen.png (opcional; pode usar retângulos)
src/test/java/com/seunome/vanillatalents/core/...   # JUnit
```

### Esquema JSON de um nó

Campos do GDD §5.5 mais três adições necessárias (`position`, `values`, `prerequisites` com nível):

```json
{
  "id": "common_health",
  "treeCategory": "common",
  "name": "talent.vanillatalents.common_health.name",
  "description": "talent.vanillatalents.common_health.desc",
  "icon": "minecraft:golden_apple",
  "maxLevel": 5,
  "prerequisites": [],
  "position": { "x": 0, "y": 0 },
  "values": { "per_level": 2.0 }
}
```

`prerequisites` = lista de `{ "id": "...", "level": N }`. `position` = célula da grade da GUI (raiz em y=0, ramos em x = −1/0/+1, capstone embaixo). `values` = chaves lidas pelo handler; os valores vêm das tabelas em `docs/arvores/`.

---
## Fase 0 — Base que compila

### Task 0: Limpeza, compilação e infraestrutura de testes

**Files:**
- Modify: `VanillaTalents.java`, `Config.java`, `event/ModEvents.java` (apagar), `event/PlayerEvents.java`, `capability/PlayerSkillData.java`, `capability/PlayerSkillProvider.java`, `build.gradle`
- Create: `src/test/java/com/seunome/vanillatalents/SmokeTest.java`

**Interfaces:**
- Produces: projeto que roda `gradlew build` sem erros; `PlayerSkillProvider.PLAYER_SKILL` funcional; JUnit configurado.

- [ ] **Step 1:** `git init`, commit inicial do estado atual (o `.gitignore` já existe; confirme que `run/`, `run-data/`, `build/` e `.gradle/` estão ignorados).
- [ ] **Step 2:** Remover de `VanillaTalents.java` e `Config.java` todo o código de exemplo do MDK (bloco, item, aba criativa, `logDirtBlock`, `magicNumber`, `items`). `Config` fica vazio por enquanto (só o `SPEC`).
- [ ] **Step 3:** Corrigir as APIs da tabela "Estado atual do código". `ModEvents.java` é apagado; `PlayerSkillData` ganha `@AutoRegisterCapability`; `PlayerEvents` passa a usar `AttachCapabilitiesEvent.Entities` e `Identifier.fromNamespaceAndPath(MODID, "skill_data")`.
- [ ] **Step 4:** Em `build.gradle`, adicionar `testImplementation 'org.junit.jupiter:junit-jupiter:5.11.4'`, `testRuntimeOnly 'org.junit.platform:junit-platform-launcher'` e `test { useJUnitPlatform() }`.
- [ ] **Step 5:** `SmokeTest.trueIsTrue()` com `assertTrue(true)`.
- [ ] **Step 6:** Run: `./gradlew build` → Expected: `BUILD SUCCESSFUL`, 1 teste executado.
- [ ] **Step 7:** Run: `./gradlew runServer` (aceitar a EULA em `run/eula.txt` se pedido) → Expected: servidor sobe até `Done (`, sem exceções com `vanillatalents` no stack trace. Parar com `stop`.
- [ ] **Step 8:** Commit: `chore: limpa código de exemplo e migra para APIs do Forge 66`.

---

## Fase 1 — Núcleo puro (`core/`)

### Task 1: Modelo de nós e validação do registro

**Files:**
- Create: `core/TreeCategory.java`, `core/Prerequisite.java`, `core/GridPos.java`, `core/TalentNode.java`, `core/TalentRegistry.java`
- Test: `src/test/java/.../core/TalentRegistryTest.java`

**Interfaces:**
- Produces:
  - `enum TreeCategory { COMMON, MINER, FARMER, EXPLORER, WARRIOR, ARCHER }` com `String id()` (`"common"`, `"miner"`…), `String idPrefix()` (`id() + "_"`), `boolean isClass()` (false só para COMMON), `static Optional<TreeCategory> byId(String)`.
  - `record Prerequisite(String nodeId, int level)`
  - `record GridPos(int x, int y)`
  - `record TalentNode(String id, TreeCategory tree, String nameKey, String descKey, String icon, int maxLevel, List<Prerequisite> prerequisites, GridPos position, Map<String, Double> values)` com `double value(String key)` (lança `IllegalStateException` com o id do nó e a chave quando ausente).
  - `final class TalentRegistry`: `static TalentRegistry build(Collection<TalentNode> nodes, List<String> errorsOut)`, `static TalentRegistry empty()`, `Optional<TalentNode> get(String id)`, `List<TalentNode> tree(TreeCategory)` (ordenada por `position.y`, depois `x`), `Collection<TalentNode> all()`, `int size()`.

Regras de `build` (cada violação adiciona uma mensagem em `errorsOut` e **descarta o nó**; descartes são reavaliados até estabilizar, para que dependentes de um nó descartado também caiam):
1. `id` começa com `tree.idPrefix()`.
2. `1 ≤ maxLevel ≤ 10`.
3. Pré-requisito existe e é da mesma árvore.
4. `ceil(pre.maxLevel / 2) ≤ level ≤ pre.maxLevel`.
5. Sem ciclos.
6. `position` única dentro da árvore.

- [ ] **Step 1: Testes** (nós construídos à mão no próprio teste):
  - `build_acceptsValidMvpTree`: `common_health`(5), `common_saturation`(3, req `common_health` 3) → `size()==2`, `errors` vazio.
  - `build_rejectsWrongPrefix`: `miner_x` com tree COMMON → descartado; mensagem contém `"miner_x"`.
  - `build_rejectsPrereqBelowHalf`: req `common_health` nível 2 (max 5 → mínimo 3) → descartado.
  - `build_rejectsPrereqAboveMax`: req nível 6 → descartado.
  - `build_rejectsUnknownAndCrossTreePrereq`: req para id inexistente e para nó de `miner` → ambos descartados.
  - `build_cascadesDiscard`: A inválido, B depende de A, C depende de B → os três descartados.
  - `build_rejectsCycle`: A→B→A → ambos descartados.
  - `build_rejectsDuplicatePosition`.
  - `value_missingKeyThrowsWithNodeId`.
- [ ] **Step 2:** `./gradlew test --tests '*TalentRegistryTest'` → FAIL (classes inexistentes).
- [ ] **Step 3:** Implementar as classes acima.
- [ ] **Step 4:** `./gradlew test --tests '*TalentRegistryTest'` → PASS.
- [ ] **Step 5:** Commit `feat(core): modelo de nós e validação do registro`.

### Task 2: Regras de compra e estado visual do nó

**Files:**
- Create: `core/SkillView.java`, `core/PurchaseResult.java`, `core/NodeState.java`, `core/TalentRules.java`
- Test: `core/TalentRulesTest.java`

**Interfaces:**
- Consumes: `TalentNode`, `TalentRegistry`, `TreeCategory` (Task 1).
- Produces:
  - `interface SkillView { String currentClass(); int availablePoints(); int rawLevel(String nodeId); }`
  - `enum PurchaseResult { OK, UNKNOWN_NODE, NO_CLASS_SELECTED, WRONG_CLASS, MAXED, NOT_ENOUGH_POINTS, PREREQUISITE_NOT_MET }`
  - `enum NodeState { LOCKED, AVAILABLE, IN_PROGRESS, MAXED }`
  - `final class TalentRules`:
    - `static int minRequiredLevel(int maxLevel)` = `ceil(maxLevel/2.0)`
    - `static int effectiveLevel(SkillView v, TalentRegistry r, String id)`: 0 se o id não existir no registro; `min(rawLevel, maxLevel)`; 0 se o nó for de classe diferente de `currentClass`.
    - `static PurchaseResult canPurchase(SkillView v, TalentRegistry r, String id)`. Checagem **nesta ordem**: UNKNOWN_NODE → (nó de classe e `currentClass=="none"`) NO_CLASS_SELECTED → (nó de classe ≠ atual) WRONG_CLASS → MAXED → NOT_ENOUGH_POINTS (custo é sempre 1 PT por nível) → PREREQUISITE_NOT_MET.
    - `static NodeState nodeState(SkillView v, TalentRegistry r, TalentNode n)`: MAXED se nível = max; IN_PROGRESS se 0 < nível < max; AVAILABLE se 0 e pré-requisitos atendidos (ignora pontos); senão LOCKED.

- [ ] **Step 1: Testes** (um `SkillView` fake em memória):
  - `canPurchase_okWhenRootAndHasPoint`
  - `canPurchase_noClassForClassNode` (`miner_haste` com classe `"none"`)
  - `canPurchase_wrongClass` (`archer_aim` com classe `miner`)
  - `canPurchase_commonNodeIgnoresClass` (`common_health` com classe `"none"` → OK)
  - `canPurchase_maxedBeforePoints` (nível 5/5 e 0 pontos → MAXED)
  - `canPurchase_prereqNeedsHalf`: `common_saturation` com `common_health`=2 → PREREQUISITE_NOT_MET; =3 → OK
  - `canPurchase_capstoneNeedsAllPrereqs`: `common_second_wind` com só 2 dos 3 requisitos → PREREQUISITE_NOT_MET
  - `effectiveLevel_unknownNodeIsZero`, `effectiveLevel_clampedToNewMax` (raw 5, max 3 → 3), `effectiveLevel_otherClassIsZero`
  - `nodeState_*` para os quatro estados
- [ ] **Step 2:** rodar → FAIL. **Step 3:** implementar. **Step 4:** rodar → PASS.
- [ ] **Step 5:** Commit `feat(core): regras de compra e estado de nó`.

### Task 3: Custo de XP e respec

**Files:**
- Create: `core/CostMode.java`, `core/XpCostRules.java`, `core/RespecRules.java`, `core/RespecCheck.java`
- Test: `core/XpCostRulesTest.java`, `core/RespecRulesTest.java`

**Interfaces:**
- Produces:
  - `enum CostMode { LEVELS, POINTS }`
  - `XpCostRules.totalXpForLevel(int level) -> int` (fórmula vanilla: L²+6L até 16; 2,5L²−40,5L+360 de 17 a 31; 4,5L²−162,5L+2220 a partir de 32)
  - `XpCostRules.canAfford(CostMode mode, int playerLevel, int playerTotalXp, int amount) -> boolean` (`LEVELS`: `playerLevel >= amount`; `POINTS`: `playerTotalXp >= amount`)
  - `RespecRules.spentClassPoints(Map<String,Integer> levels, String classId) -> int` (soma dos níveis com prefixo `classId + "_"`)
  - `RespecRules.refund(int spent, int percent) -> int` = `floor(spent * percent / 100)`
  - `enum RespecCheck { OK_FIRST_CHOICE, OK_PAID, INVALID_CLASS, SAME_CLASS, NOT_ENOUGH_LEVELS }`
  - `RespecRules.validateChange(String currentClass, String newClass, int playerLevel, int feeLevels) -> RespecCheck` (`newClass` deve ser `isClass()`; `currentClass=="none"` → OK_FIRST_CHOICE, sem taxa)

- [ ] **Step 1: Testes:**
  - `totalXpForLevel`: 5→55, 15→315, 30→1395, 35→2045 (diferenças conferem com o README: 0→5=55, 10→15=155, 25→30=485, 30→35=650).
  - `canAfford_levels`: (LEVELS, 4, …, 5) → false; (LEVELS, 5, …, 5) → true. `canAfford_points`: (POINTS, 0, 99, 100) → false.
  - `refund`: (36, 25) → 9; (35, 25) → 8; (0, 25) → 0.
  - `spentClassPoints_ignoresCommonAndOtherClasses`.
  - `validateChange`: none→miner → OK_FIRST_CHOICE; miner→miner → SAME_CLASS; miner→"wizard" → INVALID_CLASS; miner→"common" → INVALID_CLASS; miner→archer com nível 9 e taxa 10 → NOT_ENOUGH_LEVELS; com nível 10 → OK_PAID.
- [ ] **Step 2–4:** FAIL → implementar → PASS.
- [ ] **Step 5:** Commit `feat(core): custo de XP e regras de respec`.

---

## Fase 2 — Persistência

### Task 4: Capability completa e eventos de ciclo de vida

**Files:**
- Modify: `capability/PlayerSkillData.java`, `capability/PlayerSkillProvider.java`, `event/PlayerEvents.java`
- Create: `capability/SkillAccess.java`
- Test: `src/test/java/.../capability/PlayerSkillDataTest.java` (somente se `CompoundTag` puder ser instanciado em teste sem bootstrap do jogo; se não puder, registrar isso no commit e cobrir pelo checklist da Task 18)

**Interfaces:**
- Consumes: `SkillView` (Task 2).
- Produces:
  - `PlayerSkillData implements INBTSerializable<CompoundTag>, SkillView`, mantendo `getCurrentClass/setCurrentClass/getAvailablePoints/addPoints/removePoints/getNodeLevel/upgradeNode/resetTree(boolean)/getUnlockedNodes` e adicionando:
    - `long getCooldownUntil(String key)` / `void setCooldownUntil(String key, long gameTime)` (persistido em `"Cooldowns"`; usado por Segundo Fôlego, Sentido Térmico, Sede de Batalha, Faro Mineral)
    - `void copyFrom(PlayerSkillData other)`
  - NBT: chaves existentes `CurrentClass`, `AvailablePoints`, `UnlockedNodes` + `Cooldowns`; leitura com `getStringOr("CurrentClass","none")` etc.
  - `SkillAccess.get(Player) -> Optional<PlayerSkillData>`
- Comportamento:
  - `PlayerEvent.Clone`: copia **sempre** (morte e volta do End).
  - `PlayerLoggedInEvent`, `PlayerRespawnEvent`, `PlayerChangedDimensionEvent`: chamar `AttributeSync.apply(player)` (criado na Task 9) e enviar `S2CSyncPlayer` (criado na Task 7). Essas duas chamadas são adicionadas nas respectivas tasks; nesta task os handlers só garantem cópia e carga dos dados.

- [ ] **Step 1:** Teste `roundTrip_preservesAllFields` (classe, pontos, 3 nós, 1 cooldown) e `deserialize_emptyTagGivesDefaults` (classe `"none"`, 0 pontos).
- [ ] **Step 2–4:** FAIL → implementar → PASS.
- [ ] **Step 5:** `./gradlew runServer` → sobe sem erro.
- [ ] **Step 6:** Commit `feat: persistência completa do PlayerSkillData`.

---

## Fase 3 — Definições data-driven

### Task 5: Loader de JSON e sincronização de definições

**Files:**
- Create: `data/TalentNodeCodec.java`, `data/TalentDataLoader.java`, `data/TalentRegistries.java`

**Interfaces:**
- Consumes: `TalentNode`, `TalentRegistry.build` (Task 1).
- Produces:
  - `TalentNodeCodec.CODEC : Codec<TalentNode>` (esquema da seção "Esquema JSON"; `prerequisites` e `values` opcionais, default vazio).
  - `TalentDataLoader extends SimpleJsonResourceReloadListener<TalentNode>` com `FileToIdConverter.json("skills")`, registrado em `AddReloadListenerEvent`. Em `apply`: `TalentRegistry.build(...)`, cada erro no log como `WARN [vanillatalents] Nó descartado: <msg>`, e um `INFO Loaded N talent nodes (M discarded)`.
  - `TalentRegistries.server()` / `setServer(TalentRegistry)` e `TalentRegistries.client()` / `setClient(TalentRegistry)`. Ambos começam com `TalentRegistry.empty()`.
- Comportamento: definições são enviadas ao cliente (`S2CSyncDefinitions`, Task 7) em `OnDatapackSyncEvent` (login e `/reload`).

- [ ] **Step 1:** Criar 2 JSON de teste temporários (`common_health`, `common_saturation`) e rodar `./gradlew runServer` → log `Loaded 2 talent nodes (0 discarded)`.
- [ ] **Step 2:** Quebrar um JSON de propósito (pré-requisito nível 1) → log `WARN ... common_saturation` e `Loaded 1 talent nodes (1 discarded)`. Desfazer.
- [ ] **Step 3:** Commit `feat(data): loader de nós via datapack`.

### Task 6: JSON das 6 árvores, tags e traduções

**Files:**
- Create: 72 arquivos em `data/vanillatalents/skills/<arvore>/`, `tags/block/dense_stone.json`, `tags/entity_type/livestock.json`, `assets/vanillatalents/lang/pt_br.json` e `en_us.json`
- Create: `src/test/java/.../data/DesignDocConsistencyTest.java`

**Interfaces:**
- Produces: 72 nós válidos cujos IDs, `maxLevel` e pré-requisitos são **exatamente** os das tabelas de `docs/arvores/0X_*.md`. `values` por nó conforme o Apêndice A.
- Tags: `dense_stone` = deepslate e variantes (`#minecraft:deepslate_ore_replaceables` + deepslate polido/lajotas/tijolos), `tuff`, `basalt`, `smooth_basalt`, `polished_basalt`, `blackstone`. `livestock` = cow, mooshroom, pig, sheep, chicken, rabbit, goat.
- `position`: raiz em (0,0); ramos esquerdo/central/direito em x = −2/0/+2 (ramos com 2 colunas usam x±1); profundidade = y; capstone em (0, maior y + 1).
- Traduções: `name` = nome em português da tabela; `desc` = coluna "Efeito por nível". `en_us` traduzido.

- [ ] **Step 1: Teste `DesignDocConsistencyTest`**: lê os JSON de `src/main/resources/data/vanillatalents/skills/**` com Gson (sem Minecraft), monta via `TalentRegistry.build` e verifica: 0 erros; 12 nós por árvore; soma de `maxLevel` por árvore = 37/36/35/34/35/37; toda chave de tradução existe em `pt_br.json` e `en_us.json`.
- [ ] **Step 2:** rodar → FAIL. **Step 3:** criar os arquivos. **Step 4:** rodar → PASS.
- [ ] **Step 5:** `./gradlew runServer` → `Loaded 72 talent nodes (0 discarded)`.
- [ ] **Step 6:** Commit `feat(data): árvores Comum e 5 classes em JSON`.

---

## Fase 4 — Rede e ações de servidor

### Task 7: Canal, pacotes e TalentActions

**Files:**
- Create: `network/ModNetwork.java`, os 6 pacotes, `server/TalentActions.java`
- Modify: `Config.java` (chaves `costMode`, `costLevels`, `costPoints`, `respecFeeLevels`, `respecRefundPercent`), `event/PlayerEvents.java`

**Interfaces:**
- Consumes: `TalentRules`, `XpCostRules`, `RespecRules` (Tasks 2–3), `SkillAccess` (Task 4), `TalentRegistries` (Task 5).
- Produces:
  - `ModNetwork.CHANNEL` (`ChannelBuilder.named(Identifier.fromNamespaceAndPath(MODID,"main")).networkProtocolVersion(1).simpleChannel()`), `ModNetwork.register()` chamado no construtor do mod, `ModNetwork.sendTo(ServerPlayer, Object)`, `ModNetwork.sendToServer(Object)`.
  - C2S: `C2SConvertXp()`, `C2SBuyNode(String nodeId)`, `C2SChangeClass(String classId)`.
  - S2C: `S2CSyncPlayer(CompoundTag data)`, `S2CSyncDefinitions(List<TalentNode> nodes)` (via `TalentNodeCodec`), `S2CProspectorHighlight(List<BlockPos> positions)` (usado na Task 12).
  - `TalentActions.convertXp(ServerPlayer) -> boolean`, `buyNode(ServerPlayer, String) -> PurchaseResult`, `changeClass(ServerPlayer, String) -> RespecCheck`. Todos rodam na main thread do servidor e, **em qualquer resultado**, terminam com `AttributeSync.apply` (se mudou algo) e `S2CSyncPlayer`.
- Regras:
  - `convertXp`: `LEVELS` → `player.giveExperienceLevels(-costLevels)`; `POINTS` → `player.giveExperiencePoints(-costPoints)`; +1 PT.
  - `changeClass` com `OK_PAID`: cobra a taxa, calcula `refund` sobre os gastos da classe antiga, `resetTree(true)`, define a classe e soma o refund.
  - Strings recebidas com mais de 64 caracteres são rejeitadas antes de qualquer lookup.

- [ ] **Step 1:** Implementar o canal e os pacotes e adicionar o envio de `S2CSyncPlayer` nos handlers de login/respawn/dimensão da Task 4.
- [ ] **Step 2:** `./gradlew build` → PASS (testes do core continuam verdes).
- [ ] **Step 3:** Teste manual com comandos temporários de debug **apenas em dev** (`/vt debug convert|buy <id>|class <id>`, registrados só quando `FMLEnvironment.production == false`): converter com 4 níveis → nada muda; com 5 → +1 PT; comprar `miner_haste` sem classe → recusado; escolher miner → grátis; trocar para archer com 12 PT gastos → recebe 3.
- [ ] **Step 4:** Commit `feat(network): pacotes e ações de servidor`.

---

## Fase 5 — Interface

### Task 8: Tecla, tela de talentos, escolha de classe e confirmação de respec

**Files:**
- Create: `client/KeyBindings.java`, `client/ClientTalentState.java`, `client/TalentScreen.java`, `client/ClassSelectScreen.java`, `client/ConfirmRespecScreen.java`

**Interfaces:**
- Consumes: `TalentRegistries.client()`, `TalentRules.nodeState`, `RespecRules.refund` (prévia), pacotes C2S (Task 7).
- Produces: `ClientTalentState.data() -> PlayerSkillData` (atualizado por `S2CSyncPlayer`; `TalentScreen` reconstrói os widgets ao receber sync).
- Comportamento:
  - Tecla `K` (`key.vanillatalents.open`, categoria `key.categories.vanillatalents`) abre `TalentScreen`.
  - Abas **Comum** e **Classe**. Sem classe, a aba Classe mostra o botão "Escolher classe" → `ClassSelectScreen` com as 5 classes.
  - Grade pela `position`; linhas entre pré-requisito e nó; cores: LOCKED escurecido, AVAILABLE destacado, IN_PROGRESS com contador `n/max`, MAXED com borda dourada.
  - Tooltip: nome, descrição, `Nível n/max`, cada pré-requisito não atendido em vermelho (`<nome> n/req`).
  - Clique esquerdo envia `C2SBuyNode` (sem atualização otimista).
  - Canto: níveis de XP, PT disponíveis, botão `Converter 5 níveis → 1 PT` (rótulo reflete `costMode`), desabilitado se o jogador não puder pagar.
  - Botão "Trocar classe" → `ConfirmRespecScreen` mostrando taxa, PT gastos e PT devolvidos.
  - Registro vazio (antes do sync) → texto "Carregando talentos…".

- [ ] **Step 1:** Implementar.
- [ ] **Step 2:** `./gradlew runClient` (mundo de teste em sobrevivência, `/xp add @s 100 levels`): abrir com K; converter; escolher Minerador; comprar `miner_haste` até 3 e ver `miner_darkvision` mudar de LOCKED para AVAILABLE; tooltip mostra requisito faltando; respec mostra a prévia correta.
- [ ] **Step 3:** Commit `feat(client): GUI da árvore de talentos`.

---
## Fase 6 — Infraestrutura de efeitos e fatia vertical (MVP)

### Task 9: AttributeSync, RecursionGuard, Mixins e Global Loot Modifier

**Files:**
- Create: `effect/AttributeSync.java`, `core/RecursionGuard.java`, `effect/hooks/{DurabilityHooks,ExhaustionHooks,ShieldHooks}.java`, `mixin/{FoodDataMixin,ServerPlayerMixin,ItemStackMixin,PlayerMixin}.java`, `src/main/resources/vanillatalents.mixins.json`, `effect/loot/TalentLootModifier.java`, `data/forge/loot_modifiers/global_loot_modifiers.json`
- Modify: `build.gradle` e `META-INF/mods.toml` (configuração de Mixin conforme os exemplos oficiais em https://github.com/MinecraftForge/MDKExamples, já citados no topo do `build.gradle`)
- Test: `core/RecursionGuardTest.java`

**Interfaces:**
- Produces:
  - `AttributeSync.apply(ServerPlayer)`: para cada linha "atributo" do Apêndice A, remove o modificador `Identifier(vanillatalents, "<nodeId>")` e, se o nível efetivo for > 0, adiciona um novo com `values.per_level × nível` e a operação indicada. Idempotente. Depois de aplicar `MAX_HEALTH`, limita a vida atual a `getMaxHealth()`.
  - `RecursionGuard`: `boolean enter(UUID player, String key)`, `void exit(UUID player, String key)`, `boolean isActive(UUID, String)`. Uso obrigatório nos efeitos que quebram blocos ou causam dano em cascata (`miner_vein`, `farmer_area_harvest`, `farmer_replant`, `warrior_cleave`).
  - `DurabilityHooks.shouldSkipDamage(ServerPlayer, ItemStack) -> boolean` (chamado pelo `ItemStackMixin` no método de dano de durabilidade; resolve `miner_durability`, `farmer_hoe_care`, `explorer_glider`).
  - `ExhaustionHooks.regenMultiplier(ServerPlayer) -> float`, `sprintMultiplier(ServerPlayer)`, `jumpMultiplier(ServerPlayer)` (usados por `FoodDataMixin` nas chamadas `addExhaustion` dentro de `FoodData.tick(ServerPlayer)` e por `ServerPlayerMixin` em `checkMovementStatistics` (sprint, 0,1/m) e `jumpFromGround`).
  - `ShieldHooks.disableTicksMultiplier(Player) -> float` (via `PlayerMixin` no ponto em que o escudo é desativado por machado).
  - `TalentLootModifier` com campo `kind` (`miner_fortune`, `farmer_harvest`, `farmer_forester`, `farmer_replant`); um JSON por `kind` em `data/vanillatalents/loot_modifiers/`.
- Regra: **nenhum outro Mixin** além desses quatro sem atualizar este plano.

- [ ] **Step 1:** Testes `RecursionGuardTest`: `enter` duas vezes com a mesma chave → a segunda retorna false; após `exit`, `enter` volta a funcionar; chaves e jogadores diferentes são independentes.
- [ ] **Step 2–4:** FAIL → implementar → PASS.
- [ ] **Step 5:** `./gradlew runClient` → o jogo abre; log mostra os 4 mixins aplicados sem erro.
- [ ] **Step 6:** Commit `feat(effect): infraestrutura de atributos, mixins e loot modifier`.

### Task 10: Efeitos do MVP (fatia vertical jogável)

Os 5 nós do plano de ação: `common_health`, `common_saturation`, `miner_haste`, `miner_darkvision`, `miner_fortune`.

**Files:**
- Create: `core/formula/CommonFormulas.java`, `core/formula/MinerFormulas.java`, `effect/CommonEffects.java`, `effect/MinerEffects.java`
- Test: `core/formula/CommonFormulasTest.java`, `core/formula/MinerFormulasTest.java`

**Interfaces:**
- Produces:
  - `MinerFormulas.breakSpeedMultiplier(int hasteLvl, double hastePer, int denseLvl, double densePer, boolean isDense, int footingLvl, boolean onGround) -> double` = `(1 + hasteLvl·hastePer) × (isDense ? 1 + denseLvl·densePer : 1) × footingFactor`, onde `footingFactor = onGround ? 1 : (0.2 + 0.4·footingLvl) / 0.2`.
  - `CommonFormulas.regenExhaustionMultiplier(int lvl, double per) -> double` = `1 − lvl·per`.
- Handlers: ver Apêndice A, linhas MVP.

- [ ] **Step 1: Testes:** `breakSpeed(5,.1,3,.15,true,0,true) == 2.175`; `breakSpeed(5,.1,0,.15,false,0,true) == 1.5`; footing nível 1 fora do chão → fator 3,0; nível 2 → 5,0; `regenExhaustionMultiplier(3, .1) == 0.7`.
- [ ] **Step 2–4:** FAIL → implementar → PASS.
- [ ] **Step 5: Checklist manual** (`runClient`): `common_health` 5 → 15 corações após morrer e renascer; `miner_haste` 5 quebra pedra visivelmente mais rápido só com picareta; `miner_darkvision` liga em Y<0 e desliga em Y≥0; com `miner_fortune` 4, minerar 100 minérios de ferro (sem Fortuna) dá ~120 de material bruto (±10).
- [ ] **Step 6:** Commit `feat: efeitos do MVP`. **Marco: o mod é jogável de ponta a ponta.**

---

## Fase 7 — Efeitos por árvore

Padrão em todas as tasks desta fase:
- Toda conta vai para `core/formula/<Arvore>Formulas` com teste JUnit usando os valores de `docs/arvores/`. O handler em `effect/<Arvore>Effects` só lê o nível (`TalentRules.effectiveLevel`), lê `values`, chama a fórmula e aplica.
- Efeitos do tipo "atributo" vêm do `AttributeSync`, não do handler.
- Todos os handlers rodam **só no servidor** (`!level.isClientSide()`), exceto os marcados como cliente.
- Recargas usam `PlayerSkillData.get/setCooldownUntil` com `level.getGameTime()`.
- Cada task termina com o checklist manual dos seus nós (coluna "Verificação" do Apêndice A) e um commit `feat(<arvore>): efeitos`.

### Task 11: Árvore Comum (10 nós restantes)
`common_regen`, `common_toughness`, `common_gourmet`, `common_breath`, `common_aqua`, `common_fireproof`, `common_extinguish`, `common_antidote`, `common_lava`, `common_second_wind`.
Testes obrigatórios em `CommonFormulasTest`: `fireMultiplier(5,.06,3,.08,lava=true) == 0.532 (±1e-6)`; `fireMultiplier(5,.06,3,.08,lava=false) == 0.70`; `secondWindTriggers(healthAfter=4.0)` true, `4.5` false; `gourmetExtraSaturation(base=12.8, lvl=3, per=.1) == 3.84`.

### Task 12: Minerador (9 nós restantes)
`miner_deepslate`, `miner_mole`, `miner_durability`, `miner_ore_xp`, `miner_prospector` (usa `S2CProspectorHighlight`; cliente gera as partículas), `miner_footing`, `miner_stoneskin`, `miner_lavasense`, `miner_vein`.
Testes: `veinLimit(1..3) == 4/8/12`; `veinCollect(start, grafo de teste, limite)` retorna no máximo N posições, só do mesmo bloco, em ordem BFS (implementar sobre uma interface `BlockGraph` em `core` para testar sem mundo); `lavasenseCooldownTicks(1..3) == 1800/1200/600`.

### Task 13: Produtor (12 nós)
Testes: `areaOffsets(1)` = 4 vizinhos em cruz; `areaOffsets(2)` = 8 vizinhos; `breedingCooldown(6000, 3, .15) == 3300`; `auraRadius(1..3) == 3/4/5`; `auraAllowed(lastMoveTick, now)` false se `now − lastMove > 1200`; teto de 32 plantas por pulso (`auraMaxPerPulse`).

### Task 14: Desbravador (12 nós)
Testes em `ExplorerFormulasTest`:
- `fallMultiplier(fallLvl=5, per=.1, rollActive=true, cap=.6) == 0.4`; `(5,.1,false,.6) == 0.5`; `(3,.1,false,.6) == 0.7`.
- `featherfootDistance(40.0) == 31.0` (o vanilla ainda subtrai 3 blocos, então o dano base = 28); `featherfootDistance(10.0) == 1.0` (sem dano).
- Com Pés de Pluma ativo, o modificador `SAFE_FALL_DISTANCE` de `explorer_safe_height` **não** é aplicado (`AttributeSync` checa o capstone).

### Task 15: Guerreiro (12 nós)
Testes: `meleeBonus(5, .5) == 2.5`; `physicalMultiplier(5,.04) == 0.8`; `executeBonus(targetHpFrac=.29, lvl=3, per=.1) == 1.3` e `(.30, …) == 1.0`; `knockbackBonus(lvl, weaponKnockbackLevel)` nunca leva o total acima de 2; `cleaveTargets(lista ordenada por distância) ≤ 3`.
Atualizar `docs/arvores/04_guerreiro.md`, linha `warrior_sweep`, para a reinterpretação descrita em "Decisões assumidas".

### Task 16: Arqueiro (12 nós)
Testes em `ArcherFormulasTest`:
- `damageMultiplier(aim=5,.04, bolt=3,.08, isCrossbow=true, long=3,.1, distance=25) == 1.74`; com `distance=20` → 1.44; com arco (`isCrossbow=false`) e `distance=25` → 1.50.
- `drawProgressPerTick(lvl=4, per=.06) == 1/0.76 (±1e-4)`.
- `reloadTicks(base=25, lvl=4, per=.08, min=8) == 17`; `reloadTicks(base=10, 4, .08, 8) == 8` (teto com Carga Rápida III).
- `conserveChance(5,.08) == 0.40`.

---

## Fase 8 — Balanceamento e fechamento

### Task 17: Config de balanceamento e tetos
**Files:** Modify `Config.java`.
Adicionar (com default = valor do plano): `fallReductionCap=0.6`, `crossbowMinTicks=8`, `auraMaxPerPulse=32`, `auraIdleTicks=1200`, `veinRequiresSneak=true`, `pvpDamageMultiplier=1.0` (aplicado a `warrior_resistance` e `warrior_steadfast` quando atacante e vítima são jogadores). As fórmulas recebem esses valores por parâmetro (já testadas nas Tasks 10–16). Revisar com o José os defaults da tabela "Decisões assumidas".
- [ ] `./gradlew build` → PASS. Commit `feat(config): tetos e opções de balanceamento`.

### Task 18: QA final
- [ ] `./gradlew build` → todos os testes passam.
- [ ] `./gradlew runServer` → `Loaded 72 talent nodes (0 discarded)`; `/reload` mantém o número e reenvia definições.
- [ ] Checklist de ciclo de vida (Review Focus 1): comprar `common_health` 5 → morrer → renascer → ir ao Nether → voltar → relogar: sempre 15 corações; `/attribute @s minecraft:max_health modifier` lista **um** modificador `vanillatalents:common_health`.
- [ ] Review Focus 2: reduzir `maxLevel` de `common_health` para 3 num datapack de teste, `/reload` → vida cai para 13 corações, sem crash; restaurar.
- [ ] Review Focus 3: com o cliente de dev, enviar `C2SBuyNode("archer_aim")` sendo Minerador → nada muda e chega um sync.
- [ ] Review Focus 4: Veio com 12 minérios conectados + Fortuna + `miner_fortune`: cada bloco rola uma vez; sem StackOverflow; Golpe Amplo não encadeia.
- [ ] Review Focus 5: respec para a mesma classe e com 9 níveis (taxa 10) → nada é cobrado.
- [ ] Coluna "Verificação" do Apêndice A, nó por nó; registrar desvios em `docs/arvores/` (seção "Riscos e decisões" da árvore) em vez de mudar valores no código.
- [ ] Commit `chore: QA final`.

---
## Apêndice A — Mapa nó → implementação

Legenda de **Tipo**: `ATR` = atributo via `AttributeSync` (operação: `ADD` = ADD_VALUE, `MB` = ADD_MULTIPLIED_BASE, `MT` = ADD_MULTIPLIED_TOTAL); `EVT` = handler de evento; `MIX` = hook de Mixin (Task 9); `GLM` = `TalentLootModifier`; `CLI` = lado do cliente. Os valores entram em `values` no JSON.

### Comum
| ID | Tipo / gancho | `values` | Verificação |
|---|---|---|---|
| `common_health` ★MVP | ATR `MAX_HEALTH` ADD | `per_level: 2` | 5 → +5 corações, persiste após morte |
| `common_regen` | EVT `PlayerTickEvent.Post`: com fome ≥ 18, ferido e gamerule ativa, chance `per_level×nível` por tick de somar +1 ao `tickTimer` do `FoodData` (acessor no `FoodDataMixin`) | `per_level: 0.10` | cronometrar 10 corações de cura: ~30% mais rápido no nível 3 |
| `common_toughness` | ATR `ARMOR` ADD | `per_level: 1` | +3 de armadura no HUD |
| `common_saturation` ★MVP | MIX `ExhaustionHooks.regenMultiplier` | `per_level: 0.10` | a fome cai mais devagar enquanto cura |
| `common_gourmet` | EVT `LivingEntityUseItemEvent.Finish` (comida): saturação extra = saturação do alimento × `per_level × nível`, limitada ao nível de fome | `per_level: 0.10` | `/data get` da saturação após comer bife |
| `common_breath` | ATR `OXYGEN_BONUS` ADD | `per_level: 0.20` | nível 3: ar dura ~1,6× |
| `common_aqua` | ATR `SUBMERGED_MINING_SPEED` ADD; não aplicar se o capacete tiver Afinidade Aquática | `per_level: 0.20` | minerar submerso: fator 0,6 no nível 2 |
| `common_fireproof` | EVT `LivingHurtEvent`, `DamageTypeTags.IS_FIRE` | `per_level: 0.06` | dano de fogo −30% no nível 5 |
| `common_extinguish` | ATR `BURNING_TIME` MB (negativo) | `per_level: -0.15` | tempo em chamas −45% |
| `common_antidote` | EVT `MobEffectEvent.Added` (Veneno, Fome): reaplicar com duração reduzida, usando `RecursionGuard` | `per_level: 0.10` | veneno de aranha de caverna dura 30% menos |
| `common_lava` | EVT `LivingHurtEvent`, `DamageTypes.LAVA`, multiplicativo com fireproof | `per_level: 0.08` | lava: −46,8% com 5/3 |
| `common_second_wind` | EVT `LivingDamageEvent` (vida após dano ≤ 4): Regeneração II por 80 ticks, recarga de 6000 ticks | `threshold: 4, duration: 80, cooldown: 6000` | dispara uma vez; não dispara de novo em 5 min |

### Minerador
| ID | Tipo / gancho | `values` | Verificação |
|---|---|---|---|
| `miner_haste` ★MVP | EVT `PlayerEvent.BreakSpeed`, ferramenta em `ItemTags.PICKAXES` | `per_level: 0.10` | +50% no nível 5 |
| `miner_deepslate` | mesmo handler, bloco em `#vanillatalents:dense_stone` | `per_level: 0.15` | deepslate mais rápido que pedra comum |
| `miner_mole` | EVT `BreakSpeed`, `ItemTags.SHOVELS` | `per_level: 0.10` | — |
| `miner_durability` | MIX `DurabilityHooks` (picareta/pá) | `per_level: 0.08` | 100 blocos com nível 3 gastam ~76 de durabilidade |
| `miner_fortune` ★MVP | GLM: bloco em `Tags.Blocks.ORES`, sem Toque Suave → com chance, duplica o resultado | `per_level: 0.05` | ver Task 10 |
| `miner_ore_xp` | EVT `BlockEvent.BreakEvent`: `setExpToDrop(round(xp × (1 + per×nível)))` | `per_level: 0.10` | — |
| `miner_prospector` | EVT `PlayerTickEvent.Post` (agachado e parado por 40 ticks) → busca minérios em raio `2×nível` → `S2CProspectorHighlight`; CLI partículas por 60 ticks; recarga de 200 ticks | `radius_per_level: 2, cooldown: 200` | partículas só nos minérios |
| `miner_darkvision` ★MVP | EVT `PlayerTickEvent.Post` a cada 200 ticks: Y < 0 → Visão Noturna por 300 ticks, sem partículas | — | liga/desliga ao cruzar Y=0 |
| `miner_footing` | `BreakSpeed` (fórmula da Task 10) | `per_level: 0.4` | quebrar pendurado em escada |
| `miner_stoneskin` | EVT `LivingHurtEvent`: `IS_EXPLOSION`, `DamageTypes.FALLING_BLOCK`, `FALLING_ANVIL`, `FALLING_STALACTITE` | `per_level: 0.08` | explosão de creeper −24% |
| `miner_lavasense` | EVT `PlayerTickEvent.Post`: em lava e Y < 0 com recarga livre → Resistência ao Fogo por 60 ticks | `cooldown_l1: 1800, cooldown_l2: 1200, cooldown_l3: 600` | — |
| `miner_vein` | EVT `BreakEvent` (agachado, `Tags.Blocks.ORES`): BFS do mesmo bloco, quebra cada um com `ServerPlayerGameMode.destroyBlock`, dentro de `RecursionGuard` | `per_level: 4` | 12 blocos no nível 3; respeita proteção de spawn |

### Produtor
| ID | Tipo / gancho | `values` | Verificação |
|---|---|---|---|
| `farmer_harvest` | GLM: `CropBlock` com idade máxima ou verruga do Nether madura → +1 do produto principal com chance | `per_level: 0.10` | — |
| `farmer_area_harvest` | EVT `BreakEvent` (planta madura + enxada) → quebra vizinhos maduros (`areaOffsets`) com `RecursionGuard` | — | cruz no nível 1, 3×3 no nível 2 |
| `farmer_hoe_care` | MIX `DurabilityHooks` (enxada) | `per_level: 0.15` | — |
| `farmer_light_step` | EVT `BlockEvent.FarmlandTrampleEvent` → cancelar se a entidade for o jogador | — | pular em terra arada |
| `farmer_animal_drops` | EVT `LivingDropsEvent`: matador é jogador, alvo em `#vanillatalents:livestock` → com chance, +1 em uma pilha de drop aleatória | `per_level: 0.10` | — |
| `farmer_shearing` | EVT `PlayerInteractEvent.EntityInteract` (tesoura em ovelha tosquiável) → com chance, dropar 1 lã da cor da ovelha | `per_level: 0.15` | — |
| `farmer_breeding` | EVT `BabyEntitySpawnEvent` → no **tick seguinte**, idade dos pais = `breedingCooldown` | `per_level: 0.15` | — |
| `farmer_twins` | mesmo evento → com chance, gerar outro filhote via `getBreedOffspring`, se o chunk tiver menos de 32 animais | `per_level: 0.05` | — |
| `farmer_growth_aura` | EVT `PlayerTickEvent.Post` a cada 100 ticks: plantas no raio (Y±1), chance de 5% de `randomTick`, até `auraMaxPerPulse` e só se o jogador se moveu nos últimos `auraIdleTicks` | `chance: 0.05, radius_base: 2` | parado por 60 s → para de funcionar |
| `farmer_bonemeal` | EVT `BonemealEvent` → se não cancelado e a chance passar, devolver 1 farinha de osso | `per_level: 0.15` | — |
| `farmer_forester` | GLM para folhas (muda e maçã) + a aura passa a incluir muda, cana, cacto e bambu | `per_level: 0.10` | — |
| `farmer_replant` | GLM remove 1 semente do loot; agenda no tick seguinte `setBlock` com idade 0, se o bloco abaixo ainda for terra arada (ou areia de almas, para verruga) | — | colheita 3×3 replanta tudo |

### Desbravador
| ID | Tipo / gancho | `values` | Verificação |
|---|---|---|---|
| `explorer_swiftness` | ATR `MOVEMENT_SPEED` MT | `per_level: 0.03` | — |
| `explorer_step` | ATR `STEP_HEIGHT` ADD 0,4 (sobe 1 bloco), removido enquanto agachado (`PlayerTickEvent` liga/desliga) | `value: 0.4` | — |
| `explorer_terrain` | ATR `MOVEMENT_EFFICIENCY` ADD | `per_level: 0.25` | areia de almas mais leve |
| `explorer_mounts` | EVT `EntityMountEvent`: modificador temporário em `MOVEMENT_SPEED` da montaria; removido ao desmontar | `per_level: 0.05` | — |
| `explorer_fall` | EVT `LivingFallEvent`: `damageMultiplier ×= fallMultiplier(...)` (inclui Cambalhota e o teto) | `per_level: 0.10` | — |
| `explorer_safe_height` | ATR `SAFE_FALL_DISTANCE` ADD (desligado se `explorer_featherfoot` estiver comprado) | `per_level: 1` | — |
| `explorer_roll` | parte da fórmula de queda: ativo se `isShiftKeyDown()` no impacto | `value: 0.20` | — |
| `explorer_glider` | MIX `DurabilityHooks` (élitra enquanto planando) | `per_level: 0.15` | — |
| `explorer_sprint` | MIX `ExhaustionHooks.sprintMultiplier` | `per_level: 0.15` | — |
| `explorer_jump` | MIX `ExhaustionHooks.jumpMultiplier` | `per_level: 0.20` | — |
| `explorer_swim` | ATR `ForgeMod.SWIM_SPEED` MB | `per_level: 0.10` | — |
| `explorer_featherfoot` | EVT `LivingFallEvent`: `setDistance(featherfootDistance(d))` | `safe_blocks: 12` | queda de 12 blocos sem dano; 40 blocos com Peso-Pena IV ≈ 6 de dano |

### Guerreiro
| ID | Tipo / gancho | `values` | Verificação |
|---|---|---|---|
| `warrior_strength` | EVT `LivingHurtEvent` (atacante jogador, arma em `ItemTags.SWORDS`/`AXES`, golpe direto) → `+meleeBonus` | `per_level: 0.5` | — |
| `warrior_sweep` | ATR `SWEEPING_DAMAGE_RATIO` ADD | `per_level: 0.15` | — |
| `warrior_crit` | EVT `CriticalHitEvent`: multiplicador × (1 + per×nível) | `per_level: 0.10` | — |
| `warrior_bloodlust` | EVT `LivingDeathEvent` (hostil, espada) → `heal(per×nível)` com recarga de 40 ticks | `per_level: 1.0, cooldown: 40` | — |
| `warrior_axe_speed` | EVT `LivingEquipmentChangeEvent` + login: modificador temporário em `ATTACK_SPEED` (MB) só com machado na mão principal | `per_level: 0.05` | — |
| `warrior_armor_break` | EVT `LivingHurtEvent` (machado): escalar o dano pela razão entre `CombatRules.getDamageAfterAbsorb` com a armadura reduzida em `per×nível` e com a armadura cheia | `per_level: 0.05` | — |
| `warrior_executioner` | EVT `LivingHurtEvent`: alvo com vida < 30% | `per_level: 0.10, threshold: 0.30` | — |
| `warrior_resistance` | EVT `LivingHurtEvent` na vítima: `source.getDirectEntity() != null` e fora de `IS_FIRE`, `IS_EXPLOSION`, `BYPASSES_ARMOR` | `per_level: 0.04` | — |
| `warrior_knockback` | ATR `ATTACK_KNOCKBACK` ADD, desligado agachado e quando a arma já tem Repulsão II | `per_level: 0.5` | — |
| `warrior_steadfast` | ATR `KNOCKBACK_RESISTANCE` ADD | `per_level: 0.10` | — |
| `warrior_shield` | MIX `ShieldHooks.disableTicksMultiplier` | `per_level: 0.25` | — |
| `warrior_cleave` | EVT `AttackEntityEvent` guarda a carga do ataque (≥ 0,9) por UUID; `LivingHurtEvent` aplica 40% a até 3 hostis no raio de 1,5 com `RecursionGuard` (sem jogadores, pets, aldeões e golens) | `ratio: 0.4, radius: 1.5, max_targets: 3` | — |

### Arqueiro
| ID | Tipo / gancho | `values` | Verificação |
|---|---|---|---|
| `archer_aim` | EVT `LivingHurtEvent`: projétil `AbstractArrow` (inclui tridente) com dono jogador; entra no `damageMultiplier` aditivo | `per_level: 0.04` | — |
| `archer_draw` | EVT `LivingEntityUseItemEvent.Tick` (arco): acumulador fracionário reduz a duração restante em ticks extras | `per_level: 0.06` | carga total em ~0,76 s no nível 4 |
| `archer_steady` | EVT `EntityJoinLevelEvent` (flecha de arco): interpolar a direção da velocidade em `per×nível` rumo ao olhar do atirador, mantendo o módulo | `per_level: 0.20` | — |
| `archer_mobile` | CLI `MovementInputUpdateEvent`: enquanto usa o arco, devolver parte do input reduzido pelo vanilla | `per_level: 0.25` | — |
| `archer_reload` | EVT `LivingEntityUseItemEvent.Tick` (besta) com o teto `crossbowMinTicks` | `per_level: 0.08` | — |
| `archer_bolt` | parte do `damageMultiplier` (flecha marcada na entrada como disparada por besta, via `getPersistentData`) | `per_level: 0.08` | — |
| `archer_firework` | EVT `LivingHurtEvent`: fonte direta `FireworkRocketEntity` com dono jogador segurando besta | `per_level: 0.10` | — |
| `archer_conserve` | EVT `EntityJoinLevelEvent` (flecha recolhível de jogador em sobrevivência): com chance, devolver a flecha ao inventário e marcar a entidade como `CREATIVE_ONLY` (não recolhível, evita duplicar) | `per_level: 0.08` | — |
| `archer_recover` | EVT `ProjectileImpactEvent` grava o item da flecha no alvo (`getPersistentData`); EVT `LivingDeathEvent` dropa cada um com chance | `per_level: 0.20` | — |
| `archer_longshot` | posição de disparo gravada em `EntityJoinLevelEvent`; distância medida no impacto (> 20 blocos) | `per_level: 0.10, min_distance: 20` | — |
| `archer_marker` | EVT `LivingHurtEvent` (flecha) → Brilho por `40×nível` ticks | `ticks_per_level: 40` | — |
| `archer_pierce` | EVT `EntityJoinLevelEvent`: flecha de arco crítica (carga total) ganha perfuração 1 se ainda não tiver; o 2º alvo recebe 70% (contador em `getPersistentData`) | `second_hit_ratio: 0.7` | — |
