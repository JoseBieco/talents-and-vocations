package com.josebieco.talentsvocations.client;

import com.josebieco.talentsvocations.capability.PlayerSkillData;
import com.josebieco.talentsvocations.core.EconomySettings;
import com.josebieco.talentsvocations.network.S2CSyncPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

/** Cópia local, só leitura, do PlayerSkillData do jogador; atualizada apenas por S2CSyncPlayer. */
public final class ClientTalentState {

    private static PlayerSkillData data = new PlayerSkillData();
    private static EconomySettings economy = EconomySettings.DEFAULTS;

    private ClientTalentState() {}

    public static PlayerSkillData data() {
        return data;
    }

    public static EconomySettings economy() {
        return economy;
    }

    static void update(CompoundTag tag) {
        PlayerSkillData fresh = new PlayerSkillData();
        fresh.deserializeNBT(null, tag);
        CompoundTag settings = tag.getCompoundOrEmpty(S2CSyncPlayer.SETTINGS_KEY);
        Map<String, Integer> values = new HashMap<>();
        for (String key : settings.keySet()) values.put(key, settings.getIntOr(key, 0));
        economy = EconomySettings.fromMap(values);
        fresh.setMaxClasses(economy.maxClasses());
        data = fresh;
        notifyScreen();
    }

    /** Telas do mod se reconstroem quando chega estado novo (sem atualização otimista). */
    static void notifyScreen() {
        var current = Minecraft.getInstance().gui.screen();
        if (current instanceof TalentScreen screen) screen.onSync();
        else if (current instanceof ClassScreen screen) screen.onSync();
    }
}
