package com.aoo.bcg.gamespi;

/** Runtime injection point used after both new-room creation and durable restore. */
public interface RoomScoreLedgerAware {
    void attachRoomScoreLedger(RoomScoreLedger ledger);
}
