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
    void respecPreview_nothingSpentHasNoFee() {
        var preview = TalentScreenModel.respecPreview(Map.of("common_health", 5), "miner", EconomySettings.DEFAULTS);
        assertEquals(0, preview.feeLevels());
        assertEquals(0, preview.spent());
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
    void economySettings_maxConversions() {
        var levels = new EconomySettings(CostMode.LEVELS, 5, 100, 10, 25);
        assertEquals(4, levels.maxConversions(23, 0f));
        var points = new EconomySettings(CostMode.POINTS, 5, 100, 10, 25);
        assertEquals(13, points.maxConversions(30, 0f));
    }

    @Test
    void economySettings_tagRoundTripViaMap() {
        var s = new EconomySettings(CostMode.POINTS, 7, 150, 12, 30);
        assertEquals(s, EconomySettings.fromMap(s.toMap()));
        assertEquals(EconomySettings.DEFAULTS, EconomySettings.fromMap(Map.of()));
    }

    @Test
    void blockReasonKey_mapsEveryResult() {
        String p = "gui.vanillatalents.reason";
        assertNull(TalentScreenModel.blockReasonKey(PurchaseResult.OK));
        assertEquals(p + ".unknown", TalentScreenModel.blockReasonKey(PurchaseResult.UNKNOWN_NODE));
        assertEquals(p + ".no_class", TalentScreenModel.blockReasonKey(PurchaseResult.NO_CLASS_SELECTED));
        assertEquals(p + ".wrong_class", TalentScreenModel.blockReasonKey(PurchaseResult.WRONG_CLASS));
        assertEquals(p + ".maxed", TalentScreenModel.blockReasonKey(PurchaseResult.MAXED));
        assertEquals(p + ".no_points", TalentScreenModel.blockReasonKey(PurchaseResult.NOT_ENOUGH_POINTS));
        assertEquals(p + ".prerequisite", TalentScreenModel.blockReasonKey(PurchaseResult.PREREQUISITE_NOT_MET));
    }

    @Test
    void treeSummary_sumsSpentAndCountsMaxed() {
        var v = new TalentRulesTest.FakeView().lvl("common_health", 5).lvl("common_saturation", 2);
        var s = TalentScreenModel.treeSummary(v, REG, TreeCategory.COMMON);
        assertEquals(7, s.spent());
        assertEquals(1, s.maxedNodes());
        assertEquals(5, s.nodeCount());
        assertEquals(17, s.totalPoints());
    }

    @Test
    void classSummary_rootCapstoneAndTotals() {
        var s = TalentScreenModel.classSummary(REG, TreeCategory.MINER);
        assertEquals("miner_haste", s.rootId());
        assertEquals("miner_darkvision", s.capstoneId());
        assertEquals(2, s.nodeCount());
        assertEquals(6, s.totalPoints());
        var empty = TalentScreenModel.classSummary(REG, TreeCategory.WARRIOR);
        assertNull(empty.rootId());
        assertNull(empty.capstoneId());
        assertEquals(0, empty.nodeCount());
        assertEquals(0, empty.totalPoints());
    }

    @Test
    void wheelScroll_movesTenPixelsPerNotchWithinBounds() {
        assertEquals(-10, TalentScreenModel.wheelScroll(300, 100, 0, -1));
        assertEquals(-30, TalentScreenModel.wheelScroll(300, 100, -10, -2));
        assertEquals(-200, TalentScreenModel.wheelScroll(300, 100, -195, -1), "não passa do fim");
        assertEquals(0, TalentScreenModel.wheelScroll(300, 100, -5, 3), "não passa do início");
        assertEquals(0, TalentScreenModel.wheelScroll(80, 100, 0, -1), "texto que cabe não rola");
    }

    @Test
    void scrollThumb_sizeAndPositionFollowContent() {
        assertNull(TalentScreenModel.scrollThumb(80, 100, 0, 100), "sem barra quando cabe");
        var top = TalentScreenModel.scrollThumb(400, 100, 0, 100);
        assertEquals(0, top.top());
        assertEquals(25, top.height());
        var bottom = TalentScreenModel.scrollThumb(400, 100, -300, 100);
        assertEquals(75, bottom.top());
        assertEquals(8, TalentScreenModel.scrollThumb(10000, 100, 0, 100).height(), "altura mínima");
    }

    @Test
    void clampScroll_limitsToContentRange() {
        assertEquals(0, TalentScreenModel.clampScroll(200, 300, -50));
        assertEquals(-100, TalentScreenModel.clampScroll(400, 300, -150));
        assertEquals(0, TalentScreenModel.clampScroll(400, 300, 20));
        assertEquals(-60, TalentScreenModel.clampScroll(400, 300, -60));
    }

    @Test
    void backgroundTexture_exactPathForEveryTree() {
        assertEquals("textures/block/stone.png", TalentScreenModel.backgroundTexture(TreeCategory.COMMON));
        assertEquals("textures/block/deepslate.png", TalentScreenModel.backgroundTexture(TreeCategory.MINER));
        assertEquals("textures/block/farmland.png", TalentScreenModel.backgroundTexture(TreeCategory.FARMER));
        assertEquals("textures/block/grass_block_side.png", TalentScreenModel.backgroundTexture(TreeCategory.EXPLORER));
        assertEquals("textures/block/polished_blackstone.png", TalentScreenModel.backgroundTexture(TreeCategory.WARRIOR));
        assertEquals("textures/block/oak_planks.png", TalentScreenModel.backgroundTexture(TreeCategory.ARCHER));
        for (TreeCategory t : TreeCategory.values()) assertNotNull(TalentScreenModel.backgroundTexture(t));
    }
}
