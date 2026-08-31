package jsproto.c2s.cclass.club;

import java.util.List;

public class ClubMemberCursorPage {
    private final List<ClubPlayerInfo> items;
    private final long nextMemberId;
    private final boolean hasMore;

    public ClubMemberCursorPage(List<ClubPlayerInfo> items, long nextMemberId, boolean hasMore) {
        this.items = List.copyOf(items);
        this.nextMemberId = nextMemberId;
        this.hasMore = hasMore;
    }

    public List<ClubPlayerInfo> getItems() { return items; }
    public long getNextMemberId() { return nextMemberId; }
    public boolean isHasMore() { return hasMore; }
}
