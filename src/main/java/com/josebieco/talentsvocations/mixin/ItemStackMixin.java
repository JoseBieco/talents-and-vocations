package com.josebieco.talentsvocations.mixin;

import com.josebieco.talentsvocations.effect.hooks.DurabilityHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Durabilidade (R4): poupa pontos em hurtAndBreak. Itens de jogador (miner_durability, farmer_hoe_care, explorer_glider,
 * common_armor_care, angler_rod_care) pela sobrecarga com ServerPlayer; itens usados por outras entidades
 * (tamer_wolf_armor) pela sobrecarga com LivingEntity, que repassa {@code null} como jogador.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Unique private static final ThreadLocal<Boolean> talentsvocations$reentry = ThreadLocal.withInitial(() -> false);

    @Inject(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"), cancellable = true)
    private void talentsvocations$reduceDamage(int amount, ServerLevel level, @Nullable ServerPlayer player,
                                             Consumer<ItemStack> onBreak, CallbackInfo ci) {
        if (player == null || talentsvocations$reentry.get()) return;
        ItemStack self = (ItemStack) (Object) this;
        int kept = DurabilityHooks.adjustDamage(player, self, amount);
        if (kept == amount) return;
        ci.cancel();
        if (kept > 0) {
            talentsvocations$reentry.set(true);
            try {
                self.hurtAndBreak(kept, level, player, onBreak);
            } finally {
                talentsvocations$reentry.set(false);
            }
        }
    }

    /**
     * Sobrecarga usada pela armadura de lobo (Wolf.actuallyHurt / hurtArmor). Para jogadores ela repassa o jogador à
     * sobrecarga acima, que já trata o item; aqui só entram as demais entidades, sem dupla aplicação.
     */
    @Inject(method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V",
            at = @At("HEAD"), cancellable = true)
    private void talentsvocations$reduceWornDamage(int amount, LivingEntity owner, EquipmentSlot slot, CallbackInfo ci) {
        if (owner instanceof Player || talentsvocations$reentry.get()) return;
        ItemStack self = (ItemStack) (Object) this;
        int kept = DurabilityHooks.adjustWornDamage(owner, self, slot, amount);
        if (kept == amount) return;
        ci.cancel();
        if (kept > 0) {
            talentsvocations$reentry.set(true);
            try {
                self.hurtAndBreak(kept, owner, slot);
            } finally {
                talentsvocations$reentry.set(false);
            }
        }
    }
}
