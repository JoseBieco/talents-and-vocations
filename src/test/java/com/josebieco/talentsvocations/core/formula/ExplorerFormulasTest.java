package com.josebieco.talentsvocations.core.formula;

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
    void fallMultiplier_withSaddle() {
        assertEquals(0.5, ExplorerFormulas.fallMultiplier(0, 0, false, 0, 2, .25, .6), 1e-9);
        // −50% × −20% × −50% daria 0,2; o teto de 60% segura em 0,4
        assertEquals(0.4, ExplorerFormulas.fallMultiplier(5, .1, true, .2, 2, .25, .6), 1e-9);
        assertEquals(0.7, ExplorerFormulas.fallMultiplier(3, .1, false, .2, 0, .25, .6), 1e-9);
    }

    @Test
    void climbBoost_multipliesOnlyWhenVanillaResetsY() {
        assertEquals(0.1176 * 1.6, ExplorerFormulas.climbBoost(0.1176, 1.6, true, false, false), 1e-9);
        assertEquals(0.1176, ExplorerFormulas.climbBoost(0.1176, 1.6, false, false, false), 1e-9);
        assertEquals(0.1176, ExplorerFormulas.climbBoost(0.1176, 1.6, true, true, false), 1e-9);
        assertEquals(0.1176, ExplorerFormulas.climbBoost(0.1176, 1.6, true, false, true), 1e-9);
        assertEquals(-0.15, ExplorerFormulas.climbBoost(-0.15, 1.6, true, false, false), 1e-9);
    }

    @Test
    void climbBoost_isBoundedByTheVanillaClimbSpeed() {
        // lançado para cima (carga de vento) dentro do andaime: 0,8 × 1,6 seria 1,28; limite 0,2 × 1,6
        assertEquals(0.32, ExplorerFormulas.climbBoost(0.8, 1.6, true, false, false), 1e-9);
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

    @Test
    void climbFactor() {
        assertEquals(1.5, ExplorerFormulas.climbFactor(2, .25), 1e-9);
        assertEquals(1.0, ExplorerFormulas.climbFactor(0, .25), 1e-9);
    }

    @Test
    void tailwindLifetime() {
        assertEquals(29, ExplorerFormulas.tailwindLifetime(20, 3, .15));
        assertEquals(20, ExplorerFormulas.tailwindLifetime(20, 0, .15));
    }

    @Test
    void phantomMinRestTicks() {
        assertEquals(96000, ExplorerFormulas.phantomMinRestTicks(1, 72000, 24000));
        assertEquals(120000, ExplorerFormulas.phantomMinRestTicks(2, 72000, 24000));
    }
}
