package com.aoo.bcg.common.club;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import com.aoo.bcg.common.paging.CursorTraversalGuard;

public final class ClubNotificationService<M> {
    private final ClubMemberIndex<M> index;
    public ClubNotificationService(ClubMemberIndex<M> index) { this.index = index; }

    public void notifyOnline(long clubId, ClubMemberIndex.MemberAudience audience, int batchSize,
                             Function<Long, Map<String, Object>> immutableMessageFactory,
                             BiConsumer<Long, Map<String, Object>> sender) {
        if (batchSize < 1 || batchSize > 1000) throw new IllegalArgumentException("batchSize must be 1..1000");
        String cursor = null;
        var traversal = new CursorTraversalGuard(100_000);
        do {
            var playerIds = index.onlinePlayerIds(clubId, audience, batchSize, cursor);
            for (Long playerId : playerIds) sender.accept(playerId, Map.copyOf(immutableMessageFactory.apply(playerId)));
            String nextCursor = playerIds.size() < batchSize ? null : Long.toUnsignedString(playerIds.get(playerIds.size() - 1));
            traversal.accept(cursor, nextCursor, playerIds.size(), nextCursor != null);
            cursor = nextCursor;
        } while (cursor != null);
    }
}
