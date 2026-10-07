package com.josebieco.talentsvocations.network;

import com.josebieco.talentsvocations.client.ClientPacketHandlers;
import com.josebieco.talentsvocations.core.TalentNode;
import com.josebieco.talentsvocations.data.TalentNodeCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.ArrayList;
import java.util.List;

/** Definições dos nós carregadas pelo servidor (login e /reload). */
public record S2CSyncDefinitions(List<TalentNode> nodes) {

    public static void encode(S2CSyncDefinitions msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.nodes.size());
        for (TalentNode node : msg.nodes) buf.writeJsonWithCodec(TalentNodeCodec.CODEC, node);
    }

    public static S2CSyncDefinitions decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<TalentNode> nodes = new ArrayList<>(size);
        for (int i = 0; i < size; i++) nodes.add(buf.readLenientJsonWithCodec(TalentNodeCodec.CODEC));
        return new S2CSyncDefinitions(nodes);
    }

    public static void handle(S2CSyncDefinitions msg, CustomPayloadEvent.Context ctx) {
        ClientPacketHandlers.syncDefinitions(msg);
    }
}
