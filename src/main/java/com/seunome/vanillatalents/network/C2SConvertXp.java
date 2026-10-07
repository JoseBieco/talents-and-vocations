package com.seunome.vanillatalents.network;

import com.seunome.vanillatalents.server.TalentActions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Pedido do cliente: converter XP em 1 PT, ou em todos os PT possíveis ({@code all}). */
public record C2SConvertXp(boolean all) {

    public static void encode(C2SConvertXp msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.all);
    }

    public static C2SConvertXp decode(FriendlyByteBuf buf) {
        return new C2SConvertXp(buf.readBoolean());
    }

    public static void handle(C2SConvertXp msg, CustomPayloadEvent.Context ctx) {
        if (ctx.getSender() != null) TalentActions.convertXp(ctx.getSender(), msg.all);
    }
}
