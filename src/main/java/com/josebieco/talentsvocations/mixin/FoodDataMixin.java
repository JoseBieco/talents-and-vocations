package com.josebieco.talentsvocations.mixin;

import com.josebieco.talentsvocations.effect.hooks.ExhaustionHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** common_regen (temporizador mais rápido) e common_saturation (menos exaustão ao regenerar). */
@Mixin(FoodData.class)
public abstract class FoodDataMixin {

    @Shadow private int tickTimer;
    @Shadow private int foodLevel;

    @Unique private ServerPlayer talentsvocations$player;

    @Inject(method = "tick", at = @At("HEAD"))
    private void talentsvocations$beforeTick(ServerPlayer player, CallbackInfo ci) {
        this.talentsvocations$player = player;
        if (this.foodLevel >= 18 && player.isHurt() && ExhaustionHooks.extraRegenTick(player)) {
            this.tickTimer++;
        }
    }

    /** As duas chamadas a addExhaustion dentro de tick são da regeneração natural. */
    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;addExhaustion(F)V"))
    private float talentsvocations$regenExhaustion(float amount) {
        ServerPlayer player = this.talentsvocations$player;
        return player == null ? amount : amount * ExhaustionHooks.regenMultiplier(player);
    }
}
