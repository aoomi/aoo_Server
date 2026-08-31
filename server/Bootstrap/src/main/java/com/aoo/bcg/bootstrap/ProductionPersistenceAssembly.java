package com.aoo.bcg.bootstrap;

import com.aoo.bcg.billing.BillingService;
import com.aoo.bcg.billing.JdbcBillingService;
import com.aoo.bcg.billing.JdbcLedgerRepository;
import com.aoo.bcg.billing.LedgerRepository;
import com.aoo.bcg.common.event.JdbcOutboxRepository;
import com.aoo.bcg.common.event.JdbcRoomEventJournal;
import com.aoo.bcg.common.event.OutboxRepository;
import com.aoo.bcg.common.event.RoomEventJournal;
import com.aoo.bcg.common.idempotency.IdempotencyStore;
import com.aoo.bcg.common.idempotency.JdbcIdempotencyStore;
import com.aoo.bcg.common.persistence.PersistenceBinding;
import com.aoo.bcg.common.persistence.PersistenceCapability;
import com.aoo.bcg.common.persistence.PersistenceReadiness;
import com.aoo.bcg.common.recovery.JdbcRoomLeaseStore;
import com.aoo.bcg.common.recovery.JdbcRoomSnapshotStore;
import com.aoo.bcg.common.recovery.RoomLeaseStore;
import com.aoo.bcg.common.recovery.RoomSnapshotStore;
import com.aoo.bcg.config.GameConfigurationRepository;
import com.aoo.bcg.common.club.ClubMemberIndex;
import com.aoo.bcg.common.club.ClubMemberRecord;
import com.aoo.bcg.common.club.JdbcClubMemberIndex;
import com.aoo.bcg.common.settlement.JdbcSettlementRepository;
import com.aoo.bcg.common.settlement.SettlementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;

public record ProductionPersistenceAssembly(
        IdempotencyStore<Object> idempotency,
        RoomEventJournal roomEvents,
        RoomSnapshotStore roomSnapshots,
        RoomLeaseStore roomLeases,
        OutboxRepository outbox,
        GameConfigurationRepository gameConfigurations,
        LedgerRepository ledger,
        BillingService billing,
        SettlementRepository settlements,
        ClubMemberIndex<ClubMemberRecord> clubMembers,
        List<PersistenceBinding> bindings) {

    public ProductionPersistenceAssembly {
        bindings=List.copyOf(bindings);
        PersistenceReadiness.requireProductionReady(bindings);
    }

    public static ProductionPersistenceAssembly create(DataSource dataSource, ObjectMapper mapper,
                                                         GameConfigurationRepository gameConfigurations,
                                                         LongSupplier ledgerIdGenerator, Clock clock) {
        Objects.requireNonNull(dataSource,"dataSource"); Objects.requireNonNull(mapper,"mapper");
        Objects.requireNonNull(gameConfigurations,"gameConfigurations"); Objects.requireNonNull(ledgerIdGenerator,"ledgerIdGenerator"); Objects.requireNonNull(clock,"clock");
        var idempotency=new JdbcIdempotencyStore<>(dataSource,mapper,Object.class,clock);
        var roomEvents=new JdbcRoomEventJournal(dataSource,mapper);
        var snapshots=new JdbcRoomSnapshotStore(dataSource,mapper);
        var leases=new JdbcRoomLeaseStore(dataSource,clock);
        var outbox=new JdbcOutboxRepository(dataSource,mapper);
        var ledger=new JdbcLedgerRepository(dataSource,ledgerIdGenerator);
        var billing=new JdbcBillingService(dataSource,ledgerIdGenerator,clock);
        var settlements=new JdbcSettlementRepository(dataSource,mapper,ledgerIdGenerator,clock);
        var clubMembers=new JdbcClubMemberIndex(dataSource,mapper,clock);
        List<PersistenceBinding> bindings=List.of(
                PersistenceBinding.production(PersistenceCapability.IDEMPOTENCY,idempotency.getClass()),
                PersistenceBinding.production(PersistenceCapability.ROOM_EVENT_JOURNAL,roomEvents.getClass()),
                PersistenceBinding.production(PersistenceCapability.ROOM_SNAPSHOT,snapshots.getClass()),
                PersistenceBinding.production(PersistenceCapability.ROOM_LEASE,leases.getClass()),
                PersistenceBinding.production(PersistenceCapability.OUTBOX,outbox.getClass()),
                PersistenceBinding.production(PersistenceCapability.GAME_CONFIGURATION,gameConfigurations.getClass()),
                PersistenceBinding.production(PersistenceCapability.BILLING_LEDGER,ledger.getClass()),
                PersistenceBinding.production(PersistenceCapability.SETTLEMENT,settlements.getClass()),
                PersistenceBinding.production(PersistenceCapability.CLUB_MEMBER_INDEX,clubMembers.getClass()));
        return new ProductionPersistenceAssembly(idempotency,roomEvents,snapshots,leases,outbox,
                gameConfigurations,ledger,billing,settlements,clubMembers,bindings);
    }
}
