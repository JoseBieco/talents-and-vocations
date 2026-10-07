package com.josebieco.talentsvocations.core.formula;

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
    void isActivity_rotationInBoatCounts() {
        assertTrue(AnglerFormulas.isActivity(0, 0, 5f, 0f, true));
        assertTrue(AnglerFormulas.isActivity(0, 0, 0f, 3f, true));
    }

    @Test
    void isActivity_driftingAsPassengerDoesNotCount() {
        assertFalse(AnglerFormulas.isActivity(0.3, 0, 0f, 0f, true));
    }

    @Test
    void isActivity_noHorizontalMoveNorRotationDoesNotCount() {
        // só dy (boiar/correnteza vertical): o helper nem recebe Y
        assertFalse(AnglerFormulas.isActivity(0, 0, 0f, 0f, false));
    }

    @Test
    void isActivity_walkingCounts() {
        assertTrue(AnglerFormulas.isActivity(0.3, 0, 0f, 0f, false));
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

    @Test
    void scaled_boatAcceleration() {
        assertEquals(0.046, AnglerFormulas.scaled(0.04, 3, 0.05), 1e-9);
    }

    @Test
    void scaled_dolphinDuration() {
        assertEquals(200, AnglerFormulas.scaled(100, 2, 0.5), 1e-9);
    }

    @Test
    void scaled_seaEyesFog() {
        assertEquals(144, AnglerFormulas.scaled(96, 2, 0.25), 1e-9);
    }

    @Test
    void tridentBonus_noHighTide() {
        assertEquals(0.24, AnglerFormulas.tridentBonus(3, 0.08, false, 0.15), 1e-9);
    }

    @Test
    void tridentBonus_highTide() {
        assertEquals(0.39, AnglerFormulas.tridentBonus(3, 0.08, true, 0.15), 1e-9);
    }

    @Test
    void tridentBonus_sharesArcherPool_R1() {
        double pool = ArcherFormulas.damageMultiplier(3, 0.1, 0, 0, false, 0, 0, 0, 0)
                + AnglerFormulas.tridentBonus(3, 0.08, false, 0.15);
        assertEquals(1.54, pool, 1e-9);
    }
}
