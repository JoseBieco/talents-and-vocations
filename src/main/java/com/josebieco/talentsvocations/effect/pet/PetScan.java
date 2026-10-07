package com.josebieco.talentsvocations.effect.pet;

import com.josebieco.talentsvocations.TalentsVocations;
import com.josebieco.talentsvocations.core.formula.TamerFormulas;
import com.josebieco.talentsvocations.effect.Talents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Varredura dos pets (spec §7.4): a cada 20 ticks por dono online que tem tamer_pack, tamer_lick ou tamer_golem,
 * só no raio dos nós (nunca global). Conta os lobos do dono para a Matilha (sentados também contam), aplica a
 * Lambida em todos os pets dele e a regeneração do Amigo do Ferro nos golens de ferro dele.
 *
 * <p>Último dano, última Lambida e última cura de golem ficam no {@code getPersistentData()} do pet.
 */
@Mod.EventBusSubscriber(modid = TalentsVocations.MODID)
public final class PetScan {

    public static final String LAST_HURT_KEY = "talentsvocations:last_hurt";
    public static final String LAST_LICK_KEY = "talentsvocations:last_lick";
    public static final String LAST_GOLEM_HEAL_KEY = "talentsvocations:last_golem_heal";

    private static final int PERIOD = 20;
    /** Raio da varredura dos golens (Amigo do Ferro não tem raio próprio; spec §7.4: 16). */
    private static final double GOLEM_RADIUS = 16;
    /** "Nunca": longe o bastante para liberar qualquer espera, sem estourar na subtração. */
    private static final long NEVER = Long.MIN_VALUE / 4;

    private static final Map<UUID, Integer> WOLVES = new HashMap<>();

    private PetScan() {}

    /** Lobos do jogador no raio da Matilha na última varredura (0 sem registro). */
    public static int nearbyWolves(ServerPlayer player) {
        return WOLVES.getOrDefault(player.getUUID(), 0);
    }

    public static void forget(UUID player) {
        WOLVES.remove(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (event.player() instanceof ServerPlayer player && player.tickCount % PERIOD == 0) scan(player);
    }

    /** Grava o momento do último dano real de qualquer pet (base do "sem dano" da Lambida). */
    @SubscribeEvent(priority = Priority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        if (event.getAmount() > 0) markHurt(event.getEntity());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        WOLVES.clear();
    }

    private static void markHurt(LivingEntity entity) {
        if (PetOwnership.ownerId(entity).isEmpty()) return;
        entity.getPersistentData().putLong(LAST_HURT_KEY, entity.level().getGameTime());
    }

    private static void scan(ServerPlayer player) {
        int pack = Talents.level(player, "tamer_pack");
        int lick = Talents.level(player, "tamer_lick");
        int golem = Talents.level(player, "tamer_golem");
        if (pack <= 0) WOLVES.remove(player.getUUID());
        if (pack <= 0 && lick <= 0 && golem <= 0) return;

        double packRadius = pack > 0 ? Talents.value(player, "tamer_pack", "radius") : 0;
        double lickRadius = lick > 0 ? Talents.value(player, "tamer_lick", "radius") : 0;
        double golemRadius = golem > 0 ? GOLEM_RADIUS : 0;
        double radius = Math.max(packRadius, Math.max(lickRadius, golemRadius));
        List<LivingEntity> pets = player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius), e -> e.isAlive() && PetOwnership.isPetOf(e, player));

        long now = player.level().getGameTime();
        int wolves = 0;
        int lickInterval = lick > 0 ? TamerFormulas.lickInterval(lick,
                (int) Talents.value(player, "tamer_lick", "interval_base"),
                (int) Talents.value(player, "tamer_lick", "interval_per_level")) : 0;
        int lickQuiet = lick > 0 ? (int) Talents.value(player, "tamer_lick", "quiet_ticks") : 0;
        float lickHeal = lick > 0 ? (float) Talents.value(player, "tamer_lick", "heal") : 0;
        int golemInterval = golem > 0 ? (int) Talents.value(player, "tamer_golem", "interval_ticks") : 0;
        float golemHeal = golem > 0 ? (float) Talents.value(player, "tamer_golem", "heal") : 0;
        for (LivingEntity pet : pets) {
            double distSq = pet.distanceToSqr(player);
            if (pack > 0 && pet instanceof Wolf && distSq <= packRadius * packRadius) wolves++;
            if (lick > 0 && distSq <= lickRadius * lickRadius) {
                heal(pet, now, LAST_LICK_KEY, lickQuiet, lickInterval, lickHeal);
            }
            if (golem > 0 && pet instanceof IronGolem && distSq <= golemRadius * golemRadius) {
                heal(pet, now, LAST_GOLEM_HEAL_KEY, 0, golemInterval, golemHeal);
            }
        }
        if (pack > 0) WOLVES.put(player.getUUID(), wolves);
    }

    /** Cura {@code amount} se o pet está ferido e a regra de tempo libera; grava a cura em {@code healKey}. */
    private static void heal(LivingEntity pet, long now, String healKey, int quietTicks, int interval, float amount) {
        if (pet.getHealth() >= pet.getMaxHealth()) return;
        CompoundTag data = pet.getPersistentData();
        long lastHurt = data.getLongOr(LAST_HURT_KEY, NEVER);
        long lastHeal = data.getLongOr(healKey, NEVER);
        if (!TamerFormulas.canHeal(now, lastHurt, lastHeal, quietTicks, interval)) return;
        pet.heal(amount);
        data.putLong(healKey, now);
    }
}
