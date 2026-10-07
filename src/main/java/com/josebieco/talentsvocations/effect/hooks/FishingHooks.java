package com.josebieco.talentsvocations.effect.hooks;

import com.josebieco.talentsvocations.Config;
import com.josebieco.talentsvocations.core.formula.AnglerFormulas;
import com.josebieco.talentsvocations.effect.ActivityTracker;
import com.josebieco.talentsvocations.effect.AnglerEffects;
import com.josebieco.talentsvocations.effect.Talents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;

/** Tempo de fisgada sem evento no Forge: chamado por FishingHookMixin logo depois de sortear timeUntilLured. */
public final class FishingHooks {

    private FishingHooks() {}

    /**
     * angler_lure e a parte de pesca de angler_high_tide. Só valem com dono {@link ServerPlayer} que se mexeu ou girou
     * a câmera nos últimos {@code anglerIdleTicks}; senão devolve o valor vanilla.
     */
    public static int lureTicks(FishingHook hook, int vanillaTicks) {
        if (!(hook.getPlayerOwner() instanceof ServerPlayer player)) return vanillaTicks;
        int lure = Talents.level(player, "angler_lure");
        boolean highTide = AnglerEffects.highTideActive(player);
        if (lure <= 0 && !highTide) return vanillaTicks;
        if (!AnglerFormulas.active(ActivityTracker.lastActive(player), player.level().getGameTime(),
                Config.ANGLER_IDLE_TICKS.get())) return vanillaTicks;
        double lurePer = lure > 0 ? Talents.value(player, "angler_lure", "per_level") : 0;
        double reduction = highTide ? Talents.value(player, "angler_high_tide", "lure_reduction") : 0;
        return AnglerFormulas.lureTicks(vanillaTicks, lure, lurePer, highTide, reduction);
    }
}
