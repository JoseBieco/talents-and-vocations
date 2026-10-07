package com.seunome.vanillatalents.core.formula;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TamerFormulasTest {

    @Test
    void packMultiplier_countsWolvesUpToTheCap() {
        assertEquals(0.88, TamerFormulas.packMultiplier(2, 0.02, 3, 3), 1e-9);
        assertEquals(0.88, TamerFormulas.packMultiplier(2, 0.02, 30, 3), 1e-9);
        assertEquals(1.0, TamerFormulas.packMultiplier(1, 0.02, 0, 3), 1e-9);
    }

    @Test
    void catGiftChance_rerollsTheMiss() {
        assertEquals(0.775, TamerFormulas.catGiftChance(.7, 1, .25), 1e-9);
        assertEquals(0.85, TamerFormulas.catGiftChance(.7, 2, .25), 1e-9);
        assertEquals(0.7, TamerFormulas.catGiftChance(.7, 0, .25), 1e-9);
    }

    @Test
    void lickInterval_shrinksPerLevel() {
        assertEquals(100, TamerFormulas.lickInterval(1, 100, 40));
        assertEquals(60, TamerFormulas.lickInterval(2, 100, 40));
    }

    @Test
    void canHeal_needsQuietTimeAndInterval() {
        assertTrue(TamerFormulas.canHeal(200, 90, 0, 100, 100));
        assertFalse(TamerFormulas.canHeal(150, 90, 0, 100, 100));
    }

    @Test
    void eternalReady_whenCooldownPassed() {
        assertTrue(TamerFormulas.eternalReady(100, 100));
        assertFalse(TamerFormulas.eternalReady(99, 100));
    }
}
