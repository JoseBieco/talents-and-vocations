package com.seunome.vanillatalents.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuilderDataTest {

    private static final Path CHEAP = Path.of("src/main/resources/data/vanillatalents/tags/item/cheap_blocks.json");
    private static final List<String> VALUABLE = List.of("iron", "gold", "diamond", "emerald", "lapis", "redstone",
            "copper_block", "netherite", "coal", "quartz", "amethyst", "raw_");

    private static List<String> cheapValues() throws IOException {
        JsonArray arr = JsonParser.parseString(Files.readString(CHEAP, StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonArray("values");
        List<String> out = new ArrayList<>();
        arr.forEach(e -> out.add(e.getAsString()));
        return out;
    }

    @Test
    void cheapBlocksHasNoValuableItems() throws IOException {
        for (String v : cheapValues()) {
            for (String bad : VALUABLE) {
                assertFalse(v.contains(bad), "item caro em cheap_blocks: " + v);
            }
        }
    }

    @Test
    void cheapBlocksHasTheProposalItems() throws IOException {
        List<String> values = cheapValues();
        for (String expected : List.of("minecraft:cobblestone", "minecraft:dirt", "minecraft:torch",
                "minecraft:scaffolding", "#minecraft:planks")) {
            assertTrue(values.contains(expected), "falta " + expected);
        }
    }
}
