package com.seunome.vanillatalents.network;

import com.seunome.vanillatalents.server.TalentActions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Pedido do cliente: escolher ou trocar de classe. */
public record C2SChangeClass(String classId) {

    public static void encode(C2SChangeClass msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.classId, C2SBuyNode.MAX_WIRE_LENGTH);
    }

    public static C2SChangeClass decode(FriendlyByteBuf buf) {
        return new C2SChangeClass(buf.readUtf(C2SBuyNode.MAX_WIRE_LENGTH));
    }

    public static void handle(C2SChangeClass msg, CustomPayloadEvent.Context ctx) {
        if (ctx.getSender() != null) TalentActions.changeClass(ctx.getSender(), msg.classId);
    }
}
