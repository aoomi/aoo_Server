package com.aoo.bcg.common.club;

import com.aoo.bcg.common.persistence.DriverManagerDataSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "AOO_DB_IT_URL", matches = ".+")
class JdbcClubMemberIndexIntegrationTest {
    @Test void pagesByCursorAndSelectsOnlyAuthorizedOnlineAudience() {
        var dataSource = new DriverManagerDataSource(System.getenv("AOO_DB_IT_URL"),
                System.getenv("AOO_DB_IT_USER"), System.getenv("AOO_DB_IT_PASSWORD"));
        var index = new JdbcClubMemberIndex(dataSource, new ObjectMapper(), Clock.systemUTC());
        long clubId = System.currentTimeMillis();
        Instant now = Instant.now();
        for (long playerId = 1; playerId <= 5; playerId++) index.upsert(clubId, playerId,
                new ClubMemberRecord(clubId, playerId, ClubMemberRecord.Status.ACTIVE,
                        playerId == 1 ? ClubMemberRecord.Role.OWNER : ClubMemberRecord.Role.MEMBER,
                        playerId != 5, Map.of("name", "p" + playerId), now, now));
        var first = index.page(clubId, null, 2);
        assertEquals(2, first.items().size()); assertTrue(first.hasMore());
        var second = index.page(clubId, first.nextCursor(), 2);
        assertEquals(java.util.List.of(3L, 4L), second.items().stream().map(ClubMemberRecord::playerId).toList());
        assertEquals(java.util.List.of(1L), index.onlinePlayerIds(clubId,
                ClubMemberIndex.MemberAudience.MANAGERS, 1000, null));
        index.remove(clubId, 3);
        assertEquals(4, index.page(clubId, null, 200).items().size());
    }
}
