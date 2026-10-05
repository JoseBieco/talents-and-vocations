package com.seunome.vanillatalents.network;

import com.seunome.vanillatalents.server.TalentActions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Pedido do cliente: converter XP em 1 PT. */
public record C2SConvertXp() {

    public static void encode(C2SConvertXp msg, FriendlyByteBuf buf) {
    }

    public static C2SConvertXp decode(FriendlyByteBuf buf) {
        return new C2SConvertXp();
    }

    public static void handle(C2SConvertXp msg, CustomPayloadEvent.Context ctx) {
        if (ctx.getSender() != null) TalentActions.convertXp(ctx.getSender());
    }
}
