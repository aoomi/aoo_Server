package com.aoo.bcg.common.event;

import org.junit.jupiter.api.Test;
import java.util.ConcurrentModificationException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JdbcRoomEventJournalCasContractTest {
    @Test void acceptsOnlyTheNextPersistedAuthorityRevision() {
        assertDoesNotThrow(() -> JdbcRoomEventJournal.requireCasTransition(false,0,0,0,1,0,1,1));
        assertDoesNotThrow(() -> JdbcRoomEventJournal.requireCasTransition(true,4,9,9,4,9,10,10));
        assertThrows(ConcurrentModificationException.class,
                () -> JdbcRoomEventJournal.requireCasTransition(true,4,10,10,4,9,10,10));
        assertThrows(ConcurrentModificationException.class,
                () -> JdbcRoomEventJournal.requireCasTransition(true,5,9,9,4,9,10,10));
        assertThrows(ConcurrentModificationException.class,
                () -> JdbcRoomEventJournal.requireCasTransition(true,4,9,9,4,9,11,11));
        assertThrows(ConcurrentModificationException.class,
                () -> JdbcRoomEventJournal.requireCasTransition(false,0,0,0,1,3,4,4));
    }
}
