package com.seunome.vanillatalents.mixin;

import com.seunome.vanillatalents.effect.hooks.DurabilityHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/** miner_durability, farmer_hoe_care, explorer_glider: poupa pontos de durabilidade. */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Unique private static final ThreadLocal<Boolean> vanillatalents$reentry = ThreadLocal.withInitial(() -> false);

    @Inject(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"), cancellable = true)
    private void vanillatalents$reduceDamage(int amount, ServerLevel level, @Nullable ServerPlayer player,
                                             Consumer<ItemStack> onBreak, CallbackInfo ci) {
        if (player == null || vanillatalents$reentry.get()) return;
        ItemStack self = (ItemStack) (Object) this;
        int kept = DurabilityHooks.adjustDamage(player, self, amount);
        if (kept == amount) return;
        ci.cancel();
        if (kept > 0) {
            vanillatalents$reentry.set(true);
            try {
                self.hurtAndBreak(kept, level, player, onBreak);
            } finally {
                vanillatalents$reentry.set(false);
            }
        }
    }
}
