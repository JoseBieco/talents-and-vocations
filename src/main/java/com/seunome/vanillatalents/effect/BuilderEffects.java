package com.seunome.vanillatalents.effect;

import com.seunome.vanillatalents.VanillaTalents;
import com.seunome.vanillatalents.core.formula.BuilderFormulas;
import com.seunome.vanillatalents.core.formula.HookFormulas;
import com.seunome.vanillatalents.effect.loot.TalentLootModifier;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Efeitos da árvore do Construtor (alcance e Passo de Gato são atributos: AttributeSync). */
@Mod.EventBusSubscriber(modid = VanillaTalents.MODID)
public final class BuilderEffects {

    public static final TagKey<Block> BUILDING_BLOCKS =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(VanillaTalents.MODID, "building_blocks"));

    public static final TagKey<Item> CHEAP_BLOCKS =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(VanillaTalents.MODID, "cheap_blocks"));

    /**
     * Jogador → gameTime da última quebra de um bloco de BUILDING_BLOCKS. Gravado em BreakEvent, que em
     * ServerPlayerGameMode.destroyBlock dispara antes de ItemStack.mineBlock (o gasto da ferramenta); só vale no
     * mesmo tick e é apagado por qualquer outra quebra.
     */
    private static final Map<UUID, Long> BUILDING_BREAK = new HashMap<>();

    /** Jogador → gameTime da última colocação de bloco (builder_height_work). */
    private static final Map<UUID, Long> LAST_PLACE = new HashMap<>();

    /** Sentinela de {@link #lastPlaceTick} para quem não colocou bloco nesta sessão. */
    public static final long NEVER = Long.MIN_VALUE;

    private BuilderEffects() {}

    public static void registerLoot() {
        TalentLootModifier.register(TalentLootModifier.Kind.BUILDER_GLASS, BuilderEffects::glassLoot);
    }

    public static void forget(UUID player) {
        BUILDING_BREAK.remove(player);
        LAST_PLACE.remove(player);
    }

    /** gameTime da última colocação de bloco do jogador, ou {@link #NEVER}. */
    public static long lastPlaceTick(ServerPlayer player) {
        return LAST_PLACE.getOrDefault(player.getUUID(), NEVER);
    }

    /**
     * builder_height_work (grava a colocação) e builder_thrifty/builder_torch (devolução). LOWEST para só contar o que
     * nenhum outro listener cancelou. Só ServerPlayer: dispensers, endermen e afins não contam.
     */
    @SubscribeEvent(priority = Priority.LOWEST)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        markPlaced(player);
        maybeRefund(player, event.getPlacedBlock());
    }

    /**
     * Camas, portas e afins disparam o evento múltiplo, que tem barramento próprio. Uma colocação = uma rolagem, no
     * máximo 1 item devolvido (o bloco do evento é o primeiro snapshot, o mesmo que o item colocado).
     */
    @SubscribeEvent(priority = Priority.LOWEST)
    public static void onMultiBlockPlace(BlockEvent.EntityMultiPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        markPlaced(player);
        maybeRefund(player, event.getPlacedBlock());
    }

    /**
     * Econômico/Iluminador. Em ForgeHooks.onPlaceItemIntoWorld o evento dispara depois do useOn (que já gastou o item),
     * mas com a mão restaurada para a cópia de antes do uso: a mão que segura o item do bloco colocado é a usada, mesmo
     * no último bloco da pilha. Itens sem forma de item (fogo, gelo do Passo Gelado) dão AIR e são ignorados.
     */
    private static void maybeRefund(ServerPlayer player, BlockState placed) {
        if (player.hasInfiniteMaterials()) return;
        Item item = placed.getBlock().asItem();
        if (item == Items.AIR) return;
        InteractionHand hand = usedHand(player, item);
        if (hand == null) return;
        ItemStack held = player.getItemInHand(hand);
        boolean cheap = held.is(CHEAP_BLOCKS);
        boolean torch = item == Items.TORCH || item == Items.SOUL_TORCH || item == Items.COPPER_TORCH;
        int thrifty = cheap ? Talents.level(player, "builder_thrifty") : 0;
        int torchLvl = torch ? Talents.level(player, "builder_torch") : 0;
        if (thrifty <= 0 && torchLvl <= 0) return;
        double chance = BuilderFormulas.refundChance(
                cheap, thrifty, thrifty > 0 ? Talents.value(player, "builder_thrifty", "per_level") : 0,
                torch, torchLvl, torchLvl > 0 ? Talents.value(player, "builder_torch", "per_level") : 0);
        if (player.getRandom().nextDouble() >= chance) return;
        giveBack(player, hand, held.copyWithCount(1));
    }

    /** Mão principal se ela segura o item, senão a secundária, senão nenhuma (null). */
    private static InteractionHand usedHand(ServerPlayer player, Item item) {
        if (player.getMainHandItem().is(item)) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().is(item)) return InteractionHand.OFF_HAND;
        return null;
    }

    /**
     * builder_yield: cada operação de craft (clique ou cada repetição do shift-clique, que dispara o evento uma vez por
     * craft com a cópia do resultado) com resultado na tag rola uma vez e devolve 1 unidade.
     */
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.hasInfiniteMaterials()) return;
        ItemStack result = event.getCrafting();
        if (result.isEmpty() || !result.is(CHEAP_BLOCKS)) return;
        int level = Talents.level(player, "builder_yield");
        if (level <= 0) return;
        if (player.getRandom().nextDouble() >= HookFormulas.chance(level, Talents.value(player, "builder_yield", "per_level"))) return;
        giveBack(player, null, result.copyWithCount(1));
    }

    /**
     * Devolve {@code one} no próximo tick do servidor: na mão usada se ela está vazia (último bloco da pilha) ou ainda
     * tem o mesmo item e cabe; senão no inventário; senão dropado aos pés do jogador. {@code hand} null = direto ao
     * inventário. Usa a instância atual do jogador (morte/troca de dimensão trocam o objeto); desconectado, perde.
     */
    public static void giveBack(ServerPlayer player, InteractionHand hand, ItemStack one) {
        MinecraftServer server = player.level().getServer();
        UUID id = player.getUUID();
        NextTick.schedule(() -> {
            ServerPlayer current = server.getPlayerList().getPlayer(id);
            if (current == null) return;
            if (hand != null) {
                ItemStack held = current.getItemInHand(hand);
                if (held.isEmpty()) {
                    current.setItemInHand(hand, one);
                    return;
                }
                if (ItemStack.isSameItemSameComponents(held, one) && held.getCount() < held.getMaxStackSize()) {
                    held.grow(1);
                    return;
                }
            }
            if (!current.getInventory().add(one)) current.spawnAtLocation(current.level(), one);
        });
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        BUILDING_BREAK.clear();
        LAST_PLACE.clear();
    }

    private static void markPlaced(ServerPlayer player) {
        LAST_PLACE.put(player.getUUID(), player.level().getGameTime());
    }

    /** Verdadeiro só durante a quebra (mesmo tick) de um bloco da tag por esse jogador. Usado pela lista R4. */
    public static boolean breakingBuildingBlock(ServerPlayer player) {
        Long time = BUILDING_BREAK.get(player.getUUID());
        return time != null && time == player.level().getGameTime();
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (event.getState().is(BUILDING_BLOCKS) && !event.getResult().isDenied()) {
            BUILDING_BREAK.put(player.getUUID(), player.level().getGameTime());
        } else {
            BUILDING_BREAK.remove(player.getUUID());
        }
    }

    /** builder_dismantle (R5: multiplica, sem teto). Os dois lados, com qualquer ferramenta. */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!event.getState().is(BUILDING_BLOCKS)) return;
        int level = Talents.level(event.getEntity(), "builder_dismantle");
        if (level <= 0) return;
        double multiplier = BuilderFormulas.breakMultiplier(level, Talents.value(event.getEntity(), "builder_dismantle", "per_level"));
        event.setNewSpeed((float) (event.getNewSpeed() * multiplier));
    }

    /**
     * builder_hardhat: redução ambiental (sem teto, fora do R2) de blocos caindo e sufocamento. Combina com a parte de
     * blocos caindo de miner_stoneskin multiplicando (cada listener aplica o seu fator).
     */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DamageSource source = event.getSource();
        if (!source.is(DamageTypes.FALLING_BLOCK) && !source.is(DamageTypes.FALLING_ANVIL)
                && !source.is(DamageTypes.FALLING_STALACTITE) && !source.is(DamageTypes.IN_WALL)) return;
        int level = Talents.level(player, "builder_hardhat");
        if (level <= 0) return;
        event.setAmount((float) (event.getAmount()
                * HookFormulas.reductionMultiplier(level, Talents.value(player, "builder_hardhat", "per_level"))));
    }

    /** builder_glass: vidro ou painel quebrado sem Toque Suave e sem drops dropa o próprio bloco. */
    static ObjectArrayList<ItemStack> glassLoot(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!(context.getOptional(LootContextParams.THIS_ENTITY) instanceof ServerPlayer player)) return loot;
        BlockState state = context.getOptional(LootContextParams.BLOCK_STATE);
        if (state == null) return loot;
        if (Talents.level(player, "builder_glass") <= 0) return loot;
        boolean glass = state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(Tags.Blocks.GLASS_PANES);
        boolean silk = glass && MinerEffects.hasSilkTouch(player, context.getOptional(LootContextParams.TOOL));
        if (!BuilderFormulas.glassDrop(glass, loot.isEmpty(), silk)) return loot;
        ItemStack drop = new ItemStack(state.getBlock().asItem());
        if (!drop.isEmpty()) loot.add(drop);
        return loot;
    }
}
