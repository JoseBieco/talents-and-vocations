package com.seunome.vanillatalents.client;

import com.seunome.vanillatalents.data.TalentRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.event.TickEvent;

import java.util.List;

/** Efeitos visuais do lado do cliente: partículas do Faro Mineral. */
public final class ClientEffects {

    private static final int DEFAULT_PARTICLE_TICKS = 60;
    private static final int SPAWN_INTERVAL = 5;

    private static List<BlockPos> highlighted = List.of();
    private static int ticksLeft;

    private ClientEffects() {}

    static void register() {
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> tick());
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
