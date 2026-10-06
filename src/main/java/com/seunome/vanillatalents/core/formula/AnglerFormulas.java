package com.seunome.vanillatalents.core.formula;

/** Fórmulas puras do Pescador. */
public final class AnglerFormulas {

    /** Resultado de uma pilha pescada: quantas cópias e se esta cópia vem cozida. */
    public record Catch(int copies, boolean cooked) {}

    private AnglerFormulas() {}

    /**
     * angler_lure (+ pesca de angler_high_tide): tempo até o peixe se aproximar. Valores ≤ 0 (Isca alta já zerou)
     * passam direto; senão nunca cai abaixo de 1.
     */
    public static int lureTicks(int ticks, int lureLvl, double lurePer, boolean highTide, double highTideReduction) {
        if (ticks <= 0) return ticks;
        double factor = (1 - lureLvl * lurePer) * (highTide ? 1 - highTideReduction : 1);
        return (int) Math.max(1, Math.round(ticks * factor));
    }

    /** Anti-AFK: o jogador se moveu ou girou a câmera nos últimos {@code idleTicks}. */
    public static boolean active(long lastActiveTick, long now, int idleTicks) {
        return now - lastActiveTick <= idleTicks;
    }

    /** angler_bountiful e angler_cook: só peixes dobram ou cozinham; tesouro e lixo saem como vieram. */
    public static Catch catchResult(boolean isFish, boolean doubleRoll, boolean cookRoll) {
        if (!isFish) return new Catch(1, false);
        return new Catch(doubleRoll ? 2 : 1, cookRoll);
    }
}
