package com.seunome.vanillatalents.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.seunome.vanillatalents.core.TalentRegistryTest.node;
import static com.seunome.vanillatalents.core.TalentRegistryTest.req;
import static org.junit.jupiter.api.Assertions.*;

class TalentScreenModelTest {

    static final TalentRegistry REG = TalentRulesTest.REG;

    @Test
    void unmetPrerequisites_listsOnlyMissing() {
        var v = new TalentRulesTest.FakeView().lvl("common_health", 5).lvl("common_saturation", 2).lvl("common_breath", 1);
        TalentNode capstone = REG.get("common_second_wind").orElseThrow();
        List<Prerequisite> unmet = TalentScreenModel.unmetPrerequisites(v, REG, capstone);
        assertEquals(List.of(new Prerequisite("common_breath", 2), new Prerequisite("common_fireproof", 3)), unmet);
    }

    @Test
    void respecPreview_usesSpentPointsOfCurrentClass() {
        var preview = TalentScreenModel.respecPreview(Map.of("miner_haste", 5, "miner_fortune", 4, "miner_vein", 3,
                "common_health", 5), "miner", new EconomySettings(CostMode.LEVELS, 5, 100, 10, 25));
        assertEquals(10, preview.feeLevels());
        assertEquals(12, preview.spent());
        assertEquals(3, preview.refund());
    }

    @Test
    void respecPreview_firstChoiceIsFree() {
        var preview = TalentScreenModel.respecPreview(Map.of(), TalentRules.NO_CLASS, EconomySettings.DEFAULTS);
        assertEquals(0, preview.feeLevels());
        assertEquals(0, preview.refund());
    }

    @Test
    void gridBounds_coversAllPositions() {
        List<TalentNode> tree = TalentRegistry.build(List.of(
                node("miner_a", TreeCategory.MINER, 1, 0, 0),
                node("miner_b", TreeCategory.MINER, 1, -4, 1, req("miner_a", 1)),
                node("miner_c", TreeCategory.MINER, 1, 3, 4, req("miner_a", 1))
        ), new ArrayList<>()).tree(TreeCategory.MINER);
        assertEquals(new TalentScreenModel.GridBounds(-4, 3, 0, 4), TalentScreenModel.gridBounds(tree));
        assertEquals(new TalentScreenModel.GridBounds(0, 0, 0, 0), TalentScreenModel.gridBounds(List.of()));
    }

    @Test
    void economySettings_costAndAffordability() {
        var levels = new EconomySettings(CostMode.LEVELS, 5, 100, 10, 25);
        assertEquals(5, levels.cost());
        assertFalse(levels.canAffordConversion(4, 0f));
        assertTrue(levels.canAffordConversion(5, 0f));
        var points = new EconomySettings(CostMode.POINTS, 5, 100, 10, 25);
        assertEquals(100, points.cost());
        assertFalse(points.canAffordConversion(7, 0f)); // nível 7 = 91 pontos
        assertTrue(points.canAffordConversion(8, 0f)); // nível 8 = 112 pontos
    }

    @Test
    void economySettings_tagRoundTripViaMap() {
        var s = new EconomySettings(CostMode.POINTS, 7, 150, 12, 30);
        assertEquals(s, EconomySettings.fromMap(s.toMap()));
        assertEquals(EconomySettings.DEFAULTS, EconomySettings.fromMap(Map.of()));
    }
}
