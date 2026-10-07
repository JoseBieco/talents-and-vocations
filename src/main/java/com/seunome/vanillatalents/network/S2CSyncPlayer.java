package com.seunome.vanillatalents.network;

import com.seunome.vanillatalents.client.ClientPacketHandlers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Estado completo do PlayerSkillData do jogador, enviado pelo servidor. */
public record S2CSyncPlayer(CompoundTag data) {

    /** Sub-tag com EconomySettings; ignorada por PlayerSkillData.deserializeNBT. */
    public static final String SETTINGS_KEY = "Settings";

    public static void encode(S2CSyncPlayer msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.data);
    }

    public static S2CSyncPlayer decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new S2CSyncPlayer(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(S2CSyncPlayer msg, CustomPayloadEvent.Context ctx) {
        ClientPacketHandlers.syncPlayer(msg);
    }
}
