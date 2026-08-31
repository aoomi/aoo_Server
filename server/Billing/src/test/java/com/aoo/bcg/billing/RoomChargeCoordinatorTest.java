package com.aoo.bcg.billing;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RoomChargeCoordinatorTest {
    @Test void lockedHostChargeUsesOneInstantAndRefundsUnusedRounds(){
        var billing=billing(Map.of(1L,100L));var account=new CurrencyAccount(1,"ROOM_CARD");
        var coordinator=new RoomChargeCoordinator(billing,Clock.fixed(Instant.EPOCH,ZoneOffset.UTC));
        var policy=policy(account,BillingPolicySnapshot.PaymentSubject.HOST,BillingPolicySnapshot.ChargePoint.FIRST_ROUND,BillingPolicySnapshot.RefundPolicy.UNUSED_ROUNDS,30,8,2,4);
        var opened=coordinator.open(new RoomChargeCoordinator.RoomChargeCommand(10,policy,Map.of(account,30L)));
        assertEquals(70,billing.balance(account));assertEquals(policy.fingerprint(),opened.display().policyFingerprint());
        coordinator.provisioned(10,RoomChargeCoordinator.ProvisionStep.ROOM_REGISTERED);
        coordinator.provisioned(10,RoomChargeCoordinator.ProvisionStep.ROUTE_BOUND);
        coordinator.provisioned(10,RoomChargeCoordinator.ProvisionStep.TABLE_PUSHED);
        assertThrows(IllegalStateException.class,()->coordinator.chargeAt(10,BillingPolicySnapshot.ChargePoint.CREATE));
        coordinator.chargeAt(10,BillingPolicySnapshot.ChargePoint.FIRST_ROUND);
        var cancelled=coordinator.cancel(10,4,"VOTE_DISMISS");
        assertEquals(RoomChargeCoordinator.ChargeStatus.PARTIALLY_REFUNDED,cancelled.charges().get(1L).status());
        assertEquals(15,cancelled.charges().get(1L).refunded());assertEquals(85,billing.balance(account));
        assertEquals(85,billing.balance(account));assertEquals(cancelled,coordinator.cancel(10,4,"VOTE_DISMISS"));
    }
    @Test void provisioningFailureAndUnstartedDismissRestoreEveryReservation(){
        var billing=billing(Map.of(1L,100L));var account=new CurrencyAccount(1,"ROOM_CARD");
        var coordinator=new RoomChargeCoordinator(billing,Clock.systemUTC());var policy=policy(account,BillingPolicySnapshot.PaymentSubject.HOST,BillingPolicySnapshot.ChargePoint.CREATE,BillingPolicySnapshot.RefundPolicy.NONE,30,8,2,4);
        coordinator.open(new RoomChargeCoordinator.RoomChargeCommand(11,policy,Map.of(account,30L)));
        var result=coordinator.creationFailed(11,RoomChargeCoordinator.ProvisionStep.ROOM_REGISTERED,"ROUTE_TIMEOUT");
        assertEquals(RoomChargeCoordinator.RoomStatus.COMPENSATED,result.status());assertEquals(100,billing.balance(account));
    }
    @Test void aaPlayersCanLeaveOrExpireBeforeChargeAndMinimumIsExplicit(){
        var billing=billing(Map.of(1L,100L,2L,100L,3L,100L));var clock=new MutableClock(Instant.EPOCH);
        var a1=new CurrencyAccount(1,"ROOM_CARD");var a2=new CurrencyAccount(2,"ROOM_CARD");
        var coordinator=new RoomChargeCoordinator(billing,clock);var policy=policy(a1,BillingPolicySnapshot.PaymentSubject.AA,BillingPolicySnapshot.ChargePoint.ROOM_FULL,BillingPolicySnapshot.RefundPolicy.UNUSED_ROUNDS,30,8,2,3);
        coordinator.open(new RoomChargeCoordinator.RoomChargeCommand(12,policy,Map.of(a1,10L,a2,10L)));
        assertTrue(coordinator.ready(12));coordinator.leaveAa(12,2,"PLAYER_LEFT");assertFalse(coordinator.ready(12));assertEquals(100,billing.balance(a2));
        clock.now=Instant.EPOCH.plus(Duration.ofMinutes(6));var expired=coordinator.releaseExpired();assertEquals(1,expired.size());assertEquals(100,billing.balance(a1));
    }
    @Test void winnerIsSelectedAndChargedOnlyAtFinish(){
        var billing=billing(Map.of(1L,100L,2L,100L));var template=new CurrencyAccount(1,"ROOM_CARD");var winner=new CurrencyAccount(2,"ROOM_CARD");
        var coordinator=new RoomChargeCoordinator(billing,Clock.systemUTC());var policy=policy(template,BillingPolicySnapshot.PaymentSubject.WINNER,BillingPolicySnapshot.ChargePoint.FINISH,BillingPolicySnapshot.RefundPolicy.NONE,20,8,2,4);
        coordinator.open(new RoomChargeCoordinator.RoomChargeCommand(13,policy,Map.of()));
        coordinator.provisioned(13,RoomChargeCoordinator.ProvisionStep.ROOM_REGISTERED);coordinator.provisioned(13,RoomChargeCoordinator.ProvisionStep.ROUTE_BOUND);coordinator.provisioned(13,RoomChargeCoordinator.ProvisionStep.TABLE_PUSHED);
        var result=coordinator.chargeWinnerAtFinish(13,winner);assertEquals(RoomChargeCoordinator.RoomStatus.SETTLED,result.status());assertEquals(80,billing.balance(winner));
        coordinator.chargeWinnerAtFinish(13,winner);assertEquals(80,billing.balance(winner));
    }
    @Test void insufficientAaReservationCompensatesEarlierPlayersAndFailedReleaseCanRetry(){
        var base=billing(Map.of(1L,100L,2L,100L));var a1=new CurrencyAccount(1,"ROOM_CARD");var a2=new CurrencyAccount(2,"ROOM_CARD");var policy=policy(a1,BillingPolicySnapshot.PaymentSubject.AA,BillingPolicySnapshot.ChargePoint.ROOM_FULL,BillingPolicySnapshot.RefundPolicy.UNUSED_ROUNDS,30,8,2,3);var debitCalls=new java.util.concurrent.atomic.AtomicInteger();
        BillingService insufficient=new BillingService(){@Override public LedgerEntry debit(String id,long player,String currency,long amount,String reason){if(debitCalls.incrementAndGet()==2)throw new IllegalStateException("insufficient balance");return base.debit(id,player,currency,amount,reason);}@Override public LedgerEntry credit(String id,long player,String currency,long amount,String reason){return base.credit(id,player,currency,amount,reason);}};
        var coordinator=new RoomChargeCoordinator(insufficient,Clock.systemUTC());assertThrows(IllegalStateException.class,()->coordinator.open(new RoomChargeCoordinator.RoomChargeCommand(14,policy,Map.of(a1,10L,a2,10L))));assertEquals(100,base.balance(a1));assertEquals(100,base.balance(a2));assertEquals(RoomChargeCoordinator.RoomStatus.COMPENSATED,coordinator.find(14).orElseThrow().status());
        var failures=new java.util.concurrent.atomic.AtomicInteger(1);BillingService flaky=new BillingService(){
            @Override public LedgerEntry debit(String id,long player,String currency,long amount,String reason){return base.debit(id,player,currency,amount,reason);}
            @Override public LedgerEntry credit(String id,long player,String currency,long amount,String reason){if(reason.equals("ROOM_RELEASE")&&failures.getAndDecrement()>0)throw new IllegalStateException("injected");return base.credit(id,player,currency,amount,reason);}
        };
        var host=new RoomChargeCoordinator(flaky,Clock.systemUTC());var hostPolicy=policy(a1,BillingPolicySnapshot.PaymentSubject.HOST,BillingPolicySnapshot.ChargePoint.CREATE,BillingPolicySnapshot.RefundPolicy.NONE,20,8,2,4);host.open(new RoomChargeCoordinator.RoomChargeCommand(15,hostPolicy,Map.of(a1,20L)));
        assertEquals(RoomChargeCoordinator.RoomStatus.COMPENSATION_PENDING,host.creationFailed(15,RoomChargeCoordinator.ProvisionStep.ROOM_REGISTERED,"REGISTER_FAIL").status());assertEquals(80,base.balance(a1));assertEquals(RoomChargeCoordinator.RoomStatus.COMPENSATED,host.retryCompensation(15).status());assertEquals(100,base.balance(a1));
    }
    private static InMemoryBillingService billing(Map<Long,Long> balances){return new InMemoryBillingService(new InMemoryLedgerRepository(),Clock.systemUTC(),balances,"ROOM_CARD");}
    private static BillingPolicySnapshot policy(CurrencyAccount account,BillingPolicySnapshot.PaymentSubject subject,BillingPolicySnapshot.ChargePoint point,BillingPolicySnapshot.RefundPolicy refund,long amount,int rounds,int min,int max){return BillingPolicySnapshot.lock("policy-1",1001,"product-1",account,subject,point,refund,rounds,min,max,amount,0,Duration.ofMinutes(5),7,Instant.EPOCH);}
    private static final class MutableClock extends Clock{private Instant now;private MutableClock(Instant now){this.now=now;}@Override public ZoneId getZone(){return ZoneOffset.UTC;}@Override public Clock withZone(ZoneId zone){return this;}@Override public Instant instant(){return now;}}
}
