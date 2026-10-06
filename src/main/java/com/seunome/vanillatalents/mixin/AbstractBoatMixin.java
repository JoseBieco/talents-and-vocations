package com.seunome.vanillatalents.mixin;

import com.seunome.vanillatalents.effect.hooks.BoatHooks;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** angler_boat_speed: troca só a aceleração para frente do barco. */
@Mixin(AbstractBoat.class)
public abstract class AbstractBoatMixin {

    @Shadow private AbstractBoat.Status status;

    /**
     * Em controlBoat, {@code acceleration += 0.04F} (tecla para frente) é o único {@code ldc 0.04f} do método
     * (os outros termos são 0.005F); conferido com javap -c no Forge 66.0.9 / MC 26.3. Reconferir a cada atualização.
     */
    @ModifyConstant(method = "controlBoat", constant = @Constant(floatValue = 0.04F))
    private float vanillatalents$forwardAcceleration(float vanilla) {
        return BoatHooks.forwardAcceleration((AbstractBoat) (Object) this, this.status, vanilla);
    }
}
