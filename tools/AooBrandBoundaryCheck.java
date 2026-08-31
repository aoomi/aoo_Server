import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class AooBrandBoundaryCheck {
    private static final Pattern LEGACY = Pattern.compile(
            "(?i)(?<![a-z0-9])qh(?=\\b|[_./:\\-])|情怀");
    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            "java", "xml", "yml", "yaml", "properties", "json", "ts", "js",
            "prefab", "scene", "meta", "atlas", "fnt", "txt", "csv", "tsv",
            "sql", "sh", "rb", "py", "plist", "project");
    private static final Set<String> EXCLUDED_SEGMENTS = Set.of(
            ".git", "node_modules", "library", "temp", "build", "target", "logs",
            "build-audit", "work", "third-party", "archived-common-room-folders",
            "archived-direct-game-folders", "archived-game-folders",
            "archived-orientation-folders", "archived-result-layouts", "Archive",
            "original", "backups", "reference", "legacy-isolation");
    /**
     * Offline migration/audit tools may name isolated 2.2.2 source roots in
     * order to compare them. They are not packaged runtime code. Keep this
     * exception narrow: ordinary tools and every runtime source remain gated.
     */
    private static final Set<String> LEGACY_REFERENCE_TOOLS = Set.of(
            "audit_rule_fact_equivalence.rb",
            "audit_legacy_entry_equivalence.rb",
            "inventory_build_entries.sh",
            "audit_side_effect_equivalence.rb",
            "generate_source_root_baseline.rb",
            "audit_precondition_equivalence.rb",
            "audit_historical_data_equivalence.rb",
            "audit-dbreal01-original-isolation.rb",
            "audit-dbreal02-backup-security.rb",
            "audit-dbreal03-zle-map.rb",
            "audit-dbreal04-log-map.rb",
            "audit-dbreal05-game-map.rb",
            "audit-dbreal09-column-ledger.rb",
            "audit-third03-necessity.rb",
            "audit-third04-security-path.rb",
            "audit-third05-modification-dossier.rb",
            "audit-third07-licenses.rb",
            "audit-third08-sbom.rb",
            "stage-native-prefab-2.2.2.rb");
    private static final Set<String> NON_RUNTIME_METADATA = Set.of(
            "tools/generate_backend_game_catalog.rb",
            "tools/generate_game_classification.rb",
            "tools/legacy-protocol-ledger/build_ledger.rb",
            "config/large-file-declarations.json",
            "scripts/audit-large01-sql-archives.rb",
            "scripts/audit-creal06-build-template.rb",
            "scripts/audit-carch06-migration-archive.rb",
            "scripts/audit-ccfg02-10-config-integrity.rb",
            "scripts/audit-creal04-web-build.rb");

    private AooBrandBoundaryCheck() {
    }

    public static void main(String[] args) throws Exception {
        Path server = Paths.get(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();
        Path client = args.length > 1
                ? Paths.get(args[1]).toAbsolutePath().normalize()
                : server.resolveSibling("Client");
        List<String> violations = new ArrayList<>();
        scan(server, violations);
        if (Files.isDirectory(client)) {
            scan(client, violations);
        }
        if (!violations.isEmpty()) {
            System.err.println("Legacy qh/QH/情怀 runtime identifiers are forbidden:");
            violations.stream().limit(200).forEach(item -> System.err.println("  " + item));
            if (violations.size() > 200) {
                System.err.println("  ... and " + (violations.size() - 200) + " more");
            }
            System.exit(1);
        }
        System.out.println("Aoo brand boundary check passed.");
    }

    private static void scan(Path root, List<String> violations) throws IOException {
        try (var paths = Files.walk(root)) {
            paths.filter(path -> !excluded(root, path))
                    .filter(Files::isRegularFile)
                    .forEach(path -> inspect(root, path, violations));
        }
    }

    private static boolean excluded(Path root, Path path) {
        Path relative = root.relativize(path);
        for (Path segment : relative) {
            String name = segment.toString();
            if (EXCLUDED_SEGMENTS.contains(name) || name.startsWith("build-")) {
                return true;
            }
        }
        return relative.startsWith("docs");
    }

    private static void inspect(Path root, Path path, List<String> violations) {
        Path relative = root.relativize(path);
        if (relative.endsWith("tools/AooBrandBoundaryCheck.java")
                || relative.endsWith("tools/AooBrandBoundaryCheckTest.java")) {
            return;
        }
        if (NON_RUNTIME_METADATA.contains(relative.toString())) {
            return;
        }
        if (relative.getNameCount() == 2
                && ("tools".equals(relative.getName(0).toString()) || "scripts".equals(relative.getName(0).toString()))
                && LEGACY_REFERENCE_TOOLS.contains(relative.getFileName().toString())) {
            return;
        }
        if (LEGACY.matcher(relative.toString()).find()) {
            violations.add(root.getFileName() + ":path:" + relative);
        }
        if (!TEXT_EXTENSIONS.contains(extension(path))) {
            return;
        }
        try {
            var decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            String content = decoder.decode(java.nio.ByteBuffer.wrap(Files.readAllBytes(path))).toString();
            var matcher = LEGACY.matcher(content);
            if (matcher.find()) {
                long line = content.substring(0, matcher.start()).lines().count();
                violations.add(root.getFileName() + ":content:" + relative + ":" + Math.max(1, line));
            }
        } catch (CharacterCodingException ignored) {
            // Historical non-UTF-8 files are handled by FS04 and old-build isolation tasks.
        } catch (IOException error) {
            throw new RuntimeException("Cannot inspect " + path, error);
        }
    }

    private static String extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }
}
