package com.josebieco.talentsvocations.core.formula;

import java.util.List;

/** Fórmulas da árvore do Desbravador. */
public final class ExplorerFormulas {

    private ExplorerFormulas() {}

    /**
     * explorer_fall × explorer_roll (quando agachado no impacto), com a redução total do mod limitada a
     * {@code cap} (ex.: 0,6 → o fator nunca fica abaixo de 0,4).
     */
    public static double fallMultiplier(int fallLvl, double fallPer, boolean rollActive, double rollValue, double cap) {
        return fallMultiplier(fallLvl, fallPer, rollActive, rollValue, 0, 0, cap);
    }

    /**
     * Igual ao anterior com tamer_saddle (Sela Firme, montado) multiplicando junto: o produto de todos os fatores
     * nunca fica abaixo de {@code 1 − cap} (R3: a Sela Firme entra no mesmo teto).
     */
    public static double fallMultiplier(int fallLvl, double fallPer, boolean rollActive, double rollValue,
                                        int saddleLvl, double saddlePer, double cap) {
        return StackingFormulas.cappedProduct(List.of(
                HookFormulas.reductionMultiplier(fallLvl, fallPer),
                rollActive ? rollFactor(rollValue) : 1.0,
                HookFormulas.reductionMultiplier(saddleLvl, saddlePer)), cap);
    }

    /** explorer_roll: fator de queda quando agachado no impacto ({@code 1 − value}, sem clamp, como sempre foi). */
    public static double rollFactor(double rollValue) {
        return 1 - rollValue;
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

    /** y que a vanilla grava subindo algo escalável (pulo ou colisão horizontal), logo depois do move. */
    public static final double CLIMB_RESET_Y = 0.2;

    /**
     * Subida com fator (explorer_climb, builder_scaffold): só multiplica quando a vanilla vai reescrever o y para
     * {@link #CLIMB_RESET_Y} no próximo tick ({@code resetCondition} = colisão horizontal ou pulo), fora d'água e sem
     * Levitação; senão o fator compõe tick após tick. O resultado nunca passa de {@code 0,2 × fator}. Sem subida
     * (y ≤ 0) devolve y.
     */
    public static double climbBoost(double y, double factor, boolean resetCondition, boolean inWater, boolean levitating) {
        if (y <= 0 || !resetCondition || inWater || levitating) return y;
        return Math.min(y * factor, CLIMB_RESET_Y * factor);
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
