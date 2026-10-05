package com.seunome.vanillatalents.capability;

import com.seunome.vanillatalents.core.SkillView;
import com.seunome.vanillatalents.core.TalentRules;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.Map;

@AutoRegisterCapability
public class PlayerSkillData implements INBTSerializable<CompoundTag>, SkillView {

    private String currentClass = TalentRules.NO_CLASS;
    private int availablePoints = 0;
    private final Map<String, Integer> unlockedNodes = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();

    public String getCurrentClass() {
        return currentClass;
    }

    public void setCurrentClass(String currentClass) {
        this.currentClass = currentClass;
    }

    public int getAvailablePoints() {
        return availablePoints;
    }

    public void addPoints(int points) {
        this.availablePoints += points;
    }

    public void removePoints(int points) {
        this.availablePoints = Math.max(0, this.availablePoints - points);
    }

    public int getNodeLevel(String nodeId) {
        return unlockedNodes.getOrDefault(nodeId, 0);
    }

    public void upgradeNode(String nodeId) {
        unlockedNodes.put(nodeId, getNodeLevel(nodeId) + 1);
    }

    public void resetTree(boolean keepCommonTree) {
        if (!keepCommonTree) {
            unlockedNodes.clear();
        } else {
            // Remove apenas os nós que não começam com "common_"
            unlockedNodes.keySet().removeIf(key -> !key.startsWith("common_"));
        }
    }

    public Map<String, Integer> getUnlockedNodes() {
        return unlockedNodes;
    }

    /** Game time até o qual a recarga {@code key} está ativa; 0 se nunca usada. */
    public long getCooldownUntil(String key) {
        return cooldowns.getOrDefault(key, 0L);
    }

    public void setCooldownUntil(String key, long gameTime) {
        cooldowns.put(key, gameTime);
    }

    public void copyFrom(PlayerSkillData other) {
        currentClass = other.currentClass;
        availablePoints = other.availablePoints;
        unlockedNodes.clear();
        unlockedNodes.putAll(other.unlockedNodes);
        cooldowns.clear();
        cooldowns.putAll(other.cooldowns);
    }

    @Override
    public String currentClass() {
        return currentClass;
    }

    @Override
    public int availablePoints() {
        return availablePoints;
    }

    @Override
    public int rawLevel(String nodeId) {
        return getNodeLevel(nodeId);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registryAccess) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("CurrentClass", currentClass);
        nbt.putInt("AvailablePoints", availablePoints);

        CompoundTag nodesTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : unlockedNodes.entrySet()) {
            nodesTag.putInt(entry.getKey(), entry.getValue());
        }
        nbt.put("UnlockedNodes", nodesTag);

        CompoundTag cooldownTag = new CompoundTag();
        for (Map.Entry<String, Long> entry : cooldowns.entrySet()) {
            cooldownTag.putLong(entry.getKey(), entry.getValue());
        }
        nbt.put("Cooldowns", cooldownTag);

        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider registryAccess, CompoundTag nbt) {
        currentClass = nbt.getStringOr("CurrentClass", TalentRules.NO_CLASS);
        availablePoints = nbt.getIntOr("AvailablePoints", 0);

        unlockedNodes.clear();
        CompoundTag nodesTag = nbt.getCompoundOrEmpty("UnlockedNodes");
        for (String key : nodesTag.keySet()) {
            unlockedNodes.put(key, nodesTag.getIntOr(key, 0));
        }

        cooldowns.clear();
        CompoundTag cooldownTag = nbt.getCompoundOrEmpty("Cooldowns");
        for (String key : cooldownTag.keySet()) {
            cooldowns.put(key, cooldownTag.getLongOr(key, 0L));
        }
    }
}
