package core.server;

import com.aoo.bcg.common.recovery.RoomLeaseStore;
import com.aoo.bcg.common.recovery.RoomSnapshotStore;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameRegistry;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gateway.RuntimeGameRoomRegistry;
import com.aoo.bcg.common.event.RoomEventJournal;
import com.aoo.bcg.gamespi.StatePayload;
import com.ddm.server.common.CommLogD;

import java.time.Clock;
import java.time.Duration;

/** Claims expired snapshots and rebuilds V2-only authoritative rooms at node startup. */
final class ProductionRoomRecoveryCoordinator {
    private final GameRegistry games;
    private final RuntimeGameRoomRegistry rooms;
    private final RoomSnapshotStore snapshots;
    private final RoomLeaseStore leases;
    private final Clock clock;
    private final String nodeId;
    private final RoomEdgeRuntimePolicy edgePolicy;
    private final RoomEventJournal events;

    ProductionRoomRecoveryCoordinator(GameRegistry games, RuntimeGameRoomRegistry rooms,
            RoomSnapshotStore snapshots, RoomLeaseStore leases, Clock clock, String nodeId) {
        this(games,rooms,snapshots,leases,null,clock,nodeId,RoomEdgeRuntimePolicy.production());
    }
    ProductionRoomRecoveryCoordinator(GameRegistry games, RuntimeGameRoomRegistry rooms,
            RoomSnapshotStore snapshots, RoomLeaseStore leases,RoomEventJournal events, Clock clock, String nodeId) {
        this(games,rooms,snapshots,leases,events,clock,nodeId,RoomEdgeRuntimePolicy.production());
    }
    ProductionRoomRecoveryCoordinator(GameRegistry games, RuntimeGameRoomRegistry rooms,
            RoomSnapshotStore snapshots, RoomLeaseStore leases, Clock clock, String nodeId,
            RoomEdgeRuntimePolicy edgePolicy) {
        this(games,rooms,snapshots,leases,null,clock,nodeId,edgePolicy);
    }
    private ProductionRoomRecoveryCoordinator(GameRegistry games, RuntimeGameRoomRegistry rooms,
            RoomSnapshotStore snapshots, RoomLeaseStore leases,RoomEventJournal events,Clock clock,String nodeId,
            RoomEdgeRuntimePolicy edgePolicy) {
        this.games=games; this.rooms=rooms; this.snapshots=snapshots; this.leases=leases;
        this.events=events;this.clock=clock; this.nodeId=nodeId; this.edgePolicy=edgePolicy;
    }

    int recoverExpired(int limit) {
        int restored=0;
        for (var snapshot : snapshots.recoverable(clock.instant(),limit)) {
            try {
                edgePolicy.validateRoomIdentity(snapshot.roomId());
                edgePolicy.fault("before-recovery-claim");
                GameProvider provider=games.require(snapshot.gameId(),snapshot.playVersion());
                var lease=leases.acquire(snapshot.roomId(),nodeId,RoomEdgeRuntimePolicy.LEASE_TTL);
                var state=StatePayload.copyOf(snapshot.authoritativeState());
                if(events!=null){var missing=events.after(snapshot.roomId(),snapshot.lastEventSequence());
                    if(!missing.isEmpty())state=provider.eventReplayProvider()
                            .orElseThrow(()->new IllegalStateException("provider event replay unavailable for "+snapshot.playVersion()))
                            .replay(state,missing);
                }
                var authority=provider.restoreAuthoritativeSession(state)
                        .orElseThrow(() -> new IllegalStateException("provider cannot restore authority"));
                if (!leases.isCurrent(lease)) throw new IllegalStateException("recovery lease lost");
                edgePolicy.fault("after-restore-before-bind");
                rooms.bind(new GameRoomHandle(snapshot.roomId(),snapshot.gameId(),snapshot.playVersion(),authority));
                restored++;
                CommLogD.info("Recovered authoritative roomId:{}, gameId:{}, fencingToken:{}",
                        snapshot.roomId(),snapshot.gameId(),lease.fencingToken());
            } catch (IllegalStateException | IllegalArgumentException race) {
                CommLogD.warn("Skipped room recovery roomId:{}, reason:{}",snapshot.roomId(),race.getMessage());
            }
        }
        return restored;
    }
}
