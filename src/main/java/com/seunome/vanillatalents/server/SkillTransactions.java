package com.seunome.vanillatalents.server;

import com.seunome.vanillatalents.capability.PlayerSkillData;
import com.seunome.vanillatalents.core.*;

/** Mutações de estado das ações C2S. Só mudam {@link PlayerSkillData} quando a regra aprova. */
public final class SkillTransactions {

    public static final int MAX_ID_LENGTH = 64;

    public record ClassChange(RespecCheck check, int feeLevels, int refund) {}

    private SkillTransactions() {}

    public static boolean isValidId(String id) {
        return id != null && id.length() <= MAX_ID_LENGTH;
    }

    public static PurchaseResult buy(PlayerSkillData data, TalentRegistry registry, String nodeId) {
        if (!isValidId(nodeId)) return PurchaseResult.UNKNOWN_NODE;
        PurchaseResult result = TalentRules.canPurchase(data, registry, nodeId);
        if (result == PurchaseResult.OK) {
            data.removePoints(registry.get(nodeId).orElseThrow().cost());
            data.upgradeNode(nodeId);
        }
        return result;
    }

    /**
     * Aplica a troca de classe do espaço {@code slot}; a taxa em níveis devolvida em {@code feeLevels} é cobrada por
     * quem chamou. A troca paga remove só os nós da classe trocada e devolve {@code refundPercent}% do que foi gasto
     * nela; trocar a principal mantém a secundária (congelada até o capstone da nova principal).
     */
    public static ClassChange changeClass(PlayerSkillData data, TalentRegistry registry, ClassSlot slot, String newClass,
                                          int playerLevel, int feeLevels, int refundPercent) {
        if (!isValidId(newClass) || slot == null) return new ClassChange(RespecCheck.INVALID_CLASS, 0, 0);
        boolean primary = slot == ClassSlot.PRIMARY;
        // Com maxClasses = 1 a secundária fica oculta; a classe dela pode virar a principal, e o espaço secundário
        // é liberado só se a troca for aprovada. Os nós salvos têm o prefixo da classe e passam a ser da principal.
        boolean takeHiddenSecondary = primary && data.maxClasses() < 2 && newClass.equals(data.getSecondaryClass());
        String current = primary ? data.getPrimaryClass() : data.getSecondaryClass();
        String other = takeHiddenSecondary ? TalentRules.NO_CLASS
                : primary ? data.getSecondaryClass() : data.getPrimaryClass();
        int spent = RespecRules.spentClassPoints(data.getUnlockedNodes(), registry, current);
        int fee = RespecRules.feeFor(spent, feeLevels);
        RespecCheck check = RespecRules.validateChange(slot, current, other, newClass,
                RespecRules.slotUnlocked(slot, data), playerLevel, fee);
        return switch (check) {
            case OK_FIRST_CHOICE -> {
                if (takeHiddenSecondary) data.setSecondaryClass(TalentRules.NO_CLASS);
                setClass(data, slot, newClass);
                yield new ClassChange(check, 0, 0);
            }
            case OK_PAID -> {
                if (takeHiddenSecondary) data.setSecondaryClass(TalentRules.NO_CLASS);
                int refund = RespecRules.refund(spent, refundPercent);
                data.removeClassNodes(current);
                setClass(data, slot, newClass);
                data.addPoints(refund);
                yield new ClassChange(check, fee, refund);
            }
            default -> new ClassChange(check, 0, 0);
        };
    }

    private static void setClass(PlayerSkillData data, ClassSlot slot, String classId) {
        if (slot == ClassSlot.PRIMARY) data.setPrimaryClass(classId);
        else data.setSecondaryClass(classId);
    }
}
