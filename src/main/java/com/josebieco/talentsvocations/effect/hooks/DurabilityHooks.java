package com.josebieco.talentsvocations.effect.hooks;

import com.josebieco.talentsvocations.Config;
import com.josebieco.talentsvocations.core.formula.HookFormulas;
import com.josebieco.talentsvocations.core.formula.StackingFormulas;
import com.josebieco.talentsvocations.effect.BuilderEffects;
import com.josebieco.talentsvocations.effect.Talents;
import com.josebieco.talentsvocations.effect.pet.PetOwnership;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraftforge.common.ToolActions;

import java.util.ArrayList;
import java.util.List;

/** Consumo de durabilidade sem evento no Forge: chamado por ItemStackMixin em hurtAndBreak. */
public final class DurabilityHooks {

    private DurabilityHooks() {}

    /**
     * Nós que protegem este item agora (R4: cada fonte rola de forma independente). Cada item tem uma fonte própria
     * (a elitra em voo usa explorer_glider no lugar de common_armor_care) e a ferramenta da mão principal soma
     * builder_tool_care enquanto quebra um bloco de construção.
     */
    static List<String> nodesFor(ServerPlayer player, ItemStack stack) {
        List<String> nodes = new ArrayList<>(2);
        if (stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS)) nodes.add("miner_durability");
        else if (stack.is(ItemTags.HOES)) nodes.add("farmer_hoe_care");
        else if (stack.is(Items.FISHING_ROD) || stack.canPerformAction(ToolActions.FISHING_ROD_CAST)) nodes.add("angler_rod_care");
        else if (stack.has(DataComponents.GLIDER) && player.isFallFlying()) nodes.add("explorer_glider");
        else {
            Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
            if (equippable != null && equippable.slot().isArmor()) nodes.add("common_armor_care");
        }
        // builder_tool_care: o item da mão principal gasto ao quebrar um bloco de construção (mineBlock), qualquer
        // ferramenta; soma-se à fonte própria do item (ex.: miner_durability) com o teto R4.
        if (stack == player.getMainHandItem() && BuilderEffects.breakingBuildingBlock(player)) {
            nodes.add("builder_tool_care");
        }
        return nodes;
    }

    /** Pontos de dano que de fato serão aplicados (cada ponto pode ser poupado, como Inquebrável). */
    public static int adjustDamage(ServerPlayer player, ItemStack stack, int amount) {
        if (amount <= 0) return amount;
        return adjust(player, nodesFor(player, stack), amount);
    }

    /**
     * Item gasto por uma entidade que não é jogador (sobrecarga {@code hurtAndBreak(int, LivingEntity, EquipmentSlot)},
     * que chega à sobrecarga com jogador passando {@code null}). Hoje só a armadura de lobo no corpo de um lobo seu,
     * com o dono online: tamer_wolf_armor do dono (R4).
     */
    public static int adjustWornDamage(LivingEntity wearer, ItemStack stack, EquipmentSlot slot, int amount) {
        if (amount <= 0 || slot != EquipmentSlot.BODY || !(wearer instanceof Wolf) || !stack.is(Items.WOLF_ARMOR)) return amount;
        ServerPlayer owner = PetOwnership.onlineOwner(wearer).orElse(null);
        if (owner == null) return amount;
        return adjust(owner, List.of("tamer_wolf_armor"), amount);
    }

    /** R4: cada nó com nível rola de forma independente, combinado e limitado por durabilitySaveCap. */
    private static int adjust(ServerPlayer player, List<String> nodes, int amount) {
        List<Double> chances = new ArrayList<>(2);
        for (String nodeId : nodes) {
            int level = Talents.level(player, nodeId);
            if (level > 0) chances.add(HookFormulas.chance(level, Talents.value(player, nodeId, "per_level")));
        }
        if (chances.isEmpty()) return amount;
        double skip = StackingFormulas.combinedChance(chances, Config.DURABILITY_SAVE_CAP.get());
        return HookFormulas.keptDamage(amount, skip, player.getRandom()::nextDouble);
    }
}
