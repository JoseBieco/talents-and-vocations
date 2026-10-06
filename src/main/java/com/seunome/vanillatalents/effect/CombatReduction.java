package com.seunome.vanillatalents.effect;

import com.seunome.vanillatalents.Config;
import com.seunome.vanillatalents.VanillaTalents;
import com.seunome.vanillatalents.core.formula.HookFormulas;
import com.seunome.vanillatalents.core.formula.MinerFormulas;
import com.seunome.vanillatalents.core.formula.StackingFormulas;
import com.seunome.vanillatalents.core.formula.TamerFormulas;
import com.seunome.vanillatalents.core.formula.WarriorFormulas;
import com.seunome.vanillatalents.effect.pet.PetScan;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * R2 (spec 9.5): reduções de dano de combate do jogador atingido, de todas as classes, combinadas
 * multiplicativamente e limitadas por {@code combatReductionCap}. Cada nó mantém a própria condição; este é o único
 * lugar onde eles são aplicados. Classes novas entram em {@link #multipliers}.
 *
 * <p>Prioridade HIGH: roda antes dos bônus de ataque (warrior_strength é plano), preservando a ordem de antes
 * (a resistência do Guerreiro era aplicada antes do bônus do atacante no mesmo handler).
 */
@Mod.EventBusSubscriber(modid = VanillaTalents.MODID)
public final class CombatReduction {

    private CombatReduction() {}

    @SubscribeEvent(priority = Priority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        List<Double> multipliers = multipliers(victim, event.getSource());
        if (multipliers.isEmpty()) return;
        double factor = StackingFormulas.combatMultiplier(multipliers, Config.COMBAT_REDUCTION_CAP.get());
        event.setAmount((float) (event.getAmount() * factor));
    }

    // Helpers não recebem o evento: o EventBus 7 exige @SubscribeEvent em todo método estático com evento.

    /** Fatores (< 1) dos nós de redução de combate que se aplicam a este dano. */
    private static List<Double> multipliers(ServerPlayer victim, DamageSource source) {
        List<Double> list = new ArrayList<>(4);
        warriorResistance(list, victim, source);
        minerStoneskin(list, victim, source);
        minerUnderdweller(list, victim, source);
        anglerDepths(list, victim, source);
        tamerPack(list, victim, source);
        return list;
    }

    /**
     * angler_depths: dano causado por Afogado ou Guardião (o Ancião estende Guardian). Usa a entidade responsável, então
     * cobre o raio (indirectMagic(guardião, guardião)), o tridente arremessado pelo afogado e os espinhos do guardião.
     */
    private static void anglerDepths(List<Double> list, ServerPlayer victim, DamageSource source) {
        if (!(source.getEntity() instanceof Drowned) && !(source.getEntity() instanceof Guardian)) return;
        int level = Talents.level(victim, "angler_depths");
        if (level <= 0) return;
        list.add(HookFormulas.reductionMultiplier(level, Talents.value(victim, "angler_depths", "per_level")));
    }

    /** tamer_pack: lobos seus no raio (contados pela varredura de PetScan, até max_wolves); só dano de combate (com entidade). */
    private static void tamerPack(List<Double> list, ServerPlayer victim, DamageSource source) {
        if (source.getEntity() == null || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        int level = Talents.level(victim, "tamer_pack");
        if (level <= 0) return;
        int wolves = PetScan.nearbyWolves(victim);
        if (wolves <= 0) return;
        list.add(TamerFormulas.packMultiplier(level, Talents.value(victim, "tamer_pack", "per_level"), wolves,
                (int) Talents.value(victim, "tamer_pack", "max_wolves")));
    }

    /** warrior_resistance: dano físico (com entidade direta), fora de fogo, explosão e dano que ignora armadura. */
    private static void warriorResistance(List<Double> list, ServerPlayer victim, DamageSource source) {
        if (source.getDirectEntity() == null || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypeTags.BYPASSES_ARMOR)) return;
        int level = Talents.level(victim, "warrior_resistance");
        if (level <= 0) return;
        double pvp = source.getEntity() instanceof Player ? Config.PVP_DAMAGE_MULTIPLIER.get() : 1.0;
        list.add(WarriorFormulas.physicalMultiplier(level, Talents.value(victim, "warrior_resistance", "per_level"), pvp));
    }

    /** miner_stoneskin, parte de combate: explosão com entidade responsável (creeper, TNT acesa por alguém...). */
    private static void minerStoneskin(List<Double> list, ServerPlayer victim, DamageSource source) {
        if (!isCombatExplosion(source)) return;
        int level = Talents.level(victim, "miner_stoneskin");
        if (level <= 0) return;
        list.add(HookFormulas.reductionMultiplier(level, Talents.value(victim, "miner_stoneskin", "per_level")));
    }

    /** miner_underdweller: dano de mob abaixo de Y 0. */
    private static void minerUnderdweller(List<Double> list, ServerPlayer victim, DamageSource source) {
        if (!(source.getEntity() instanceof Mob)) return;
        int level = Talents.level(victim, "miner_underdweller");
        if (level <= 0) return;
        list.add(MinerFormulas.underdwellerMultiplier(level, Talents.value(victim, "miner_underdweller", "per_level"), victim.getY()));
    }

    /** Explosão de combate (R2): tem entidade responsável. Sem entidade (cama, TNT sem quem acendeu) é ambiental. */
    static boolean isCombatExplosion(DamageSource source) {
        return source.is(DamageTypeTags.IS_EXPLOSION) && source.getEntity() != null;
    }
}
