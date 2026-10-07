package com.josebieco.talentsvocations.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Nós do tipo "atributo" (Apêndice A, linhas ATR): quanto cada nó soma em qual atributo, dado o estado do
 * jogador e o contexto atual. AttributeSync aplica o resultado com modificadores de id fixo por nó.
 */
public final class AttributeBonuses {

    public enum Attr {
        MAX_HEALTH, ARMOR, OXYGEN_BONUS, SUBMERGED_MINING_SPEED, BURNING_TIME, MOVEMENT_SPEED, STEP_HEIGHT,
        MOVEMENT_EFFICIENCY, SAFE_FALL_DISTANCE, SWIM_SPEED, SWEEPING_DAMAGE_RATIO, ATTACK_KNOCKBACK,
        KNOCKBACK_RESISTANCE, ATTACK_SPEED, LUCK, BLOCK_INTERACTION_RANGE, SNEAKING_SPEED
    }

    public enum Op { ADD_VALUE, ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL }

    /** Estado do jogador que liga/desliga bônus condicionais. */
    public record Context(boolean sneaking, boolean helmetAquaAffinity, boolean holdingAxe, int weaponKnockbackLevel) {}

    public record Bonus(String nodeId, Attr attribute, Op op, double amount) {}

    private record Row(String nodeId, Attr attribute, Op op) {}

    /** Teto de Repulsão total (encantamento + nó), em níveis de Repulsão. */
    public static final double KNOCKBACK_CAP = 2.0;

    private static final List<Row> ROWS = List.of(
            new Row("common_health", Attr.MAX_HEALTH, Op.ADD_VALUE),
            new Row("common_toughness", Attr.ARMOR, Op.ADD_VALUE),
            new Row("common_breath", Attr.OXYGEN_BONUS, Op.ADD_VALUE),
            new Row("common_aqua", Attr.SUBMERGED_MINING_SPEED, Op.ADD_VALUE),
            new Row("common_extinguish", Attr.BURNING_TIME, Op.ADD_MULTIPLIED_BASE),
            new Row("explorer_swiftness", Attr.MOVEMENT_SPEED, Op.ADD_MULTIPLIED_TOTAL),
            new Row("explorer_step", Attr.STEP_HEIGHT, Op.ADD_VALUE),
            new Row("explorer_terrain", Attr.MOVEMENT_EFFICIENCY, Op.ADD_VALUE),
            new Row("explorer_safe_height", Attr.SAFE_FALL_DISTANCE, Op.ADD_VALUE),
            new Row("explorer_swim", Attr.SWIM_SPEED, Op.ADD_MULTIPLIED_BASE),
            new Row("warrior_sweep", Attr.SWEEPING_DAMAGE_RATIO, Op.ADD_VALUE),
            new Row("warrior_knockback", Attr.ATTACK_KNOCKBACK, Op.ADD_VALUE),
            new Row("warrior_steadfast", Attr.KNOCKBACK_RESISTANCE, Op.ADD_VALUE),
            new Row("warrior_axe_speed", Attr.ATTACK_SPEED, Op.ADD_MULTIPLIED_BASE),
            new Row("angler_treasure", Attr.LUCK, Op.ADD_VALUE),
            new Row("builder_reach", Attr.BLOCK_INTERACTION_RANGE, Op.ADD_VALUE),
            new Row("builder_sneak", Attr.SNEAKING_SPEED, Op.ADD_MULTIPLIED_BASE)
    );

    public static final List<String> NODE_IDS = ROWS.stream().map(Row::nodeId).toList();

    private AttributeBonuses() {}

    /** Uma entrada por nó de atributo; {@code amount == 0} significa "remover o modificador". */
    public static List<Bonus> compute(SkillView v, TalentRegistry r, Context ctx) {
        List<Bonus> result = new ArrayList<>(ROWS.size());
        for (Row row : ROWS) {
            int level = TalentRules.effectiveLevel(v, r, row.nodeId());
            double amount = level <= 0 ? 0 : amount(row.nodeId(), level, r.get(row.nodeId()).orElseThrow(), v, r, ctx);
            result.add(new Bonus(row.nodeId(), row.attribute(), row.op(), amount));
        }
        return result;
    }

    private static double amount(String id, int level, TalentNode node, SkillView v, TalentRegistry r, Context ctx) {
        return switch (id) {
            case "common_aqua" -> ctx.helmetAquaAffinity() ? 0 : level * node.value("per_level");
            case "explorer_step" -> ctx.sneaking() ? 0 : node.value("value");
            case "explorer_safe_height" ->
                    TalentRules.effectiveLevel(v, r, "explorer_featherfoot") > 0 ? 0 : level * node.value("per_level");
            case "warrior_knockback" ->
                    ctx.sneaking() ? 0 : knockbackBonus(level, node.value("per_level"), ctx.weaponKnockbackLevel());
            case "warrior_axe_speed" -> ctx.holdingAxe() ? level * node.value("per_level") : 0;
            default -> level * node.value("per_level");
        };
    }

    /**
     * Vida após renascer e reaplicar os bônus. Na morte a vanilla enche a vida antes dos bônus existirem, então
     * renasce-se com a vida máxima nova; na saída do End a vida do jogador antigo é mantida.
     */
    public static float respawnHealth(boolean endConquered, float currentHealth, float maxHealth) {
        return endConquered ? Math.min(currentHealth, maxHealth) : maxHealth;
    }

    /** Bônus do nó limitado para que encantamento + nó não passem de {@link #KNOCKBACK_CAP}. */
    public static double knockbackBonus(int level, double perLevel, double weaponKnockbackLevel) {
        return Math.max(0, Math.min(level * perLevel, KNOCKBACK_CAP - weaponKnockbackLevel));
    }
}
