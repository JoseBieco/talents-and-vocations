package com.seunome.vanillatalents.client;

import com.seunome.vanillatalents.VanillaTalents;
import com.seunome.vanillatalents.core.TalentRegistry;
import com.seunome.vanillatalents.data.TalentRegistries;
import com.seunome.vanillatalents.network.S2CProspectorHighlight;
import com.seunome.vanillatalents.network.S2CSyncDefinitions;
import com.seunome.vanillatalents.network.S2CSyncPlayer;

import java.util.ArrayList;
import java.util.List;

/** Handlers S2C. Classe separada para que o servidor dedicado nunca a carregue. */
public final class ClientPacketHandlers {

    private ClientPacketHandlers() {}

    public static void syncPlayer(S2CSyncPlayer msg) {
        ClientTalentState.update(msg.data());
    }

    public static void syncDefinitions(S2CSyncDefinitions msg) {
        List<String> errors = new ArrayList<>();
        TalentRegistries.setClient(TalentRegistry.build(msg.nodes(), errors));
        errors.forEach(e -> VanillaTalents.LOGGER.warn("[vanillatalents] Nó descartado no cliente: {}", e));
    }

    public static void prospectorHighlight(S2CProspectorHighlight msg) {
        // As partículas do Faro Mineral são desenhadas pela Task 12 (ClientEffects).
    }
}
