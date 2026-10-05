package com.seunome.vanillatalents.core.formula;

/** Fórmulas da Árvore Comum. */
public final class CommonFormulas {

    private CommonFormulas() {}

    /** common_saturation: fator sobre a exaustão gerada pela regeneração natural. */
    public static double regenExhaustionMultiplier(int level, double perLevel) {
        return HookFormulas.reductionMultiplier(level, perLevel);
    }

    /** common_fireproof × common_lava (só em lava), multiplicativos entre si. */
    public static double fireMultiplier(int fireLvl, double firePer, int lavaLvl, double lavaPer, boolean isLava) {
        double fire = HookFormulas.reductionMultiplier(fireLvl, firePer);
        double lava = isLava ? HookFormulas.reductionMultiplier(lavaLvl, lavaPer) : 1;
        return fire * lava;
    }

    /** common_second_wind: dispara quando a vida após o dano fica em (0, threshold]. */
    public static boolean secondWindTriggers(float healthAfter, double threshold) {
        return healthAfter > 0 && healthAfter <= threshold;
    }

    /** common_gourmet: saturação extra = saturação do alimento × per × nível. */
    public static double gourmetExtraSaturation(double foodSaturation, int level, double perLevel) {
        return foodSaturation * level * perLevel;
    }

    /** common_antidote: duração reduzida de Veneno/Fome, nunca abaixo de 1 tick. */
    public static int antidoteDuration(int duration, int level, double perLevel) {
        return Math.max(1, (int) Math.round(duration * HookFormulas.reductionMultiplier(level, perLevel)));
    }
}
