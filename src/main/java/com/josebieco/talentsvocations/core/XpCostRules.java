package com.josebieco.talentsvocations.core;

public final class XpCostRules {

    private XpCostRules() {}

    /** Pontos de XP totais para ir do nível 0 até {@code level} (curva vanilla). */
    public static int totalXpForLevel(int level) {
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (5 * level * level - 81 * level + 720) / 2;
        return (9 * level * level - 325 * level + 4440) / 2;
    }

    /** Pontos para ir de {@code level} a {@code level + 1} (vanilla Player.getXpNeededForNextLevel). */
    public static int xpNeededForNextLevel(int level) {
        if (level >= 30) return 112 + (level - 30) * 9;
        if (level >= 15) return 37 + (level - 15) * 5;
        return 7 + level * 2;
    }

    /**
     * XP real que o jogador tem agora. O {@code totalExperience} vanilla não serve: é uma pontuação
     * que não diminui quando níveis são gastos em encantamentos.
     */
    public static int currentTotalXp(int level, float progress) {
        return totalXpForLevel(level) + Math.round(progress * xpNeededForNextLevel(level));
    }

    /** Quantos PT o jogador consegue comprar de uma vez com o XP atual ("Converter tudo"). */
    public static int maxConversions(CostMode mode, int playerLevel, float progress, int cost) {
        if (cost <= 0) return 0;
        int available = switch (mode) {
            case LEVELS -> playerLevel;
            case POINTS -> currentTotalXp(playerLevel, progress);
        };
        return Math.max(0, available / cost);
    }

    public static boolean canAfford(CostMode mode, int playerLevel, int playerTotalXp, int amount) {
        return switch (mode) {
            case LEVELS -> playerLevel >= amount;
            case POINTS -> playerTotalXp >= amount;
        };
    }
}
