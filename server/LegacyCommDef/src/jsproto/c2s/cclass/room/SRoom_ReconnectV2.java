package jsproto.c2s.cclass.room;

import jsproto.c2s.cclass.BaseSendMsg;
import java.util.List;

public class SRoom_ReconnectV2 extends BaseSendMsg {
    private final Object viewerSnapshot;
    private final List<Event> events;
    private final long serverSeq;
    private final boolean hasMore;

    public SRoom_ReconnectV2(Object viewerSnapshot, List<Event> events, long serverSeq, boolean hasMore) {
        this.viewerSnapshot = viewerSnapshot;
        this.events = List.copyOf(events);
        this.serverSeq = serverSeq;
        this.hasMore = hasMore;
    }

    public Object getViewerSnapshot() { return viewerSnapshot; }
    public List<Event> getEvents() { return events; }
    public long getServerSeq() { return serverSeq; }
    public boolean isHasMore() { return hasMore; }

    public static class Event {
        private final long sequence;
        private final String messageId;
        private final String payload;

        public Event(long sequence, String messageId, String payload) {
            this.sequence = sequence;
            this.messageId = messageId;
            this.payload = payload;
        }

        public long getSequence() { return sequence; }
        public String getMessageId() { return messageId; }
        public String getPayload() { return payload; }
    }
}
