package com.seunome.vanillatalents.mixin;

import com.seunome.vanillatalents.effect.hooks.SmithingHooks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ferreiro: rola a devolução do molde antes de {@code SmithingMenu.onTake} consumir as entradas. */
@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin {

    @Inject(method = "onTake", at = @At("HEAD"))
    private void vanillatalents$beforeTake(Player player, ItemStack carried, CallbackInfo ci) {
        SmithingHooks.beforeTake((SmithingMenu) (Object) this, player);
    }
}
