package com.aoo.bcg.common.error;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Append-only public error-code ledger. Published numbers and meanings are permanent. */
public final class ErrorCodeCatalog {
    public record Definition(int code,String symbolicName,String meaning,String introducedVersion,String retiredVersion){
        public Definition{
            if(code<1000||code>6999||symbolicName==null||!symbolicName.matches("[A-Z][A-Z0-9_]+")
                    ||meaning==null||meaning.isBlank()||introducedVersion==null||!introducedVersion.matches("\\d+\\.\\d+\\.\\d+")
                    ||(retiredVersion!=null&&!retiredVersion.matches("\\d+\\.\\d+\\.\\d+")))throw new IllegalArgumentException("invalid error-code definition");
        }
        Definition retire(String version){if(retiredVersion!=null)throw new IllegalStateException("error code already retired");return new Definition(code,symbolicName,meaning,introducedVersion,version);}
    }
    /**
     * Cross-service symbolic error contract. The symbolic code is immutable;
     * legacy aliases are scoped to their transport so an old numeric value
     * cannot silently become a different error in another service.
     */
    public record SymbolicDefinition(String code,String domain,int httpStatus,String publicMessage,
                                     String compatibilityNamespace,Integer legacyNumericCode,Set<String> legacyAliases){
        public SymbolicDefinition{
            if(code==null||!code.matches("[A-Z][A-Z0-9_]+")||domain==null||domain.isBlank()
                    ||httpStatus<400||httpStatus>599||publicMessage==null||publicMessage.isBlank()
                    ||compatibilityNamespace==null||compatibilityNamespace.isBlank()
                    ||(legacyNumericCode!=null&&(legacyNumericCode<0||legacyNumericCode>9999)))
                throw new IllegalArgumentException("invalid symbolic error definition");
            legacyAliases=Set.copyOf(legacyAliases==null?Set.of():legacyAliases);
            if(legacyAliases.stream().anyMatch(alias->alias==null||alias.isBlank()))
                throw new IllegalArgumentException("invalid symbolic error alias");
        }
        String legacyKey(){return legacyNumericCode==null?null:compatibilityNamespace+":"+legacyNumericCode;}
    }
    private final Map<Integer,Definition> definitions=new LinkedHashMap<>();
    private final Map<String,SymbolicDefinition> symbolicDefinitions=new LinkedHashMap<>();
    private final Map<String,String> symbolicAliases=new LinkedHashMap<>();
    private final Map<String,String> legacyNumericOwners=new LinkedHashMap<>();
    public synchronized void publish(Definition candidate){
        Definition existing=definitions.get(candidate.code());
        if(existing!=null&&!existing.equals(candidate))throw new IllegalStateException("published error code cannot change meaning or be reused: "+candidate.code());
        if(existing==null&&definitions.values().stream().anyMatch(value->value.symbolicName().equals(candidate.symbolicName())))throw new IllegalStateException("error symbolic name cannot be reused");
        definitions.putIfAbsent(candidate.code(),candidate);
    }
    public synchronized void retire(int code,String version){Definition value=require(code);definitions.put(code,value.retire(version));}
    public synchronized Definition require(int code){Definition value=definitions.get(code);if(value==null)throw new IllegalArgumentException("unpublished error code: "+code);return value;}
    public synchronized Map<Integer,Definition> snapshot(){return Map.copyOf(definitions);}
    public synchronized void publish(SymbolicDefinition candidate){
        SymbolicDefinition existing=symbolicDefinitions.get(candidate.code());
        if(existing!=null&&!existing.equals(candidate))throw new IllegalStateException("published symbolic error cannot change meaning: "+candidate.code());
        if(existing==null){
            if(symbolicAliases.containsKey(candidate.code()))throw new IllegalStateException("symbolic error aliases cannot become canonical: "+candidate.code());
            String legacyKey=candidate.legacyKey();
            if(legacyKey!=null){
                String owner=legacyNumericOwners.get(legacyKey);
                if(owner!=null&&!owner.equals(candidate.code()))throw new IllegalStateException("legacy numeric error collision: "+legacyKey);
                legacyNumericOwners.put(legacyKey,candidate.code());
            }
            for(String alias:candidate.legacyAliases()){
                String owner=symbolicAliases.get(alias);
                if(owner!=null&&!owner.equals(candidate.code()))throw new IllegalStateException("symbolic error alias collision: "+alias);
                if(symbolicDefinitions.containsKey(alias)&&!alias.equals(candidate.code()))throw new IllegalStateException("symbolic error alias collides with canonical code: "+alias);
                symbolicAliases.put(alias,candidate.code());
            }
            symbolicDefinitions.put(candidate.code(),candidate);
        }
    }
    public synchronized SymbolicDefinition requireSymbolic(String code){
        String canonical=symbolicDefinitions.containsKey(code)?code:symbolicAliases.get(code);
        if(canonical==null)throw new IllegalArgumentException("unpublished symbolic error: "+code);
        return symbolicDefinitions.get(canonical);
    }
    public synchronized String canonicalSymbolicCode(String code){return requireSymbolic(code).code();}
    /** Resolves an additive compatibility alias without changing the wire value. */
    public synchronized String canonicalSymbolicCodeOr(String code,String fallback){
        if(code==null||code.isBlank())return fallback;
        try{return canonicalSymbolicCode(code);}catch(IllegalArgumentException ignored){return fallback;}
    }
    /** Classifies a published compatibility identifier or diagnostic text; unknown input remains the caller's explicit fallback. */
    public static String classify(String codeOrMessage,String fallback){
        if(codeOrMessage==null||codeOrMessage.isBlank())return fallback;
        ErrorCodeCatalog catalog=standard();
        String direct=catalog.canonicalSymbolicCodeOr(codeOrMessage,null);
        if(direct!=null)return direct;
        String value=codeOrMessage.toUpperCase(java.util.Locale.ROOT);
        if(value.contains("PROVIDER")||value.contains("GAME_ROUTE")||value.contains("ROUTE_UNAVAILABLE"))return "PROVIDER_MISSING";
        if(value.contains("PRIC")||value.contains("COST_POLICY"))return "PRICING_POLICY_MISSING";
        if(value.contains("SNAPSHOT")||value.contains("RULE_LOCK"))return "SNAPSHOT_MISSING";
        if(value.contains("ACTIVE_GAME")||value.contains("GAME_ALREADY")||value.contains("ALREADY_EXISTS")||value.contains("EXISTS")
                ||value.contains("ALREADY PROCESSED")||value.contains("REPLAY")||value.contains("DUPLICATE"))return "REQUEST_REPLAYED";
        if(value.contains("STATE_CONFLICT")||value.contains("NOT_CURRENT")||value.contains("ACTIVE GAME")
                ||value.contains("ILLEGAL_GAME_OPERATION"))return "ROOM_STATE_CONFLICT";
        if(value.contains("IDENTITY_NOT_FOUND")||value.contains("IDENTITY MISSING"))return "IDENTITY_NOT_FOUND";
        if(value.contains("IDENTITY")||value.contains("ROOM_IDENTITY"))return "IDENTITY_MISMATCH";
        if(value.contains("MISSING")||value.contains("NOT_FOUND")||value.contains("CONFIG")
                ||value.contains("TEMPLATE")||value.contains("RULE_SCHEMA"))return "CONFIG_NOT_FOUND";
        if(value.contains("VERSION")||value.contains("UPGRADE")||value.contains("PROTOCOL"))return "VERSION_UNSUPPORTED";
        if(value.contains("PERMISSION")||value.contains("FORBIDDEN")||value.contains("UNAUTHORIZED")
                ||value.contains("MEMBERSHIP")||value.contains("MANAGER_REQUIRED")||value.contains("MANAGER REQUIRED")
                ||value.contains("OWNER REQUIRED"))return "PERMISSION_DENIED";
        if(value.contains("AUTHORITY")||value.contains("UPSTREAM")||value.contains("CONNECTION")
                ||value.contains("TIMEOUT"))return "UPSTREAM_UNAVAILABLE";
        if(value.contains("REQUEST")||value.contains("ENVELOPE")||value.contains("REQUIRED")
                ||value.contains("INVALID"))return "INVALID_ENVELOPE";
        return fallback;
    }
    public static String classify(Throwable failure,String fallback){
        for(Throwable current=failure;current!=null;current=current.getCause()){
            String classified=classify(current.getMessage(),null);
            if(classified!=null)return classified;
        }
        return fallback;
    }
    public synchronized Map<String,SymbolicDefinition> symbolicSnapshot(){return Map.copyOf(symbolicDefinitions);}
    public static ErrorCodeCatalog standard(){
        var catalog=new ErrorCodeCatalog();
        catalog.publish(new Definition(1008,"DATABASE_UNAVAILABLE","数据库服务暂时不可用","2.0.0",null));
        catalog.publish(new Definition(1009,"DUPLICATE_SUBMISSION","数据已存在，请勿重复提交","2.0.0",null));
        catalog.publish(new Definition(1010,"CONCURRENT_CONFLICT","请求发生并发冲突，请重试","2.0.0",null));
        catalog.publish(new Definition(1011,"BUSINESS_CONSTRAINT","数据不符合业务约束","2.0.0",null));
        catalog.publish(new Definition(1012,"OPERATION_TIMEOUT","请求处理超时，请重试","2.0.0",null));
        catalog.publish(new Definition(2001,"UNAUTHORIZED","登录状态无效","2.0.0",null));
        catalog.publish(new Definition(3001,"ROOM_NOT_FOUND","房间不存在","2.0.0",null));
        catalog.publish(new Definition(4001,"ILLEGAL_GAME_OPERATION","当前操作不合法","2.0.0",null));
        catalog.publish(new SymbolicDefinition("IDENTITY_NOT_FOUND","HALL",404,"identity not found","hall",null,Set.of("HALL_NOT_FOUND","HALL_IDENTITY_NOT_FOUND","HALL_IDENTITY_NOT_CURRENT_PUBLISHED","HALL_ACTIVE_GAME_NOT_FOUND")));
        catalog.publish(new SymbolicDefinition("CONFIG_NOT_FOUND","HALL",404,"active game configuration not found","hall",null,Set.of("HALL_GAME_UNAVAILABLE","HALL_RULE_SCHEMA_UNAVAILABLE","HALL_CONFIG_NOT_FOUND","HALL_CONFIGURATION_NOT_FOUND")));
        catalog.publish(new SymbolicDefinition("PROVIDER_MISSING","HALL",503,"game provider is unavailable","hall",null,Set.of("HALL_GAME_ROUTE_UNAVAILABLE","HALL_PROVIDER_NOT_FOUND","HALL_PROVIDER_UNAVAILABLE","HALL_GAME_PROVIDER_MISSING")));
        catalog.publish(new SymbolicDefinition("PRICING_POLICY_MISSING","BILLING",409,"active room cost policy is missing","hall",null,Set.of("HALL_COST_POLICY_MISSING")));
        catalog.publish(new SymbolicDefinition("SNAPSHOT_MISSING","ROOM",409,"room snapshot is unavailable","hall",null,Set.of("HALL_ROOM_RULE_SNAPSHOT_UNAVAILABLE","HALL_SNAPSHOT_MISSING","HALL_ROOM_SNAPSHOT_MISSING","PLAYING_SNAPSHOT_MISSING")));
        catalog.publish(new SymbolicDefinition("IDENTITY_MISMATCH","ROOM",409,"room identity does not match","hall",null,Set.of("HALL_RUNTIME_IDENTITY_CONFLICT","HALL_ROOM_IDENTITY_CONFLICT","HALL_IDENTITY_MISMATCH","ROOM_RUNTIME_IDENTITY_CONFLICT")));
        catalog.publish(new SymbolicDefinition("VERSION_UNSUPPORTED","COMMON",426,"client or protocol version is unsupported","gateway",3005,Set.of("HALL_CLIENT_UPGRADE_REQUIRED","HALL_VERSION_UNSUPPORTED","GATEWAY_VERSION_INCOMPATIBLE")));
        catalog.publish(new SymbolicDefinition("UPSTREAM_UNAVAILABLE","COMMON",503,"upstream service is unavailable","gateway",3010,Set.of("HALL_AUTHORITY_REJECTED","HALL_UPSTREAM_UNAVAILABLE","ROOM_AUTHORITY_UNAVAILABLE","CLUB_PROVIDER_UNAVAILABLE","UPSTREAM_UNAVAILABLE","INTERNAL_AUTHORITY_FAILED","HALL_INTERNAL_ERROR")));
        catalog.publish(new SymbolicDefinition("INVALID_ENVELOPE","COMMON",400,"request envelope is invalid","gateway",1001,Set.of("HALL_INVALID_REQUEST","HALL_INVALID_INTERNAL_REQUEST","HALL_INVALID_ENVELOPE","HALL_INVALID_ROOM_ID","HALL_AUTHORITATIVE_FIELD_FORBIDDEN","ROOM_AUTHORITY_TYPED_DESTROY_REQUIRED","GATEWAY_INVALID_ENVELOPE")));
        catalog.publish(new SymbolicDefinition("PERMISSION_DENIED","COMMON",403,"permission denied","gateway",1002,Set.of("HALL_FORBIDDEN","HALL_UNAUTHORIZED","HALL_INTERNAL_FORBIDDEN","HALL_RECORD_ACCESS_DENIED","HALL_CLUB_ACCESS_DENIED","HALL_CLUB_MEMBERSHIP_REQUIRED","HALL_CLUB_MANAGER_REQUIRED","HALL_ROOM_ACCESS_DENIED","CLUB_PERMISSION_DENIED","GATEWAY_UNAUTHORIZED","GATEWAY_ROOM_ADMIN_FORBIDDEN")));
        catalog.publish(new SymbolicDefinition("CLUB_LIST_EMPTY","CLUB",404,"club list is empty","club",3008,Set.of("CLUB_LEGACY_3008_EMPTY_LIST")));
        catalog.publish(new SymbolicDefinition("ROOM_STATE_CONFLICT","ROOM",409,"room state conflict","gateway",3008,Set.of("GATEWAY_ROOM_STATE_CONFLICT","HALL_ROOM_STATE_CONFLICT")));
        catalog.publish(new SymbolicDefinition("REQUEST_REPLAYED","COMMON",409,"request was already processed","gateway",1003,Set.of("HALL_IDEMPOTENCY_CONFLICT","HALL_REQUEST_ID_CONFLICT")));
        catalog.publish(new SymbolicDefinition("RATE_LIMITED","COMMON",429,"request rate limited","gateway",1004,Set.of("GATEWAY_RATE_LIMITED")));
        catalog.publish(new SymbolicDefinition("PAYLOAD_TOO_LARGE","COMMON",413,"request payload is too large","gateway",1005,Set.of("GATEWAY_PAYLOAD_TOO_LARGE")));
        catalog.publish(new SymbolicDefinition("BACKPRESSURE","COMMON",503,"service is overloaded","gateway",1006,Set.of("GATEWAY_BACKPRESSURE")));
        catalog.publish(new SymbolicDefinition("TICKET_EXPIRED","COMMON",401,"websocket ticket expired","gateway",1007,Set.of("GATEWAY_WS_TICKET_EXPIRED")));
        catalog.publish(new SymbolicDefinition("TICKET_REJECTED","COMMON",403,"websocket ticket rejected","gateway",1008,Set.of("GATEWAY_WS_TICKET_REJECTED")));
        catalog.publish(new SymbolicDefinition("IDEMPOTENCY_CONFLICT","COMMON",409,"idempotency key already used","gateway",1009,Set.of("GATEWAY_IDEMPOTENCY_CONFLICT")));
        catalog.publish(new SymbolicDefinition("API_VERSION_REQUIRED","COMMON",426,"API version is required","gateway",1010,Set.of("GATEWAY_API_VERSION_REQUIRED")));
        catalog.publish(new SymbolicDefinition("LEGACY_ENTRY_DISABLED","COMMON",410,"legacy entry is disabled","gateway",1011,Set.of("GATEWAY_LEGACY_ENTRY_DISABLED")));
        catalog.publish(new SymbolicDefinition("RISK_ADMISSION_REJECTED","COMMON",403,"request cannot be accepted","gateway",1012,Set.of("GATEWAY_RISK_ADMISSION_REJECTED")));
        catalog.publish(new SymbolicDefinition("ROUTE_NOT_FOUND","ROOM",404,"room route not found","gateway",3001,Set.of("GATEWAY_ROUTE_NOT_FOUND","HALL_INTERNAL_ROUTE_NOT_FOUND","HALL_ROUTE_NOT_FOUND")));
        catalog.publish(new SymbolicDefinition("ROUTE_MOVED","ROOM",409,"room route moved","gateway",3002,Set.of("GATEWAY_ROUTE_MOVED")));
        catalog.publish(new SymbolicDefinition("ROUTE_STALE","ROOM",409,"room route is stale","gateway",3003,Set.of("GATEWAY_ROUTE_STALE")));
        catalog.publish(new SymbolicDefinition("NODE_DRAINING","ROOM",503,"room node is draining","gateway",3004,Set.of("GATEWAY_NODE_DRAINING")));
        catalog.publish(new SymbolicDefinition("FENCING_REJECTED","ROOM",409,"stale room owner rejected","gateway",3006,Set.of("GATEWAY_FENCING_REJECTED")));
        catalog.publish(new SymbolicDefinition("ROOM_CAPABILITY_UNSUPPORTED","ROOM",409,"room capability unsupported","gateway",3009,Set.of("GATEWAY_ROOM_CAPABILITY_UNSUPPORTED")));
        return catalog;
    }
}
