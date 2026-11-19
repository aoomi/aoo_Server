package com.aoo.bcg.poker.cx;

import static org.junit.jupiter.api.Assertions.*;

import com.aoo.bcg.gamespi.CommonRoomPublicSeat;
import com.aoo.bcg.gamespi.CommonRoomPublicView;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CxCommonRoomPublicViewTest {
    @Test
    void projectsEveryPhysicalSeatAndOnlyOccupiedAuthorityFields() {
        CxSession session = new CxSession(7401L, 101L, 17L,
                CxRules.from(Map.of("startPlayers", 6, "roundCount", 10)));
        session.sit(101L, 500L, "seat-101");
        session.sit(202L, 750L, "seat-202");

        CommonRoomPublicView spectator = session.commonRoomPublicView(999L);
        assertEquals(8, spectator.seatCount());
        assertEquals(-1, spectator.localSeatId());
        assertEquals(8, spectator.seats().size());
        for (int index = 0; index < 8; index++) assertEquals(index, spectator.seats().get(index).seatId());

        @SuppressWarnings("unchecked")
        Map<Integer, Long> players = (Map<Integer, Long>) session.authoritativeState().get("players");
        @SuppressWarnings("unchecked")
        Map<Integer, Long> scores = (Map<Integer, Long>) session.authoritativeState().get("scores");
        for (CommonRoomPublicSeat seat : spectator.seats()) {
            Long playerId = players.get(seat.seatId());
            if (playerId == null) {
                assertFalse(seat.occupied());
                assertNull(seat.playerId());
                assertNull(seat.score());
                assertEquals(Map.of("seatId", seat.seatId(), "occupied", false), seat.asMap());
            } else {
                assertTrue(seat.occupied());
                assertEquals(playerId, seat.playerId());
                assertEquals(scores.get(seat.seatId()), seat.score());
                assertNull(seat.displayName());
                assertNull(seat.avatarUrl());
            }
        }
        assertEquals(players.entrySet().stream().filter(entry -> entry.getValue() == 101L)
                .findFirst().orElseThrow().getKey(), session.commonRoomPublicView(101L).localSeatId());
    }
}
