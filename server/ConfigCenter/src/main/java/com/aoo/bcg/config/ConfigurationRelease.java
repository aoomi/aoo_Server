package com.aoo.bcg.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/** Immutable, atomic version set spanning every configuration domain. */
public record ConfigurationRelease(String releaseId, Map<String,String> domainVersions,
                                   String checksum, Instant publishedAt) {
    public ConfigurationRelease {
        if (releaseId == null || releaseId.isBlank() || domainVersions == null || domainVersions.isEmpty()
                || publishedAt == null) throw new IllegalArgumentException("invalid configuration release");
        domainVersions = Map.copyOf(domainVersions);
        domainVersions.forEach((domain,version) -> {
            if (!domain.matches("[a-z][a-z0-9.-]*") || version == null || version.isBlank())
                throw new IllegalArgumentException("invalid configuration domain version");
        });
        if (!digest(domainVersions).equals(checksum)) throw new IllegalArgumentException("configuration release checksum mismatch");
    }

    public static ConfigurationRelease create(String releaseId, Map<String,String> versions, Instant now) {
        return new ConfigurationRelease(releaseId, versions, digest(versions), now);
    }

    private static String digest(Map<String,String> versions) {
        try {
            var canonical = new StringBuilder();
            new TreeMap<>(versions).forEach((key,value) -> canonical.append(key).append('=').append(value).append('\n'));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) { throw new IllegalStateException(error); }
    }
}
