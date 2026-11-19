package core.server;

import com.aoo.bcg.common.idempotency.JdbcIdempotencyStore;
import com.aoo.bcg.common.event.JdbcRoomEventJournal;
import com.aoo.bcg.common.event.JdbcOutboxRepository;
import com.aoo.bcg.common.recovery.JdbcRoomLeaseStore;
import com.aoo.bcg.common.recovery.JdbcRoomSnapshotStore;
import com.aoo.bcg.common.settlement.JdbcSettlementRepository;
import com.aoo.bcg.common.settlement.SettlementExecutor;
import com.aoo.bcg.common.readiness.ServiceReadinessGate;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.time.Clock;
import java.util.Objects;
import java.util.List;
import java.util.function.LongSupplier;

/** Minimal production persistence assembly owned by each legacy game node. */
record ProductionPersistenceAssembly(
        JdbcIdempotencyStore<GameCommandResult> idempotency,
        JdbcRoomEventJournal roomEvents,
        JdbcRoomSnapshotStore roomSnapshots,
        JdbcRoomLeaseStore roomLeases,
        JdbcOutboxRepository outbox,
        SettlementExecutor settlements) {

    static ProductionPersistenceAssembly create(DataSource dataSource, ObjectMapper mapper,
                                                 LongSupplier ids, Clock clock) {
        Objects.requireNonNull(dataSource, "dataSource");
        Objects.requireNonNull(mapper, "mapper");
        Objects.requireNonNull(ids, "ids");
        Objects.requireNonNull(clock, "clock");
        ServiceReadinessGate readiness = new ServiceReadinessGate(List.of(
                ServiceReadinessGate.check("databaseConnectionAndSchema",
                        () -> ProductionSchemaReadiness.verify(dataSource))));
        readiness.verifyAndOpen();
        readiness.requireAcceptingTraffic();
        return new ProductionPersistenceAssembly(
                new JdbcIdempotencyStore<>(dataSource, mapper, GameCommandResult.class, clock),
                new JdbcRoomEventJournal(dataSource, mapper),
                new JdbcRoomSnapshotStore(dataSource, mapper),
                new JdbcRoomLeaseStore(dataSource, clock),
                new JdbcOutboxRepository(dataSource,mapper),
                new SettlementExecutor(new JdbcSettlementRepository(dataSource, mapper, ids, clock)));
    }
}
