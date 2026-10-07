package com.seunome.vanillatalents.core.formula;

/** Vizinhança de blocos abstrata, para testar buscas (Veio) sem um mundo. */
public interface BlockGraph<P> {
    Iterable<P> neighbors(P pos);

    /** O bloco em {@code pos} faz parte do veio (mesmo tipo do bloco inicial). */
    boolean matches(P pos);
}
