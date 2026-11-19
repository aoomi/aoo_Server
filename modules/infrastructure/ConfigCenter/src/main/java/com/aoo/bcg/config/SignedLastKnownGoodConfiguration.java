package com.aoo.bcg.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aoo.bcg.common.resource.AtomicFilePublisher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

/** Signed, bounded-age fallback used only when the authoritative configuration center is unavailable. */
public final class SignedLastKnownGoodConfiguration {
    private record Envelope(String releaseId, Map<String,String> domainVersions, String checksum,
                            Instant publishedAt, Instant savedAt, String signature) {}
    private final Path path;
    private final ObjectMapper json;
    private final Clock clock;
    private final Duration maximumAge;

    public SignedLastKnownGoodConfiguration(Path path, ObjectMapper json, Clock clock, Duration maximumAge) {
        if (maximumAge.isNegative() || maximumAge.isZero()) throw new IllegalArgumentException("maximum age must be positive");
        this.path = path; this.json = json; this.clock = clock; this.maximumAge = maximumAge;
    }

    public void save(ConfigurationRelease release, byte[] signingKey) {
        try {
            Instant savedAt = clock.instant();
            String signature = sign(release.releaseId(), release.domainVersions(), release.checksum(), release.publishedAt(), savedAt, signingKey);
            byte[] body = json.writeValueAsBytes(new Envelope(release.releaseId(), release.domainVersions(), release.checksum(),
                release.publishedAt(), savedAt, signature));
            AtomicFilePublisher.write(path, body, AtomicFilePublisher.sha256(body));
        } catch (Exception error) { throw new IllegalStateException("cannot persist signed last-known-good configuration", error); }
    }

    public ConfigurationRelease loadAfterRemoteFailure(Throwable remoteFailure, byte[] signingKey) {
        if (remoteFailure == null) throw new IllegalArgumentException("remote failure is required before fallback");
        try {
            Envelope envelope = json.readValue(Files.readAllBytes(path), Envelope.class);
            if (envelope.savedAt().plus(maximumAge).isBefore(clock.instant())) throw new IllegalStateException("last-known-good configuration expired");
            String expected = sign(envelope.releaseId(), envelope.domainVersions(), envelope.checksum(),
                envelope.publishedAt(), envelope.savedAt(), signingKey);
            if (!MessageDigest.isEqual(Base64.getDecoder().decode(expected), Base64.getDecoder().decode(envelope.signature())))
                throw new IllegalStateException("last-known-good configuration signature mismatch");
            return new ConfigurationRelease(envelope.releaseId(), envelope.domainVersions(), envelope.checksum(), envelope.publishedAt());
        } catch (IllegalStateException error) { throw error; }
        catch (Exception error) { throw new IllegalStateException("cannot load signed last-known-good configuration", error); }
    }

    private static String sign(String releaseId, Map<String,String> versions, String checksum,
                               Instant publishedAt, Instant savedAt, byte[] key) throws Exception {
        if (key == null || key.length < 32) throw new IllegalArgumentException("snapshot signing key must contain at least 32 bytes");
        StringBuilder canonical = new StringBuilder(releaseId).append('\n').append(checksum).append('\n')
            .append(publishedAt).append('\n').append(savedAt).append('\n');
        new TreeMap<>(versions).forEach((domain,version) -> canonical.append(domain).append('=').append(version).append('\n'));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(canonical.toString().getBytes(StandardCharsets.UTF_8)));
    }
}
