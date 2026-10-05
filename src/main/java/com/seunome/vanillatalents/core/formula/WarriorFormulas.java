package com.seunome.vanillatalents.core.formula;

import java.util.List;

/** Fórmulas da árvore do Guerreiro (repulsão fica em AttributeBonuses.knockbackBonus). */
public final class WarriorFormulas {

    /** Dano depois da armadura, para uma dada armadura (CombatRules no jogo). */
    @FunctionalInterface
    public interface Absorb {
        float apply(float damage, float armor);
    }

    private WarriorFormulas() {}

    /** warrior_strength: dano corpo a corpo adicional. */
    public static double meleeBonus(int level, double perLevel) {
        return level * perLevel;
    }

    /** warrior_resistance: fator sobre dano físico recebido. */
    public static double physicalMultiplier(int level, double perLevel) {
        return HookFormulas.reductionMultiplier(level, perLevel);
    }

    /** warrior_executioner: fator de dano contra alvo com vida abaixo do limite. */
    public static double executeBonus(double targetHealthFraction, int level, double perLevel, double threshold) {
        return targetHealthFraction < threshold ? 1 + level * perLevel : 1;
    }

    /** warrior_crit: fator extra sobre o multiplicador de crítico. */
    public static double critMultiplier(int level, double perLevel) {
        return 1 + level * perLevel;
    }

    /** warrior_cleave: os mais próximos primeiro (lista já ordenada), até {@code max}. */
    public static <T> List<T> cleaveTargets(List<T> sortedByDistance, int max) {
        return sortedByDistance.subList(0, Math.min(Math.max(0, max), sortedByDistance.size()));
    }

    /**
     * warrior_armor_break: fator a aplicar no dano antes da armadura para que o resultado seja o de um alvo com
     * {@code per × nível} a menos de armadura.
     */
    public static double armorBreakRatio(float damage, float armor, int level, double perLevel, Absorb absorb) {
        if (armor <= 0 || damage <= 0) return 1;
        float full = absorb.apply(damage, armor);
        if (full <= 0) return 1;
        float reduced = absorb.apply(damage, (float) (armor * HookFormulas.reductionMultiplier(level, perLevel)));
        return reduced / full;
    }
}
