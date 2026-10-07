package com.seunome.vanillatalents.effect.pet;

import com.seunome.vanillatalents.VanillaTalents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dono dos golems construídos (Global Constraints): o clique com abóbora esculpida ou lanterna de abóbora é gravado;
 * um golem de ferro {@code isPlayerCreated()} ou de neve que entra no mundo (não do disco) no mesmo tick ou no
 * seguinte, a até 8 blocos do bloco clicado, recebe o dono. Dispensador não dá dono (não há clique).
 * <p>Desvio intencional: a distância de 8 blocos é medida a partir do BLOCO CLICADO, não do jogador
 * (o golem nasce junto à abóbora, que fica ao lado do bloco clicado).
 */
@Mod.EventBusSubscriber(modid = VanillaTalents.MODID)
public final class GolemBuilders {

    static final double MAX_DISTANCE = 8.0;
    static final long MAX_TICKS = 1;

    private record Click(UUID player, long gameTime, ResourceKey<Level> dimension, BlockPos pos) {}

    private static final List<Click> RECENT = new ArrayList<>();

    private GolemBuilders() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isGolemHead(event.getItemStack())) return;
        long now = player.level().getGameTime();
        prune(now);
        RECENT.add(new Click(player.getUUID(), now, player.level().dimension(), event.getPos().immutable()));
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || event.getLevel().isClientSide()) return;
        Entity entity = event.getEntity();
        boolean built = (entity instanceof IronGolem iron && iron.isPlayerCreated()) || entity instanceof SnowGolem;
        if (!built || PetOwnership.hasGolemOwner(entity)) return;
        long now = event.getLevel().getGameTime();
        prune(now);
        builder(entity, now).ifPresent(owner -> {
            PetOwnership.setGolemOwner(entity, owner);
            if (entity instanceof LivingEntity golem) {
                PetOwnership.onlineOwner(golem).ifPresent(player -> {
                    PetSync.apply(golem, player);
                    golem.setHealth(golem.getMaxHealth()); // golem recém-construído nasce com a vida cheia
                });
            }
        });
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        RECENT.clear();
    }

    /** Logout: descarta os cliques gravados do jogador. */
    public static void forget(UUID player) {
        RECENT.removeIf(c -> c.player().equals(player));
    }

    private static Optional<UUID> builder(Entity golem, long now) {
        double maxSq = MAX_DISTANCE * MAX_DISTANCE;
        return RECENT.stream()
                .filter(c -> c.dimension().equals(golem.level().dimension()))
                .filter(c -> distanceSq(c, golem) <= maxSq)
                .min(Comparator.comparingDouble(c -> distanceSq(c, golem)))
                .map(Click::player);
    }

    private static double distanceSq(Click click, Entity golem) {
        return Vec3.atCenterOf(click.pos()).distanceToSqr(golem.position());
    }

    /** Mantém só os cliques deste tick e do anterior. */
    private static void prune(long now) {
        RECENT.removeIf(c -> now - c.gameTime() > MAX_TICKS || c.gameTime() > now);
    }

    private static boolean isGolemHead(ItemStack stack) {
        return stack.is(Items.CARVED_PUMPKIN) || stack.is(Items.JACK_O_LANTERN);
    }
}
