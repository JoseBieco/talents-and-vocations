package com.seunome.vanillatalents.core;

import java.util.Map;

public final class RespecRules {

    private RespecRules() {}

    /** Soma dos níveis investidos em nós cujo id começa com {@code classId + "_"}. */
    public static int spentClassPoints(Map<String, Integer> levels, String classId) {
        String prefix = classId + "_";
        int total = 0;
        for (Map.Entry<String, Integer> e : levels.entrySet()) {
            if (e.getKey().startsWith(prefix)) total += Math.max(0, e.getValue());
        }
        return total;
    }

    /** Taxa efetiva: trocar sem ter gasto nada na classe atual não custa nada (nada é zerado). */
    public static int feeFor(int spentOnCurrentClass, int feeLevels) {
        return spentOnCurrentClass <= 0 ? 0 : feeLevels;
    }

    public static int refund(int spent, int percent) {
        return Math.floorDiv(spent * percent, 100);
    }

    public static RespecCheck validateChange(String currentClass, String newClass, int playerLevel, int feeLevels) {
        boolean valid = newClass != null && TreeCategory.byId(newClass).map(TreeCategory::isClass).orElse(false);
        if (!valid) return RespecCheck.INVALID_CLASS;
        if (newClass.equals(currentClass)) return RespecCheck.SAME_CLASS;
        if (TalentRules.NO_CLASS.equals(currentClass)) return RespecCheck.OK_FIRST_CHOICE;
        if (playerLevel < feeLevels) return RespecCheck.NOT_ENOUGH_LEVELS;
        return RespecCheck.OK_PAID;
    }
}
