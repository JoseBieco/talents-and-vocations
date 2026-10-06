package com.seunome.vanillatalents.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.seunome.vanillatalents.capability.PlayerSkillData;
import com.seunome.vanillatalents.client.ui.VanillaGui;
import com.seunome.vanillatalents.core.ClassSlot;
import com.seunome.vanillatalents.core.TalentNode;
import com.seunome.vanillatalents.core.TalentRules;
import com.seunome.vanillatalents.core.TalentScreenModel;
import com.seunome.vanillatalents.core.TreeCategory;
import com.seunome.vanillatalents.core.TwoStepConfirm;
import com.seunome.vanillatalents.data.TalentRegistries;
import com.seunome.vanillatalents.network.C2SChangeClass;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Escolha e troca da classe de um espaço (principal ou secundária): lista à esquerda, detalhes à direita e
 * confirmação em dois cliques. A classe do outro espaço aparece apagada e não pode ser escolhida.
 */
public class ClassScreen extends Screen {

    private static final int MAX_W = 320;
    private static final int MAX_H = 214;
    private static final int MARGIN = 7;
    private static final int GAP = 4;
    private static final int TITLE_H = 18;
    private static final int LIST_W = 116;
    private static final int ROW_H = 28;
    private static final int FRAME = 26;
    private static final int SMALL_FRAME = 20;
    private static final int BUTTON_H = 20;
    private static final int PAD = 4;
    private static final int LINE_H = 10;
    private static final int CONFIRM_MIN_TICKS = 10;
    private static final int CONFIRM_WINDOW_TICKS = 60;
    private static final int COLOR_TITLE = 0xFF404040;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_MUTED = 0xFFCCCCCC;
    private static final int COLOR_ROW_HOVER = 0x40FFFFFF;
    private static final int COLOR_SELECTED = 0xFFFFFFFF;
    private static final int COLOR_DANGER = 0xFFFF5555;

    private final Screen parent;
    private final ClassSlot slot;
    private final TwoStepConfirm confirm = new TwoStepConfirm(CONFIRM_MIN_TICKS, CONFIRM_WINDOW_TICKS);
    private final Map<String, ItemStack> icons = new HashMap<>();
    private final List<TreeCategory> classes = new ArrayList<>();
    private long ticks;
    private @Nullable TreeCategory selected;
    private @Nullable Button action;

    private int left, top, winW, winH;
    private int listX, listY, panelX, panelY, panelW, panelH;
    /** Rolagem do texto do painel (≤ 0) e as medidas do último quadro. */
    private int detailScroll, detailContentH, detailViewH;

    public ClassScreen(Screen parent, ClassSlot slot) {
        super(Component.translatable(slot == ClassSlot.PRIMARY
                ? "gui.vanillatalents.classes.title.primary" : "gui.vanillatalents.classes.title.secondary"));
        this.parent = parent;
        this.slot = slot;
        for (TreeCategory tree : TreeCategory.values()) {
            if (tree.isClass()) classes.add(tree);
        }
    }

    /** Chamado quando chega estado novo do servidor: lista e painel são reconstruídos. */
    void onSync() {
        confirm.reset();
        rebuildWidgets();
    }

    /** Classe hoje no espaço que esta tela edita. */
    private String classInSlot() {
        PlayerSkillData data = ClientTalentState.data();
        return TalentScreenModel.classInSlot(slot, data.getPrimaryClass(), data.getSecondaryClass());
    }

    private TalentScreenModel.ClassChoice choice(TreeCategory tree) {
        PlayerSkillData data = ClientTalentState.data();
        return TalentScreenModel.classChoice(slot, tree.id(), data.getPrimaryClass(), data.getSecondaryClass());
    }

    private boolean selectable(TreeCategory tree) {
        return choice(tree) == TalentScreenModel.ClassChoice.AVAILABLE;
    }

    private boolean hasClass() {
        return !TalentRules.NO_CLASS.equals(classInSlot());
    }

    @Override
    protected void init() {
        winW = Math.min(MAX_W, width - 2 * MARGIN);
        winH = Math.min(MAX_H, height - 2 * MARGIN);
        left = (width - winW) / 2;
        top = (height - winH) / 2;
        listX = left + MARGIN + 1;
        listY = top + TITLE_H + 1;
        panelX = listX + LIST_W + GAP;
        panelW = left + winW - MARGIN - 1 - panelX;
        panelY = listY;
        panelH = winH - TITLE_H - MARGIN - 2;

        if (selected == null || !selectable(selected)) {
            selected = classes.stream().filter(this::selectable).findFirst().orElse(null);
            confirm.reset();
        }

        // Voltar fica abaixo da lista.
        addRenderableWidget(Button.builder(Component.translatable("gui.vanillatalents.back"), b -> goBack(false))
                .bounds(listX, panelY + panelH - BUTTON_H, LIST_W, BUTTON_H).build());

        action = null;
        if (selected != null) {
            action = Button.builder(actionLabel(), b -> onAction())
                    .bounds(panelX + PAD, panelY + panelH - PAD - BUTTON_H, panelW - 2 * PAD, BUTTON_H).build();
            action.active = canAct();
            addRenderableWidget(action);
        }
    }

    private Component className(TreeCategory tree) {
        return Component.translatable("vanillatalents.class." + tree.id());
    }

    private TalentScreenModel.RespecPreview preview() {
        PlayerSkillData data = ClientTalentState.data();
        return TalentScreenModel.respecPreview(data.getUnlockedNodes(), TalentRegistries.client(), slot,
                data.getPrimaryClass(), data.getSecondaryClass(), ClientTalentState.economy());
    }

    private boolean enoughLevels() {
        LocalPlayer player = minecraft.player;
        return player != null && player.experienceLevel >= preview().feeLevels();
    }

    private boolean canAct() {
        return selected != null && (!hasClass() || enoughLevels());
    }

    private Component actionLabel() {
        TreeCategory tree = selected;
        if (tree == null) return Component.empty();
        Component name = className(tree);
        if (!hasClass()) return Component.translatable("gui.vanillatalents.classes.choose", name);
        // Vermelho = ação destrutiva (zera a árvore atual); botões vanilla não aceitam fundo colorido.
        if (confirm.isArmed(ticks)) {
            return Component.translatable("gui.vanillatalents.classes.confirm").withStyle(ChatFormatting.RED);
        }
        int fee = preview().feeLevels();
        return (fee == 0
                ? Component.translatable("gui.vanillatalents.classes.change_free", name)
                : Component.translatable("gui.vanillatalents.classes.change", name, fee)).withStyle(ChatFormatting.RED);
    }

    private void onAction() {
        TreeCategory tree = selected;
        if (tree == null || !canAct()) return;
        if (!hasClass() || confirm.click(ticks)) {
            ModNetwork.sendToServer(new C2SChangeClass(slot, tree.id()));
            goBack(true);
        } else if (action != null) {
            action.setMessage(actionLabel());
        }
    }

    /** Volta à tela anterior; depois de agir, abre a aba do espaço alterado. */
    private void goBack(boolean toSlotTab) {
        minecraft.gui.setScreen(parent);
        if (toSlotTab && parent instanceof TalentScreen talent) {
            talent.openTab(slot == ClassSlot.PRIMARY ? TalentScreen.Tab.PRIMARY : TalentScreen.Tab.SECONDARY);
        }
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        if (action != null) {
            action.setMessage(actionLabel());
            action.active = canAct();
        }
    }

    private ItemStack icon(TalentNode node) {
        return icons.computeIfAbsent(node.id(), id -> {
            Identifier itemId = Identifier.tryParse(node.icon());
            return new ItemStack(itemId == null ? Items.BARRIER : BuiltInRegistries.ITEM.getValue(itemId));
        });
    }

    private @Nullable TalentNode node(@Nullable String id) {
        return id == null ? null : TalentRegistries.client().get(id).orElse(null);
    }

    private TalentScreenModel.ClassSummary summary(TreeCategory tree) {
        return TalentScreenModel.classSummary(TalentRegistries.client(), tree);
    }

    /** Linha de status (motivo em vermelho): fixa logo acima do botão de ação. */
    private int statusY() {
        return panelY + panelH - PAD - BUTTON_H - PAD - LINE_H;
    }

    private int rowAt(double mx, double my) {
        if (mx < listX || mx >= listX + LIST_W || my < listY) return -1;
        int i = (int) ((my - listY) / ROW_H);
        return i < classes.size() ? i : -1;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        VanillaGui.raisedPanel(g, left, top, winW, winH);
        g.text(font, title, left + MARGIN + 1, top + 6, COLOR_TITLE, false);

        VanillaGui.insetPanel(g, listX - 1, listY - 1, LIST_W + 2, classes.size() * ROW_H + 2);
        for (int i = 0; i < classes.size(); i++) drawRow(g, i, mouseX, mouseY);

        VanillaGui.insetPanel(g, panelX, panelY - 1, panelW, panelH + 2);
        if (selected != null) {
            // Texto corrido é recortado acima da área reservada ao status e ao botão.
            int top = panelY + PAD, bottom = statusY() - 1;
            g.enableScissor(panelX, panelY, panelX + panelW, bottom);
            detailContentH = drawDetails(g, selected, top + detailScroll) - detailScroll - top;
            g.disableScissor();
            detailViewH = bottom - top;
            detailScroll = TalentScreenModel.clampScroll(detailContentH, detailViewH, detailScroll);
            VanillaGui.scrollbar(g, panelX + panelW - PAD + 1, top, bottom,
                    TalentScreenModel.scrollThumb(detailContentH, detailViewH, detailScroll, bottom - top));
            if (hasClass() && !enoughLevels()) {
                g.text(font, Component.translatable("gui.vanillatalents.respec.not_enough", preview().feeLevels()),
                        panelX + PAD, statusY(), COLOR_DANGER, true);
            }
        }

        super.extractRenderState(g, mouseX, mouseY, a);
    }

    private void drawRow(GuiGraphicsExtractor g, int index, int mouseX, int mouseY) {
        TreeCategory tree = classes.get(index);
        TalentScreenModel.ClassChoice choice = choice(tree);
        boolean muted = choice != TalentScreenModel.ClassChoice.AVAILABLE;
        int y = listY + index * ROW_H;
        if (tree == selected) g.outline(listX, y, LIST_W, ROW_H, COLOR_SELECTED);
        else if (!muted && rowAt(mouseX, mouseY) == index) g.fill(listX, y, listX + LIST_W, y + ROW_H, COLOR_ROW_HOVER);

        int fy = y + (ROW_H - FRAME) / 2;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, VanillaGui.frameSprite(false, false), listX + 2, fy, FRAME, FRAME);
        TalentNode root = node(summary(tree).rootId());
        if (root != null) g.item(icon(root), listX + 2 + 5, fy + 5);

        Component label = switch (choice) {
            case CURRENT -> Component.translatable("gui.vanillatalents.classes.current", className(tree));
            case OTHER_SLOT -> Component.translatable("gui.vanillatalents.classes.other_slot", className(tree));
            case AVAILABLE -> className(tree);
        };
        List<FormattedCharSequence> lines = font.split(label, LIST_W - FRAME - 8);
        int shown = Math.min(lines.size(), 2);
        int ty = y + (ROW_H - shown * 9) / 2;
        for (int i = 0; i < shown; i++) {
            g.text(font, lines.get(i), listX + FRAME + 6, ty + i * 9, muted ? COLOR_MUTED : COLOR_TEXT, true);
        }
    }

    /** Desenha o texto do painel a partir de {@code y} e devolve o y logo abaixo da última linha. */
    private int drawDetails(GuiGraphicsExtractor g, TreeCategory tree, int y) {
        int x = panelX + PAD;
        int textW = panelW - 2 * PAD - 3;

        g.text(font, className(tree), x, y, COLOR_TEXT, true);
        y += LINE_H + 2;
        for (FormattedCharSequence line : font.split(Component.translatable("vanillatalents.class." + tree.id() + ".desc"), textW)) {
            g.text(font, line, x, y, COLOR_MUTED, false);
            y += 9;
        }
        y += 4;

        TalentScreenModel.ClassSummary summary = summary(tree);
        y = drawNodeRow(g, x, y, textW, "gui.vanillatalents.classes.root", node(summary.rootId()), false);
        y = drawNodeRow(g, x, y, textW, "gui.vanillatalents.classes.capstone", node(summary.capstoneId()), true);
        g.text(font, Component.translatable("gui.vanillatalents.classes.size", summary.nodeCount(), summary.totalPoints()), x, y + 1, COLOR_MUTED, false);
        y += LINE_H + 5;

        TalentScreenModel.RespecPreview preview = preview();
        // Com multiclasse desligado a secundária já não tem efeito: o aviso não se aplica.
        if (preview.freezesSecondary() && ClientTalentState.data().maxClasses() >= 2) {
            for (FormattedCharSequence line : font.split(Component.translatable("gui.vanillatalents.classes.freezes_secondary"), textW)) {
                g.text(font, line, x, y, COLOR_DANGER, true);
                y += 9;
            }
            y += 4;
        }
        if (!hasClass()) {
            g.text(font, Component.translatable("gui.vanillatalents.select_class.first_free"), x, y, COLOR_TEXT, true);
            return y + LINE_H;
        }
        if (preview.feeLevels() > 0) {
            g.text(font, Component.translatable("gui.vanillatalents.classes.fee", preview.feeLevels()), x, y, COLOR_TEXT, true);
            y += LINE_H;
        }
        g.text(font, Component.translatable("gui.vanillatalents.classes.reset", preview.spent()), x, y, COLOR_TEXT, true);
        y += LINE_H;
        g.text(font, Component.translatable("gui.vanillatalents.classes.refund", preview.refund()), x, y, COLOR_TEXT, true);
        y += LINE_H;
        g.text(font, Component.translatable("gui.vanillatalents.classes.common_safe"), x, y, COLOR_MUTED, false);
        return y + LINE_H;
    }

    private int drawNodeRow(GuiGraphicsExtractor g, int x, int y, int textW, String key, @Nullable TalentNode node, boolean capstone) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, VanillaGui.frameSprite(capstone, false), x, y, SMALL_FRAME, SMALL_FRAME);
        if (node != null) {
            g.item(icon(node), x + 2, y + 2);
            Component text = Component.translatable(key, Component.translatable(node.nameKey()));
            List<FormattedCharSequence> lines = font.split(text, textW - SMALL_FRAME - 4);
            if (!lines.isEmpty()) g.text(font, lines.get(0), x + SMALL_FRAME + 4, y + (SMALL_FRAME - 8) / 2, COLOR_TEXT, true);
        }
        return y + SMALL_FRAME + 2;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
        int row = rowAt(event.x(), event.y());
        if (row < 0) return false;
        TreeCategory tree = classes.get(row);
        if (!selectable(tree)) return true;
        if (tree != selected) {
            selected = tree;
            detailScroll = 0;
            confirm.reset();
            rebuildWidgets();
        }
        return true;
    }

    /** Roda do mouse rola o texto do painel de detalhes. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        boolean overPanel = mouseX >= panelX && mouseX < panelX + panelW && mouseY >= panelY && mouseY < statusY();
        if (scrollY != 0 && selected != null && overPanel) {
            detailScroll = TalentScreenModel.wheelScroll(detailContentH, detailViewH, detailScroll, scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        goBack(false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
