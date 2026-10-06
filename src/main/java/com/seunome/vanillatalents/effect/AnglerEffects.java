package com.seunome.vanillatalents.effect;

import com.seunome.vanillatalents.VanillaTalents;
import com.seunome.vanillatalents.core.RecursionGuard;
import com.seunome.vanillatalents.core.formula.AnglerFormulas;
import com.seunome.vanillatalents.core.formula.HookFormulas;
import com.seunome.vanillatalents.effect.loot.Smelting;
import com.seunome.vanillatalents.effect.loot.TalentLootModifier;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Efeitos do Pescador. A fisgada (angler_lure / Maré Alta) fica em FishingHooks e o barco em BoatHooks; aqui ficam o
 * loot, as condições e o Amigo dos Golfinhos.
 */
@Mod.EventBusSubscriber(modid = VanillaTalents.MODID)
public final class AnglerEffects {

    private static final String DOLPHIN_GUARD = "angler_dolphin";

    /** Graça do Golfinho recém-aplicada a estender no próximo tick: jogador → duração aplicada. */
    private static final Map<UUID, Integer> PENDING_DOLPHIN = new HashMap<>();

    private AnglerEffects() {}

    /** angler_dolphin: marca a Graça do Golfinho recém-aplicada para reaplicar mais longa no próximo tick. */
    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MobEffectInstance effect = event.getEffectInstance();
        if (!effect.is(MobEffects.DOLPHINS_GRACE) || effect.isInfiniteDuration()) return;
        if (RecursionGuard.SERVER.isActive(player.getUUID(), DOLPHIN_GUARD)) return;
        if (Talents.level(player, "angler_dolphin") <= 0) return;
        PENDING_DOLPHIN.put(player.getUUID(), effect.getDuration());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) return;
        Integer appliedDuration = PENDING_DOLPHIN.remove(player.getUUID());
        if (appliedDuration == null) return;
        int level = Talents.level(player, "angler_dolphin");
        if (level <= 0) return;
        MobEffectInstance current = player.getEffect(MobEffects.DOLPHINS_GRACE);
        // Só estende se o efeito ativo é o que acabou de ser aplicado (não um mais longo que já existia).
        if (current == null || current.isInfiniteDuration()
                || current.getDuration() > appliedDuration || current.getDuration() < appliedDuration - 2) return;
        int duration = (int) Math.round(AnglerFormulas.scaled(current.getDuration(), level,
                Talents.value(player, "angler_dolphin", "per_level")));
        MobEffectInstance extended = new MobEffectInstance(MobEffects.DOLPHINS_GRACE, duration,
                current.getAmplifier(), current.isAmbient(), current.isVisible(), current.showIcon());
        RecursionGuard.SERVER.runGuarded(player.getUUID(), DOLPHIN_GUARD, () -> {
            player.removeEffect(MobEffects.DOLPHINS_GRACE);
            player.addEffect(extended, null);
        });
    }

    public static void registerLoot() {
        TalentLootModifier.register(TalentLootModifier.Kind.ANGLER_CATCH, AnglerEffects::catchLoot);
    }

    /** "Na chuva ou submerso" (definição única do Pescador). */
    public static boolean inRainOrSubmerged(Player player) {
        return player.isUnderWater() || player.level().isRainingAt(player.blockPosition());
    }

    /** angler_high_tide ativo agora: tem o capstone e está na chuva ou submerso. */
    public static boolean highTideActive(Player player) {
        return Talents.level(player, "angler_high_tide") > 0 && inRainOrSubmerged(player);
    }

    /**
     * angler_bountiful e angler_cook: só no loot de pesca (anzol + jogador dono). Cada pilha de peixe (ItemTags.FISHES)
     * rola Rede Cheia (cópia extra) e, por cópia, Peixe na Brasa (saída de fornalha; sem receita, fica cru).
     * Tesouro e lixo passam intactos.
     */
    static ObjectArrayList<ItemStack> catchLoot(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!(context.getOptional(LootContextParams.THIS_ENTITY) instanceof FishingHook)) return loot;
        if (!(context.getOptional(LootContextParams.ATTACKING_ENTITY) instanceof ServerPlayer player)) return loot;
        double doubleChance = chance(player, "angler_bountiful");
        double cookChance = chance(player, "angler_cook");
        if (doubleChance <= 0 && cookChance <= 0) return loot;

        ServerLevel level = context.getLevel();
        RandomSource random = context.getRandom();
        ObjectArrayList<ItemStack> result = new ObjectArrayList<>(loot.size() + 1);
        for (ItemStack stack : loot) {
            if (!stack.is(ItemTags.FISHES)) {
                result.add(stack);
                continue;
            }
            AnglerFormulas.Catch first = AnglerFormulas.catchResult(true,
                    random.nextDouble() < doubleChance, random.nextDouble() < cookChance);
            result.add(first.cooked() ? cooked(level, stack) : stack);
            for (int i = 1; i < first.copies(); i++) {
                ItemStack copy = stack.copy();
                result.add(random.nextDouble() < cookChance ? cooked(level, copy) : copy);
            }
        }
        return result;
    }

    private static double chance(ServerPlayer player, String nodeId) {
        int level = Talents.level(player, nodeId);
        return level <= 0 ? 0 : HookFormulas.chance(level, Talents.value(player, nodeId, "per_level"));
    }

    private static ItemStack cooked(ServerLevel level, ItemStack stack) {
        return Smelting.result(level, stack).orElse(stack);
    }
}
