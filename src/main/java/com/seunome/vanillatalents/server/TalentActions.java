package com.seunome.vanillatalents.server;

import com.seunome.vanillatalents.Config;
import com.seunome.vanillatalents.capability.PlayerSkillData;
import com.seunome.vanillatalents.capability.SkillAccess;
import com.seunome.vanillatalents.core.CostMode;
import com.seunome.vanillatalents.core.EconomySettings;
import com.seunome.vanillatalents.core.PurchaseResult;
import com.seunome.vanillatalents.core.RespecCheck;
import com.seunome.vanillatalents.data.TalentRegistries;
import com.seunome.vanillatalents.effect.AttributeSync;
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

    /**
     * Converte XP em PT: 1, ou todos os possíveis quando {@code all}. A quantidade é sempre recalculada aqui
     * a partir do XP real do jogador. Devolve quantos PT foram comprados.
     */
    public static int convertXp(ServerPlayer player, boolean all) {
        int converted = SkillAccess.get(player).map(data -> {
            EconomySettings economy = economy();
            int affordable = economy.maxConversions(player.experienceLevel, player.experienceProgress);
            int count = all ? affordable : Math.min(1, affordable);
            if (count <= 0) return 0;
            int total = count * economy.cost();
            if (economy.mode() == CostMode.LEVELS) {
                player.giveExperienceLevels(-total);
            } else {
                player.giveExperiencePoints(-total);
            }
            data.addPoints(count);
            return count;
        }).orElse(0);
        sync(player);
        return converted;
    }

    public static PurchaseResult buyNode(ServerPlayer player, String nodeId) {
        PurchaseResult result = SkillAccess.get(player)
                .map(data -> SkillTransactions.buy(data, TalentRegistries.server(), nodeId))
                .orElse(PurchaseResult.UNKNOWN_NODE);
        if (result == PurchaseResult.OK) AttributeSync.apply(player);
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
        if (check == RespecCheck.OK_FIRST_CHOICE || check == RespecCheck.OK_PAID) AttributeSync.apply(player);
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
