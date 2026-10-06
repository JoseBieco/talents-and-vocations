package com.seunome.vanillatalents.core.formula;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StackingFormulasTest {

    @Test
    void combatMultiplier_multipliesBelowTheCap() {
        assertEquals(0.56, StackingFormulas.combatMultiplier(List.of(0.8, 0.7), 0.5), 1e-9);
    }

    @Test
    void combatMultiplier_isLimitedByTheCap() {
        assertEquals(0.5, StackingFormulas.combatMultiplier(List.of(0.6, 0.7), 0.5), 1e-9);
    }

    @Test
    void combatMultiplier_emptyListIsOne() {
        assertEquals(1.0, StackingFormulas.combatMultiplier(List.of(), 0.5), 1e-9);
    }

    @Test
    void combinedChance_rollsIndependentlyAndIsLimitedByTheCap() {
        assertEquals(0.5, StackingFormulas.combinedChance(List.of(0.4, 0.2), 0.5), 1e-9);
    }

    @Test
    void combinedChance_singleSourceBelowTheCapIsUnchanged() {
        assertEquals(0.4, StackingFormulas.combinedChance(List.of(0.4), 0.5), 1e-9);
    }
}
