package com.josebieco.talentsvocations.core;

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

    @Test
    void clickBeforeMinTicksKeepsArmedWithoutReset() {
        var c = new TwoStepConfirm(10, 60);
        assertFalse(c.click(0));
        assertFalse(c.click(5));
        assertTrue(c.isArmed(6));
        assertTrue(c.click(10));
    }

    @Test
    void clickAtExactlyWindowConfirms() {
        var c = new TwoStepConfirm(10, 60);
        c.click(0);
        assertTrue(c.click(60));
    }

    @Test
    void clickAtMinTicksConfirms() {
        var c = new TwoStepConfirm(10, 60);
        c.click(100);
        assertTrue(c.click(110));
    }
}
