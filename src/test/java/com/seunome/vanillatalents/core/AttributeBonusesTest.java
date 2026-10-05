package com.seunome.vanillatalents.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AttributeBonusesTest {

    static TalentNode n(String id, TreeCategory tree, int max, int x, Map<String, Double> values) {
        return new TalentNode(id, tree, "n", "d", "minecraft:stone", max, List.of(), new GridPos(x, 0), values);
    }

    static final TalentRegistry REG = TalentRegistry.build(List.of(
            n("common_health", TreeCategory.COMMON, 5, 0, Map.of("per_level", 2.0)),
            n("common_toughness", TreeCategory.COMMON, 3, 1, Map.of("per_level", 1.0)),
            n("common_aqua", TreeCategory.COMMON, 2, 2, Map.of("per_level", 0.2)),
            n("common_extinguish", TreeCategory.COMMON, 3, 3, Map.of("per_level", -0.15)),
            n("explorer_step", TreeCategory.EXPLORER, 1, 0, Map.of("value", 0.4)),
            n("explorer_safe_height", TreeCategory.EXPLORER, 3, 1, Map.of("per_level", 1.0)),
            n("explorer_featherfoot", TreeCategory.EXPLORER, 1, 2, Map.of("safe_blocks", 12.0)),
            n("explorer_swiftness", TreeCategory.EXPLORER, 5, 3, Map.of("per_level", 0.03)),
            n("warrior_knockback", TreeCategory.WARRIOR, 2, 0, Map.of("per_level", 0.5)),
            n("warrior_axe_speed", TreeCategory.WARRIOR, 3, 1, Map.of("per_level", 0.05))
    ), new ArrayList<>());

    static final AttributeBonuses.Context NEUTRAL = new AttributeBonuses.Context(false, false, false, 0);

    static Map<String, AttributeBonuses.Bonus> byNode(SkillView v, AttributeBonuses.Context ctx) {
        return AttributeBonuses.compute(v, REG, ctx).stream()
                .collect(Collectors.toMap(AttributeBonuses.Bonus::nodeId, Function.identity()));
    }

    @Test
    void resultCoversEveryAttributeNodeEvenWhenInactive() {
        var result = byNode(new TalentRulesTest.FakeView(), NEUTRAL);
        assertEquals(AttributeBonuses.NODE_IDS.size(), result.size());
        assertTrue(result.values().stream().allMatch(b -> b.amount() == 0));
    }

    @Test
    void healthIsPerLevelAddValue() {
        var b = byNode(new TalentRulesTest.FakeView().lvl("common_health", 5), NEUTRAL).get("common_health");
        assertEquals(AttributeBonuses.Attr.MAX_HEALTH, b.attribute());
        assertEquals(AttributeBonuses.Op.ADD_VALUE, b.op());
        assertEquals(10.0, b.amount(), 1e-9);
    }

    @Test
    void extinguishIsNegativeMultipliedBase() {
        var b = byNode(new TalentRulesTest.FakeView().lvl("common_extinguish", 3), NEUTRAL).get("common_extinguish");
        assertEquals(AttributeBonuses.Op.ADD_MULTIPLIED_BASE, b.op());
        assertEquals(-0.45, b.amount(), 1e-9);
    }

    @Test
    void classNodesOnlyForCurrentClass() {
        var v = new TalentRulesTest.FakeView().cls("miner").lvl("explorer_swiftness", 5);
        assertEquals(0, byNode(v, NEUTRAL).get("explorer_swiftness").amount());
        v.cls("explorer");
        var b = byNode(v, NEUTRAL).get("explorer_swiftness");
        assertEquals(AttributeBonuses.Op.ADD_MULTIPLIED_TOTAL, b.op());
        assertEquals(0.15, b.amount(), 1e-9);
    }

    @Test
    void stepUsesFlatValueAndTurnsOffWhileSneaking() {
        var v = new TalentRulesTest.FakeView().cls("explorer").lvl("explorer_step", 1);
        assertEquals(0.4, byNode(v, NEUTRAL).get("explorer_step").amount(), 1e-9);
        assertEquals(0, byNode(v, new AttributeBonuses.Context(true, false, false, 0)).get("explorer_step").amount());
    }

    @Test
    void safeHeightOffWhenFeatherfootOwned() {
        var v = new TalentRulesTest.FakeView().cls("explorer").lvl("explorer_safe_height", 3);
        assertEquals(3.0, byNode(v, NEUTRAL).get("explorer_safe_height").amount(), 1e-9);
        v.lvl("explorer_featherfoot", 1);
        assertEquals(0, byNode(v, NEUTRAL).get("explorer_safe_height").amount());
    }

    @Test
    void aquaSkippedWithAquaAffinityHelmet() {
        var v = new TalentRulesTest.FakeView().lvl("common_aqua", 2);
        assertEquals(0.4, byNode(v, NEUTRAL).get("common_aqua").amount(), 1e-9);
        assertEquals(0, byNode(v, new AttributeBonuses.Context(false, true, false, 0)).get("common_aqua").amount());
    }

    @Test
    void knockbackNeverPushesTotalAboveTwoAndOffWhileSneaking() {
        var v = new TalentRulesTest.FakeView().cls("warrior").lvl("warrior_knockback", 2);
        assertEquals(1.0, byNode(v, NEUTRAL).get("warrior_knockback").amount(), 1e-9);
        assertEquals(1.0, byNode(v, new AttributeBonuses.Context(false, false, false, 1)).get("warrior_knockback").amount(), 1e-9);
        assertEquals(0, byNode(v, new AttributeBonuses.Context(false, false, false, 2)).get("warrior_knockback").amount());
        assertEquals(0, byNode(v, new AttributeBonuses.Context(true, false, false, 0)).get("warrior_knockback").amount());
        assertEquals(0.5, AttributeBonuses.knockbackBonus(2, 0.5, 1.5), 1e-9);
    }

    @Test
    void axeSpeedOnlyWithAxe() {
        var v = new TalentRulesTest.FakeView().cls("warrior").lvl("warrior_axe_speed", 3);
        assertEquals(0, byNode(v, NEUTRAL).get("warrior_axe_speed").amount());
        assertEquals(0.15, byNode(v, new AttributeBonuses.Context(false, false, true, 0)).get("warrior_axe_speed").amount(), 1e-9);
    }
}
