package com.aoo.bcg.hall.room;

import com.aoo.bcg.hall.http.HallError;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;

public final class RoomCreateSaga {
    private static final Duration PROCESSING_LEASE=Duration.ofSeconds(30);
    private final Store store; private final BillingPort billing; private final RoomPort rooms; private final AuthorityPort authority;
    public RoomCreateSaga(Store store,BillingPort billing,RoomPort rooms,AuthorityPort authority){this.store=Objects.requireNonNull(store);this.billing=Objects.requireNonNull(billing);this.rooms=Objects.requireNonNull(rooms);this.authority=Objects.requireNonNull(authority);}

    public Map<String,Object> create(long account,String requestId,Map<String,Object> request){
        Command command=Command.parse(account,requestId,request);State initial=store.begin(command);
        if(!initial.requestHash().equals(command.requestHash()))throw HallError.conflict("HALL_REQUEST_ID_CONFLICT","requestId was reused with different input");
        String token=UUID.randomUUID().toString();if(!store.acquire(command.requestId(),token,PROCESSING_LEASE))throw HallError.conflict("HALL_CREATE_IN_PROGRESS","room creation is already in progress");
        try{
            State state=store.find(command.requestId());
            if(state.step()==Step.CONFIRMED)return state.response();
            if(state.step()==Step.COMPENSATED)throw HallError.conflict("HALL_CREATE_COMPENSATED","previous room creation was compensated; use a new requestId");
            if(state.step()==Step.COMPENSATION_PENDING){
                boolean pending=compensate(command,state,new IllegalStateException("retry compensation"));
                throw new HallError(pending?503:409,pending?"HALL_COMPENSATION_PENDING":"HALL_CREATE_COMPENSATED",pending?"room creation compensation is pending":"previous room creation was compensated; use a new requestId");
            }
            try{
                if(before(state.step(),Step.RESERVED)){billing.reserve(command);state=store.advance(command.requestId(),state.version(),Step.RESERVED,null);}
                if(before(state.step(),Step.REGISTERED)){rooms.register(command);state=store.advance(command.requestId(),state.version(),Step.REGISTERED,null);}
                if(before(state.step(),Step.AUTHORITY_CREATED)){
                    Map<String,Object> lease=requireLease(command,authority.create(command));
                    try{state=store.advance(command.requestId(),state.version(),Step.AUTHORITY_CREATED,lease);}catch(RuntimeException persistenceFailure){throw new AuthorityPersistenceException(persistenceFailure);}
                }else{
                    Map<String,Object> recovered=requireLease(command,authority.recover(command));
                    Map<String,Object> durable=requireLease(command,state.response());
                    if(fencing(recovered)!=fencing(durable))throw new IllegalStateException("room authority fencing token changed during recovery");
                }
                Map<String,Object> response=new LinkedHashMap<>(rooms.confirm(command,state.version()+1));response.putAll(state.response());billing.confirm(command);
                return store.advance(command.requestId(),state.version(),Step.CONFIRMED,response).response();
            }catch(AuthorityPersistenceException failure){throw failure;}
            catch(RuntimeException failure){compensate(command,state,failure);throw failure;}
        }finally{store.release(command.requestId(),token);}
    }

    public RecoveryReport recoverPending(int limit){int completed=0,pending=0;List<String> failures=new ArrayList<>();for(Pending item:store.pending(Math.max(1,Math.min(limit,256))))try{create(item.accountId(),item.requestId(),item.request());completed++;}catch(HallError error){if(!"HALL_CREATE_IN_PROGRESS".equals(error.code())){pending++;failures.add(item.requestId()+':'+error.code());}}catch(RuntimeException error){pending++;failures.add(item.requestId()+':'+error.getClass().getSimpleName());}return new RecoveryReport(completed,pending,List.copyOf(failures));}
    private boolean compensate(Command command,State state,RuntimeException failure){if(state.step()==Step.CONFIRMED||state.step()==Step.COMPENSATED)return false;Step origin=state.step()==Step.COMPENSATION_PENDING?state.compensationOrigin():state.step();if(origin==null)throw new IllegalStateException("room saga compensation origin missing",failure);boolean pending=false;if(origin.ordinal()>=Step.AUTHORITY_CREATED.ordinal())try{authority.remove(command,fencing(state.response()));}catch(RuntimeException ignored){pending=true;}if(origin.ordinal()>=Step.REGISTERED.ordinal())try{rooms.remove(command);}catch(RuntimeException ignored){pending=true;}if(origin.ordinal()>=Step.RESERVED.ordinal())try{billing.release(command);}catch(RuntimeException ignored){pending=true;}store.fail(command.requestId(),state.version(),pending?Step.COMPENSATION_PENDING:Step.COMPENSATED,origin,failure.getClass().getSimpleName());return pending;}
    private static boolean before(Step current,Step target){return current.ordinal()<target.ordinal();}
    private static long fencing(Map<String,Object> lease){Object value=lease.get("fencingToken");if(!(value instanceof Number number)||number.longValue()<=0)throw new IllegalStateException("authority fencing token missing");return number.longValue();}
    private static Map<String,Object> requireLease(Command command,Map<String,Object> lease){if(lease==null||number(lease,"roomId")!=command.roomId()||number(lease,"gameId")!=command.gameId()||!command.playVersion().equals(String.valueOf(lease.get("playVersion"))))throw new IllegalStateException("room authority returned a mismatched lease");fencing(lease);return Map.copyOf(lease);}
    private static long number(Map<String,Object> value,String key){Object raw=value.get(key);return raw instanceof Number number?number.longValue():0;}

    public enum Step{STARTED,RESERVED,REGISTERED,AUTHORITY_CREATED,CONFIRMED,COMPENSATION_PENDING,COMPENSATED}
    public record Command(long accountId,String requestId,long roomId,int gameId,String playVersion,String clientVersion,long stateVersion,Map<String,Object>rules,Scope scope,Map<String,Object>admission,String traceId,String requestHash){
        static Command parse(long account,String requestId,Map<String,Object> body){
            if(requestId==null||!requestId.matches("[A-Za-z0-9_.:-]{16,128}"))throw HallError.bad("HALL_INVALID_REQUEST","requestId must be 16-128 safe characters");
            long room=positive(body,"roomId"),state=positive(body,"stateVersion"),game=positive(body,"gameId");String play=text(body,"playVersion"),client=text(body,"clientVersion");
            if(!play.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}"))throw HallError.bad("HALL_INVALID_REQUEST","invalid playVersion");
            if(body.containsKey("regionCode")||body.containsKey("result")||body.containsKey("state")||body.containsKey("settlement")||body.containsKey("winner"))throw HallError.bad("HALL_AUTHORITATIVE_FIELD_FORBIDDEN","client cannot submit region routing or authoritative results");
            @SuppressWarnings("unchecked")Map<String,Object> raw=body.get("rules")instanceof Map<?,?> map?(Map<String,Object>)map:Map.of();
            if(raw.containsKey("regionCode"))throw HallError.bad("HALL_AUTHORITATIVE_FIELD_FORBIDDEN","regionCode is a catalog filter and cannot route a room");
            Map<String,Object> rules=Map.copyOf(raw);Scope scope=Scope.parse(body.get("scope"));Map<String,Object>admission=admission(body);String trace=String.valueOf(body.getOrDefault("_traceId",requestId));
            String canonical=account+"|"+room+"|"+game+"|"+play+"|"+client+"|"+state+"|"+canonical(rules)+"|"+canonical(scope.asMap())+"|"+canonical(admission);
            return new Command(account,requestId,room,Math.toIntExact(game),play,client,state,rules,scope,admission,trace,sha256(canonical));
        }
        public Map<String,Object> requestBody(){Map<String,Object> body=new LinkedHashMap<>();body.put("roomId",roomId);body.put("gameId",gameId);body.put("playVersion",playVersion);body.put("clientVersion",clientVersion);body.put("stateVersion",stateVersion);body.put("rules",rules);body.put("scope",scope.asMap());body.put("admission",admission);body.put("_traceId",traceId);return body;}
        private static Map<String,Object> admission(Map<String,Object> body){Map<String,Object>value=new LinkedHashMap<>();Object ip=body.get("_admissionIp");if(ip instanceof String text&&!text.isBlank())value.put("ipAddress",text.strip());Object latitude=body.get("_admissionLatitude"),longitude=body.get("_admissionLongitude");if(latitude instanceof Number number)value.put("latitude",number.doubleValue());if(longitude instanceof Number number)value.put("longitude",number.doubleValue());return Map.copyOf(value);}
        private static long positive(Map<String,Object>body,String key){Object value=body.get(key);if(!(value instanceof Number number)||number.longValue()<=0)throw HallError.bad("HALL_INVALID_REQUEST",key+" must be positive");return number.longValue();}
        private static String text(Map<String,Object>body,String key){Object value=body.get(key);if(!(value instanceof String string)||string.isBlank())throw HallError.bad("HALL_INVALID_REQUEST",key+" is required");return string;}
        private static String canonical(Object value){if(value instanceof Map<?,?>map){TreeMap<String,Object>sorted=new TreeMap<>();map.forEach((key,item)->sorted.put(String.valueOf(key),item));StringBuilder out=new StringBuilder("{");sorted.forEach((key,item)->out.append(key).append('=').append(canonical(item)).append(';'));return out.append('}').toString();}if(value instanceof Iterable<?>items){StringBuilder out=new StringBuilder("[");items.forEach(item->out.append(canonical(item)).append(','));return out.append(']').toString();}return String.valueOf(value);}
        private static String sha256(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception error){throw new IllegalStateException("SHA-256 unavailable",error);}}
    }
    public record Scope(String type,long clubId,String templateCode){
        static Scope parse(Object raw){
            if(raw==null)return new Scope("PERSONAL",0,"");
            if(!(raw instanceof Map<?,?> map))throw HallError.bad("HALL_INVALID_REQUEST","scope must be an object");
            Object rawType=map.get("type");String type=String.valueOf(rawType==null?"PERSONAL":rawType).strip().toUpperCase(Locale.ROOT);
            if("PERSONAL".equals(type)){
                if(map.containsKey("clubId")||map.containsKey("templateCode"))throw HallError.bad("HALL_INVALID_REQUEST","personal scope cannot contain club fields");
                return new Scope(type,0,"");
            }
            if(!"CLUB".equals(type))throw HallError.bad("HALL_INVALID_REQUEST","unsupported room scope");
            Object rawClub=map.get("clubId");long club=rawClub instanceof Number number?number.longValue():0;
            Object rawTemplate=map.get("templateCode");String template=String.valueOf(rawTemplate==null?"default":rawTemplate).strip();
            if(club<=0)throw HallError.bad("HALL_INVALID_REQUEST","clubId must be positive for club scope");
            if(!template.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}"))throw HallError.bad("HALL_INVALID_REQUEST","invalid club templateCode");
            return new Scope(type,club,template);
        }
        public Map<String,Object> asMap(){return "CLUB".equals(type)?Map.of("type",type,"clubId",clubId,"templateCode",templateCode):Map.of("type","PERSONAL");}
        public boolean club(){return "CLUB".equals(type);}
    }
    public record State(String requestId,String requestHash,Step step,long version,Map<String,Object>response,Step compensationOrigin){public State{response=response==null?Map.of():Map.copyOf(response);}public State(String requestId,String requestHash,Step step,long version,Map<String,Object>response){this(requestId,requestHash,step,version,response,null);}}
    public record Pending(long accountId,String requestId,Map<String,Object>request){}
    public record RecoveryReport(int completed,int pending,List<String>failures){}
    public interface Store{State begin(Command command);State find(String requestId);State advance(String requestId,long expectedVersion,Step step,Map<String,Object>response);void fail(String requestId,long expectedVersion,Step step,Step compensationOrigin,String reason);default boolean acquire(String requestId,String token,Duration lease){return true;}default void release(String requestId,String token){}default List<Pending>pending(int limit){return List.of();}}
    public interface BillingPort{void reserve(Command command);void confirm(Command command);void release(Command command);}
    public interface RoomPort{void register(Command command);Map<String,Object>confirm(Command command,long stateVersion);void remove(Command command);}
    public interface AuthorityPort{Map<String,Object>create(Command command);Map<String,Object>recover(Command command);void remove(Command command,long fencingToken);}
    private static final class AuthorityPersistenceException extends RuntimeException{AuthorityPersistenceException(RuntimeException cause){super("room authority was created but saga persistence failed; recovery will reuse requestId",cause);}}
}
