package core.server;

import java.sql.SQLTransientException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

/** Dependency-free fault matrix; run with assertions enabled. */
public final class RoomEdgeRuntimePolicySelfTest {
    private RoomEdgeRuntimePolicySelfTest() { }
    public static void main(String[] args) {
        Instant now=Instant.parse("2026-08-24T00:30:00Z");
        var active=new RoomEdgeRuntimePolicy.Lifecycle(now.minusSeconds(60),now,true,false,false);
        var healthy=new RoomEdgeRuntimePolicy.Dependencies(RoomEdgeRuntimePolicy.Availability.AVAILABLE,
                RoomEdgeRuntimePolicy.Availability.AVAILABLE,RoomEdgeRuntimePolicy.Availability.AVAILABLE);
        var policy=RoomEdgeRuntimePolicy.production();
        check(policy.decide(healthy,active,now).acceptMutation(),"healthy command");
        check(policy.decide(healthy,active,now).retainSeat(),"disconnect retains seat");

        var dbDown=new RoomEdgeRuntimePolicy.Dependencies(RoomEdgeRuntimePolicy.Availability.UNAVAILABLE,
                RoomEdgeRuntimePolicy.Availability.AVAILABLE,RoomEdgeRuntimePolicy.Availability.AVAILABLE);
        check(!policy.decide(dbDown,active,now).acceptMutation(),"database admission closes");
        var mqDown=new RoomEdgeRuntimePolicy.Dependencies(RoomEdgeRuntimePolicy.Availability.AVAILABLE,
                RoomEdgeRuntimePolicy.Availability.AVAILABLE,RoomEdgeRuntimePolicy.Availability.UNAVAILABLE);
        check(policy.decide(mqDown,active,now).deferBroadcast(),"confirmation can replay");
        var redisDown=new RoomEdgeRuntimePolicy.Dependencies(RoomEdgeRuntimePolicy.Availability.AVAILABLE,
                RoomEdgeRuntimePolicy.Availability.UNAVAILABLE,RoomEdgeRuntimePolicy.Availability.AVAILABLE);
        check(!policy.decide(redisDown,active,now).allowTakeover(),"no split brain takeover");

        check(policy.accountingDay(now,ZoneId.of("Asia/Shanghai")).equals("2026-08-24"),"business timezone");
        expectFailure(() -> policy.validateIdentity(Long.MAX_VALUE,1));
        expectFailure(() -> policy.validateIdentity(1,Integer.MAX_VALUE));
        expectFailure(() -> policy.validateCommandIdentity(1,2,1));
        var idle=new RoomEdgeRuntimePolicy.Lifecycle(now.minusSeconds(8*86400),now.minusSeconds(7*3600),false,false,false);
        check(!policy.decide(healthy,idle,now).allowNextRound(),"long idle expires");
        check(policy.banDecision(true)==RoomEdgeRuntimePolicy.BanDecision.FINISH_CURRENT_ROUND,"in-round ban");
        check(policy.retirementDecision(true,true)==RoomEdgeRuntimePolicy.RetirementDecision.REJECT_NEW_ROUND,"retired next round");

        AtomicInteger calls=new AtomicInteger();
        var injected=new RoomEdgeRuntimePolicy(point -> { if (calls.incrementAndGet()==1) throw new InjectedFailure(new SQLTransientException()); });
        expectFailure(() -> injected.fault("persistence-attempt-1"));
        check(RoomEdgeRuntimePolicy.retryable(new IllegalStateException(new RuntimeException(new SQLTransientException()))),"nested transient database failure");
        check(calls.get()==1,"fault injected exactly once");
        System.out.println("RoomEdgeRuntimePolicySelfTest: edge policy assertions PASS");
    }
    private static void check(boolean value,String label) { if (!value) throw new AssertionError(label); }
    private static void expectFailure(Runnable action) { try { action.run(); throw new AssertionError("expected failure"); } catch (IllegalArgumentException|InjectedFailure expected) { } }
    private static final class InjectedFailure extends RuntimeException { InjectedFailure(Throwable cause){super(cause);} }
}
