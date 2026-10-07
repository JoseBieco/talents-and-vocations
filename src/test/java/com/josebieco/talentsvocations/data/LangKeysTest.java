package com.josebieco.talentsvocations.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.josebieco.talentsvocations.core.TreeCategory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Toda chave de tradução literal usada no código Java existe em pt_br e en_us. */
class LangKeysTest {

    static final Path SOURCES = Path.of("src/main/java");
    static final Path LANG = Path.of("src/main/resources/assets/talentsvocations/lang");
    static final Pattern KEY = Pattern.compile("\"((?:gui|key)\\.talentsvocations\\.[a-z_.]+|key\\.category\\.talentsvocations\\.[a-z_]+)\"");

    static Set<String> keysInCode() throws IOException {
        Set<String> keys = new TreeSet<>();
        try (Stream<Path> files = Files.walk(SOURCES)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                Matcher m = KEY.matcher(Files.readString(p, StandardCharsets.UTF_8));
                while (m.find()) keys.add(m.group(1));
            }
        }
        keys.add("key.category.talentsvocations.main");
        for (TreeCategory tree : TreeCategory.values()) {
            if (tree.isClass()) {
                keys.add("talentsvocations.class." + tree.id());
                keys.add("talentsvocations.class." + tree.id() + ".desc");
            }
        }
        return keys;
    }

    @Test
    void codeUsesSomeGuiKeys() throws IOException {
        assertTrue(keysInCode().contains("gui.talentsvocations.title"));
        assertTrue(keysInCode().contains("key.talentsvocations.open"));
    }

    @Test
    void everyKeyExistsInBothLanguages() throws IOException {
        Set<String> keys = keysInCode();
        for (String lang : List.of("pt_br", "en_us")) {
            JsonObject json = JsonParser.parseString(Files.readString(LANG.resolve(lang + ".json"), StandardCharsets.UTF_8))
                    .getAsJsonObject();
            Set<String> missing = new TreeSet<>(keys);
            missing.removeIf(json::has);
            assertTrue(missing.isEmpty(), lang + " sem: " + missing);
        }
    }
}
