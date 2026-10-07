package com.josebieco.talentsvocations.core;

/**
 * Vista da árvore com zoom e arrastar. O deslocamento é guardado em double (arrastos lentos de frações de pixel
 * se acumulam) e sempre limitado às bordas: conteúdo que cabe fica centralizado; o que não cabe vai de
 * {@code vista − conteúdo×zoom} até 0. Coordenadas "de vista" são relativas ao canto da área da árvore.
 */
public final class TreeViewport {

    public static final double[] ZOOMS = {0.5, 0.75, 1.0, 1.25};
    public static final int DEFAULT_ZOOM_INDEX = 2;

    private final int viewW, viewH;
    private int contentW, contentH;
    private int zoomIndex = DEFAULT_ZOOM_INDEX;
    private double offX, offY;

    public TreeViewport(int viewW, int viewH) {
        this.viewW = viewW;
        this.viewH = viewH;
    }

    /** Tamanho do conteúdo sem zoom; reaplica os limites (o conteúdo pode ter mudado após um sync). */
    public void setContent(int contentW, int contentH) {
        this.contentW = contentW;
        this.contentH = contentH;
        clamp();
    }

    public int zoomIndex() {
        return zoomIndex;
    }

    public double zoom() {
        return ZOOMS[zoomIndex];
    }

    /** Define o nível (limitado aos existentes) mantendo o centro da vista parado. */
    public void setZoomIndex(int index) {
        applyZoom(Math.max(0, Math.min(ZOOMS.length - 1, index)), viewW / 2.0, viewH / 2.0);
    }

    /** Aproxima ({@code steps > 0}) ou afasta mantendo parado o ponto sob o cursor; false se o nível não mudou. */
    public boolean zoomAt(int steps, double cursorX, double cursorY) {
        int target = Math.max(0, Math.min(ZOOMS.length - 1, zoomIndex + steps));
        if (target == zoomIndex) return false;
        applyZoom(target, cursorX, cursorY);
        return true;
    }

    private void applyZoom(int target, double cursorX, double cursorY) {
        double contentX = toContentX(cursorX), contentY = toContentY(cursorY);
        zoomIndex = target;
        offX = cursorX - contentX * zoom();
        offY = cursorY - contentY * zoom();
        clamp();
    }

    public void drag(double dx, double dy) {
        offX += dx;
        offY += dy;
        clamp();
    }

    public double offsetX() {
        return offX;
    }

    public double offsetY() {
        return offY;
    }

    public double toContentX(double viewX) {
        return (viewX - offX) / zoom();
    }

    public double toContentY(double viewY) {
        return (viewY - offY) / zoom();
    }

    private void clamp() {
        offX = clampAxis(offX, contentW * zoom(), viewW);
        offY = clampAxis(offY, contentH * zoom(), viewH);
    }

    private static double clampAxis(double offset, double scaled, int view) {
        if (scaled <= view) return (view - scaled) / 2;
        return Math.max(view - scaled, Math.min(0, offset));
    }
}
