package com.josebieco.talentsvocations.core.formula;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArtisanFormulasTest {

    @Test
    void anvilCost_discountsFivePercentPerLevelRoundingDown() {
        assertEquals(15, ArtisanFormulas.anvilCost(20, true, 5, .05));
    }

    @Test
    void anvilCost_neverBelowOneWithResult() {
        assertEquals(1, ArtisanFormulas.anvilCost(1, true, 5, .05));
    }

    @Test
    void anvilCost_withoutResultKeepsVanillaCost() {
        assertEquals(39, ArtisanFormulas.anvilCost(39, false, 5, .05));
        assertEquals(0, ArtisanFormulas.anvilCost(0, false, 5, .05));
    }

    @Test
    void anvilCost_levelZeroKeepsCost() {
        assertEquals(20, ArtisanFormulas.anvilCost(20, true, 0, .05));
    }

    @Test
    void repairPlan_fullyDamagedUsesAllUnits() {
        // 1000/4 · 1,3 = 325 por unidade: 4 unidades zeram 1000
        assertEquals(new ArtisanFormulas.RepairPlan(4, 0), ArtisanFormulas.repairPlan(1000, 1000, 4, 3, .1));
    }

    @Test
    void repairPlan_usesOnlyTheUnitsNeeded() {
        assertEquals(new ArtisanFormulas.RepairPlan(2, 0), ArtisanFormulas.repairPlan(500, 1000, 4, 3, .1));
    }

    @Test
    void repairPlan_limitedByAvailable() {
        assertEquals(new ArtisanFormulas.RepairPlan(1, 175), ArtisanFormulas.repairPlan(500, 1000, 1, 3, .1));
    }

    @Test
    void repairPlan_levelZeroMatchesVanilla() {
        // vanilla: 250 por unidade, 3 unidades para 700
        assertEquals(new ArtisanFormulas.RepairPlan(3, 0), ArtisanFormulas.repairPlan(700, 1000, 64, 0, .1));
    }

    @Test
    void masterRepairCost_keepsTheHigherPenaltyWithoutDoubling() {
        assertEquals(7, ArtisanFormulas.masterRepairCost(7, 3));
        assertEquals(3, ArtisanFormulas.masterRepairCost(0, 3));
    }

    @Test
    void breakChance_reducesFifteenPercentPerLevel() {
        assertEquals(.066f, ArtisanFormulas.breakChance(.12f, 3, .15), 1e-6);
    }

    @Test
    void tradeDiscount_roundsDownWithMinimumOneUnit() {
        assertEquals(1, ArtisanFormulas.tradeDiscount(10, 3, .05));
        assertEquals(4, ArtisanFormulas.tradeDiscount(32, 3, .05));
        assertEquals(1, ArtisanFormulas.tradeDiscount(1, 1, .05));
    }

    @Test
    void tradeDiscount_levelZeroIsZero() {
        assertEquals(0, ArtisanFormulas.tradeDiscount(10, 0, .05));
    }

    @Test
    void mentorBonus_wholePartPlusFractionRoll() {
        assertEquals(5, ArtisanFormulas.mentorBonus(10, 2, .25, .99));
        assertEquals(1, ArtisanFormulas.mentorBonus(1, 1, .25, .1));
        assertEquals(0, ArtisanFormulas.mentorBonus(1, 1, .25, .5));
    }

    @Test
    void mentorBonus_levelZeroIsZero() {
        assertEquals(0, ArtisanFormulas.mentorBonus(10, 0, .25, 0));
    }

    @Test
    void potionDuration_tenPercentPerLevel() {
        assertEquals(4680, ArtisanFormulas.potionDuration(3600, 3, .1));
    }

    @Test
    void boostedShelves_capsAtMaxShelves() {
        assertEquals(15, ArtisanFormulas.boostedShelves(15, 2, .25, 15));
    }

    @Test
    void boostedShelves_levelTwoCountsOneAndAHalf() {
        assertEquals(12, ArtisanFormulas.boostedShelves(8, 2, .25, 15));
    }

    @Test
    void boostedShelves_levelOneCountsOneAndAQuarter() {
        assertEquals(10, ArtisanFormulas.boostedShelves(8, 1, .25, 15));
    }

    @Test
    void boostedShelves_noShelvesStaysZero() {
        assertEquals(0, ArtisanFormulas.boostedShelves(0, 2, .25, 15));
    }
}
