package com.seunome.vanillatalents.mixin;

import com.seunome.vanillatalents.effect.hooks.FishingHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.FishingHook;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** angler_lure e angler_high_tide: encurta o tempo até o peixe se aproximar. */
@Mixin(FishingHook.class)
public abstract class FishingHookMixin {

    @Shadow private int timeUntilLured;

    /**
     * PUTFIELD timeUntilLured em catchingFish, na ordem do bytecode: 0 = "= 0" (fim da mordida), 1 = "-= fishingSpeed",
     * 2 = "= nextInt(100, 600)", 3 = "= timeUntilLured - lureSpeed" (ramo else). Ordinal 3 + AFTER = valor sorteado
     * já com a Isca vanilla aplicada.
     */
    @Inject(method = "catchingFish",
            at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
                    target = "Lnet/minecraft/world/entity/projectile/FishingHook;timeUntilLured:I",
                    ordinal = 3, shift = At.Shift.AFTER))
    private void vanillatalents$lureTicks(BlockPos blockPos, CallbackInfo ci) {
        this.timeUntilLured = FishingHooks.lureTicks((FishingHook) (Object) this, this.timeUntilLured);
    }
}
