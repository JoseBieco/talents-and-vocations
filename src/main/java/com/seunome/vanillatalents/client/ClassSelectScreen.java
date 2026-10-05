package com.seunome.vanillatalents.client;

import com.seunome.vanillatalents.core.TalentRules;
import com.seunome.vanillatalents.core.TreeCategory;
import com.seunome.vanillatalents.network.C2SChangeClass;
import com.seunome.vanillatalents.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Lista as 5 classes. Primeira escolha: envia direto (gratuita). Troca: abre a confirmação de respec. */
public class ClassSelectScreen extends Screen {

    private final Screen parent;

    public ClassSelectScreen(Screen parent) {
        super(Component.translatable("gui.vanillatalents.select_class.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        String current = ClientTalentState.data().getCurrentClass();
        int y = height / 2 - 60;
        for (TreeCategory tree : TreeCategory.values()) {
            if (!tree.isClass()) continue;
            Button button = Button.builder(Component.translatable("vanillatalents.class." + tree.id()), b -> choose(tree.id()))
                    .bounds((width - 150) / 2, y, 150, 20).build();
            button.active = !tree.id().equals(current);
            addRenderableWidget(button);
            y += 24;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds((width - 150) / 2, y + 8, 150, 20).build());
    }

    private void choose(String classId) {
        if (TalentRules.NO_CLASS.equals(ClientTalentState.data().getCurrentClass())) {
            ModNetwork.sendToServer(new C2SChangeClass(classId));
            minecraft.gui.setScreen(parent);
        } else {
            minecraft.gui.setScreen(new ConfirmRespecScreen(parent, classId));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        g.centeredText(font, title, width / 2, height / 2 - 90, 0xFFFFFFFF);
        if (TalentRules.NO_CLASS.equals(ClientTalentState.data().getCurrentClass())) {
            g.centeredText(font, Component.translatable("gui.vanillatalents.select_class.first_free"), width / 2, height / 2 - 76, 0xFFAAAAAA);
        }
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
