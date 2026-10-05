package com.seunome.vanillatalents.effect;

import com.seunome.vanillatalents.capability.SkillAccess;
import com.seunome.vanillatalents.core.TalentNode;
import com.seunome.vanillatalents.core.TalentRules;
import com.seunome.vanillatalents.data.TalentRegistries;
import net.minecraft.world.entity.player.Player;

/** Leitura de nível efetivo e de {@code values} para os handlers de servidor. */
public final class Talents {

    private Talents() {}

    /** Nível efetivo do nó para o jogador (0 se não tiver, se for de outra classe ou se o nó não existir). */
    public static int level(Player player, String nodeId) {
        return SkillAccess.get(player)
                .map(data -> TalentRules.effectiveLevel(data, TalentRegistries.server(), nodeId))
                .orElse(0);
    }

    /** Valor de balanceamento do JSON; só chamar quando {@link #level} > 0. */
    public static double value(String nodeId, String key) {
        TalentNode node = TalentRegistries.server().get(nodeId)
                .orElseThrow(() -> new IllegalStateException("Talent node not loaded: " + nodeId));
        return node.value(key);
    }
}
