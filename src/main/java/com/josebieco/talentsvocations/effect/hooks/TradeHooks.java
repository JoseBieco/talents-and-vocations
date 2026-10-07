package com.josebieco.talentsvocations.effect.hooks;

import com.josebieco.talentsvocations.core.formula.ArtisanFormulas;
import com.josebieco.talentsvocations.effect.Talents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;

/** Trocas com aldeões do Artífice (artisan_trade, artisan_mentor). Só servidor. */
public final class TradeHooks {

    private TradeHooks() {}

    /**
     * artisan_trade: chamado pelo VillagerMixin em {@code Villager.updateSpecialPrices(Player)} depois dos descontos de
     * reputação e Herói da Vila e ANTES do envio das ofertas ao jogador que está trocando — o cliente recebe o preço já
     * com desconto. A vanilla zera os descontos ({@code resetSpecialPrices}) no começo de cada {@code updateSpecialPrices},
     * então nada se acumula; o preço final nunca cai abaixo de 1 ({@code MerchantOffer.getCostA} limita).
     */
    public static void applyDiscount(Villager villager, Player player) {
        if (villager.level().isClientSide() || !(player instanceof ServerPlayer)) return;
        int level = Talents.level(player, "artisan_trade");
        if (level <= 0) return;
        double per = Talents.value(player, "artisan_trade", "per_level");
        for (MerchantOffer offer : villager.getOffers()) {
            int discount = ArtisanFormulas.tradeDiscount(offer.getBaseCostA().getCount(), level, per);
            if (discount > 0) offer.addToSpecialPriceDiff(-discount);
        }
    }

    /**
     * artisan_mentor: XP de profissão extra para o aldeão depois de uma troca. {@code TradeWithVillagerEvent} dispara no
     * fim de {@code AbstractVillager.notifyTrade}, depois de {@code rewardTradeXp} (que já somou o XP da oferta, já subiu
     * de nível se preciso e já soltou o orbe do jogador). Se o bônus fizer o aldeão passar do limite do nível, sobe como a
     * vanilla em {@code rewardTradeXp} ({@code increaseMerchantCareer} + Regeneração 10 s), mas SEM os +5 de XP do orbe:
     * o XP do jogador nunca muda.
     */
    public static void mentor(ServerPlayer player, Villager villager, MerchantOffer offer) {
        int level = Talents.level(player, "artisan_mentor");
        if (level <= 0) return;
        int bonus = ArtisanFormulas.mentorBonus(offer.getXp(), level,
                Talents.value(player, "artisan_mentor", "per_level"), player.getRandom().nextDouble());
        if (bonus <= 0) return;
        villager.setVillagerXp(villager.getVillagerXp() + bonus);
        int villagerLevel = villager.getVillagerData().level();
        // Mesmo teste do privado Villager.shouldIncreaseLevel.
        if (VillagerData.canLevelUp(villagerLevel) && villager.getVillagerXp() >= VillagerData.getMaxXpPerLevel(villagerLevel)
                && villager.level() instanceof ServerLevel serverLevel) {
            villager.increaseMerchantCareer(serverLevel); // AT; reenvia as ofertas (novo nível) via updateTrades
            villager.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
        }
    }
}
