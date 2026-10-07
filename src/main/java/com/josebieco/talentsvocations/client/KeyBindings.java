package com.josebieco.talentsvocations.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.josebieco.talentsvocations.TalentsVocations;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;

/** Tecla única do mod (padrão K) que abre a tela de talentos. */
public final class KeyBindings {

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(TalentsVocations.MODID, "main"));

    public static final KeyMapping OPEN_TALENTS = new KeyMapping("key.talentsvocations.open", InputConstants.KEY_K, CATEGORY);

    private KeyBindings() {}

    public static void register() {
        RegisterKeyMappingsEvent.BUS.addListener(event -> event.register(OPEN_TALENTS));
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> onClientTick());
    }

    private static void onClientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN_TALENTS.consumeClick()) {
            if (minecraft.player != null && minecraft.gui.screen() == null) {
                minecraft.gui.setScreen(new TalentScreen());
            }
        }
    }
}
