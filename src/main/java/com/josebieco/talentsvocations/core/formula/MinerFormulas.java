package com.josebieco.talentsvocations.core.formula;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Fórmulas da árvore do Minerador. */
public final class MinerFormulas {

    /** Penalidade vanilla de quebrar blocos fora do chão: velocidade × 0,2. */
    static final double AIR_PENALTY = 0.2;

    private MinerFormulas() {}

    /**
     * Multiplicador sobre a velocidade de quebra:
     * {@code (1 + haste·per) × (denso ? 1 + dense·per : 1) × fator de Pés Firmes}. Pés Firmes recupera
     * {@code per} da velocidade perdida por nível quando o jogador não está no chão.
     */
    public static double breakSpeedMultiplier(int hasteLvl, double hastePer, int denseLvl, double densePer, boolean isDense,
                                              int footingLvl, boolean onGround) {
        double haste = 1 + hasteLvl * hastePer;
        double dense = isDense ? 1 + denseLvl * densePer : 1;
        double footing = onGround ? 1 : footingFactor(footingLvl);
        return haste * dense * footing;
    }

    static double footingFactor(int footingLvl) {
        return (AIR_PENALTY + 0.4 * footingLvl) / AIR_PENALTY;
    }

    /** miner_mole: multiplicador de velocidade com pá. */
    public static double shovelMultiplier(int level, double perLevel) {
        return 1 + level * perLevel;
    }

    /** miner_ore_xp: XP do minério com bônus, arredondado. */
    public static int oreXp(int xp, int level, double perLevel) {
        return (int) Math.round(xp * (1 + level * perLevel));
    }

    /** miner_prospector: raio de busca em blocos. */
    public static int prospectorRadius(int level, double radiusPerLevel) {
        return (int) Math.round(level * radiusPerLevel);
    }

    /** miner_lavasense: recarga em ticks conforme o nível (90/60/30 s). */
    public static int lavasenseCooldownTicks(int level, int l1, int l2, int l3) {
        return switch (Math.max(1, Math.min(3, level))) {
            case 1 -> l1;
            case 2 -> l2;
            default -> l3;
        };
    }

    /** miner_vein: quantos blocos extras o Veio quebra. */
    public static int veinLimit(int level, double perLevel) {
        return (int) Math.round(level * perLevel);
    }

    /**
     * miner_vein: busca em largura a partir de {@code start} por blocos conectados que casam com o veio.
     * Devolve até {@code limit} posições, sem incluir {@code start}, na ordem em que foram encontradas.
     */
    public static <P> List<P> veinCollect(P start, BlockGraph<P> graph, int limit) {
        List<P> found = new ArrayList<>();
        if (limit <= 0) return found;
        Set<P> visited = new HashSet<>();
        Deque<P> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            P current = queue.poll();
            for (P next : graph.neighbors(current)) {
                if (!visited.add(next) || !graph.matches(next)) continue;
                found.add(next);
                if (found.size() >= limit) return found;
                queue.add(next);
            }
        }
        return found;
    }

    /** Chance de Toque de Midas duplicar o drop do minério. */
    public static double fortuneChance(int level, double perLevel) {
        return HookFormulas.chance(level, perLevel);
    }

    /** miner_obsidian: multiplicador de velocidade em pedra dura (obsidiana, detritos ancestrais). */
    public static double hardStoneMultiplier(int level, double perLevel) {
        return 1 + level * perLevel;
    }

    /** miner_smelter: lingote correspondente ao material bruto, se houver. */
    public static java.util.Optional<String> smeltTarget(String itemId) {
        return switch (itemId) {
            case "minecraft:raw_iron" -> java.util.Optional.of("minecraft:iron_ingot");
            case "minecraft:raw_gold" -> java.util.Optional.of("minecraft:gold_ingot");
            case "minecraft:raw_copper" -> java.util.Optional.of("minecraft:copper_ingot");
            default -> java.util.Optional.empty();
        };
    }

    /** miner_underdweller: multiplicador de dano de mobs; reduz só abaixo de Y 0. */
    public static double underdwellerMultiplier(int level, double perLevel, double y) {
        return y < 0 ? 1 - level * perLevel : 1;
    }
}
