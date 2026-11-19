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
    @Test void winnerPoliciesAndChargeOperationAreDisabledWithoutLedgerEffects(){
        var ledger=new InMemoryLedgerRepository();var billing=new InMemoryBillingService(ledger,Clock.systemUTC(),Map.of(1L,100L,2L,100L),"ROOM_CARD");
        var template=new CurrencyAccount(1,"ROOM_CARD");var winner=new CurrencyAccount(2,"ROOM_CARD");
        assertEquals(BillingPolicySnapshot.PaymentSubject.WINNER,BillingPolicySnapshot.PaymentSubject.valueOf("WINNER"));
        assertThrows(IllegalArgumentException.class,()->policy(template,BillingPolicySnapshot.PaymentSubject.WINNER,BillingPolicySnapshot.ChargePoint.FINISH,BillingPolicySnapshot.RefundPolicy.NONE,20,8,2,4));
        assertTrue(ledger.entries().isEmpty());
        var coordinator=new RoomChargeCoordinator(billing,Clock.systemUTC());var policy=policy(template,BillingPolicySnapshot.PaymentSubject.HOST,BillingPolicySnapshot.ChargePoint.FINISH,BillingPolicySnapshot.RefundPolicy.NONE,20,8,2,4);
        coordinator.open(new RoomChargeCoordinator.RoomChargeCommand(13,policy,Map.of(template,20L)));
        var before=ledger.entries();
        assertThrows(IllegalStateException.class,()->coordinator.chargeWinnerAtFinish(13,winner));
        assertEquals(before,ledger.entries());assertEquals(100,billing.balance(winner));assertEquals(80,billing.balance(template));
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
    @Test void aaPriceIsIndependentAndMissingOrPollutedPricesFailClosed(){
        var account=new CurrencyAccount(1,"ROOM_CARD");var policy=aaPolicy(account,7,3);
        assertEquals(7,policy.amountForAaPlayer());assertEquals(30,policy.display().amount());assertEquals(7,policy.display().aaPlayerAmount());
        assertNotEquals(policy.fingerprint(),aaPolicy(account,8,3).fingerprint());
        assertThrows(IllegalArgumentException.class,()->BillingPolicySnapshot.lock("p",1001,"room",account,BillingPolicySnapshot.PaymentSubject.AA,BillingPolicySnapshot.ChargePoint.ROOM_FULL,BillingPolicySnapshot.RefundPolicy.NONE,8,2,3,30,0,Duration.ofMinutes(5),1,Instant.EPOCH));
        assertThrows(IllegalArgumentException.class,()->BillingPolicySnapshot.lock("p",1001,"room",account,BillingPolicySnapshot.PaymentSubject.HOST,BillingPolicySnapshot.ChargePoint.ROOM_FULL,BillingPolicySnapshot.RefundPolicy.NONE,8,2,3,30,7,0,Duration.ofMinutes(5),1,Instant.EPOCH));
        var billing=billing(Map.of(1L,100L,2L,100L));var coordinator=new RoomChargeCoordinator(billing,Clock.systemUTC());
        coordinator.open(new RoomChargeCoordinator.RoomChargeCommand(20,policy,Map.of(account,7L)));
        coordinator.joinAa(20,new CurrencyAccount(2,"ROOM_CARD"));assertEquals(93,billing.balance(account));assertEquals(93,billing.balance(2,"ROOM_CARD"));
    }
    @Test void everyAaJoinChecksAssetsCapacityAndReleasedAttemptsBeforeDebit(){
        var ledger=new InMemoryLedgerRepository();var billing=new InMemoryBillingService(ledger,Clock.systemUTC(),Map.of(1L,100L,2L,100L,3L,100L),"ROOM_CARD");
        var a1=new CurrencyAccount(1,"ROOM_CARD");var a2=new CurrencyAccount(2,"ROOM_CARD");var c=new RoomChargeCoordinator(billing,Clock.systemUTC());
        c.open(new RoomChargeCoordinator.RoomChargeCommand(21,aaPolicy(a1,7,2),Map.of(a1,7L)));
        var entries=ledger.entries();
        assertThrows(IllegalArgumentException.class,()->c.joinAa(21,new CurrencyAccount(2,"CRYSTAL")));
        assertThrows(IllegalArgumentException.class,()->c.joinAa(21,new CurrencyAccount(2,"SPORTS_POINT",99)));
        assertThrows(IllegalArgumentException.class,()->c.joinAa(21,new CurrencyAccount(1,"CRYSTAL")));
        assertEquals(entries,ledger.entries());var joined=c.joinAa(21,a2);assertEquals(joined,c.joinAa(21,a2));
        assertThrows(IllegalStateException.class,()->c.joinAa(21,new CurrencyAccount(3,"ROOM_CARD")));assertEquals(100,billing.balance(3,"ROOM_CARD"));
        c.leaveAa(21,2,"LEAVE");assertEquals(100,billing.balance(a2));entries=ledger.entries();
        assertThrows(IllegalStateException.class,()->c.joinAa(21,a2));assertEquals(entries,ledger.entries());
        entries=ledger.entries();
        assertThrows(IllegalArgumentException.class,()->c.joinAa(21,new CurrencyAccount(4,"GOLD")));
        assertEquals(entries,ledger.entries());
    }
    @Test void confirmationRequiresAaMinimumAndReplayPreservesVersionAndTime(){
        var billing=billing(Map.of(1L,100L,2L,100L));var a1=new CurrencyAccount(1,"ROOM_CARD");var c=new RoomChargeCoordinator(billing,Clock.systemUTC());
        c.open(new RoomChargeCoordinator.RoomChargeCommand(23,aaPolicy(a1,7,3),Map.of(a1,7L)));provision(c,23);
        assertThrows(IllegalStateException.class,()->c.chargeAt(23,BillingPolicySnapshot.ChargePoint.ROOM_FULL));
        c.joinAa(23,new CurrencyAccount(2,"ROOM_CARD"));var first=c.chargeAt(23,BillingPolicySnapshot.ChargePoint.ROOM_FULL);
        assertEquals(first,c.chargeAt(23,BillingPolicySnapshot.ChargePoint.ROOM_FULL));assertEquals(93,billing.balance(a1));
        c.open(new RoomChargeCoordinator.RoomChargeCommand(24,aaPolicy(a1,7,3),Map.of(a1,7L)));provision(c,24);c.cancel(24,0,"CANCEL");
        assertThrows(IllegalStateException.class,()->c.chargeAt(24,BillingPolicySnapshot.ChargePoint.ROOM_FULL));
    }
    @Test void stableAttemptBindsDebitsReleasesAndRejectsDifferentCommands(){
        var ledger=new InMemoryLedgerRepository();var billing=new InMemoryBillingService(ledger,Clock.systemUTC(),Map.of(1L,100L),"ROOM_CARD");var a=new CurrencyAccount(1,"ROOM_CARD");var c=new RoomChargeCoordinator(billing,Clock.systemUTC());
        var p=policy(a,BillingPolicySnapshot.PaymentSubject.HOST,BillingPolicySnapshot.ChargePoint.CREATE,BillingPolicySnapshot.RefundPolicy.NONE,30,8,2,4);
        var command=new RoomChargeCoordinator.RoomChargeCommand(25,p,Map.of(a,30L),"create.request-1");var first=c.open(command);
        assertEquals(first,c.open(command));assertEquals("create.request-1",first.billingAttemptId());assertEquals(RoomBillingBusinessIds.reserve(25,1,"create.request-1"),first.charges().get(1L).reserveBusinessId());
        assertThrows(IllegalArgumentException.class,()->c.open(new RoomChargeCoordinator.RoomChargeCommand(25,p,Map.of(a,30L),"create.request-2")));
        assertThrows(IllegalArgumentException.class,()->c.open(new RoomChargeCoordinator.RoomChargeCommand(25,p,Map.of(a,29L),"create.request-1")));
        assertEquals(1,ledger.entries().size());c.creationFailed(25,RoomChargeCoordinator.ProvisionStep.ROOM_REGISTERED,"FAILED");
        assertEquals(100,billing.balance(a));assertTrue(ledger.findByBusinessId(RoomBillingBusinessIds.release(25,1,"create.request-1")).isPresent());
        assertNotEquals(RoomBillingBusinessIds.reserve(25,1,"loop.1"),RoomBillingBusinessIds.reserve(25,1,"loop.2"));
        assertEquals("room:25:player:1:reserve",RoomBillingBusinessIds.reserve(25,1));
        assertTrue(RoomBillingBusinessIds.partialRefund(Long.MAX_VALUE,Long.MAX_VALUE,Integer.MAX_VALUE,"x".repeat(48)).length()<=128);
        assertThrows(IllegalArgumentException.class,()->new RoomChargeCoordinator.RoomChargeCommand(26,p,Map.of(a,30L),"x".repeat(49)));
    }
    private static void provision(RoomChargeCoordinator c,long room){c.provisioned(room,RoomChargeCoordinator.ProvisionStep.ROOM_REGISTERED);c.provisioned(room,RoomChargeCoordinator.ProvisionStep.ROUTE_BOUND);c.provisioned(room,RoomChargeCoordinator.ProvisionStep.TABLE_PUSHED);}
    private static BillingPolicySnapshot aaPolicy(CurrencyAccount account,long aa,int maximum){return BillingPolicySnapshot.lock("policy-aa",1001,"product-aa",account,BillingPolicySnapshot.PaymentSubject.AA,BillingPolicySnapshot.ChargePoint.ROOM_FULL,BillingPolicySnapshot.RefundPolicy.NONE,8,2,maximum,30,aa,0,Duration.ofMinutes(5),7,Instant.EPOCH);}
    private static BillingPolicySnapshot policy(CurrencyAccount account,BillingPolicySnapshot.PaymentSubject subject,BillingPolicySnapshot.ChargePoint point,BillingPolicySnapshot.RefundPolicy refund,long amount,int rounds,int min,int max){return subject==BillingPolicySnapshot.PaymentSubject.AA?BillingPolicySnapshot.lock("policy-1",1001,"product-1",account,subject,point,refund,rounds,min,max,amount,10L,0,Duration.ofMinutes(5),7,Instant.EPOCH):BillingPolicySnapshot.lock("policy-1",1001,"product-1",account,subject,point,refund,rounds,min,max,amount,0,Duration.ofMinutes(5),7,Instant.EPOCH);}
    private static final class MutableClock extends Clock{private Instant now;private MutableClock(Instant now){this.now=now;}@Override public ZoneId getZone(){return ZoneOffset.UTC;}@Override public Clock withZone(ZoneId zone){return this;}@Override public Instant instant(){return now;}}
}
