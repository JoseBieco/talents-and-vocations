package com.seunome.vanillatalents.effect;

import com.seunome.vanillatalents.capability.SkillAccess;
import com.seunome.vanillatalents.core.SkillView;
import com.seunome.vanillatalents.core.TalentNode;
import com.seunome.vanillatalents.core.TalentRegistry;
import com.seunome.vanillatalents.core.TalentRules;
import com.seunome.vanillatalents.data.TalentRegistries;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

/**
 * Leitura de nível efetivo e de {@code values} para os handlers. No servidor lê a capability; no cliente
 * (efeitos que o cliente precisa prever, como velocidade de quebra) lê o estado sincronizado.
 */
public final class Talents {

    private static volatile Supplier<SkillView> clientView;

    private Talents() {}

    /** Chamado pela inicialização do cliente. */
    public static void setClientView(Supplier<SkillView> view) {
        clientView = view;
    }

    /** Nível efetivo do nó para o jogador (0 se não tiver, se for de outra classe ou se o nó não existir). */
    public static int level(Player player, String nodeId) {
        if (player.level().isClientSide()) {
            Supplier<SkillView> view = clientView;
            return view == null ? 0 : TalentRules.effectiveLevel(view.get(), TalentRegistries.client(), nodeId);
        }
        return SkillAccess.get(player)
                .map(data -> TalentRules.effectiveLevel(data, TalentRegistries.server(), nodeId))
                .orElse(0);
    }

    /** Valor de balanceamento do JSON do lado do jogador; só chamar quando {@link #level} > 0. */
    public static double value(Player player, String nodeId, String key) {
        TalentRegistry registry = player.level().isClientSide() ? TalentRegistries.client() : TalentRegistries.server();
        TalentNode node = registry.get(nodeId)
                .orElseThrow(() -> new IllegalStateException("Talent node not loaded: " + nodeId));
        return node.value(key);
    }
}
