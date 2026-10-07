package com.seunome.vanillatalents.core.formula;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BuilderFormulasTest {

    @Test
    void breakMultiplier_isOnePlusLevelTimesPerLevel() {
        assertEquals(1.6, BuilderFormulas.breakMultiplier(3, .2), 1e-9);
        assertEquals(1.0, BuilderFormulas.breakMultiplier(0, .2), 1e-9);
    }

    @Test
    void recentlyPlaced_withinTheWindowInclusive() {
        assertTrue(BuilderFormulas.recentlyPlaced(0, 100, 100));
        assertFalse(BuilderFormulas.recentlyPlaced(0, 101, 100));
    }

    @Test
    void recentlyPlaced_neverPlacedIsFalse() {
        assertFalse(BuilderFormulas.recentlyPlaced(Long.MIN_VALUE, 50, 100));
    }

    @Test
    void scaffoldFactor_isOnePlusLevelTimesPerLevel() {
        assertEquals(1.6, BuilderFormulas.scaffoldFactor(2, .3), 1e-9);
        assertEquals(1.0, BuilderFormulas.scaffoldFactor(0, .3), 1e-9);
    }

    @Test
    void glassDrop_onlyGlassWithEmptyDropsAndNoSilkTouch() {
        assertTrue(BuilderFormulas.glassDrop(true, true, false));
        assertFalse(BuilderFormulas.glassDrop(true, true, true));
        assertFalse(BuilderFormulas.glassDrop(true, false, false));
        assertFalse(BuilderFormulas.glassDrop(false, true, false));
    }

    @Test
    void refundChance_combinesThriftyAndTorchIndependently() {
        assertEquals(.2, BuilderFormulas.refundChance(true, 4, .05, false, 0, .25), 1e-9);
        assertEquals(.6, BuilderFormulas.refundChance(true, 4, .05, true, 2, .25), 1e-9);
        assertEquals(0, BuilderFormulas.refundChance(false, 4, .05, false, 2, .25), 1e-9);
    }

    @Test
    void quickDelay_onlyShortensWithBlockItem() {
        assertEquals(2, BuilderFormulas.quickDelay(4, true, 2));
        assertEquals(4, BuilderFormulas.quickDelay(4, false, 2));
        assertEquals(1, BuilderFormulas.quickDelay(1, true, 2));
    }
}
