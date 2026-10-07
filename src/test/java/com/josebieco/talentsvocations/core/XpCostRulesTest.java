package com.josebieco.talentsvocations.core;

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
    void xpNeededForNextLevel_matchesVanilla() {
        assertEquals(7, XpCostRules.xpNeededForNextLevel(0));
        assertEquals(35, XpCostRules.xpNeededForNextLevel(14));
        assertEquals(37, XpCostRules.xpNeededForNextLevel(15));
        assertEquals(107, XpCostRules.xpNeededForNextLevel(29));
        assertEquals(112, XpCostRules.xpNeededForNextLevel(30));
        for (int level = 0; level < 40; level++) {
            assertEquals(XpCostRules.totalXpForLevel(level + 1) - XpCostRules.totalXpForLevel(level),
                    XpCostRules.xpNeededForNextLevel(level), "level " + level);
        }
    }

    @Test
    void currentTotalXp_addsBarProgress() {
        assertEquals(55, XpCostRules.currentTotalXp(5, 0f));
        assertEquals(55 + 9, XpCostRules.currentTotalXp(5, 0.5f)); // nível 5 precisa de 17
        assertEquals(0, XpCostRules.currentTotalXp(0, 0f));
    }

    @Test
    void maxConversions_levels() {
        assertEquals(4, XpCostRules.maxConversions(CostMode.LEVELS, 23, 0.9f, 5));
        assertEquals(0, XpCostRules.maxConversions(CostMode.LEVELS, 4, 0.9f, 5));
        assertEquals(1, XpCostRules.maxConversions(CostMode.LEVELS, 5, 0f, 5));
    }

    @Test
    void maxConversions_points() {
        // nível 10 = 160 pontos; + metade da barra do nível 10 (27/2 ≈ 14) = 174 → 1 PT de 100
        assertEquals(1, XpCostRules.maxConversions(CostMode.POINTS, 10, 0.5f, 100));
        assertEquals(13, XpCostRules.maxConversions(CostMode.POINTS, 30, 0f, 100)); // 1395 pontos
        assertEquals(0, XpCostRules.maxConversions(CostMode.POINTS, 0, 0f, 100));
    }

    @Test
    void maxConversions_zeroCostIsSafe() {
        assertEquals(0, XpCostRules.maxConversions(CostMode.LEVELS, 50, 0f, 0));
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
