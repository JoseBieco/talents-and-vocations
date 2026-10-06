package com.seunome.vanillatalents.effect;

import com.seunome.vanillatalents.Config;
import com.seunome.vanillatalents.VanillaTalents;
import com.seunome.vanillatalents.core.formula.ExplorerFormulas;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerSpawnPhantomsEvent;
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
        int saddle = player.isPassenger() ? Talents.level(player, "tamer_saddle") : 0;
        // Montado, a queda chega aqui pela propagação da montaria, com o multiplicador que a montaria usou. Se a Sela
        // Firme já reduziu a montaria (TamerEffects.onMountFall), tira-se esse fator para não somar duas vezes.
        double incoming = event.getDamageMultiplier();
        double mountFactor = player.isPassenger() ? TamerEffects.saddleMountFactor(player.getVehicle()) : 1;
        if (mountFactor > 0 && mountFactor < 1) incoming /= mountFactor;
        if (fall == 0 && !roll && saddle == 0) {
            if (incoming != event.getDamageMultiplier()) event.setDamageMultiplier((float) incoming);
            return;
        }
        double multiplier = ExplorerFormulas.fallMultiplier(
                fall, fall > 0 ? Talents.value(player, "explorer_fall", "per_level") : 0,
                roll, roll ? Talents.value(player, "explorer_roll", "value") : 0,
                saddle, saddle > 0 ? Talents.value(player, "tamer_saddle", "per_level") : 0,
                Config.FALL_REDUCTION_CAP.get());
        event.setDamageMultiplier((float) (incoming * multiplier));
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

    /**
     * explorer_tailwind: o foguete usado planando nasce preso ao jogador (construtor com {@code stuckTo}, que também
     * define o dono). Foguetes de besta saem com {@code shotAtAngle} e, planando, a vanilla não deixa usar o foguete
     * num bloco, então dono jogador + planando + sem ângulo identifica o foguete preso. O {@code lifetime} já foi
     * sorteado no construtor, antes de a entidade entrar no mundo.
     */
    @SubscribeEvent
    public static void onRocketJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof FireworkRocketEntity rocket) || rocket.isShotAtAngle()) return;
        if (!(rocket.getOwner() instanceof ServerPlayer player) || !player.isFallFlying()) return;
        int level = Talents.level(player, "explorer_tailwind");
        if (level <= 0) return;
        rocket.lifetime = ExplorerFormulas.tailwindLifetime(rocket.lifetime, level,
                Talents.value(player, "explorer_tailwind", "per_level"));
    }

    /** explorer_nightwatch: nega phantoms enquanto o jogador não passou do mínimo de tempo sem dormir. */
    @SubscribeEvent
    public static void onSpawnPhantoms(PlayerSpawnPhantomsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int level = Talents.level(player, "explorer_nightwatch");
        if (level <= 0) return;
        int minRest = ExplorerFormulas.phantomMinRestTicks(level,
                (int) Talents.value(player, "explorer_nightwatch", "base_ticks"),
                (int) Talents.value(player, "explorer_nightwatch", "per_level_ticks"));
        int sinceRest = player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        if (sinceRest < minRest) event.setResult(Result.DENY);
    }
}
