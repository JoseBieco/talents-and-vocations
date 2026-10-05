package com.seunome.vanillatalents.client;

import com.seunome.vanillatalents.capability.PlayerSkillData;
import com.seunome.vanillatalents.core.TalentScreenModel;
import com.seunome.vanillatalents.network.C2SChangeClass;
import com.seunome.vanillatalents.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Confirmação da troca de classe com a prévia de taxa, PT gastos e PT devolvidos. */
public class ConfirmRespecScreen extends ConfirmScreen {

    public ConfirmRespecScreen(Screen parent, String newClass) {
        super(confirmed -> {
            if (confirmed) ModNetwork.sendToServer(new C2SChangeClass(newClass));
            Minecraft.getInstance().gui.setScreen(parent);
        }, Component.translatable("gui.vanillatalents.respec.title",
                Component.translatable("vanillatalents.class." + newClass)), message());
    }

    private static Component message() {
        PlayerSkillData data = ClientTalentState.data();
        var preview = TalentScreenModel.respecPreview(data.getUnlockedNodes(), data.getCurrentClass(), ClientTalentState.economy());
        MutableComponent text = Component.translatable("gui.vanillatalents.respec.message",
                preview.feeLevels(), preview.spent(), preview.refund());
        var player = Minecraft.getInstance().player;
        if (player != null && player.experienceLevel < preview.feeLevels()) {
            text.append("\n").append(Component.translatable("gui.vanillatalents.respec.not_enough", preview.feeLevels())
                    .withStyle(ChatFormatting.RED));
        }
        return text;
    }
}
