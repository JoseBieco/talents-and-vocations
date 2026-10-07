package com.seunome.vanillatalents.effect;

import com.seunome.vanillatalents.Config;
import com.seunome.vanillatalents.VanillaTalents;
import com.seunome.vanillatalents.core.formula.ExplorerFormulas;
import com.seunome.vanillatalents.core.formula.TamerFormulas;
import com.seunome.vanillatalents.effect.pet.PetOwnership;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Efeitos do Domador por evento: Fogo Amigo, Sela Firme (montaria) e Laço Eterno. Matilha fica na CombatReduction; a varredura em PetScan. */
@Mod.EventBusSubscriber(modid = VanillaTalents.MODID)
public final class TamerEffects {

    public static final String ETERNAL_UNTIL_KEY = "vanillatalents:eternal_until";

    private TamerEffects() {}

    /**
     * tamer_friendly: dano a um pet seu causado por você (golpe, varredura, flecha, explosão: a entidade responsável)
     * é anulado (true cancela). Não cruza com warrior_parry: lá a vítima é sempre um jogador, aqui nunca.
     */
    @SubscribeEvent
    public static boolean onAttacked(LivingAttackEvent event) {
        return friendlyFire(event.getEntity(), event.getSource());
    }

    /**
     * tamer_eternal: pet de dono online com o capstone sobrevive a dano fatal (true cancela a morte; {@code die}
     * retorna antes de marcar {@code dead}). Prioridade máxima para nenhum outro ouvinte tratar a morte cancelada.
     */
    @SubscribeEvent(priority = Priority.HIGHEST)
    public static boolean onDeath(LivingDeathEvent event) {
        return eternal(event.getEntity(), event.getSource());
    }

    /**
     * tamer_saddle: a montaria ({@link AbstractHorse}) cujo passageiro controlador tem o nó recebe menos dano de queda.
     * A parte do jogador fica em ExplorerEffects.onFall, no mesmo teto do Pouso/Rolamento.
     */
    @SubscribeEvent
    public static void onMountFall(LivingFallEvent event) {
        double factor = saddleMountFactor(event.getEntity());
        if (factor < 1) event.setDamageMultiplier((float) (event.getDamageMultiplier() * factor));
    }

    /**
     * Fator da Sela Firme aplicado à queda de {@code mount}: {@code fallMultiplier(0, 0, false, 0, nível, per, teto)}
     * quando é um {@link AbstractHorse} conduzido por um {@link ServerPlayer} com o nó; 1 caso contrário. Usado também
     * por ExplorerEffects para descontar o fator que a montaria repassa aos passageiros.
     */
    public static double saddleMountFactor(Entity mount) {
        if (!(mount instanceof AbstractHorse horse)) return 1;
        if (!(horse.getControllingPassenger() instanceof ServerPlayer rider)) return 1;
        int level = Talents.level(rider, "tamer_saddle");
        if (level <= 0) return 1;
        return ExplorerFormulas.fallMultiplier(0, 0, false, 0, level,
                Talents.value(rider, "tamer_saddle", "per_level"), Config.FALL_REDUCTION_CAP.get());
    }

    // Helpers não recebem o evento: o EventBus 7 exige @SubscribeEvent em todo método estático com evento.

    private static boolean friendlyFire(LivingEntity victim, DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer attacker)) return false;
        if (!PetOwnership.isPetOf(victim, attacker)) return false;
        return Talents.level(attacker, "tamer_friendly") > 0;
    }

    private static boolean eternal(LivingEntity pet, DamageSource source) {
        if (pet.level().isClientSide() || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (PetOwnership.kind(pet).isEmpty()) return false;
        ServerPlayer owner = PetOwnership.onlineOwner(pet).orElse(null);
        if (owner == null || Talents.level(owner, "tamer_eternal") <= 0) return false;
        long now = pet.level().getGameTime();
        if (!TamerFormulas.eternalReady(now, pet.getPersistentData().getLongOr(ETERNAL_UNTIL_KEY, 0L))) return false;
        pet.setHealth(1.0F);
        pet.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,
                (int) Talents.value(owner, "tamer_eternal", "resistance_ticks"),
                (int) Talents.value(owner, "tamer_eternal", "resistance_amplifier")));
        pet.getPersistentData().putLong(ETERNAL_UNTIL_KEY, now + (long) Talents.value(owner, "tamer_eternal", "cooldown_ticks"));
        return true;
    }
}
