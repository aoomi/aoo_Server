package com.aoo.bcg.gamespi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable build/release projection of the canonical runtime metadata manifest.
 *
 * <p>The projection is intentionally independent of a filesystem path. A release
 * build supplies its bytes (or a generated class-path resource) and the runtime
 * receives this value; production code must never discover the manifest by
 * walking a developer checkout or by deriving fields from an identity.</p>
 */
public final class RuntimeMetadataProjection {
    public enum Trust {
        UNVERIFIED,
        SIGNED_CANONICAL,
        VERIFIED_LOCAL_MIRROR
    }
    public static final int REQUIRED_ROW_COUNT = 32;
    public static final List<String> REQUIRED_FIELDS = List.of(
            "snapshotSchemaVersion", "cardCodecVersion", "eventInterpreterVersion",
            "protocolVersion", "contractVersion", "sourceHash", "releaseId", "indexGeneration");

    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern EDITION = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");
    private static final Pattern VERSION = EDITION;

    private final String manifestSha256;
    private final String edition;
    private final Trust trust;
    private final String canonicalRowsDigest;
    private final List<Entry> rows;
    private final Map<Integer, Entry> byId;
    private final Map<IdentityKey, Entry> byIdentity;

    public RuntimeMetadataProjection(String manifestSha256, String edition,
                                     Collection<? extends Entry> rows) {
        this(manifestSha256, edition, rows, Trust.UNVERIFIED);
    }

    static RuntimeMetadataProjection trusted(String manifestSha256, String edition,
                                             Collection<? extends Entry> rows, Trust trust) {
        if (trust == Trust.UNVERIFIED) throw new IllegalArgumentException("trusted projection authority required");
        return new RuntimeMetadataProjection(manifestSha256, edition, rows, trust);
    }

    private RuntimeMetadataProjection(String manifestSha256, String edition,
                                      Collection<? extends Entry> rows, Trust trust) {
        this.manifestSha256 = requireSha(manifestSha256, "RUNTIME_METADATA_MANIFEST_SHA_REQUIRED");
        this.edition = requireEdition(edition);
        this.trust = Objects.requireNonNull(trust, "trust");
        Objects.requireNonNull(rows, "rows");
        List<Entry> normalized = new ArrayList<>(rows);
        if (normalized.size() != REQUIRED_ROW_COUNT) {
            throw new IllegalArgumentException("RUNTIME_METADATA_PROJECTION_ROW_COUNT_INVALID rows="
                    + normalized.size() + " expected=" + REQUIRED_ROW_COUNT);
        }
        normalized.sort(Comparator.comparingInt(Entry::gameId));
        Map<Integer, Entry> ids = new LinkedHashMap<>();
        Map<IdentityKey, Entry> identities = new LinkedHashMap<>();
        List<Long> releases = new ArrayList<>();
        List<Long> generations = new ArrayList<>();
        for (Entry row : normalized) {
            Objects.requireNonNull(row, "runtime metadata row");
            if (row.gameId() == 516 || row.playVersion().toLowerCase(java.util.Locale.ROOT).startsWith("legacy-")) {
                throw new IllegalArgumentException("RUNTIME_METADATA_COMPATIBILITY_IDENTITY gameId="
                        + row.gameId());
            }
            if (ids.put(row.gameId(), row) != null) {
                throw new IllegalArgumentException("RUNTIME_METADATA_PROJECTION_DUPLICATE_GAME_ID gameId="
                        + row.gameId());
            }
            IdentityKey identity = IdentityKey.of(row);
            if (identities.put(identity, row) != null) {
                throw new IllegalArgumentException("RUNTIME_METADATA_PROJECTION_DUPLICATE_IDENTITY gameId="
                        + row.gameId());
            }
            releases.add(row.releaseId());
            generations.add(row.indexGeneration());
        }
        if (releases.stream().distinct().count() != REQUIRED_ROW_COUNT) {
            throw new IllegalArgumentException("RUNTIME_METADATA_PROJECTION_RELEASE_ID_NOT_UNIQUE");
        }
        if (generations.stream().distinct().count() != REQUIRED_ROW_COUNT) {
            throw new IllegalArgumentException("RUNTIME_METADATA_PROJECTION_INDEX_GENERATION_NOT_UNIQUE");
        }
        this.rows = List.copyOf(normalized);
        this.byId = Map.copyOf(ids);
        this.byIdentity = Map.copyOf(identities);
        this.canonicalRowsDigest = digestRows(this.rows);
    }

    public String manifestSha256() { return manifestSha256; }

    /** Alias used by release/build adapters that call the field manifestSha. */
    public String manifestSha() { return manifestSha256; }

    public String edition() { return edition; }

    public Trust trust() { return trust; }

    public String canonicalRowsDigest() { return canonicalRowsDigest; }

    /** Alias matching the signed manifest's top-level field. */
    public String releaseEdition() { return edition; }

    public int rowCount() { return rows.size(); }

    public List<Entry> rows() { return rows; }

    public Entry require(int gameId) {
        Entry row = byId.get(gameId);
        if (row == null) {
            throw new IllegalStateException("RUNTIME_METADATA_PROJECTION_ROW_MISSING gameId=" + gameId);
        }
        return row;
    }

    public Entry require(int gameId, String gameCode, String family,
                         String playVersion, String regionCode) {
        Entry row = require(gameId);
        if (!row.gameCode().equals(requireText(gameCode, "gameCode"))
                || !row.family().equals(requireText(family, "family"))
                || !row.playVersion().equals(requireText(playVersion, "playVersion"))
                || !row.regionCode().equals(requireText(regionCode, "regionCode"))) {
            throw new IllegalStateException("RUNTIME_METADATA_PROJECTION_IDENTITY_MISMATCH gameId=" + gameId);
        }
        return row;
    }

    public Entry find(int gameId, String gameCode, String family,
                      String playVersion, String regionCode) {
        if (gameCode == null || family == null || playVersion == null || regionCode == null) return null;
        return byIdentity.get(new IdentityKey(gameId, gameCode.trim(), family.trim(),
                playVersion.trim(), regionCode.trim()));
    }

    public Map<String, Object> manifestEnvelope() {
        return Map.of("manifestSha256", manifestSha256, "edition", edition,
                "rowCount", rowCount(), "requiredFields", REQUIRED_FIELDS);
    }

    @Override public boolean equals(Object other) {
        if (!(other instanceof RuntimeMetadataProjection that)) return false;
        return manifestSha256.equals(that.manifestSha256)
                && edition.equals(that.edition) && trust == that.trust && rows.equals(that.rows);
    }

    @Override public int hashCode() {
        return Objects.hash(manifestSha256, edition, trust, rows);
    }

    private static String requireSha(String value, String error) {
        if (value == null || !SHA256.matcher(value).matches()) throw new IllegalArgumentException(error);
        return value;
    }

    private static String requireEdition(String value) {
        if (value == null || !EDITION.matcher(value).matches()) {
            throw new IllegalArgumentException("RUNTIME_METADATA_MANIFEST_EDITION_INVALID");
        }
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }

    private static String digestRows(List<Entry> rows) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            for (Entry row : rows) {
                String canonical = row.gameId() + "\u001f" + row.gameCode() + "\u001f" + row.family()
                        + "\u001f" + row.playVersion() + "\u001f" + row.regionCode() + "\u001f"
                        + row.snapshotSchemaVersion() + "\u001f" + row.cardCodecVersion() + "\u001f"
                        + row.eventInterpreterVersion() + "\u001f" + row.protocolVersion() + "\u001f"
                        + row.contractVersion() + "\u001f" + row.sourceHash() + "\u001f"
                        + row.releaseId() + "\u001f" + row.indexGeneration() + "\n";
                digest.update(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    public static class Entry {
        private final int gameId;
        private final String gameCode;
        private final String family;
        private final String playVersion;
        private final String regionCode;
        private final String snapshotSchemaVersion;
        private final String cardCodecVersion;
        private final String eventInterpreterVersion;
        private final String protocolVersion;
        private final String contractVersion;
        private final String sourceHash;
        private final long releaseId;
        private final long indexGeneration;

        public Entry(int gameId, String gameCode, String family, String playVersion, String regionCode,
                     String snapshotSchemaVersion, String cardCodecVersion,
                     String eventInterpreterVersion, String protocolVersion, String contractVersion,
                     String sourceHash, long releaseId, long indexGeneration) {
            if (gameId <= 0) throw new IllegalArgumentException("RUNTIME_METADATA_GAME_ID_INVALID");
            this.gameId = gameId;
            this.gameCode = requireText(gameCode, "gameCode");
            this.family = requireText(family, "family");
            this.playVersion = requireText(playVersion, "playVersion");
            this.regionCode = requireText(regionCode, "regionCode");
            this.snapshotSchemaVersion = version(snapshotSchemaVersion, "snapshotSchemaVersion");
            this.cardCodecVersion = version(cardCodecVersion, "cardCodecVersion");
            this.eventInterpreterVersion = version(eventInterpreterVersion, "eventInterpreterVersion");
            this.protocolVersion = version(protocolVersion, "protocolVersion");
            this.contractVersion = version(contractVersion, "contractVersion");
            this.sourceHash = requireSha(sourceHash, "RUNTIME_METADATA_SOURCE_HASH_INVALID");
            if (releaseId <= 0) throw new IllegalArgumentException("RUNTIME_METADATA_RELEASE_ID_INVALID");
            if (indexGeneration <= 0) throw new IllegalArgumentException("RUNTIME_METADATA_INDEX_GENERATION_INVALID");
            this.releaseId = releaseId;
            this.indexGeneration = indexGeneration;
        }

        public final int gameId() { return gameId; }
        public final String gameCode() { return gameCode; }
        public final String code() { return gameCode; }
        public final String family() { return family; }
        public final String playVersion() { return playVersion; }
        public final String regionCode() { return regionCode; }
        public final String snapshotSchemaVersion() { return snapshotSchemaVersion; }
        public final String cardCodecVersion() { return cardCodecVersion; }
        public final String eventInterpreterVersion() { return eventInterpreterVersion; }
        public final String protocolVersion() { return protocolVersion; }
        public final String contractVersion() { return contractVersion; }
        public final String sourceHash() { return sourceHash; }
        public final long releaseId() { return releaseId; }
        public final long indexGeneration() { return indexGeneration; }

        public final Map<String, Object> fields() {
            return Map.of("snapshotSchemaVersion", snapshotSchemaVersion,
                    "cardCodecVersion", cardCodecVersion,
                    "eventInterpreterVersion", eventInterpreterVersion,
                    "protocolVersion", protocolVersion,
                    "contractVersion", contractVersion,
                    "sourceHash", sourceHash,
                    "releaseId", releaseId,
                    "indexGeneration", indexGeneration);
        }

        @Override public final boolean equals(Object other) {
            if (!(other instanceof Entry that)) return false;
            return gameId == that.gameId && releaseId == that.releaseId
                    && indexGeneration == that.indexGeneration
                    && gameCode.equals(that.gameCode) && family.equals(that.family)
                    && playVersion.equals(that.playVersion) && regionCode.equals(that.regionCode)
                    && snapshotSchemaVersion.equals(that.snapshotSchemaVersion)
                    && cardCodecVersion.equals(that.cardCodecVersion)
                    && eventInterpreterVersion.equals(that.eventInterpreterVersion)
                    && protocolVersion.equals(that.protocolVersion)
                    && contractVersion.equals(that.contractVersion)
                    && sourceHash.equals(that.sourceHash);
        }

        @Override public final int hashCode() {
            return Objects.hash(gameId, gameCode, family, playVersion, regionCode,
                    snapshotSchemaVersion, cardCodecVersion, eventInterpreterVersion,
                    protocolVersion, contractVersion, sourceHash, releaseId, indexGeneration);
        }

        @Override public final String toString() {
            return "RuntimeMetadataEntry[" + gameId + "/" + gameCode + "/" + playVersion
                    + ", releaseId=" + releaseId + ", indexGeneration=" + indexGeneration + "]";
        }

        private static String version(String value, String field) {
            if (value == null || !VERSION.matcher(value).matches()) {
                throw new IllegalArgumentException("RUNTIME_METADATA_" + field.toUpperCase() + "_INVALID");
            }
            return value;
        }
    }

    /** Short row name for build adapters and focused tests. */
    public static final class Row extends Entry {
        public Row(int gameId, String gameCode, String family, String playVersion, String regionCode,
                   String snapshotSchemaVersion, String cardCodecVersion,
                   String eventInterpreterVersion, String protocolVersion, String contractVersion,
                   String sourceHash, long releaseId, long indexGeneration) {
            super(gameId, gameCode, family, playVersion, regionCode, snapshotSchemaVersion,
                    cardCodecVersion, eventInterpreterVersion, protocolVersion, contractVersion, sourceHash,
                    releaseId, indexGeneration);
        }
    }

    private record IdentityKey(int gameId, String gameCode, String family,
                               String playVersion, String regionCode) {
        private static IdentityKey of(Entry row) {
            return new IdentityKey(row.gameId(), row.gameCode(), row.family(), row.playVersion(), row.regionCode());
        }
    }
}
