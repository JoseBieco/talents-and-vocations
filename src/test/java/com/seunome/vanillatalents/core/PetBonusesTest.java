package com.seunome.vanillatalents.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class PetBonusesTest {

    static TalentNode n(String id, int max, int x, Map<String, Double> values) {
        return new TalentNode(id, TreeCategory.TAMER, "n", "d", "minecraft:stone", max, List.of(), new GridPos(x, 0), values);
    }

    static final TalentRegistry REG = TalentRegistry.build(List.of(
            n("tamer_bond", 5, 0, Map.of("per_level", 2.0)),
            n("tamer_fangs", 4, 1, Map.of("per_level", 0.5)),
            n("tamer_steed", 3, 2, Map.of("per_level", 0.1)),
            n("tamer_warhorse", 3, 3, Map.of("per_level", 2.0)),
            n("tamer_golem", 3, 4, Map.of("per_level", 0.2, "heal", 1.0, "interval_ticks", 200.0))
    ), new ArrayList<>());

    static TalentRulesTest.FakeView tamer() {
        return new TalentRulesTest.FakeView().cls("tamer");
    }

    static Map<String, PetBonuses.Bonus> byNode(SkillView v, PetKind kind) {
        return PetBonuses.compute(v, REG, kind).stream()
                .collect(Collectors.toMap(PetBonuses.Bonus::nodeId, Function.identity()));
    }

    @Test
    void bondGivesWolfHealth() {
        var b = byNode(tamer().lvl("tamer_bond", 5), PetKind.WOLF).get("tamer_bond");
        assertEquals(PetBonuses.Attr.MAX_HEALTH, b.attribute());
        assertEquals(AttributeBonuses.Op.ADD_VALUE, b.op());
        assertEquals(10.0, b.amount(), 1e-9);
    }

    @Test
    void fangsOnlyForWolves() {
        var v = tamer().lvl("tamer_fangs", 4);
        var b = byNode(v, PetKind.WOLF).get("tamer_fangs");
        assertEquals(PetBonuses.Attr.ATTACK_DAMAGE, b.attribute());
        assertEquals(AttributeBonuses.Op.ADD_VALUE, b.op());
        assertEquals(2.0, b.amount(), 1e-9);
        assertFalse(byNode(v, PetKind.MOUNT).containsKey("tamer_fangs"));
    }

    @Test
    void steedBoostsMountJump() {
        var b = byNode(tamer().lvl("tamer_steed", 3), PetKind.MOUNT).get("tamer_steed");
        assertEquals(PetBonuses.Attr.JUMP_STRENGTH, b.attribute());
        assertEquals(AttributeBonuses.Op.ADD_MULTIPLIED_BASE, b.op());
        assertEquals(0.3, b.amount(), 1e-9);
    }

    @Test
    void mountGetsWarhorseAndBond() {
        var m = byNode(tamer().lvl("tamer_warhorse", 3).lvl("tamer_bond", 5), PetKind.MOUNT);
        assertEquals(6.0, m.get("tamer_warhorse").amount(), 1e-9);
        assertEquals(PetBonuses.Attr.MAX_HEALTH, m.get("tamer_warhorse").attribute());
        assertEquals(10.0, m.get("tamer_bond").amount(), 1e-9);
    }

    @Test
    void golemGetsMultipliedHealth() {
        var b = byNode(tamer().lvl("tamer_golem", 3), PetKind.IRON_GOLEM).get("tamer_golem");
        assertEquals(PetBonuses.Attr.MAX_HEALTH, b.attribute());
        assertEquals(AttributeBonuses.Op.ADD_MULTIPLIED_BASE, b.op());
        assertEquals(0.6, b.amount(), 1e-9);
        assertFalse(byNode(tamer().lvl("tamer_golem", 3), PetKind.SNOW_GOLEM).containsKey("tamer_golem"));
    }

    @Test
    void otherClassRemovesEverything() {
        var v = new TalentRulesTest.FakeView().cls("miner")
                .lvl("tamer_bond", 5).lvl("tamer_fangs", 4).lvl("tamer_steed", 3)
                .lvl("tamer_warhorse", 3).lvl("tamer_golem", 3);
        for (PetKind kind : PetKind.values()) {
            List<PetBonuses.Bonus> all = PetBonuses.compute(v, REG, kind);
            assertFalse(all.isEmpty());
            assertTrue(all.stream().allMatch(b -> b.amount() == 0), kind.name());
        }
    }

    @Test
    void levelZeroStillListsRowForRemoval() {
        var b = byNode(tamer(), PetKind.WOLF).get("tamer_bond");
        assertNotNull(b);
        assertEquals(0, b.amount());
    }

    @Test
    void clampedHealthNeverAboveMax() {
        assertEquals(30f, PetBonuses.clampedHealth(40f, 30f));
        assertEquals(20f, PetBonuses.clampedHealth(20f, 30f));
    }
}
