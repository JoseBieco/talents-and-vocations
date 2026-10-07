package com.josebieco.talentsvocations.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.josebieco.talentsvocations.effect.hooks.ScaffoldHooks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** builder_scaffold: velocidade de descida no andaime, depois do limite de −0,15 de {@code handleOnClimbable}. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @ModifyReturnValue(method = "handleOnClimbable", at = @At("RETURN"))
    private Vec3 talentsvocations$scaffoldDescent(Vec3 original) {
        return ScaffoldHooks.climbDelta((LivingEntity) (Object) this, original);
    }
}
