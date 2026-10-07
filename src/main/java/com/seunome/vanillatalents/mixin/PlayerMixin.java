package com.seunome.vanillatalents.mixin;

import com.seunome.vanillatalents.effect.hooks.ShieldHooks;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** warrior_shield: reduz os segundos de escudo desativado após golpe de machado. */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @ModifyArg(method = "blockUsingItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/BlocksAttacks;disable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/item/ItemStack;)V"),
            index = 2)
    private float vanillatalents$shieldDisableSeconds(float seconds) {
        return seconds * ShieldHooks.disableTicksMultiplier((Player) (Object) this);
    }
}
