package com.aoo.bcg.config;
import java.time.Instant;
import java.util.Map;
public record ReleaseManifest(int gameId, String playVersion, String protocolVersion, String componentVersion,
                              String clientBundleVersion, Instant publishedAt, Map<String, String> checksums) {
    public ReleaseManifest { checksums = Map.copyOf(checksums == null ? Map.of() : checksums); }
}
