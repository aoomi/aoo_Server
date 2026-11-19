package com.aoo.bcg.billing;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryBillingServiceTest {
    @Test void duplicateDebitReturnsOriginalEntryWithoutChargingAgain() {
        InMemoryBillingService service = new InMemoryBillingService(new InMemoryLedgerRepository(), Clock.systemUTC(), Map.of(7L, 100L), "ROOM_CARD");
        LedgerEntry first = service.debit("room-1", 7, "ROOM_CARD", 30, "CREATE_ROOM");
        LedgerEntry duplicate = service.debit("room-1", 7, "ROOM_CARD", 30, "CREATE_ROOM");
        assertSame(first, duplicate);
        assertEquals(70, service.balance(7, "ROOM_CARD"));
    }
    @Test void insufficientBalanceDoesNotChangeBalance() {
        InMemoryBillingService service = new InMemoryBillingService(new InMemoryLedgerRepository(), Clock.systemUTC(), Map.of(7L, 10L), "ROOM_CARD");
        assertThrows(IllegalStateException.class, () -> service.debit("room-2", 7, "ROOM_CARD", 20, "CREATE_ROOM"));
        assertEquals(10, service.balance(7, "ROOM_CARD"));
    }
    @Test void businessIdCannotBeReusedWithDifferentCommandAndBeforeBalanceIsImmutable() {
        InMemoryBillingService service = new InMemoryBillingService(new InMemoryLedgerRepository(), Clock.systemUTC(), Map.of(7L, 100L), "ROOM_CARD");
        LedgerEntry entry=service.debit("room-3",7,"ROOM_CARD",20,"CREATE_ROOM");
        assertEquals(100,entry.balanceBefore());
        assertThrows(IllegalArgumentException.class,()->service.debit("room-3",7,"ROOM_CARD",21,"CREATE_ROOM"));
        assertEquals(80,service.balance(7,"ROOM_CARD"));
    }
}
