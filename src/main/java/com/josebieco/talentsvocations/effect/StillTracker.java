package com.josebieco.talentsvocations.effect;

import com.josebieco.talentsvocations.TalentsVocations;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Há quantos ticks cada jogador (servidor) está agachado e parado no mesmo lugar. Usado por miner_prospector e
 * archer_held_breath. O primeiro tick agachado conta 0; soltar o agachar ou se mover zera.
 */
@Mod.EventBusSubscriber(modid = TalentsVocations.MODID)
public final class StillTracker {

    private record Stillness(Vec3 pos, int ticks) {}

    private static final Map<UUID, Stillness> STILL = new HashMap<>();

    private StillTracker() {}

    /** Ticks seguidos agachado e parado (0 se não estiver agachado). */
    public static int stillTicks(Player player) {
        Stillness current = STILL.get(player.getUUID());
        return current == null ? 0 : current.ticks();
    }

    public static void forget(UUID player) {
        STILL.remove(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Pre event) {
        if (event.player() instanceof ServerPlayer player) update(player);
    }

    private static void update(ServerPlayer player) {
        if (!player.isShiftKeyDown()) {
            STILL.remove(player.getUUID());
            return;
        }
        Stillness previous = STILL.get(player.getUUID());
        Vec3 pos = player.position();
        int ticks = previous != null && previous.pos().distanceToSqr(pos) < 1.0E-4 ? previous.ticks() + 1 : 0;
        STILL.put(player.getUUID(), new Stillness(pos, ticks));
    }
}
