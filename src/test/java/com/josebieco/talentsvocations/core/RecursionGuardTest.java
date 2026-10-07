package com.josebieco.talentsvocations.core;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RecursionGuardTest {

    static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @Test
    void enterTwiceWithSameKeyIsRejected() {
        RecursionGuard guard = new RecursionGuard();
        assertTrue(guard.enter(A, "miner_vein"));
        assertTrue(guard.isActive(A, "miner_vein"));
        assertFalse(guard.enter(A, "miner_vein"));
    }

    @Test
    void exitAllowsEnteringAgain() {
        RecursionGuard guard = new RecursionGuard();
        assertTrue(guard.enter(A, "miner_vein"));
        guard.exit(A, "miner_vein");
        assertFalse(guard.isActive(A, "miner_vein"));
        assertTrue(guard.enter(A, "miner_vein"));
    }

    @Test
    void keysAndPlayersAreIndependent() {
        RecursionGuard guard = new RecursionGuard();
        assertTrue(guard.enter(A, "miner_vein"));
        assertTrue(guard.enter(A, "warrior_cleave"));
        assertTrue(guard.enter(B, "miner_vein"));
        assertFalse(guard.isActive(B, "warrior_cleave"));
    }

    @Test
    void runGuarded_skipsNestedCallAndAlwaysExits() {
        RecursionGuard guard = new RecursionGuard();
        int[] calls = {0};
        Runnable[] self = new Runnable[1];
        self[0] = () -> {
            calls[0]++;
            guard.runGuarded(A, "farmer_area_harvest", self[0]);
        };
        assertTrue(guard.runGuarded(A, "farmer_area_harvest", self[0]));
        assertEquals(1, calls[0]);
        assertFalse(guard.isActive(A, "farmer_area_harvest"));

        assertThrows(IllegalStateException.class,
                () -> guard.runGuarded(A, "x", () -> { throw new IllegalStateException("boom"); }));
        assertFalse(guard.isActive(A, "x"));
    }
}
