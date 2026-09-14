package com.aoo.bcg.common.replay;

import java.util.Base64;

/** Immutable replay frame whose canonical payload is protected by a hash chain. */
public final class IntegrityReplayFrame {
    private final long sequence;
    private final int roundNo;
    private final String eventId;
    private final String eventType;
    private final int schemaVersion;
    private final String canonicalPayloadBase64;
    private final String previousHash;
    private final String hash;

    IntegrityReplayFrame(long sequence, int roundNo, String eventId, String eventType,
                         int schemaVersion, byte[] canonicalPayload, String previousHash,
                         String hash) {
        this.sequence = sequence;
        this.roundNo = roundNo;
        this.eventId = eventId;
        this.eventType = eventType;
        this.schemaVersion = schemaVersion;
        this.canonicalPayloadBase64 = Base64.getEncoder().encodeToString(canonicalPayload);
        this.previousHash = previousHash;
        this.hash = hash;
    }

    public long sequence() { return sequence; }
    public int roundNo() { return roundNo; }
    public String eventId() { return eventId; }
    public String eventType() { return eventType; }
    public int schemaVersion() { return schemaVersion; }
    public byte[] canonicalPayload() { return Base64.getDecoder().decode(canonicalPayloadBase64); }
    public String previousHash() { return previousHash; }
    public String hash() { return hash; }
}
