package com.seunome.vanillatalents.client.ui;

import com.seunome.vanillatalents.capability.PlayerSkillData;
import com.seunome.vanillatalents.client.ClientTalentState;
import com.seunome.vanillatalents.core.*;
import com.seunome.vanillatalents.data.TalentRegistries;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Área com o fundo de bloco, as conexões e os nós de uma árvore de talentos, com arrastar e zoom (roda do mouse).
 * O conteúdo é desenhado em coordenadas próprias e posicionado pela {@link TreeViewport} via pose.
 */
public final class TreeView {

    public static final int CELL_W = 32;
    public static final int CELL_H = 36;
    public static final int FRAME = 26;

    private static final int PAD = 6;
    private static final int EDGE = 12;
    private static final int COLOR_LINE_UNMET = 0xFF555555;
    private static final int COLOR_LINE_MET = 0xFFFFFFFF;
    private static final int COLOR_BLACK = 0xFF000000;
    private static final int COLOR_GREEN = 0xFF55FF55;
    private static final int COLOR_SELECTED = 0xFFFFFF55;
    private static final int COLOR_LOCKED = 0x99000000;
    private static final int COLOR_EDGE = 0x66000000;
    private static final long ZOOM_LABEL_MS = 1000;

    /** Nível de zoom lembrado por árvore enquanto o jogo estiver aberto. */
    private static final Map<TreeCategory, Integer> ZOOM_MEMORY = new EnumMap<>(TreeCategory.class);

    private final int x, y, w, h;
    private @Nullable TreeCategory tree;
    private TreeViewport viewport;
    private long zoomChangedAt = Long.MIN_VALUE;
    private final Map<String, ItemStack> icons = new HashMap<>();

    public TreeView(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.viewport = new TreeViewport(w, h);
    }

    /** Troca a árvore exibida: volta ao canto e restaura o zoom lembrado dessa árvore. */
    public void setTree(TreeCategory tree) {
        this.tree = tree;
        icons.clear();
        viewport = new TreeViewport(w, h);
        reclamp();
        viewport.setZoomIndex(ZOOM_MEMORY.getOrDefault(tree, TreeViewport.DEFAULT_ZOOM_INDEX));
    }

    public boolean contains(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public void drag(double dx, double dy) {
        if (tree != null) viewport.drag(dx, dy);
    }

    /** Aproxima ({@code steps > 0}) ou afasta mantendo parado o ponto sob o cursor. */
    public void zoom(int steps, double mouseX, double mouseY) {
        if (tree == null || !viewport.zoomAt(steps, mouseX - x, mouseY - y)) return;
        ZOOM_MEMORY.put(tree, viewport.zoomIndex());
        zoomChangedAt = Util.getMillis();
    }

    /** Reaplica os limites (o conteúdo pode ter mudado após um sync/recarga). */
    public void reclamp() {
        TalentScreenModel.GridBounds b = bounds();
        viewport.setContent(contentW(b), contentH(b));
    }

    private List<TalentNode> nodes() {
        return tree == null ? List.of() : TalentRegistries.client().tree(tree);
    }

    private TalentScreenModel.GridBounds bounds() {
        return TalentScreenModel.gridBounds(nodes());
    }

    private static int contentW(TalentScreenModel.GridBounds b) {
        return (b.maxX() - b.minX()) * CELL_W + FRAME + 2 * PAD;
    }

    private static int contentH(TalentScreenModel.GridBounds b) {
        return (b.maxY() - b.minY()) * CELL_H + FRAME + 2 * PAD;
    }

    /** Posição do nó em coordenadas de conteúdo (antes do zoom e do deslocamento). */
    private static int nodeX(TalentNode n, TalentScreenModel.GridBounds b) {
        return PAD + (n.position().x() - b.minX()) * CELL_W;
    }

    private static int nodeY(TalentNode n, TalentScreenModel.GridBounds b) {
        return PAD + (n.position().y() - b.minY()) * CELL_H;
    }

    /** Nó sob o ponto (coordenadas de tela), ou null. */
    public @Nullable TalentNode nodeAt(double mouseX, double mouseY) {
        if (!contains(mouseX, mouseY)) return null;
        List<TalentNode> nodes = nodes();
        if (nodes.isEmpty()) return null;
        TalentScreenModel.GridBounds b = TalentScreenModel.gridBounds(nodes);
        double cx = viewport.toContentX(mouseX - x), cy = viewport.toContentY(mouseY - y);
        for (TalentNode n : nodes) {
            int nx = nodeX(n, b), ny = nodeY(n, b);
            if (cx >= nx && cx < nx + FRAME && cy >= ny && cy < ny + FRAME) return n;
        }
        return null;
    }

    public void render(GuiGraphicsExtractor g, @Nullable String selectedId, int mouseX, int mouseY) {
        if (tree == null) return;
        int offX = (int) Math.round(viewport.offsetX()), offY = (int) Math.round(viewport.offsetY());
        VanillaGui.tiled(g, Identifier.withDefaultNamespace(TalentScreenModel.backgroundTexture(tree)), x, y, w, h, offX, offY);
        g.fillGradient(x, y, x + w, y + EDGE, COLOR_EDGE, 0);
        g.fillGradient(x, y + h - EDGE, x + w, y + h, 0, COLOR_EDGE);

        List<TalentNode> nodes = nodes();
        if (nodes.isEmpty()) return;
        TalentScreenModel.GridBounds b = TalentScreenModel.gridBounds(nodes);
        TalentRegistry registry = TalentRegistries.client();
        PlayerSkillData data = ClientTalentState.data();
        TalentNode capstone = nodes.get(nodes.size() - 1);
        Font font = Minecraft.getInstance().font;

        // O scissor é transformado pela pose atual: liga antes de aplicar o zoom.
        g.enableScissor(x, y, x + w, y + h);
        g.pose().pushMatrix();
        g.pose().translate((float) (x + viewport.offsetX()), (float) (y + viewport.offsetY()));
        g.pose().scale((float) viewport.zoom(), (float) viewport.zoom());
        for (TalentNode n : nodes) drawConnections(g, data, registry, n, b);
        for (TalentNode n : nodes) drawNode(g, font, data, registry, n, b, n == capstone, n.id().equals(selectedId));
        g.pose().popMatrix();
        g.disableScissor();

        if (Util.getMillis() - zoomChangedAt < ZOOM_LABEL_MS) {
            String label = Math.round(viewport.zoom() * 100) + "%";
            g.text(font, label, x + w - font.width(label) - 4, y + 4, 0xFFFFFFFF, true);
        }
    }

    private static void drawConnections(GuiGraphicsExtractor g, PlayerSkillData data, TalentRegistry registry,
                                        TalentNode child, TalentScreenModel.GridBounds b) {
        int cx = nodeX(child, b) + FRAME / 2;
        int cy = nodeY(child, b);
        int midY = cy - (CELL_H - FRAME) / 2;
        for (Prerequisite p : child.prerequisites()) {
            TalentNode parent = registry.get(p.nodeId()).orElse(null);
            if (parent == null) continue;
            boolean met = TalentRules.effectiveLevel(data, registry, p.nodeId()) >= p.level();
            int px = nodeX(parent, b) + FRAME / 2;
            int py = nodeY(parent, b) + FRAME;
            if (met) line(g, px, py, midY, cx, cy, 1, COLOR_BLACK);
            line(g, px, py, midY, cx, cy, 0, met ? COLOR_LINE_MET : COLOR_LINE_UNMET);
        }
    }

    /** Desenha o L (vertical, horizontal, vertical) com espessura 1 + 2*grow px. */
    private static void line(GuiGraphicsExtractor g, int px, int py, int midY, int cx, int cy, int grow, int color) {
        g.fill(px - grow, py, px + 1 + grow, midY + 1 + grow, color);
        g.fill(Math.min(px, cx) - grow, midY - grow, Math.max(px, cx) + 1 + grow, midY + 1 + grow, color);
        g.fill(cx - grow, midY, cx + 1 + grow, cy, color);
    }

    private void drawNode(GuiGraphicsExtractor g, Font font, PlayerSkillData data, TalentRegistry registry,
                          TalentNode n, TalentScreenModel.GridBounds b, boolean capstone, boolean selected) {
        int nx = nodeX(n, b), ny = nodeY(n, b);
        NodeState state = TalentRules.nodeState(data, registry, n);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, VanillaGui.frameSprite(capstone, state == NodeState.MAXED), nx, ny, FRAME, FRAME);
        g.item(icon(n), nx + 5, ny + 5);

        if (state == NodeState.LOCKED) g.fill(nx + 3, ny + 3, nx + FRAME - 3, ny + FRAME - 3, COLOR_LOCKED);
        if (state == NodeState.AVAILABLE) g.outline(nx, ny, FRAME, FRAME, COLOR_GREEN);
        if (state == NodeState.IN_PROGRESS || state == NodeState.MAXED) {
            int level = TalentRules.effectiveLevel(data, registry, n.id());
            if (state == NodeState.IN_PROGRESS) {
                int bar = Math.max(1, (FRAME - 6) * level / n.maxLevel());
                g.fill(nx + 3, ny + FRAME - 4, nx + 3 + bar, ny + FRAME - 2, COLOR_GREEN);
            }
            String counter = level + "/" + n.maxLevel();
            g.text(font, counter, nx + FRAME - font.width(counter) - 1, ny + FRAME - 9, 0xFFFFFFFF, true);
        }
        if (selected) {
            g.outline(nx - 1, ny - 1, FRAME + 2, FRAME + 2, COLOR_SELECTED);
            g.outline(nx - 2, ny - 2, FRAME + 4, FRAME + 4, COLOR_SELECTED);
        }
    }

    private ItemStack icon(TalentNode n) {
        return icons.computeIfAbsent(n.id(), id -> {
            Identifier itemId = Identifier.tryParse(n.icon());
            return new ItemStack(itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)
                    ? Items.BARRIER : BuiltInRegistries.ITEM.getValue(itemId));
        });
    }
}
