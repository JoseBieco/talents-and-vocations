package com.josebieco.talentsvocations.client;

import com.josebieco.talentsvocations.TalentsVocations;
import com.josebieco.talentsvocations.core.TalentRegistry;
import com.josebieco.talentsvocations.data.TalentRegistries;
import com.josebieco.talentsvocations.network.S2CEnchantInsight;
import com.josebieco.talentsvocations.network.S2CProspectorHighlight;
import com.josebieco.talentsvocations.network.S2CSyncDefinitions;
import com.josebieco.talentsvocations.network.S2CSyncPlayer;

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
        errors.forEach(e -> TalentsVocations.LOGGER.warn("[talentsvocations] Nó descartado no cliente: {}", e));
        ClientTalentState.notifyScreen();
    }

    public static void prospectorHighlight(S2CProspectorHighlight msg) {
        OreHighlights.show(msg.positions());
    }

    public static void enchantInsight(S2CEnchantInsight msg) {
        EnchantInsightClient.receive(msg);
    }
}
