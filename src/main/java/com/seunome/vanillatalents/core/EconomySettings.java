package com.seunome.vanillatalents.core;

import java.util.Map;

/** Custos vigentes no servidor; o cliente os recebe no sync para rótulos e prévias. */
public record EconomySettings(CostMode mode, int costLevels, int costPoints, int respecFeeLevels, int respecRefundPercent) {

    public static final EconomySettings DEFAULTS = new EconomySettings(CostMode.LEVELS, 5, 100, 10, 25);

    public int cost() {
        return mode == CostMode.LEVELS ? costLevels : costPoints;
    }

    public boolean canAffordConversion(int playerLevel, float progress) {
        return XpCostRules.canAfford(mode, playerLevel, XpCostRules.currentTotalXp(playerLevel, progress), cost());
    }

    public Map<String, Integer> toMap() {
        return Map.of("CostMode", mode.ordinal(), "CostLevels", costLevels, "CostPoints", costPoints,
                "RespecFee", respecFeeLevels, "RespecRefund", respecRefundPercent);
    }

    public static EconomySettings fromMap(Map<String, Integer> map) {
        int modeIndex = map.getOrDefault("CostMode", DEFAULTS.mode.ordinal());
        CostMode mode = modeIndex >= 0 && modeIndex < CostMode.values().length ? CostMode.values()[modeIndex] : DEFAULTS.mode;
        return new EconomySettings(mode,
                map.getOrDefault("CostLevels", DEFAULTS.costLevels),
                map.getOrDefault("CostPoints", DEFAULTS.costPoints),
                map.getOrDefault("RespecFee", DEFAULTS.respecFeeLevels),
                map.getOrDefault("RespecRefund", DEFAULTS.respecRefundPercent));
    }
}
