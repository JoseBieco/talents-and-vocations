package com.seunome.vanillatalents.core.formula;

import com.seunome.vanillatalents.core.AttributeBonuses;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WarriorFormulasTest {

    @Test
    void meleeBonus() {
        assertEquals(2.5, WarriorFormulas.meleeBonus(5, .5), 1e-9);
    }

    @Test
    void physicalMultiplier() {
        assertEquals(0.8, WarriorFormulas.physicalMultiplier(5, .04), 1e-9);
    }

    @Test
    void physicalMultiplier_pvpScalesTheReduction() {
        assertEquals(0.8, WarriorFormulas.physicalMultiplier(5, .04, 1.0), 1e-9);
        assertEquals(0.9, WarriorFormulas.physicalMultiplier(5, .04, 0.5), 1e-9);
        assertEquals(1.0, WarriorFormulas.physicalMultiplier(5, .04, 0.0), 1e-9);
    }

    @Test
    void steadfastPvpFactor_restoresPartOfTheKnockback() {
        // KR total 0,3, dos quais 0,3 vêm de Firme; PvP 0,5 → KR efetiva 0,15 → força × 0,85/0,7
        assertEquals(0.85 / 0.7, WarriorFormulas.steadfastPvpFactor(0.3, 0.3, 0.5), 1e-9);
        assertEquals(1.0, WarriorFormulas.steadfastPvpFactor(0.3, 0.3, 1.0), 1e-9);
        assertEquals(1.0, WarriorFormulas.steadfastPvpFactor(1.0, 0.3, 0.5), 1e-9, "KR total ≥ 1: sem repulsão, nada a compensar");
    }

    @Test
    void executeBonus_onlyBelowThreshold() {
        assertEquals(1.3, WarriorFormulas.executeBonus(.29, 3, .1, .30), 1e-9);
        assertEquals(1.0, WarriorFormulas.executeBonus(.30, 3, .1, .30), 1e-9);
    }

    @Test
    void critMultiplier() {
        assertEquals(1.3, WarriorFormulas.critMultiplier(3, .1), 1e-9);
    }

    @Test
    void knockbackBonus_neverAboveTwoTotal() {
        for (int weapon = 0; weapon <= 3; weapon++) {
            for (int lvl = 0; lvl <= 2; lvl++) {
                double bonus = AttributeBonuses.knockbackBonus(lvl, .5, weapon);
                assertTrue(bonus >= 0);
                assertTrue(weapon + bonus <= Math.max(2, weapon), "weapon " + weapon + " lvl " + lvl);
            }
        }
    }

    @Test
    void cleaveTargets_atMostMax() {
        assertEquals(List.of("a", "b", "c"), WarriorFormulas.cleaveTargets(List.of("a", "b", "c", "d"), 3));
        assertEquals(List.of("a"), WarriorFormulas.cleaveTargets(List.of("a"), 3));
    }

    @Test
    void armorBreakRatio_scalesDamageByArmorIgnored() {
        // função de absorção fictícia: dano × (1 − armadura/25)
        WarriorFormulas.Absorb absorb = (damage, armor) -> damage * (1 - armor / 25f);
        assertEquals(1.0, WarriorFormulas.armorBreakRatio(10f, 0f, 3, .05, absorb), 1e-6);
        float full = 10f * (1 - 20f / 25f);
        float reduced = 10f * (1 - 17f / 25f);
        assertEquals(reduced / full, WarriorFormulas.armorBreakRatio(10f, 20f, 3, .05, absorb), 1e-6);
    }

    @Test
    void parryEligible_allTrue() {
        assertTrue(WarriorFormulas.parryEligible(true, true, true));
    }

    @Test
    void parryEligible_noSword() {
        assertFalse(WarriorFormulas.parryEligible(false, true, true));
    }

    @Test
    void parryEligible_projectile() {
        assertFalse(WarriorFormulas.parryEligible(true, false, true));
    }

    @Test
    void parryEligible_onCooldown() {
        assertFalse(WarriorFormulas.parryEligible(true, true, false));
    }

    @Test
    void bonusMultiplier() {
        assertEquals(1.45, WarriorFormulas.bonusMultiplier(3, .15), 1e-9);
        assertEquals(1.4, WarriorFormulas.bonusMultiplier(2, .2), 1e-9);
    }
}
