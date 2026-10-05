package com.seunome.vanillatalents.core.formula;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FarmerFormulasTest {

    @Test
    void areaOffsets_crossAtLevelOne() {
        assertEquals(Set.of(new FarmerFormulas.Offset(1, 0), new FarmerFormulas.Offset(-1, 0),
                        new FarmerFormulas.Offset(0, 1), new FarmerFormulas.Offset(0, -1)),
                Set.copyOf(FarmerFormulas.areaOffsets(1)));
        assertEquals(4, FarmerFormulas.areaOffsets(1).size());
    }

    @Test
    void areaOffsets_threeByThreeAtLevelTwo() {
        List<FarmerFormulas.Offset> offsets = FarmerFormulas.areaOffsets(2);
        assertEquals(8, offsets.size());
        assertFalse(offsets.contains(new FarmerFormulas.Offset(0, 0)));
        assertTrue(offsets.contains(new FarmerFormulas.Offset(1, 1)));
        assertTrue(FarmerFormulas.areaOffsets(0).isEmpty());
    }

    @Test
    void breedingCooldown() {
        assertEquals(3300, FarmerFormulas.breedingCooldown(6000, 3, .15));
        assertEquals(6000, FarmerFormulas.breedingCooldown(6000, 0, .15));
    }

    @Test
    void auraRadius_isBasePlusLevel() {
        assertEquals(3, FarmerFormulas.auraRadius(1, 2));
        assertEquals(4, FarmerFormulas.auraRadius(2, 2));
        assertEquals(5, FarmerFormulas.auraRadius(3, 2));
    }

    @Test
    void auraAllowed_onlyIfMovedRecently() {
        assertTrue(FarmerFormulas.auraAllowed(1000, 2200, 1200));
        assertFalse(FarmerFormulas.auraAllowed(1000, 2201, 1200));
    }

    @Test
    void auraPick_capsAtMaxPerPulse() {
        // 100 candidatos, todas as rolagens passam → no máximo 32
        assertEquals(32, FarmerFormulas.auraPickCount(100, 0.05, () -> 0.0, 32));
        // nenhuma rolagem passa
        assertEquals(0, FarmerFormulas.auraPickCount(100, 0.05, () -> 0.99, 32));
    }
}
