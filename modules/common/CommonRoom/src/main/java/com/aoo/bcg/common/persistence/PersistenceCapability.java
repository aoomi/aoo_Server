package com.aoo.bcg.common.persistence;

public enum PersistenceCapability {
    IDEMPOTENCY,
    ROOM_EVENT_JOURNAL,
    ROOM_SNAPSHOT,
    ROOM_LEASE,
    OUTBOX,
    GAME_CONFIGURATION,
    BILLING_LEDGER,
    SETTLEMENT,
    CLUB_MEMBER_INDEX
}
