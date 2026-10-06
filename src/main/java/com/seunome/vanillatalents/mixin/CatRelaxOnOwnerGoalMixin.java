package com.seunome.vanillatalents.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.seunome.vanillatalents.effect.hooks.PetHooks;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * tamer_cat_gift: troca a chance do presente lida em {@code stop()}. A classe interna é privada, então o alvo vai por
 * nome. {@code EnvironmentAttributeSystem.getValue} é genérico: no bytecode devolve {@code Object} (seguido de
 * {@code checkcast Float}), por isso o handler recebe e devolve {@code Object}.
 */
@Mixin(targets = "net.minecraft.world.entity.animal.feline.Cat$CatRelaxOnOwnerGoal")
public abstract class CatRelaxOnOwnerGoalMixin {

    @Shadow private Cat cat;
    @Shadow private Player ownerPlayer;

    @ModifyExpressionValue(method = "stop",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/attribute/EnvironmentAttributeSystem;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;Lnet/minecraft/world/phys/Vec3;)Ljava/lang/Object;"))
    private Object vanillatalents$giftChance(Object original) {
        if (!(original instanceof Float vanilla)) return original;
        return PetHooks.catGiftChance(this.cat, this.ownerPlayer, vanilla);
    }
}
