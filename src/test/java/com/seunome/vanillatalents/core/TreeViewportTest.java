package com.seunome.vanillatalents.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TreeViewportTest {

    static TreeViewport big() {
        TreeViewport v = new TreeViewport(300, 200);
        v.setContent(600, 400);
        return v;
    }

    @Test
    void contentThatFitsIsCentered() {
        TreeViewport v = new TreeViewport(300, 200);
        v.setContent(200, 100);
        assertEquals(50, v.offsetX(), 1e-9);
        assertEquals(50, v.offsetY(), 1e-9);
        v.drag(-30, 40);
        assertEquals(50, v.offsetX(), 1e-9, "conteúdo que cabe não arrasta");
    }

    @Test
    void slowSubPixelDragsAccumulateInBothDirections() {
        TreeViewport v = big();
        for (int i = 0; i < 4; i++) v.drag(-0.25, -0.5);
        assertEquals(-1.0, v.offsetX(), 1e-9);
        assertEquals(-2.0, v.offsetY(), 1e-9);
        for (int i = 0; i < 2; i++) v.drag(0.25, 0.5);
        assertEquals(-0.5, v.offsetX(), 1e-9);
        assertEquals(-1.0, v.offsetY(), 1e-9);
    }

    @Test
    void dragIsClampedToContentEdgesWithoutDrift() {
        TreeViewport v = big();
        v.drag(-1000, -1000);
        assertEquals(-300, v.offsetX(), 1e-9);
        assertEquals(-200, v.offsetY(), 1e-9);
        v.drag(10, 0);
        assertEquals(-290, v.offsetX(), 1e-9, "sair da borda responde no primeiro movimento");
        v.drag(5000, 0);
        assertEquals(0, v.offsetX(), 1e-9);
    }

    @Test
    void zoomKeepsThePointUnderTheCursor() {
        TreeViewport v = big();
        v.drag(-150, 0);
        assertEquals(300, v.toContentX(150), 1e-9);
        assertTrue(v.zoomAt(-1, 150, 100));
        assertEquals(0.75, v.zoom(), 1e-9);
        assertEquals(300, v.toContentX(150), 1e-9);
        assertEquals(-75, v.offsetX(), 1e-9);
    }

    @Test
    void zoomOutUntilItFitsThenCenters() {
        TreeViewport v = big();
        v.zoomAt(-1, 0, 0);
        v.zoomAt(-1, 0, 0);
        assertEquals(0.5, v.zoom(), 1e-9);
        assertEquals(0, v.offsetX(), 1e-9, "600×0,5 = 300 = vista: cabe exato");
        assertEquals(0, v.offsetY(), 1e-9);
        assertFalse(v.zoomAt(-1, 0, 0), "já está no mínimo");
    }

    @Test
    void zoomLevelsAndBounds() {
        TreeViewport v = big();
        assertEquals(1.0, v.zoom(), 1e-9);
        assertEquals(TreeViewport.DEFAULT_ZOOM_INDEX, v.zoomIndex());
        assertTrue(v.zoomAt(5, 0, 0));
        assertEquals(1.25, v.zoom(), 1e-9);
        assertFalse(v.zoomAt(1, 0, 0));
        v.setZoomIndex(-3);
        assertEquals(0.5, v.zoom(), 1e-9);
        v.setZoomIndex(99);
        assertEquals(1.25, v.zoom(), 1e-9);
    }

    @Test
    void toContentInvertsZoomAndOffset() {
        TreeViewport v = big();
        v.drag(-100, -40);
        v.zoomAt(1, 0, 0);
        double cx = v.toContentX(80), cy = v.toContentY(60);
        assertEquals(80, v.offsetX() + cx * v.zoom(), 1e-9);
        assertEquals(60, v.offsetY() + cy * v.zoom(), 1e-9);
    }

    @Test
    void shrinkingContentReclamps() {
        TreeViewport v = big();
        v.drag(-300, -200);
        v.setContent(350, 250);
        assertEquals(-50, v.offsetX(), 1e-9);
        assertEquals(-50, v.offsetY(), 1e-9);
    }
}
