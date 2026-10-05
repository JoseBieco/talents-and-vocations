package com.seunome.vanillatalents.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TwoStepConfirmTest {

    @Test
    void secondClickWithinWindowConfirms() {
        var c = new TwoStepConfirm(60);
        assertFalse(c.click(0));
        assertTrue(c.isArmed(10));
        assertTrue(c.click(30));
        assertFalse(c.isArmed(31));
    }

    @Test
    void clickAfterWindowRearms() {
        var c = new TwoStepConfirm(60);
        c.click(0);
        assertFalse(c.click(61));
        assertTrue(c.isArmed(61));
    }

    @Test
    void resetDisarms() {
        var c = new TwoStepConfirm(60);
        c.click(0);
        c.reset();
        assertFalse(c.isArmed(1));
    }
}
