package com.aoo.bcg.common.replay;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Bounded, gap-free, duplicate-free replay journal with manifest-bound tamper evidence. */
public final class IntegrityReplayJournal {
    private final ReplayVersionManifest manifest;
    private final int maximumFrames;
    private final long maximumBytes;
    private final List<IntegrityReplayFrame> frames = new ArrayList<>();
    private final Set<String> eventIds = new HashSet<>();
    private long bytes;

    public IntegrityReplayJournal(ReplayVersionManifest manifest, int maximumFrames, long maximumBytes) {
        this.manifest = Objects.requireNonNull(manifest, "manifest");
        if (maximumFrames < 1 || maximumBytes < 1) throw new IllegalArgumentException("positive replay budgets required");
        this.maximumFrames = maximumFrames;
        this.maximumBytes = maximumBytes;
    }

    public synchronized IntegrityReplayFrame append(long sequence, int roundNo, String eventId,
                                                     String eventType, int schemaVersion,
                                                     byte[] canonicalPayload) {
        if (sequence != frames.size() + 1L || roundNo < 0 || blank(eventId) || blank(eventType)
                || schemaVersion != manifest.eventSchemaVersion() || canonicalPayload == null)
            throw new IllegalArgumentException("invalid replay frame identity or sequence");
        if (!eventIds.add(eventId)) throw new IllegalStateException("duplicate replay event id");
        if (frames.size() == maximumFrames || Math.addExact(bytes, canonicalPayload.length) > maximumBytes) {
            eventIds.remove(eventId);
            throw new IllegalStateException("replay retention budget exceeded");
        }
        String previousHash = frames.isEmpty() ? manifest.initialStateHash() : frames.getLast().hash();
        String hash = calculate(manifest, sequence, roundNo, eventId, eventType, schemaVersion,
                canonicalPayload, previousHash);
        IntegrityReplayFrame frame = new IntegrityReplayFrame(sequence, roundNo, eventId, eventType,
                schemaVersion, canonicalPayload.clone(), previousHash, hash);
        frames.add(frame);
        bytes += canonicalPayload.length;
        return frame;
    }

    public synchronized List<IntegrityReplayFrame> frames() { return List.copyOf(frames); }
    public synchronized long bytes() { return bytes; }
    public ReplayVersionManifest manifest() { return manifest; }

    public static void verify(ReplayVersionManifest manifest, List<IntegrityReplayFrame> frames) {
        Objects.requireNonNull(manifest, "manifest");
        String previous = manifest.initialStateHash();
        Set<String> eventIds = new HashSet<>();
        long expected = 1;
        for (IntegrityReplayFrame frame : List.copyOf(frames)) {
            if (frame.sequence() != expected++ || !eventIds.add(frame.eventId())
                    || frame.schemaVersion() != manifest.eventSchemaVersion()
                    || !previous.equals(frame.previousHash()))
                throw new SecurityException("replay sequence, identity or version mismatch");
            String calculated = calculate(manifest, frame.sequence(), frame.roundNo(), frame.eventId(),
                    frame.eventType(), frame.schemaVersion(), frame.canonicalPayload(), previous);
            if (!MessageDigest.isEqual(calculated.getBytes(StandardCharsets.US_ASCII),
                    frame.hash().getBytes(StandardCharsets.US_ASCII)))
                throw new SecurityException("replay hash mismatch");
            previous = frame.hash();
        }
    }

    private static String calculate(ReplayVersionManifest manifest, long sequence, int roundNo,
                                    String eventId, String eventType, int schemaVersion,
                                    byte[] payload, String previousHash) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, manifest.roomId());
            update(digest, manifest.gameId());
            update(digest, manifest.playVersion());
            update(digest, manifest.ruleVersion());
            update(digest, manifest.algorithmVersion());
            update(digest, manifest.cardEncodingVersion());
            update(digest, manifest.randomCommitment());
            update(digest, sequence);
            update(digest, roundNo);
            update(digest, eventId);
            update(digest, eventType);
            update(digest, schemaVersion);
            update(digest, previousHash);
            digest.update(payload);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        update(digest, bytes.length);
        digest.update(bytes);
    }
    private static void update(MessageDigest digest, long value) {
        digest.update(ByteBuffer.allocate(Long.BYTES).putLong(value).array());
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
