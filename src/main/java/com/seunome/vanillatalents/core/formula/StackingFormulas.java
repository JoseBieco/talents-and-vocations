package com.seunome.vanillatalents.core.formula;

import java.util.List;

/** Regras de acúmulo entre classes (spec 9.5): R2 (combate), R3 (queda) e R4 (poupar durabilidade). */
public final class StackingFormulas {

    private StackingFormulas() {}

    /**
     * Fatores de redução combinam multiplicativamente, {@code max(Π fᵢ, 1 − cap)}: o total não reduz mais que
     * {@code cap}. Lista vazia → 1. Base de R2 (combate) e R3 (queda, {@code fallReductionCap}).
     */
    public static double cappedProduct(List<Double> factors, double cap) {
        double product = 1;
        for (double f : factors) product *= f;
        return Math.max(product, 1 - cap);
    }

    /** R2: fatores de redução de combate combinam multiplicativamente; o total não reduz mais que {@code cap}. */
    public static double combatMultiplier(List<Double> multipliers, double cap) {
        return cappedProduct(multipliers, cap);
    }

    /** R4: chances de fontes diferentes rolam de forma independente, {@code 1 − Π(1 − pᵢ)}, limitado a {@code cap}. */
    public static double combinedChance(List<Double> chances, double cap) {
        double none = 1;
        for (double p : chances) none *= 1 - p;
        return Math.min(1 - none, cap);
    }
}
