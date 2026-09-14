package com.aoo.bcg.common.resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;

/** Recomputes upload metadata from server-observed bytes and decoder output. */
public final class UploadMetadataVerifier {
    public record Claim(String fileName, String mimeType, long sizeBytes, long durationMillis,
                        String sha256) {
        public Claim {
            if (fileName == null || mimeType == null || mimeType.isBlank() || sizeBytes < 0
                    || durationMillis < 0 || sha256 == null || !sha256.matches("(?i)[0-9a-f]{64}"))
                throw new IllegalArgumentException("invalid claimed upload metadata");
        }
    }
    public record Verified(String safeFileName, String mimeType, long sizeBytes,
                           long durationMillis, String sha256) { }

    private final long maximumBytes;
    private final long maximumDurationMillis;
    private final Set<String> allowedMimeTypes;

    public UploadMetadataVerifier(long maximumBytes, long maximumDurationMillis,
                                  Set<String> allowedMimeTypes) {
        if (maximumBytes < 1 || maximumDurationMillis < 0 || allowedMimeTypes == null
                || allowedMimeTypes.isEmpty()) throw new IllegalArgumentException("invalid upload policy");
        this.maximumBytes = maximumBytes;
        this.maximumDurationMillis = maximumDurationMillis;
        this.allowedMimeTypes = Set.copyOf(allowedMimeTypes.stream()
                .map(value -> value.toLowerCase(Locale.ROOT)).toList());
    }

    public Verified verify(Claim claim, InputStream bytes, String detectedMimeType,
                           long decodedDurationMillis) throws IOException {
        if (claim == null || bytes == null || detectedMimeType == null)
            throw new IllegalArgumentException("upload claim, bytes and detected MIME are required");
        String safeName = safeName(claim.fileName());
        String mime = detectedMimeType.toLowerCase(Locale.ROOT);
        if (!allowedMimeTypes.contains(mime)) throw new SecurityException("detected MIME is not allowed");
        if (!mime.equals(claim.mimeType().toLowerCase(Locale.ROOT)))
            throw new SecurityException("claimed MIME does not match server detection");
        if (decodedDurationMillis < 0 || decodedDurationMillis > maximumDurationMillis
                || claim.durationMillis() != decodedDurationMillis)
            throw new SecurityException("claimed duration does not match server decoder");

        MessageDigest digest = sha256();
        long size = 0;
        byte[] buffer = new byte[8192];
        int read;
        int consecutiveEmptyReads = 0;
        while ((read = bytes.read(buffer)) >= 0) {
            if (read == 0) {
                if (++consecutiveEmptyReads > 16)
                    throw new IOException("upload stream made no progress");
                continue;
            }
            consecutiveEmptyReads = 0;
            size = Math.addExact(size, read);
            if (size > maximumBytes) throw new SecurityException("upload exceeds byte budget");
            digest.update(buffer, 0, read);
        }
        String hash = HexFormat.of().formatHex(digest.digest());
        if (claim.sizeBytes() != size) throw new SecurityException("claimed size does not match received bytes");
        if (claim.sha256() == null || !MessageDigest.isEqual(hash.getBytes(StandardCharsets.US_ASCII),
                claim.sha256().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII)))
            throw new SecurityException("claimed hash does not match received bytes");
        return new Verified(safeName, mime, size, decodedDurationMillis, hash);
    }

    private static String safeName(String fileName) {
        if (fileName == null) throw new IllegalArgumentException("file name is required");
        String value = Normalizer.normalize(fileName, Normalizer.Form.NFC).strip();
        if (value.isEmpty() || value.length() > 128 || value.equals(".") || value.equals("..")
                || value.indexOf('/') >= 0 || value.indexOf('\\') >= 0
                || value.chars().anyMatch(Character::isISOControl))
            throw new SecurityException("unsafe upload file name");
        return value;
    }

    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
