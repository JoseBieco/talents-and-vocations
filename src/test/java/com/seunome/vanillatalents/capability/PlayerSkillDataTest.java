package com.seunome.vanillatalents.capability;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlayerSkillDataTest {

    @Test
    void roundTrip_preservesAllFields() {
        PlayerSkillData data = new PlayerSkillData();
        data.setPrimaryClass("miner");
        data.setSecondaryClass("archer");
        data.addPoints(7);
        data.upgradeNode("common_health");
        data.upgradeNode("common_health");
        data.upgradeNode("miner_haste");
        data.upgradeNode("miner_fortune");
        data.setCooldownUntil("common_second_wind", 123456789L);

        CompoundTag tag = data.serializeNBT(null);
        PlayerSkillData copy = new PlayerSkillData();
        copy.deserializeNBT(null, tag);

        assertEquals("miner", copy.getPrimaryClass());
        assertEquals("archer", copy.getSecondaryClass());
        assertEquals("miner", tag.getStringOr("PrimaryClass", ""));
        assertEquals("archer", tag.getStringOr("SecondaryClass", ""));
        assertFalse(tag.contains("CurrentClass"));
        assertEquals(7, copy.getAvailablePoints());
        assertEquals(Map.of("common_health", 2, "miner_haste", 1, "miner_fortune", 1), copy.getUnlockedNodes());
        assertEquals(123456789L, copy.getCooldownUntil("common_second_wind"));
        assertEquals(0L, copy.getCooldownUntil("other"));
    }

    @Test
    void deserialize_emptyTagGivesDefaults() {
        PlayerSkillData data = new PlayerSkillData();
        data.setPrimaryClass("archer");
        data.setSecondaryClass("miner");
        data.addPoints(3);
        data.upgradeNode("archer_aim");
        data.setCooldownUntil("x", 5);

        data.deserializeNBT(null, new CompoundTag());

        assertEquals("none", data.getPrimaryClass());
        assertEquals("none", data.getSecondaryClass());
        assertEquals(0, data.getAvailablePoints());
        assertTrue(data.getUnlockedNodes().isEmpty());
        assertEquals(0L, data.getCooldownUntil("x"));
    }

    @Test
    void copyFrom_isIndependentCopy() {
        PlayerSkillData a = new PlayerSkillData();
        a.setPrimaryClass("warrior");
        a.setSecondaryClass("farmer");
        a.addPoints(2);
        a.upgradeNode("warrior_strength");
        a.setCooldownUntil("warrior_bloodlust", 40);

        PlayerSkillData b = new PlayerSkillData();
        b.upgradeNode("common_toughness");
        b.copyFrom(a);
        a.upgradeNode("warrior_strength");

        assertEquals("warrior", b.getPrimaryClass());
        assertEquals("farmer", b.getSecondaryClass());
        assertEquals(2, b.getAvailablePoints());
        assertEquals(Map.of("warrior_strength", 1), b.getUnlockedNodes());
        assertEquals(40L, b.getCooldownUntil("warrior_bloodlust"));
    }

    @Test
    void skillView_exposesState() {
        PlayerSkillData data = new PlayerSkillData();
        data.setPrimaryClass("farmer");
        data.setSecondaryClass("miner");
        data.addPoints(4);
        data.upgradeNode("farmer_harvest");
        com.seunome.vanillatalents.core.SkillView view = data;
        assertEquals("farmer", view.primaryClass());
        assertEquals("miner", view.secondaryClass());
        assertEquals(2, view.maxClasses());
        data.setMaxClasses(1);
        assertEquals(1, view.maxClasses());
        assertEquals(4, view.availablePoints());
        assertEquals(1, view.rawLevel("farmer_harvest"));
        assertEquals(0, view.rawLevel("farmer_twins"));
    }

    @Test
    void legacyCurrentClass_becomesPrimary() {
        CompoundTag tag = new CompoundTag();
        tag.putString("CurrentClass", "miner");
        tag.putInt("AvailablePoints", 6);
        CompoundTag nodes = new CompoundTag();
        nodes.putInt("miner_haste", 3);
        nodes.putInt("common_health", 2);
        tag.put("UnlockedNodes", nodes);

        PlayerSkillData data = new PlayerSkillData();
        data.deserializeNBT(null, tag);

        assertEquals("miner", data.getPrimaryClass());
        assertEquals("none", data.getSecondaryClass());
        assertEquals(6, data.getAvailablePoints());
        assertEquals(Map.of("miner_haste", 3, "common_health", 2), data.getUnlockedNodes());
    }

    @Test
    void primaryClassWinsOverLegacyCurrentClass() {
        CompoundTag tag = new CompoundTag();
        tag.putString("CurrentClass", "miner");
        tag.putString("PrimaryClass", "archer");
        PlayerSkillData data = new PlayerSkillData();
        data.deserializeNBT(null, tag);
        assertEquals("archer", data.getPrimaryClass());
    }

    @Test
    void maxClasses_isNotSaved() {
        PlayerSkillData data = new PlayerSkillData();
        data.setMaxClasses(1);
        CompoundTag tag = data.serializeNBT(null);
        PlayerSkillData copy = new PlayerSkillData();
        copy.deserializeNBT(null, tag);
        assertEquals(2, copy.maxClasses());
    }

    @Test
    void removeClassNodes_removesOnlyThatTree() {
        PlayerSkillData data = new PlayerSkillData();
        data.upgradeNode("common_health");
        data.upgradeNode("miner_haste");
        data.upgradeNode("archer_aim");
        data.removeClassNodes("miner");
        assertEquals(Map.of("common_health", 1, "archer_aim", 1), data.getUnlockedNodes());
    }
}
