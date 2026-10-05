package com.seunome.vanillatalents.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.Map;

public class PlayerSkillData implements INBTSerializable<CompoundTag> {

    private String currentClass = "none";
    private int availablePoints = 0;
    private final Map<String, Integer> unlockedNodes = new HashMap<>();

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

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("CurrentClass", currentClass);
        nbt.putInt("AvailablePoints", availablePoints);

        CompoundTag nodesTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : unlockedNodes.entrySet()) {
            nodesTag.putInt(entry.getKey(), entry.getValue());
        }
        nbt.put("UnlockedNodes", nodesTag);

        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        currentClass = nbt.getString("CurrentClass");
        availablePoints = nbt.getInt("AvailablePoints");

        unlockedNodes.clear();
        if (nbt.contains("UnlockedNodes")) {
            CompoundTag nodesTag = nbt.getCompound("UnlockedNodes");
            for (String key : nodesTag.getAllKeys()) {
                unlockedNodes.put(key, nodesTag.getInt(key));
            }
        }
    }
}
