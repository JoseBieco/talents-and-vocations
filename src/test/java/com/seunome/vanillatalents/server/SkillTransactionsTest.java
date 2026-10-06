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
        d.setPrimaryClass(cls);
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
    void buy_deductsNodeCost() {
        TalentRegistry reg = TalentRulesTest.MC_REG;
        PlayerSkillData d = data("miner", 10);
        for (int i = 0; i < 5; i++) d.upgradeNode("common_health");
        d.upgradeNode("common_second_wind");
        for (int i = 0; i < 5; i++) d.upgradeNode("miner_haste");
        d.upgradeNode("miner_vein");
        assertEquals(PurchaseResult.OK, SkillTransactions.buy(d, reg, TalentRules.SECOND_VOCATION));
        assertEquals(0, d.getAvailablePoints());
        assertEquals(1, d.getNodeLevel(TalentRules.SECOND_VOCATION));
    }

    @Test
    void buy_secondVocationWithNinePointsChangesNothing() {
        TalentRegistry reg = TalentRulesTest.MC_REG;
        PlayerSkillData d = data("miner", 9);
        for (int i = 0; i < 5; i++) d.upgradeNode("common_health");
        d.upgradeNode("common_second_wind");
        for (int i = 0; i < 5; i++) d.upgradeNode("miner_haste");
        d.upgradeNode("miner_vein");
        assertEquals(PurchaseResult.NOT_ENOUGH_POINTS, SkillTransactions.buy(d, reg, TalentRules.SECOND_VOCATION));
        assertEquals(9, d.getAvailablePoints());
        assertEquals(0, d.getNodeLevel(TalentRules.SECOND_VOCATION));
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
        var result = SkillTransactions.changeClass(d, REG, ClassSlot.PRIMARY, "miner", 0, 10, 25);
        assertEquals(RespecCheck.OK_FIRST_CHOICE, result.check());
        assertEquals(0, result.feeLevels());
        assertEquals("miner", d.getPrimaryClass());
        assertEquals(4, d.getAvailablePoints());
        assertEquals(Map.of("common_health", 1), d.getUnlockedNodes());
    }

    @Test
    void changeClass_paidResetsClassTreeKeepsCommonAndRefunds() {
        PlayerSkillData d = data("miner", 1);
        for (int i = 0; i < 5; i++) d.upgradeNode("common_health");
        for (int i = 0; i < 11; i++) d.upgradeNode("miner_haste");
        d.upgradeNode("miner_darkvision");
        var result = SkillTransactions.changeClass(d, REG, ClassSlot.PRIMARY, "archer", 10, 10, 25);
        assertEquals(RespecCheck.OK_PAID, result.check());
        assertEquals(10, result.feeLevels());
        assertEquals(3, result.refund());
        assertEquals("archer", d.getPrimaryClass());
        assertEquals(1 + 3, d.getAvailablePoints());
        assertEquals(Map.of("common_health", 5), d.getUnlockedNodes());
    }

    @Test
    void changeClass_withNothingSpentIsFreeEvenWithoutLevels() {
        PlayerSkillData d = data("miner", 2);
        d.upgradeNode("common_health");
        var result = SkillTransactions.changeClass(d, REG, ClassSlot.PRIMARY, "archer", 0, 10, 25);
        assertEquals(RespecCheck.OK_PAID, result.check());
        assertEquals(0, result.feeLevels());
        assertEquals(0, result.refund());
        assertEquals("archer", d.getPrimaryClass());
        assertEquals(2, d.getAvailablePoints());
        assertEquals(Map.of("common_health", 1), d.getUnlockedNodes());
    }

    @Test
    void changeClass_rejectedChangesNothing() {
        PlayerSkillData d = data("miner", 2);
        d.upgradeNode("miner_haste");
        assertEquals(RespecCheck.SAME_CLASS, SkillTransactions.changeClass(d, REG, ClassSlot.PRIMARY, "miner", 50, 10, 25).check());
        assertEquals(RespecCheck.NOT_ENOUGH_LEVELS, SkillTransactions.changeClass(d, REG, ClassSlot.PRIMARY, "archer", 9, 10, 25).check());
        assertEquals(RespecCheck.INVALID_CLASS, SkillTransactions.changeClass(d, REG, ClassSlot.PRIMARY, "wizard", 50, 10, 25).check());
        assertEquals(RespecCheck.INVALID_CLASS, SkillTransactions.changeClass(d, REG, ClassSlot.PRIMARY, "z".repeat(65), 50, 10, 25).check());
        assertEquals("miner", d.getPrimaryClass());
        assertEquals(2, d.getAvailablePoints());
        assertEquals(Map.of("miner_haste", 1), d.getUnlockedNodes());
    }

    /** Principal minerador com capstone, secundária arqueiro ativa (Segunda Vocação comprada). */
    static PlayerSkillData multiclass() {
        PlayerSkillData d = data("miner", 0);
        d.setSecondaryClass("archer");
        for (int i = 0; i < 5; i++) d.upgradeNode("common_health");
        d.upgradeNode("common_second_wind");
        d.upgradeNode(TalentRules.SECOND_VOCATION);
        for (int i = 0; i < 5; i++) d.upgradeNode("miner_haste");
        d.upgradeNode("miner_vein");
        for (int i = 0; i < 4; i++) d.upgradeNode("archer_aim");
        return d;
    }

    @Test
    void changePrimary_keepsSecondaryNodes_andFreezesIt() {
        TalentRegistry reg = TalentRulesTest.MC_REG;
        PlayerSkillData d = multiclass();
        assertTrue(TalentRules.secondaryActive(d, reg));
        var result = SkillTransactions.changeClass(d, reg, ClassSlot.PRIMARY, "farmer", 50, 10, 50);
        assertEquals(RespecCheck.OK_PAID, result.check());
        assertEquals(10, result.feeLevels());
        assertEquals(3, result.refund()); // 50% de 6 PT do minerador
        assertEquals("farmer", d.getPrimaryClass());
        assertEquals("archer", d.getSecondaryClass());
        assertEquals(3, d.getAvailablePoints());
        assertEquals(Map.of("common_health", 5, "common_second_wind", 1, TalentRules.SECOND_VOCATION, 1, "archer_aim", 4),
                d.getUnlockedNodes());
        assertFalse(TalentRules.secondaryActive(d, reg));
        assertEquals(0, TalentRules.effectiveLevel(d, reg, "archer_aim"));
    }

    @Test
    void changeSecondary_refundsOnlySecondaryTree() {
        TalentRegistry reg = TalentRulesTest.MC_REG;
        PlayerSkillData d = multiclass();
        var result = SkillTransactions.changeClass(d, reg, ClassSlot.SECONDARY, "farmer", 10, 10, 100);
        assertEquals(RespecCheck.OK_PAID, result.check());
        assertEquals(10, result.feeLevels());
        assertEquals(4, result.refund());
        assertEquals("miner", d.getPrimaryClass());
        assertEquals("farmer", d.getSecondaryClass());
        assertEquals(4, d.getAvailablePoints());
        assertEquals(5, d.getNodeLevel("miner_haste"));
        assertEquals(1, d.getNodeLevel("miner_vein"));
        assertEquals(0, d.getNodeLevel("archer_aim"));
    }

    @Test
    void changeSecondary_firstChoiceIsFree() {
        TalentRegistry reg = TalentRulesTest.MC_REG;
        PlayerSkillData d = multiclass();
        d.setSecondaryClass(TalentRules.NO_CLASS);
        var result = SkillTransactions.changeClass(d, reg, ClassSlot.SECONDARY, "archer", 0, 10, 25);
        assertEquals(RespecCheck.OK_FIRST_CHOICE, result.check());
        assertEquals("archer", d.getSecondaryClass());
        assertEquals(4, d.getNodeLevel("archer_aim"));
    }

    @Test
    void changeSecondary_nothingSpentIsPaidButFree() {
        TalentRegistry reg = TalentRulesTest.MC_REG;
        PlayerSkillData d = multiclass();
        d.removeClassNodes("archer");
        int points = d.getAvailablePoints();
        var result = SkillTransactions.changeClass(d, reg, ClassSlot.SECONDARY, "farmer", 0, 10, 25);
        assertEquals(RespecCheck.OK_PAID, result.check());
        assertEquals(0, result.feeLevels());
        assertEquals(0, result.refund());
        assertEquals("farmer", d.getSecondaryClass());
        assertEquals("miner", d.getPrimaryClass());
        assertEquals(points, d.getAvailablePoints());
        assertEquals(5, d.getNodeLevel("miner_haste"));
    }

    @Test
    void secondarySameAsPrimary_refused() {
        PlayerSkillData d = multiclass();
        var result = SkillTransactions.changeClass(d, TalentRulesTest.MC_REG, ClassSlot.SECONDARY, "miner", 50, 10, 25);
        assertEquals(RespecCheck.SAME_CLASS, result.check());
        assertEquals("archer", d.getSecondaryClass());
        assertEquals(4, d.getNodeLevel("archer_aim"));
    }

    @Test
    void maxClassesOne_refusesSecondarySlot() {
        TalentRegistry reg = TalentRulesTest.MC_REG;
        PlayerSkillData d = multiclass();
        d.setMaxClasses(1);
        var result = SkillTransactions.changeClass(d, reg, ClassSlot.SECONDARY, "farmer", 50, 10, 25);
        assertEquals(RespecCheck.SLOT_LOCKED, result.check());
        assertEquals(0, result.feeLevels());
        assertEquals("archer", d.getSecondaryClass());
        assertEquals(0, d.getAvailablePoints());
        assertEquals(4, d.getNodeLevel("archer_aim"));

        PlayerSkillData noVocation = data("miner", 0);
        assertEquals(RespecCheck.SLOT_LOCKED,
                SkillTransactions.changeClass(noVocation, reg, ClassSlot.SECONDARY, "archer", 50, 10, 25).check());
        assertEquals(TalentRules.NO_CLASS, noVocation.getSecondaryClass());
    }

    @Test
    void maxClassesOne_secondaryClassCanBecomePrimary() {
        TalentRegistry reg = TalentRulesTest.MC_REG;
        PlayerSkillData d = multiclass();
        d.setMaxClasses(1);
        var result = SkillTransactions.changeClass(d, reg, ClassSlot.PRIMARY, "archer", 50, 0, 25);
        assertNotEquals(RespecCheck.SAME_CLASS, result.check());
        assertEquals("archer", d.getPrimaryClass());
        assertEquals(TalentRules.NO_CLASS, d.getSecondaryClass());
    }
}
