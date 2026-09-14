package business.global.pk.zypk;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Immutable reconnect/state view already cropped for one authenticated player. */
public record ZYPKPlayerView(long roomId, ZYPKTable.State state, int viewerSeat,
                             int dealerSeat, int operatorSeat, int currentBet,
                             int remainingCardCount, Map<Integer, SeatView> seats) {
    public ZYPKPlayerView {
        seats = Map.copyOf(seats);
    }

    public record SeatView(int seatId, long playerId, List<Integer> cards, int cardCount,
                           int wager, int multiplier, int chips, boolean viewed, boolean revealed,
                           boolean folded, Set<Integer> visibleSeats) {
        public SeatView {
            cards = List.copyOf(cards);
            visibleSeats = Set.copyOf(visibleSeats);
        }
    }
}
