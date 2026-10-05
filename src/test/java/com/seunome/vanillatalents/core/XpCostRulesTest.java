package com.seunome.vanillatalents.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XpCostRulesTest {

    @Test
    void totalXpForLevel_matchesVanillaCurve() {
        assertEquals(0, XpCostRules.totalXpForLevel(0));
        assertEquals(55, XpCostRules.totalXpForLevel(5));
        assertEquals(160, XpCostRules.totalXpForLevel(10));
        assertEquals(315, XpCostRules.totalXpForLevel(15));
        assertEquals(352, XpCostRules.totalXpForLevel(16));
        assertEquals(394, XpCostRules.totalXpForLevel(17));
        assertEquals(910, XpCostRules.totalXpForLevel(25));
        assertEquals(1395, XpCostRules.totalXpForLevel(30));
        assertEquals(1507, XpCostRules.totalXpForLevel(31));
        assertEquals(1628, XpCostRules.totalXpForLevel(32));
        assertEquals(2045, XpCostRules.totalXpForLevel(35));
    }

    @Test
    void totalXpForLevel_differencesMatchReadme() {
        assertEquals(55, XpCostRules.totalXpForLevel(5) - XpCostRules.totalXpForLevel(0));
        assertEquals(155, XpCostRules.totalXpForLevel(15) - XpCostRules.totalXpForLevel(10));
        assertEquals(485, XpCostRules.totalXpForLevel(30) - XpCostRules.totalXpForLevel(25));
        assertEquals(650, XpCostRules.totalXpForLevel(35) - XpCostRules.totalXpForLevel(30));
    }

    @Test
    void canAfford_levels() {
        assertFalse(XpCostRules.canAfford(CostMode.LEVELS, 4, 1000, 5));
        assertTrue(XpCostRules.canAfford(CostMode.LEVELS, 5, 0, 5));
    }

    @Test
    void canAfford_points() {
        assertFalse(XpCostRules.canAfford(CostMode.POINTS, 0, 99, 100));
        assertTrue(XpCostRules.canAfford(CostMode.POINTS, 0, 100, 100));
    }
}
