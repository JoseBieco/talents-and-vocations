package com.josebieco.talentsvocations.network;

import com.josebieco.talentsvocations.core.ClassSlot;
import com.josebieco.talentsvocations.server.TalentActions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Pedido do cliente: escolher ou trocar a classe de um espaço (principal ou secundário). */
public record C2SChangeClass(ClassSlot slot, String classId) {

    public static void encode(C2SChangeClass msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.slot);
        buf.writeUtf(msg.classId, C2SBuyNode.MAX_WIRE_LENGTH);
    }

    public static C2SChangeClass decode(FriendlyByteBuf buf) {
        ClassSlot slot = buf.readEnum(ClassSlot.class);
        return new C2SChangeClass(slot, buf.readUtf(C2SBuyNode.MAX_WIRE_LENGTH));
    }

    public static void handle(C2SChangeClass msg, CustomPayloadEvent.Context ctx) {
        if (ctx.getSender() != null) TalentActions.changeClass(ctx.getSender(), msg.slot, msg.classId);
    }
}
