package com.seunome.vanillatalents.client;

import com.seunome.vanillatalents.core.formula.ArcherFormulas;
import com.seunome.vanillatalents.effect.Talents;
import com.seunome.vanillatalents.core.formula.ExplorerFormulas;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;

/**
 * Efeitos do lado do cliente: Atirar Andando, Comer Andando e Escalador (o Faro Mineral fica em OreHighlights).
 * O movimento do jogador local é calculado no cliente, então estes efeitos só funcionam aqui.
 */
public final class ClientEffects {

    private ClientEffects() {}

    static void register() {
        MovementInputUpdateEvent.BUS.addListener(ClientEffects::onMovementInput);
        TickEvent.PlayerTickEvent.Post.BUS.addListener(ClientEffects::onPlayerTick);
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
     * A vanilla reescreve o y para 0,2 a cada tick de subida, então o fator não acumula.
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
        player.setDeltaMovement(motion.x, motion.y * factor, motion.z);
    }
}
