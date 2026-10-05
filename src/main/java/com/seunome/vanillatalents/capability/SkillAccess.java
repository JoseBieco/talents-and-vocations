package com.seunome.vanillatalents.capability;

import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class SkillAccess {

    private SkillAccess() {}

    public static Optional<PlayerSkillData> get(Player player) {
        return player.getCapability(PlayerSkillProvider.PLAYER_SKILL).resolve();
    }
}
