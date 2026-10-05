package com.seunome.vanillatalents.effect;

import com.seunome.vanillatalents.VanillaTalents;
import com.seunome.vanillatalents.core.RecursionGuard;
import com.seunome.vanillatalents.core.formula.WarriorFormulas;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Efeitos do Guerreiro que usam eventos. sweep, knockback, steadfast e axe_speed vêm de AttributeSync;
 * shield vem do hook de Mixin.
 */
@Mod.EventBusSubscriber(modid = VanillaTalents.MODID)
public final class WarriorEffects {

    private static final String CLEAVE_GUARD = "warrior_cleave";
    private static final float FULL_CHARGE = 0.9F;

    /** Se o último ataque corpo a corpo do jogador saiu com carga ≥ 0,9 (lido antes do reset da carga). */
    private static final Map<UUID, Boolean> CHARGED = new HashMap<>();

    private WarriorEffects() {}

    public static void forget(UUID player) {
        CHARGED.remove(player);
    }

    private static boolean isSword(ItemStack stack) {
        return stack.is(ItemTags.SWORDS);
    }

    private static boolean isAxe(ItemStack stack) {
        return stack.is(ItemTags.AXES);
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CHARGED.put(player.getUUID(), player.getAttackStrengthScale(0.5F) >= FULL_CHARGE);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (event.getEntity() instanceof ServerPlayer victim) {
            onPlayerHurt(event, victim, source);
        }
        if (source.getEntity() instanceof ServerPlayer attacker && source.getDirectEntity() == attacker) {
            onMeleeHit(event, attacker, source);
        }
    }

    /** warrior_resistance: dano físico (com entidade direta), fora de fogo, explosão e dano que ignora armadura. */
    private static void onPlayerHurt(LivingHurtEvent event, ServerPlayer victim, DamageSource source) {
        if (source.getDirectEntity() == null || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypeTags.BYPASSES_ARMOR)) return;
        int level = Talents.level(victim, "warrior_resistance");
        if (level <= 0) return;
        double multiplier = WarriorFormulas.physicalMultiplier(level, Talents.value(victim, "warrior_resistance", "per_level"));
        event.setAmount((float) (event.getAmount() * multiplier));
    }

    /** warrior_strength, warrior_executioner, warrior_armor_break e warrior_cleave. */
    private static void onMeleeHit(LivingHurtEvent event, ServerPlayer player, DamageSource source) {
        if (RecursionGuard.SERVER.isActive(player.getUUID(), CLEAVE_GUARD)) return; // dano do Golpe Amplo não encadeia
        ItemStack weapon = player.getMainHandItem();
        boolean axe = isAxe(weapon);
        if (!axe && !isSword(weapon)) return;
        LivingEntity target = event.getEntity();
        float amount = event.getAmount();

        int strength = Talents.level(player, "warrior_strength");
        if (strength > 0) amount += (float) WarriorFormulas.meleeBonus(strength, Talents.value(player, "warrior_strength", "per_level"));

        int execute = Talents.level(player, "warrior_executioner");
        if (execute > 0) {
            amount *= (float) WarriorFormulas.executeBonus(target.getHealth() / target.getMaxHealth(), execute,
                    Talents.value(player, "warrior_executioner", "per_level"), Talents.value(player, "warrior_executioner", "threshold"));
        }

        int armorBreak = axe ? Talents.level(player, "warrior_armor_break") : 0;
        if (armorBreak > 0) {
            float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
            amount *= (float) WarriorFormulas.armorBreakRatio(amount, target.getArmorValue(), armorBreak,
                    Talents.value(player, "warrior_armor_break", "per_level"),
                    (dmg, armor) -> CombatRules.getDamageAfterAbsorb(target, dmg, source, armor, toughness));
        }
        event.setAmount(amount);

        boolean charged = Boolean.TRUE.equals(CHARGED.remove(player.getUUID()));
        if (axe && charged && Talents.level(player, "warrior_cleave") > 0) cleave(player, target, amount);
    }

    /** warrior_cleave: fração do dano a até N hostis perto do alvo; sem jogadores, pets, aldeões e golens. */
    private static void cleave(ServerPlayer player, LivingEntity target, float amount) {
        double ratio = Talents.value(player, "warrior_cleave", "ratio");
        double radius = Talents.value(player, "warrior_cleave", "radius");
        int max = (int) Talents.value(player, "warrior_cleave", "max_targets");
        ServerLevel level = player.level();
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(radius),
                e -> e != target && e != player && e.isAlive() && isCleavable(e));
        nearby.sort(Comparator.comparingDouble(e -> e.distanceToSqr(target)));
        List<LivingEntity> hit = WarriorFormulas.cleaveTargets(nearby, max);
        RecursionGuard.SERVER.runGuarded(player.getUUID(), CLEAVE_GUARD, () -> {
            for (LivingEntity e : hit) e.hurtServer(level, player.damageSources().playerAttack(player), (float) (amount * ratio));
        });
    }

    private static boolean isCleavable(LivingEntity entity) {
        if (entity instanceof Player || !(entity instanceof Enemy)) return false;
        return !(entity instanceof OwnableEntity owned) || owned.getOwnerReference() == null;
    }

    /** warrior_crit: aumenta o multiplicador de acertos críticos. */
    @SubscribeEvent
    public static void onCritical(CriticalHitEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.isVanillaCritical()) return;
        int level = Talents.level(player, "warrior_crit");
        if (level <= 0) return;
        event.setDamageModifier((float) (event.getDamageModifier()
                * WarriorFormulas.critMultiplier(level, Talents.value(player, "warrior_crit", "per_level"))));
    }

    /** warrior_bloodlust: abater hostil com espada cura, com recarga. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getEntity() instanceof Enemy) || !isSword(player.getMainHandItem())) return;
        if (RecursionGuard.SERVER.isActive(player.getUUID(), CLEAVE_GUARD)) return;
        int level = Talents.level(player, "warrior_bloodlust");
        if (level <= 0 || !Talents.cooldownReady(player, "warrior_bloodlust")) return;
        player.heal((float) (level * Talents.value(player, "warrior_bloodlust", "per_level")));
        Talents.startCooldown(player, "warrior_bloodlust", (long) Talents.value(player, "warrior_bloodlust", "cooldown"));
    }
}
