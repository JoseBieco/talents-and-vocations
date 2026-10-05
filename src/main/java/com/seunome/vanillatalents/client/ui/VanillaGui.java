package com.seunome.vanillatalents.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Utilitários de desenho no estilo da interface vanilla (painéis em relevo, fundos ladrilhados, molduras). */
public final class VanillaGui {

    public static final Identifier TASK_FRAME_UNOBTAINED = Identifier.withDefaultNamespace("advancements/task_frame_unobtained");
    public static final Identifier TASK_FRAME_OBTAINED = Identifier.withDefaultNamespace("advancements/task_frame_obtained");
    public static final Identifier CHALLENGE_FRAME_UNOBTAINED = Identifier.withDefaultNamespace("advancements/challenge_frame_unobtained");
    public static final Identifier CHALLENGE_FRAME_OBTAINED = Identifier.withDefaultNamespace("advancements/challenge_frame_obtained");
    public static final Identifier TAB_ABOVE_LEFT = Identifier.withDefaultNamespace("advancements/tab_above_left");
    public static final Identifier TAB_ABOVE_LEFT_SELECTED = Identifier.withDefaultNamespace("advancements/tab_above_left_selected");
    public static final Identifier TAB_ABOVE_MIDDLE = Identifier.withDefaultNamespace("advancements/tab_above_middle");
    public static final Identifier TAB_ABOVE_MIDDLE_SELECTED = Identifier.withDefaultNamespace("advancements/tab_above_middle_selected");

    public static final int COLOR_PANEL = 0xFFC6C6C6;
    public static final int COLOR_LIGHT = 0xFFFFFFFF;
    public static final int COLOR_DARK = 0xFF555555;
    public static final int COLOR_INSET = 0xFF8B8B8B;
    public static final int COLOR_INSET_DARK = 0xFF373737;
    public static final int COLOR_OUTLINE = 0xFF000000;

    private static final int TILE = 16;

    private VanillaGui() {
    }

    /** Janela em relevo (clara em cima/esquerda, escura embaixo/direita) com contorno preto externo. */
    public static void raisedPanel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, COLOR_OUTLINE);
        g.fill(x, y, x + w, y + h, COLOR_PANEL);
        g.fill(x, y, x + w - 1, y + 1, COLOR_LIGHT);
        g.fill(x, y, x + 1, y + h - 1, COLOR_LIGHT);
        g.fill(x + 1, y + h - 1, x + w, y + h, COLOR_DARK);
        g.fill(x + w - 1, y + 1, x + w, y + h, COLOR_DARK);
    }

    /** Painel afundado (escuro em cima/esquerda, claro embaixo/direita). */
    public static void insetPanel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, COLOR_INSET);
        g.fill(x, y, x + w - 1, y + 1, COLOR_INSET_DARK);
        g.fill(x, y, x + 1, y + h - 1, COLOR_INSET_DARK);
        g.fill(x + 1, y + h - 1, x + w, y + h, COLOR_LIGHT);
        g.fill(x + w - 1, y + 1, x + w, y + h, COLOR_LIGHT);
    }

    /** Preenche a área com ladrilhos de 16 px da textura, deslocados por (offsetX, offsetY) e recortados por scissor. */
    public static void tiled(GuiGraphicsExtractor g, Identifier texture, int x, int y, int w, int h, int offsetX, int offsetY) {
        g.enableScissor(x, y, x + w, y + h);
        int startX = x + Math.floorMod(offsetX, TILE) - TILE;
        int startY = y + Math.floorMod(offsetY, TILE) - TILE;
        for (int ty = startY; ty < y + h; ty += TILE) {
            for (int tx = startX; tx < x + w; tx += TILE) {
                g.blit(RenderPipelines.GUI_TEXTURED, texture, tx, ty, 0f, 0f, TILE, TILE, TILE, TILE);
            }
        }
        g.disableScissor();
    }

    /** Sprite da moldura de um nó: desafio para o nó final da árvore, tarefa para os demais. */
    public static Identifier frameSprite(boolean capstone, boolean obtained) {
        if (capstone) return obtained ? CHALLENGE_FRAME_OBTAINED : CHALLENGE_FRAME_UNOBTAINED;
        return obtained ? TASK_FRAME_OBTAINED : TASK_FRAME_UNOBTAINED;
    }
}
