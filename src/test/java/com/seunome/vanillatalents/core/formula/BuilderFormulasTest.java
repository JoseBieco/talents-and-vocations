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
    void glassDrop_onlyGlassWithEmptyDropsAndNoSilkTouch() {
        assertTrue(BuilderFormulas.glassDrop(true, true, false));
        assertFalse(BuilderFormulas.glassDrop(true, true, true));
        assertFalse(BuilderFormulas.glassDrop(true, false, false));
        assertFalse(BuilderFormulas.glassDrop(false, true, false));
    }
}
