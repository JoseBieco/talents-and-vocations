package com.seunome.vanillatalents.effect.hooks;

import com.seunome.vanillatalents.core.formula.AnglerFormulas;
import com.seunome.vanillatalents.effect.Talents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;

/**
 * angler_boat_speed: chamado por AbstractBoatMixin no lugar da aceleração para frente (0.04F) de
 * {@code AbstractBoat.controlBoat}. A vanilla só chama controlBoat no cliente que controla o barco (o piloto), então
 * aqui {@link Talents#level} lê a visão do jogador local; num servidor dedicado o método nem é chamado.
 */
public final class BoatHooks {

    private BoatHooks() {}

    /**
     * Só acelera mais com o barco na superfície da água ({@code IN_WATER}); no gelo/terra ({@code ON_LAND}), no ar ou
     * submerso fica o vanilla. O status vem do campo privado do barco, lido pelo Mixin.
     */
    public static float forwardAcceleration(AbstractBoat boat, AbstractBoat.Status status, float vanilla) {
        if (status != AbstractBoat.Status.IN_WATER) return vanilla;
        if (!(boat.getControllingPassenger() instanceof Player player)) return vanilla;
        int level = Talents.level(player, "angler_boat_speed");
        if (level <= 0) return vanilla;
        return (float) AnglerFormulas.scaled(vanilla, level, Talents.value(player, "angler_boat_speed", "per_level"));
    }
}
