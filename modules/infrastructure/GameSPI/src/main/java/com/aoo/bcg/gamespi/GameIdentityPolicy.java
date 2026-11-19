package com.aoo.bcg.gamespi;

/**
 * Registration policy for the production provider projection.
 *
 * <p>The canonical identity registry is the source used to generate the
 * catalog and provider index.  This small gate is intentionally fail-closed
 * for the legacy identities that are already known to be unsafe in a current
 * room route.  Compatibility registries opt in explicitly so historical room
 * restore can continue without making the legacy provider discoverable by a
 * new-room registry.</p>
 */
public final class GameIdentityPolicy {
    private static final java.util.Map<Integer, CompatibilityIdentity> COMPATIBILITY_IDENTITIES =
            java.util.Map.ofEntries(
                    java.util.Map.entry(136, new CompatibilityIdentity(136, "byzp", "1.0.0")),
                    java.util.Map.entry(153, new CompatibilityIdentity(153, "xpphz", "1.0.0")),
                    java.util.Map.entry(176, new CompatibilityIdentity(176, "ahphz", "1.0.0")),
                    java.util.Map.entry(302, new CompatibilityIdentity(302, "glzp", "1.0.0")),
                    java.util.Map.entry(342, new CompatibilityIdentity(342, "yzchz", "1.0.0")),
                    java.util.Map.entry(404, new CompatibilityIdentity(404, "ycsdr", "1.0.0")),
                    java.util.Map.entry(407, new CompatibilityIdentity(407, "ychp", "1.0.0")),
                    java.util.Map.entry(462, new CompatibilityIdentity(462, "bzp", "1.0.0")),
                    java.util.Map.entry(490, new CompatibilityIdentity(490, "lczp", "1.0.0")),
                    java.util.Map.entry(596, new CompatibilityIdentity(596, "pxphz", "1.0.0")));
    private static final java.util.Map<Integer, String> CURRENT_PLAY_VERSIONS = currentPlayVersions();
    private static final java.util.Map<String, PublishedIdentity> CURRENT_IDENTITIES = currentIdentities();

    /**
     * The canonical identity projection used by non-registry modules that
     * need to render or persist a game reference.  It is derived from the
     * same current-game-identities.tsv used by admission; callers must not
     * maintain a second game-code table.
     */
    public record PublishedIdentity(int gameId, String gameCode, String playVersion, String family) { }

    private GameIdentityPolicy() { }

    public static boolean isCompatibilityOnly(GameDescriptor descriptor) {
        if (descriptor == null) return true;
        if (descriptor.gameId() == 516) return true;
        CompatibilityIdentity compatibility = COMPATIBILITY_IDENTITIES.get(descriptor.gameId());
        if (compatibility != null && compatibility.matches(descriptor)) return true;
        if (descriptor.category() == GameCategory.WORD_CARD) return true;
        String version = descriptor.version().trim().toLowerCase(java.util.Locale.ROOT);
        return version.startsWith("legacy-") || version.startsWith("legacy_");
    }

    public static void requireProduction(GameDescriptor descriptor) {
        if (isCompatibilityOnly(descriptor)) {
            throw new IllegalArgumentException("IDENTITY_COMPATIBILITY_ONLY gameId="
                    + descriptor.gameId() + " gameCode=" + descriptor.code()
                    + " playVersion=" + descriptor.version());
        }
    }

    /**
     * Admission gate for a new Hall room.  Registry discovery may still load
     * historical catalog rows for migration, but a new room may only use one
     * of the published 32 identity tuples.  Keeping this check here prevents
     * an old seed or a stale client from turning a historical row into a new
     * room even when a database route was accidentally left active.
     */
    public static void requireCurrentPublished(int gameId, String playVersion) {
        String expected = CURRENT_PLAY_VERSIONS.get(gameId);
        if (expected == null || playVersion == null || !expected.equals(playVersion.trim())) {
            throw new IllegalArgumentException("IDENTITY_NOT_CURRENT_PUBLISHED gameId="
                    + gameId + " playVersion=" + String.valueOf(playVersion)
                    + " expected=" + String.valueOf(expected));
        }
    }

    public static boolean isCurrentPublished(int gameId, String playVersion) {
        try {
            requireCurrentPublished(gameId, playVersion);
            return true;
        } catch (IllegalArgumentException rejected) {
            return false;
        }
    }

    public static java.util.Optional<PublishedIdentity> currentByCode(String gameCode) {
        if (gameCode == null || gameCode.isBlank()) return java.util.Optional.empty();
        return java.util.Optional.ofNullable(CURRENT_IDENTITIES.get(gameCode.trim().toLowerCase(java.util.Locale.ROOT)));
    }

    public static java.util.Optional<PublishedIdentity> currentById(int gameId) {
        return CURRENT_IDENTITIES.values().stream().filter(identity -> identity.gameId() == gameId).findFirst();
    }

    private static java.util.Map<Integer, String> currentPlayVersions() {
        java.util.Map<Integer, String> result = new java.util.LinkedHashMap<>();
        try (java.io.InputStream input = GameIdentityPolicy.class
                .getResourceAsStream("/current-game-identities.tsv")) {
            if (input == null) throw new IllegalStateException("CURRENT_IDENTITY_MANIFEST_MISSING");
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(
                    input, java.nio.charset.StandardCharsets.UTF_8))) {
                String header = reader.readLine();
                if (!"gameId\tgameCode\tplayVersion\tfamily".equals(header)) {
                    throw new IllegalStateException("CURRENT_IDENTITY_MANIFEST_HEADER_INVALID");
                }
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] fields = line.split("\\t", -1);
                    if (fields.length != 4) throw new IllegalStateException("CURRENT_IDENTITY_MANIFEST_ROW_INVALID");
                    int gameId = Integer.parseInt(fields[0]);
                    String code = fields[1].trim();
                    String version = fields[2].trim();
                    String family = fields[3].trim();
                    if (gameId <= 0 || code.isBlank() || version.isBlank() || family.isBlank()
                            || result.putIfAbsent(gameId, version) != null) {
                        throw new IllegalStateException("CURRENT_IDENTITY_MANIFEST_DUPLICATE_OR_INVALID");
                    }
                }
            }
        } catch (java.io.IOException | RuntimeException error) {
            throw new IllegalStateException("CURRENT_IDENTITY_MANIFEST_INVALID", error);
        }
        if (result.size() != 32) throw new IllegalStateException("CURRENT_IDENTITY_MANIFEST_COUNT_INVALID");
        return java.util.Map.copyOf(result);
    }

    private static java.util.Map<String, PublishedIdentity> currentIdentities() {
        java.util.Map<String, PublishedIdentity> result = new java.util.LinkedHashMap<>();
        try (java.io.InputStream input = GameIdentityPolicy.class
                .getResourceAsStream("/current-game-identities.tsv")) {
            if (input == null) throw new IllegalStateException("CURRENT_IDENTITY_MANIFEST_MISSING");
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(
                    input, java.nio.charset.StandardCharsets.UTF_8))) {
                String header = reader.readLine();
                if (!"gameId\tgameCode\tplayVersion\tfamily".equals(header)) {
                    throw new IllegalStateException("CURRENT_IDENTITY_MANIFEST_HEADER_INVALID");
                }
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] fields = line.split("\\t", -1);
                    if (fields.length != 4) throw new IllegalStateException("CURRENT_IDENTITY_MANIFEST_ROW_INVALID");
                    PublishedIdentity identity = new PublishedIdentity(Integer.parseInt(fields[0]),
                            fields[1].trim(), fields[2].trim(), fields[3].trim());
                    String key = identity.gameCode().toLowerCase(java.util.Locale.ROOT);
                    if (identity.gameId() <= 0 || key.isBlank() || identity.playVersion().isBlank()
                            || identity.family().isBlank() || result.putIfAbsent(key, identity) != null) {
                        throw new IllegalStateException("CURRENT_IDENTITY_MANIFEST_DUPLICATE_OR_INVALID");
                    }
                }
            }
        } catch (java.io.IOException | RuntimeException error) {
            throw new IllegalStateException("CURRENT_IDENTITY_MANIFEST_INVALID", error);
        }
        if (result.size() != 32) throw new IllegalStateException("CURRENT_IDENTITY_MANIFEST_COUNT_INVALID");
        return java.util.Map.copyOf(result);
    }

    private record CompatibilityIdentity(int gameId, String gameCode, String playVersion) {
        private boolean matches(GameDescriptor descriptor) {
            return gameId == descriptor.gameId()
                    && gameCode.equals(descriptor.code())
                    && playVersion.equals(descriptor.version());
        }
    }
}
