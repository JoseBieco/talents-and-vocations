package com.seunome.vanillatalents.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RespecRulesTest {

    @Test
    void refund_roundsDown() {
        assertEquals(9, RespecRules.refund(36, 25));
        assertEquals(8, RespecRules.refund(35, 25));
        assertEquals(0, RespecRules.refund(0, 25));
    }

    @Test
    void spentClassPoints_ignoresCommonAndOtherClasses() {
        Map<String, Integer> levels = Map.of(
                "common_health", 5,
                "miner_haste", 4,
                "miner_fortune", 2,
                "archer_aim", 3,
                "minerx_fake", 7);
        assertEquals(6, RespecRules.spentClassPoints(levels, "miner"));
        assertEquals(3, RespecRules.spentClassPoints(levels, "archer"));
        assertEquals(0, RespecRules.spentClassPoints(levels, "none"));
    }

    @Test
    void validateChange_firstChoiceIsFree() {
        assertEquals(RespecCheck.OK_FIRST_CHOICE, RespecRules.validateChange("none", "miner", 0, 10));
    }

    @Test
    void validateChange_sameClass() {
        assertEquals(RespecCheck.SAME_CLASS, RespecRules.validateChange("miner", "miner", 50, 10));
    }

    @Test
    void validateChange_invalidClass() {
        assertEquals(RespecCheck.INVALID_CLASS, RespecRules.validateChange("miner", "wizard", 50, 10));
        assertEquals(RespecCheck.INVALID_CLASS, RespecRules.validateChange("miner", "common", 50, 10));
        assertEquals(RespecCheck.INVALID_CLASS, RespecRules.validateChange("miner", "none", 50, 10));
        assertEquals(RespecCheck.INVALID_CLASS, RespecRules.validateChange("none", null, 50, 10));
    }

    @Test
    void validateChange_feeLevels() {
        assertEquals(RespecCheck.NOT_ENOUGH_LEVELS, RespecRules.validateChange("miner", "archer", 9, 10));
        assertEquals(RespecCheck.OK_PAID, RespecRules.validateChange("miner", "archer", 10, 10));
    }
}
