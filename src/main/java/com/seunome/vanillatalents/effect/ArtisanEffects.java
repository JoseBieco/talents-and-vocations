package com.seunome.vanillatalents.effect;

import com.seunome.vanillatalents.VanillaTalents;
import com.seunome.vanillatalents.core.RecursionGuard;
import com.seunome.vanillatalents.core.formula.ArtisanFormulas;
import com.seunome.vanillatalents.effect.hooks.TradeHooks;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Efeitos da árvore do Artífice por evento (bigorna, trocas e ferraria por Mixin ficam em effect/hooks: AnvilHooks,
 * TradeHooks, SmithingHooks).
 */
@Mod.EventBusSubscriber(modid = VanillaTalents.MODID)
public final class ArtisanEffects {

    /** Folga (ticks) entre a duração aplicada pela poção e a lida no fim do tick. */
    private static final int POTION_TOLERANCE = 2;

    private ArtisanEffects() {}

    /**
     * artisan_anvil_care: menos chance de a bigorna se degradar. O evento dispara em AnvilMenu.onTake nos dois lados,
     * mas só o servidor degrada o bloco.
     */
    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int level = Talents.level(player, "artisan_anvil_care");
        if (level <= 0) return;
        event.setBreakChance(ArtisanFormulas.breakChance(event.getBreakChance(), level,
                Talents.value(player, "artisan_anvil_care", "per_level")));
    }

    /**
     * artisan_mentor: o evento só dispara no servidor, no fim de notifyTrade (depois de rewardTradeXp). Só aldeões
     * (o mercador ambulante não tem XP de profissão).
     */
    @SubscribeEvent
    public static void onTrade(TradeWithVillagerEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getAbstractVillager() instanceof Villager villager)) return;
        TradeHooks.mentor(player, villager, event.getMerchantOffer());
    }

    /** Efeito com duração que a poção aplicou: tipo, duração (já escalada) e amplificador. */
    private record PotionEffect(Holder<MobEffect> effect, int duration, int amplifier) {}

    /**
     * artisan_brewing: poção BEBIDA (só {@code Items.POTION}: arremessáveis, persistentes, flechas e sinalizadores não
     * passam por aqui). Finish dispara depois de {@code finishUsingItem}, com a cópia da poção, então os efeitos já foram
     * aplicados. No fim do tick do servidor, cada efeito da poção que o jogador tem com o mesmo amplificador e duração
     * até a da poção (ou seja, veio dela) passa a durar {@code potionDuration(duração atual)}.
     * <p>
     * Ordem com common_antidote/common_willpower: o gole acontece dentro de {@code Player.tick} (LivingEntity.tick →
     * updatingUsingItem), então o {@code PlayerTickEvent.Post} do Antídoto já reduziu o efeito quando o NextTick roda
     * ({@code ServerTickEvent.Post}); a extensão incide sobre a duração reduzida. A reaplicação ({@code addEffect}, que
     * dispara {@code MobEffectEvent.Added} de novo) roda sob o guard do Antídoto para ele não reduzir duas vezes. Este
     * handler não ouve Added, então não há laço do lado do Alquimista.
     */
    @SubscribeEvent
    public static void onFinishUsing(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItem();
        if (!stack.is(Items.POTION)) return;
        int level = Talents.level(player, "artisan_brewing");
        if (level <= 0) return;
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return;
        float scale = stack.getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F);
        List<PotionEffect> effects = new ArrayList<>();
        contents.forEachEffect(e -> {
            if (!e.getEffect().value().isInstantaneous() && !e.isInfiniteDuration()) {
                effects.add(new PotionEffect(e.getEffect(), e.getDuration(), e.getAmplifier()));
            }
        }, scale);
        if (effects.isEmpty()) return;
        double per = Talents.value(player, "artisan_brewing", "per_level");
        MinecraftServer server = player.level().getServer();
        UUID id = player.getUUID();
        NextTick.schedule(() -> {
            ServerPlayer current = server.getPlayerList().getPlayer(id);
            if (current == null || !current.isAlive()) return;
            RecursionGuard.SERVER.runGuarded(id, CommonEffects.ANTIDOTE_GUARD, () -> {
                for (PotionEffect e : effects) {
                    MobEffectInstance active = current.getEffect(e.effect());
                    if (active == null || active.isInfiniteDuration() || active.getAmplifier() != e.amplifier()) continue;
                    if (active.getDuration() > e.duration() + POTION_TOLERANCE) continue; // um mais longo já existia
                    int duration = ArtisanFormulas.potionDuration(active.getDuration(), level, per);
                    if (duration <= active.getDuration()) continue;
                    current.addEffect(new MobEffectInstance(e.effect(), duration, active.getAmplifier(), active.isAmbient(),
                            active.isVisible(), active.showIcon()), null);
                }
            });
        });
    }
}
