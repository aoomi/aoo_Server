package jsproto.c2s.cclass.playback;

import java.util.List;

public class PerspectiveReplayPage {
    private final List<Event> items;
    private final long nextSequence;
    private final boolean hasMore;

    public PerspectiveReplayPage(List<Event> items, long nextSequence, boolean hasMore) {
        this.items = List.copyOf(items);
        this.nextSequence = nextSequence;
        this.hasMore = hasMore;
    }

    public List<Event> getItems() { return items; }
    public long getNextSequence() { return nextSequence; }
    public boolean isHasMore() { return hasMore; }

    public static class Event {
        private final long sequence;
        private final String messageId;
        private final String payload;

        public Event(long sequence, String messageId, String payload) {
            this.sequence = sequence; this.messageId = messageId; this.payload = payload;
        }
        public long getSequence() { return sequence; }
        public String getMessageId() { return messageId; }
        public String getPayload() { return payload; }
    }
}
