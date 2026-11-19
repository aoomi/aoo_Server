import java.nio.file.Files;
import java.nio.file.Path;

public final class AooBrandBoundaryCheckTest {
    public static void main(String[] args) throws Exception {
        Path fixture = Files.createTempDirectory("aoo-brand-gate-");
        try {
            Path server = Files.createDirectories(fixture.resolve("Server"));
            Path client = Files.createDirectories(fixture.resolve("Client"));
            Files.createDirectories(server.resolve("server/App/src/main/java"));
            Files.writeString(server.resolve("server/App/src/main/java/Application.java"), "final class Application {}\n");
            assertExit(0, server, client, "clean runtime must pass");

            Files.createDirectories(server.resolve("engineering/audit/tool-inputs"));
            Files.writeString(server.resolve("engineering/audit/tool-inputs/generate_backend_game_catalog.rb"), "old = 'qh_reference'\n");
            Files.createDirectories(client.resolve("build-audit"));
            Files.writeString(client.resolve("build-audit/generated.js"), "const old = 'qh_bundle';\n");
            Files.createDirectories(client.resolve("build-v11"));
            Files.writeString(client.resolve("build-v11/generated.js"), "const old = 'qh_bundle';\n");
            Files.createDirectories(server.resolve("engineering/audit/tool-inputs/legacy-isolation"));
            Files.writeString(server.resolve("engineering/audit/tool-inputs/legacy-isolation/gate.py"), "fixture = 'qh_reference'\n");
            Files.createDirectories(client.resolve("work/preview-snapshots/.chrome-volatile"));
            Files.writeString(client.resolve("work/preview-snapshots/.chrome-volatile/runtime.js"),
                    "const old = 'qh_runtime';\n");
            assertExit(0, server, client, "isolated audit evidence must not become a runtime violation");

            String source = Files.readString(gateMainPath());
            if (!source.contains("preVisitDirectory") || !source.contains("FileVisitResult.SKIP_SUBTREE")) {
                throw new AssertionError("excluded directories must be skipped before traversal enters the subtree");
            }

            Files.writeString(server.resolve("server/App/src/main/java/Application.java"),
                    "final class Application { String runtimeName = \"qh_service\"; }\n");
            assertExit(1, server, client, "runtime legacy brand must fail the gate");
        } finally {
            deleteTree(fixture);
        }

        Path fixture2 = Files.createTempDirectory("aoo-brand-gate-narrow-");
        try {
            Path server2 = Files.createDirectories(fixture2.resolve("Server"));
            Path client2 = Files.createDirectories(fixture2.resolve("Client"));
            Path migrations = Files.createDirectories(server2.resolve("database/migrations"));
            Files.writeString(migrations.resolve("V20260821_01__baseline_legacy_player_table.sql"),
                    "legacy qh_reference 情怀\n");
            assertExit(0, server2, client2, "exact exempt path must pass");

            Files.writeString(migrations.resolve("V20260821_02__baseline_legacy_player_table.sql"),
                    "legacy qh_reference 情怀\n");
            assertExit(1, server2, client2, "sibling migration name must still fail");

            Path siblingMigrations = Files.createDirectories(server2.resolve("database/migrations2"));
            Files.writeString(siblingMigrations.resolve("V20260821_01__baseline_legacy_player_table.sql"),
                    "legacy qh_reference 情怀\n");
            assertExit(1, server2, client2, "sibling directory must still fail");

            Files.writeString(server2.resolve("database/V20260821_01__baseline_legacy_player_table.sql"),
                    "legacy qh_reference 情怀\n");
            assertExit(1, server2, client2, "top-level directory variant must still fail");
        } finally {
            deleteTree(fixture2);
        }
    }

    private static void assertExit(int expected, Path server, Path client, String message) throws Exception {
        // Same origin as the check's rootA (user.dir): works with CWD=Aoo (direct run) and CWD=Server (Maven exec).
        Path repo = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        final Path gateMainPath = gateMainPath();
        Process process = new ProcessBuilder(javaBinary(),
                gateMainPath.toString(),
                server.toString(), client.toString()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        int actual = process.waitFor();
        if (actual != expected) {
            throw new AssertionError(message + ": exit=" + actual + " output=" + output);
        }
    }

    private static Path gateMainPath() {
        Path repo = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path gateMain = repo.resolve("server/engineering/audit/tool-inputs/AooBrandBoundaryCheck.java");
        return Files.isRegularFile(gateMain) ? gateMain : repo.resolve("engineering/audit/tool-inputs/AooBrandBoundaryCheck.java");
    }

    private static String javaBinary() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    private static void deleteTree(Path root) throws Exception {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
