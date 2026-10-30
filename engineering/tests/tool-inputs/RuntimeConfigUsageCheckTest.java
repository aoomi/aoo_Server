import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public final class RuntimeConfigUsageCheckTest {
    private static final String[] NAMES = {
            "KEY_01", "KEY_02", "KEY_03", "KEY_04", "KEY_05", "KEY_06", "KEY_07", "KEY_08",
            "KEY_09", "KEY_10", "KEY_11", "KEY_12", "KEY_13", "KEY_14", "KEY_15", "KEY_16"
    };

    public static void main(String[] args) throws Exception {
        Path fixture = Files.createTempDirectory("runtime-config-usage-");
        try {
            Path catalog = fixture.resolve("modules/common/CommonRoom/src/main/java/com/aoo/bcg/common/config/RuntimeConfigKey.java");
            Path application = fixture.resolve("modules/App/src/main/java/com/aoo/bcg/app/Application.java");
            Files.createDirectories(catalog.getParent());
            Files.createDirectories(application.getParent());
            Files.writeString(catalog, catalogSource());

            Files.writeString(application, usageSource(true, false));
            assertExit(0, fixture, "Runtime config usage check passed: 16 keys.",
                    "protocol and split literals without lookup context must pass");

            Files.writeString(application, usageSource(true, true));
            assertExit(1, fixture, "uncatalogued runtime key literal: aoo.brand-new",
                    "System.getProperty literal must fail");
            assertAudit(fixture, "uncatalogued runtime key literal: admin.sneaky.key");
            assertAudit(fixture, "uncatalogued runtime key literal: feature.sneaky.switch");
            assertAudit(fixture, "uncatalogued runtime key literal: aoo.v2");

            Files.writeString(application, usageSource(false, false));
            assertExit(1, fixture, "catalog key has no production read point: existing.key-16",
                    "direction 1 catalog usage check must remain unchanged");
        } finally {
            deleteTree(fixture);
        }
        System.out.println("Runtime config usage check test passed.");
    }

    private static String catalogSource() {
        StringBuilder source = new StringBuilder("public enum RuntimeConfigKey {\n");
        for (int index = 0; index < NAMES.length; index++) {
            source.append("    ").append(NAMES[index]).append("(\"existing.key-")
                    .append(index + 1).append("\")").append(index + 1 == NAMES.length ? ";\n" : ",\n");
        }
        return source.append("}\n").toString();
    }

    private static String usageSource(boolean includeLastCatalogUse, boolean includeViolations) {
        StringBuilder source = new StringBuilder("package com.aoo.bcg.app;\nfinal class Application {\n");
        int limit = includeLastCatalogUse ? NAMES.length : NAMES.length - 1;
        for (int index = 0; index < limit; index++) {
            source.append("  Object value").append(index).append(" = RuntimeConfigKey.").append(NAMES[index]).append(";\n");
        }
        source.append("  String protocol = \"aoo.v2\";\n")
                .append("  String[] segments = \"payload\".split(\"aoo.ticket.\");\n");
        if (includeViolations) {
            source.append("  String direct = System.getProperty(\"aoo.brand-new\");\n")
                    .append("  String notAProtocol = System.getProperty(\"aoo.v2\");\n")
                    .append("  String environment = env.getProperty(\"admin.sneaky.key\");\n")
                    .append("  @Value(\"${feature.sneaky.switch}\") String injected;\n");
        }
        return source.append("}\n").toString();
    }

    private static void assertExit(int expected, Path root, String expectedText, String message) throws Exception {
        Path repo = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path gateMain = repo.resolve("Server/engineering/audit/tool-inputs/RuntimeConfigUsageCheck.java");
        if (!Files.isRegularFile(gateMain)) {
            gateMain = repo.resolve("engineering/audit/tool-inputs/RuntimeConfigUsageCheck.java");
        }
        Process process = new ProcessBuilder(javaBinary(), gateMain.toString(), root.toString())
                .redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int actual = process.waitFor();
        String audit = Files.readString(root.resolve("build-only/audit/dead10-runtime-config-usage.txt"));
        if (actual != expected || !(output + audit).contains(expectedText)) {
            throw new AssertionError(message + ": exit=" + actual + " output=" + output + " audit=" + audit);
        }
    }

    private static void assertAudit(Path root, String expectedText) throws Exception {
        String audit = Files.readString(root.resolve("build-only/audit/dead10-runtime-config-usage.txt"));
        if (!audit.contains(expectedText)) {
            throw new AssertionError("missing audit line: " + expectedText + " audit=" + audit);
        }
    }

    private static String javaBinary() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    private static void deleteTree(Path root) throws Exception {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
