package com.seunome.vanillatalents.effect.hooks;

import com.seunome.vanillatalents.core.formula.HookFormulas;
import com.seunome.vanillatalents.effect.Talents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;

/** Consumo de durabilidade sem evento no Forge: chamado por ItemStackMixin em hurtAndBreak. */
public final class DurabilityHooks {

    private DurabilityHooks() {}

    /** Nó que protege este item agora, ou null. */
    static String nodeFor(ServerPlayer player, ItemStack stack) {
        if (stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS)) return "miner_durability";
        if (stack.is(ItemTags.HOES)) return "farmer_hoe_care";
        if (stack.has(DataComponents.GLIDER) && player.isFallFlying()) return "explorer_glider";
        return null;
    }

    /** Pontos de dano que de fato serão aplicados (cada ponto pode ser poupado, como Inquebrável). */
    public static int adjustDamage(ServerPlayer player, ItemStack stack, int amount) {
        if (amount <= 0) return amount;
        String nodeId = nodeFor(player, stack);
        if (nodeId == null) return amount;
        int level = Talents.level(player, nodeId);
        if (level <= 0) return amount;
        double skip = HookFormulas.chance(level, Talents.value(player, nodeId, "per_level"));
        return HookFormulas.keptDamage(amount, skip, player.getRandom()::nextDouble);
    }
}
