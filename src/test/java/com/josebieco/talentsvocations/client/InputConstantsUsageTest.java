package com.josebieco.talentsvocations.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * No MC 26.x os botões do mouse seguem o SDL3 (esquerdo = 1, não 0). Comparar com número literal quebrou o
 * clique nos nós; o código do cliente deve usar InputConstants.MOUSE_BUTTON_*.
 */
class InputConstantsUsageTest {

    static final Path CLIENT = Path.of("src/main/java/com/josebieco/talentsvocations/client");
    static final Pattern LITERAL_BUTTON = Pattern.compile("button\\(\\)\\s*[=!]=\\s*\\d");

    @Test
    void noLiteralMouseButtonNumbers() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(CLIENT)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    if (LITERAL_BUTTON.matcher(lines.get(i)).find()) offenders.add(p.getFileName() + ":" + (i + 1));
                }
            }
        }
        assertTrue(offenders.isEmpty(), "use InputConstants.MOUSE_BUTTON_*: " + offenders);
    }
}
