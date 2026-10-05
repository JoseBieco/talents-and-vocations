package com.seunome.vanillatalents.core;

/** Leitura do estado de talentos de um jogador. */
public interface SkillView {
    String currentClass();

    int availablePoints();

    int rawLevel(String nodeId);
}
