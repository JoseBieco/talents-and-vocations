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
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Tela de talentos no estilo Conquistas: abas, árvore arrastável, painel de detalhes e rodapé de conversão. */
public class TalentScreen extends Screen {

    /** Abas: Árvore Comum, classe principal e classe secundária. */
    public enum Tab { COMMON, PRIMARY, SECONDARY }

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
    private static final int COLOR_BANNER_BG = 0xC0000000;
    private static final int COLOR_BANNER_TEXT = 0xFFFF5555;
    private static final Identifier XP_BAR_BACKGROUND = Identifier.withDefaultNamespace("hud/experience_bar_background");
    private static final Identifier XP_BAR_PROGRESS = Identifier.withDefaultNamespace("hud/experience_bar_progress");

    private final ItemStack commonIcon = new ItemStack(Items.SHIELD);
    private final ItemStack noClassIcon = new ItemStack(Items.COMPASS);
    private final ItemStack noSecondaryIcon = new ItemStack(Items.BOOK);

    private Tab tab = Tab.COMMON;
    private @Nullable String selectedId;

    // Layout calculado em init().
    private int left, top, winW, winH;
    private int treeX, treeY, treeW, treeH;
    private @Nullable TreeView treeView;
    private @Nullable String treeViewBounds;
    private @Nullable TreeCategory shownTree;
    private @Nullable NodeDetailPanel panel;
    private ItemStack classIcon = ItemStack.EMPTY;
    private ItemStack secondaryIcon = ItemStack.EMPTY;

    // Estado do arrastar.
    private boolean draggingTree;
    private boolean deselectOnRelease;
    private boolean lastClickBought;

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

    /** Mostra a aba pedida (a seleção de nó é limpa ao mudar de aba). */
    public void openTab(Tab target) {
        if (tab != target) selectedId = null;
        tab = target;
        if (width > 0) rebuildWidgets();
    }

    private static @Nullable TreeCategory classTree(String id) {
        return TreeCategory.byId(id).filter(TreeCategory::isClass).orElse(null);
    }

    private TalentScreenModel.SecondaryTab secondaryState() {
        return TalentScreenModel.secondaryTab(ClientTalentState.data(), TalentRegistries.client());
    }

    /** Árvore mostrada na aba atual; a secundária aparece também congelada (com a faixa de aviso). */
    private @Nullable TreeCategory visibleTree() {
        PlayerSkillData data = ClientTalentState.data();
        return switch (tab) {
            case COMMON -> TreeCategory.COMMON;
            case PRIMARY -> classTree(data.getPrimaryClass());
            case SECONDARY -> switch (secondaryState()) {
                case ACTIVE, FROZEN -> classTree(data.getSecondaryClass());
                default -> null;
            };
        };
    }

    private static ClassSlot slotOf(Tab tab) {
        return tab == Tab.SECONDARY ? ClassSlot.SECONDARY : ClassSlot.PRIMARY;
    }

    private boolean secondaryFrozen() {
        return tab == Tab.SECONDARY && secondaryState() == TalentScreenModel.SecondaryTab.FROZEN;
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
        treeView.reclamp();

        NodeDetailPanel previousPanel = panel;
        panel = new NodeDetailPanel(panelX(), treeY - 1, PANEL_W, treeH + 2);
        panel.keepScrollFrom(previousPanel);
        addRenderableWidget(panel.buyButton());

        TreeCategory primary = classTree(data.getPrimaryClass());
        classIcon = primary == null ? noClassIcon : rootIcon(primary, noClassIcon);
        TreeCategory secondary = classTree(data.getSecondaryClass());
        secondaryIcon = secondary == null ? noSecondaryIcon : rootIcon(secondary, noSecondaryIcon);

        boolean changeClass = tab != Tab.COMMON && tree != null;
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
            String key = tab == Tab.PRIMARY ? "gui.vanillatalents.change_class" : "gui.vanillatalents.change_secondary";
            addRenderableWidget(Button.builder(Component.translatable(key), b -> openClassScreen())
                    .bounds(bx + 2 * (bw + GAP), by, bw, BUTTON_H).build());
        } else if (tab == Tab.PRIMARY || (tab == Tab.SECONDARY && secondaryState() == TalentScreenModel.SecondaryTab.CHOOSE)) {
            String key = tab == Tab.PRIMARY ? "gui.vanillatalents.choose_class" : "gui.vanillatalents.choose_secondary";
            int w = Math.min(120, treeW - 2 * GAP);
            addRenderableWidget(Button.builder(Component.translatable(key), b -> openClassScreen())
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
        minecraft.gui.setScreen(new ClassScreen(this, slotOf(tab)));
    }

    private ItemStack rootIcon(TreeCategory tree, ItemStack fallback) {
        TalentRegistry registry = TalentRegistries.client();
        String rootId = TalentScreenModel.classSummary(registry, tree).rootId();
        TalentNode root = rootId == null ? null : registry.get(rootId).orElse(null);
        if (root == null) return fallback;
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
        for (int i = 0; i < Tab.values().length; i++) {
            int tx = tabX(i);
            if (mx >= tx && mx < tx + TAB_W && my >= tabY() && my < top) return i;
        }
        return -1;
    }

    private static Component tabName(Tab tab) {
        return Component.translatable(switch (tab) {
            case COMMON -> "gui.vanillatalents.tab.common";
            case PRIMARY -> "gui.vanillatalents.tab.primary";
            case SECONDARY -> "gui.vanillatalents.tab.secondary";
        });
    }

    private Component treeName(@Nullable TreeCategory tree) {
        if (tree == null) return tabName(tab);
        if (tree == TreeCategory.COMMON) return Component.translatable("gui.vanillatalents.tab.common");
        return Component.translatable("vanillatalents.class." + tree.id());
    }

    /** Mensagem da área da árvore quando a aba não tem árvore para mostrar. */
    private Component emptyTreeMessage() {
        if (tab != Tab.SECONDARY) return Component.translatable("gui.vanillatalents.no_class");
        return Component.translatable(switch (secondaryState()) {
            case DISABLED -> "gui.vanillatalents.secondary.disabled";
            case LOCKED -> "gui.vanillatalents.secondary.locked";
            default -> "gui.vanillatalents.secondary.none";
        });
    }

    /** Linhas do tooltip de uma aba: nome da aba e, nas de classe, a classe escolhida. */
    private List<FormattedCharSequence> tabTooltip(Tab hovered) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(tabName(hovered).getVisualOrderText());
        PlayerSkillData data = ClientTalentState.data();
        TreeCategory tree = switch (hovered) {
            case COMMON -> null;
            case PRIMARY -> classTree(data.getPrimaryClass());
            case SECONDARY -> switch (secondaryState()) {
                case ACTIVE, FROZEN -> classTree(data.getSecondaryClass());
                default -> null;
            };
        };
        if (tree != null) {
            lines.add(Component.translatable("vanillatalents.class." + tree.id()).withStyle(ChatFormatting.GRAY).getVisualOrderText());
        }
        return lines;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        for (Tab t : Tab.values()) {
            if (t != tab) drawTab(g, t.ordinal(), false);
        }
        VanillaGui.raisedPanel(g, left, top, winW, winH);
        drawTab(g, tab.ordinal(), true);

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
            List<FormattedCharSequence> lines = font.split(emptyTreeMessage(), treeW - 4 * GAP);
            int ty = treeY + treeH / 2 - 4 - lines.size() * font.lineHeight;
            for (FormattedCharSequence line : lines) {
                g.centeredText(font, line, treeX + treeW / 2, ty, COLOR_INFO);
                ty += font.lineHeight;
            }
        } else if (treeView != null) {
            treeView.render(g, selectedId, mouseX, mouseY);
            if (secondaryFrozen()) drawFrozenBanner(g);
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
            g.setTooltipForNextFrame(font, tabTooltip(Tab.values()[hoveredTab]), mouseX, mouseY);
        } else if (tree != null && treeView != null && !draggingTree && !overFrozenBanner(mouseX, mouseY)) {
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
        ItemStack icon = switch (Tab.values()[index]) {
            case COMMON -> commonIcon;
            case PRIMARY -> classIcon;
            case SECONDARY -> secondaryIcon;
        };
        g.fakeItem(icon, tx + 6, ty + 9);
    }

    private List<FormattedCharSequence> frozenBannerLines() {
        return font.split(Component.translatable("gui.vanillatalents.secondary.frozen"), treeW - 4 * GAP);
    }

    private int frozenBannerHeight() {
        return frozenBannerLines().size() * font.lineHeight + 2 * GAP;
    }

    /** Faixa escura no topo da árvore congelada, para o aviso ficar legível sobre o fundo. */
    private void drawFrozenBanner(GuiGraphicsExtractor g) {
        g.fill(treeX, treeY, treeX + treeW, treeY + frozenBannerHeight(), COLOR_BANNER_BG);
        int ty = treeY + GAP;
        for (FormattedCharSequence line : frozenBannerLines()) {
            g.centeredText(font, line, treeX + treeW / 2, ty, COLOR_BANNER_TEXT);
            ty += font.lineHeight;
        }
    }

    private boolean overFrozenBanner(double mx, double my) {
        return secondaryFrozen() && mx >= treeX && mx < treeX + treeW && my >= treeY && my < treeY + frozenBannerHeight();
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

        int clickedTab = tabAt(event.x(), event.y());
        if (clickedTab >= 0) {
            openTab(Tab.values()[clickedTab]);
            return true;
        }
        if (treeView == null || visibleTree() == null || !treeView.contains(event.x(), event.y())) return false;

        draggingTree = true;
        // A faixa de "congelada" cobre o topo da árvore: clicar nela só arrasta.
        TalentNode node = overFrozenBanner(event.x(), event.y()) ? null : treeView.nodeAt(event.x(), event.y());
        deselectOnRelease = node == null;
        boolean bought = false;
        if (node != null) {
            boolean alreadySelected = node.id().equals(selectedId);
            selectedId = node.id();
            if (doubleClick && alreadySelected && !lastClickBought
                    && TalentRules.canPurchase(ClientTalentState.data(), TalentRegistries.client(), node.id()) == PurchaseResult.OK) {
                ModNetwork.sendToServer(new C2SBuyNode(node.id()));
                bought = true;
            }
        }
        lastClickBought = bought;
        return true;
    }

    /** Roda do mouse: zoom sobre a árvore, rolagem sobre o painel de detalhes. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && treeView != null && visibleTree() != null && treeView.contains(mouseX, mouseY)) {
            treeView.zoom(scrollY > 0 ? 1 : -1, mouseX, mouseY);
            return true;
        }
        if (scrollY != 0 && panel != null && panel.contains(mouseX, mouseY)) {
            panel.scroll(scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
