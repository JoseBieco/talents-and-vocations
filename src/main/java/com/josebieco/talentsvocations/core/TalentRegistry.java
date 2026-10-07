package com.josebieco.talentsvocations.core;

import java.util.*;

/** Coleção imutável de nós validados. Nós inválidos são descartados em {@link #build}. */
public final class TalentRegistry {

    private static final Comparator<TalentNode> GRID_ORDER =
            Comparator.comparingInt((TalentNode n) -> n.position().y()).thenComparingInt(n -> n.position().x());

    private final Map<String, TalentNode> nodes;
    private final Map<TreeCategory, List<TalentNode>> byTree;

    private TalentRegistry(Map<String, TalentNode> nodes) {
        this.nodes = Collections.unmodifiableMap(nodes);
        Map<TreeCategory, List<TalentNode>> trees = new EnumMap<>(TreeCategory.class);
        for (TreeCategory tree : TreeCategory.values()) {
            trees.put(tree, nodes.values().stream().filter(n -> n.tree() == tree).sorted(GRID_ORDER).toList());
        }
        this.byTree = Collections.unmodifiableMap(trees);
    }

    public static TalentRegistry empty() {
        return new TalentRegistry(new LinkedHashMap<>());
    }

    /**
     * Valida os nós e devolve um registro só com os válidos. Cada nó descartado gera uma mensagem em
     * {@code errorsOut}; os descartes são reavaliados até estabilizar, então dependentes também caem.
     */
    public static TalentRegistry build(Collection<TalentNode> input, List<String> errorsOut) {
        Map<String, TalentNode> remaining = new LinkedHashMap<>();
        input.stream().sorted(Comparator.comparing(TalentNode::id)).forEach(n -> {
            String error = staticError(n);
            if (error == null && remaining.containsKey(n.id())) error = "id duplicado";
            if (error != null) {
                errorsOut.add(n.id() + ": " + error);
            } else {
                remaining.put(n.id(), n);
            }
        });

        while (true) {
            Map<String, String> discard = prerequisiteErrors(remaining);
            if (discard.isEmpty()) discard = cycleErrors(remaining);
            if (discard.isEmpty()) discard = positionErrors(remaining);
            if (discard.isEmpty()) break;
            discard.forEach((id, error) -> {
                remaining.remove(id);
                errorsOut.add(id + ": " + error);
            });
        }
        return new TalentRegistry(remaining);
    }

    private static String staticError(TalentNode n) {
        if (n.id() == null || n.tree() == null) return "id ou treeCategory ausente";
        if (!n.id().startsWith(n.tree().idPrefix())) {
            return "id deve começar com '" + n.tree().idPrefix() + "'";
        }
        if (n.maxLevel() < 1 || n.maxLevel() > 10) return "maxLevel " + n.maxLevel() + " fora de 1..10";
        if (n.cost() < 1) return "cost " + n.cost() + " < 1";
        for (String c : n.conditions()) {
            if (!TalentNode.CONDITION_PRIMARY_CAPSTONE.equals(c)) return "condição desconhecida '" + c + "'";
        }
        return null;
    }

    private static Map<String, String> prerequisiteErrors(Map<String, TalentNode> remaining) {
        Map<String, String> discard = new LinkedHashMap<>();
        for (TalentNode n : remaining.values()) {
            for (Prerequisite p : n.prerequisites()) {
                TalentNode pre = remaining.get(p.nodeId());
                if (pre == null) {
                    discard.put(n.id(), "pré-requisito '" + p.nodeId() + "' inexistente ou descartado");
                } else if (pre.tree() != n.tree()) {
                    discard.put(n.id(), "pré-requisito '" + p.nodeId() + "' é de outra árvore");
                } else if (p.level() < TalentRules.minRequiredLevel(pre.maxLevel()) || p.level() > pre.maxLevel()) {
                    discard.put(n.id(), "nível exigido de '" + p.nodeId() + "' (" + p.level() + ") fora de "
                            + TalentRules.minRequiredLevel(pre.maxLevel()) + ".." + pre.maxLevel());
                }
                if (discard.containsKey(n.id())) break;
            }
        }
        return discard;
    }

    /** Ordenação topológica (Kahn): o que sobra está num ciclo ou depende de um. */
    private static Map<String, String> cycleErrors(Map<String, TalentNode> remaining) {
        Map<String, Integer> pending = new HashMap<>();
        Map<String, List<String>> dependents = new HashMap<>();
        Deque<String> ready = new ArrayDeque<>();
        for (TalentNode n : remaining.values()) {
            pending.put(n.id(), n.prerequisites().size());
            for (Prerequisite p : n.prerequisites()) {
                dependents.computeIfAbsent(p.nodeId(), k -> new ArrayList<>()).add(n.id());
            }
            if (n.prerequisites().isEmpty()) ready.add(n.id());
        }
        while (!ready.isEmpty()) {
            String id = ready.poll();
            pending.remove(id);
            for (String dep : dependents.getOrDefault(id, List.of())) {
                if (pending.merge(dep, -1, Integer::sum) == 0) ready.add(dep);
            }
        }
        Map<String, String> discard = new LinkedHashMap<>();
        for (String id : remaining.keySet()) {
            if (pending.containsKey(id)) discard.put(id, "ciclo de pré-requisitos");
        }
        return discard;
    }

    private static Map<String, String> positionErrors(Map<String, TalentNode> remaining) {
        Map<String, String> discard = new LinkedHashMap<>();
        Map<String, String> taken = new HashMap<>();
        for (TalentNode n : remaining.values()) {
            String key = n.tree().id() + "@" + n.position().x() + "," + n.position().y();
            String owner = taken.putIfAbsent(key, n.id());
            if (owner != null) discard.put(n.id(), "posição " + n.position() + " já usada por '" + owner + "'");
        }
        return discard;
    }

    public Optional<TalentNode> get(String id) {
        return Optional.ofNullable(nodes.get(id));
    }

    /** Primeiro nó da árvore marcado como capstone. */
    public Optional<TalentNode> capstone(TreeCategory tree) {
        return byTree.get(tree).stream().filter(TalentNode::capstone).findFirst();
    }

    public List<TalentNode> tree(TreeCategory tree) {
        return byTree.get(tree);
    }

    public Collection<TalentNode> all() {
        return nodes.values();
    }

    public int size() {
        return nodes.size();
    }
}
