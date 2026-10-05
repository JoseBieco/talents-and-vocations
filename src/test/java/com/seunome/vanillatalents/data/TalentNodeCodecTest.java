package com.seunome.vanillatalents.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.seunome.vanillatalents.core.GridPos;
import com.seunome.vanillatalents.core.Prerequisite;
import com.seunome.vanillatalents.core.TalentNode;
import com.seunome.vanillatalents.core.TreeCategory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TalentNodeCodecTest {

    static TalentNode parse(String json) {
        return TalentNodeCodec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    @Test
    void parsesFullNode() {
        TalentNode n = parse("""
                {
                  "id": "common_saturation",
                  "treeCategory": "common",
                  "name": "talent.vanillatalents.common_saturation.name",
                  "description": "talent.vanillatalents.common_saturation.desc",
                  "icon": "minecraft:cooked_beef",
                  "maxLevel": 3,
                  "prerequisites": [ { "id": "common_health", "level": 3 } ],
                  "position": { "x": -2, "y": 1 },
                  "values": { "per_level": 0.1 }
                }""");
        assertEquals("common_saturation", n.id());
        assertEquals(TreeCategory.COMMON, n.tree());
        assertEquals("talent.vanillatalents.common_saturation.name", n.nameKey());
        assertEquals("talent.vanillatalents.common_saturation.desc", n.descKey());
        assertEquals("minecraft:cooked_beef", n.icon());
        assertEquals(3, n.maxLevel());
        assertEquals(List.of(new Prerequisite("common_health", 3)), n.prerequisites());
        assertEquals(new GridPos(-2, 1), n.position());
        assertEquals(0.1, n.value("per_level"));
    }

    @Test
    void prerequisitesAndValuesAreOptional() {
        TalentNode n = parse("""
                { "id": "common_health", "treeCategory": "common", "name": "n", "description": "d",
                  "icon": "minecraft:golden_apple", "maxLevel": 5, "position": { "x": 0, "y": 0 } }""");
        assertTrue(n.prerequisites().isEmpty());
        assertTrue(n.values().isEmpty());
    }

    @Test
    void unknownTreeCategoryFails() {
        var result = TalentNodeCodec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                { "id": "wizard_x", "treeCategory": "wizard", "name": "n", "description": "d",
                  "icon": "minecraft:stone", "maxLevel": 1, "position": { "x": 0, "y": 0 } }"""));
        assertTrue(result.isError());
    }

    @Test
    void encodeThenDecodeRoundTrips() {
        TalentNode original = new TalentNode("miner_vein", TreeCategory.MINER, "n", "d", "minecraft:iron_ore", 3,
                List.of(new Prerequisite("miner_fortune", 2), new Prerequisite("miner_lavasense", 2)),
                new GridPos(0, 4), Map.of("per_level", 4.0));
        JsonElement json = TalentNodeCodec.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        assertEquals(original, TalentNodeCodec.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }
}
