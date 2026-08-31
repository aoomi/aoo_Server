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

            Files.createDirectories(server.resolve("tools"));
            Files.writeString(server.resolve("tools/generate_backend_game_catalog.rb"), "old = 'qh_reference'\n");
            Files.createDirectories(client.resolve("build-audit"));
            Files.writeString(client.resolve("build-audit/generated.js"), "const old = 'qh_bundle';\n");
            Files.createDirectories(client.resolve("build-v11"));
            Files.writeString(client.resolve("build-v11/generated.js"), "const old = 'qh_bundle';\n");
            Files.createDirectories(server.resolve("tools/legacy-isolation"));
            Files.writeString(server.resolve("tools/legacy-isolation/gate.py"), "fixture = 'qh_reference'\n");
            assertExit(0, server, client, "isolated audit evidence must not become a runtime violation");

            Files.writeString(server.resolve("server/App/src/main/java/Application.java"),
                    "final class Application { String runtimeName = \"qh_service\"; }\n");
            assertExit(1, server, client, "runtime legacy brand must fail the gate");
        } finally {
            deleteTree(fixture);
        }
    }

    private static void assertExit(int expected, Path server, Path client, String message) throws Exception {
        Process process = new ProcessBuilder(javaBinary(),
                Path.of("tools/AooBrandBoundaryCheck.java").toAbsolutePath().toString(),
                server.toString(), client.toString()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        int actual = process.waitFor();
        if (actual != expected) {
            throw new AssertionError(message + ": exit=" + actual + " output=" + output);
        }
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
