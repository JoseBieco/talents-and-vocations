package com.seunome.vanillatalents.effect;

import com.seunome.vanillatalents.core.formula.AnglerFormulas;
import com.seunome.vanillatalents.core.formula.HookFormulas;
import com.seunome.vanillatalents.effect.loot.Smelting;
import com.seunome.vanillatalents.effect.loot.TalentLootModifier;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Efeitos do Pescador. A fisgada (angler_lure / Maré Alta) fica em FishingHooks; aqui ficam o loot e as condições. */
public final class AnglerEffects {

    private AnglerEffects() {}

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
            boolean fish = stack.is(ItemTags.FISHES);
            AnglerFormulas.Catch first = AnglerFormulas.catchResult(fish,
                    random.nextDouble() < doubleChance, random.nextDouble() < cookChance);
            result.add(first.cooked() ? cooked(level, stack) : stack);
            for (int i = 1; i < first.copies(); i++) {
                boolean cook = AnglerFormulas.catchResult(fish, false, random.nextDouble() < cookChance).cooked();
                ItemStack copy = stack.copy();
                result.add(cook ? cooked(level, copy) : copy);
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
