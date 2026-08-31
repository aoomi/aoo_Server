package com.aoo.bcg.billing;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AssetGrantServiceTest {
    @Test void giftRecordsSourceTargetTaxLimitsRiskAndIdempotency(){
        var billing=billing();var repo=new Grants();var service=new AssetGrantService(billing,repo,ignored->AssetGrant.RiskDecision.ALLOW,Clock.systemUTC());
        var source=new CurrencyAccount(1,"ROOM_CARD");var target=new CurrencyAccount(2,"ROOM_CARD");var tax=new CurrencyAccount(3,"ROOM_CARD");
        var policy=new AssetGrantService.Policy(100,100,1000,tax);var command=new AssetGrantService.Command("gift-1",AssetGrant.Kind.GIFT,source,target,50,"PLAYER_GIFT",LocalDate.of(2026,8,24));
        var result=service.execute(command,policy);assertEquals(AssetGrant.State.COMPLETED,result.state());assertEquals(5,result.taxAmount());assertEquals(45,result.netAmount());
        assertEquals(50,billing.balance(source));assertEquals(45,billing.balance(target));assertEquals(5,billing.balance(tax));assertEquals(result,service.execute(command,policy));
        var overDaily=new AssetGrantService.Command("gift-2",AssetGrant.Kind.GIFT,source,target,60,"PLAYER_GIFT",command.businessDate());
        assertEquals(AssetGrant.State.REJECTED,service.execute(overDaily,policy).state());assertEquals(50,billing.balance(source));
    }
    @Test void reviewDoesNotMoveAssetsAndFailedTargetCreditCompensatesSource(){
        var base=billing();var repo=new Grants();var review=new AssetGrantService(base,repo,ignored->AssetGrant.RiskDecision.REVIEW,Clock.systemUTC());
        var command=new AssetGrantService.Command("reward-review",AssetGrant.Kind.REWARD,null,new CurrencyAccount(2,"ROOM_CARD"),20,"CAMPAIGN",LocalDate.now());
        assertEquals(AssetGrant.State.REVIEW,review.execute(command,new AssetGrantService.Policy(100,100,0,null)).state());assertEquals(0,base.balance(2,"ROOM_CARD"));
        BillingService failing=new BillingService(){
            @Override public LedgerEntry debit(String id,long player,String currency,long amount,String reason){return base.debit(id,player,currency,amount,reason);}
            @Override public LedgerEntry credit(String id,long player,String currency,long amount,String reason){if(id.endsWith("target-credit"))throw new IllegalStateException("injected");return base.credit(id,player,currency,amount,reason);}
        };
        var compensation=new AssetGrantService(failing,new Grants(),ignored->AssetGrant.RiskDecision.ALLOW,Clock.systemUTC());
        var gift=new AssetGrantService.Command("gift-fail",AssetGrant.Kind.GIFT,new CurrencyAccount(1,"ROOM_CARD"),new CurrencyAccount(2,"ROOM_CARD"),25,"PLAYER_GIFT",LocalDate.now());
        assertEquals(AssetGrant.State.COMPENSATED,compensation.execute(gift,new AssetGrantService.Policy(100,100,0,null)).state());assertEquals(100,base.balance(1,"ROOM_CARD"));assertEquals(0,base.balance(2,"ROOM_CARD"));
    }
    @Test void agentAllocationCannotCrossScopedLedgers(){
        var billing=new InMemoryBillingService(new InMemoryLedgerRepository(),Clock.systemUTC(),Map.of(),"ROOM_CARD");var a=new CurrencyAccount(1,"CITY_ROOM_CARD",510100);var b=new CurrencyAccount(2,"CITY_ROOM_CARD",510100);var other=new CurrencyAccount(2,"CITY_ROOM_CARD",320100);billing.seed(a,20);billing.seed(b,0);billing.seed(other,0);
        var service=new AssetGrantService(billing,new Grants(),ignored->AssetGrant.RiskDecision.ALLOW,Clock.systemUTC());var policy=new AssetGrantService.Policy(20,20,0,null);
        var good=new AssetGrantService.Command("agent-1",AssetGrant.Kind.AGENT_ALLOCATION,a,b,10,"AGENT",LocalDate.now());assertEquals(AssetGrant.State.COMPLETED,service.execute(good,policy).state());assertEquals(10,billing.balance(a));assertEquals(10,billing.balance(b));assertEquals(0,billing.balance(other));
        var bad=new AssetGrantService.Command("agent-2",AssetGrant.Kind.AGENT_ALLOCATION,a,other,1,"AGENT",LocalDate.now());assertThrows(IllegalArgumentException.class,()->service.execute(bad,policy));
    }
    private static InMemoryBillingService billing(){return new InMemoryBillingService(new InMemoryLedgerRepository(),Clock.systemUTC(),Map.of(1L,100L,2L,0L,3L,0L),"ROOM_CARD");}
    private static final class Grants implements AssetGrantRepository{
        private final Map<String,AssetGrant> values=new HashMap<>();private final Map<String,Long> limits=new HashMap<>();private final Map<String,String> reservations=new HashMap<>();
        @Override public synchronized Optional<AssetGrant> find(String id){return Optional.ofNullable(values.get(id));}
        @Override public synchronized boolean insert(AssetGrant grant){return values.putIfAbsent(grant.businessId(),grant)==null;}
        @Override public synchronized boolean replace(long expected,AssetGrant grant){AssetGrant current=values.get(grant.businessId());if(current==null||current.version()!=expected)return false;values.put(grant.businessId(),grant);return true;}
        @Override public synchronized boolean reserveDailyLimit(String key,LocalDate date,String business,long amount,long maximum){String reservation=key+"/"+date+"/"+business;if(reservations.containsKey(reservation))return true;String bucket=key+"/"+date;long next=Math.addExact(limits.getOrDefault(bucket,0L),amount);if(next>maximum)return false;limits.put(bucket,next);reservations.put(reservation,business);return true;}
    }
}
