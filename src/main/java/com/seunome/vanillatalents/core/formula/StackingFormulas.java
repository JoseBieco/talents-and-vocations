package com.seunome.vanillatalents.core.formula;

import java.util.List;

/** Regras de acúmulo entre classes (spec 9.5): R2 (redução de combate) e R4 (poupar durabilidade). */
public final class StackingFormulas {

    private StackingFormulas() {}

    /** R2: fatores de redução de combate combinam multiplicativamente; o total não reduz mais que {@code cap}. */
    public static double combatMultiplier(List<Double> multipliers, double cap) {
        double product = 1;
        for (double m : multipliers) product *= m;
        return Math.max(product, 1 - cap);
    }

    /** R4: chances de fontes diferentes rolam de forma independente, {@code 1 − Π(1 − pᵢ)}, limitado a {@code cap}. */
    public static double combinedChance(List<Double> chances, double cap) {
        double none = 1;
        for (double p : chances) none *= 1 - p;
        return Math.min(1 - none, cap);
    }
}
