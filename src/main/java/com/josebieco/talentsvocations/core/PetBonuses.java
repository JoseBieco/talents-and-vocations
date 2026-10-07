package com.josebieco.talentsvocations.core;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Bônus permanentes de atributo que o Domador dá aos seus pets (spec §6.3). Calculado com o estado do DONO;
 * PetSync aplica com id fixo {@code talentsvocations:<nodeId>}. Uma entrada por linha que vale para o tipo de pet;
 * {@code amount == 0} significa "remover o modificador".
 */
public final class PetBonuses {

    public enum Attr { MAX_HEALTH, ATTACK_DAMAGE, JUMP_STRENGTH }

    public record Bonus(String nodeId, Attr attribute, AttributeBonuses.Op op, double amount) {}

    private record Row(String nodeId, Attr attribute, AttributeBonuses.Op op, Set<PetKind> kinds) {}

    private static final List<Row> ROWS = List.of(
            new Row("tamer_bond", Attr.MAX_HEALTH, AttributeBonuses.Op.ADD_VALUE, EnumSet.allOf(PetKind.class)),
            new Row("tamer_fangs", Attr.ATTACK_DAMAGE, AttributeBonuses.Op.ADD_VALUE, EnumSet.of(PetKind.WOLF)),
            new Row("tamer_steed", Attr.JUMP_STRENGTH, AttributeBonuses.Op.ADD_MULTIPLIED_BASE, EnumSet.of(PetKind.MOUNT)),
            new Row("tamer_warhorse", Attr.MAX_HEALTH, AttributeBonuses.Op.ADD_VALUE, EnumSet.of(PetKind.MOUNT)),
            new Row("tamer_golem", Attr.MAX_HEALTH, AttributeBonuses.Op.ADD_MULTIPLIED_BASE, EnumSet.of(PetKind.IRON_GOLEM))
    );

    private PetBonuses() {}

    public static List<Bonus> compute(SkillView owner, TalentRegistry r, PetKind kind) {
        List<Bonus> result = new ArrayList<>(ROWS.size());
        for (Row row : ROWS) {
            if (!row.kinds().contains(kind)) continue;
            int level = TalentRules.effectiveLevel(owner, r, row.nodeId());
            double amount = level <= 0 ? 0 : level * r.get(row.nodeId()).orElseThrow().value("per_level");
            result.add(new Bonus(row.nodeId(), row.attribute(), row.op(), amount));
        }
        return result;
    }

    /** Vida atual depois de a máxima mudar: nunca acima da nova máxima (aumentos não curam). */
    public static float clampedHealth(float health, float newMax) {
        return Math.min(health, newMax);
    }
}
