package com.josebieco.talentsvocations.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.josebieco.talentsvocations.core.TalentRegistryTest.node;
import static com.josebieco.talentsvocations.core.TalentRegistryTest.req;
import static org.junit.jupiter.api.Assertions.*;

public class TalentRulesTest {

    static final class FakeView implements SkillView {
        String primaryClass = "none";
        String secondaryClass = "none";
        int maxClasses = 2;
        int points = 0;
        final Map<String, Integer> levels = new HashMap<>();

        FakeView cls(String c) { primaryClass = c; return this; }
        FakeView sec(String c) { secondaryClass = c; return this; }
        FakeView maxClasses(int m) { maxClasses = m; return this; }
        FakeView points(int p) { points = p; return this; }
        FakeView lvl(String id, int l) { levels.put(id, l); return this; }

        @Override public String primaryClass() { return primaryClass; }
        @Override public String secondaryClass() { return secondaryClass; }
        @Override public int maxClasses() { return maxClasses; }
        @Override public int availablePoints() { return points; }
        @Override public int rawLevel(String nodeId) { return levels.getOrDefault(nodeId, 0); }
    }

    static final TalentRegistry REG = TalentRegistry.build(List.of(
            node("common_health", TreeCategory.COMMON, 5, 0, 0),
            node("common_saturation", TreeCategory.COMMON, 3, -2, 1, req("common_health", 3)),
            node("common_breath", TreeCategory.COMMON, 3, 0, 1, req("common_health", 3)),
            node("common_fireproof", TreeCategory.COMMON, 5, 2, 1, req("common_health", 3)),
            node("common_second_wind", TreeCategory.COMMON, 1, 0, 2,
                    req("common_saturation", 2), req("common_breath", 2), req("common_fireproof", 3)),
            node("miner_haste", TreeCategory.MINER, 5, 0, 0),
            node("miner_darkvision", TreeCategory.MINER, 1, 0, 1, req("miner_haste", 3)),
            node("archer_aim", TreeCategory.ARCHER, 5, 0, 0)
    ), new ArrayList<>());

    static TalentNode special(String id, TreeCategory tree, int max, int x, int y, int cost, boolean capstone,
                              List<String> conditions, Prerequisite... prereqs) {
        return new TalentNode(id, tree, "talent.talentsvocations." + id + ".name", "talent.talentsvocations." + id + ".desc",
                "minecraft:stone", max, List.of(prereqs), new GridPos(x, y), Map.of(), cost, capstone, conditions);
    }

    /** Registro com capstones marcados (Comum, Minerador, Arqueiro) e a Segunda Vocação. */
    public static final TalentRegistry MC_REG = TalentRegistry.build(List.of(
            node("common_health", TreeCategory.COMMON, 5, 0, 0),
            special("common_second_wind", TreeCategory.COMMON, 1, 0, 1, 1, true, List.of(), req("common_health", 3)),
            special(TalentRules.SECOND_VOCATION, TreeCategory.COMMON, 1, 0, 2, 10, false,
                    List.of(TalentNode.CONDITION_PRIMARY_CAPSTONE), req("common_second_wind", 1)),
            node("miner_haste", TreeCategory.MINER, 5, 0, 0),
            special("miner_vein", TreeCategory.MINER, 1, 0, 1, 1, true, List.of(), req("miner_haste", 3)),
            node("miner_extra", TreeCategory.MINER, 2, 1, 2, req("miner_haste", 3)),
            node("archer_aim", TreeCategory.ARCHER, 5, 0, 0),
            special("archer_pierce", TreeCategory.ARCHER, 1, 0, 1, 1, true, List.of(), req("archer_aim", 3))
    ), new ArrayList<>());

    /** Principal minerador com capstone, secundária arqueiro, Segunda Vocação comprada. */
    static FakeView activeMulticlass() {
        return new FakeView().cls("miner").sec("archer").lvl("miner_haste", 5).lvl("miner_vein", 1)
                .lvl("common_health", 5).lvl("common_second_wind", 1).lvl(TalentRules.SECOND_VOCATION, 1)
                .lvl("archer_aim", 4).lvl("archer_pierce", 1);
    }

    @Test
    void registryFixtureIsValid() {
        assertEquals(8, REG.size());
        assertEquals(8, MC_REG.size());
    }

    @Test
    void secondaryNode_countsWhenActive() {
        FakeView v = activeMulticlass().points(1);
        assertTrue(TalentRules.primaryCapstoneOwned(v, MC_REG));
        assertTrue(TalentRules.secondaryActive(v, MC_REG));
        assertEquals(4, TalentRules.effectiveLevel(v, MC_REG, "archer_aim"));
        assertEquals(5, TalentRules.effectiveLevel(v, MC_REG, "miner_haste"));
        assertEquals(PurchaseResult.OK, TalentRules.canPurchase(v, MC_REG, "archer_aim"));
    }

    @Test
    void secondaryNode_frozenWithoutPrimaryCapstone() {
        FakeView v = activeMulticlass().points(5).lvl("miner_vein", 0);
        assertFalse(TalentRules.primaryCapstoneOwned(v, MC_REG));
        assertFalse(TalentRules.secondaryActive(v, MC_REG));
        assertEquals(0, TalentRules.effectiveLevel(v, MC_REG, "archer_aim"));
        assertEquals(PurchaseResult.SECONDARY_LOCKED, TalentRules.canPurchase(v, MC_REG, "archer_aim"));
    }

    @Test
    void secondaryNode_frozenWithoutSecondVocation() {
        FakeView v = activeMulticlass().points(5).lvl(TalentRules.SECOND_VOCATION, 0);
        assertFalse(TalentRules.secondaryActive(v, MC_REG));
        assertEquals(0, TalentRules.effectiveLevel(v, MC_REG, "archer_aim"));
        assertEquals(PurchaseResult.SECONDARY_LOCKED, TalentRules.canPurchase(v, MC_REG, "archer_aim"));
    }

    @Test
    void secondaryCapstone_neverCounts_andCannotBeBought() {
        FakeView v = activeMulticlass().points(5);
        assertEquals(0, TalentRules.effectiveLevel(v, MC_REG, "archer_pierce"));
        v.lvl("archer_pierce", 0);
        assertEquals(PurchaseResult.CAPSTONE_PRIMARY_ONLY, TalentRules.canPurchase(v, MC_REG, "archer_pierce"));
    }

    @Test
    void secondVocation_needsPrimaryCapstone() {
        FakeView v = activeMulticlass().lvl(TalentRules.SECOND_VOCATION, 0).lvl("miner_vein", 0).points(10);
        assertEquals(PurchaseResult.PRIMARY_CAPSTONE_REQUIRED, TalentRules.canPurchase(v, MC_REG, TalentRules.SECOND_VOCATION));
        TalentNode vocation = MC_REG.get(TalentRules.SECOND_VOCATION).orElseThrow();
        assertFalse(TalentRules.conditionsMet(v, MC_REG, vocation));
        assertEquals(NodeState.LOCKED, TalentRules.nodeState(v, MC_REG, vocation));
        v.lvl("miner_vein", 1);
        assertTrue(TalentRules.conditionsMet(v, MC_REG, vocation));
        assertEquals(NodeState.AVAILABLE, TalentRules.nodeState(v, MC_REG, vocation));
        assertEquals(PurchaseResult.OK, TalentRules.canPurchase(v, MC_REG, TalentRules.SECOND_VOCATION));
        v.points(9);
        assertEquals(PurchaseResult.NOT_ENOUGH_POINTS, TalentRules.canPurchase(v, MC_REG, TalentRules.SECOND_VOCATION));
    }

    @Test
    void secondVocation_withoutPrimaryClassNeedsCapstone() {
        FakeView v = new FakeView().points(10).lvl("common_health", 5).lvl("common_second_wind", 1);
        assertEquals(PurchaseResult.PRIMARY_CAPSTONE_REQUIRED, TalentRules.canPurchase(v, MC_REG, TalentRules.SECOND_VOCATION));
    }

    @Test
    void maxClassesOne_disablesSecondary() {
        FakeView v = activeMulticlass().maxClasses(1).points(10);
        assertFalse(TalentRules.secondaryActive(v, MC_REG));
        assertEquals(0, TalentRules.effectiveLevel(v, MC_REG, "archer_aim"));
        assertEquals(5, TalentRules.effectiveLevel(v, MC_REG, "miner_haste"));
        assertEquals(PurchaseResult.SECONDARY_LOCKED, TalentRules.canPurchase(v, MC_REG, "archer_aim"));
        v.lvl(TalentRules.SECOND_VOCATION, 0);
        assertEquals(PurchaseResult.MULTICLASS_DISABLED, TalentRules.canPurchase(v, MC_REG, TalentRules.SECOND_VOCATION));
    }

    @Test
    void secondarySameAsPrimary_isNotActive() {
        FakeView v = activeMulticlass().sec("miner");
        assertFalse(TalentRules.secondaryActive(v, MC_REG));
        assertEquals(1, TalentRules.effectiveLevel(v, MC_REG, "miner_vein"), "a principal continua valendo, capstone inclusive");
    }

    @Test
    void thirdTree_isWrongClassEvenWithSecondary() {
        FakeView v = activeMulticlass().points(5);
        TalentRegistry withFarmer = TalentRegistry.build(List.of(node("farmer_harvest", TreeCategory.FARMER, 3, 0, 0)), new ArrayList<>());
        assertEquals(PurchaseResult.WRONG_CLASS, TalentRules.canPurchase(v, withFarmer, "farmer_harvest"));
    }

    @Test
    void minRequiredLevel_isHalfRoundedUp() {
        assertEquals(1, TalentRules.minRequiredLevel(1));
        assertEquals(1, TalentRules.minRequiredLevel(2));
        assertEquals(2, TalentRules.minRequiredLevel(3));
        assertEquals(2, TalentRules.minRequiredLevel(4));
        assertEquals(3, TalentRules.minRequiredLevel(5));
    }

    @Test
    void canPurchase_okWhenRootAndHasPoint() {
        assertEquals(PurchaseResult.OK, TalentRules.canPurchase(new FakeView().cls("miner").points(1), REG, "miner_haste"));
    }

    @Test
    void canPurchase_unknownNode() {
        assertEquals(PurchaseResult.UNKNOWN_NODE, TalentRules.canPurchase(new FakeView().points(1), REG, "common_nope"));
    }

    @Test
    void canPurchase_noClassForClassNode() {
        assertEquals(PurchaseResult.NO_CLASS_SELECTED, TalentRules.canPurchase(new FakeView().points(1), REG, "miner_haste"));
    }

    @Test
    void canPurchase_wrongClass() {
        assertEquals(PurchaseResult.WRONG_CLASS, TalentRules.canPurchase(new FakeView().cls("miner").points(1), REG, "archer_aim"));
    }

    @Test
    void canPurchase_commonNodeIgnoresClass() {
        assertEquals(PurchaseResult.OK, TalentRules.canPurchase(new FakeView().points(1), REG, "common_health"));
    }

    @Test
    void canPurchase_maxedBeforePoints() {
        assertEquals(PurchaseResult.MAXED,
                TalentRules.canPurchase(new FakeView().points(0).lvl("common_health", 5), REG, "common_health"));
    }

    @Test
    void canPurchase_notEnoughPoints() {
        assertEquals(PurchaseResult.NOT_ENOUGH_POINTS, TalentRules.canPurchase(new FakeView().points(0), REG, "common_health"));
    }

    @Test
    void canPurchase_prereqNeedsHalf() {
        assertEquals(PurchaseResult.PREREQUISITE_NOT_MET,
                TalentRules.canPurchase(new FakeView().points(1).lvl("common_health", 2), REG, "common_saturation"));
        assertEquals(PurchaseResult.OK,
                TalentRules.canPurchase(new FakeView().points(1).lvl("common_health", 3), REG, "common_saturation"));
    }

    @Test
    void canPurchase_capstoneNeedsAllPrereqs() {
        FakeView v = new FakeView().points(1).lvl("common_health", 5)
                .lvl("common_saturation", 2).lvl("common_breath", 2);
        assertEquals(PurchaseResult.PREREQUISITE_NOT_MET, TalentRules.canPurchase(v, REG, "common_second_wind"));
        v.lvl("common_fireproof", 3);
        assertEquals(PurchaseResult.OK, TalentRules.canPurchase(v, REG, "common_second_wind"));
    }

    @Test
    void effectiveLevel_unknownNodeIsZero() {
        assertEquals(0, TalentRules.effectiveLevel(new FakeView().lvl("common_gone", 4), REG, "common_gone"));
    }

    @Test
    void effectiveLevel_clampedToNewMax() {
        TalentRegistry shrunk = TalentRegistry.build(List.of(node("common_health", TreeCategory.COMMON, 3, 0, 0)), new ArrayList<>());
        assertEquals(3, TalentRules.effectiveLevel(new FakeView().lvl("common_health", 5), shrunk, "common_health"));
    }

    @Test
    void effectiveLevel_otherClassIsZero() {
        FakeView v = new FakeView().cls("archer").lvl("miner_haste", 4).lvl("archer_aim", 2);
        assertEquals(0, TalentRules.effectiveLevel(v, REG, "miner_haste"));
        assertEquals(2, TalentRules.effectiveLevel(v, REG, "archer_aim"));
    }

    @Test
    void prerequisitesMet_usesEffectiveLevelOfPrereq() {
        // Pré-requisito com nível bruto acima do novo máximo continua contando como atendido.
        FakeView v = new FakeView().cls("miner").points(1).lvl("miner_haste", 9);
        assertEquals(PurchaseResult.OK, TalentRules.canPurchase(v, REG, "miner_darkvision"));
    }

    @Test
    void nodeState_locked() {
        TalentNode n = REG.get("common_saturation").orElseThrow();
        assertEquals(NodeState.LOCKED, TalentRules.nodeState(new FakeView().lvl("common_health", 2), REG, n));
    }

    @Test
    void nodeState_availableIgnoresPoints() {
        TalentNode n = REG.get("common_saturation").orElseThrow();
        assertEquals(NodeState.AVAILABLE, TalentRules.nodeState(new FakeView().points(0).lvl("common_health", 3), REG, n));
    }

    @Test
    void nodeState_inProgress() {
        TalentNode n = REG.get("common_health").orElseThrow();
        assertEquals(NodeState.IN_PROGRESS, TalentRules.nodeState(new FakeView().lvl("common_health", 2), REG, n));
    }

    @Test
    void nodeState_maxed() {
        TalentNode n = REG.get("common_health").orElseThrow();
        assertEquals(NodeState.MAXED, TalentRules.nodeState(new FakeView().lvl("common_health", 5), REG, n));
    }
}
