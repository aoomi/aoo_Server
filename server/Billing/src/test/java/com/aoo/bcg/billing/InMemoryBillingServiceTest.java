package com.aoo.bcg.billing;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
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
    @Test void scopedAccountsAreIsolatedAndConcurrentDebitsCannotOverspend() throws Exception {
        var service=new InMemoryBillingService(new InMemoryLedgerRepository(),Clock.systemUTC(),Map.of(),"ROOM_CARD");
        var cityA=new CurrencyAccount(9,"CITY_ROOM_CARD",510100);var cityB=new CurrencyAccount(9,"CITY_ROOM_CARD",320100);
        service.seed(cityA,50);service.seed(cityB,50);AtomicInteger succeeded=new AtomicInteger();
        try(var executor=Executors.newFixedThreadPool(12)){
            var tasks=java.util.stream.IntStream.range(0,100).<java.util.concurrent.Callable<Void>>mapToObj(i->()->{try{service.debit("city-a-"+i,cityA,1,"GAME");succeeded.incrementAndGet();}catch(IllegalStateException expected){}return null;}).toList();
            for(var future:executor.invokeAll(tasks))future.get();
        }
        assertEquals(50,succeeded.get());assertEquals(0,service.balance(cityA));assertEquals(50,service.balance(cityB));
        service.credit("scoped-business",cityA,1,"GAME");
        assertThrows(IllegalArgumentException.class,()->service.credit("scoped-business",cityB,1,"GAME"));
    }
}
