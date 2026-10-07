package com.josebieco.talentsvocations.effect.pet;

import com.josebieco.talentsvocations.core.PetKind;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * "Seus pets" (Global Constraints): TamableAnimal domado e AbstractHorse domado com dono; golem de ferro ou de
 * neve construído pelo jogador, com o dono gravado em {@code getPersistentData()} sob {@link #OWNER_KEY}.
 * Gado não é pet.
 */
public final class PetOwnership {

    public static final String OWNER_KEY = "talentsvocations:owner";

    private PetOwnership() {}

    public static Optional<UUID> ownerId(Entity entity) {
        if (entity instanceof TamableAnimal tamable) {
            return tamable.isTame() ? uuid(tamable.getOwnerReference()) : Optional.empty();
        }
        if (entity instanceof AbstractHorse horse) {
            return horse.isTamed() ? uuid(horse.getOwnerReference()) : Optional.empty();
        }
        if (entity instanceof IronGolem || entity instanceof SnowGolem) {
            return entity.getPersistentData().getString(OWNER_KEY).flatMap(PetOwnership::parse);
        }
        return Optional.empty();
    }

    /** Tipo do pet; vazio se a entidade não é pet de ninguém. */
    public static Optional<PetKind> kind(Entity entity) {
        if (ownerId(entity).isEmpty()) return Optional.empty();
        if (entity instanceof Wolf) return Optional.of(PetKind.WOLF);
        if (entity instanceof TamableAnimal) return Optional.of(PetKind.OTHER_TAMABLE);
        if (entity instanceof AbstractHorse) return Optional.of(PetKind.MOUNT);
        if (entity instanceof IronGolem) return Optional.of(PetKind.IRON_GOLEM);
        if (entity instanceof SnowGolem) return Optional.of(PetKind.SNOW_GOLEM);
        return Optional.empty();
    }

    public static boolean isPetOf(Entity entity, Player player) {
        return entity instanceof LivingEntity && ownerId(entity).filter(player.getUUID()::equals).isPresent();
    }

    /** Dono online (lista de jogadores do servidor por UUID); vazio se offline, sem dono ou no cliente. */
    public static Optional<ServerPlayer> onlineOwner(Entity entity) {
        MinecraftServer server = entity.level().getServer();
        if (server == null) return Optional.empty();
        return ownerId(entity).map(id -> server.getPlayerList().getPlayer(id));
    }

    /** Marca um golem como construído por {@code owner}. */
    public static void setGolemOwner(Entity golem, UUID owner) {
        golem.getPersistentData().putString(OWNER_KEY, owner.toString());
    }

    public static boolean hasGolemOwner(Entity golem) {
        return golem.getPersistentData().getString(OWNER_KEY).flatMap(PetOwnership::parse).isPresent();
    }

    private static Optional<UUID> uuid(EntityReference<LivingEntity> ref) {
        return ref == null ? Optional.empty() : Optional.of(ref.getUUID());
    }

    private static Optional<UUID> parse(String s) {
        try {
            return Optional.of(UUID.fromString(s));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
