package com.seunome.vanillatalents.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Cálculos da GUI que não dependem do Minecraft. */
public final class TalentScreenModel {

    public record RespecPreview(int feeLevels, int spent, int refund) {}

    public record GridBounds(int minX, int maxX, int minY, int maxY) {}

    private TalentScreenModel() {}

    public static List<Prerequisite> unmetPrerequisites(SkillView v, TalentRegistry r, TalentNode n) {
        List<Prerequisite> unmet = new ArrayList<>();
        for (Prerequisite p : n.prerequisites()) {
            if (TalentRules.effectiveLevel(v, r, p.nodeId()) < p.level()) unmet.add(p);
        }
        return unmet;
    }

    public static RespecPreview respecPreview(Map<String, Integer> levels, String currentClass, EconomySettings settings) {
        if (TalentRules.NO_CLASS.equals(currentClass)) return new RespecPreview(0, 0, 0);
        int spent = RespecRules.spentClassPoints(levels, currentClass);
        return new RespecPreview(settings.respecFeeLevels(), spent, RespecRules.refund(spent, settings.respecRefundPercent()));
    }

    public static GridBounds gridBounds(List<TalentNode> nodes) {
        if (nodes.isEmpty()) return new GridBounds(0, 0, 0, 0);
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (TalentNode n : nodes) {
            minX = Math.min(minX, n.position().x());
            maxX = Math.max(maxX, n.position().x());
            minY = Math.min(minY, n.position().y());
            maxY = Math.max(maxY, n.position().y());
        }
        return new GridBounds(minX, maxX, minY, maxY);
    }
}
