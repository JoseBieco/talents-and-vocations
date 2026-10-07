package com.seunome.vanillatalents.effect.hooks;

import com.seunome.vanillatalents.core.formula.HookFormulas;
import com.seunome.vanillatalents.effect.Talents;
import net.minecraft.server.level.ServerPlayer;

/** Exaustão de fome sem evento no Forge: chamados por FoodDataMixin e ServerPlayerMixin. */
public final class ExhaustionHooks {

    private ExhaustionHooks() {}

    /** common_saturation: fator sobre a exaustão da regeneração natural. */
    public static float regenMultiplier(ServerPlayer player) {
        return multiplier(player, "common_saturation");
    }

    /** explorer_sprint: fator sobre a exaustão de correr. */
    public static float sprintMultiplier(ServerPlayer player) {
        return multiplier(player, "explorer_sprint");
    }

    /** explorer_jump: fator sobre a exaustão de pular. */
    public static float jumpMultiplier(ServerPlayer player) {
        return multiplier(player, "explorer_jump");
    }

    /** common_regen: chance, por tick de regeneração, de avançar o temporizador em +1 (cura mais rápida). */
    public static boolean extraRegenTick(ServerPlayer player) {
        int level = Talents.level(player, "common_regen");
        if (level <= 0) return false;
        return player.getRandom().nextDouble() < HookFormulas.chance(level, Talents.value(player, "common_regen", "per_level"));
    }

    private static float multiplier(ServerPlayer player, String nodeId) {
        int level = Talents.level(player, nodeId);
        if (level <= 0) return 1f;
        return (float) HookFormulas.reductionMultiplier(level, Talents.value(player, nodeId, "per_level"));
    }
}
