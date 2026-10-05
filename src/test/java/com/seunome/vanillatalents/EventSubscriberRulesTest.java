package com.seunome.vanillatalents;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O EventBus 7 recusa a classe inteira (mod quebra no carregamento) se uma classe {@code @Mod.EventBusSubscriber}
 * tiver um método estático que recebe um evento sem {@code @SubscribeEvent}. Helpers não podem receber o evento.
 */
class EventSubscriberRulesTest {

    static final Path SOURCES = Path.of("src/main/java");
    static final Pattern STATIC_METHOD = Pattern.compile(
            "(@SubscribeEvent\\s+)?(?:public|private|protected)?\\s*static\\s+[\\w<>\\[\\]?, ]+\\s+(\\w+)\\s*\\(\\s*([\\w.]+)\\s+\\w+");

    @Test
    void staticMethodsTakingEventsAreAnnotated() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SOURCES)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String src = Files.readString(p, StandardCharsets.UTF_8);
                if (!src.contains("@Mod.EventBusSubscriber")) continue;
                Matcher m = STATIC_METHOD.matcher(src);
                while (m.find()) {
                    boolean annotated = m.group(1) != null;
                    String firstParamType = m.group(3);
                    if (firstParamType.endsWith("Event") && !annotated) offenders.add(p.getFileName() + "#" + m.group(2));
                }
            }
        }
        assertTrue(offenders.isEmpty(), "métodos estáticos com evento sem @SubscribeEvent: " + offenders);
    }
}
