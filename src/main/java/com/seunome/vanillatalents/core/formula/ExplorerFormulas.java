package com.seunome.vanillatalents.core.formula;

/** Fórmulas da árvore do Desbravador. */
public final class ExplorerFormulas {

    private ExplorerFormulas() {}

    /**
     * explorer_fall × explorer_roll (quando agachado no impacto), com a redução total do mod limitada a
     * {@code cap} (ex.: 0,6 → o fator nunca fica abaixo de 0,4).
     */
    public static double fallMultiplier(int fallLvl, double fallPer, boolean rollActive, double rollValue, double cap) {
        double multiplier = HookFormulas.reductionMultiplier(fallLvl, fallPer);
        if (rollActive) multiplier *= 1 - rollValue;
        return Math.max(multiplier, 1 - cap);
    }

    /**
     * explorer_featherfoot: distância de queda equivalente para que os primeiros {@code safeBlocks} não contem.
     * A vanilla ainda desconta {@code vanillaSafe} (3) depois, então soma-se de volta.
     */
    public static double featherfootDistance(double distance, double safeBlocks, double vanillaSafe) {
        return Math.max(0, distance - safeBlocks + vanillaSafe);
    }

    /** explorer_mounts: bônus multiplicativo na velocidade da montaria. */
    public static double mountBonus(int level, double perLevel) {
        return level * perLevel;
    }

    /** explorer_climb: multiplicador da velocidade vertical subindo escada de mão ou trepadeira. */
    public static double climbFactor(int level, double perLevel) {
        return 1 + level * perLevel;
    }

    /** explorer_tailwind: duração do foguete (ticks) usado durante o voo de élitra. */
    public static int tailwindLifetime(int lifetime, int level, double perLevel) {
        return (int) Math.round(lifetime * (1 + level * perLevel));
    }

    /** explorer_nightwatch: ticks mínimos sem dormir antes de phantoms poderem aparecer. */
    public static int phantomMinRestTicks(int level, int baseTicks, int perLevelTicks) {
        return baseTicks + level * perLevelTicks;
    }
}
