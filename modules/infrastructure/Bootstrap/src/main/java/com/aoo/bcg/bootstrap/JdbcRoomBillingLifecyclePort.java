package com.aoo.bcg.bootstrap;

import com.aoo.bcg.billing.*;
import com.aoo.bcg.billing.RoomBillingSourceBinding.*;
import com.aoo.bcg.club.JdbcClubService;
import com.aoo.bcg.config.RoomRuntimeIdentityStore;
import com.aoo.bcg.gamespi.RoomRuntimeIdentity;
import com.aoo.bcg.lobby.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import java.sql.*;
import java.time.Clock;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.LongSupplier;
import static com.aoo.bcg.billing.RoomBillingBusinessOutcome.EventType.*;

/** Authenticated durable create source; no caller-supplied payer or independent ledger path. */
final class JdbcRoomBillingLifecyclePort implements LobbyRoomCreateSaga.BillingPort {
    private final DataSource source;
    private final Clock clock;
    private final ObjectMapper json=new ObjectMapper().findAndRegisterModules();
    private final JdbcRoomBillingSourceBindingRepository bindings=new JdbcRoomBillingSourceBindingRepository(new JdbcRoomBillingIntentJournal());
    private final JdbcRoomBillingBusinessOutcomeRepository outcomes=new JdbcRoomBillingBusinessOutcomeRepository(bindings);
    private final JdbcRoomBillingPayerResolver payer=new JdbcRoomBillingPayerResolver(json);
    private final JdbcRoomChargeCoordinator charges;
    JdbcRoomBillingLifecyclePort(DataSource source,LongSupplier ids,Clock clock){
        this.source=Objects.requireNonNull(source);this.clock=Objects.requireNonNull(clock);
        charges=payer.createCoordinator(source,new JdbcBillingService(source,ids,clock),new JdbcRoomCostPolicyRepository(source,false),clock);
    }
    private interface Work<T>{T apply(Connection c)throws Exception;}
    private <T>T transaction(Work<T> work){try(Connection c=source.getConnection()){
        c.setAutoCommit(false);try{T result=work.apply(c);c.commit();return result;}catch(Exception e){c.rollback();throw e;}
    }catch(Exception e){if(e instanceof RuntimeException x)throw x;throw new IllegalStateException("room billing lifecycle persistence failed",e);}}
    private static SourceKey key(LobbyRoomCreateSaga.Command c){return new SourceKey("HALL_CREATE",c.requestId(),c.requestHash(),c.accountId());}
    @Override public void reserve(LobbyRoomCreateSaga.Command command){
        BoundIntent bound=transaction(c->{
            var key=key(command);var old=bindings.find(c,key,true);
            if(old.isPresent())return old.get();
            if(!command.normalizedRules()||command.ruleRevision()==null)throw new IllegalArgumentException("canonical create rules required");
            JdbcLobbyRepository.authorizeCreateScope(c,command);
            var revision=command.ruleRevision();var authority=new AuthorityReference(revision.releaseId(),revision.indexGeneration(),revision.sourceHash(),revision.classificationRegionCode());
            Map<String,Object> schema=publishedSchema(c,command,authority);
            var defaults=defaults(schema);String measure=Objects.toString(schema.get("roomMeasureKind"),"");
            if(measure.isEmpty()){
                boolean duration=defaults.containsKey("roomDurationMinutes"),rounds=has(defaults,"roundCount","rounds","totalRounds","setCount");
                if(duration==rounds)throw new IllegalArgumentException("unambiguous published create measure required");
                measure=duration?"DURATION_MINUTES":"ROUND_COUNT";
            }
            if(!Set.of("ROUND_COUNT","DURATION_MINUTES").contains(measure))throw new IllegalArgumentException("unsupported create billing time");
            int value=measure.equals("ROUND_COUNT")?positive(command.rules(),defaults,"roundCount","rounds","totalRounds","setCount"):positive(command.rules(),defaults,"roomDurationMinutes");
            int players=positive(command.rules(),defaults,"playerCount","maxPlayers","seatLimit","playerNum");
            var context=context(c,command);var mode=context==RoomChargeOperation.Context.HALL?RoomChargeOperation.Payer.OWNER:context==RoomChargeOperation.Context.CLUB?RoomChargeOperation.Payer.CLUB:RoomChargeOperation.Payer.UNION;
            if(context==RoomChargeOperation.Context.HALL){Object selected=command.rules().getOrDefault("payerMode",defaults.get("payerMode"));
                if(!"OWNER".equals(selected))throw new IllegalArgumentException("participant billing requires its own authenticated entry");}
            var subject=new JdbcRoomChargeCoordinator.Subject(command.accountId(),command.accountId(),command.scope().club()?command.scope().clubId():0);
            var provisional=operation(command,authority,context,mode,value,players,measure,command.accountId());
            long resolved=payer.resolve(c,provisional,new JdbcRoomBillingPayerResolver.Subject(subject.initiatingActorId(),subject.creatorId(),subject.sourceClubId()));
            var operation=operation(command,authority,context,mode,value,players,measure,resolved);
            return bindings.bind(c,key,new RoomBillingIntent(operation,clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MILLIS)),subject,authority);
        });
        charges.reserve(bound);
    }
    private static RoomChargeOperation operation(LobbyRoomCreateSaga.Command c,AuthorityReference a,RoomChargeOperation.Context context,RoomChargeOperation.Payer mode,int value,int players,String measure,long payer){
        return new RoomChargeOperation(c.roomId(),attempt(c.roomId(),c.requestId()),"CREATE",c.accountId(),payer,c.gameId(),c.playVersion(),a.classificationRegionCode(),RoomChargeOperation.Kind.FIXED,context,mode,measure,value,players,0);
    }
    static String attempt(long room,String request){try{var hash=MessageDigest.getInstance("SHA-256");
        for(String part:List.of("CREATE_V1",Long.toString(room),request)){byte[] b=part.getBytes(StandardCharsets.UTF_8);hash.update((b.length+":").getBytes(StandardCharsets.US_ASCII));hash.update(b);}
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash.digest());
    }catch(Exception e){throw new IllegalStateException(e);}}
    private Map<String,Object> publishedSchema(Connection c,LobbyRoomCreateSaga.Command command,AuthorityReference a)throws Exception{
        String sql="SELECT i.rule_validator FROM aoo_compiled_index_active p JOIN aoo_compiled_room_create_index i ON i.game_id=p.game_id AND i.play_version=p.play_version AND i.region_code=p.region_code AND i.release_id=p.release_id AND i.index_generation=p.index_generation JOIN aoo_game_release r ON r.release_id=p.release_id AND r.game_id=p.game_id AND r.play_version=p.play_version WHERE p.game_id=? AND p.play_version=? AND p.region_code=? AND p.release_id=? AND p.index_generation=? AND r.source_hash=? AND r.status='ACTIVE' AND i.lifecycle_state='ACTIVE'";
        try(var q=c.prepareStatement(sql)){q.setInt(1,command.gameId());q.setString(2,command.playVersion());q.setString(3,a.classificationRegionCode());q.setLong(4,a.releaseId());q.setLong(5,a.indexGeneration());q.setString(6,a.runtimeSourceHash());try(var rows=q.executeQuery()){
            if(!rows.next())throw new IllegalArgumentException("current published rule authority missing");
            Map<String,Object> result=json.readValue(rows.getString(1),Map.class);if(rows.next())throw new IllegalArgumentException("published rule authority ambiguous");return result;
        }}
    }
    private RoomChargeOperation.Context context(Connection c,LobbyRoomCreateSaga.Command command)throws Exception{
        if(!command.scope().club())return RoomChargeOperation.Context.HALL;
        try(var q=c.prepareStatement("SELECT state_json FROM aoo_club_state WHERE club_id=? FOR UPDATE")){q.setLong(1,command.scope().clubId());try(var rows=q.executeQuery()){
            if(!rows.next())throw new SecurityException("club missing");var state=json.readValue(rows.getString(1),JdbcClubService.State.class);
            if(rows.next()||state.id()!=command.scope().clubId()||!"ACTIVE".equals(state.status()))throw new SecurityException("club source mismatch");
            var templates=state.templates().stream().filter(t->t.id().equals(command.scope().templateCode())).toList();
            if(templates.size()!=1)throw new SecurityException("explicit server template required");
            Object marker=state.settings().get("templateScope."+templates.getFirst().index());
            if("CLUB".equals(marker))return RoomChargeOperation.Context.CLUB;
            if(marker instanceof String text&&text.matches("UNION:[1-9][0-9]*")&&state.settings().get("unionId") instanceof Number n&&text.equals("UNION:"+n.longValue()))return RoomChargeOperation.Context.UNION;
            throw new SecurityException("explicit server template scope required");
        }}
    }
    private static Map<String,Object> defaults(Map<String,Object> schema){if(!(schema.get("fields") instanceof List<?> fields))throw new IllegalArgumentException("published rule fields required");Map<String,Object> result=new HashMap<>();for(Object v:fields)if(v instanceof Map<?,?> m&&m.get("key") instanceof String k&&m.containsKey("defaultValue"))result.put(k,m.get("defaultValue"));return result;}
    private static boolean has(Map<String,?> m,String...keys){for(String k:keys)if(m.containsKey(k))return true;return false;}
    private static int positive(Map<String,Object> rules,Map<String,Object> defaults,String...keys){Object chosen=null;for(String k:keys){Object v=rules.containsKey(k)?rules.get(k):defaults.get(k);if(v!=null){if(chosen!=null&&!chosen.toString().equals(v.toString()))throw new IllegalArgumentException("conflicting billing dimensions");chosen=v;}}try{int n=new java.math.BigDecimal(Objects.toString(chosen,"")).intValueExact();if(n<=0)throw new ArithmeticException();return n;}catch(RuntimeException e){throw new IllegalArgumentException("positive integral billing dimension required");}}
    BoundIntent requireRoom(Connection c,long roomId)throws SQLException{return requireRoom(c,roomId,false);}
    private BoundIntent requireRoom(Connection c,long roomId,boolean ownedAbortCleanup)throws SQLException{
        SourceKey key;
        try(var q=c.prepareStatement("SELECT request_id,request_hash,account_id FROM aoo_room_create_saga WHERE room_id=? FOR UPDATE")){q.setLong(1,roomId);try(var rows=q.executeQuery()){
            if(!rows.next())throw new IllegalStateException("room billing source unknown");key=new SourceKey("HALL_CREATE",rows.getString(1),rows.getString(2),rows.getLong(3));if(rows.next())throw new IllegalStateException("room billing source ambiguous");
        }}
        var bound=bindings.find(c,key,true).orElseThrow(()->new IllegalStateException("room billing binding missing"));
        if(!ownedAbortCleanup&&outcomes.find(c,key,CREATE_ABORTED_BEFORE_COMMIT).isPresent())throw new IllegalStateException("room create producer aborted");return bound;
    }
    void requireCreateSource(BoundIntent bound,String requestId,long creatorId,long gameId,String playVersion) {
        var operation=bound.intent().operation();
        if(!bound.key().requestId().equals(requestId)||creatorId!=bound.key().authenticatedActor()
                ||creatorId!=bound.subject().creatorId()||operation.gameId()!=gameId||!operation.playVersion().equals(playVersion))
            throw new SecurityException("create command does not match durable billing source");
    }
    void requireCommitted(Connection c,BoundIntent bound,RoomRuntimeIdentity identity)throws SQLException {
        sameIdentity(outcomes.find(c,bound.key(),CREATE_COMMITTED).orElseThrow(()->new IllegalStateException("create commit receipt unknown")),identity);
    }
    void snapshotCommitted(Connection c,BoundIntent bound,RoomRuntimeIdentity identity)throws SQLException{
        var old=outcomes.find(c,bound.key(),CREATE_COMMITTED);
        if(old.isPresent()){sameIdentity(old.get(),identity);return;}
        outcomes.recordCommit(c,bound,outcome(bound,CREATE_COMMITTED,identity));
    }
    private RoomBillingBusinessOutcome outcome(BoundIntent b,RoomBillingBusinessOutcome.EventType type,RoomRuntimeIdentity id){var op=b.intent().operation();return new RoomBillingBusinessOutcome(type,op.roomId(),b.subject().creatorId(),id.roomInstanceId(),id.lifecycleEpoch(),id.fencingToken(),id.stateVersion(),op.gameId(),op.playVersion(),clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));}
    private static void sameIdentity(RoomBillingBusinessOutcome o,RoomRuntimeIdentity id){if(!o.roomInstanceId().equals(id.roomInstanceId())||o.lifecycleEpoch()!=id.lifecycleEpoch()||o.fencingToken()!=id.fencingToken()||id.stateVersion()<o.stateVersion())throw new SecurityException("billing committed identity mismatch");}
    void destroy(Connection c,RoomRuntimeIdentityStore identities,long roomId,RoomRuntimeIdentity id,RoomRuntimeIdentityStore.AuditContext audit)throws SQLException{
        var b=requireRoom(c,roomId);var committed=outcomes.find(c,b.key(),CREATE_COMMITTED).orElseThrow(()->new IllegalStateException("destroy commit receipt unknown"));sameIdentity(committed,id);
        identities.destroy(c,roomId,id,audit);outcomes.recordDestroy(c,b,outcome(b,CREATE_DESTROYED,id));
    }
    void cleanupOwnedCreate(Connection c,RoomRuntimeIdentityStore identities,long roomId,RoomRuntimeIdentity id,String ownedRequestId,RoomRuntimeIdentityStore.AuditContext audit)throws SQLException {
        var bound=requireRoom(c,roomId,true);
        if(!bound.key().requestId().equals(ownedRequestId))throw new SecurityException("owned create source mismatch");
        var committed=outcomes.find(c,bound.key(),CREATE_COMMITTED);
        if(committed.isPresent()){sameIdentity(committed.get(),id);identities.destroy(c,roomId,id,audit);outcomes.recordDestroy(c,bound,outcome(bound,CREATE_DESTROYED,id));return;}
        if(outcomes.find(c,bound.key(),CREATE_ABORTED_BEFORE_COMMIT).isEmpty())throw new IllegalStateException("owned cleanup outcome unknown");
        identities.destroy(c,roomId,id,audit);
    }
    @Override public void confirm(LobbyRoomCreateSaga.Command command){var proof=transaction(c->{var b=bindings.find(c,key(command),true).orElseThrow();var receipt=outcomes.find(c,b.key(),CREATE_COMMITTED).orElseThrow(()->new IllegalStateException("create outcome unknown"));return Map.entry(b,receipt);});
        charges.confirm(proof.getKey().intent().operation().requestId(),"create:"+attempt(command.roomId(),command.requestId()),new JdbcRoomChargeCoordinator.OutcomeEvidence("create:"+attempt(command.roomId(),command.requestId()),proof.getValue().hash()));
    }
    @Override public void release(LobbyRoomCreateSaga.Command command){var proof=transaction(c->{var b=bindings.find(c,key(command),true);if(b.isEmpty())return null;
        var committed=outcomes.find(c,b.get().key(),CREATE_COMMITTED);
        var receipt=committed.isEmpty()?outcomes.abortBeforeCommit(c,b.get().key()):outcomes.find(c,b.get().key(),CREATE_DESTROYED).orElseThrow(()->new IllegalStateException("committed room destroy outcome unknown"));return Map.entry(b.get(),receipt);});
        if(proof==null)return;
        if(charges.find(proof.getKey().intent().operation().requestId()).isEmpty())return;
        charges.refund(proof.getKey().intent().operation().requestId(),"failed:"+attempt(command.roomId(),command.requestId()),new JdbcRoomChargeCoordinator.RefundEvidence(JdbcRoomChargeCoordinator.RefundCause.BUSINESS_FAILED,"failed:"+attempt(command.roomId(),command.requestId()),proof.getValue().hash()));
    }
}
