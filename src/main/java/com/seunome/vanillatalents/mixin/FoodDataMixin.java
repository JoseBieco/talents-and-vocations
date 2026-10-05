package com.seunome.vanillatalents.mixin;

import com.seunome.vanillatalents.effect.hooks.ExhaustionHooks;
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

    @Unique private ServerPlayer vanillatalents$player;

    @Inject(method = "tick", at = @At("HEAD"))
    private void vanillatalents$beforeTick(ServerPlayer player, CallbackInfo ci) {
        this.vanillatalents$player = player;
        if (this.foodLevel >= 18 && player.isHurt() && ExhaustionHooks.extraRegenTick(player)) {
            this.tickTimer++;
        }
    }

    /** As duas chamadas a addExhaustion dentro de tick são da regeneração natural. */
    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;addExhaustion(F)V"))
    private float vanillatalents$regenExhaustion(float amount) {
        ServerPlayer player = this.vanillatalents$player;
        return player == null ? amount : amount * ExhaustionHooks.regenMultiplier(player);
    }
}
