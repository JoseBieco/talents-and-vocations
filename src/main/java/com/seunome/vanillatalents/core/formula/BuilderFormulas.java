package com.seunome.vanillatalents.core.formula;

/** Fórmulas da árvore do Construtor. */
public final class BuilderFormulas {

    private BuilderFormulas() {}

    /** builder_dismantle: multiplicador de velocidade de quebra {@code 1 + lvl·per} (R5: sem teto). */
    public static double breakMultiplier(int level, double perLevel) {
        return 1 + level * perLevel;
    }

    /** builder_glass: o bloco de vidro quebrado sem Toque Suave e sem drops passa a dropar a si mesmo. */
    public static boolean glassDrop(boolean isGlass, boolean dropsEmpty, boolean silkTouch) {
        return isGlass && dropsEmpty && !silkTouch;
    }
}
