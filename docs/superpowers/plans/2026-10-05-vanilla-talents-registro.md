# Vanilla+ Talents — Registro da execução do plano

Decisões tomadas durante a implementação (`Ruling`), pendências e achados da revisão final. Gerado do ledger de execução.

## Decisões

- Setup: Ruling: trabalho na branch feat/vanilla-talents no próprio checkout, sem worktree — o projeto está no OneDrive e depende do cache .gradle/mavenizer local (um worktree refaria o setup do Forge); o commit inicial ed1b192 em master é o Step 1 da Task 0 — custo se errado: nenhum, a branch isola o trabalho
- Setup: Ruling: parar ao fim de cada Fase para revisão do José — instrução "Como executar" do próprio plano, escrita pelo usuário, prevalece sobre a execução contínua da skill — custo se errado: pausas extras
- Task 0: Ruling: PlayerEvents.onPlayerCloned chama reviveCaps()/invalidateCaps() no jogador original — Entity.remove() invalida as caps antes do Clone no Forge 66, sem isso a cópia na morte seria vazia — custo se errado: nenhum (no-op se já válidas)
- Task 0: Ruling: Step 7 (runServer) adiado — José disse 'siga' sem aceitar a EULA; não aceito termos em nome dele; verificações runServer das Tasks 0/4/5/6 ficam pendentes até eula=true — custo se errado: erro de carga do mod só aparece depois
- Task 1: Ruling: posições duplicadas mantêm o primeiro nó em ordem de id e descartam os demais; ids duplicados idem; dependentes de ciclo são descartados como 'ciclo' — plano não define desempate — custo se errado: mensagem de erro diferente no log
- Task 2: Ruling: pré-requisitos são checados pelo nível EFETIVO do pré-requisito (limitado ao maxLevel atual) — coerente com Review Focus 2 — custo se errado: nenhum na prática
- Task 3: Ruling: validateChange checa nesta ordem INVALID_CLASS → SAME_CLASS → OK_FIRST_CHOICE → NOT_ENOUGH_LEVELS; 'none' e null como destino são INVALID_CLASS — plano não fixa a ordem — custo se errado: código de rejeição diferente
- Task 4: Ruling: handlers de login/respawn/dimensão não foram criados vazios agora — o plano diz que AttributeSync (T9) e S2CSyncPlayer (T7) são adicionados nas respectivas tasks; criá-los vazios seria código morto — custo se errado: nenhum
- Task 5: Ruling: adicionado TalentNodeCodecTest (4 testes, parse/opcionais/árvore inválida/round-trip) no lugar da verificação runServer dos Steps 1–2, bloqueada pela EULA; Steps 1–2 seguem pendentes — custo se errado: falha de registro do listener só aparece no runServer
- Task 5: Ruling: falhas de parse do codec são logadas pelo próprio SimpleJsonResourceReloadListener (ERROR "Couldn't parse data file") e não entram no contador "M discarded", que conta só descartes de validação — custo se errado: contagem menor no log
- Task 6: Ruling: ramos posicionados em x = −3/0/+3 (filhos em x±1 do centro do ramo), não −2/0/+2 — com −2/0/+2 o ramo direito da Comum (3 filhos de fireproof) colide com o ramo central; capstone em (0,4) em todas as árvores — custo se errado: só layout da GUI
- Task 6: Ruling: values extras além do Apêndice A: miner_darkvision {interval:200,duration:300}, miner_lavasense duration:60 — constantes de tempo citadas no Apêndice, movidas para JSON pela regra "números no values" — custo se errado: chaves sem uso
- Task 6: Ruling: DesignDocConsistencyTest também compara ids/maxLevel/pré-requisitos/nome pt_br com as tabelas de docs/arvores/*.md (parse do markdown) — garante o "exatamente as tabelas" do plano — custo se errado: teste quebra se o formato da tabela mudar
- Task 6: Ruling: JSON e lang gerados por script Node descartável (scratchpad), não versionado — os JSON são a fonte da verdade daqui em diante — custo se errado: edição em massa futura é manual
- Task 7: Ruling: mutações de estado extraídas para server/SkillTransactions (buy/changeClass/isValidId) com 6 testes JUnit; TalentActions só cobra XP e sincroniza — TDD exige teste antes do código e o plano só tinha teste manual — custo se errado: nenhum
- Task 7: Ruling: modo POINTS usa XP real calculada (XpCostRules.currentTotalXp = curva + progresso da barra), não Player.totalExperience — totalExperience é pontuação e não cai ao encantar, permitiria cobrar XP que o jogador não tem — custo se errado: nenhum
- Task 7: Ruling: ClientTalentState mínimo e client/ClientPacketHandlers criados agora (não na Task 8) — S2CSyncPlayer/S2CSyncDefinitions precisam de destino; handlers em classe separada para o servidor dedicado não carregar código cliente — custo se errado: nenhum
- Task 7: Ruling: handler de S2CProspectorHighlight é no-op até a Task 12 — custo se errado: nenhum
- Task 7: Ruling: AttributeSync.apply ainda não é chamado nas ações (classe nasce na Task 9) — será ligado lá — custo se errado: nenhum até a Task 9
- Task 8: Ruling: S2CSyncPlayer leva sub-tag "Settings" com EconomySettings (custo/modo/taxa/retorno do servidor) — config COMMON é local; num servidor remoto o cliente mostraria custo errado no botão e na prévia de respec — custo se errado: nenhum
- Task 8: Ruling: cálculos da GUI em core/TalentScreenModel + core/EconomySettings (6 testes) e LangKeysTest (toda chave literal do código existe em pt_br/en_us) — GUI não é testável em JUnit; o resto é cola de render — custo se errado: nenhum
- Task 8: Ruling: categoria de tecla é "key.category.vanillatalents.main" (não "key.categories.vanillatalents") — KeyMapping.Category no 26.3 deriva a chave do Identifier — custo se errado: nenhum
- Task 8: Ruling: fluxo de troca = botão "Trocar classe" → ClassSelectScreen → ConfirmRespecScreen (prévia); sem classe, a escolha é enviada direto (grátis) — o plano não diz como escolher a classe nova antes da confirmação — custo se errado: um clique a mais
- Task 9: Ruling: tabela de atributos + condições (agachado, Afinidade Aquática, machado, Repulsão da arma, featherfoot) em core/AttributeBonuses (9 testes); AttributeSync só aplica e reaplica quando o contexto muda (tick) — antecipa as condições das Tasks 11/14/15 porque vivem no mesmo cálculo — custo se errado: nenhum
- Task 9: Ruling: modificadores PERMANENTES com id fixo vanillatalents:<nodeId> (remove+add) — transientes não são salvos e a vida atual acima de 20 seria cortada ao relogar antes do reapply — custo se errado: modificador órfão se um nó de atributo for removido do código (não do datapack)
- Task 9: Ruling: hooks de Mixin implementados por completo agora (common_saturation, common_regen, explorer_sprint/jump, miner_durability, farmer_hoe_care, explorer_glider, warrior_shield) com core/formula/HookFormulas (3 testes) — a Task 9 diz que os hooks "resolvem" esses nós — custo se errado: nenhum
- Task 9: Ruling: DurabilityHooks.adjustDamage(player, stack, amount) → int (rola por ponto, como Inquebrável) em vez de shouldSkipDamage → boolean — dano > 1 por golpe existe — custo se errado: nenhum
- Task 9: Ruling: common_regen via @Inject HEAD em FoodData.tick (tickTimer++ se fome ≥ 18 e ferido), não PlayerTickEvent + accessor — mesma regra, sem accessor, dentro do Mixin já previsto — custo se errado: nenhum
- Task 9: Ruling: ItemStackMixin usa @Inject HEAD cancelável + rechamada com guarda ThreadLocal, não @ModifyVariable — precisa do ServerPlayer do argumento; @Inject captura args de forma garantida — custo se errado: nenhum
- Task 9: Ruling: GLM despacha por kind para handlers registrados pelas árvores (TalentLootModifier.register); global_loot_modifiers.json começa com entries vazio e cada task adiciona seu JSON — evita casos vazios para kinds das Tasks 10/13 — custo se errado: nenhum
- Task 9: Ruling: compatibilityLevel JAVA_21 (máximo do Mixin 0.8.7); sem annotation processor de refmap — MC 26.x não é ofuscado — custo se errado: falha de mixin no jar de produção (verificar no QA)
- Task 10: Ruling: BreakSpeed roda nos DOIS lados (exceção à regra "só servidor"); Talents.level/value leem ClientTalentState + registro do cliente quando isClientSide — o cliente prevê a quebra; sem isso o bloco "volta" ao quebrar — custo se errado: nenhum
- Task 10: Ruling: miner_darkvision values mudados para interval 20 / duration 400 (era 200/300) — Visão Noturna pisca quando restam < 200 ticks; 300 renovado a cada 200 piscaria sempre — custo se errado: nenhum (não é balanceamento)
- Task 10: Ruling: Visão Noturna "nossa" = ambient && !visible && duração ≤ duration; só ela é removida ao subir (não remove poção bebida) — custo se errado: uma poção ambient sem partículas de outro mod poderia ser removida
- Task 10: Ruling: miner_fortune duplica TODA a lista de drop do minério uma vez quando a chance passa — "duplica o resultado" — custo se errado: nenhum
- Task 11: Ruling: common_antidote reaplica no tick seguinte (Added dispara antes do put e a duração não é mutável); só reduz se o efeito ativo é o recém-aplicado (não encurta um efeito mais longo já existente); RecursionGuard evita reprocessar o próprio re-add — custo se errado: 1 tick com duração cheia
- Task 11: Ruling: common_second_wind não dispara em dano fatal (vida após ≤ 0) — "ao ficar com ≤ 4 de vida" pressupõe sobreviver — custo se errado: nenhum
- Task 11: Ruling: common_gourmet usa FoodProperties.saturation() (já = nutrição × modificador × 2) e limita a saturação total ao nível de fome — custo se errado: nenhum
- Task 12: Ruling: Veio usa vizinhança de 26 blocos (inclui diagonais), para se a ferramenta quebrar ou deixar de ser a correta, e cada bloco extra passa por gameMode.destroyBlock (proteção, drops, durabilidade, exaustão, 1 rolagem de miner_fortune por bloco); RecursionGuard impede o Veio de encadear — custo se errado: veios diagonais seriam ignorados
- Task 12: Ruling: miner_prospector ganhou values still_ticks 40 e particle_ticks 60 (números da spec: 2 s parado, 3 s de partículas) — regra "números no JSON" — custo se errado: nenhum
- Task 12: Ruling: partículas WAX_ON a cada 5 ticks em cada minério destacado — spec só diz "partículas" — custo se errado: só visual
- Task 13: Ruling: farmer_shearing usa PlayerInteractEvent.EntityInteractSpecific (EntityInteract não existe no Forge 66; Specific dispara em Player.interactOn no servidor) — custo se errado: nenhum
- Task 13: Ruling: farmer_bonemeal faz stack.grow(1) no BonemealEvent só se o alvo é válido — o evento vem antes do shrink(1); evita duplicar farinha em alvo inválido — custo se errado: nenhum
- Task 13: Ruling: farmer_forester = com chance, +1 de cada muda/maçã que as folhas JÁ deram (não rerrola a loot table) — rerrolar dentro do GLM arrisca recursão de loot — custo se errado: bônus menor que "chance de muda extra" literal
- Task 13: Ruling: produto principal (farmer_harvest) = primeiro drop que não é a semente; se só houver a semente (cenoura/batata/verruga), ela — custo se errado: nenhum para vanilla
- Task 13: Ruling: auraMaxPerPulse/auraIdleTicks entraram no Config já nesta task (eram da Task 17); interval 100 e chunk_limit 32 foram para o JSON — custo se errado: nenhum
- Task 13: Ruling: replantio e espera de reprodução rodam no ServerTickEvent seguinte (fila NEXT_TICK) — o bloco ainda existe durante o loot; a vanilla seta setAge(6000) depois do evento — custo se errado: nenhum
- Task 14: Ruling: fallMultiplier recebe rollValue como parâmetro (fallMultiplier(5,.1,true,.2,.6)) — o plano omitia o 0,2 da Cambalhota, que vive no JSON — custo se errado: nenhum
- Task 14: Ruling: Pés de Pluma soma de volta o SAFE_FALL_DISTANCE atual do jogador (não 3 fixo) — se outro mod mexer no atributo, a conta continua certa — custo se errado: nenhum
- Task 14: Ruling: fallReductionCap entrou no Config já nesta task (era da Task 17) — custo se errado: nenhum
- Task 14: Ruling: explorer_mounts vale para AbstractHorse (cavalo, burro, mula, camelo, lhamas) com modificador TRANSIENTE na montaria — não fica salvo no cavalo — custo se errado: lhama também ganha bônus
- Task 15: Ruling: dano de melee do mod (strength, executioner, armor_break) só com espada/machado e golpe direto (getDirectEntity == jogador) — spec "Braço Forte: espadas e machados" — custo se errado: Carrasco não vale com mão vazia/tridente corpo a corpo
- Task 15: Ruling: armor_break escala o dano pré-armadura pela razão CombatRules(armadura reduzida)/CombatRules(armadura cheia) — LivingHurtEvent vem antes da armadura; não há gancho para mudar a armadura do alvo — custo se errado: nenhum
- Task 15: Ruling: Golpe Amplo usa o dano final do golpe principal (após strength/executioner/armor_break) × 0,4; dentro do RecursionGuard nenhum bônus de melee nem Sede de Batalha dispara — spec "não encadeia" — custo se errado: nenhum
- Task 15: Ruling: warrior_crit só em crítico vanilla (isVanillaCritical) — custo se errado: críticos forçados por outros mods não ganham bônus
- Task 15: Ruling: docs/arvores/04_guerreiro.md e as traduções de warrior_sweep atualizadas para "+0,15 SWEEPING_DAMAGE_RATIO" (pedido do plano) — custo se errado: nenhum
- Task 16: Ruling: AccessTransformer (META-INF/accesstransformer.cfg + minecraft.accessTransformer = true) para AbstractArrow.setPierceLevel — o plano proíbe Mixins extras, não ATs; não há API pública para perfuração — custo se errado: um AT a manter entre versões do MC
- Task 16: Ruling: Atirar Andando lê a lentidão do item via componente USE_EFFECTS (itemUseSpeedMultiplier é privado) e escala ClientInput.moveVector no MovementInputUpdateEvent — custo se errado: nenhum
- Task 16: Ruling: draw e reload usam acumulador fracionário por (lado, jogador) e rodam nos dois lados (cliente anima/carrega, servidor calcula a força) — custo se errado: pequena dessincronia visual
- Task 16: Ruling: bolt/longshot/marker só para flechas (não tridentes); aim inclui tridente — spec "Tridentes: incluídos no Olho de Águia... não no restante" — custo se errado: nenhum
- Task 16: Ruling: Recolhedor rola a chance no impacto e guarda a flecha (ItemStack via CODEC) no persistentData do alvo; dropa tudo na morte; só flechas com pickup ALLOWED — custo se errado: alvo que nunca morre guarda NBT
- Task 17: Ruling: defaults de "Decisões assumidas" mantidos (LEVELS/5, taxa 10, retorno 25%, miner_fortune em ferro/ouro/cobre) — José pediu "siga" sem escolher; todos são config — custo se errado: só editar o toml
- Task 17: Ruling: pvpDamageMultiplier escala a REDUÇÃO de warrior_resistance (1 − redução×pvp) e, para warrior_steadfast (atributo), compensa a força no LivingKnockBackEvent quando o último golpe do tick veio de jogador (getLastHurtByMob + timestamp) — o evento de knockback não traz a fonte — custo se errado: knockback de jogador indireto no mesmo tick pode ser tratado como PvP
- Task 18: Ruling: "respec com 0 pontos gastos → nenhuma cobrança" (Review Focus 5) implementado como troca gratuita (RespecRules.feeFor); a classe muda, nada é zerado — interpretar como "recusar a troca" deixaria o jogador preso numa classe escolhida por engano — custo se errado: troca grátis sem gastar pontos (sem ganho: nada a devolver)

## Achados da revisão final

- Final review: self-review (subagente não disparado — o José não pediu subagentes nesta sessão)
- Final: fixed renascer com a vida extra vazia (restoreFrom faz setHealth(max) antes do reapply) — AttributeBonusesTest.respawnHealth_deathRespawnsAtFullBonusHealth RED→GREEN, suite 134/134
- Final: minor (deferred): Talents.level no cliente usa os níveis do jogador local também para entidades de outros jogadores (só a animação de puxar arco deles)
- Final: minor (deferred): kind desconhecido no JSON de loot modifier lança exceção no codec (Kind.valueOf) em vez de erro de parse tratado
- Final: minor (deferred): mods.toml versionRange do minecraft "[26.3,26.4,)" tem vírgula sobrando (herdado do projeto original; carrega)

## Verificações pendentes (manuais)

- Task 0: Steps 1–6 ok (commit c8e0be6, ./gradlew build → BUILD SUCCESSFUL, 1/1 teste). Step 7 BLOQUEADO: runServer para em "You need to agree to the EULA" — aceitar a EULA exige autorização do José.
- Task 4: PlayerSkillDataTest roda sem bootstrap (CompoundTag instanciável; HolderLookup.Provider = null). Step 5 (runServer) pendente pela EULA.
- Task 6: Step 5 (runServer "Loaded 72 talent nodes") pendente pela EULA.
- Task 7: Step 3 (/vt debug em jogo) pendente: precisa de cliente com jogador (manual do José).
- Task 8: Step 2 parcial: runClient sobe até a tela de título sem erros do mod (log: "mod:vanillatalents", config gerado; só erros de Realms offline). Checklist em jogo (K, converter, comprar, tooltip, prévia) pendente — manual do José.
- Task 10: Step 5 (checklist em jogo) pendente — manual do José.
- Task 11: checklist em jogo pendente — manual do José.
- Task 12: checklist em jogo pendente — manual do José.
- Task 13: checklist em jogo pendente — manual do José.
- Task 14: checklist em jogo pendente — manual do José.
- Task 15: checklist em jogo pendente — manual do José.
- Task 16: checklist em jogo pendente — manual do José.
- Task 18: QA estático dos 5 Review Focus feito no código; jar verificado (MixinConfigs no manifest, AT, 72 skills, 4 GLMs, lang). runServer e checklists em jogo pendentes (EULA / manual do José).

## Correções após o teste em jogo do José

- Clique nos nós não comprava: no MC 26.x os botões do mouse seguem o SDL3 (`InputConstants.MOUSE_BUTTON_LEFT = 1`); `TalentScreen` comparava `event.button() == 0`. Evidência: save do mundo com `CurrentClass=archer`, `AvailablePoints=20`, `UnlockedNodes` vazio. Regressão coberta por `InputConstantsUsageTest`.
- Texto do botão "Trocar classe" sobreposto: o rótulo "Classe: X" ficava no mesmo canto do botão; movido para cima dele.
