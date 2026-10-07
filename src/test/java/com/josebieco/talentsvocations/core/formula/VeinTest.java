package com.josebieco.talentsvocations.core.formula;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class VeinTest {

    record P(int x, int y, int z) {}

    /** Grade 3D: posições em {@code ore} são do mesmo minério; vizinhança de 6 faces. */
    static BlockGraph<P> graph(Set<P> ore) {
        return new BlockGraph<>() {
            @Override
            public Iterable<P> neighbors(P p) {
                return List.of(new P(p.x + 1, p.y, p.z), new P(p.x - 1, p.y, p.z), new P(p.x, p.y + 1, p.z),
                        new P(p.x, p.y - 1, p.z), new P(p.x, p.y, p.z + 1), new P(p.x, p.y, p.z - 1));
            }

            @Override
            public boolean matches(P p) {
                return ore.contains(p);
            }
        };
    }

    @Test
    void veinLimit_isFourPerLevel() {
        assertEquals(4, MinerFormulas.veinLimit(1, 4));
        assertEquals(8, MinerFormulas.veinLimit(2, 4));
        assertEquals(12, MinerFormulas.veinLimit(3, 4));
    }

    @Test
    void collect_excludesStartAndRespectsLimit() {
        Set<P> line = Set.of(new P(0, 0, 0), new P(1, 0, 0), new P(2, 0, 0), new P(3, 0, 0), new P(4, 0, 0), new P(5, 0, 0));
        List<P> found = MinerFormulas.veinCollect(new P(0, 0, 0), graph(line), 3);
        assertEquals(List.of(new P(1, 0, 0), new P(2, 0, 0), new P(3, 0, 0)), found);
    }

    @Test
    void collect_onlyFollowsConnectedMatchingBlocks() {
        // (2,0,0) está isolado por um bloco que não é minério em (1,0,0)
        Set<P> ore = Set.of(new P(0, 0, 0), new P(0, 1, 0), new P(2, 0, 0));
        List<P> found = MinerFormulas.veinCollect(new P(0, 0, 0), graph(ore), 12);
        assertEquals(List.of(new P(0, 1, 0)), found);
    }

    @Test
    void collect_isBreadthFirst() {
        // estrela: vizinhos diretos primeiro, depois os de distância 2
        Set<P> ore = Set.of(new P(0, 0, 0), new P(1, 0, 0), new P(2, 0, 0), new P(-1, 0, 0), new P(-2, 0, 0));
        List<P> found = new ArrayList<>(MinerFormulas.veinCollect(new P(0, 0, 0), graph(ore), 12));
        assertEquals(Set.of(new P(1, 0, 0), new P(-1, 0, 0)), Set.copyOf(found.subList(0, 2)));
        assertEquals(Set.of(new P(2, 0, 0), new P(-2, 0, 0)), Set.copyOf(found.subList(2, 4)));
    }

    @Test
    void collect_zeroLimitFindsNothing() {
        assertTrue(MinerFormulas.veinCollect(new P(0, 0, 0), graph(Set.of(new P(0, 0, 0), new P(1, 0, 0))), 0).isEmpty());
    }
}
