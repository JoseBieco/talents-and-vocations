package com.seunome.vanillatalents.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.seunome.vanillatalents.effect.hooks.EnchantHooks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Economia de Lápis: envolve {@code currency.consume(enchantmentCost, player)}, que fica no lambda passado a
 * {@code access.execute} em {@code clickMenuButton}. O alvo vai por regex porque o número do lambda sintético depende
 * do compilador ({@code lambda$clickMenuButton$0} no jar do Forge 26.3-66.0.9); o {@code ItemStack.consume} só aparece
 * nesse lambda (conferido com {@code javap -c}).
 */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {

    @WrapOperation(method = "/^lambda\\$clickMenuButton\\$\\d+$/",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;consume(ILnet/minecraft/world/entity/LivingEntity;)V"))
    private void vanillatalents$lapis(ItemStack currency, int amount, LivingEntity owner, Operation<Void> original) {
        if (EnchantHooks.keepLapis(owner)) return;
        original.call(currency, amount, owner);
    }
}
