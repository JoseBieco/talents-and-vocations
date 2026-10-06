package com.seunome.vanillatalents.effect.hooks;

import com.seunome.vanillatalents.Config;
import com.seunome.vanillatalents.core.formula.HookFormulas;
import com.seunome.vanillatalents.core.formula.StackingFormulas;
import com.seunome.vanillatalents.effect.Talents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

import java.util.ArrayList;
import java.util.List;

/** Consumo de durabilidade sem evento no Forge: chamado por ItemStackMixin em hurtAndBreak. */
public final class DurabilityHooks {

    private DurabilityHooks() {}

    /**
     * Nós que protegem este item agora (R4: cada fonte rola de forma independente). Hoje cada item tem no máximo um;
     * a elitra em voo usa explorer_glider no lugar de common_armor_care.
     */
    static List<String> nodesFor(ServerPlayer player, ItemStack stack) {
        List<String> nodes = new ArrayList<>(2);
        if (stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS)) nodes.add("miner_durability");
        else if (stack.is(ItemTags.HOES)) nodes.add("farmer_hoe_care");
        else if (stack.has(DataComponents.GLIDER) && player.isFallFlying()) nodes.add("explorer_glider");
        else {
            Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
            if (equippable != null && equippable.slot().isArmor()) nodes.add("common_armor_care");
        }
        return nodes;
    }

    /** Pontos de dano que de fato serão aplicados (cada ponto pode ser poupado, como Inquebrável). */
    public static int adjustDamage(ServerPlayer player, ItemStack stack, int amount) {
        if (amount <= 0) return amount;
        List<Double> chances = new ArrayList<>(2);
        for (String nodeId : nodesFor(player, stack)) {
            int level = Talents.level(player, nodeId);
            if (level > 0) chances.add(HookFormulas.chance(level, Talents.value(player, nodeId, "per_level")));
        }
        if (chances.isEmpty()) return amount;
        double skip = StackingFormulas.combinedChance(chances, Config.DURABILITY_SAVE_CAP.get());
        return HookFormulas.keptDamage(amount, skip, player.getRandom()::nextDouble);
    }
}
