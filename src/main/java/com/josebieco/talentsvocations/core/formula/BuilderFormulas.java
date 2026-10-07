package com.josebieco.talentsvocations.core.formula;

/** Fórmulas da árvore do Construtor. */
public final class BuilderFormulas {

    private BuilderFormulas() {}

    /** builder_dismantle: multiplicador de velocidade de quebra {@code 1 + lvl·per} (R5: sem teto). */
    public static double breakMultiplier(int level, double perLevel) {
        return 1 + level * perLevel;
    }

    /**
     * builder_height_work: colocou um bloco nos últimos {@code windowTicks} ticks ({@code now − lastPlace ≤ janela}).
     * Escrito como {@code lastPlace ≥ now − janela} para não estourar com o sentinela "nunca" ({@code Long.MIN_VALUE}).
     */
    public static boolean recentlyPlaced(long lastPlace, long now, int windowTicks) {
        return lastPlace >= now - windowTicks;
    }

    /** builder_scaffold: multiplicador da velocidade vertical dentro do andaime, {@code 1 + lvl·per}. */
    public static double scaffoldFactor(int level, double perLevel) {
        return 1 + level * perLevel;
    }

    /** builder_glass: o bloco de vidro quebrado sem Toque Suave e sem drops passa a dropar a si mesmo. */
    public static boolean glassDrop(boolean isGlass, boolean dropsEmpty, boolean silkTouch) {
        return isGlass && dropsEmpty && !silkTouch;
    }

    /**
     * builder_thrifty + builder_torch: chance de devolver o bloco colocado. As duas fontes são independentes (uma tocha
     * barata conta nas duas): {@code 1 − (1 − econômico)·(1 − iluminador)}.
     */
    public static double refundChance(boolean cheap, int thriftyLvl, double thriftyPer,
                                      boolean torch, int torchLvl, double torchPer) {
        double thrifty = cheap ? HookFormulas.chance(thriftyLvl, thriftyPer) : 0;
        double torchChance = torch ? HookFormulas.chance(torchLvl, torchPer) : 0;
        return 1 - (1 - thrifty) * (1 - torchChance);
    }

    /**
     * builder_thrifty/builder_torch: só devolve se a mão registrada no clique direito é do mesmo tick da colocação e
     * segurava (cópia de antes do uso) o BlockItem do bloco colocado — senão o bloco veio de outra coisa (enxada em
     * terra grossa vira terra) e devolver seria duplicar. Criativo nunca devolve.
     */
    public static boolean refundEligible(boolean sameTick, boolean heldIsBlockItemOfPlaced, boolean creative) {
        return sameTick && heldIsBlockItemOfPlaced && !creative;
    }

    /**
     * builder_quick_hands: a mão que o clique direito vai usar tem BlockItem. Exige mirar num bloco (mirando o ar o
     * BlockItem passa e a vanilla tenta a outra mão) e: principal com bloco e secundária vazia ou com bloco, ou principal
     * vazia e secundária com bloco. Assim terra + ovo/bola de neve ou pérola + terra ficam no intervalo vanilla.
     */
    public static boolean quickHandsHolding(boolean aimingAtBlock, boolean mainEmpty, boolean mainBlock,
                                            boolean offEmpty, boolean offBlock) {
        if (!aimingAtBlock) return false;
        if (mainBlock) return offEmpty || offBlock;
        return mainEmpty && offBlock;
    }

    /**
     * builder_quick_hands: com BlockItem na mão, o atraso do clique direito ({@code Minecraft.rightClickDelay}) cai
     * para {@code quickDelay}; nunca aumenta um atraso que já é menor.
     */
    public static int quickDelay(int current, boolean holdingBlockItem, int quickDelay) {
        return holdingBlockItem && current > quickDelay ? quickDelay : current;
    }
}
