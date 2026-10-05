package com.seunome.vanillatalents.effect;

import com.seunome.vanillatalents.Config;
import com.seunome.vanillatalents.VanillaTalents;
import com.seunome.vanillatalents.core.formula.ExplorerFormulas;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Efeitos do Desbravador que usam eventos. Atributos (swiftness, step, terrain, safe_height, swim) vêm de
 * AttributeSync; glider, sprint e jump vêm dos hooks de Mixin.
 */
@Mod.EventBusSubscriber(modid = VanillaTalents.MODID)
public final class ExplorerEffects {

    private static final Identifier MOUNT_MODIFIER = Identifier.fromNamespaceAndPath(VanillaTalents.MODID, "explorer_mounts");

    private ExplorerEffects() {}

    /** explorer_featherfoot (distância), explorer_fall e explorer_roll (multiplicador com teto). */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (Talents.level(player, "explorer_featherfoot") > 0) {
            double safeBlocks = Talents.value(player, "explorer_featherfoot", "safe_blocks");
            event.setDistance(ExplorerFormulas.featherfootDistance(event.getDistance(), safeBlocks,
                    player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE)));
        }

        int fall = Talents.level(player, "explorer_fall");
        boolean roll = player.isShiftKeyDown() && Talents.level(player, "explorer_roll") > 0;
        if (fall == 0 && !roll) return;
        double multiplier = ExplorerFormulas.fallMultiplier(
                fall, fall > 0 ? Talents.value(player, "explorer_fall", "per_level") : 0,
                roll, roll ? Talents.value(player, "explorer_roll", "value") : 0,
                Config.FALL_REDUCTION_CAP.get());
        event.setDamageMultiplier((float) (event.getDamageMultiplier() * multiplier));
    }

    /** explorer_mounts: modificador temporário na velocidade da montaria, removido ao desmontar. */
    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (!(event.getEntityMounting() instanceof ServerPlayer player)) return;
        if (!(event.getEntityBeingMounted() instanceof AbstractHorse mount)) return;
        AttributeInstance speed = mount.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        speed.removeModifier(MOUNT_MODIFIER);
        if (!event.isMounting()) return;
        int level = Talents.level(player, "explorer_mounts");
        if (level <= 0) return;
        double bonus = ExplorerFormulas.mountBonus(level, Talents.value(player, "explorer_mounts", "per_level"));
        speed.addTransientModifier(new AttributeModifier(MOUNT_MODIFIER, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
