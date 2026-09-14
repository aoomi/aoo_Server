package core.server;

import com.aoo.bcg.common.event.RoomEventIdentity;
import com.aoo.bcg.common.event.RoomEventJournal;
import com.aoo.bcg.common.recovery.*;
import com.aoo.bcg.gamespi.*;
import com.aoo.bcg.gamespi.time.OperationDeadline;
import com.aoo.bcg.gamespi.time.OperationDeadlineArbiter;
import com.aoo.bcg.gateway.RuntimeGameRoomRegistry;
import java.time.*;
import java.util.*;

/** Executable dependency-fault tests for the actual production committer/recovery classes. */
public final class ProductionRoomEdgeFaultSelfTest {
    private static final Instant NOW=Instant.parse("2026-08-24T00:00:00Z");
    public static void main(String[] args) {
        atomicSuccess(); unknownResultConfirmed(); identityMismatchFails(); confirmationFieldNegatives(); roomMismatchFails(); nonJdbcRejected();
        recoverySuccessAndRaces();
        System.out.println("ProductionRoomEdgeFaultSelfTest: production dependency faults PASS");
    }
    private static void atomicSuccess() {
        Store store=new Store(); AtomicFake atomic=new AtomicFake(store,false,false);
        committer(atomic,store,new Leases(true)).commit(room(7),request(7,"r1"),result("r1"));
        check(atomic.calls==1 && store.latest(7).isPresent(),"atomic success");
    }
    private static void unknownResultConfirmed() {
        Store store=new Store(); AtomicFake atomic=new AtomicFake(store,true,false);
        committer(atomic,store,new Leases(true)).commit(room(8),request(8,"r2"),result("r2"));
        check(atomic.calls==1,"unknown result confirmed without retry");
    }
    private static void identityMismatchFails() {
        Store store=new Store(); AtomicFake atomic=new AtomicFake(store,true,true);
        expect(RuntimeException.class,()->committer(atomic,store,new Leases(true)).commit(room(9),request(9,"r3"),result("r3")));
        check(atomic.calls==1,"identity mismatch never retries");
    }
    private static void confirmationFieldNegatives() {
        for(String field:List.of("sequence","fencing","msgId")) {
            Store store=new Store(); AtomicFake atomic=new AtomicFake(store,true,field);
            expect(RuntimeException.class,()->committer(atomic,store,new Leases(true)).commit(room(30),request(30,"identity-"+field),result("identity-"+field)));
            check(atomic.calls==1,field+" mismatch never retries");
        }
    }
    private static void roomMismatchFails() {
        expect(IllegalArgumentException.class,()->committer(new AtomicFake(new Store(),false,false),new Store(),new Leases(true))
                .commit(room(10),request(11,"r4"),result("r4")));
    }
    private static void nonJdbcRejected() {
        RoomEventJournal journal=new RoomEventJournal(){public void append(RoomEventIdentity i,Object p){} public List<Object> after(long r,long s){return List.of();}};
        expect(IllegalArgumentException.class,()->new ProductionRoomCommandCommitter(journal,new Store(),new Leases(true),Clock.fixed(NOW,ZoneOffset.UTC),"n"));
    }
    private static void recoverySuccessAndRaces() {
        GameRegistry games=new GameRegistry(); games.register(provider());
        Store one=new Store(); one.recoverable.add(snapshot(21,"ok","m")); RuntimeGameRoomRegistry rooms=new RuntimeGameRoomRegistry();
        check(new ProductionRoomRecoveryCoordinator(games,rooms,one,new Leases(true),Clock.fixed(NOW,ZoneOffset.UTC),"n").recoverExpired(10)==1,"recovery bind");
        check(rooms.require(21).roomId()==21,"bound authority");
        Store fenced=new Store(); fenced.recoverable.add(snapshot(22,"ok","m"));
        check(new ProductionRoomRecoveryCoordinator(games,new RuntimeGameRoomRegistry(),fenced,new Leases(false),Clock.fixed(NOW,ZoneOffset.UTC),"n").recoverExpired(10)==0,"fencing race skipped");
        Store restoreFail=new Store(); restoreFail.recoverable.add(snapshot(23,"fail","m"));
        check(new ProductionRoomRecoveryCoordinator(games,new RuntimeGameRoomRegistry(),restoreFail,new Leases(true),Clock.fixed(NOW,ZoneOffset.UTC),"n").recoverExpired(10)==0,"restore failure skipped");
        Store acquireFail=new Store(); acquireFail.recoverable.add(snapshot(24,"ok","m"));
        check(new ProductionRoomRecoveryCoordinator(games,new RuntimeGameRoomRegistry(),acquireFail,new Leases(true,true),Clock.fixed(NOW,ZoneOffset.UTC),"n").recoverExpired(10)==0,"acquire race skipped");
    }
    private static ProductionRoomCommandCommitter committer(ProductionRoomCommandCommitter.AtomicPersistence p,Store s,Leases l){return new ProductionRoomCommandCommitter(p,s,l,Clock.fixed(NOW,ZoneOffset.UTC),"n",RoomEdgeRuntimePolicy.production());}
    private static GameRoomHandle room(long id){return new GameRoomHandle(id,1,"v1",session(Map.of("players",Map.of("0",1L))));}
    private static GameCommandRequest request(long id,String request){return new GameCommandRequest("move",request,1,id,1,"v1","u",0,Map.of());}
    private static GameCommandResult result(String request){return new GameCommandResult("move",request,Map.of("ok",true));}
    private static RoomSnapshot snapshot(long id,String marker,String msg){return new RoomSnapshot(id,1,"v1","v1",1,1,NOW,Map.of("marker",marker,"msg",msg));}
    private static AuthoritativeGameSession session(Map<String,Object> state){return new AuthoritativeGameSession(){public GameCommandResult execute(GameCommandRequest r){return result(r.requestId());} public Map<String,Object> viewFor(long p){return state;} public Map<String,Object> authoritativeState(){return state;} public long stateVersion(){return 1L;} public OperationDeadline operationDeadline(){return OperationDeadline.none();} public OperationDeadlineArbiter deadlineArbiter(){return new OperationDeadlineArbiter();} public List<String> invariantViolations(){return List.of();} public SettlementPayload settlement(int r,String v){return new SettlementPayload(1,r,v,Map.of(1L,0L));}};}
    private static GameProvider provider(){return new GameProvider(){public GameDescriptor descriptor(){return new GameDescriptor(1,"edge","edge",GameCategory.values()[0],"edge",RegionScope.values()[0],"","","v1");} public GameRoomFactory roomFactory(){return c->room(c.roomId());} public Optional<AuthoritativeGameSession> restoreAuthoritativeSession(StatePayload state){return "fail".equals(state.get("marker"))?Optional.empty():Optional.of(session(state.asMap()));}};}
    private static final class Store implements RoomSnapshotStore {final Map<Long,RoomSnapshot> values=new HashMap<>(); final List<RoomSnapshot> recoverable=new ArrayList<>(); public void save(RoomSnapshot s){values.put(s.roomId(),s);} public Optional<RoomSnapshot> latest(long id){return Optional.ofNullable(values.get(id));} public List<RoomSnapshot> recoverable(Instant n,int l){return recoverable.stream().limit(l).toList();}}
    private static final class AtomicFake implements ProductionRoomCommandCommitter.AtomicPersistence {final Store store;final boolean throwAfter;final String corrupt;int calls;AtomicFake(Store s,boolean t,boolean c){this(s,t,c?"requestId":"");}AtomicFake(Store s,boolean t,String c){store=s;throwAfter=t;corrupt=c;} public void commit(RoomSnapshot s,RoomEventIdentity i,Map<String,Object> p,long expectedStateVersion){if(s.stateVersion()!=expectedStateVersion+1)throw new AssertionError("CAS expectation");calls++; Map<String,Object> m=new HashMap<>(s.authoritativeState());long sequence=s.lastEventSequence(),fencing=s.fencingToken();if("requestId".equals(corrupt))m.put("_lastCommittedRequestId","other");if("msgId".equals(corrupt))m.put("_lastCommittedMsgId","other");if("sequence".equals(corrupt))sequence++;if("fencing".equals(corrupt))fencing++;RoomSnapshot saved=new RoomSnapshot(s.roomId(),s.gameId(),s.playVersion(),s.componentVersion(),fencing,sequence,s.capturedAt(),m);store.save(saved);if(throwAfter)throw new IllegalStateException("lost ack");}}
    private static final class Leases implements RoomLeaseStore {final boolean current,failAcquire;Leases(boolean c){this(c,false);}Leases(boolean c,boolean f){current=c;failAcquire=f;}public RoomLease acquire(long id,String n,Duration t){if(failAcquire)throw new IllegalStateException("claim race");return new RoomLease(id,n,1,NOW.plus(t));}public boolean isCurrent(RoomLease l){return current;}public void release(RoomLease l){}}
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);} private static void expect(Class<? extends Throwable> t,Runnable r){try{r.run();throw new AssertionError("expected "+t);}catch(Throwable e){if(!t.isInstance(e))throw e;}}
}
