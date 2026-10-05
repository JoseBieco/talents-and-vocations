package com.seunome.vanillatalents.effect;

import com.seunome.vanillatalents.VanillaTalents;
import com.seunome.vanillatalents.core.formula.MinerFormulas;
import com.seunome.vanillatalents.effect.loot.TalentLootModifier;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Efeitos da árvore do Minerador. */
@Mod.EventBusSubscriber(modid = VanillaTalents.MODID)
public final class MinerEffects {

    public static final TagKey<Block> DENSE_STONE =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(VanillaTalents.MODID, "dense_stone"));

    private MinerEffects() {}

    public static void registerLoot() {
        TalentLootModifier.register(TalentLootModifier.Kind.MINER_FORTUNE, MinerEffects::fortuneLoot);
    }

    /** miner_haste, miner_deepslate, miner_footing. Roda nos dois lados: o cliente prevê a quebra. */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        boolean pickaxe = player.getMainHandItem().is(ItemTags.PICKAXES);
        int haste = pickaxe ? Talents.level(player, "miner_haste") : 0;
        int dense = pickaxe ? Talents.level(player, "miner_deepslate") : 0;
        int footing = Talents.level(player, "miner_footing");
        if (haste == 0 && dense == 0 && footing == 0) return;

        double multiplier = MinerFormulas.breakSpeedMultiplier(
                haste, haste > 0 ? Talents.value(player, "miner_haste", "per_level") : 0,
                dense, dense > 0 ? Talents.value(player, "miner_deepslate", "per_level") : 0,
                event.getState().is(DENSE_STONE),
                footing, player.onGround());
        event.setNewSpeed((float) (event.getNewSpeed() * multiplier));
    }

    /** miner_darkvision: Visão Noturna enquanto Y < 0, sem partículas; removida ao subir. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) return;
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

    private static boolean hasSilkTouch(ServerPlayer player, ItemInstance tool) {
        if (tool == null) return false;
        return player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.SILK_TOUCH)
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, tool) > 0)
                .orElse(false);
    }
}
