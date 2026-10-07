package com.josebieco.talentsvocations.core;

/** Leitura do estado de talentos de um jogador. */
public interface SkillView {
    /** Classe do espaço principal, ou {@link TalentRules#NO_CLASS}. */
    String primaryClass();

    /** Classe do espaço secundário, ou {@link TalentRules#NO_CLASS}. */
    String secondaryClass();

    /** Limite de classes da config (1 desliga o multiclasse). */
    int maxClasses();

    int availablePoints();

    int rawLevel(String nodeId);
}
