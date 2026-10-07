package com.josebieco.talentsvocations.core.formula;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArcherFormulasTest {

    @Test
    void damageMultiplier_bonusesAreAdditive() {
        assertEquals(1.74, ArcherFormulas.damageMultiplier(5, .04, 3, .08, true, 3, .1, 25, 20), 1e-9);
        assertEquals(1.44, ArcherFormulas.damageMultiplier(5, .04, 3, .08, true, 3, .1, 20, 20), 1e-9);
        assertEquals(1.50, ArcherFormulas.damageMultiplier(5, .04, 3, .08, false, 3, .1, 25, 20), 1e-9);
    }

    @Test
    void drawProgressPerTick() {
        assertEquals(1 / 0.76, ArcherFormulas.drawProgressPerTick(4, .06), 1e-4);
    }

    @Test
    void reloadTicks_withFloor() {
        assertEquals(17, ArcherFormulas.reloadTicks(25, 4, .08, 8));
        assertEquals(8, ArcherFormulas.reloadTicks(10, 4, .08, 8));
    }

    @Test
    void conserveChance() {
        assertEquals(0.40, ArcherFormulas.conserveChance(5, .08), 1e-9);
    }

    @Test
    void accumulate_convertsFractionalProgressIntoWholeTicks() {
        // fator 1,5: 0,5 extra por tick → um tick extra a cada 2 ticks
        ArcherFormulas.Step s1 = ArcherFormulas.accumulate(0, 1.5);
        assertEquals(0, s1.extraTicks());
        ArcherFormulas.Step s2 = ArcherFormulas.accumulate(s1.remainder(), 1.5);
        assertEquals(1, s2.extraTicks());
        assertEquals(0, s2.remainder(), 1e-9);
        assertEquals(0, ArcherFormulas.accumulate(0, 1.0).extraTicks());
    }

    @Test
    void mobileInputFactor_restoresPartOfUseSlowdown() {
        // vanilla 0,2; −25% da lentidão por nível: nível 2 → 0,2 + 0,8×0,5 = 0,6 → fator 3
        assertEquals(3.0, ArcherFormulas.mobileInputFactor(2, .25, 0.2), 1e-9);
        assertEquals(1.0, ArcherFormulas.mobileInputFactor(0, .25, 0.2), 1e-9);
    }

    @Test
    void damageMultiplier_antiAirJoinsAdditivePool() {
        assertEquals(2.04, ArcherFormulas.damageMultiplier(5, .04, 3, .08, true, 3, .1, 25, 20, 3, .1, true), 1e-9);
        assertEquals(1.74, ArcherFormulas.damageMultiplier(5, .04, 3, .08, true, 3, .1, 25, 20, 3, .1, false), 1e-9);
    }

    @Test
    void alchemyDuration_extendsArrowEffect() {
        assertEquals(140, ArcherFormulas.alchemyDuration(100, 100, 2, .2));
    }

    @Test
    void alchemyDuration_keepsLongerExisting() {
        assertEquals(300, ArcherFormulas.alchemyDuration(300, 100, 2, .2));
    }
}
