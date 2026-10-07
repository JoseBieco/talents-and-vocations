package com.josebieco.talentsvocations.network;

import com.josebieco.talentsvocations.client.ClientPacketHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.ArrayList;
import java.util.List;

/** Posições de minérios que o cliente deve destacar com partículas (Faro Mineral). */
public record S2CProspectorHighlight(List<BlockPos> positions) {

    static final int MAX_POSITIONS = 4096;

    public static void encode(S2CProspectorHighlight msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.positions.size());
        for (BlockPos pos : msg.positions) buf.writeBlockPos(pos);
    }

    public static S2CProspectorHighlight decode(FriendlyByteBuf buf) {
        int size = Math.min(buf.readVarInt(), MAX_POSITIONS);
        List<BlockPos> positions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) positions.add(buf.readBlockPos());
        return new S2CProspectorHighlight(positions);
    }

    public static void handle(S2CProspectorHighlight msg, CustomPayloadEvent.Context ctx) {
        ClientPacketHandlers.prospectorHighlight(msg);
    }
}
