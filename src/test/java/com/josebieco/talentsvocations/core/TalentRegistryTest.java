package com.josebieco.talentsvocations.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TalentRegistryTest {

    public static TalentNode node(String id, TreeCategory tree, int max, int x, int y, Prerequisite... prereqs) {
        return new TalentNode(id, tree, "talent.talentsvocations." + id + ".name", "talent.talentsvocations." + id + ".desc",
                "minecraft:stone", max, List.of(prereqs), new GridPos(x, y), Map.of());
    }

    public static Prerequisite req(String id, int level) {
        return new Prerequisite(id, level);
    }

    @Test
    void build_acceptsValidMvpTree() {
        List<String> errors = new ArrayList<>();
        TalentRegistry r = TalentRegistry.build(List.of(
                node("common_health", TreeCategory.COMMON, 5, 0, 0),
                node("common_saturation", TreeCategory.COMMON, 3, 0, 1, req("common_health", 3))
        ), errors);
        assertEquals(2, r.size());
        assertTrue(errors.isEmpty(), errors.toString());
    }

    @Test
    void build_rejectsWrongPrefix() {
        List<String> errors = new ArrayList<>();
        TalentRegistry r = TalentRegistry.build(List.of(node("miner_x", TreeCategory.COMMON, 3, 0, 0)), errors);
        assertEquals(0, r.size());
        assertTrue(errors.stream().anyMatch(e -> e.contains("miner_x")), errors.toString());
    }

    @Test
    void build_rejectsMaxLevelOutOfRange() {
        List<String> errors = new ArrayList<>();
        TalentRegistry r = TalentRegistry.build(List.of(
                node("common_zero", TreeCategory.COMMON, 0, 0, 0),
                node("common_eleven", TreeCategory.COMMON, 11, 1, 0)
        ), errors);
        assertEquals(0, r.size());
        assertEquals(2, errors.size(), errors.toString());
    }

    @Test
    void build_rejectsPrereqBelowHalf() {
        List<String> errors = new ArrayList<>();
        TalentRegistry r = TalentRegistry.build(List.of(
                node("common_health", TreeCategory.COMMON, 5, 0, 0),
                node("common_saturation", TreeCategory.COMMON, 3, 0, 1, req("common_health", 2))
        ), errors);
        assertTrue(r.get("common_saturation").isEmpty());
        assertTrue(r.get("common_health").isPresent());
        assertTrue(errors.stream().anyMatch(e -> e.contains("common_saturation")), errors.toString());
    }

    @Test
    void build_rejectsPrereqAboveMax() {
        List<String> errors = new ArrayList<>();
        TalentRegistry r = TalentRegistry.build(List.of(
                node("common_health", TreeCategory.COMMON, 5, 0, 0),
                node("common_saturation", TreeCategory.COMMON, 3, 0, 1, req("common_health", 6))
        ), errors);
        assertTrue(r.get("common_saturation").isEmpty());
        assertEquals(1, r.size());
    }

    @Test
    void build_rejectsUnknownAndCrossTreePrereq() {
        List<String> errors = new ArrayList<>();
        TalentRegistry r = TalentRegistry.build(List.of(
                node("miner_haste", TreeCategory.MINER, 5, 0, 0),
                node("common_a", TreeCategory.COMMON, 3, 0, 0, req("common_missing", 1)),
                node("common_b", TreeCategory.COMMON, 3, 1, 0, req("miner_haste", 3))
        ), errors);
        assertTrue(r.get("common_a").isEmpty());
        assertTrue(r.get("common_b").isEmpty());
        assertTrue(r.get("miner_haste").isPresent());
        assertEquals(2, errors.size(), errors.toString());
    }

    @Test
    void build_cascadesDiscard() {
        List<String> errors = new ArrayList<>();
        TalentRegistry r = TalentRegistry.build(List.of(
                node("common_a", TreeCategory.COMMON, 0, 0, 0),
                node("common_b", TreeCategory.COMMON, 2, 0, 1, req("common_a", 1)),
                node("common_c", TreeCategory.COMMON, 2, 0, 2, req("common_b", 1))
        ), errors);
        assertEquals(0, r.size());
        assertEquals(3, errors.size(), errors.toString());
    }

    @Test
    void build_rejectsCycle() {
        List<String> errors = new ArrayList<>();
        TalentRegistry r = TalentRegistry.build(List.of(
                node("common_a", TreeCategory.COMMON, 2, 0, 0, req("common_b", 1)),
                node("common_b", TreeCategory.COMMON, 2, 0, 1, req("common_a", 1))
        ), errors);
        assertEquals(0, r.size());
        assertFalse(errors.isEmpty());
    }

    @Test
    void build_rejectsDuplicatePosition() {
        List<String> errors = new ArrayList<>();
        TalentRegistry r = TalentRegistry.build(List.of(
                node("common_a", TreeCategory.COMMON, 2, 0, 0),
                node("common_b", TreeCategory.COMMON, 2, 0, 0),
                node("miner_c", TreeCategory.MINER, 2, 0, 0)
        ), errors);
        assertEquals(2, r.size());
        assertTrue(r.get("miner_c").isPresent());
        assertEquals(1, errors.size(), errors.toString());
    }

    @Test
    void tree_sortedByYThenX() {
        TalentRegistry r = TalentRegistry.build(List.of(
                node("common_c", TreeCategory.COMMON, 1, 1, 1),
                node("common_a", TreeCategory.COMMON, 1, 0, 0),
                node("common_b", TreeCategory.COMMON, 1, -1, 1)
        ), new ArrayList<>());
        assertEquals(List.of("common_a", "common_b", "common_c"),
                r.tree(TreeCategory.COMMON).stream().map(TalentNode::id).toList());
    }

    @Test
    void value_missingKeyThrowsWithNodeId() {
        TalentNode n = new TalentNode("common_health", TreeCategory.COMMON, "n", "d", "i", 5, List.of(),
                new GridPos(0, 0), Map.of("per_level", 2.0));
        assertEquals(2.0, n.value("per_level"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> n.value("missing"));
        assertTrue(ex.getMessage().contains("common_health"));
        assertTrue(ex.getMessage().contains("missing"));
    }

    @Test
    void treeCategory_byIdAndPrefix() {
        assertEquals(TreeCategory.MINER, TreeCategory.byId("miner").orElseThrow());
        assertTrue(TreeCategory.byId("wizard").isEmpty());
        assertEquals("archer_", TreeCategory.ARCHER.idPrefix());
        assertFalse(TreeCategory.COMMON.isClass());
        assertTrue(TreeCategory.WARRIOR.isClass());
    }

    private static TalentNode withExtras(String id, int cost, boolean capstone, List<String> conditions) {
        return new TalentNode(id, TreeCategory.MINER, "n", "d", "i", 1, List.of(), new GridPos(0, 0), Map.of(),
                cost, capstone, conditions);
    }

    @Test
    void build_rejectsCostBelowOne() {
        List<String> errors = new ArrayList<>();
        TalentRegistry r = TalentRegistry.build(List.of(withExtras("miner_x", 0, false, List.of())), errors);
        assertEquals(0, r.size());
        assertTrue(errors.stream().anyMatch(e -> e.contains("miner_x") && e.contains("cost")), errors.toString());
    }

    @Test
    void build_rejectsUnknownCondition() {
        List<String> errors = new ArrayList<>();
        TalentRegistry r = TalentRegistry.build(List.of(withExtras("miner_x", 1, false, List.of("foo"))), errors);
        assertEquals(0, r.size());
        assertTrue(errors.stream().anyMatch(e -> e.contains("foo")), errors.toString());
    }

    @Test
    void capstone_returnsMarkedNode() {
        TalentRegistry r = TalentRegistry.build(List.of(
                node("miner_a", TreeCategory.MINER, 1, 0, 0),
                new TalentNode("miner_z", TreeCategory.MINER, "n", "d", "i", 1, List.of(), new GridPos(0, 1), Map.of(),
                        1, true, List.of())), new ArrayList<>());
        assertEquals("miner_z", r.capstone(TreeCategory.MINER).orElseThrow().id());
        assertTrue(r.capstone(TreeCategory.ARCHER).isEmpty());
    }
}
