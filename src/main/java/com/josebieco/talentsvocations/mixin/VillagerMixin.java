package com.josebieco.talentsvocations.mixin;

import com.josebieco.talentsvocations.effect.hooks.TradeHooks;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Freguês Fiel: desconto nas ofertas em {@code Villager.updateSpecialPrices(Player)}.
 * <p>
 * Não é {@code TAIL}: em 26.3 o próprio {@code updateSpecialPrices} termina enviando as ofertas
 * ({@code sendMerchantOffers}) ao jogador que já está trocando (reabastecimento, subida de nível, fofoca, reputação).
 * O ponto é logo ANTES da única chamada {@code this.getTradingPlayer()} do método, que vem depois do laço de
 * reputação/Herói da Vila e antes do envio — assim o cliente sempre recebe o preço com desconto. Na abertura
 * ({@code startTrading}) o jogador ainda não é o {@code tradingPlayer} e as ofertas vão logo depois em
 * {@code openTradingScreen}, também já com desconto.
 * ATENÇÃO: reconferir com {@code javap -c Villager} a cada atualização (hoje: um único invokevirtual getTradingPlayer).
 */
@Mixin(Villager.class)
public abstract class VillagerMixin {

    @Inject(method = "updateSpecialPrices",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/npc/villager/Villager;getTradingPlayer()Lnet/minecraft/world/entity/player/Player;"))
    private void talentsvocations$applyTradeDiscount(Player player, CallbackInfo ci) {
        TradeHooks.applyDiscount((Villager) (Object) this, player);
    }
}
