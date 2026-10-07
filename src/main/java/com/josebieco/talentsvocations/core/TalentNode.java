package com.josebieco.talentsvocations.core;

import java.util.List;
import java.util.Map;

public record TalentNode(String id, TreeCategory tree, String nameKey, String descKey, String icon, int maxLevel,
                         List<Prerequisite> prerequisites, GridPos position, Map<String, Double> values,
                         int cost, boolean capstone, List<String> conditions) {

    public static final String CONDITION_PRIMARY_CAPSTONE = "primary_capstone";

    public TalentNode {
        prerequisites = List.copyOf(prerequisites);
        values = Map.copyOf(values);
        conditions = List.copyOf(conditions);
    }

    public TalentNode(String id, TreeCategory tree, String nameKey, String descKey, String icon, int maxLevel,
                      List<Prerequisite> prerequisites, GridPos position, Map<String, Double> values) {
        this(id, tree, nameKey, descKey, icon, maxLevel, prerequisites, position, values, 1, false, List.of());
    }

    public double value(String key) {
        Double v = values.get(key);
        if (v == null) {
            throw new IllegalStateException("Talent node '" + id + "' has no value '" + key + "'");
        }
        return v;
    }
}
