package com.seunome.vanillatalents.core.formula;

/** Fórmulas da árvore do Minerador. */
public final class MinerFormulas {

    /** Penalidade vanilla de quebrar blocos fora do chão: velocidade × 0,2. */
    static final double AIR_PENALTY = 0.2;

    private MinerFormulas() {}

    /**
     * Multiplicador sobre a velocidade de quebra:
     * {@code (1 + haste·per) × (denso ? 1 + dense·per : 1) × fator de Pés Firmes}. Pés Firmes recupera
     * {@code per} da velocidade perdida por nível quando o jogador não está no chão.
     */
    public static double breakSpeedMultiplier(int hasteLvl, double hastePer, int denseLvl, double densePer, boolean isDense,
                                              int footingLvl, boolean onGround) {
        double haste = 1 + hasteLvl * hastePer;
        double dense = isDense ? 1 + denseLvl * densePer : 1;
        double footing = onGround ? 1 : footingFactor(footingLvl);
        return haste * dense * footing;
    }

    static double footingFactor(int footingLvl) {
        return (AIR_PENALTY + 0.4 * footingLvl) / AIR_PENALTY;
    }

    /** Chance de Toque de Midas duplicar o drop do minério. */
    public static double fortuneChance(int level, double perLevel) {
        return HookFormulas.chance(level, perLevel);
    }
}
