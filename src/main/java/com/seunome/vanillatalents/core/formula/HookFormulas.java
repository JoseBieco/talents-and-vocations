package com.seunome.vanillatalents.core.formula;

import java.util.function.DoubleSupplier;

/** Fórmulas usadas pelos hooks de Mixin (exaustão, durabilidade, escudo). */
public final class HookFormulas {

    private HookFormulas() {}

    /** Fator de redução "−per por nível": {@code 1 − lvl·per}, limitado a [0, 1]. */
    public static double reductionMultiplier(int level, double perLevel) {
        return Math.max(0, Math.min(1, 1 - level * perLevel));
    }

    /** Chance "per por nível", limitada a [0, 1]. */
    public static double chance(int level, double perLevel) {
        return Math.max(0, Math.min(1, level * perLevel));
    }

    /**
     * Pontos de durabilidade que realmente serão gastos: cada ponto é pulado com {@code skipChance}
     * (como Inquebrável). Valores ≤ 0 (reparo) passam intactos.
     */
    public static int keptDamage(int amount, double skipChance, DoubleSupplier random) {
        if (amount <= 0 || skipChance <= 0) return amount;
        int kept = 0;
        for (int i = 0; i < amount; i++) {
            if (random.getAsDouble() >= skipChance) kept++;
        }
        return kept;
    }
}
