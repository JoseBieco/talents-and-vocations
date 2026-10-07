package com.seunome.vanillatalents.core.formula;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class OreHighlightTest {

    @Test
    void colorFor_usesTheSpecificOreTag() {
        assertEquals(OreHighlight.DIAMOND, OreHighlight.colorFor(List.of("ores", "ores/diamond", "mineable/pickaxe")));
        assertEquals(OreHighlight.GOLD, OreHighlight.colorFor(List.of("ores/gold")));
        assertEquals(OreHighlight.NETHERITE_SCRAP, OreHighlight.colorFor(List.of("ores/netherite_scrap")));
    }

    @Test
    void colorFor_unknownOreIsWhite() {
        assertEquals(OreHighlight.DEFAULT, OreHighlight.colorFor(List.of("ores", "ores/tin")));
        assertEquals(OreHighlight.DEFAULT, OreHighlight.colorFor(List.of()));
    }

    @Test
    void everyVanillaOreHasADistinctColor() {
        List<Integer> colors = List.of(OreHighlight.COAL, OreHighlight.IRON, OreHighlight.COPPER, OreHighlight.GOLD,
                OreHighlight.REDSTONE, OreHighlight.LAPIS, OreHighlight.DIAMOND, OreHighlight.EMERALD,
                OreHighlight.QUARTZ, OreHighlight.NETHERITE_SCRAP);
        assertEquals(colors.size(), colors.stream().distinct().count());
    }

    @Test
    void insetTranslation_centersTheScaledBlock() {
        assertEquals(0.01f, OreHighlight.insetTranslation(0.98f), 1e-6);
        assertEquals(0f, OreHighlight.insetTranslation(1f), 1e-6);
    }

    @Test
    void capped_keepsAtMostMax() {
        List<Integer> many = IntStream.range(0, 300).boxed().toList();
        assertEquals(256, OreHighlight.capped(many, 256).size());
        assertEquals(List.of(1, 2), OreHighlight.capped(List.of(1, 2), 256));
    }
}
