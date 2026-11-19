package core.club;

import java.util.List;

public record ClubMemberPage<T>(List<T> items, long nextMemberId, boolean hasMore) {
    public ClubMemberPage {
        items = List.copyOf(items);
        if (nextMemberId < 0) throw new IllegalArgumentException("invalid next cursor");
        if (items.isEmpty() && hasMore) throw new IllegalArgumentException("empty page cannot have more data");
    }
}
