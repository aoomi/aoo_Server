package com.aoo.bcg.billing;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GoldTableSettlementServiceTest {
    @Test void buyInResultsFeesAndRetriesConserveAssets(){
        var billing=new InMemoryBillingService(new InMemoryLedgerRepository(),Clock.systemUTC(),Map.of(1L,200L,2L,200L),"GOLD");var service=new GoldTableSettlementService(billing,Clock.systemUTC());var a=new CurrencyAccount(1,"GOLD");var b=new CurrencyAccount(2,"GOLD");
        service.enter(10,a,100);service.enter(10,b,100);assertEquals(100,billing.balance(a));assertEquals(100,billing.balance(b));
        var settled=service.settle(10,Map.of(1L,40L,2L,-40L),1000,"v1");assertEquals(GoldTableSettlementService.Status.SETTLED,settled.status());assertEquals(4,settled.totalServiceFee());assertEquals(236,billing.balance(a));assertEquals(160,billing.balance(b));
        service.settle(10,Map.of(1L,40L,2L,-40L),1000,"v1");assertEquals(396,billing.balance(a)+billing.balance(b));
        assertThrows(IllegalArgumentException.class,()->service.settle(10,Map.of(1L,30L,2L,-40L),1000,"v2"));
    }
    @Test void invalidGameAbortReleasesBuyInsIncludingAbnormalExit(){
        var billing=new InMemoryBillingService(new InMemoryLedgerRepository(),Clock.systemUTC(),Map.of(1L,100L),"GOLD");var service=new GoldTableSettlementService(billing,Clock.systemUTC());var account=new CurrencyAccount(1,"GOLD");
        service.enter(11,account,80);assertEquals(20,billing.balance(account));assertEquals(GoldTableSettlementService.Status.ABORTED,service.abort(11,"NODE_FAILURE").status());assertEquals(100,billing.balance(account));service.abort(11,"NODE_FAILURE");assertEquals(100,billing.balance(account));
    }
    @Test void partialSettlementFailureRetriesWithStableLedgerIds(){
        var base=new InMemoryBillingService(new InMemoryLedgerRepository(),Clock.systemUTC(),Map.of(1L,100L,2L,100L),"GOLD");var failures=new java.util.concurrent.atomic.AtomicInteger(1);
        BillingService flaky=new BillingService(){@Override public LedgerEntry debit(String id,long player,String currency,long amount,String reason){return base.debit(id,player,currency,amount,reason);}@Override public LedgerEntry credit(String id,long player,String currency,long amount,String reason){if(player==2&&reason.equals("GOLD_SETTLE")&&failures.getAndDecrement()>0)throw new IllegalStateException("injected");return base.credit(id,player,currency,amount,reason);}};
        var service=new GoldTableSettlementService(flaky,Clock.systemUTC());service.enter(12,new CurrencyAccount(1,"GOLD"),50);service.enter(12,new CurrencyAccount(2,"GOLD"),50);
        assertThrows(IllegalStateException.class,()->service.settle(12,Map.of(1L,10L,2L,-10L),0,"v1"));assertEquals(GoldTableSettlementService.Status.SETTLEMENT_PENDING,service.find(12).orElseThrow().status());
        var settled=service.settle(12,Map.of(1L,10L,2L,-10L),0,"v1");assertEquals(GoldTableSettlementService.Status.SETTLED,settled.status());assertEquals(110,base.balance(1,"GOLD"));assertEquals(90,base.balance(2,"GOLD"));
    }
}
