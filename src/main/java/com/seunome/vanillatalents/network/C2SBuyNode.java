package com.seunome.vanillatalents.network;

import com.seunome.vanillatalents.server.TalentActions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Pedido do cliente: comprar 1 nível do nó. */
public record C2SBuyNode(String nodeId) {

    /** Limite de leitura do buffer; o limite lógico (64) é checado em TalentActions. */
    static final int MAX_WIRE_LENGTH = 256;

    public static void encode(C2SBuyNode msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.nodeId, MAX_WIRE_LENGTH);
    }

    public static C2SBuyNode decode(FriendlyByteBuf buf) {
        return new C2SBuyNode(buf.readUtf(MAX_WIRE_LENGTH));
    }

    public static void handle(C2SBuyNode msg, CustomPayloadEvent.Context ctx) {
        if (ctx.getSender() != null) TalentActions.buyNode(ctx.getSender(), msg.nodeId);
    }
}
