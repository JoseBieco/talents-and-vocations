package com.josebieco.talentsvocations.core.formula;

import java.util.List;
import java.util.Map;

/** Faro Mineral: cor do contorno por tipo de minério e geometria do destaque. */
public final class OreHighlight {

    public static final int COAL = 0x4A4A4A;
    public static final int IRON = 0xD8AF93;
    public static final int COPPER = 0xE77C56;
    public static final int GOLD = 0xFCEE4B;
    public static final int REDSTONE = 0xFF2A2A;
    public static final int LAPIS = 0x345EC3;
    public static final int DIAMOND = 0x5DECF5;
    public static final int EMERALD = 0x17DD62;
    public static final int QUARTZ = 0xEDE6DE;
    public static final int NETHERITE_SCRAP = 0x6D4B3A;
    public static final int DEFAULT = 0xFFFFFF;

    /** Caminho da tag comum ({@code c:ores/<tipo>}) → cor. */
    private static final Map<String, Integer> BY_TAG = Map.of(
            "ores/coal", COAL, "ores/iron", IRON, "ores/copper", COPPER, "ores/gold", GOLD,
            "ores/redstone", REDSTONE, "ores/lapis", LAPIS, "ores/diamond", DIAMOND,
            "ores/emerald", EMERALD, "ores/quartz", QUARTZ, "ores/netherite_scrap", NETHERITE_SCRAP);

    private OreHighlight() {}

    /** Cor do contorno a partir dos caminhos das tags {@code c:} do bloco; branco para minérios sem cor própria. */
    public static int colorFor(List<String> commonTagPaths) {
        for (String path : commonTagPaths) {
            Integer color = BY_TAG.get(path);
            if (color != null) return color;
        }
        return DEFAULT;
    }

    /** Deslocamento para que um bloco escalado por {@code scale} fique centrado dentro da célula. */
    public static float insetTranslation(float scale) {
        return (1f - scale) / 2f;
    }

    public static <T> List<T> capped(List<T> positions, int max) {
        return positions.size() <= max ? positions : positions.subList(0, max);
    }
}
