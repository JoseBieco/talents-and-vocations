package com.seunome.vanillatalents.core;

import java.util.Map;

public final class RespecRules {

    private RespecRules() {}

    /**
     * PT investidos na árvore {@code classId}: Σ nível × {@code cost} dos nós com prefixo {@code classId + "_"}.
     * Um id fora do registro conta {@code cost} 1.
     */
    public static int spentClassPoints(Map<String, Integer> levels, TalentRegistry registry, String classId) {
        String prefix = classId + "_";
        int total = 0;
        for (Map.Entry<String, Integer> e : levels.entrySet()) {
            if (!e.getKey().startsWith(prefix)) continue;
            int cost = registry.get(e.getKey()).map(TalentNode::cost).orElse(1);
            total += Math.max(0, e.getValue()) * cost;
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

    /** Espaço secundário liberado: multiclasse ligado e Segunda Vocação comprada. O principal está sempre liberado. */
    public static boolean slotUnlocked(ClassSlot slot, SkillView view) {
        if (slot == ClassSlot.PRIMARY) return true;
        return view.maxClasses() >= 2 && view.rawLevel(TalentRules.SECOND_VOCATION) >= 1;
    }

    /**
     * @param current   classe hoje no espaço {@code slot}
     * @param other     classe do outro espaço
     * @param feeLevels taxa efetiva (já passada por {@link #feeFor})
     */
    public static RespecCheck validateChange(ClassSlot slot, String current, String other, String newClass,
                                             boolean slotUnlocked, int playerLevel, int feeLevels) {
        boolean valid = newClass != null && TreeCategory.byId(newClass).map(TreeCategory::isClass).orElse(false);
        if (!valid) return RespecCheck.INVALID_CLASS;
        if (!slotUnlocked) return RespecCheck.SLOT_LOCKED;
        if (newClass.equals(current) || newClass.equals(other)) return RespecCheck.SAME_CLASS;
        if (TalentRules.NO_CLASS.equals(current)) return RespecCheck.OK_FIRST_CHOICE;
        if (playerLevel < feeLevels) return RespecCheck.NOT_ENOUGH_LEVELS;
        return RespecCheck.OK_PAID;
    }
}
