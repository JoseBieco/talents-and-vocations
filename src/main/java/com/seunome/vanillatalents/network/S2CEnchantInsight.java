package com.seunome.vanillatalents.network;

import com.seunome.vanillatalents.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Visão Arcana: lista completa de encantamentos de cada linha (0–2) da mesa aberta no menu {@code containerId}.
 * Linha sem opção = lista vazia.
 */
public record S2CEnchantInsight(int containerId, List<List<EnchantLine>> rows) {

    /** Um encantamento: id do registro ({@code minecraft:sharpness}) e nível. */
    public record EnchantLine(String enchantId, int level) {}

    static final int MAX_ROWS = 3;
    static final int MAX_LINES = 64;

    public static void encode(S2CEnchantInsight msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.containerId);
        buf.writeVarInt(msg.rows.size());
        for (List<EnchantLine> row : msg.rows) {
            buf.writeVarInt(row.size());
            for (EnchantLine line : row) {
                buf.writeUtf(line.enchantId());
                buf.writeVarInt(line.level());
            }
        }
    }

    public static S2CEnchantInsight decode(FriendlyByteBuf buf) {
        int containerId = buf.readVarInt();
        int rowCount = buf.readVarInt();
        if (rowCount < 0 || rowCount > MAX_ROWS) throw new IllegalArgumentException("Too many rows: " + rowCount);
        List<List<EnchantLine>> rows = new ArrayList<>(rowCount);
        for (int r = 0; r < rowCount; r++) {
            int size = buf.readVarInt();
            if (size < 0 || size > MAX_LINES) throw new IllegalArgumentException("Too many enchantments: " + size);
            List<EnchantLine> lines = new ArrayList<>(size);
            for (int i = 0; i < size; i++) lines.add(new EnchantLine(buf.readUtf(), buf.readVarInt()));
            rows.add(lines);
        }
        return new S2CEnchantInsight(containerId, rows);
    }

    public static void handle(S2CEnchantInsight msg, CustomPayloadEvent.Context ctx) {
        ClientPacketHandlers.enchantInsight(msg);
    }
}
