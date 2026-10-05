package com.seunome.vanillatalents.client.ui;

import com.seunome.vanillatalents.capability.PlayerSkillData;
import com.seunome.vanillatalents.client.ClientTalentState;
import com.seunome.vanillatalents.core.*;
import com.seunome.vanillatalents.data.TalentRegistries;
import com.seunome.vanillatalents.network.C2SBuyNode;
import com.seunome.vanillatalents.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/** Painel lateral: detalhes do nó selecionado (com o botão Comprar) ou o resumo da árvore. */
public final class NodeDetailPanel {

    private static final int PAD = 5;
    private static final int BUTTON_H = 20;
    private static final int COLOR_NAME = 0xFFFFFF55;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_MUTED = 0xFFE0E0E0;
    private static final int COLOR_MET = 0xFF55FF55;
    private static final int COLOR_UNMET = 0xFFFF5555;
    /** Largura reservada à direita para a barra de rolagem. */
    private static final int SCROLLBAR_W = 3;

    private final int x, y, w, h;
    private final Button buy;
    private @Nullable String shownId;
    /** Rolagem do texto (≤ 0) e as medidas do último quadro, usadas pela roda do mouse. */
    private int scroll, contentH, viewH;
    private boolean summaryShown;

    public NodeDetailPanel(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.buy = Button.builder(Component.translatable("gui.vanillatalents.buy"), b -> buyShown())
                .bounds(x + PAD, y + h - PAD - BUTTON_H, w - 2 * PAD, BUTTON_H).build();
        this.buy.visible = false;
    }

    public boolean contains(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Rola o texto do painel; {@code wheel > 0} sobe. */
    public void scroll(double wheel) {
        scroll = TalentScreenModel.wheelScroll(contentH, viewH, scroll, wheel);
    }

    /** Mantém a rolagem de um painel anterior (a tela recria o painel a cada sync). */
    public void keepScrollFrom(@Nullable NodeDetailPanel previous) {
        if (previous == null) return;
        shownId = previous.shownId;
        summaryShown = previous.summaryShown;
        scroll = previous.scroll;
        contentH = previous.contentH;
        viewH = previous.viewH;
    }

    /** Botão Comprar no rodapé do painel; a tela o registra como widget. Visível só com um nó selecionado. */
    public Button buyButton() {
        return buy;
    }

    private void buyShown() {
        if (shownId == null) return;
        if (TalentRules.canPurchase(ClientTalentState.data(), TalentRegistries.client(), shownId) == PurchaseResult.OK) {
            ModNetwork.sendToServer(new C2SBuyNode(shownId));
        }
    }

    public void render(GuiGraphicsExtractor g, @Nullable TalentNode selected, TreeCategory tree) {
        VanillaGui.insetPanel(g, x, y, w, h);
        Font font = Minecraft.getInstance().font;
        PlayerSkillData data = ClientTalentState.data();
        TalentRegistry registry = TalentRegistries.client();
        String id = selected == null ? null : selected.id();
        if (!java.util.Objects.equals(id, shownId) || summaryShown != (selected == null)) scroll = 0;
        shownId = id;
        summaryShown = selected == null;
        buy.visible = selected != null;

        if (selected == null) {
            buy.active = false;
            renderSummary(g, font, data, registry, tree);
            return;
        }

        PurchaseResult result = TalentRules.canPurchase(data, registry, selected.id());
        buy.active = result == PurchaseResult.OK;
        String reasonKey = TalentScreenModel.blockReasonKey(result);
        int reasonLines = reasonKey == null ? 0 : font.split(Component.translatable(reasonKey), w - 2 * PAD).size();
        int textBottom = buy.getY() - 2 - reasonLines * font.lineHeight;

        int top = y + PAD;
        g.enableScissor(x + 1, top - 1, x + w - 1, textBottom);
        int ty = top + scroll;
        ty = wrapped(g, font, Component.translatable(selected.nameKey()), ty, COLOR_NAME);
        ty = wrapped(g, font, Component.translatable(selected.descKey()), ty + 2, COLOR_MUTED);
        ty = wrapped(g, font, Component.translatable("gui.vanillatalents.level",
                TalentRules.effectiveLevel(data, registry, selected.id()), selected.maxLevel()), ty + 3, COLOR_TEXT);
        if (!selected.prerequisites().isEmpty()) {
            ty = wrapped(g, font, Component.translatable("gui.vanillatalents.detail.requirements"), ty + 3, COLOR_TEXT);
            for (Prerequisite p : selected.prerequisites()) {
                int current = TalentRules.effectiveLevel(data, registry, p.nodeId());
                boolean met = current >= p.level();
                Component name = registry.get(p.nodeId()).<Component>map(n -> Component.translatable(n.nameKey()))
                        .orElse(Component.literal(p.nodeId()));
                String key = met ? "gui.vanillatalents.detail.met" : "gui.vanillatalents.detail.unmet";
                ty = wrapped(g, font, Component.translatable(key, name, current, p.level()), ty, met ? COLOR_MET : COLOR_UNMET);
            }
        }
        ty = wrapped(g, font, Component.translatable("gui.vanillatalents.detail.cost"), ty + 3, COLOR_TEXT);
        g.disableScissor();
        measure(ty - scroll - top, textBottom - top);
        scrollbar(g, top, textBottom);

        if (reasonKey != null) wrapped(g, font, Component.translatable(reasonKey), textBottom, COLOR_UNMET);
    }

    private void renderSummary(GuiGraphicsExtractor g, Font font, PlayerSkillData data, TalentRegistry registry, TreeCategory tree) {
        TalentScreenModel.TreeSummary s = TalentScreenModel.treeSummary(data, registry, tree);
        int top = y + PAD, bottom = y + h - PAD;
        g.enableScissor(x + 1, top - 1, x + w - 1, bottom);
        int ty = top + scroll;
        ty = wrapped(g, font, Component.translatable("gui.vanillatalents.summary.spent", s.spent()), ty, COLOR_TEXT);
        ty = wrapped(g, font, Component.translatable("gui.vanillatalents.summary.nodes", s.maxedNodes(), s.nodeCount()), ty + 2, COLOR_TEXT);
        ty = wrapped(g, font, Component.translatable("gui.vanillatalents.summary.total",
                Math.max(0, s.totalPoints() - s.spent())), ty + 2, COLOR_TEXT);
        ty = wrapped(g, font, Component.translatable("gui.vanillatalents.summary.hint"), ty + 8, COLOR_MUTED);
        g.disableScissor();
        measure(ty - scroll - top, bottom - top);
        scrollbar(g, top, bottom);
    }

    /** Guarda as medidas do texto e reaplica o limite (o texto pode ter encolhido). */
    private void measure(int textH, int areaH) {
        contentH = textH;
        viewH = areaH;
        scroll = TalentScreenModel.clampScroll(contentH, viewH, scroll);
    }

    private void scrollbar(GuiGraphicsExtractor g, int top, int bottom) {
        VanillaGui.scrollbar(g, x + w - PAD + 1, top, bottom, TalentScreenModel.scrollThumb(contentH, viewH, scroll, bottom - top));
    }

    /** Escreve o texto quebrado na largura do painel e devolve o y da linha seguinte. */
    private int wrapped(GuiGraphicsExtractor g, Font font, Component text, int ty, int color) {
        for (FormattedCharSequence line : font.split(text, w - 2 * PAD - SCROLLBAR_W)) {
            g.text(font, line, x + PAD, ty, color, true);
            ty += font.lineHeight;
        }
        return ty;
    }
}
