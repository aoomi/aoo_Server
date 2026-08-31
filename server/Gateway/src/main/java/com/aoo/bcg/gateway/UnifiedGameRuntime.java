package com.aoo.bcg.gateway;

import com.aoo.bcg.common.idempotency.IdempotencyStore;
import com.aoo.bcg.common.settlement.SettlementBalancePolicy;
import com.aoo.bcg.common.settlement.SettlementEntry;
import com.aoo.bcg.common.settlement.SettlementExecutor;
import com.aoo.bcg.common.settlement.SettlementResult;
import com.aoo.bcg.common.settlement.SettlementScope;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.aoo.bcg.gamespi.GameCommandCommitter;
import com.aoo.bcg.gamespi.GameRegistry;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.RoomCreationContext;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/** Production orchestration boundary. No transport may keep a separate game state. */
public final class UnifiedGameRuntime {
    private final GameRegistry games;
    private final RuntimeGameRoomRegistry rooms;
    private final GameWebSocketRouter router;
    private final SettlementExecutor settlements;

    public UnifiedGameRuntime(GameRegistry games, RuntimeGameRoomRegistry rooms,
            IdempotencyStore<GameCommandResult> idempotency, SettlementExecutor settlements,
            Clock clock, Duration requestSkew, Duration idempotencyRetention) {
        this(games, rooms, idempotency, settlements, clock, requestSkew, idempotencyRetention,
                GameCommandCommitter.noOp());
    }

    public UnifiedGameRuntime(GameRegistry games, RuntimeGameRoomRegistry rooms,
            IdempotencyStore<GameCommandResult> idempotency, SettlementExecutor settlements,
            Clock clock, Duration requestSkew, Duration idempotencyRetention,
                               GameCommandCommitter runtimeCommitter) {
        this(games,rooms,idempotency,settlements,clock,requestSkew,idempotencyRetention,runtimeCommitter,null);
    }

    public UnifiedGameRuntime(GameRegistry games, RuntimeGameRoomRegistry rooms,
            IdempotencyStore<GameCommandResult> idempotency, SettlementExecutor settlements,
            Clock clock, Duration requestSkew, Duration idempotencyRetention,
            GameCommandCommitter runtimeCommitter,RiskAdmissionAuthority risk) {
        this.games = Objects.requireNonNull(games, "games");
        this.rooms = Objects.requireNonNull(rooms, "rooms");
        this.settlements = Objects.requireNonNull(settlements, "settlements");
        this.router = new GameWebSocketRouter(games, rooms,
                new WebSocketRequestGuard(clock, requestSkew), idempotency, idempotencyRetention,
                runtimeCommitter,new com.aoo.bcg.gamespi.time.AuthoritativeTimeSource(clock),risk);
    }

    public GameRoomHandle createRoom(int gameId, RoomCreationContext context) {
        return rooms.create(games.require(gameId), context);
    }

    public GameRoomHandle bindRoom(GameRoomHandle room) { return rooms.bind(room); }
    public GameWebSocketRouter router() { return router; }
    public void removeRoom(long roomId) { rooms.remove(roomId); }
    public GameWebSocketRouter.RoutedResult route(ConnectionSession session, WebSocketFrame frame) {
        return router.route(session, frame);
    }
    public GameWebSocketRouter.RequestOutcome queryOutcome(ConnectionSession session,WebSocketFrame frame){return router.queryOutcome(session,frame);}

    public SettlementResult settleRound(long roomId, int roundNo) {
        GameRoomHandle room = rooms.require(roomId);
        var payload = games.require(room.gameId(),room.playVersion()).settlementProvider()
                .orElseThrow(() -> new IllegalStateException("settlement provider unavailable"))
                .settle(room, roundNo);
        var entries = payload.scoreDelta().entrySet().stream()
                .map(entry -> new SettlementEntry(entry.getKey(), entry.getValue(), Map.of("game", entry.getValue())))
                .toList();
        return settlements.execute(SettlementScope.ROUND, SettlementBalancePolicy.ZERO_SUM,
                roomId, roundNo, room.playVersion(),
                new SettlementResult(roomId, roundNo, room.playVersion(), entries));
    }
}
