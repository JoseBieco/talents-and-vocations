package com.seunome.vanillatalents.core;

import java.util.Optional;

/** Regras de compra e de nível efetivo. Custo: sempre 1 PT por nível. */
public final class TalentRules {

    public static final String NO_CLASS = "none";

    private TalentRules() {}

    public static int minRequiredLevel(int maxLevel) {
        return (maxLevel + 1) / 2;
    }

    /** Nível que vale para efeitos: 0 se o nó não existe ou é de outra classe; limitado ao maxLevel atual. */
    public static int effectiveLevel(SkillView v, TalentRegistry r, String id) {
        Optional<TalentNode> node = r.get(id);
        if (node.isEmpty()) return 0;
        TalentNode n = node.get();
        if (n.tree().isClass() && !n.tree().id().equals(v.currentClass())) return 0;
        return Math.max(0, Math.min(v.rawLevel(id), n.maxLevel()));
    }

    public static PurchaseResult canPurchase(SkillView v, TalentRegistry r, String id) {
        Optional<TalentNode> node = r.get(id);
        if (node.isEmpty()) return PurchaseResult.UNKNOWN_NODE;
        TalentNode n = node.get();
        if (n.tree().isClass()) {
            if (NO_CLASS.equals(v.currentClass())) return PurchaseResult.NO_CLASS_SELECTED;
            if (!n.tree().id().equals(v.currentClass())) return PurchaseResult.WRONG_CLASS;
        }
        if (effectiveLevel(v, r, id) >= n.maxLevel()) return PurchaseResult.MAXED;
        if (v.availablePoints() < 1) return PurchaseResult.NOT_ENOUGH_POINTS;
        if (!prerequisitesMet(v, r, n)) return PurchaseResult.PREREQUISITE_NOT_MET;
        return PurchaseResult.OK;
    }

    public static boolean prerequisitesMet(SkillView v, TalentRegistry r, TalentNode n) {
        for (Prerequisite p : n.prerequisites()) {
            if (effectiveLevel(v, r, p.nodeId()) < p.level()) return false;
        }
        return true;
    }

    public static NodeState nodeState(SkillView v, TalentRegistry r, TalentNode n) {
        int level = effectiveLevel(v, r, n.id());
        if (level >= n.maxLevel()) return NodeState.MAXED;
        if (level > 0) return NodeState.IN_PROGRESS;
        return prerequisitesMet(v, r, n) ? NodeState.AVAILABLE : NodeState.LOCKED;
    }
}
