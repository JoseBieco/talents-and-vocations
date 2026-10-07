package com.josebieco.talentsvocations.effect;

import com.josebieco.talentsvocations.Config;
import com.josebieco.talentsvocations.TalentsVocations;
import com.josebieco.talentsvocations.core.RecursionGuard;
import com.josebieco.talentsvocations.core.formula.BlockGraph;
import com.josebieco.talentsvocations.core.formula.HookFormulas;
import com.josebieco.talentsvocations.core.formula.MinerFormulas;
import com.josebieco.talentsvocations.effect.loot.TalentLootModifier;
import com.josebieco.talentsvocations.network.ModNetwork;
import com.josebieco.talentsvocations.network.S2CProspectorHighlight;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** Efeitos da árvore do Minerador (durabilidade e footing ficam em hooks/fórmula de quebra). */
@Mod.EventBusSubscriber(modid = TalentsVocations.MODID)
public final class MinerEffects {

    public static final TagKey<Block> DENSE_STONE =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(TalentsVocations.MODID, "dense_stone"));

    public static final TagKey<Block> HARD_STONE =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(TalentsVocations.MODID, "hard_stone"));

    private static final String VEIN_GUARD = "miner_vein";

    private MinerEffects() {}

    public static void registerLoot() {
        TalentLootModifier.register(TalentLootModifier.Kind.MINER_FORTUNE, MinerEffects::fortuneLoot);
        TalentLootModifier.register(TalentLootModifier.Kind.MINER_SMELTER, MinerEffects::smelterLoot);
        TalentLootModifier.register(TalentLootModifier.Kind.MINER_GEODE, MinerEffects::geodeLoot);
    }

    /** miner_haste, miner_deepslate, miner_mole, miner_footing. Roda nos dois lados: o cliente prevê a quebra. */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        ItemStack tool = player.getMainHandItem();
        boolean pickaxe = tool.is(ItemTags.PICKAXES);
        int haste = pickaxe ? Talents.level(player, "miner_haste") : 0;
        int dense = pickaxe ? Talents.level(player, "miner_deepslate") : 0;
        int mole = tool.is(ItemTags.SHOVELS) ? Talents.level(player, "miner_mole") : 0;
        int footing = Talents.level(player, "miner_footing");
        int obsidian = pickaxe && event.getState().is(HARD_STONE) ? Talents.level(player, "miner_obsidian") : 0;
        if (haste == 0 && dense == 0 && mole == 0 && footing == 0 && obsidian == 0) return;

        double multiplier = MinerFormulas.breakSpeedMultiplier(
                haste, haste > 0 ? Talents.value(player, "miner_haste", "per_level") : 0,
                dense, dense > 0 ? Talents.value(player, "miner_deepslate", "per_level") : 0,
                event.getState().is(DENSE_STONE),
                footing, player.onGround());
        if (mole > 0) multiplier *= MinerFormulas.shovelMultiplier(mole, Talents.value(player, "miner_mole", "per_level"));
        if (obsidian > 0) multiplier *= MinerFormulas.hardStoneMultiplier(obsidian, Talents.value(player, "miner_obsidian", "per_level"));
        event.setNewSpeed((float) (event.getNewSpeed() * multiplier));
    }

    /** miner_ore_xp e miner_vein. */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        BlockState state = event.getState();
        if (!state.is(Tags.Blocks.ORES)) return;

        int xpLevel = Talents.level(player, "miner_ore_xp");
        if (xpLevel > 0 && event.getExpToDrop() > 0) {
            event.setExpToDrop(MinerFormulas.oreXp(event.getExpToDrop(), xpLevel, Talents.value(player, "miner_ore_xp", "per_level")));
        }

        int veinLevel = Talents.level(player, "miner_vein");
        if (veinLevel > 0 && (player.isShiftKeyDown() || !Config.VEIN_REQUIRES_SNEAK.get()) && !event.getResult().isDenied()) {
            int limit = MinerFormulas.veinLimit(veinLevel, Talents.value(player, "miner_vein", "per_level"));
            RecursionGuard.SERVER.runGuarded(player.getUUID(), VEIN_GUARD,
                    () -> breakVein(player, player.level(), event.getPos(), state.getBlock(), limit));
        }
    }

    /** Cada bloco extra passa por destroyBlock: proteção de spawn, drops, durabilidade e exaustão normais. */
    private static void breakVein(ServerPlayer player, ServerLevel level, BlockPos origin, Block block, int limit) {
        BlockGraph<BlockPos> graph = new BlockGraph<>() {
            @Override
            public Iterable<BlockPos> neighbors(BlockPos pos) {
                List<BlockPos> list = new ArrayList<>(26);
                for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                    if (!p.equals(pos)) list.add(p.immutable());
                }
                return list;
            }

            @Override
            public boolean matches(BlockPos pos) {
                return level.isLoaded(pos) && level.getBlockState(pos).is(block);
            }
        };
        for (BlockPos pos : MinerFormulas.veinCollect(origin, graph, limit)) {
            ItemStack tool = player.getMainHandItem();
            if (tool.isEmpty() || !player.hasCorrectToolForDrops(level.getBlockState(pos))) break;
            player.gameMode.destroyBlock(pos);
        }
    }

    /**
     * miner_stoneskin, parte ambiental (sem teto): blocos caindo e explosões sem entidade responsável (cama, TNT sem
     * quem acendeu). A parte de explosão com entidade e miner_underdweller ficam em CombatReduction (R2).
     */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DamageSource source = event.getSource();
        boolean fallingBlock = source.is(DamageTypes.FALLING_BLOCK) || source.is(DamageTypes.FALLING_ANVIL)
                || source.is(DamageTypes.FALLING_STALACTITE);
        boolean environmentalExplosion = source.is(DamageTypeTags.IS_EXPLOSION) && !CombatReduction.isCombatExplosion(source);
        if (!fallingBlock && !environmentalExplosion) return;
        int stoneskin = Talents.level(player, "miner_stoneskin");
        if (stoneskin <= 0) return;
        event.setAmount((float) (event.getAmount()
                * HookFormulas.reductionMultiplier(stoneskin, Talents.value(player, "miner_stoneskin", "per_level"))));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) return;
        darkvision(player);
        lavasense(player);
        prospector(player);
    }

    /** miner_darkvision: Visão Noturna enquanto Y < 0, sem partículas; removida ao subir. */
    private static void darkvision(ServerPlayer player) {
        if (Talents.level(player, "miner_darkvision") <= 0) return;
        int interval = (int) Talents.value(player, "miner_darkvision", "interval");
        if (player.tickCount % Math.max(1, interval) != 0) return;
        int duration = (int) Talents.value(player, "miner_darkvision", "duration");

        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
        boolean ours = current != null && current.isAmbient() && !current.isVisible() && current.getDuration() <= duration;
        if (player.getY() < 0) {
            if (current == null || ours) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, true, false), null);
            }
        } else if (ours) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    /** miner_lavasense: Resistência ao Fogo ao tocar lava abaixo de Y 0, com recarga por nível. */
    private static void lavasense(ServerPlayer player) {
        if (!player.isInLava() || player.getY() >= 0) return;
        int level = Talents.level(player, "miner_lavasense");
        if (level <= 0 || !Talents.cooldownReady(player, "miner_lavasense")) return;
        int duration = (int) Talents.value(player, "miner_lavasense", "duration");
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0), null);
        int cooldown = MinerFormulas.lavasenseCooldownTicks(level,
                (int) Talents.value(player, "miner_lavasense", "cooldown_l1"),
                (int) Talents.value(player, "miner_lavasense", "cooldown_l2"),
                (int) Talents.value(player, "miner_lavasense", "cooldown_l3"));
        Talents.startCooldown(player, "miner_lavasense", cooldown);
    }

    /** miner_prospector: agachado e parado → envia ao cliente as posições de minérios no raio. */
    private static void prospector(ServerPlayer player) {
        int level = Talents.level(player, "miner_prospector");
        if (level <= 0 || !player.isShiftKeyDown()) return;
        int ticks = StillTracker.stillTicks(player);
        if (ticks != (int) Talents.value(player, "miner_prospector", "still_ticks")) return;
        if (!Talents.cooldownReady(player, "miner_prospector")) return;

        int radius = MinerFormulas.prospectorRadius(level, Talents.value(player, "miner_prospector", "radius_per_level"));
        BlockPos center = player.blockPosition();
        List<BlockPos> ores = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (player.level().getBlockState(p).is(Tags.Blocks.ORES)) ores.add(p.immutable());
        }
        ModNetwork.sendTo(player, new S2CProspectorHighlight(ores));
        Talents.startCooldown(player, "miner_prospector", (long) Talents.value(player, "miner_prospector", "cooldown"));
    }

    /** miner_fortune: com chance, duplica o drop de minérios (sem Toque Suave; acumula com Fortuna). */
    static ObjectArrayList<ItemStack> fortuneLoot(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!(context.getOptional(LootContextParams.THIS_ENTITY) instanceof ServerPlayer player)) return loot;
        BlockState state = context.getOptional(LootContextParams.BLOCK_STATE);
        if (state == null || !state.is(Tags.Blocks.ORES)) return loot;
        if (hasSilkTouch(player, context.getOptional(LootContextParams.TOOL))) return loot;

        int level = Talents.level(player, "miner_fortune");
        if (level <= 0) return loot;
        double chance = MinerFormulas.fortuneChance(level, Talents.value(player, "miner_fortune", "per_level"));
        if (context.getRandom().nextDouble() >= chance) return loot;

        ObjectArrayList<ItemStack> doubled = new ObjectArrayList<>(loot.size() * 2);
        for (ItemStack stack : loot) {
            doubled.add(stack);
            doubled.add(stack.copy());
        }
        return doubled;
    }

    /** miner_smelter: com chance, troca material bruto por lingote (sem Toque Suave). Roda depois de miner_fortune. */
    static ObjectArrayList<ItemStack> smelterLoot(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!(context.getOptional(LootContextParams.THIS_ENTITY) instanceof ServerPlayer player)) return loot;
        BlockState state = context.getOptional(LootContextParams.BLOCK_STATE);
        if (state == null || !state.is(Tags.Blocks.ORES)) return loot;
        if (hasSilkTouch(player, context.getOptional(LootContextParams.TOOL))) return loot;

        int level = Talents.level(player, "miner_smelter");
        if (level <= 0) return loot;
        double chance = HookFormulas.chance(level, Talents.value(player, "miner_smelter", "per_level"));
        if (context.getRandom().nextDouble() >= chance) return loot;

        ObjectArrayList<ItemStack> result = new ObjectArrayList<>(loot.size());
        for (ItemStack stack : loot) {
            var target = MinerFormulas.smeltTarget(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                    .map(Identifier::parse)
                    .flatMap(id -> BuiltInRegistries.ITEM.get(id))
                    .map(holder -> holder.value());
            result.add(target.isPresent() ? new ItemStack(target.get(), stack.getCount()) : stack);
        }
        return result;
    }

    /** miner_geode: com chance, +1 fragmento de ametista (aglomerado) ou +1 quartzo (minério de quartzo do Nether). */
    static ObjectArrayList<ItemStack> geodeLoot(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!(context.getOptional(LootContextParams.THIS_ENTITY) instanceof ServerPlayer player)) return loot;
        BlockState state = context.getOptional(LootContextParams.BLOCK_STATE);
        if (state == null) return loot;
        ItemStack bonus;
        if (state.is(Blocks.AMETHYST_CLUSTER)) bonus = new ItemStack(Items.AMETHYST_SHARD);
        else if (state.is(Blocks.NETHER_QUARTZ_ORE)) bonus = new ItemStack(Items.QUARTZ);
        else return loot;

        int level = Talents.level(player, "miner_geode");
        if (level <= 0) return loot;
        double chance = HookFormulas.chance(level, Talents.value(player, "miner_geode", "per_level"));
        if (context.getRandom().nextDouble() >= chance) return loot;
        loot.add(bonus);
        return loot;
    }

    static boolean hasSilkTouch(ServerPlayer player, ItemInstance tool) {
        if (tool == null) return false;
        return player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.SILK_TOUCH)
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, tool) > 0)
                .orElse(false);
    }
}
