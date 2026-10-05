package com.seunome.vanillatalents.client;

import com.seunome.vanillatalents.capability.PlayerSkillData;
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

/** Tela principal: abas Comum/Classe, grade de nós, conversão de XP e troca de classe. */
public class TalentScreen extends Screen {

    static final int CELL_W = 30;
    static final int CELL_H = 34;
    static final int NODE = 22;
    static final int TREE_TOP = 52;

    private static final int COLOR_LINE_MET = 0xFFAAAAAA;
    private static final int COLOR_LINE_UNMET = 0xFF444444;
    private static final int COLOR_GOLD = 0xFFFFD700;

    private boolean classTab = false;
    private final Map<String, ItemStack> icons = new HashMap<>();

    public TalentScreen() {
        super(Component.translatable("gui.vanillatalents.title"));
    }

    /** Chamado quando chega S2CSyncPlayer ou S2CSyncDefinitions. */
    void onSync() {
        rebuildWidgets();
    }

    @Override
    protected void init() {
        PlayerSkillData data = ClientTalentState.data();
        EconomySettings economy = ClientTalentState.economy();
        LocalPlayer player = minecraft.player;

        addRenderableWidget(Button.builder(Component.translatable("gui.vanillatalents.tab.common"), b -> switchTab(false))
                .bounds(8, 8, 70, 20).build()).active = classTab;
        addRenderableWidget(Button.builder(Component.translatable("gui.vanillatalents.tab.class"), b -> switchTab(true))
                .bounds(82, 8, 70, 20).build()).active = !classTab;

        String convertKey = economy.mode() == CostMode.LEVELS ? "gui.vanillatalents.convert.levels" : "gui.vanillatalents.convert.points";
        Button convert = Button.builder(Component.translatable(convertKey, economy.cost()), b -> ModNetwork.sendToServer(new C2SConvertXp()))
                .bounds(8, height - 28, 160, 20).build();
        convert.active = player != null && economy.canAffordConversion(player.experienceLevel, player.experienceProgress);
        addRenderableWidget(convert);

        if (classTab) {
            boolean noClass = TalentRules.NO_CLASS.equals(data.getCurrentClass());
            String key = noClass ? "gui.vanillatalents.choose_class" : "gui.vanillatalents.change_class";
            int w = 120;
            int x = noClass ? (width - w) / 2 : width - w - 8;
            int y = noClass ? height / 2 : height - 28;
            addRenderableWidget(Button.builder(Component.translatable(key), b -> minecraft.gui.setScreen(new ClassSelectScreen(this)))
                    .bounds(x, y, w, 20).build());
        }
    }

    private void switchTab(boolean toClass) {
        classTab = toClass;
        rebuildWidgets();
    }

    private @Nullable TreeCategory visibleTree() {
        if (!classTab) return TreeCategory.COMMON;
        return TreeCategory.byId(ClientTalentState.data().getCurrentClass()).filter(TreeCategory::isClass).orElse(null);
    }

    private int originX(List<TalentNode> nodes) {
        TalentScreenModel.GridBounds b = TalentScreenModel.gridBounds(nodes);
        int treeWidth = (b.maxX() - b.minX()) * CELL_W + NODE;
        return (width - treeWidth) / 2 - b.minX() * CELL_W;
    }

    private int nodeX(TalentNode n, int originX) {
        return originX + n.position().x() * CELL_W;
    }

    private int nodeY(TalentNode n) {
        return TREE_TOP + n.position().y() * CELL_H;
    }

    private @Nullable TalentNode nodeAt(double mouseX, double mouseY) {
        TreeCategory tree = visibleTree();
        if (tree == null) return null;
        List<TalentNode> nodes = TalentRegistries.client().tree(tree);
        int ox = originX(nodes);
        for (TalentNode n : nodes) {
            int x = nodeX(n, ox), y = nodeY(n);
            if (mouseX >= x && mouseX < x + NODE && mouseY >= y && mouseY < y + NODE) return n;
        }
        return null;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        PlayerSkillData data = ClientTalentState.data();
        TalentRegistry registry = TalentRegistries.client();

        LocalPlayer player = minecraft.player;
        int level = player == null ? 0 : player.experienceLevel;
        Component info = Component.translatable("gui.vanillatalents.xp_level", level).append("   ")
                .append(Component.translatable("gui.vanillatalents.points", data.getAvailablePoints()));
        g.text(font, info, width - font.width(info) - 8, 14, 0xFFFFFFFF);
        g.centeredText(font, title, width / 2, 36, 0xFFFFFFFF);

        if (registry.size() == 0) {
            g.centeredText(font, Component.translatable("gui.vanillatalents.loading"), width / 2, height / 2 - 20, 0xFFAAAAAA);
            return;
        }
        TreeCategory tree = visibleTree();
        if (tree == null) {
            g.centeredText(font, Component.translatable("gui.vanillatalents.no_class"), width / 2, height / 2 - 20, 0xFFAAAAAA);
            return;
        }
        if (tree.isClass()) {
            Component cls = Component.translatable("gui.vanillatalents.current_class",
                    Component.translatable("vanillatalents.class." + tree.id()));
            g.text(font, cls, width - font.width(cls) - 8, height - 22, 0xFFFFFFFF);
        }

        List<TalentNode> nodes = registry.tree(tree);
        int ox = originX(nodes);
        for (TalentNode n : nodes) drawConnections(g, data, registry, n, ox);
        for (TalentNode n : nodes) drawNode(g, data, registry, n, ox);

        TalentNode hovered = nodeAt(mouseX, mouseY);
        if (hovered != null) g.setTooltipForNextFrame(font, tooltip(data, registry, hovered), mouseX, mouseY);
    }

    private void drawConnections(GuiGraphicsExtractor g, PlayerSkillData data, TalentRegistry registry, TalentNode child, int ox) {
        int cx = nodeX(child, ox) + NODE / 2;
        int cy = nodeY(child);
        int midY = cy - (CELL_H - NODE) / 2;
        for (Prerequisite p : child.prerequisites()) {
            TalentNode parent = registry.get(p.nodeId()).orElse(null);
            if (parent == null) continue;
            int color = TalentRules.effectiveLevel(data, registry, p.nodeId()) >= p.level() ? COLOR_LINE_MET : COLOR_LINE_UNMET;
            int px = nodeX(parent, ox) + NODE / 2;
            g.verticalLine(px, nodeY(parent) + NODE, midY, color);
            g.horizontalLine(px, cx, midY, color);
            g.verticalLine(cx, midY, cy, color);
        }
    }

    private void drawNode(GuiGraphicsExtractor g, PlayerSkillData data, TalentRegistry registry, TalentNode n, int ox) {
        int x = nodeX(n, ox), y = nodeY(n);
        NodeState state = TalentRules.nodeState(data, registry, n);
        int bg = switch (state) {
            case LOCKED -> 0xFF202020;
            case AVAILABLE -> 0xFF2E5A2E;
            case IN_PROGRESS -> 0xFF35356A;
            case MAXED -> 0xFF5A4A1A;
        };
        int border = switch (state) {
            case LOCKED -> 0xFF555555;
            case AVAILABLE -> 0xFF55FF55;
            case IN_PROGRESS -> 0xFF8888FF;
            case MAXED -> COLOR_GOLD;
        };
        g.fill(x, y, x + NODE, y + NODE, bg);
        g.outline(x, y, NODE, NODE, border);
        if (state == NodeState.MAXED) g.outline(x - 1, y - 1, NODE + 2, NODE + 2, COLOR_GOLD);
        g.item(icon(n), x + 3, y + 3);
        if (state == NodeState.LOCKED) g.fill(x + 1, y + 1, x + NODE - 1, y + NODE - 1, 0xAA000000);
        if (state == NodeState.IN_PROGRESS || state == NodeState.MAXED) {
            String counter = TalentRules.effectiveLevel(data, registry, n.id()) + "/" + n.maxLevel();
            g.text(font, counter, x + NODE - font.width(counter), y + NODE - 6, 0xFFFFFFFF, true);
        }
    }

    private ItemStack icon(TalentNode n) {
        return icons.computeIfAbsent(n.id(), id -> {
            Identifier itemId = Identifier.tryParse(n.icon());
            return new ItemStack(itemId == null ? Items.BARRIER : BuiltInRegistries.ITEM.getValue(itemId));
        });
    }

    private List<FormattedCharSequence> tooltip(PlayerSkillData data, TalentRegistry registry, TalentNode n) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(Component.translatable(n.nameKey()).withStyle(ChatFormatting.YELLOW).getVisualOrderText());
        lines.addAll(font.split(Component.translatable(n.descKey()).withStyle(ChatFormatting.GRAY), 220));
        lines.add(Component.translatable("gui.vanillatalents.level",
                TalentRules.effectiveLevel(data, registry, n.id()), n.maxLevel()).getVisualOrderText());
        for (Prerequisite p : TalentScreenModel.unmetPrerequisites(data, registry, n)) {
            Component name = registry.get(p.nodeId()).<Component>map(pre -> Component.translatable(pre.nameKey()))
                    .orElse(Component.literal(p.nodeId()));
            lines.add(Component.translatable("gui.vanillatalents.requires", name,
                    TalentRules.effectiveLevel(data, registry, p.nodeId()), p.level())
                    .withStyle(ChatFormatting.RED).getVisualOrderText());
        }
        return lines;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() == 0) {
            TalentNode node = nodeAt(event.x(), event.y());
            if (node != null) {
                ModNetwork.sendToServer(new C2SBuyNode(node.id()));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
