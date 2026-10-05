package com.seunome.vanillatalents.client;

import com.seunome.vanillatalents.core.formula.ArcherFormulas;
import com.seunome.vanillatalents.data.TalentRegistries;
import com.seunome.vanillatalents.effect.Talents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.ClientInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.UseEffects;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;

import java.util.List;

/** Efeitos do lado do cliente: partículas do Faro Mineral e Atirar Andando. */
public final class ClientEffects {

    private static final int DEFAULT_PARTICLE_TICKS = 60;
    private static final int SPAWN_INTERVAL = 5;

    private static List<BlockPos> highlighted = List.of();
    private static int ticksLeft;

    private ClientEffects() {}

    static void register() {
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> tick());
        MovementInputUpdateEvent.BUS.addListener(ClientEffects::onMovementInput);
    }

    /** archer_mobile: devolve parte do input que a vanilla corta enquanto o arco é puxado. */
    private static void onMovementInput(MovementInputUpdateEvent event) {
        Player player = event.getEntity();
        if (!player.isUsingItem() || player.isPassenger()) return;
        ItemStack using = player.getUseItem();
        if (!(using.getItem() instanceof BowItem)) return;
        int level = Talents.level(player, "archer_mobile");
        if (level <= 0) return;
        float vanilla = using.getOrDefault(DataComponents.USE_EFFECTS, UseEffects.DEFAULT).speedMultiplier();
        if (vanilla <= 0 || vanilla >= 1) return;
        double factor = ArcherFormulas.mobileInputFactor(level, Talents.value(player, "archer_mobile", "per_level"), vanilla);
        ClientInput input = event.getInput();
        input.moveVector = input.moveVector.scale((float) factor);
    }

    static void highlight(List<BlockPos> positions) {
        highlighted = List.copyOf(positions);
        ticksLeft = TalentRegistries.client().get("miner_prospector")
                .map(n -> n.values().getOrDefault("particle_ticks", (double) DEFAULT_PARTICLE_TICKS).intValue())
                .orElse(DEFAULT_PARTICLE_TICKS);
    }

    private static void tick() {
        if (ticksLeft <= 0) return;
        ticksLeft--;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            ticksLeft = 0;
            return;
        }
        if (ticksLeft % SPAWN_INTERVAL != 0) return;
        for (BlockPos pos : highlighted) {
            level.addParticle(ParticleTypes.WAX_ON,
                    pos.getX() + level.getRandom().nextDouble(), pos.getY() + level.getRandom().nextDouble(),
                    pos.getZ() + level.getRandom().nextDouble(), 0, 0, 0);
        }
    }
}
