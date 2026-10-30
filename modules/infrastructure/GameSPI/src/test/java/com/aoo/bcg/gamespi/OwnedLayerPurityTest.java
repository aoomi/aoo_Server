package com.aoo.bcg.gamespi;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OwnedLayerPurityTest {
    @Test void spiProductionSourcesHaveNoReverseDependencyOrReflectiveGameAccess() throws Exception {
        Path source = Path.of(System.getProperty("basedir", "."), "src/main/java");
        List<String> violations = new ArrayList<>();
        try (var files = Files.walk(source)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                int lineNo = 0;
                for (String line : Files.readAllLines(file)) {
                    lineNo++;
                    String trimmed = line.strip();
                    if (trimmed.startsWith("import ") && !trimmed.startsWith("import java.")
                            && !trimmed.startsWith("import javax.")
                            && !trimmed.startsWith("import com.aoo.bcg.gamespi."))
                        violations.add(file + ":" + lineNo + " reverse import " + trimmed);
                    if (line.contains("setAccessible(") || line.contains("getDeclaredField(")
                            || line.contains("Class.forName("))
                        violations.add(file + ":" + lineNo + " reflective access");
                }
            }
        }
        assertTrue(violations.isEmpty(), () -> String.join("\n", violations));
    }
}
