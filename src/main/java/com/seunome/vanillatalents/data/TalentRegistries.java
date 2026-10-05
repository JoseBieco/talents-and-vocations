package com.seunome.vanillatalents.data;

import com.seunome.vanillatalents.core.TalentRegistry;

/** Registro ativo de cada lado: o servidor carrega do datapack; o cliente recebe por S2CSyncDefinitions. */
public final class TalentRegistries {

    private static volatile TalentRegistry server = TalentRegistry.empty();
    private static volatile TalentRegistry client = TalentRegistry.empty();

    private TalentRegistries() {}

    public static TalentRegistry server() {
        return server;
    }

    public static void setServer(TalentRegistry registry) {
        server = registry;
    }

    public static TalentRegistry client() {
        return client;
    }

    public static void setClient(TalentRegistry registry) {
        client = registry;
    }
}
