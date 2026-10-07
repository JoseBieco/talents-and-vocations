package com.seunome.vanillatalents.effect;

import com.seunome.vanillatalents.VanillaTalents;
import com.seunome.vanillatalents.core.RecursionGuard;
import com.seunome.vanillatalents.core.formula.CommonFormulas;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Efeitos da Árvore Comum que usam eventos. Os demais vêm de AttributeSync (health, toughness, breath, aqua,
 * extinguish) e dos hooks de Mixin (regen, saturation).
 */
@Mod.EventBusSubscriber(modid = VanillaTalents.MODID)
public final class CommonEffects {

    /** Também usado pelo Alquimista (ArtisanEffects): efeito reaplicado sob este guard não é reduzido de novo. */
    static final String ANTIDOTE_GUARD = "common_antidote";

    /** Efeito → nó que reduz sua duração (mesma fórmula, mesmo guard). */
    private static final Map<Holder<MobEffect>, String> EFFECT_NODES = Map.of(
            MobEffects.POISON, "common_antidote",
            MobEffects.HUNGER, "common_antidote",
            MobEffects.SLOWNESS, "common_willpower",
            MobEffects.WEAKNESS, "common_willpower",
            MobEffects.MINING_FATIGUE, "common_willpower");

    /** Efeitos recém-aplicados a reduzir no próximo tick: jogador → efeito → duração aplicada. */
    private static final Map<UUID, Map<Holder<MobEffect>, Integer>> PENDING_EFFECTS = new HashMap<>();

    private CommonEffects() {}

    /** common_fireproof, common_lava e common_frostblood (true cancela o dano). */
    @SubscribeEvent
    public static boolean onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return false;
        DamageSource source = event.getSource();
        if (source.is(DamageTypes.FREEZE)) {
            int frost = Talents.level(player, "common_frostblood");
            if (frost <= 0) return false;
            float reduced = (float) (event.getAmount() * CommonFormulas.frostMultiplier(frost, Talents.value(player, "common_frostblood", "per_level")));
            if (reduced <= 0) return true;
            event.setAmount(reduced);
            return false;
        }
        if (!source.is(DamageTypeTags.IS_FIRE)) return false;
        int fire = Talents.level(player, "common_fireproof");
        boolean isLava = source.is(DamageTypes.LAVA);
        int lava = isLava ? Talents.level(player, "common_lava") : 0;
        if (fire == 0 && lava == 0) return false;
        double multiplier = CommonFormulas.fireMultiplier(
                fire, fire > 0 ? Talents.value(player, "common_fireproof", "per_level") : 0,
                lava, lava > 0 ? Talents.value(player, "common_lava", "per_level") : 0, isLava);
        event.setAmount((float) (event.getAmount() * multiplier));
        return false;
    }

    /** common_second_wind: Regeneração II ao ficar com pouca vida, com recarga persistida. */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (Talents.level(player, "common_second_wind") <= 0) return;
        float healthAfter = player.getHealth() - event.getAmount();
        if (!CommonFormulas.secondWindTriggers(healthAfter, Talents.value(player, "common_second_wind", "threshold"))) return;
        if (!Talents.cooldownReady(player, "common_second_wind")) return;
        int duration = (int) Talents.value(player, "common_second_wind", "duration");
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 1), null);
        Talents.startCooldown(player, "common_second_wind", (long) Talents.value(player, "common_second_wind", "cooldown"));
    }

    /** common_gourmet: saturação extra ao terminar de comer, limitada ao nível de fome. */
    @SubscribeEvent
    public static void onFinishUsing(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ironGut(player, event.getItem());
        FoodProperties food = event.getItem().get(DataComponents.FOOD);
        if (food == null) return;
        int level = Talents.level(player, "common_gourmet");
        if (level <= 0) return;
        FoodData data = player.getFoodData();
        double extra = CommonFormulas.gourmetExtraSaturation(food.saturation(), level, Talents.value(player, "common_gourmet", "per_level"));
        data.setSaturation((float) Math.min(data.getSaturationLevel() + extra, data.getFoodLevel()));
    }

    /** common_iron_gut: remove a Fome que a carne podre/frango cru acabou de aplicar. */
    private static void ironGut(ServerPlayer player, ItemStack stack) {
        if (!stack.is(Items.ROTTEN_FLESH) && !stack.is(Items.CHICKEN)) return;
        if (Talents.level(player, "common_iron_gut") <= 0) return;
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);
        if (consumable == null) return;
        int foodDuration = 0;
        for (ConsumeEffect consumeEffect : consumable.onConsumeEffects()) {
            if (!(consumeEffect instanceof ApplyStatusEffectsConsumeEffect apply)) continue;
            for (MobEffectInstance inst : apply.effects()) {
                if (inst.is(MobEffects.HUNGER)) foodDuration = Math.max(foodDuration, inst.getDuration());
            }
        }
        if (foodDuration <= 0) return;
        MobEffectInstance current = player.getEffect(MobEffects.HUNGER);
        if (current != null && CommonFormulas.ironGutClears(current.getDuration(), foodDuration)) {
            player.removeEffect(MobEffects.HUNGER);
        }
    }

    /** common_antidote e common_willpower: marca os efeitos para reaplicar com duração reduzida no próximo tick. */
    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MobEffectInstance effect = event.getEffectInstance();
        String node = EFFECT_NODES.get(effect.getEffect());
        if (node == null) return;
        if (effect.isInfiniteDuration()) return;
        if (RecursionGuard.SERVER.isActive(player.getUUID(), ANTIDOTE_GUARD)) return;
        if (Talents.level(player, node) <= 0) return;
        PENDING_EFFECTS.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(effect.getEffect(), effect.getDuration());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) return;
        Map<Holder<MobEffect>, Integer> pending = PENDING_EFFECTS.remove(player.getUUID());
        if (pending == null) return;
        pending.forEach((type, appliedDuration) -> {
            String node = EFFECT_NODES.get(type);
            if (node == null) return;
            int level = Talents.level(player, node);
            if (level <= 0) return;
            double per = Talents.value(player, node, "per_level");
            MobEffectInstance current = player.getEffect(type);
            // Só reduz se o efeito ativo é o que acabou de ser aplicado (não um mais longo que já existia).
            if (current == null || current.getDuration() > appliedDuration || current.getDuration() < appliedDuration - 2) return;
            MobEffectInstance reduced = new MobEffectInstance(type, CommonFormulas.antidoteDuration(current.getDuration(), level, per),
                    current.getAmplifier(), current.isAmbient(), current.isVisible(), current.showIcon());
            RecursionGuard.SERVER.runGuarded(player.getUUID(), ANTIDOTE_GUARD, () -> {
                player.removeEffect(type);
                player.addEffect(reduced, null);
            });
        });
    }
}
