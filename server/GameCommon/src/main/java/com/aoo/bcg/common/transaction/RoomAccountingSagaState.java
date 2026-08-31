package com.aoo.bcg.common.transaction;

public enum RoomAccountingSagaState {
    NEW,
    GAME_APPLYING,
    GAME_APPLIED,
    ACCOUNTING_APPLYING,
    ACCOUNTING_APPLIED,
    FINALIZING,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    MANUAL_RECOVERY
}
