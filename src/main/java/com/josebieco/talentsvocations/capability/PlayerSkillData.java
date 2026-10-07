package com.josebieco.talentsvocations.capability;

import com.josebieco.talentsvocations.core.SkillView;
import com.josebieco.talentsvocations.core.TalentRules;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.Map;

@AutoRegisterCapability
public class PlayerSkillData implements INBTSerializable<CompoundTag>, SkillView {

    private String primaryClass = TalentRules.NO_CLASS;
    private String secondaryClass = TalentRules.NO_CLASS;
    /** Vem da config a cada acesso; não é salvo em NBT. */
    private int maxClasses = 2;
    private int availablePoints = 0;
    private final Map<String, Integer> unlockedNodes = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();

    public String getPrimaryClass() {
        return primaryClass;
    }

    public void setPrimaryClass(String primaryClass) {
        this.primaryClass = primaryClass;
    }

    public String getSecondaryClass() {
        return secondaryClass;
    }

    public void setSecondaryClass(String secondaryClass) {
        this.secondaryClass = secondaryClass;
    }

    public void setMaxClasses(int maxClasses) {
        this.maxClasses = maxClasses;
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

    /** Remove os nós da árvore {@code classId} (ids com o prefixo {@code classId_}). */
    public void removeClassNodes(String classId) {
        String prefix = classId + "_";
        unlockedNodes.keySet().removeIf(key -> key.startsWith(prefix));
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
        primaryClass = other.primaryClass;
        secondaryClass = other.secondaryClass;
        availablePoints = other.availablePoints;
        unlockedNodes.clear();
        unlockedNodes.putAll(other.unlockedNodes);
        cooldowns.clear();
        cooldowns.putAll(other.cooldowns);
    }

    @Override
    public String primaryClass() {
        return primaryClass;
    }

    @Override
    public String secondaryClass() {
        return secondaryClass;
    }

    @Override
    public int maxClasses() {
        return maxClasses;
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
        nbt.putString("PrimaryClass", primaryClass);
        nbt.putString("SecondaryClass", secondaryClass);
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
        // Save antigo (classe única) só tem CurrentClass: vira a principal.
        primaryClass = nbt.getStringOr("PrimaryClass", nbt.getStringOr("CurrentClass", TalentRules.NO_CLASS));
        secondaryClass = nbt.getStringOr("SecondaryClass", TalentRules.NO_CLASS);
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
