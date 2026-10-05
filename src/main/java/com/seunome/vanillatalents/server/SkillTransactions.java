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
            data.removePoints(1);
            data.upgradeNode(nodeId);
        }
        return result;
    }

    /** Aplica a troca de classe; a taxa em níveis devolvida em {@code feeLevels} é cobrada por quem chamou. */
    public static ClassChange changeClass(PlayerSkillData data, String newClass, int playerLevel, int feeLevels, int refundPercent) {
        if (!isValidId(newClass)) return new ClassChange(RespecCheck.INVALID_CLASS, 0, 0);
        RespecCheck check = RespecRules.validateChange(data.getCurrentClass(), newClass, playerLevel, feeLevels);
        return switch (check) {
            case OK_FIRST_CHOICE -> {
                data.setCurrentClass(newClass);
                yield new ClassChange(check, 0, 0);
            }
            case OK_PAID -> {
                int spent = RespecRules.spentClassPoints(data.getUnlockedNodes(), data.getCurrentClass());
                int refund = RespecRules.refund(spent, refundPercent);
                data.resetTree(true);
                data.setCurrentClass(newClass);
                data.addPoints(refund);
                yield new ClassChange(check, feeLevels, refund);
            }
            default -> new ClassChange(check, 0, 0);
        };
    }
}
