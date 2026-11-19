package com.aoo.bcg.poker;

import com.aoo.bcg.gamespi.CommonRoomPublicSeat;
import com.aoo.bcg.gamespi.CommonRoomPublicView;
import com.aoo.bcg.gamespi.GameCommandRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PokerCommonRoomPublicViewTest {
    @Test void projectsCompleteAuthoritativeTwoThreeAndFourSeatViews() {
        for (int seatCount : List.of(2, 3, 4)) {
            long ownerId = 1000L + seatCount;
            PokerAuthoritativeSession session = new PokerAuthoritativeSession(
                    8000 + seatCount, ownerId, seatCount, 71,
                    new PaoDeKuaiFamily(PaoDeKuaiConfig.defaults()));
            int guestSeat = seatCount - 1;
            long guestId = ownerId + 100;
            if (seatCount > 2) {
                session.execute(join(session, guestSeat, guestId));
                session = withScores(session, Map.of(0, 12L, guestSeat, -12L));
            } else {
                session = withScores(session, Map.of(0, 12L));
            }

            CommonRoomPublicView owner = session.commonRoomPublicView(ownerId);
            assertEquals(seatCount, owner.seatCount());
            assertEquals(0, owner.localSeatId());
            assertEquals(seatCount, owner.seats().size());
            assertEquals(range(seatCount), owner.seats().stream()
                    .map(CommonRoomPublicSeat::seatId).toList());
            assertEquals(new CommonRoomPublicSeat(0, true, ownerId, null, null, 12L),
                    owner.seats().get(0));

            for (int seat = 1; seat < seatCount; seat++) {
                CommonRoomPublicSeat projected = owner.seats().get(seat);
                if (seatCount > 2 && seat == guestSeat) {
                    assertEquals(new CommonRoomPublicSeat(
                            seat, true, guestId, null, null, -12L), projected);
                } else {
                    assertEquals(new CommonRoomPublicSeat(
                            seat, false, null, null, null, null), projected);
                }
            }

            CommonRoomPublicView unknown = session.commonRoomPublicView(999_999L);
            assertEquals(-1, unknown.localSeatId());
            assertEquals(owner.seats(), unknown.seats());
            assertTrue(unknown.seats().stream().filter(seat -> !seat.occupied())
                    .allMatch(seat -> seat.playerId() == null
                            && seat.displayName() == null
                            && seat.avatarUrl() == null
                            && seat.score() == null));

            if (seatCount > 2) {
                assertEquals(guestSeat, session.commonRoomPublicView(guestId).localSeatId());
            }
        }
    }

    @Test void spiRejectsDuplicateAndOutOfRangePhysicalSeats() {
        CommonRoomPublicSeat occupied = new CommonRoomPublicSeat(0, true, 10L, null, null, 0L);
        CommonRoomPublicSeat vacant = new CommonRoomPublicSeat(1, false, null, null, null, null);
        assertThrows(IllegalArgumentException.class,
                () -> new CommonRoomPublicView(2, 0, List.of(occupied, occupied)));
        assertThrows(IllegalArgumentException.class,
                () -> new CommonRoomPublicView(2, 0, List.of(occupied,
                        new CommonRoomPublicSeat(2, false, null, null, null, null))));
        assertDoesNotThrow(() -> new CommonRoomPublicView(2, 0, List.of(occupied, vacant)));
    }

    private static PokerAuthoritativeSession withScores(
            PokerAuthoritativeSession session, Map<Integer, Long> scores) {
        Map<String, Object> state = new LinkedHashMap<>(session.authoritativeState());
        state.put("totalScores", new LinkedHashMap<>(scores));
        return PokerAuthoritativeSession.restore(state, session.family());
    }

    private static List<Integer> range(int seatCount) {
        List<Integer> values = new ArrayList<>(seatCount);
        for (int seat = 0; seat < seatCount; seat++) values.add(seat);
        return values;
    }

    private static GameCommandRequest join(
            PokerAuthoritativeSession session, int seat, long playerId) {
        return new GameCommandRequest(
                "join_req", "join-" + seat, seat, 8000L + session.commonRoomPublicView(1002).seatCount(),
                1, "pdk-v1", String.valueOf(playerId), seat, Map.of());
    }
}
