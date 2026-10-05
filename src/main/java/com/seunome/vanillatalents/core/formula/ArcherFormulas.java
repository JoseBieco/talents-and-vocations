package com.seunome.vanillatalents.core.formula;

/** Fórmulas da árvore do Arqueiro. Bônus de dano do Arqueiro são aditivos entre si. */
public final class ArcherFormulas {

    /** Resultado de um passo do acumulador fracionário: ticks extras inteiros e o resto. */
    public record Step(int extraTicks, double remainder) {}

    private ArcherFormulas() {}

    /**
     * archer_aim + archer_bolt (só besta) + archer_longshot (só acima de {@code minDistance}), somados antes
     * de multiplicar o dano.
     */
    public static double damageMultiplier(int aimLvl, double aimPer, int boltLvl, double boltPer, boolean isCrossbow,
                                          int longLvl, double longPer, double distance, double minDistance) {
        double bonus = aimLvl * aimPer;
        if (isCrossbow) bonus += boltLvl * boltPer;
        if (distance > minDistance) bonus += longLvl * longPer;
        return 1 + bonus;
    }

    /** archer_draw: progresso de carga do arco por tick (1 = vanilla). */
    public static double drawProgressPerTick(int level, double perLevel) {
        double remaining = HookFormulas.reductionMultiplier(level, perLevel);
        return remaining <= 0 ? 1 : 1 / remaining;
    }

    /** archer_reload: duração da recarga da besta, nunca abaixo de {@code minTicks}. */
    public static int reloadTicks(int baseTicks, int level, double perLevel, int minTicks) {
        int reduced = (int) Math.round(baseTicks * HookFormulas.reductionMultiplier(level, perLevel));
        return Math.max(Math.min(minTicks, baseTicks), reduced);
    }

    /** archer_conserve: chance de não consumir a flecha. */
    public static double conserveChance(int level, double perLevel) {
        return HookFormulas.chance(level, perLevel);
    }

    /** Acumula o progresso extra de um tick com {@code factor} (1,5 = 50% mais rápido). */
    public static Step accumulate(double remainder, double factor) {
        double total = remainder + Math.max(0, factor - 1);
        int extra = (int) Math.floor(total);
        return new Step(extra, total - extra);
    }

    /** archer_mobile: fator sobre o input de movimento enquanto puxa o arco (vanilla reduz a {@code vanillaFactor}). */
    public static double mobileInputFactor(int level, double perLevel, double vanillaFactor) {
        double target = vanillaFactor + (1 - vanillaFactor) * HookFormulas.chance(level, perLevel);
        return target / vanillaFactor;
    }
}
