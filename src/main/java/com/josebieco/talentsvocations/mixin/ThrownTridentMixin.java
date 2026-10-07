package com.josebieco.talentsvocations.mixin;

import com.josebieco.talentsvocations.effect.hooks.TridentHooks;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** angler_loyalty: troca a aceleração de volta do tridente com Lealdade. */
@Mixin(ThrownTrident.class)
public abstract class ThrownTridentMixin {

    /**
     * Em tick, {@code double accel = 0.05 * loyalty} é a única variável local double do método (único DSTORE; o
     * {@code ldc2_w 0.05d} também é único); conferido com javap -c no Forge 66.0.9 / MC 26.3. Reconferir a cada
     * atualização.
     */
    @ModifyVariable(method = "tick", at = @At("STORE"), ordinal = 0)
    private double talentsvocations$returnAcceleration(double vanilla) {
        return TridentHooks.returnAcceleration((ThrownTrident) (Object) this, vanilla);
    }
}
