package com.seunome.vanillatalents.core.formula;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnglerFormulasTest {

    @Test
    void lureTicks_lureOnly() {
        assertEquals(240, AnglerFormulas.lureTicks(400, 5, 0.08, false, 0.25));
    }

    @Test
    void lureTicks_highTideMultipliesOnTop() {
        assertEquals(180, AnglerFormulas.lureTicks(400, 5, 0.08, true, 0.25));
    }

    @Test
    void lureTicks_nonPositiveIsUntouched() {
        assertEquals(0, AnglerFormulas.lureTicks(0, 5, 0.08, true, 0.25));
    }

    @Test
    void lureTicks_levelZeroIsVanilla() {
        assertEquals(400, AnglerFormulas.lureTicks(400, 0, 0.08, false, 0.25));
    }

    @Test
    void lureTicks_neverBelowOne() {
        assertEquals(1, AnglerFormulas.lureTicks(1, 5, 0.08, true, 0.25));
    }

    @Test
    void active_idleLimitIsInclusive() {
        assertTrue(AnglerFormulas.active(0, 1200, 1200));
        assertFalse(AnglerFormulas.active(0, 1201, 1200));
    }

    @Test
    void catchResult_nonFishIsNeverDoubledNorCooked() {
        assertEquals(new AnglerFormulas.Catch(1, false), AnglerFormulas.catchResult(false, true, true));
    }

    @Test
    void catchResult_fishDoubledRaw() {
        assertEquals(new AnglerFormulas.Catch(2, false), AnglerFormulas.catchResult(true, true, false));
    }

    @Test
    void catchResult_fishSingleCooked() {
        assertEquals(new AnglerFormulas.Catch(1, true), AnglerFormulas.catchResult(true, false, true));
    }
}
