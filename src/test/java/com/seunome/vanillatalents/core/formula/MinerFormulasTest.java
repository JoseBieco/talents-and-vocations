package com.seunome.vanillatalents.core.formula;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MinerFormulasTest {

    @Test
    void breakSpeed_hasteAndDenseStoneMultiply() {
        assertEquals(2.175, MinerFormulas.breakSpeedMultiplier(5, .1, 3, .15, true, 0, true), 1e-9);
    }

    @Test
    void breakSpeed_denseBonusOnlyOnDenseStone() {
        assertEquals(1.5, MinerFormulas.breakSpeedMultiplier(5, .1, 0, .15, false, 0, true), 1e-9);
        assertEquals(1.5, MinerFormulas.breakSpeedMultiplier(5, .1, 3, .15, false, 0, true), 1e-9);
    }

    @Test
    void breakSpeed_footingOffsetsAirPenalty() {
        assertEquals(1.0, MinerFormulas.breakSpeedMultiplier(0, .1, 0, .15, false, 0, false), 1e-9);
        assertEquals(3.0, MinerFormulas.breakSpeedMultiplier(0, .1, 0, .15, false, 1, false), 1e-9);
        assertEquals(5.0, MinerFormulas.breakSpeedMultiplier(0, .1, 0, .15, false, 2, false), 1e-9);
        assertEquals(1.0, MinerFormulas.breakSpeedMultiplier(0, .1, 0, .15, false, 2, true), 1e-9);
    }

    @Test
    void lavasenseCooldownTicks_byLevel() {
        assertEquals(1800, MinerFormulas.lavasenseCooldownTicks(1, 1800, 1200, 600));
        assertEquals(1200, MinerFormulas.lavasenseCooldownTicks(2, 1800, 1200, 600));
        assertEquals(600, MinerFormulas.lavasenseCooldownTicks(3, 1800, 1200, 600));
    }

    @Test
    void oreXp_roundsBonus() {
        assertEquals(13, MinerFormulas.oreXp(10, 3, .1));
        assertEquals(4, MinerFormulas.oreXp(3, 3, .1)); // 3,9 → 4
        assertEquals(0, MinerFormulas.oreXp(0, 3, .1));
    }

    @Test
    void shovelMultiplier_andProspectorRadius() {
        assertEquals(1.3, MinerFormulas.shovelMultiplier(3, .1), 1e-9);
        assertEquals(6, MinerFormulas.prospectorRadius(3, 2));
    }

    @Test
    void fortuneChance_isPerLevel() {
        assertEquals(0.20, MinerFormulas.fortuneChance(4, .05), 1e-9);
    }
}
