package com.josebieco.talentsvocations.client;

import com.josebieco.talentsvocations.core.formula.AnglerFormulas;
import com.josebieco.talentsvocations.core.formula.ArcherFormulas;
import com.josebieco.talentsvocations.core.formula.BuilderFormulas;
import com.josebieco.talentsvocations.effect.Talents;
import com.josebieco.talentsvocations.core.formula.ExplorerFormulas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;

/**
 * Efeitos do lado do cliente: Atirar Andando, Comer Andando, Escalador, Andaimista (subida; a descida fica em
 * ScaffoldHooks), Olhos do Mar (o Faro Mineral fica em OreHighlights) e Mãos Rápidas.
 * O movimento do jogador local é calculado no cliente, então estes efeitos só funcionam aqui.
 */
public final class ClientEffects {

    private ClientEffects() {}

    static void register() {
        MovementInputUpdateEvent.BUS.addListener(ClientEffects::onMovementInput);
        TickEvent.PlayerTickEvent.Post.BUS.addListener(ClientEffects::onPlayerTick);
        TickEvent.PlayerTickEvent.Post.BUS.addListener(ClientEffects::onScaffoldTick);
        ViewportEvent.RenderFog.BUS.addListener(ClientEffects::onRenderFog);
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> onQuickHandsTick());
    }

    /**
     * builder_quick_hands: encurta Minecraft.rightClickDelay (AT) com bloco na mão. Em Minecraft.tick a ordem é:
     * decremento do atraso → ClientTickEvent.Pre → handleKeybinds (startUseItem põe 4) → ClientTickEvent.Post. No Post o
     * valor ainda é o 4 recém-posto, então trocá-lo por delay_ticks dá exatamente esse intervalo entre colocações (no Pre
     * o jogo já teria descontado um tick: 3 → 2 daria intervalo 3). Quando vale: {@link BuilderFormulas#quickHandsHolding}
     * (mirando um bloco, e nenhuma mão que a vanilla possa tentar segura pérola, ovo, bola de neve ou comida).
     */
    private static void onQuickHandsTick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        if (Talents.level(player, "builder_quick_hands") <= 0) return;
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        boolean aimingAtBlock = minecraft.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK;
        boolean holdingBlock = BuilderFormulas.quickHandsHolding(aimingAtBlock,
                main.isEmpty(), main.getItem() instanceof BlockItem, off.isEmpty(), off.getItem() instanceof BlockItem);
        int delay = (int) Talents.value(player, "builder_quick_hands", "delay_ticks");
        minecraft.rightClickDelay = BuilderFormulas.quickDelay(minecraft.rightClickDelay, holdingBlock, delay);
    }

    /**
     * archer_mobile (arco) e explorer_eat_move (comer/beber): devolve parte do input que a vanilla corta enquanto o
     * item está em uso.
     */
    private static void onMovementInput(MovementInputUpdateEvent event) {
        Player player = event.getEntity();
        if (!player.isUsingItem() || player.isPassenger()) return;
        ItemStack using = player.getUseItem();
        int level;
        double perLevel;
        if (using.getItem() instanceof BowItem) {
            level = Talents.level(player, "archer_mobile");
            if (level <= 0) return;
            perLevel = Talents.value(player, "archer_mobile", "per_level");
        } else if (isEatOrDrink(using.getUseAnimation())) {
            if (Talents.level(player, "explorer_eat_move") <= 0) return;
            level = 1;
            perLevel = Talents.value(player, "explorer_eat_move", "value");
        } else {
            return;
        }
        float vanilla = using.getOrDefault(DataComponents.USE_EFFECTS, UseEffects.DEFAULT).speedMultiplier();
        if (vanilla <= 0 || vanilla >= 1) return;
        double factor = ArcherFormulas.mobileInputFactor(level, perLevel, vanilla);
        ClientInput input = event.getInput();
        input.moveVector = input.moveVector.scale((float) factor);
    }

    private static boolean isEatOrDrink(ItemUseAnimation animation) {
        return animation == ItemUseAnimation.EAT || animation == ItemUseAnimation.DRINK;
    }

    /**
     * explorer_climb: depois do tick do jogador local, subindo uma escada de mão ou trepadeira (tag
     * {@code #minecraft:climbable}, sem o andaime), multiplica a velocidade vertical usada no próximo tick.
     * A vanilla reescreve o y para 0,2 a cada tick de subida, então o fator não acumula — desde que essa reescrita
     * aconteça (colisão horizontal ou pulo), fora d'água e sem Levitação: ver {@link #climbBoost}.
     */
    private static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof LocalPlayer player)) return;
        if (player.getAbilities().flying || !player.onClimbable()) return;
        Vec3 motion = player.getDeltaMovement();
        if (motion.y <= 0) return;
        BlockState state = player.getInBlockState();
        if (!state.is(BlockTags.CLIMBABLE) || state.isScaffolding(player)) return;
        int level = Talents.level(player, "explorer_climb");
        if (level <= 0) return;
        double factor = ExplorerFormulas.climbFactor(level, Talents.value(player, "explorer_climb", "per_level"));
        player.setDeltaMovement(motion.x, climbBoost(player, motion.y, factor), motion.z);
    }

    /**
     * builder_scaffold (subida): depois do tick do jogador local, dentro do andaime e subindo, multiplica a velocidade
     * vertical usada no próximo tick. Subindo (pulo ou colisão horizontal) a vanilla reescreve o y para 0,2 logo depois
     * do move, então o fator não acumula; fora dessa condição (levitação, água, lançado para cima) ele não se aplica
     * ({@link #climbBoost}). Não colide com explorer_climb, que exclui o andaime. A descida não dá para
     * fazer aqui: o próximo tick prende o y em −0,15 antes do move (LivingEntity.handleOnClimbable), desfazendo
     * qualquer fator; ela fica em ScaffoldHooks (mixin nesse método).
     */
    private static void onScaffoldTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof LocalPlayer player)) return;
        if (player.getAbilities().flying) return;
        Vec3 motion = player.getDeltaMovement();
        if (motion.y <= 0) return;
        if (!player.getInBlockState().isScaffolding(player)) return;
        int level = Talents.level(player, "builder_scaffold");
        if (level <= 0) return;
        double factor = BuilderFormulas.scaffoldFactor(level, Talents.value(player, "builder_scaffold", "per_level"));
        player.setDeltaMovement(motion.x, climbBoost(player, motion.y, factor), motion.z);
    }

    /**
     * Condição da reescrita vanilla do y (LivingEntity.handleRelativeFrictionAndCalculateMovement:
     * {@code (horizontalCollision || jumping) && onClimbable()}; no jogador local {@code jumping} vem da tecla de pulo)
     * passada à regra pura ExplorerFormulas.climbBoost.
     */
    private static double climbBoost(LocalPlayer player, double y, double factor) {
        boolean reset = player.horizontalCollision || player.input.keyPresses.jump();
        return ExplorerFormulas.climbBoost(y, factor, reset, player.isInWater(), player.hasEffect(MobEffects.LEVITATION));
    }

    /**
     * angler_sea_eyes: debaixo d'água (FogType.WATER), afasta o começo e o fim da neblina por scaled(1, nível,
     * per_level). Na 26.3 a neblina d'água vem dos planos "environmental" do FogData (WaterFogEnvironment, padrão
     * 96 blocos), não dos planos da distância de renderização (getNear/FarPlaneDistance), que ficam como estão para
     * não revelar a borda dos chunks. skyEnd/cloudEnd acompanham, como na vanilla. O Forge aplica as mudanças no
     * FogData sem precisar cancelar o evento.
     */
    private static void onRenderFog(ViewportEvent.RenderFog event) {
        if (event.getType() != FogType.WATER) return;
        if (!(event.getCamera().entity() instanceof LocalPlayer player)) return;
        int level = Talents.level(player, "angler_sea_eyes");
        if (level <= 0) return;
        float factor = (float) AnglerFormulas.scaled(1, level, Talents.value(player, "angler_sea_eyes", "per_level"));
        FogData data = event.getData();
        data.environmentalStart *= factor;
        data.environmentalEnd *= factor;
        data.skyEnd *= factor;
        data.cloudEnd *= factor;
    }
}
