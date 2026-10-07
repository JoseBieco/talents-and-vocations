package com.seunome.vanillatalents.core;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Impede que efeitos que disparam o próprio evento (Veio, Foice Larga, Golpe Amplo, Colheita Perpétua,
 * Sangue Forte) se chamem de novo em cascata. Usado só na thread do servidor.
 */
public final class RecursionGuard {

    public static final RecursionGuard SERVER = new RecursionGuard();

    private final Set<String> active = new HashSet<>();

    private static String id(UUID player, String key) {
        return player + "/" + key;
    }

    /** @return false se o par (jogador, chave) já está ativo; nesse caso nada muda. */
    public boolean enter(UUID player, String key) {
        return active.add(id(player, key));
    }

    public void exit(UUID player, String key) {
        active.remove(id(player, key));
    }

    public boolean isActive(UUID player, String key) {
        return active.contains(id(player, key));
    }

    /** Executa {@code action} se não estiver ativo, garantindo {@link #exit} mesmo com exceção. */
    public boolean runGuarded(UUID player, String key, Runnable action) {
        if (!enter(player, key)) return false;
        try {
            action.run();
        } finally {
            exit(player, key);
        }
        return true;
    }
}
