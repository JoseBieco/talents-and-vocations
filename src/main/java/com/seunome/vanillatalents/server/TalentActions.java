package com.seunome.vanillatalents.server;

import com.seunome.vanillatalents.Config;
import com.seunome.vanillatalents.capability.PlayerSkillData;
import com.seunome.vanillatalents.capability.SkillAccess;
import com.seunome.vanillatalents.core.CostMode;
import com.seunome.vanillatalents.core.EconomySettings;
import com.seunome.vanillatalents.core.PurchaseResult;
import com.seunome.vanillatalents.core.RespecCheck;
import com.seunome.vanillatalents.core.XpCostRules;
import com.seunome.vanillatalents.data.TalentRegistries;
import com.seunome.vanillatalents.network.ModNetwork;
import com.seunome.vanillatalents.network.S2CSyncPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Lógica de servidor dos pacotes C2S. Roda na main thread; o servidor é a única autoridade.
 * Toda ação termina reenviando o estado ao cliente, aprovada ou não.
 */
public final class TalentActions {

    private TalentActions() {}

    public static boolean convertXp(ServerPlayer player) {
        boolean converted = SkillAccess.get(player).map(data -> {
            CostMode mode = Config.COST_MODE.get();
            int cost = mode == CostMode.LEVELS ? Config.COST_LEVELS.get() : Config.COST_POINTS.get();
            int totalXp = XpCostRules.currentTotalXp(player.experienceLevel, player.experienceProgress);
            if (!XpCostRules.canAfford(mode, player.experienceLevel, totalXp, cost)) return false;
            if (mode == CostMode.LEVELS) {
                player.giveExperienceLevels(-cost);
            } else {
                player.giveExperiencePoints(-cost);
            }
            data.addPoints(1);
            return true;
        }).orElse(false);
        sync(player);
        return converted;
    }

    public static PurchaseResult buyNode(ServerPlayer player, String nodeId) {
        PurchaseResult result = SkillAccess.get(player)
                .map(data -> SkillTransactions.buy(data, TalentRegistries.server(), nodeId))
                .orElse(PurchaseResult.UNKNOWN_NODE);
        sync(player);
        return result;
    }

    public static RespecCheck changeClass(ServerPlayer player, String classId) {
        RespecCheck check = SkillAccess.get(player).map(data -> {
            var change = SkillTransactions.changeClass(data, classId, player.experienceLevel,
                    Config.RESPEC_FEE_LEVELS.get(), Config.RESPEC_REFUND_PERCENT.get());
            if (change.feeLevels() > 0) player.giveExperienceLevels(-change.feeLevels());
            return change.check();
        }).orElse(RespecCheck.INVALID_CLASS);
        sync(player);
        return check;
    }

    public static EconomySettings economy() {
        return new EconomySettings(Config.COST_MODE.get(), Config.COST_LEVELS.get(), Config.COST_POINTS.get(),
                Config.RESPEC_FEE_LEVELS.get(), Config.RESPEC_REFUND_PERCENT.get());
    }

    /** Envia o PlayerSkillData e, em "Settings", os custos vigentes (o cliente não confia na própria config). */
    public static void sync(ServerPlayer player) {
        Optional<PlayerSkillData> data = SkillAccess.get(player);
        data.ifPresent(d -> {
            CompoundTag tag = d.serializeNBT(player.registryAccess());
            CompoundTag settings = new CompoundTag();
            economy().toMap().forEach(settings::putInt);
            tag.put(S2CSyncPlayer.SETTINGS_KEY, settings);
            ModNetwork.sendTo(player, new S2CSyncPlayer(tag));
        });
    }
}
