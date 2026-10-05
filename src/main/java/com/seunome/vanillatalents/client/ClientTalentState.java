package com.seunome.vanillatalents.client;

import com.seunome.vanillatalents.capability.PlayerSkillData;
import net.minecraft.nbt.CompoundTag;

/** Cópia local, só leitura, do PlayerSkillData do jogador; atualizada apenas por S2CSyncPlayer. */
public final class ClientTalentState {

    private static PlayerSkillData data = new PlayerSkillData();

    private ClientTalentState() {}

    public static PlayerSkillData data() {
        return data;
    }

    static void update(CompoundTag tag) {
        PlayerSkillData fresh = new PlayerSkillData();
        fresh.deserializeNBT(null, tag);
        data = fresh;
    }
}
