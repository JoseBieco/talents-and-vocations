package com.josebieco.talentsvocations.core;

/** Confirmação em dois cliques: o primeiro arma, o segundo dentro da janela confirma. */
public final class TwoStepConfirm {

    private final int minTicks;
    private final int windowTicks;
    private boolean armed;
    private long armedAt;

    public TwoStepConfirm(int windowTicks) {
        this(0, windowTicks);
    }

    /** O segundo clique só confirma entre minTicks e windowTicks (inclusive) após armar. */
    public TwoStepConfirm(int minTicks, int windowTicks) {
        this.minTicks = minTicks;
        this.windowTicks = windowTicks;
    }

    /** Devolve true só no segundo clique dentro da janela; caso contrário arma e devolve false. */
    public boolean click(long tick) {
        if (isArmed(tick)) {
            if (tick - armedAt < minTicks) return false;
            armed = false;
            return true;
        }
        armed = true;
        armedAt = tick;
        return false;
    }

    public boolean isArmed(long tick) {
        return armed && tick - armedAt <= windowTicks;
    }

    public void reset() {
        armed = false;
    }
}
