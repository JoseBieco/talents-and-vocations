package com.seunome.vanillatalents.core;

import java.util.List;
import java.util.Map;

public record TalentNode(String id, TreeCategory tree, String nameKey, String descKey, String icon, int maxLevel,
                         List<Prerequisite> prerequisites, GridPos position, Map<String, Double> values) {

    public TalentNode {
        prerequisites = List.copyOf(prerequisites);
        values = Map.copyOf(values);
    }

    public double value(String key) {
        Double v = values.get(key);
        if (v == null) {
            throw new IllegalStateException("Talent node '" + id + "' has no value '" + key + "'");
        }
        return v;
    }
}
