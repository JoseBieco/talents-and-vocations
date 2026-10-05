package com.seunome.vanillatalents.server;

import com.seunome.vanillatalents.capability.PlayerSkillData;
import com.seunome.vanillatalents.core.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.seunome.vanillatalents.core.TalentRegistryTest.node;
import static com.seunome.vanillatalents.core.TalentRegistryTest.req;
import static org.junit.jupiter.api.Assertions.*;

class SkillTransactionsTest {

    static final TalentRegistry REG = TalentRegistry.build(List.of(
            node("common_health", TreeCategory.COMMON, 5, 0, 0),
            node("miner_haste", TreeCategory.MINER, 5, 0, 0),
            node("miner_darkvision", TreeCategory.MINER, 1, 0, 1, req("miner_haste", 3)),
            node("archer_aim", TreeCategory.ARCHER, 5, 0, 0)
    ), new ArrayList<>());

    static PlayerSkillData data(String cls, int points) {
        PlayerSkillData d = new PlayerSkillData();
        d.setCurrentClass(cls);
        d.addPoints(points);
        return d;
    }

    @Test
    void isValidId_rejectsNullAndLongStrings() {
        assertTrue(SkillTransactions.isValidId("common_health"));
        assertTrue(SkillTransactions.isValidId("a".repeat(64)));
        assertFalse(SkillTransactions.isValidId("a".repeat(65)));
        assertFalse(SkillTransactions.isValidId(null));
    }

    @Test
    void buy_okSpendsOnePointAndUpgrades() {
        PlayerSkillData d = data("miner", 2);
        assertEquals(PurchaseResult.OK, SkillTransactions.buy(d, REG, "miner_haste"));
        assertEquals(1, d.getAvailablePoints());
        assertEquals(1, d.getNodeLevel("miner_haste"));
    }

    @Test
    void buy_rejectedLeavesStateUntouched() {
        PlayerSkillData d = data("miner", 3);
        assertEquals(PurchaseResult.WRONG_CLASS, SkillTransactions.buy(d, REG, "archer_aim"));
        assertEquals(PurchaseResult.PREREQUISITE_NOT_MET, SkillTransactions.buy(d, REG, "miner_darkvision"));
        assertEquals(PurchaseResult.UNKNOWN_NODE, SkillTransactions.buy(d, REG, "x".repeat(65)));
        assertEquals(3, d.getAvailablePoints());
        assertTrue(d.getUnlockedNodes().isEmpty());
    }

    @Test
    void changeClass_firstChoiceIsFreeAndKeepsPoints() {
        PlayerSkillData d = data("none", 4);
        d.upgradeNode("common_health");
        var result = SkillTransactions.changeClass(d, "miner", 0, 10, 25);
        assertEquals(RespecCheck.OK_FIRST_CHOICE, result.check());
        assertEquals(0, result.feeLevels());
        assertEquals("miner", d.getCurrentClass());
        assertEquals(4, d.getAvailablePoints());
        assertEquals(Map.of("common_health", 1), d.getUnlockedNodes());
    }

    @Test
    void changeClass_paidResetsClassTreeKeepsCommonAndRefunds() {
        PlayerSkillData d = data("miner", 1);
        for (int i = 0; i < 5; i++) d.upgradeNode("common_health");
        for (int i = 0; i < 11; i++) d.upgradeNode("miner_haste");
        d.upgradeNode("miner_darkvision");
        var result = SkillTransactions.changeClass(d, "archer", 10, 10, 25);
        assertEquals(RespecCheck.OK_PAID, result.check());
        assertEquals(10, result.feeLevels());
        assertEquals(3, result.refund());
        assertEquals("archer", d.getCurrentClass());
        assertEquals(1 + 3, d.getAvailablePoints());
        assertEquals(Map.of("common_health", 5), d.getUnlockedNodes());
    }

    @Test
    void changeClass_withNothingSpentIsFreeEvenWithoutLevels() {
        PlayerSkillData d = data("miner", 2);
        d.upgradeNode("common_health");
        var result = SkillTransactions.changeClass(d, "archer", 0, 10, 25);
        assertEquals(RespecCheck.OK_PAID, result.check());
        assertEquals(0, result.feeLevels());
        assertEquals(0, result.refund());
        assertEquals("archer", d.getCurrentClass());
        assertEquals(2, d.getAvailablePoints());
        assertEquals(Map.of("common_health", 1), d.getUnlockedNodes());
    }

    @Test
    void changeClass_rejectedChangesNothing() {
        PlayerSkillData d = data("miner", 2);
        d.upgradeNode("miner_haste");
        assertEquals(RespecCheck.SAME_CLASS, SkillTransactions.changeClass(d, "miner", 50, 10, 25).check());
        assertEquals(RespecCheck.NOT_ENOUGH_LEVELS, SkillTransactions.changeClass(d, "archer", 9, 10, 25).check());
        assertEquals(RespecCheck.INVALID_CLASS, SkillTransactions.changeClass(d, "wizard", 50, 10, 25).check());
        assertEquals(RespecCheck.INVALID_CLASS, SkillTransactions.changeClass(d, "z".repeat(65), 50, 10, 25).check());
        assertEquals("miner", d.getCurrentClass());
        assertEquals(2, d.getAvailablePoints());
        assertEquals(Map.of("miner_haste", 1), d.getUnlockedNodes());
    }
}
