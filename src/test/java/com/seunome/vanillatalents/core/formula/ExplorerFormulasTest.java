package com.seunome.vanillatalents.core.formula;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExplorerFormulasTest {

    @Test
    void fallMultiplier_rollAndCap() {
        assertEquals(0.4, ExplorerFormulas.fallMultiplier(5, .1, true, .2, .6), 1e-9);
        assertEquals(0.5, ExplorerFormulas.fallMultiplier(5, .1, false, .2, .6), 1e-9);
        assertEquals(0.7, ExplorerFormulas.fallMultiplier(3, .1, false, .2, .6), 1e-9);
    }

    @Test
    void fallMultiplier_neverBelowCap() {
        // −50% e −20% de rolagem daria 0,4; com teto de 50% fica em 0,5
        assertEquals(0.5, ExplorerFormulas.fallMultiplier(5, .1, true, .2, .5), 1e-9);
        assertEquals(0.8, ExplorerFormulas.fallMultiplier(0, .1, true, .2, .6), 1e-9);
    }

    @Test
    void featherfootDistance_shiftsByTwelveBlocks() {
        assertEquals(31.0, ExplorerFormulas.featherfootDistance(40.0, 12, 3), 1e-9);
        assertEquals(1.0, ExplorerFormulas.featherfootDistance(10.0, 12, 3), 1e-9);
        assertEquals(0.0, ExplorerFormulas.featherfootDistance(5.0, 12, 3), 1e-9);
    }

    @Test
    void mountBonus() {
        assertEquals(0.15, ExplorerFormulas.mountBonus(3, .05), 1e-9);
    }
}
