package com.seunome.vanillatalents.capability;

import com.seunome.vanillatalents.Config;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class SkillAccess {

    private SkillAccess() {}

    /** Capability do jogador, com {@code maxClasses} vindo da config (não é salvo no NBT). */
    public static Optional<PlayerSkillData> get(Player player) {
        Optional<PlayerSkillData> data = player.getCapability(PlayerSkillProvider.PLAYER_SKILL).resolve();
        data.ifPresent(d -> d.setMaxClasses(Config.MAX_CLASSES.get()));
        return data;
    }
}
