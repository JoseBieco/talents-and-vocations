package com.seunome.vanillatalents.core.formula;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

/** Fórmulas da árvore do Produtor. */
public final class FarmerFormulas {

    /** Deslocamento horizontal (x, z) em relação ao bloco colhido. */
    public record Offset(int dx, int dz) {}

    private FarmerFormulas() {}

    /** farmer_area_harvest: nível 1 = cruz (4 vizinhos); nível 2+ = 3×3 (8 vizinhos). */
    public static List<Offset> areaOffsets(int level) {
        List<Offset> offsets = new ArrayList<>();
        if (level <= 0) return offsets;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                boolean diagonal = dx != 0 && dz != 0;
                if (level == 1 && diagonal) continue;
                offsets.add(new Offset(dx, dz));
            }
        }
        return offsets;
    }

    /** farmer_breeding: espera para reproduzir de novo, em ticks. */
    public static int breedingCooldown(int baseTicks, int level, double perLevel) {
        return (int) Math.round(baseTicks * HookFormulas.reductionMultiplier(level, perLevel));
    }

    /** farmer_growth_aura: raio = base + nível (3/4/5). */
    public static int auraRadius(int level, int radiusBase) {
        return radiusBase + level;
    }

    /** A aura só funciona se o jogador se moveu nos últimos {@code idleTicks}. */
    public static boolean auraAllowed(long lastMoveTick, long now, int idleTicks) {
        return now - lastMoveTick <= idleTicks;
    }

    /** Quantos dos {@code candidates} recebem um tick extra: cada um rola {@code chance}, até {@code maxPerPulse}. */
    public static int auraPickCount(int candidates, double chance, DoubleSupplier random, int maxPerPulse) {
        int picked = 0;
        for (int i = 0; i < candidates && picked < maxPerPulse; i++) {
            if (random.getAsDouble() < chance) picked++;
        }
        return picked;
    }

    /** farmer_coop: ticks a descontar do {@code eggTime} de cada galinha a cada {@code intervalTicks}. */
    public static int coopExtraTicks(int intervalTicks, int level, double perLevel) {
        return (int) Math.round(intervalTicks * level * perLevel);
    }

    /** Nível máximo da composteira antes de ficar pronta (ComposterBlock.MAX_LEVEL). */
    public static final int COMPOSTER_MAX_LEVEL = 7;

    /**
     * farmer_compost: novo nível da composteira depois de um item, ou {@code -1} para não mexer.
     * Não mexe se a vanilla já subiu, se já está em 7 (ou pronta) ou se a rolagem falhou.
     */
    public static int compostNextLevel(int before, int after, boolean rollPassed) {
        if (after != before) return -1;
        if (before >= COMPOSTER_MAX_LEVEL) return -1;
        if (!rollPassed) return -1;
        return before + 1;
    }
}
