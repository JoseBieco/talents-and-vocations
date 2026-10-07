package com.seunome.vanillatalents.effect;

import com.seunome.vanillatalents.VanillaTalents;
import com.seunome.vanillatalents.core.formula.AnglerFormulas;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Anti-AFK do Pescador: game time do último tick em que cada jogador (servidor) girou a câmera ou andou na horizontal
 * sem estar montado (regra em {@link AnglerFormulas#isActivity}; deriva de barco e Y não contam).
 * Usado por angler_lure e pela parte de pesca de angler_high_tide.
 */
@Mod.EventBusSubscriber(modid = VanillaTalents.MODID)
public final class ActivityTracker {

    private record Activity(Vec3 pos, float yRot, float xRot, long lastActiveTick) {}

    private static final Map<UUID, Activity> ACTIVITY = new HashMap<>();

    private ActivityTracker() {}

    /** Último tick ativo; um jogador ainda sem registro conta como ativo agora. */
    public static long lastActive(ServerPlayer player) {
        Activity current = ACTIVITY.get(player.getUUID());
        return current == null ? player.level().getGameTime() : current.lastActiveTick();
    }

    public static void forget(UUID player) {
        ACTIVITY.remove(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (event.player() instanceof ServerPlayer player) update(player);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVITY.clear();
    }

    private static void update(ServerPlayer player) {
        long now = player.level().getGameTime();
        Vec3 pos = player.position();
        float yRot = player.getYRot();
        float xRot = player.getXRot();
        Activity previous = ACTIVITY.get(player.getUUID());
        boolean active = previous == null || AnglerFormulas.isActivity(
                pos.x - previous.pos().x, pos.z - previous.pos().z,
                yRot - previous.yRot(), xRot - previous.xRot(), player.isPassenger());
        ACTIVITY.put(player.getUUID(), new Activity(pos, yRot, xRot, active ? now : previous.lastActiveTick()));
    }
}
