package com.seunome.vanillatalents.client;

import com.seunome.vanillatalents.effect.Talents;

/** Inicialização exclusiva do cliente; só é carregada quando {@code FMLEnvironment.dist.isClient()}. */
public final class ClientSetup {

    private ClientSetup() {}

    public static void init() {
        KeyBindings.register();
        ClientEffects.register();
        Talents.setClientView(ClientTalentState::data);
    }
}
