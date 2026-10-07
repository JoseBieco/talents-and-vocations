package com.josebieco.talentsvocations.core.formula;

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

    /**
     * Escala linear {@code base · (1 + level·perLevel)}: aceleração do barco (angler_boat_speed), duração da Graça do
     * Golfinho (angler_dolphin), alcance da neblina d'água (angler_sea_eyes) e a Lealdade.
     */
    public static double scaled(double base, int level, double perLevel) {
        return base * (1 + level * perLevel);
    }

    /**
     * angler_trident (+ angler_high_tide na chuva/submerso): parte aditiva do bônus de dano do tridente, sem o 1.
     * Arremessado soma no pool aditivo do Arqueiro (R1); corpo a corpo multiplica por {@code 1 + bônus}.
     */
    public static double tridentBonus(int level, double perLevel, boolean highTide, double highTideBonus) {
        return level * perLevel + (highTide ? highTideBonus : 0);
    }

    /** Anti-AFK: o jogador se moveu ou girou a câmera nos últimos {@code idleTicks}. */
    public static boolean active(long lastActiveTick, long now, int idleTicks) {
        return now - lastActiveTick <= idleTicks;
    }

    /**
     * Anti-AFK: conta como atividade girar a câmera ou andar na horizontal sem estar montado. Y é ignorado (boiar na
     * superfície, correnteza), e o deslocamento de barco/montaria não conta.
     */
    public static boolean isActivity(double dx, double dz, float dYaw, float dPitch, boolean passenger) {
        if (Math.abs(dYaw) > LOOK_EPSILON || Math.abs(dPitch) > LOOK_EPSILON) return true;
        return !passenger && dx * dx + dz * dz > MOVE_EPSILON_SQR;
    }

    private static final double MOVE_EPSILON_SQR = 1.0E-4;
    private static final float LOOK_EPSILON = 0.01F;

    /** angler_bountiful e angler_cook: só peixes dobram ou cozinham; tesouro e lixo saem como vieram. */
    public static Catch catchResult(boolean isFish, boolean doubleRoll, boolean cookRoll) {
        if (!isFish) return new Catch(1, false);
        return new Catch(doubleRoll ? 2 : 1, cookRoll);
    }
}
