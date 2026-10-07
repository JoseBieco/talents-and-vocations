package com.josebieco.talentsvocations.effect;

import com.josebieco.talentsvocations.TalentsVocations;
import com.josebieco.talentsvocations.core.RecursionGuard;
import com.josebieco.talentsvocations.core.formula.ArtisanFormulas;
import com.josebieco.talentsvocations.effect.hooks.EnchantHooks;
import com.josebieco.talentsvocations.effect.hooks.TradeHooks;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.enchanting.EnchantmentLevelSetEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Efeitos da árvore do Artífice por evento (bigorna, trocas, ferraria e lápis por Mixin ficam em effect/hooks: AnvilHooks,
 * TradeHooks, SmithingHooks, EnchantHooks).
 */
@Mod.EventBusSubscriber(modid = TalentsVocations.MODID)
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
            // Só efeitos benéficos/neutros: estender Veneno ou Fraqueza bebidos seria uma penalidade.
            if (!e.getEffect().value().isInstantaneous() && !e.isInfiniteDuration()
                    && e.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) {
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

    /**
     * artisan_bookshelf: o evento dispara em {@code EnchantmentMenu.slotsChanged} (só servidor) para cada linha, depois
     * do custo vanilla. Se outro mod já mudou o nível, não mexemos. Detalhes (como achar o jogador) em
     * {@link EnchantHooks#bookshelfLevel}.
     */
    @SubscribeEvent
    public static void onEnchantmentLevelSet(EnchantmentLevelSetEvent event) {
        if (event.getEnchantLevel() != event.getOriginalLevel()) return;
        event.setEnchantLevel(EnchantHooks.bookshelfLevel(event.getLevel(), event.getItem(), event.getEnchantRow(),
                event.getPower(), event.getEnchantLevel()));
    }

    /** artisan_insight: só com a mesa aberta; ver {@link EnchantHooks#tickInsight}. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (event.player() instanceof ServerPlayer player) EnchantHooks.tickInsight(player);
    }
}
