package com.seunome.vanillatalents.core;

import java.util.Optional;

public enum TreeCategory {
    COMMON("common"),
    MINER("miner"),
    FARMER("farmer"),
    EXPLORER("explorer"),
    WARRIOR("warrior"),
    ARCHER("archer"),
    ANGLER("angler"),
    TAMER("tamer");

    private final String id;

    TreeCategory(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String idPrefix() {
        return id + "_";
    }

    public boolean isClass() {
        return this != COMMON;
    }

    public static Optional<TreeCategory> byId(String id) {
        for (TreeCategory tree : values()) {
            if (tree.id.equals(id)) return Optional.of(tree);
        }
        return Optional.empty();
    }
}
