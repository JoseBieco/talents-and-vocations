package com.seunome.vanillatalents.core.formula;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommonFormulasTest {

    @Test
    void regenExhaustionMultiplier() {
        assertEquals(0.7, CommonFormulas.regenExhaustionMultiplier(3, .1), 1e-9);
        assertEquals(1.0, CommonFormulas.regenExhaustionMultiplier(0, .1), 1e-9);
    }
}
