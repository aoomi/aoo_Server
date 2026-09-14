package com.aoo.bcg.common.club;

import java.util.List;
import java.util.Optional;

public interface ClubMemberIndex<M> {
    Optional<M> find(long clubId, long playerId);
    CursorPage<M> page(long clubId, String cursor, int limit);
    List<Long> onlinePlayerIds(long clubId, MemberAudience audience, int batchSize, String cursor);
    void upsert(long clubId, long playerId, M member);
    void remove(long clubId, long playerId);

    record CursorPage<M>(List<M> items, String nextCursor, boolean hasMore) {
        public CursorPage { items = List.copyOf(items); if(hasMore&&(items.isEmpty()||nextCursor==null||nextCursor.isBlank()))throw new IllegalArgumentException("non-terminal page requires items and cursor");if(!hasMore&&nextCursor!=null)throw new IllegalArgumentException("terminal page cannot expose cursor"); }
    }
    enum MemberAudience { ALL, MANAGERS }
}
