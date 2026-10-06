package com.seunome.vanillatalents.effect;

import com.mojang.serialization.Dynamic;
import com.seunome.vanillatalents.Config;
import com.seunome.vanillatalents.VanillaTalents;
import com.seunome.vanillatalents.core.formula.ArcherFormulas;
import com.seunome.vanillatalents.core.formula.HookFormulas;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Efeitos do Arqueiro (archer_mobile é do cliente, em ClientEffects). */
@Mod.EventBusSubscriber(modid = VanillaTalents.MODID)
public final class ArcherEffects {

    // Chaves em Entity.getPersistentData()
    private static final String CROSSBOW = "vanillatalents_crossbow";
    private static final String ORIGIN_X = "vanillatalents_ox";
    private static final String ORIGIN_Y = "vanillatalents_oy";
    private static final String ORIGIN_Z = "vanillatalents_oz";
    private static final String PIERCE = "vanillatalents_pierce";
    private static final String PIERCE_HITS = "vanillatalents_pierce_hits";
    private static final String RECOVER = "vanillatalents_recover";

    /** Acumulador de progresso de puxada/recarga por (lado, entidade). */
    private static final Map<String, Double> USE_REMAINDER = new HashMap<>();

    private ArcherEffects() {}

    public static void forget(UUID player) {
        USE_REMAINDER.remove("S" + player);
        USE_REMAINDER.remove("C" + player);
    }

    // ---- Disparo ---------------------------------------------------------------------------------------------------

    /** Marca a flecha ao entrar no mundo: besta/arco, origem (Tiro Longo), Mão Firme, Aljava Econômica, Perfurante. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof AbstractArrow arrow) || arrow instanceof ThrownTrident) return;
        if (!(arrow.getOwner() instanceof ServerPlayer player)) return;
        ItemStack weapon = arrow.getWeaponItem();
        boolean crossbow = weapon != null && weapon.getItem() instanceof CrossbowItem;
        boolean bow = weapon != null && weapon.getItem() instanceof BowItem;

        CompoundTag data = arrow.getPersistentData();
        data.putBoolean(CROSSBOW, crossbow);
        data.putDouble(ORIGIN_X, player.getX());
        data.putDouble(ORIGIN_Y, player.getEyeY());
        data.putDouble(ORIGIN_Z, player.getZ());

        if (bow) steady(arrow, player);
        if (bow) heldBreath(arrow, player);
        if (bow) pierce(arrow, player, data);
        conserve(arrow, player);
    }

    /** archer_held_breath: agachado e parado há {@code still_ticks}, a flecha de arco sai mais rápida. */
    private static void heldBreath(AbstractArrow arrow, ServerPlayer player) {
        if (!player.isShiftKeyDown() || Talents.level(player, "archer_held_breath") <= 0) return;
        if (StillTracker.stillTicks(player) < (int) Talents.value(player, "archer_held_breath", "still_ticks")) return;
        double factor = 1 + Talents.value(player, "archer_held_breath", "value");
        arrow.setDeltaMovement(arrow.getDeltaMovement().scale(factor));
    }

    /** archer_double_load: depois do disparo da besta, com chance, recarrega no próximo tick gastando munição. */
    @SubscribeEvent
    public static void onLoose(ArrowLooseEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack crossbow = event.getBow();
        if (!(crossbow.getItem() instanceof CrossbowItem)) return;
        int level = Talents.level(player, "archer_double_load");
        if (level <= 0) return;
        if (player.getRandom().nextDouble() >= HookFormulas.chance(level, Talents.value(player, "archer_double_load", "per_level"))) return;
        NextTick.schedule(() -> reloadCrossbow(player, crossbow));
    }

    /** Recarrega a besta se ela ainda estiver na mão e descarregada; sem munição, não faz nada. */
    private static void reloadCrossbow(ServerPlayer player, ItemStack crossbow) {
        if (player.isRemoved() || !player.isAlive()) return;
        if (player.getMainHandItem() != crossbow && player.getOffhandItem() != crossbow) return;
        if (CrossbowItem.isCharged(crossbow) || !CrossbowItem.tryLoadProjectiles(player, crossbow)) return;
        Holder<SoundEvent> sound = EnchantmentHelper
                .pickHighestLevel(crossbow, EnchantmentEffectComponents.CROSSBOW_CHARGING_SOUNDS)
                .flatMap(CrossbowItem.ChargingSounds::end)
                .orElse(SoundEvents.CROSSBOW_LOADING_END);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound.value(), player.getSoundSource(),
                1.0F, 1.0F / (player.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F);
    }

    /** archer_steady: puxa a direção da flecha em direção ao olhar, mantendo a velocidade. */
    private static void steady(AbstractArrow arrow, ServerPlayer player) {
        int level = Talents.level(player, "archer_steady");
        if (level <= 0) return;
        double t = HookFormulas.chance(level, Talents.value(player, "archer_steady", "per_level"));
        Vec3 velocity = arrow.getDeltaMovement();
        double speed = velocity.length();
        if (speed < 1.0E-6) return;
        Vec3 direction = velocity.normalize().lerp(player.getLookAngle(), t).normalize();
        arrow.setDeltaMovement(direction.scale(speed));
    }

    /** archer_pierce: flecha de arco com carga total ganha Perfuração I se ainda não tiver. */
    private static void pierce(AbstractArrow arrow, ServerPlayer player, CompoundTag data) {
        if (!arrow.isCritArrow() || arrow.getPierceLevel() > 0) return;
        if (Talents.level(player, "archer_pierce") <= 0) return;
        arrow.setPierceLevel((byte) 1);
        data.putBoolean(PIERCE, true);
    }

    /** archer_conserve: devolve a flecha e torna a disparada não recolhível (evita duplicar). */
    private static void conserve(AbstractArrow arrow, ServerPlayer player) {
        if (arrow.pickup != AbstractArrow.Pickup.ALLOWED || player.hasInfiniteMaterials()) return;
        int level = Talents.level(player, "archer_conserve");
        if (level <= 0) return;
        double chance = ArcherFormulas.conserveChance(level, Talents.value(player, "archer_conserve", "per_level"));
        if (player.getRandom().nextDouble() >= chance) return;
        ItemStack refund = arrow.getPickupItemStackOrigin().copyWithCount(1);
        if (!player.getInventory().add(refund)) player.spawnAtLocation(player.level(), refund);
        arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
    }

    // ---- Puxada e recarga ------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player player) USE_REMAINDER.remove(key(player));
    }

    /** archer_draw (arco) e archer_reload (besta): avança a carga com um acumulador fracionário. Roda nos dois lados. */
    @SubscribeEvent
    public static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack stack = event.getItem();
        double factor;
        if (stack.getItem() instanceof BowItem) {
            int level = Talents.level(player, "archer_draw");
            if (level <= 0) return;
            factor = ArcherFormulas.drawProgressPerTick(level, Talents.value(player, "archer_draw", "per_level"));
        } else if (stack.getItem() instanceof CrossbowItem && !CrossbowItem.isCharged(stack)) {
            int level = Talents.level(player, "archer_reload");
            if (level <= 0) return;
            int base = CrossbowItem.getChargeDuration(stack, player);
            int target = ArcherFormulas.reloadTicks(base, level, Talents.value(player, "archer_reload", "per_level"),
                    Config.CROSSBOW_MIN_TICKS.get());
            factor = (double) base / Math.max(1, target);
        } else {
            return;
        }
        ArcherFormulas.Step step = ArcherFormulas.accumulate(USE_REMAINDER.getOrDefault(key(player), 0.0), factor);
        USE_REMAINDER.put(key(player), step.remainder());
        if (step.extraTicks() > 0) event.setDuration(Math.max(0, event.getDuration() - step.extraTicks()));
    }

    private static String key(Player player) {
        return (player.level().isClientSide() ? "C" : "S") + player.getUUID();
    }

    // ---- Acerto ----------------------------------------------------------------------------------------------------

    /** archer_aim/bolt/longshot (aditivos), archer_pierce (2º alvo), archer_marker, archer_firework. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (event.getSource().getDirectEntity() instanceof FireworkRocketEntity rocket) {
            event.setAmount(fireworkAmount(event.getAmount(), rocket));
            return;
        }
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof ServerPlayer player)) return;
        boolean trident = arrow instanceof ThrownTrident;
        CompoundTag data = arrow.getPersistentData();

        int aim = Talents.level(player, "archer_aim");
        int bolt = trident ? 0 : Talents.level(player, "archer_bolt");
        int longshot = trident ? 0 : Talents.level(player, "archer_longshot");
        int antiAir = trident ? 0 : Talents.level(player, "archer_antiair");
        double multiplier = 1;
        if (aim > 0 || bolt > 0 || longshot > 0 || antiAir > 0) {
            Vec3 origin = new Vec3(data.getDoubleOr(ORIGIN_X, target.getX()), data.getDoubleOr(ORIGIN_Y, target.getY()),
                    data.getDoubleOr(ORIGIN_Z, target.getZ()));
            multiplier = ArcherFormulas.damageMultiplier(
                    aim, aim > 0 ? Talents.value(player, "archer_aim", "per_level") : 0,
                    bolt, bolt > 0 ? Talents.value(player, "archer_bolt", "per_level") : 0, data.getBooleanOr(CROSSBOW, false),
                    longshot, longshot > 0 ? Talents.value(player, "archer_longshot", "per_level") : 0,
                    origin.distanceTo(target.getEyePosition()),
                    longshot > 0 ? Talents.value(player, "archer_longshot", "min_distance") : 0,
                    antiAir, antiAir > 0 ? Talents.value(player, "archer_antiair", "per_level") : 0,
                    !target.onGround() && !target.isInWater());
        }

        if (data.getBooleanOr(PIERCE, false)) {
            int hits = data.getIntOr(PIERCE_HITS, 0);
            if (hits >= 1) multiplier *= Talents.value(player, "archer_pierce", "second_hit_ratio");
            data.putInt(PIERCE_HITS, hits + 1);
        }
        event.setAmount((float) (event.getAmount() * multiplier));

        int marker = trident ? 0 : Talents.level(player, "archer_marker");
        if (marker > 0) {
            int ticks = (int) (marker * Talents.value(player, "archer_marker", "ticks_per_level"));
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0), player);
        }

        if (!trident) alchemy(arrow, player, target);
    }

    /** Efeito que a flecha aplica: tipo, duração (já escalada) e amplificador. */
    private record ArrowEffect(Holder<MobEffect> effect, int duration, int amplifier) {}

    /** archer_alchemy: no próximo tick (depois de a flecha aplicar os efeitos), estende os efeitos dela no alvo. */
    private static void alchemy(AbstractArrow arrow, ServerPlayer player, LivingEntity target) {
        int level = Talents.level(player, "archer_alchemy");
        if (level <= 0) return;
        List<ArrowEffect> effects = arrowEffects(arrow);
        if (effects.isEmpty()) return;
        double perLevel = Talents.value(player, "archer_alchemy", "per_level");
        NextTick.schedule(() -> {
            if (!target.isAlive()) return;
            for (ArrowEffect e : effects) {
                MobEffectInstance current = target.getEffect(e.effect());
                if (current == null || current.isInfiniteDuration() || current.getAmplifier() != e.amplifier()) continue;
                int duration = ArcherFormulas.alchemyDuration(current.getDuration(), e.duration(), level, perLevel);
                if (duration <= current.getDuration()) continue;
                target.addEffect(new MobEffectInstance(e.effect(), duration, current.getAmplifier(), current.isAmbient(),
                        current.isVisible(), current.showIcon()), player);
            }
        });
    }

    /** Efeitos com duração da flecha: os da poção (escalados como a vanilla) ou Brilho da espectral. */
    private static List<ArrowEffect> arrowEffects(AbstractArrow arrow) {
        List<ArrowEffect> effects = new ArrayList<>();
        if (arrow instanceof SpectralArrow spectral) {
            effects.add(new ArrowEffect(MobEffects.GLOWING, spectral.duration, 0));
        } else if (arrow instanceof Arrow) {
            ItemStack origin = arrow.getPickupItemStackOrigin();
            PotionContents contents = origin.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            float scale = origin.getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F);
            contents.forEachEffect(e -> {
                if (!e.getEffect().value().isInstantaneous()) {
                    effects.add(new ArrowEffect(e.getEffect(), e.getDuration(), e.getAmplifier()));
                }
            }, scale);
        }
        return effects;
    }

    /** archer_firework: foguete com dono jogador segurando besta. */
    private static float fireworkAmount(float amount, FireworkRocketEntity rocket) {
        if (!(rocket.getOwner() instanceof ServerPlayer player)) return amount;
        boolean holdingCrossbow = player.getMainHandItem().getItem() instanceof CrossbowItem
                || player.getOffhandItem().getItem() instanceof CrossbowItem;
        int level = holdingCrossbow ? Talents.level(player, "archer_firework") : 0;
        if (level <= 0) return amount;
        return (float) (amount * (1 + level * Talents.value(player, "archer_firework", "per_level")));
    }

    // ---- Recolhedor ------------------------------------------------------------------------------------------------

    /** archer_recover: ao acertar, rola a chance e guarda a flecha no alvo para dropar quando ele morrer. */
    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow) || arrow instanceof ThrownTrident) return;
        if (arrow.level().isClientSide() || arrow.pickup != AbstractArrow.Pickup.ALLOWED) return;
        if (!(arrow.getOwner() instanceof ServerPlayer player)) return;
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity target)) return;
        int level = Talents.level(player, "archer_recover");
        if (level <= 0) return;
        if (player.getRandom().nextDouble() >= HookFormulas.chance(level, Talents.value(player, "archer_recover", "per_level"))) return;

        var ops = player.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        ItemStack.CODEC.encodeStart(ops, arrow.getPickupItemStackOrigin().copyWithCount(1)).result().ifPresent(tag -> {
            ListTag stored = target.getPersistentData().getListOrEmpty(RECOVER);
            stored.add(tag);
            target.getPersistentData().put(RECOVER, stored);
        });
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) return;
        ListTag stored = entity.getPersistentData().getListOrEmpty(RECOVER);
        if (stored.isEmpty()) return;
        var ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        for (Tag tag : stored) {
            ItemStack.CODEC.parse(new Dynamic<>(ops, tag)).result().ifPresent(stack -> entity.spawnAtLocation(level, stack));
        }
        entity.getPersistentData().remove(RECOVER);
    }
}
