package com.aoo.bcg.config;
import java.time.Instant;
import java.util.Map;
public record ReleaseManifest(int gameId, String playVersion, String snapshotSchemaVersion,
                              String cardCodecVersion, String eventInterpreterVersion, String protocolVersion,
                              String sourceHash, long releaseId, long indexGeneration,
                              String componentVersion, String clientBundleVersion,
                              Instant publishedAt, Map<String, String> checksums) {
    static final String RUNTIME_VERSION = "[A-Za-z0-9][A-Za-z0-9._-]{0,63}";
    static final String SOURCE_HASH = "[0-9a-f]{64}";

    public ReleaseManifest {
        requireRuntimeVersion(snapshotSchemaVersion, "snapshot schema version");
        requireRuntimeVersion(cardCodecVersion, "card codec version");
        requireRuntimeVersion(eventInterpreterVersion, "event interpreter version");
        requireRuntimeVersion(protocolVersion, "protocol version");
        if (sourceHash == null || !sourceHash.matches(SOURCE_HASH))
            throw new IllegalArgumentException("source hash is required and must be a lowercase SHA-256 release field");
        if (releaseId <= 0 || indexGeneration <= 0)
            throw new IllegalArgumentException("release id and index generation are required canonical literals");
        checksums = Map.copyOf(checksums == null ? Map.of() : checksums);
    }

    private static void requireRuntimeVersion(String value, String field) {
        if (value == null || !value.matches(RUNTIME_VERSION))
            throw new IllegalArgumentException(field + " is required and must be a valid release field");
    }
}
