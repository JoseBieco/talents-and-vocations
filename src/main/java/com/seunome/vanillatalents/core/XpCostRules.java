package com.seunome.vanillatalents.core;

public final class XpCostRules {

    private XpCostRules() {}

    /** Pontos de XP totais para ir do nível 0 até {@code level} (curva vanilla). */
    public static int totalXpForLevel(int level) {
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (5 * level * level - 81 * level + 720) / 2;
        return (9 * level * level - 325 * level + 4440) / 2;
    }

    public static boolean canAfford(CostMode mode, int playerLevel, int playerTotalXp, int amount) {
        return switch (mode) {
            case LEVELS -> playerLevel >= amount;
            case POINTS -> playerTotalXp >= amount;
        };
    }
}
