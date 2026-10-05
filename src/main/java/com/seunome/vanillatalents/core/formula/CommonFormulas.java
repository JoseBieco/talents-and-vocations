package com.seunome.vanillatalents.core.formula;

/** Fórmulas da Árvore Comum. */
public final class CommonFormulas {

    private CommonFormulas() {}

    /** common_saturation: fator sobre a exaustão gerada pela regeneração natural. */
    public static double regenExhaustionMultiplier(int level, double perLevel) {
        return HookFormulas.reductionMultiplier(level, perLevel);
    }
}
