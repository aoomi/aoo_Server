package com.aoo.bcg.common.random;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Commit-before-play deterministic random stream using rejection sampling without modulo bias. */
public final class FairRandomSession implements GameRandomSource {
    public static final String ALGORITHM_VERSION = "hmac-sha256-counter-v1";
    private static final SecureRandom SEED_SOURCE = new SecureRandom();
    private final byte[] secret;
    private final String domain;
    private final String commitment;
    private long drawCount;
    private boolean sealed;

    private FairRandomSession(byte[] secret, String domain) {
        if (secret == null || secret.length < 32 || domain == null || domain.isBlank())
            throw new IllegalArgumentException("a 256-bit secret and audit domain are required");
        this.secret = secret.clone();
        this.domain = domain;
        this.commitment = digest(ALGORITHM_VERSION + '\n' + domain + '\n'
                + Base64.getEncoder().encodeToString(secret));
    }

    public static FairRandomSession create(long roomId, int roundNo, String playVersion) {
        byte[] secret = new byte[32];
        SEED_SOURCE.nextBytes(secret);
        return new FairRandomSession(secret, domain(roomId, roundNo, playVersion));
    }

    public static FairRandomSession replay(RandomAuditReveal reveal) {
        Objects.requireNonNull(reveal, "reveal");
        if (!ALGORITHM_VERSION.equals(reveal.algorithmVersion()))
            throw new IllegalArgumentException("unsupported random algorithm version");
        FairRandomSession replay = new FairRandomSession(Base64.getDecoder().decode(reveal.secretBase64()),
                reveal.domain());
        if (!MessageDigest.isEqual(replay.commitment.getBytes(StandardCharsets.US_ASCII),
                reveal.commitment().getBytes(StandardCharsets.US_ASCII)))
            throw new SecurityException("random commitment mismatch");
        return replay;
    }

    static FairRandomSession deterministic(byte[] secret, long roomId, int roundNo, String playVersion) {
        return new FairRandomSession(secret, domain(roomId, roundNo, playVersion));
    }

    public String commitment() { return commitment; }
    public long drawCount() { return drawCount; }

    @Override public synchronized int nextInt(int bound) {
        requireOpen();
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive");
        long range = 1L << 32;
        long limit = range - range % bound;
        long candidate;
        do { candidate = nextUnsignedInt(); } while (candidate >= limit);
        return (int) (candidate % bound);
    }

    @Override public synchronized boolean nextBoolean() { return nextInt(2) == 1; }

    @Override public synchronized void shuffle(List<?> values) {
        requireOpen();
        Objects.requireNonNull(values, "values");
        for (int index = values.size() - 1; index > 0; index--)
            Collections.swap(values, index, nextInt(index + 1));
    }

    /** Legacy numeric seeds are intentionally unavailable for this audited stream. */
    @Override public long seed() {
        throw new UnsupportedOperationException("raw random material is available only through a sealed audit reveal");
    }

    public synchronized RandomAuditReveal seal(String finalStateHash) {
        if (sealed) throw new IllegalStateException("random session already sealed");
        if (finalStateHash == null || finalStateHash.isBlank())
            throw new IllegalArgumentException("final authoritative state hash is required");
        sealed = true;
        return new RandomAuditReveal(ALGORITHM_VERSION, domain, commitment,
                Base64.getEncoder().encodeToString(secret), finalStateHash, drawCount);
    }

    private long nextUnsignedInt() {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            mac.update(domain.getBytes(StandardCharsets.UTF_8));
            byte[] bytes = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(drawCount++).array());
            return Integer.toUnsignedLong(ByteBuffer.wrap(bytes).getInt());
        } catch (Exception failure) {
            throw new IllegalStateException("HMAC random source unavailable", failure);
        }
    }

    private void requireOpen() {
        if (sealed) throw new IllegalStateException("sealed random session cannot be used");
    }

    private static String domain(long roomId, int roundNo, String playVersion) {
        if (roomId <= 0 || roundNo < 0 || playVersion == null || playVersion.isBlank())
            throw new IllegalArgumentException("invalid random audit identity");
        return roomId + ":" + roundNo + ":" + playVersion;
    }

    private static String digest(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
