package com.aoo.bcg.common;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OwnedLayerPurityTest {
    private static final List<String> CONCRETE_GAMES = List.of(
            "cdxzmj", "scjymj", "xuezhan", "njpdk", "xcpdk", "paodekuai");

    @Test void commonProductionSourcesHaveNoConcreteGameReverseDependencyOrReflection() throws Exception {
        Path source = Path.of(System.getProperty("basedir", "."), "src/main/java");
        List<String> violations = new ArrayList<>();
        try (var files = Files.walk(source)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                int lineNo = 0;
                for (String line : Files.readAllLines(file)) {
                    lineNo++;
                    String trimmed = line.strip();
                    String lower = line.toLowerCase(Locale.ROOT);
                    if (trimmed.startsWith("import ") && !allowedImport(trimmed))
                        violations.add(file + ":" + lineNo + " reverse import " + trimmed);
                    if (CONCRETE_GAMES.stream().anyMatch(lower::contains))
                        violations.add(file + ":" + lineNo + " concrete game reference");
                    if (line.contains("setAccessible(") || line.contains("getDeclaredField(")
                            || line.contains("Class.forName("))
                        violations.add(file + ":" + lineNo + " reflective access");
                }
            }
        }
        assertTrue(violations.isEmpty(), () -> String.join("\n", violations));
    }

    private static boolean allowedImport(String value) {
        return value.startsWith("import java.") || value.startsWith("import javax.")
                || value.startsWith("import com.aoo.bcg.common.")
                || value.startsWith("import com.aoo.bcg.gamespi.")
                || value.startsWith("import com.fasterxml.jackson.")
                || value.startsWith("import org.slf4j.")
                || value.startsWith("import org.apache.rocketmq.");
    }
}
