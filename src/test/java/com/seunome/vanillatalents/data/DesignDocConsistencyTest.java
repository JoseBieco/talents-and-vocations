package com.seunome.vanillatalents.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.seunome.vanillatalents.core.Prerequisite;
import com.seunome.vanillatalents.core.TalentNode;
import com.seunome.vanillatalents.core.TalentRegistry;
import com.seunome.vanillatalents.core.TreeCategory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Confere os JSON de skills contra as tabelas de docs/arvores e as traduções. */
class DesignDocConsistencyTest {

    static final Path SKILLS = Path.of("src/main/resources/data/vanillatalents/skills");
    static final Path LANG = Path.of("src/main/resources/assets/vanillatalents/lang");
    static final Path DOCS = Path.of("docs/arvores");

    record Expected(int nodes, int points) {}

    static final Map<TreeCategory, Expected> EXPECTED = Map.of(
            TreeCategory.COMMON, new Expected(17, 56), TreeCategory.MINER, new Expected(16, 47),
            TreeCategory.FARMER, new Expected(16, 46), TreeCategory.EXPLORER, new Expected(16, 42),
            TreeCategory.WARRIOR, new Expected(16, 46), TreeCategory.ARCHER, new Expected(16, 46),
            TreeCategory.ANGLER, new Expected(12, 32),
            TreeCategory.TAMER, new Expected(12, 31));

    static int totalExpectedNodes() {
        return EXPECTED.values().stream().mapToInt(Expected::nodes).sum();
    }

    record DocRow(String id, String name, int maxLevel, Set<Prerequisite> prerequisites) {}

    static List<TalentNode> nodes;
    static TalentRegistry registry;
    static List<String> errors;

    @BeforeAll
    static void load() throws IOException {
        nodes = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SKILLS)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".json")).sorted().toList()) {
                var json = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8));
                TalentNode n = TalentNodeCodec.CODEC.parse(JsonOps.INSTANCE, json)
                        .getOrThrow(msg -> new AssertionError(p + ": " + msg));
                assertEquals(n.id() + ".json", p.getFileName().toString(), "nome do arquivo = id");
                assertEquals(n.tree().id(), p.getParent().getFileName().toString(), "pasta = treeCategory");
                nodes.add(n);
            }
        }
        errors = new ArrayList<>();
        registry = TalentRegistry.build(nodes, errors);
    }

    @Test
    void allNodesValid() {
        assertTrue(errors.isEmpty(), errors.toString());
        assertEquals(totalExpectedNodes(), registry.size());
    }

    @Test
    void expectedNodesPerTreeAndPointTotals() {
        for (TreeCategory tree : TreeCategory.values()) {
            List<TalentNode> t = registry.tree(tree);
            assertEquals(EXPECTED.get(tree).nodes(), t.size(), tree.id());
            assertEquals(EXPECTED.get(tree).points(), t.stream().mapToInt(n -> n.maxLevel() * n.cost()).sum(), tree.id());
        }
    }

    @Test
    void nodesMatchDesignDocTables() throws IOException {
        Map<String, DocRow> doc = readDocRows();
        assertEquals(totalExpectedNodes(), doc.size(), "linhas de tabela em docs/arvores");
        assertEquals(doc.keySet(), new TreeSet<>(registry.all().stream().map(TalentNode::id).toList()));
        for (TalentNode n : registry.all()) {
            DocRow row = doc.get(n.id());
            assertEquals(row.maxLevel(), n.maxLevel(), n.id() + " maxLevel");
            assertEquals(row.prerequisites(), new HashSet<>(n.prerequisites()), n.id() + " prerequisites");
        }
    }

    @Test
    void translationKeysExistAndPtBrNamesMatchDocs() throws IOException {
        Map<String, DocRow> doc = readDocRows();
        for (String lang : List.of("pt_br", "en_us")) {
            JsonObject json = JsonParser.parseString(Files.readString(LANG.resolve(lang + ".json"), StandardCharsets.UTF_8))
                    .getAsJsonObject();
            for (TalentNode n : registry.all()) {
                assertEquals("talent.vanillatalents." + n.id() + ".name", n.nameKey());
                assertEquals("talent.vanillatalents." + n.id() + ".desc", n.descKey());
                assertTrue(json.has(n.nameKey()), lang + " sem " + n.nameKey());
                assertTrue(json.has(n.descKey()), lang + " sem " + n.descKey());
                if (lang.equals("pt_br")) {
                    assertEquals(doc.get(n.id()).name(), json.get(n.nameKey()).getAsString(), n.id());
                }
            }
        }
    }

    @Test
    void capstoneIsBelowEveryOtherNode() {
        for (TreeCategory tree : TreeCategory.values()) {
            List<TalentNode> t = registry.tree(tree);
            TalentNode last = registry.capstone(tree).orElseThrow();
            assertEquals(3, last.prerequisites().size(), tree.id() + " capstone exige os três ramos");
            assertEquals(0, last.position().x(), tree.id() + " capstone x");
            assertTrue(t.stream().filter(n -> n != last && !n.conditions().contains(TalentNode.CONDITION_PRIMARY_CAPSTONE)).allMatch(n -> n.position().y() < last.position().y()));
        }
    }

    @Test
    void attributeNodesExistAndHaveTheirValues() {
        for (String id : com.seunome.vanillatalents.core.AttributeBonuses.NODE_IDS) {
            assertTrue(registry.get(id).isPresent(), id);
        }
        for (TreeCategory tree : TreeCategory.values()) {
            if (!tree.isClass()) continue;
            com.seunome.vanillatalents.core.SkillView maxed = new com.seunome.vanillatalents.core.SkillView() {
                public String primaryClass() { return tree.id(); }
                public String secondaryClass() { return com.seunome.vanillatalents.core.TalentRules.NO_CLASS; }
                public int maxClasses() { return 2; }
                public int availablePoints() { return 0; }
                public int rawLevel(String nodeId) { return 10; }
            };
            var ctx = new com.seunome.vanillatalents.core.AttributeBonuses.Context(false, false, true, 0);
            assertDoesNotThrow(() -> com.seunome.vanillatalents.core.AttributeBonuses.compute(maxed, registry, ctx), tree.id());
        }
    }

    private static final Pattern ROW = Pattern.compile("^\\| `([a-z_]+)` \\| ([^|]+?) \\| [^|]* \\| (\\d+) \\| ([^|]*) \\|$");
    private static final Pattern PREREQ = Pattern.compile("`([a-z_]+)` (?:≥|=) (\\d+)");

    static Map<String, DocRow> readDocRows() throws IOException {
        Map<String, DocRow> rows = new TreeMap<>();
        try (Stream<Path> files = Files.list(DOCS)) {
            for (Path p : files.filter(f -> f.getFileName().toString().matches("0\\d_.*\\.md")).toList()) {
                for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                    Matcher m = ROW.matcher(line.strip());
                    if (!m.matches()) continue;
                    Set<Prerequisite> prereqs = new HashSet<>();
                    Matcher pm = PREREQ.matcher(m.group(4));
                    while (pm.find()) prereqs.add(new Prerequisite(pm.group(1), Integer.parseInt(pm.group(2))));
                    String name = m.group(2).replace("★", "").strip();
                    rows.put(m.group(1), new DocRow(m.group(1), name, Integer.parseInt(m.group(3)), prereqs));
                }
            }
        }
        return rows;
    }

    @Test
    void classSummaries_matchDesignTotals() {
        var miner = com.seunome.vanillatalents.core.TalentScreenModel.classSummary(registry, TreeCategory.MINER);
        assertEquals("miner_haste", miner.rootId());
        assertEquals("miner_vein", miner.capstoneId());
        assertEquals(EXPECTED.get(TreeCategory.MINER).nodes(), miner.nodeCount());
        assertEquals(EXPECTED.get(TreeCategory.MINER).points(), miner.totalPoints());
        EXPECTED.forEach((tree, expected) -> {
            var summary = com.seunome.vanillatalents.core.TalentScreenModel.classSummary(registry, tree);
            assertEquals(expected.nodes(), summary.nodeCount(), tree.name());
            assertEquals(expected.points(), summary.totalPoints(), tree.name());
        });
        assertEquals("common_second_wind",
                com.seunome.vanillatalents.core.TalentScreenModel.classSummary(registry, TreeCategory.COMMON).capstoneId());
    }
}
