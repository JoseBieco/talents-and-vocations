package com.seunome.vanillatalents.effect.hooks;

import com.seunome.vanillatalents.core.formula.AnglerFormulas;
import com.seunome.vanillatalents.effect.Talents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;

/**
 * angler_loyalty: chamado por ThrownTridentMixin no lugar de {@code accel = 0.05 · loyalty} (aceleração de volta do
 * tridente com Lealdade) em {@code ThrownTrident.tick}.
 *
 * <p>Lado: tick roda nos dois lados. O servidor é autoritativo (manda posição/velocidade ao cliente), então lá vale
 * para qualquer dono jogador com o nó. No cliente, {@link Talents#level} só conhece a árvore do jogador local; por isso
 * o cliente só acelera o tridente do próprio jogador local (prevê igual ao servidor, sem tranco) e deixa os de outros
 * jogadores vanilla, corrigidos pelas atualizações do servidor.
 */
public final class TridentHooks {

    private TridentHooks() {}

    public static double returnAcceleration(ThrownTrident trident, double vanilla) {
        if (!(trident.getOwner() instanceof Player owner)) return vanilla;
        if (trident.level().isClientSide() && !owner.isLocalPlayer()) return vanilla;
        int level = Talents.level(owner, "angler_loyalty");
        if (level <= 0) return vanilla;
        return AnglerFormulas.scaled(vanilla, level, Talents.value(owner, "angler_loyalty", "per_level"));
    }
}
