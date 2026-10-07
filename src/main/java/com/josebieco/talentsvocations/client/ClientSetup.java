package com.josebieco.talentsvocations.client;

import com.josebieco.talentsvocations.effect.Talents;

/** Inicialização exclusiva do cliente; só é carregada quando {@code FMLEnvironment.dist.isClient()}. */
public final class ClientSetup {

    private ClientSetup() {}

    public static void init() {
        KeyBindings.register();
        ClientEffects.register();
        OreHighlights.register();
        EnchantInsightClient.register();
        Talents.setClientView(ClientTalentState::data);
    }
}
