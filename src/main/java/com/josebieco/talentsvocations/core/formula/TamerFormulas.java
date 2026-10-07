package com.josebieco.talentsvocations.core.formula;

/** Fórmulas do Domador (Matilha, Lambida, Amigo do Ferro, Laço Eterno). */
public final class TamerFormulas {

    private TamerFormulas() {}

    /** tamer_pack: {@code 1 − lvl·per·min(lobos, máx)}, limitado a [0, 1]. */
    public static double packMultiplier(int level, double perLevel, int wolves, int maxWolves) {
        int counted = Math.max(0, Math.min(wolves, maxWolves));
        return Math.max(0, Math.min(1, 1 - level * perLevel * counted));
    }

    /** tamer_lick: intervalo entre curas, {@code base − (lvl − 1)·per} (nível 1 → 100, nível 2 → 60), mínimo 1. */
    public static int lickInterval(int level, int base, int perLevel) {
        return Math.max(1, base - (level - 1) * perLevel);
    }

    /** Cura liberada: sem dano há {@code quietTicks} e a última cura foi há pelo menos {@code interval}. */
    public static boolean canHeal(long now, long lastHurt, long lastHeal, int quietTicks, int interval) {
        return now - lastHurt >= quietTicks && now - lastHeal >= interval;
    }

    /**
     * tamer_cat_gift: segunda rolagem quando a primeira falha, {@code vanilla + (1 − vanilla)·lvl·per}, limitada a
     * [vanilla, 1]. Com 0,7 vanilla: 0,775 (nível 1) e 0,85 (nível 2).
     */
    public static double catGiftChance(double vanilla, int level, double perLevel) {
        double reroll = Math.max(0, Math.min(1, level * perLevel));
        return vanilla + (1 - vanilla) * reroll;
    }

    /** tamer_eternal: recarga do pet pronta quando o game time alcança o limite gravado. */
    public static boolean eternalReady(long now, long until) {
        return now >= until;
    }
}
