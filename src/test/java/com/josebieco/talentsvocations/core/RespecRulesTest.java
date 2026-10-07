package com.josebieco.talentsvocations.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.josebieco.talentsvocations.core.ClassSlot.PRIMARY;
import static com.josebieco.talentsvocations.core.ClassSlot.SECONDARY;
import static com.josebieco.talentsvocations.core.TalentRegistryTest.node;
import static org.junit.jupiter.api.Assertions.*;

class RespecRulesTest {

    static final TalentRegistry EMPTY = TalentRegistry.build(List.of(), new ArrayList<>());

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
        assertEquals(6, RespecRules.spentClassPoints(levels, EMPTY, "miner"));
        assertEquals(3, RespecRules.spentClassPoints(levels, EMPTY, "archer"));
        assertEquals(0, RespecRules.spentClassPoints(levels, EMPTY, "none"));
    }

    @Test
    void spentPoints_useCost() {
        TalentRegistry reg = TalentRegistry.build(List.of(
                node("miner_haste", TreeCategory.MINER, 5, 0, 0),
                TalentRulesTest.special("miner_big", TreeCategory.MINER, 2, 0, 1, 3, false, List.of())
        ), new ArrayList<>());
        // 4×1 + 2×3 + "miner_gone" fora do registro conta cost 1 (2×1)
        Map<String, Integer> levels = Map.of("miner_haste", 4, "miner_big", 2, "miner_gone", 2, "common_health", 5);
        assertEquals(12, RespecRules.spentClassPoints(levels, reg, "miner"));
    }

    @Test
    void validateChange_firstChoiceIsFree() {
        assertEquals(RespecCheck.OK_FIRST_CHOICE, RespecRules.validateChange(PRIMARY, "none", "none", "miner", true, 0, 10));
    }

    @Test
    void validateChange_sameClass() {
        assertEquals(RespecCheck.SAME_CLASS, RespecRules.validateChange(PRIMARY, "miner", "none", "miner", true, 50, 10));
    }

    @Test
    void validateChange_invalidClass() {
        assertEquals(RespecCheck.INVALID_CLASS, RespecRules.validateChange(PRIMARY, "miner", "none", "wizard", true, 50, 10));
        assertEquals(RespecCheck.INVALID_CLASS, RespecRules.validateChange(PRIMARY, "miner", "none", "common", true, 50, 10));
        assertEquals(RespecCheck.INVALID_CLASS, RespecRules.validateChange(PRIMARY, "miner", "none", "none", true, 50, 10));
        assertEquals(RespecCheck.INVALID_CLASS, RespecRules.validateChange(PRIMARY, "none", "none", null, true, 50, 10));
    }

    @Test
    void validateChange_feeLevels() {
        assertEquals(RespecCheck.NOT_ENOUGH_LEVELS, RespecRules.validateChange(PRIMARY, "miner", "none", "archer", true, 9, 10));
        assertEquals(RespecCheck.OK_PAID, RespecRules.validateChange(PRIMARY, "miner", "none", "archer", true, 10, 10));
    }

    @Test
    void secondary_firstChoiceFree() {
        assertEquals(RespecCheck.OK_FIRST_CHOICE, RespecRules.validateChange(SECONDARY, "none", "miner", "archer", true, 0, 10));
        assertEquals(RespecCheck.NOT_ENOUGH_LEVELS, RespecRules.validateChange(SECONDARY, "archer", "miner", "farmer", true, 0, 10));
        assertEquals(RespecCheck.OK_PAID, RespecRules.validateChange(SECONDARY, "archer", "miner", "farmer", true, 10, 10));
    }

    @Test
    void secondary_lockedWithoutVocation() {
        assertEquals(RespecCheck.SLOT_LOCKED, RespecRules.validateChange(SECONDARY, "none", "miner", "archer", false, 50, 10));
        // classe inválida é reportada antes do bloqueio
        assertEquals(RespecCheck.INVALID_CLASS, RespecRules.validateChange(SECONDARY, "none", "miner", "wizard", false, 50, 10));
    }

    @Test
    void sameAsOtherSlot_refused() {
        // secundária igual à principal
        assertEquals(RespecCheck.SAME_CLASS, RespecRules.validateChange(SECONDARY, "none", "miner", "miner", true, 50, 10));
        assertEquals(RespecCheck.SAME_CLASS, RespecRules.validateChange(SECONDARY, "archer", "miner", "miner", true, 50, 10));
        // principal igual à secundária
        assertEquals(RespecCheck.SAME_CLASS, RespecRules.validateChange(PRIMARY, "miner", "archer", "archer", true, 50, 10));
    }
}
