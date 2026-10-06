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
    static final TalentRegistry EMPTY_REG = TalentRegistry.build(List.of(), new ArrayList<>());

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
                "common_health", 5), EMPTY_REG, ClassSlot.PRIMARY, "miner", TalentRules.NO_CLASS, new EconomySettings(CostMode.LEVELS, 5, 100, 10, 25, 2));
        assertEquals(10, preview.feeLevels());
        assertEquals(12, preview.spent());
        assertEquals(3, preview.refund());
    }

    @Test
    void respecPreview_nothingSpentHasNoFee() {
        var preview = TalentScreenModel.respecPreview(Map.of("common_health", 5), EMPTY_REG, ClassSlot.PRIMARY, "miner", TalentRules.NO_CLASS, EconomySettings.DEFAULTS);
        assertEquals(0, preview.feeLevels());
        assertEquals(0, preview.spent());
    }

    @Test
    void respecPreview_firstChoiceIsFree() {
        var preview = TalentScreenModel.respecPreview(Map.of(), EMPTY_REG, ClassSlot.PRIMARY, TalentRules.NO_CLASS, TalentRules.NO_CLASS, EconomySettings.DEFAULTS);
        assertEquals(0, preview.feeLevels());
        assertEquals(0, preview.refund());
        assertFalse(preview.freezesSecondary());
    }

    @Test
    void respecPreview_primaryWithSecondary_flagsFreeze() {
        TalentRegistry reg = TalentRulesTest.MC_REG;
        Map<String, Integer> levels = Map.of("miner_haste", 5, "miner_vein", 1, "archer_aim", 4,
                "common_health", 5, TalentRules.SECOND_VOCATION, 1);
        var primary = TalentScreenModel.respecPreview(levels, reg, ClassSlot.PRIMARY, "miner", "archer", EconomySettings.DEFAULTS);
        assertTrue(primary.freezesSecondary());
        assertEquals(6, primary.spent());
        assertEquals(10, primary.feeLevels());
        assertEquals(1, primary.refund());

        var secondary = TalentScreenModel.respecPreview(levels, reg, ClassSlot.SECONDARY, "miner", "archer", EconomySettings.DEFAULTS);
        assertFalse(secondary.freezesSecondary());
        assertEquals(4, secondary.spent());
        assertEquals(1, secondary.refund());

        var noSecondary = TalentScreenModel.respecPreview(levels, reg, ClassSlot.PRIMARY, "miner", TalentRules.NO_CLASS, EconomySettings.DEFAULTS);
        assertFalse(noSecondary.freezesSecondary());
    }

    @Test
    void respecPreview_secondaryUsesSecondarySlotClass() {
        TalentRegistry reg = TalentRulesTest.MC_REG;
        Map<String, Integer> levels = Map.of("miner_haste", 5, "miner_vein", 1, TalentRules.SECOND_VOCATION, 1);
        // primeira secundária: espaço vazio → grátis, mesmo com PT gastos na principal
        var first = TalentScreenModel.respecPreview(levels, reg, ClassSlot.SECONDARY, "miner", TalentRules.NO_CLASS, EconomySettings.DEFAULTS);
        assertEquals(0, first.feeLevels());
        assertEquals(0, first.spent());
        assertFalse(first.freezesSecondary());
        // secundária escolhida sem nada gasto nela → taxa 0
        var empty = TalentScreenModel.respecPreview(levels, reg, ClassSlot.SECONDARY, "miner", "archer", EconomySettings.DEFAULTS);
        assertEquals(0, empty.feeLevels());
        assertEquals(0, empty.spent());
    }

    @Test
    void secondaryTab_states() {
        TalentRegistry reg = TalentRulesTest.MC_REG;
        assertEquals(TalentScreenModel.SecondaryTab.ACTIVE,
                TalentScreenModel.secondaryTab(TalentRulesTest.activeMulticlass(), reg));
        assertEquals(TalentScreenModel.SecondaryTab.DISABLED,
                TalentScreenModel.secondaryTab(TalentRulesTest.activeMulticlass().maxClasses(1), reg));
        assertEquals(TalentScreenModel.SecondaryTab.LOCKED,
                TalentScreenModel.secondaryTab(TalentRulesTest.activeMulticlass().lvl(TalentRules.SECOND_VOCATION, 0), reg));
        assertEquals(TalentScreenModel.SecondaryTab.CHOOSE,
                TalentScreenModel.secondaryTab(TalentRulesTest.activeMulticlass().sec(TalentRules.NO_CLASS), reg));
        assertEquals(TalentScreenModel.SecondaryTab.FROZEN,
                TalentScreenModel.secondaryTab(TalentRulesTest.activeMulticlass().lvl("miner_vein", 0), reg));
        assertEquals(TalentScreenModel.SecondaryTab.FROZEN,
                TalentScreenModel.secondaryTab(TalentRulesTest.activeMulticlass().cls("farmer"), reg),
                "principal trocada sem capstone");
        assertEquals(TalentScreenModel.SecondaryTab.LOCKED,
                TalentScreenModel.secondaryTab(new TalentRulesTest.FakeView(), reg));
    }

    @Test
    void displayLevel_frozenSecondaryShowsSavedLevels() {
        TalentRegistry reg = TalentRulesTest.MC_REG;
        var frozen = TalentRulesTest.activeMulticlass().lvl("miner_vein", 0);
        assertTrue(TalentScreenModel.frozenSecondary(frozen, reg, TreeCategory.ARCHER));
        assertFalse(TalentScreenModel.frozenSecondary(frozen, reg, TreeCategory.MINER));
        assertEquals(0, TalentRules.effectiveLevel(frozen, reg, "archer_aim"));
        assertEquals(4, TalentScreenModel.displayLevel(frozen, reg, reg.get("archer_aim").orElseThrow()));
        assertEquals(0, TalentScreenModel.displayLevel(frozen, reg, reg.get("archer_pierce").orElseThrow()), "capstone da secundária nunca conta");
        assertEquals(5, TalentScreenModel.displayLevel(frozen, reg, reg.get("miner_haste").orElseThrow()));
        assertEquals(4, TalentScreenModel.treeSummary(frozen, reg, TreeCategory.ARCHER).spent());

        var active = TalentRulesTest.activeMulticlass();
        assertFalse(TalentScreenModel.frozenSecondary(active, reg, TreeCategory.ARCHER));
        var other = TalentRulesTest.activeMulticlass().cls("farmer").lvl("miner_haste", 2);
        assertEquals(0, TalentScreenModel.displayLevel(other, reg, reg.get("miner_haste").orElseThrow()), "árvore sem espaço continua 0");
        assertEquals(0, TalentScreenModel.displayLevel(TalentRulesTest.activeMulticlass().maxClasses(1).lvl("miner_vein", 0), reg,
                reg.get("archer_aim").orElseThrow()), "multiclasse desligado não é congelada");
    }

    @Test
    void primaryCapstoneId_followsPrimaryClass() {
        TalentRegistry reg = TalentRulesTest.MC_REG;
        assertEquals("miner_vein", TalentScreenModel.primaryCapstoneId(new TalentRulesTest.FakeView().cls("miner"), reg));
        assertEquals("archer_pierce", TalentScreenModel.primaryCapstoneId(new TalentRulesTest.FakeView().cls("archer").sec("miner"), reg));
        assertNull(TalentScreenModel.primaryCapstoneId(new TalentRulesTest.FakeView(), reg));
        assertNull(TalentScreenModel.primaryCapstoneId(new TalentRulesTest.FakeView().cls("farmer"), reg), "árvore sem capstone");
    }

    @Test
    void classChoice_bySlot() {
        assertEquals(TalentScreenModel.ClassChoice.CURRENT, TalentScreenModel.classChoice(ClassSlot.PRIMARY, "miner", "miner", "archer"));
        assertEquals(TalentScreenModel.ClassChoice.OTHER_SLOT, TalentScreenModel.classChoice(ClassSlot.PRIMARY, "archer", "miner", "archer"));
        assertEquals(TalentScreenModel.ClassChoice.AVAILABLE, TalentScreenModel.classChoice(ClassSlot.PRIMARY, "farmer", "miner", "archer"));
        assertEquals(TalentScreenModel.ClassChoice.CURRENT, TalentScreenModel.classChoice(ClassSlot.SECONDARY, "archer", "miner", "archer"));
        assertEquals(TalentScreenModel.ClassChoice.OTHER_SLOT, TalentScreenModel.classChoice(ClassSlot.SECONDARY, "miner", "miner", "archer"));
        assertEquals(TalentScreenModel.ClassChoice.AVAILABLE, TalentScreenModel.classChoice(ClassSlot.SECONDARY, "farmer", "miner", TalentRules.NO_CLASS));
        assertEquals("miner", TalentScreenModel.classInSlot(ClassSlot.PRIMARY, "miner", "archer"));
        assertEquals("archer", TalentScreenModel.classInSlot(ClassSlot.SECONDARY, "miner", "archer"));
    }

    @Test
    void respecPreview_spentWeightsByCost() {
        TalentRegistry reg = TalentRulesTest.MC_REG;
        // a Segunda Vocação (cost 10) é Comum e não entra; miner_vein cost 1
        var preview = TalentScreenModel.respecPreview(Map.of("miner_haste", 2, TalentRules.SECOND_VOCATION, 1), reg,
                ClassSlot.PRIMARY, "miner", TalentRules.NO_CLASS, EconomySettings.DEFAULTS);
        assertEquals(2, preview.spent());
    }

    @Test
    void economySettings_maxClassesDefaultsToTwo() {
        assertEquals(2, EconomySettings.DEFAULTS.maxClasses());
        assertEquals(2, EconomySettings.fromMap(Map.of()).maxClasses());
        assertEquals(1, EconomySettings.fromMap(Map.of("maxClasses", 1)).maxClasses());
        assertEquals(1, new EconomySettings(CostMode.LEVELS, 5, 100, 10, 25, 1).toMap().get("maxClasses"));
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
        var levels = new EconomySettings(CostMode.LEVELS, 5, 100, 10, 25, 2);
        assertEquals(5, levels.cost());
        assertFalse(levels.canAffordConversion(4, 0f));
        assertTrue(levels.canAffordConversion(5, 0f));
        var points = new EconomySettings(CostMode.POINTS, 5, 100, 10, 25, 2);
        assertEquals(100, points.cost());
        assertFalse(points.canAffordConversion(7, 0f)); // nível 7 = 91 pontos
        assertTrue(points.canAffordConversion(8, 0f)); // nível 8 = 112 pontos
    }

    @Test
    void economySettings_maxConversions() {
        var levels = new EconomySettings(CostMode.LEVELS, 5, 100, 10, 25, 2);
        assertEquals(4, levels.maxConversions(23, 0f));
        var points = new EconomySettings(CostMode.POINTS, 5, 100, 10, 25, 2);
        assertEquals(13, points.maxConversions(30, 0f));
    }

    @Test
    void economySettings_tagRoundTripViaMap() {
        var s = new EconomySettings(CostMode.POINTS, 7, 150, 12, 30, 1);
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
        assertEquals(p + ".secondary_locked", TalentScreenModel.blockReasonKey(PurchaseResult.SECONDARY_LOCKED));
        assertEquals(p + ".capstone_primary_only", TalentScreenModel.blockReasonKey(PurchaseResult.CAPSTONE_PRIMARY_ONLY));
        assertEquals(p + ".primary_capstone", TalentScreenModel.blockReasonKey(PurchaseResult.PRIMARY_CAPSTONE_REQUIRED));
        assertEquals(p + ".multiclass_disabled", TalentScreenModel.blockReasonKey(PurchaseResult.MULTICLASS_DISABLED));
        for (PurchaseResult r : PurchaseResult.values()) {
            if (r != PurchaseResult.OK) assertNotNull(TalentScreenModel.blockReasonKey(r), r.name());
        }
    }

    @Test
    void treeSummary_weightsByCost() {
        TalentRegistry reg = TalentRulesTest.MC_REG;
        var v = new TalentRulesTest.FakeView().cls("miner").lvl("common_health", 5).lvl("common_second_wind", 1)
                .lvl(TalentRules.SECOND_VOCATION, 1);
        var s = TalentScreenModel.treeSummary(v, reg, TreeCategory.COMMON);
        assertEquals(5 + 1 + 10, s.spent());
        assertEquals(3, s.maxedNodes());
        assertEquals(3, s.nodeCount());
        assertEquals(5 + 1 + 10, s.totalPoints());
    }

    @Test
    void classSummary_usesMarkedCapstoneEvenWhenNotLast() {
        TalentRegistry reg = TalentRulesTest.MC_REG;
        var s = TalentScreenModel.classSummary(reg, TreeCategory.MINER);
        assertEquals("miner_haste", s.rootId());
        assertEquals("miner_vein", s.capstoneId());
        assertEquals("miner_extra", reg.tree(TreeCategory.MINER).get(2).id(), "o capstone não é o último nó");
        assertEquals(3, s.nodeCount());
        assertEquals(5 + 1 + 2, s.totalPoints());
        var common = TalentScreenModel.classSummary(reg, TreeCategory.COMMON);
        assertEquals("common_second_wind", common.capstoneId());
        assertEquals(5 + 1 + 10, common.totalPoints());
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
    void listScroll_scrollsOnlyWhenRowsOverflowTheView() {
        int scrolled = TalentScreenModel.listScroll(6, 28, 140, 0, -1);
        assertTrue(scrolled < 0, "seis linhas não cabem em 140 px");
        assertEquals(140 - 6 * 28, TalentScreenModel.listScroll(6, 28, 140, scrolled, -50), "não passa do fim");
        assertEquals(0, TalentScreenModel.listScroll(6, 28, 140, scrolled, 50), "não passa do início");
        assertEquals(0, TalentScreenModel.listScroll(4, 28, 140, 0, -1), "lista que cabe não rola");
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
        assertEquals("textures/block/sand.png", TalentScreenModel.backgroundTexture(TreeCategory.ANGLER));
        for (TreeCategory t : TreeCategory.values()) assertNotNull(TalentScreenModel.backgroundTexture(t));
    }
}
