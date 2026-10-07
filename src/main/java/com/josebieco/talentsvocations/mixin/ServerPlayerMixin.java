package com.josebieco.talentsvocations.mixin;

import com.josebieco.talentsvocations.effect.hooks.ExhaustionHooks;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** explorer_sprint e explorer_jump: exaustão de correr e de pular. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    /** Ordinal 3 = ramo "no chão e correndo" de checkMovementStatistics (0,1,2 = nadando/submerso/na água). */
    @ModifyArg(method = "checkMovementStatistics",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V", ordinal = 3))
    private float talentsvocations$sprintExhaustion(float amount) {
        return amount * ExhaustionHooks.sprintMultiplier((ServerPlayer) (Object) this);
    }

    @ModifyArg(method = "jumpFromGround",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V"))
    private float talentsvocations$jumpExhaustion(float amount) {
        return amount * ExhaustionHooks.jumpMultiplier((ServerPlayer) (Object) this);
    }
}
