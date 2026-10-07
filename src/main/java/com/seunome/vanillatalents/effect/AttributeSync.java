package com.seunome.vanillatalents.effect;

import com.seunome.vanillatalents.VanillaTalents;
import com.seunome.vanillatalents.capability.SkillAccess;
import com.seunome.vanillatalents.core.AttributeBonuses;
import com.seunome.vanillatalents.data.TalentRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.ForgeMod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Aplica os bônus de atributo dos nós como AttributeModifier de id fixo {@code vanillatalents:<nodeId>}.
 * Idempotente: remove e recria; nunca soma duas vezes. Reaplicado em login, respawn, troca de dimensão,
 * compra/respec, /reload e sempre que o contexto (agachar, capacete, arma) muda.
 */
public final class AttributeSync {

    private static final Map<UUID, AttributeBonuses.Context> LAST_CONTEXT = new HashMap<>();

    private AttributeSync() {}

    public static void apply(ServerPlayer player) {
        AttributeBonuses.Context ctx = context(player);
        LAST_CONTEXT.put(player.getUUID(), ctx);
        SkillAccess.get(player).ifPresent(data -> {
            for (AttributeBonuses.Bonus bonus : AttributeBonuses.compute(data, TalentRegistries.server(), ctx)) {
                AttributeInstance instance = player.getAttribute(holder(bonus.attribute()));
                if (instance == null) continue;
                Identifier id = Identifier.fromNamespaceAndPath(VanillaTalents.MODID, bonus.nodeId());
                instance.removeModifier(id);
                if (bonus.amount() != 0) {
                    instance.addPermanentModifier(new AttributeModifier(id, bonus.amount(), operation(bonus.op())));
                }
            }
        });
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    /** Chamado a cada tick do jogador: só reaplica se o contexto mudou. */
    public static void tick(ServerPlayer player) {
        if (!context(player).equals(LAST_CONTEXT.get(player.getUUID()))) apply(player);
    }

    public static void forget(UUID player) {
        LAST_CONTEXT.remove(player);
    }

    private static AttributeBonuses.Context context(ServerPlayer player) {
        ItemStack weapon = player.getMainHandItem();
        return new AttributeBonuses.Context(
                player.isShiftKeyDown(),
                enchantmentLevel(player, Enchantments.AQUA_AFFINITY, player.getItemBySlot(EquipmentSlot.HEAD)) > 0,
                weapon.is(ItemTags.AXES),
                enchantmentLevel(player, Enchantments.KNOCKBACK, weapon));
    }

    private static int enchantmentLevel(ServerPlayer player, ResourceKey<Enchantment> key, ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key)
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack))
                .orElse(0);
    }

    private static Holder<Attribute> holder(AttributeBonuses.Attr attr) {
        return switch (attr) {
            case MAX_HEALTH -> Attributes.MAX_HEALTH;
            case ARMOR -> Attributes.ARMOR;
            case OXYGEN_BONUS -> Attributes.OXYGEN_BONUS;
            case SUBMERGED_MINING_SPEED -> Attributes.SUBMERGED_MINING_SPEED;
            case BURNING_TIME -> Attributes.BURNING_TIME;
            case MOVEMENT_SPEED -> Attributes.MOVEMENT_SPEED;
            case STEP_HEIGHT -> Attributes.STEP_HEIGHT;
            case MOVEMENT_EFFICIENCY -> Attributes.MOVEMENT_EFFICIENCY;
            case SAFE_FALL_DISTANCE -> Attributes.SAFE_FALL_DISTANCE;
            case SWIM_SPEED -> ForgeMod.SWIM_SPEED.getHolder().orElseThrow();
            case SWEEPING_DAMAGE_RATIO -> Attributes.SWEEPING_DAMAGE_RATIO;
            case ATTACK_KNOCKBACK -> Attributes.ATTACK_KNOCKBACK;
            case KNOCKBACK_RESISTANCE -> Attributes.KNOCKBACK_RESISTANCE;
            case ATTACK_SPEED -> Attributes.ATTACK_SPEED;
            case LUCK -> Attributes.LUCK;
            case BLOCK_INTERACTION_RANGE -> Attributes.BLOCK_INTERACTION_RANGE;
            case SNEAKING_SPEED -> Attributes.SNEAKING_SPEED;
        };
    }

    private static AttributeModifier.Operation operation(AttributeBonuses.Op op) {
        return switch (op) {
            case ADD_VALUE -> AttributeModifier.Operation.ADD_VALUE;
            case ADD_MULTIPLIED_BASE -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case ADD_MULTIPLIED_TOTAL -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        };
    }
}
