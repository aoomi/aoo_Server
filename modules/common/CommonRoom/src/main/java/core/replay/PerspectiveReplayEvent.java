package core.replay;

import java.util.Objects;
import java.util.Arrays;

/** Immutable replay event stored without trusting a client supplied viewer. */
public final class PerspectiveReplayEvent {
    private final long roomId;
    private final int setId;
    private final long sequence;
    private final ReplayEventVisibility visibility;
    private final long ownerPlayerId;
    private final String messageId;
    private final byte[] payload;
    private final int schemaVersion;
    private final String playVersion;

    public PerspectiveReplayEvent(long roomId, int setId, long sequence,
            ReplayEventVisibility visibility, long ownerPlayerId,
            String messageId, byte[] payload) {
        this(roomId,setId,sequence,visibility,ownerPlayerId,messageId,payload,1,"legacy-v1");
    }

    public PerspectiveReplayEvent(long roomId,int setId,long sequence,ReplayEventVisibility visibility,
            long ownerPlayerId,String messageId,byte[] payload,int schemaVersion,String playVersion) {
        if (roomId <= 0 || setId < 0 || sequence < 0) {
            throw new IllegalArgumentException("Invalid replay event identity");
        }
        this.visibility = Objects.requireNonNull(visibility, "visibility");
        if (visibility == ReplayEventVisibility.PLAYER_PRIVATE && ownerPlayerId <= 0) {
            throw new IllegalArgumentException("Private replay event requires an owner");
        }
        this.roomId = roomId;
        this.setId = setId;
        this.sequence = sequence;
        this.ownerPlayerId = ownerPlayerId;
        this.messageId = Objects.requireNonNull(messageId, "messageId");
        this.payload = Objects.requireNonNull(payload, "payload").clone();
        if(schemaVersion<=0||playVersion==null||playVersion.isBlank())throw new IllegalArgumentException("Invalid replay version");
        this.schemaVersion=schemaVersion;this.playVersion=playVersion;
    }

    public long getRoomId() { return roomId; }
    public int getSetId() { return setId; }
    public long getSequence() { return sequence; }
    public ReplayEventVisibility getVisibility() { return visibility; }
    public long getOwnerPlayerId() { return ownerPlayerId; }
    public String getMessageId() { return messageId; }
    public byte[] getPayload() { return payload.clone(); }
    public int getSchemaVersion(){return schemaVersion;}
    public String getPlayVersion(){return playVersion;}

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PerspectiveReplayEvent event)) return false;
        return roomId == event.roomId && setId == event.setId && sequence == event.sequence
                && ownerPlayerId == event.ownerPlayerId && visibility == event.visibility
                && schemaVersion==event.schemaVersion&&playVersion.equals(event.playVersion)
                && messageId.equals(event.messageId) && Arrays.equals(payload, event.payload);
    }

    @Override public int hashCode() {
        int result = Objects.hash(roomId, setId, sequence, visibility, ownerPlayerId, messageId,schemaVersion,playVersion);
        return 31 * result + Arrays.hashCode(payload);
    }
}
