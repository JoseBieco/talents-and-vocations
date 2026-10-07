package com.seunome.vanillatalents.core;

import java.util.Optional;

/** Regras de compra e de nível efetivo. Custo: {@link TalentNode#cost()} PT por nível. */
public final class TalentRules {

    public static final String NO_CLASS = "none";

    /** Nó Comum que libera a classe secundária. */
    public static final String SECOND_VOCATION = "common_second_vocation";

    private TalentRules() {}

    public static int minRequiredLevel(int maxLevel) {
        return (maxLevel + 1) / 2;
    }

    /** Nível bruto limitado a 0..maxLevel, sem olhar a classe. */
    private static int clamped(SkillView v, TalentNode n) {
        return Math.max(0, Math.min(v.rawLevel(n.id()), n.maxLevel()));
    }

    /** O capstone da árvore principal está comprado. */
    public static boolean primaryCapstoneOwned(SkillView v, TalentRegistry r) {
        Optional<TreeCategory> primary = TreeCategory.byId(v.primaryClass()).filter(TreeCategory::isClass);
        if (primary.isEmpty()) return false;
        return r.capstone(primary.get()).map(c -> clamped(v, c) >= c.maxLevel()).orElse(false);
    }

    /**
     * A secundária tem efeito e aceita compra: multiclasse ligado, secundária escolhida e diferente da principal,
     * Segunda Vocação comprada e capstone da principal comprado.
     */
    public static boolean secondaryActive(SkillView v, TalentRegistry r) {
        String secondary = v.secondaryClass();
        if (v.maxClasses() < 2) return false;
        if (NO_CLASS.equals(secondary) || secondary.equals(v.primaryClass())) return false;
        if (v.rawLevel(SECOND_VOCATION) < 1) return false;
        return primaryCapstoneOwned(v, r);
    }

    /** Nível que vale para efeitos: 0 se o nó não existe ou é de árvore sem efeito; limitado ao maxLevel atual. */
    public static int effectiveLevel(SkillView v, TalentRegistry r, String id) {
        Optional<TalentNode> node = r.get(id);
        if (node.isEmpty()) return 0;
        TalentNode n = node.get();
        if (n.tree().isClass()) {
            String tree = n.tree().id();
            boolean counts = tree.equals(v.primaryClass())
                    || (tree.equals(v.secondaryClass()) && !n.capstone() && secondaryActive(v, r));
            if (!counts) return 0;
        }
        return clamped(v, n);
    }

    public static PurchaseResult canPurchase(SkillView v, TalentRegistry r, String id) {
        Optional<TalentNode> node = r.get(id);
        if (node.isEmpty()) return PurchaseResult.UNKNOWN_NODE;
        TalentNode n = node.get();
        if (n.tree().isClass()) {
            String tree = n.tree().id();
            if (tree.equals(v.primaryClass())) {
                // segue
            } else if (tree.equals(v.secondaryClass())) {
                if (!secondaryActive(v, r)) return PurchaseResult.SECONDARY_LOCKED;
                if (n.capstone()) return PurchaseResult.CAPSTONE_PRIMARY_ONLY;
            } else {
                if (NO_CLASS.equals(v.primaryClass())) return PurchaseResult.NO_CLASS_SELECTED;
                return PurchaseResult.WRONG_CLASS;
            }
        }
        if (SECOND_VOCATION.equals(id) && v.maxClasses() < 2) return PurchaseResult.MULTICLASS_DISABLED;
        if (effectiveLevel(v, r, id) >= n.maxLevel()) return PurchaseResult.MAXED;
        if (v.availablePoints() < n.cost()) return PurchaseResult.NOT_ENOUGH_POINTS;
        if (!prerequisitesMet(v, r, n)) return PurchaseResult.PREREQUISITE_NOT_MET;
        if (!conditionsMet(v, r, n)) return PurchaseResult.PRIMARY_CAPSTONE_REQUIRED;
        return PurchaseResult.OK;
    }

    public static boolean prerequisitesMet(SkillView v, TalentRegistry r, TalentNode n) {
        for (Prerequisite p : n.prerequisites()) {
            if (effectiveLevel(v, r, p.nodeId()) < p.level()) return false;
        }
        return true;
    }

    /** Condições extras do nó (hoje só {@code primary_capstone}). */
    public static boolean conditionsMet(SkillView v, TalentRegistry r, TalentNode n) {
        for (String c : n.conditions()) {
            if (TalentNode.CONDITION_PRIMARY_CAPSTONE.equals(c) && !primaryCapstoneOwned(v, r)) return false;
        }
        return true;
    }

    public static NodeState nodeState(SkillView v, TalentRegistry r, TalentNode n) {
        int level = effectiveLevel(v, r, n.id());
        if (level >= n.maxLevel()) return NodeState.MAXED;
        if (level > 0) return NodeState.IN_PROGRESS;
        return prerequisitesMet(v, r, n) && conditionsMet(v, r, n) ? NodeState.AVAILABLE : NodeState.LOCKED;
    }
}
