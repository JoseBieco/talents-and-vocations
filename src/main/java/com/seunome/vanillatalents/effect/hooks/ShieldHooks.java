package com.seunome.vanillatalents.effect.hooks;

import com.seunome.vanillatalents.core.formula.HookFormulas;
import com.seunome.vanillatalents.effect.Talents;
import net.minecraft.world.entity.player.Player;

/** Tempo de escudo desativado por machado: chamado por PlayerMixin em blockUsingItem. */
public final class ShieldHooks {

    private ShieldHooks() {}

    /** warrior_shield: fator sobre os segundos de desativação. */
    public static float disableTicksMultiplier(Player player) {
        int level = Talents.level(player, "warrior_shield");
        if (level <= 0) return 1f;
        return (float) HookFormulas.reductionMultiplier(level, Talents.value(player, "warrior_shield", "per_level"));
    }
}
