package com.seunome.vanillatalents.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.seunome.vanillatalents.capability.PlayerSkillData;
import com.seunome.vanillatalents.client.ui.NodeDetailPanel;
import com.seunome.vanillatalents.client.ui.TreeView;
import com.seunome.vanillatalents.client.ui.VanillaGui;
import com.seunome.vanillatalents.core.*;
import com.seunome.vanillatalents.data.TalentRegistries;
import com.seunome.vanillatalents.network.C2SBuyNode;
import com.seunome.vanillatalents.network.C2SConvertXp;
import com.seunome.vanillatalents.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** Tela de talentos no estilo Conquistas: abas, árvore arrastável, painel de detalhes e rodapé de conversão. */
public class TalentScreen extends Screen {

    private static final int MAX_W = 400;
    private static final int MAX_H = 225;
    private static final int PANEL_W = 110;
    private static final int MARGIN = 7;
    private static final int GAP = 4;
    private static final int TITLE_H = 18;
    private static final int BUTTON_H = 20;
    private static final int XP_BAR_W = 182;
    private static final int XP_BAR_H = 5;
    private static final int TAB_W = 28;
    private static final int TAB_H = 32;
    private static final int COLOR_TITLE = 0xFF404040;
    private static final int COLOR_INFO = 0xFFFFFFFF;
    private static final Identifier XP_BAR_BACKGROUND = Identifier.withDefaultNamespace("hud/experience_bar_background");
    private static final Identifier XP_BAR_PROGRESS = Identifier.withDefaultNamespace("hud/experience_bar_progress");

    private final ItemStack commonIcon = new ItemStack(Items.SHIELD);
    private final ItemStack noClassIcon = new ItemStack(Items.COMPASS);

    private boolean classTab = false;
    private @Nullable String selectedId;

    // Layout calculado em init().
    private int left, top, winW, winH;
    private int treeX, treeY, treeW, treeH;
    private @Nullable TreeView treeView;
    private @Nullable String treeViewBounds;
    private @Nullable TreeCategory shownTree;
    private @Nullable NodeDetailPanel panel;
    private ItemStack classIcon = ItemStack.EMPTY;

    // Estado do arrastar.
    private boolean draggingTree;
    private boolean deselectOnRelease;

    public TalentScreen() {
        super(Component.translatable("gui.vanillatalents.title"));
    }

    /** Chamado quando chega S2CSyncPlayer ou S2CSyncDefinitions. */
    void onSync() {
        if (selectedId != null) {
            TreeCategory tree = visibleTree();
            TalentNode node = TalentRegistries.client().get(selectedId).orElse(null);
            if (node == null || node.tree() != tree) selectedId = null;
        }
        rebuildWidgets();
    }

    /** Mostra a aba Comum (false) ou Classe (true). */
    public void openTab(boolean toClassTab) {
        if (classTab != toClassTab) selectedId = null;
        classTab = toClassTab;
        if (width > 0) rebuildWidgets();
    }

    private @Nullable TreeCategory visibleTree() {
        if (!classTab) return TreeCategory.COMMON;
        return TreeCategory.byId(ClientTalentState.data().getCurrentClass()).filter(TreeCategory::isClass).orElse(null);
    }

    @Override
    protected void init() {
        layout();
        TreeCategory tree = visibleTree();
        PlayerSkillData data = ClientTalentState.data();
        EconomySettings economy = ClientTalentState.economy();
        LocalPlayer player = minecraft.player;

        // A TreeView sobrevive aos rebuilds do sync (mantém o arrastar); só é recriada se o tamanho mudou.
        String bounds = treeX + "," + treeY + "," + treeW + "," + treeH;
        if (treeView == null || !bounds.equals(treeViewBounds)) {
            treeView = new TreeView(treeX, treeY, treeW, treeH);
            treeViewBounds = bounds;
            shownTree = null;
        }
        if (tree != null && tree != shownTree) treeView.setTree(tree);
        shownTree = tree;

        panel = new NodeDetailPanel(panelX(), treeY - 1, PANEL_W, treeH + 2);
        addRenderableWidget(panel.buyButton());

        TreeCategory currentClass = TreeCategory.byId(data.getCurrentClass()).filter(TreeCategory::isClass).orElse(null);
        classIcon = currentClass == null ? noClassIcon : rootIcon(currentClass);

        boolean changeClass = classTab && tree != null;
        int buttons = changeClass ? 3 : 2;
        int rowW = winW - 2 * MARGIN;
        int bw = (rowW - GAP * (buttons - 1)) / buttons;
        int by = top + winH - MARGIN - BUTTON_H;
        int bx = left + MARGIN;

        String convertKey = economy.mode() == CostMode.LEVELS ? "gui.vanillatalents.convert.levels" : "gui.vanillatalents.convert.points";
        Button convert = Button.builder(Component.translatable(convertKey, economy.cost()), b -> ModNetwork.sendToServer(new C2SConvertXp(false)))
                .bounds(bx, by, bw, BUTTON_H).build();
        convert.active = player != null && economy.canAffordConversion(player.experienceLevel, player.experienceProgress);
        addRenderableWidget(convert);

        // Prévia; o servidor recalcula a quantidade real ao converter.
        int affordable = player == null ? 0 : economy.maxConversions(player.experienceLevel, player.experienceProgress);
        Button convertAll = Button.builder(Component.translatable("gui.vanillatalents.convert.all", affordable),
                b -> ModNetwork.sendToServer(new C2SConvertXp(true))).bounds(bx + bw + GAP, by, bw, BUTTON_H).build();
        convertAll.active = affordable > 0;
        addRenderableWidget(convertAll);

        if (changeClass) {
            addRenderableWidget(Button.builder(Component.translatable("gui.vanillatalents.change_class"), b -> openClassScreen())
                    .bounds(bx + 2 * (bw + GAP), by, bw, BUTTON_H).build());
        } else if (classTab) {
            int w = Math.min(120, treeW - 2 * GAP);
            addRenderableWidget(Button.builder(Component.translatable("gui.vanillatalents.choose_class"), b -> openClassScreen())
                    .bounds(treeX + (treeW - w) / 2, treeY + treeH / 2 + 4, w, BUTTON_H).build());
        }
    }

    /** Janela de até 400×225 centralizada; encolhe para caber (abas ocupam ~28 px acima). */
    private void layout() {
        winW = Math.min(MAX_W, width - 2 * MARGIN);
        winH = Math.min(MAX_H, height - TAB_H - 4);
        left = (width - winW) / 2;
        top = (height - winH + TAB_H - 4) / 2;
        treeX = left + MARGIN + 1;
        treeY = top + TITLE_H + 1;
        treeW = winW - 2 * MARGIN - PANEL_W - GAP - 2;
        int footerH = BUTTON_H + XP_BAR_H + 2 * GAP;
        treeH = winH - TITLE_H - MARGIN - footerH - 2;
    }

    private int panelX() {
        return treeX + treeW + 1 + GAP;
    }

    private void openClassScreen() {
        minecraft.gui.setScreen(new ClassSelectScreen(this));
    }

    private ItemStack rootIcon(TreeCategory tree) {
        TalentRegistry registry = TalentRegistries.client();
        String rootId = TalentScreenModel.classSummary(registry, tree).rootId();
        TalentNode root = rootId == null ? null : registry.get(rootId).orElse(null);
        if (root == null) return noClassIcon;
        Identifier itemId = Identifier.tryParse(root.icon());
        return new ItemStack(itemId == null ? Items.BARRIER : BuiltInRegistries.ITEM.getValue(itemId));
    }

    private int tabX(int index) {
        return left + (TAB_W + 4) * index;
    }

    private int tabY() {
        return top - TAB_H + 4;
    }

    private int tabAt(double mx, double my) {
        for (int i = 0; i < 2; i++) {
            int tx = tabX(i);
            if (mx >= tx && mx < tx + TAB_W && my >= tabY() && my < top) return i;
        }
        return -1;
    }

    private Component treeName(@Nullable TreeCategory tree) {
        if (tree == null) return Component.translatable("gui.vanillatalents.tab.class");
        if (tree == TreeCategory.COMMON) return Component.translatable("gui.vanillatalents.tab.common");
        return Component.translatable("vanillatalents.class." + tree.id());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int selectedTab = classTab ? 1 : 0;
        drawTab(g, 1 - selectedTab, false);
        VanillaGui.raisedPanel(g, left, top, winW, winH);
        drawTab(g, selectedTab, true);

        TreeCategory tree = visibleTree();
        PlayerSkillData data = ClientTalentState.data();
        TalentRegistry registry = TalentRegistries.client();
        LocalPlayer player = minecraft.player;

        g.text(font, Component.translatable("gui.vanillatalents.title.tree", treeName(tree)), left + MARGIN + 1, top + 6, COLOR_TITLE, false);
        Component status = Component.translatable("gui.vanillatalents.status", data.getAvailablePoints(), player == null ? 0 : player.experienceLevel);
        g.text(font, status, left + winW - MARGIN - 1 - font.width(status), top + 6, COLOR_TITLE, false);

        VanillaGui.insetPanel(g, treeX - 1, treeY - 1, treeW + 2, treeH + 2);
        TalentNode selected = selectedId == null ? null : registry.get(selectedId).orElse(null);
        if (registry.size() == 0) {
            g.centeredText(font, Component.translatable("gui.vanillatalents.loading"), treeX + treeW / 2, treeY + treeH / 2 - 4, COLOR_INFO);
        } else if (tree == null) {
            g.centeredText(font, Component.translatable("gui.vanillatalents.no_class"), treeX + treeW / 2, treeY + treeH / 2 - 12, COLOR_INFO);
        } else if (treeView != null) {
            treeView.render(g, selectedId, mouseX, mouseY);
        }

        if (panel != null) {
            if (tree != null && registry.size() > 0) {
                panel.render(g, selected, tree);
            } else {
                VanillaGui.insetPanel(g, panelX(), treeY - 1, PANEL_W, treeH + 2);
                panel.buyButton().visible = false;
            }
        }

        drawXpBar(g, player);
        super.extractRenderState(g, mouseX, mouseY, a);

        int hoveredTab = tabAt(mouseX, mouseY);
        if (hoveredTab >= 0) {
            Component name = Component.translatable(hoveredTab == 0 ? "gui.vanillatalents.tab.common" : "gui.vanillatalents.tab.class");
            g.setTooltipForNextFrame(font, List.of(name.getVisualOrderText()), mouseX, mouseY);
        } else if (tree != null && treeView != null && !draggingTree) {
            TalentNode hovered = treeView.nodeAt(mouseX, mouseY);
            if (hovered != null) {
                g.setTooltipForNextFrame(font, List.of(Component.translatable(hovered.nameKey()).getVisualOrderText()), mouseX, mouseY);
            }
        }
    }

    private void drawTab(GuiGraphicsExtractor g, int index, boolean selected) {
        Identifier sprite = index == 0
                ? (selected ? VanillaGui.TAB_ABOVE_LEFT_SELECTED : VanillaGui.TAB_ABOVE_LEFT)
                : (selected ? VanillaGui.TAB_ABOVE_MIDDLE_SELECTED : VanillaGui.TAB_ABOVE_MIDDLE);
        int tx = tabX(index), ty = tabY();
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, tx, ty, TAB_W, TAB_H);
        g.fakeItem(index == 0 ? commonIcon : classIcon, tx + 6, ty + 9);
    }

    private void drawXpBar(GuiGraphicsExtractor g, @Nullable LocalPlayer player) {
        int bx = left + (winW - XP_BAR_W) / 2;
        int by = top + winH - MARGIN - BUTTON_H - GAP - XP_BAR_H;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, XP_BAR_BACKGROUND, bx, by, XP_BAR_W, XP_BAR_H);
        int progress = player == null ? 0 : (int) (player.experienceProgress * (XP_BAR_W + 1));
        if (progress > 0) {
            g.blitSprite(RenderPipelines.GUI_TEXTURED, XP_BAR_PROGRESS, XP_BAR_W, XP_BAR_H, 0, 0, bx, by, Math.min(progress, XP_BAR_W), XP_BAR_H);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;

        int tab = tabAt(event.x(), event.y());
        if (tab >= 0) {
            openTab(tab == 1);
            return true;
        }
        if (treeView == null || visibleTree() == null || !treeView.contains(event.x(), event.y())) return false;

        draggingTree = true;
        TalentNode node = treeView.nodeAt(event.x(), event.y());
        deselectOnRelease = node == null;
        if (node != null) {
            selectedId = node.id();
            if (doubleClick && TalentRules.canPurchase(ClientTalentState.data(), TalentRegistries.client(), node.id()) == PurchaseResult.OK) {
                ModNetwork.sendToServer(new C2SBuyNode(node.id()));
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingTree && event.button() == InputConstants.MOUSE_BUTTON_LEFT && treeView != null) {
            treeView.drag(dx, dy);
            deselectOnRelease = false;
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingTree && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            if (deselectOnRelease) selectedId = null;
            draggingTree = false;
            deselectOnRelease = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
