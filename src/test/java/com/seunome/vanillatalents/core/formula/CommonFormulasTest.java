package com.seunome.vanillatalents.core.formula;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommonFormulasTest {

    @Test
    void regenExhaustionMultiplier() {
        assertEquals(0.7, CommonFormulas.regenExhaustionMultiplier(3, .1), 1e-9);
        assertEquals(1.0, CommonFormulas.regenExhaustionMultiplier(0, .1), 1e-9);
    }

    @Test
    void fireMultiplier_lavaStacksMultiplicatively() {
        assertEquals(0.532, CommonFormulas.fireMultiplier(5, .06, 3, .08, true), 1e-6);
        assertEquals(0.70, CommonFormulas.fireMultiplier(5, .06, 3, .08, false), 1e-6);
        assertEquals(0.76, CommonFormulas.fireMultiplier(0, .06, 3, .08, true), 1e-6);
    }

    @Test
    void secondWindTriggers_atOrBelowThresholdButAlive() {
        assertTrue(CommonFormulas.secondWindTriggers(4.0f, 4));
        assertFalse(CommonFormulas.secondWindTriggers(4.5f, 4));
        assertFalse(CommonFormulas.secondWindTriggers(0f, 4), "dano fatal não dispara");
    }

    @Test
    void gourmetExtraSaturation() {
        assertEquals(3.84, CommonFormulas.gourmetExtraSaturation(12.8, 3, .1), 1e-6);
    }

    @Test
    void antidoteDuration_roundsAndNeverBelowOne() {
        assertEquals(420, CommonFormulas.antidoteDuration(600, 3, .1));
        assertEquals(1, CommonFormulas.antidoteDuration(1, 3, .1));
        assertEquals(600, CommonFormulas.antidoteDuration(600, 0, .1));
    }
}
