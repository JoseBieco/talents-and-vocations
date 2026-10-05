package com.seunome.vanillatalents.core.formula;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HookFormulasTest {

    @Test
    void reductionMultiplier_isOneMinusLevelTimesPerClampedAtZero() {
        assertEquals(1.0, HookFormulas.reductionMultiplier(0, 0.1), 1e-9);
        assertEquals(0.7, HookFormulas.reductionMultiplier(3, 0.1), 1e-9);
        assertEquals(0.4, HookFormulas.reductionMultiplier(4, 0.15), 1e-9);
        assertEquals(0.4, HookFormulas.reductionMultiplier(3, 0.2), 1e-9);
        assertEquals(0.5, HookFormulas.reductionMultiplier(2, 0.25), 1e-9);
        assertEquals(0.0, HookFormulas.reductionMultiplier(10, 0.2), 1e-9);
    }

    @Test
    void chance_isClampedBetweenZeroAndOne() {
        assertEquals(0.24, HookFormulas.chance(3, 0.08), 1e-9);
        assertEquals(1.0, HookFormulas.chance(10, 0.15), 1e-9);
        assertEquals(0.0, HookFormulas.chance(0, 0.15), 1e-9);
    }

    @Test
    void keptDamage_rollsOncePerPoint() {
        Iterator<Double> rolls = List.of(0.1, 0.5, 0.2, 0.9).iterator();
        // chance de pular 0.3: rolagens < 0.3 são puladas (0.1 e 0.2) → 2 de 4 pontos ficam
        assertEquals(2, HookFormulas.keptDamage(4, 0.3, rolls::next));
        assertEquals(5, HookFormulas.keptDamage(5, 0.0, () -> 0.0));
        assertEquals(0, HookFormulas.keptDamage(0, 0.5, () -> 0.0));
        assertEquals(-2, HookFormulas.keptDamage(-2, 1.0, () -> 0.0), "reparo (negativo) não é alterado");
    }
}
