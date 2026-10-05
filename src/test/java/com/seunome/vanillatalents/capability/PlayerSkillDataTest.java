package com.seunome.vanillatalents.capability;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlayerSkillDataTest {

    @Test
    void roundTrip_preservesAllFields() {
        PlayerSkillData data = new PlayerSkillData();
        data.setCurrentClass("miner");
        data.addPoints(7);
        data.upgradeNode("common_health");
        data.upgradeNode("common_health");
        data.upgradeNode("miner_haste");
        data.upgradeNode("miner_fortune");
        data.setCooldownUntil("common_second_wind", 123456789L);

        CompoundTag tag = data.serializeNBT(null);
        PlayerSkillData copy = new PlayerSkillData();
        copy.deserializeNBT(null, tag);

        assertEquals("miner", copy.getCurrentClass());
        assertEquals(7, copy.getAvailablePoints());
        assertEquals(Map.of("common_health", 2, "miner_haste", 1, "miner_fortune", 1), copy.getUnlockedNodes());
        assertEquals(123456789L, copy.getCooldownUntil("common_second_wind"));
        assertEquals(0L, copy.getCooldownUntil("other"));
    }

    @Test
    void deserialize_emptyTagGivesDefaults() {
        PlayerSkillData data = new PlayerSkillData();
        data.setCurrentClass("archer");
        data.addPoints(3);
        data.upgradeNode("archer_aim");
        data.setCooldownUntil("x", 5);

        data.deserializeNBT(null, new CompoundTag());

        assertEquals("none", data.getCurrentClass());
        assertEquals(0, data.getAvailablePoints());
        assertTrue(data.getUnlockedNodes().isEmpty());
        assertEquals(0L, data.getCooldownUntil("x"));
    }

    @Test
    void copyFrom_isIndependentCopy() {
        PlayerSkillData a = new PlayerSkillData();
        a.setCurrentClass("warrior");
        a.addPoints(2);
        a.upgradeNode("warrior_strength");
        a.setCooldownUntil("warrior_bloodlust", 40);

        PlayerSkillData b = new PlayerSkillData();
        b.upgradeNode("common_toughness");
        b.copyFrom(a);
        a.upgradeNode("warrior_strength");

        assertEquals("warrior", b.getCurrentClass());
        assertEquals(2, b.getAvailablePoints());
        assertEquals(Map.of("warrior_strength", 1), b.getUnlockedNodes());
        assertEquals(40L, b.getCooldownUntil("warrior_bloodlust"));
    }

    @Test
    void skillView_exposesState() {
        PlayerSkillData data = new PlayerSkillData();
        data.setCurrentClass("farmer");
        data.addPoints(4);
        data.upgradeNode("farmer_harvest");
        com.seunome.vanillatalents.core.SkillView view = data;
        assertEquals("farmer", view.currentClass());
        assertEquals(4, view.availablePoints());
        assertEquals(1, view.rawLevel("farmer_harvest"));
        assertEquals(0, view.rawLevel("farmer_twins"));
    }
}
