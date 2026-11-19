package com.aoo.bcg.lobby;

import com.aoo.bcg.hall.http.HallError;
import com.aoo.bcg.gamespi.RuntimeMetadataProjection;
import com.aoo.bcg.gamespi.RuntimeMetadataProjectionValidator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Clock;
import java.util.*;

/** Transactional production repository for the Hall catalog and room lifecycle. */
public class JdbcLobbyRepository {
    public static final String FILTER_ACTIVE_INDEX_JOIN="JOIN aoo_compiled_index_active ai ON ai.game_id=gr.game_id";
    public static final String CATALOG_ACTIVE_INDEX_JOIN="JOIN aoo_compiled_index_active a ON a.game_id=g.game_id AND a.region_code=gr.region_code";
    private static final String SHA256="^[0-9a-f]{64}$";
    private static final String VERSION="^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$";
    private static final String CANONICAL_DECIMAL="0|[1-9][0-9]{0,18}";
    private static final Set<String> CLUB_TEMPLATE_POLICY_FIELDS=Set.of(
            "roomSportsType","roomSportsEveryoneConsume","percentage","prizePool",
            "bigWinnerConsumeList","rule","roomSportsThreshold","JoinGamePoint","autoDismiss");
    private static final List<String> LEAVE_IDENTITY_FIELDS=List.of(
            "roomInstanceId","sessionId","lifecycleEpoch","fencingToken","stateVersion",
            "releaseId","indexGeneration","regionCode","sourceHash","ruleContentHash",
            "componentContentHash","cardCodecVersion","eventInterpreterVersion",
            "protocolVersion","snapshotSchemaVersion","gameId","gameCode","permission");
    private static final Set<String> PUBLIC_LEAVE_FIELDS=Set.of(
            "roomId","gameId","gameCode","playVersion","roomInstanceId","lifecycleEpoch",
            "fencingToken","stateVersion","releaseId","indexGeneration","regionCode","sourceHash",
            "ruleContentHash","componentContentHash","cardCodecVersion","eventInterpreterVersion",
            "protocolVersion","snapshotSchemaVersion");
    private final DataSource dataSource;private final ObjectMapper json;private final Clock clock;private final LobbyMembershipAuthorityPort membershipAuthority;private final SecureRandom random=new SecureRandom();
    private final RuntimeMetadataProjection runtimeMetadataProjection;
    public JdbcLobbyRepository(DataSource dataSource,ObjectMapper json,Clock clock,LobbyMembershipAuthorityPort membershipAuthority){
        this(dataSource,json,clock,membershipAuthority,null);
    }
    /** Strict constructor used by a release-wired Hall assembly. */
    public JdbcLobbyRepository(DataSource dataSource,ObjectMapper json,Clock clock,
                              LobbyMembershipAuthorityPort membershipAuthority,
                              RuntimeMetadataProjection runtimeMetadataProjection){
        this.dataSource=Objects.requireNonNull(dataSource);this.json=Objects.requireNonNull(json);this.clock=Objects.requireNonNull(clock);this.membershipAuthority=Objects.requireNonNull(membershipAuthority);this.runtimeMetadataProjection=runtimeMetadataProjection == null ? null : RuntimeMetadataProjectionValidator.validate(runtimeMetadataProjection);
    }

    public RuntimeMetadataProjection requireRuntimeMetadataProjection() {
        if (runtimeMetadataProjection == null) throw new IllegalStateException("RUNTIME_METADATA_PROJECTION_REQUIRED");
        return runtimeMetadataProjection;
    }

    private RuntimeMetadataProjection requireCanonicalLobbyProjection() {
        if (runtimeMetadataProjection == null) {
            throw new HallError(503, "HALL_RUNTIME_METADATA_PROJECTION_REQUIRED",
                    "canonical runtime metadata projection is required");
        }
        return runtimeMetadataProjection;
    }

    public List<Map<String,Object>> playFilters(String clientVersion){
        require(clientVersion,"clientVersion");
        String sql="SELECT DISTINCT r.region_code,r.parent_region_code,r.region_type,r.display_name,r.path,r.depth FROM aoo_region r WHERE r.status='ACTIVE' AND (EXISTS(SELECT 1 FROM aoo_game_region gr JOIN aoo_game_catalog g ON g.game_id=gr.game_id "+FILTER_ACTIVE_INDEX_JOIN+" JOIN aoo_play_version p ON p.game_id=ai.game_id AND p.play_version=ai.play_version WHERE gr.region_code=r.region_code AND gr.availability='AVAILABLE' AND g.status='ACTIVE' AND p.status='ACTIVE' AND EXISTS(SELECT 1 FROM aoo_game_service_route sr WHERE sr.game_id=g.game_id AND sr.play_version=p.play_version AND sr.status='ACTIVE')) OR EXISTS(SELECT 1 FROM aoo_region child JOIN aoo_game_region gr ON gr.region_code=child.region_code JOIN aoo_game_catalog g ON g.game_id=gr.game_id "+FILTER_ACTIVE_INDEX_JOIN+" JOIN aoo_play_version p ON p.game_id=ai.game_id AND p.play_version=ai.play_version WHERE child.path LIKE CONCAT(r.path,'/%') AND gr.availability='AVAILABLE' AND g.status='ACTIVE' AND p.status='ACTIVE' AND EXISTS(SELECT 1 FROM aoo_game_service_route sr WHERE sr.game_id=g.game_id AND sr.play_version=p.play_version AND sr.status='ACTIVE'))) ORDER BY r.depth,r.path";
        try(Connection c=dataSource.getConnection();PreparedStatement s=c.prepareStatement(sql);ResultSet r=s.executeQuery()){List<Map<String,Object>> out=new ArrayList<>();out.add(Map.of("regionCode","ALL","regionType","ALL","displayName","全部玩法","depth",0));while(r.next())out.add(row(r,"region_code","parent_region_code","region_type","display_name","path","depth"));return out;}catch(SQLException e){throw storage(e);}
    }
    public List<Map<String,Object>> catalog(String region,String clientVersion){
        require(region,"regionCode");require(clientVersion,"clientVersion");
        RuntimeMetadataProjection projection=requireCanonicalLobbyProjection();
        // MySQL requires every ORDER BY expression to participate in a DISTINCT
        // projection. Keep the deterministic classification priority explicit;
        // the aliases are internal sort columns and are not exposed by row(...).
        String sql="SELECT DISTINCT g.game_id,g.game_code,g.display_name,g.category_code,g.family_code,a.play_version,p.compatibility_floor,a.release_id,a.index_generation,a.cache_epoch,rel.snapshot_schema_version,rel.card_codec_version,rel.event_interpreter_version,rel.protocol_version,rel.source_hash catalog_source_hash,gr.region_code classification_region_code,r.display_name classification_name,r.region_type,r.parent_region_code,parent.display_name parent_region_name,CASE WHEN gr.region_code='GLOBAL' THEN 0 ELSE 1 END catalog_scope_sort,r.depth catalog_depth_sort,gr.priority catalog_priority_sort FROM aoo_game_catalog g JOIN aoo_game_region gr ON gr.game_id=g.game_id JOIN aoo_region r ON r.region_code=gr.region_code LEFT JOIN aoo_region parent ON parent.region_code=r.parent_region_code "+CATALOG_ACTIVE_INDEX_JOIN+" JOIN aoo_game_release rel ON rel.release_id=a.release_id AND rel.game_id=a.game_id AND rel.play_version=a.play_version AND rel.status='ACTIVE' JOIN aoo_play_version p ON p.game_id=a.game_id AND p.play_version=a.play_version WHERE (?='ALL' OR gr.region_code='GLOBAL' OR gr.region_code=? OR EXISTS(SELECT 1 FROM aoo_region filter_region WHERE filter_region.region_code=? AND (r.path=filter_region.path OR r.path LIKE CONCAT(filter_region.path,'/%')))) AND g.status='ACTIVE' AND gr.availability='AVAILABLE' AND p.status='ACTIVE' AND EXISTS(SELECT 1 FROM aoo_game_service_route sr WHERE sr.game_id=g.game_id AND sr.play_version=a.play_version AND sr.status='ACTIVE') ORDER BY catalog_scope_sort,catalog_depth_sort DESC,catalog_priority_sort,g.game_id";
        try(Connection c=dataSource.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setString(1,region);s.setString(2,region);s.setString(3,region);try(ResultSet r=s.executeQuery()){Map<Long,Map<String,Object>> unique=new LinkedHashMap<>();while(r.next()){String floor=r.getString("compatibility_floor");if(compatible(clientVersion,floor)){long gameId=r.getLong("game_id");String gameCode=r.getString("game_code"),family=r.getString("family_code"),play=r.getString("play_version"),classificationRegion=r.getString("classification_region_code");RuntimeMetadataProjection.Entry expected=requireCanonicalProjectionRow(projection,gameId,gameCode,family,play,classificationRegion,r.getLong("release_id"),r.getLong("index_generation"),r.getString("catalog_source_hash"),r.getString("snapshot_schema_version"),r.getString("card_codec_version"),r.getString("event_interpreter_version"),r.getString("protocol_version"));Map<String,Object> value=row(r,"game_id","game_code","display_name","category_code","family_code","play_version","release_id","index_generation","cache_epoch","classification_region_code","classification_name","region_type","parent_region_code","parent_region_name");value.putAll(expected.fields());value.putAll(projection.manifestEnvelope());unique.putIfAbsent(gameId,value);}}return List.copyOf(unique.values());}}catch(SQLException e){throw storage(e);}
    }
    public Map<String,Object> configuration(long gameId,String playVersion,String clientVersion){
        return configuration(gameId,null,playVersion,clientVersion);
    }
    public Map<String,Object> configuration(long gameId,String classificationRegionCode,String playVersion,String clientVersion){
        require(playVersion,"playVersion");require(clientVersion,"clientVersion");RuntimeMetadataProjection projection=requireCanonicalLobbyProjection();RuntimeMetadataProjection.Entry expected=requireCanonicalProjectionIdentity(projection,gameId);if(!expected.playVersion().equals(playVersion))throw new HallError(503,"HALL_RUNTIME_METADATA_PROJECTION_CONFLICT","requested play version is not canonical");if(classificationRegionCode!=null&&!classificationRegionCode.isBlank()&&!expected.regionCode().equals(classificationRegionCode))throw new HallError(503,"HALL_RUNTIME_METADATA_PROJECTION_CONFLICT","requested region is not canonical");String region=activeRegion(gameId,playVersion,expected.regionCode());
        String sql="SELECT i.release_id,i.index_generation,i.component_chain,i.rule_validator,i.ui_schema,i.bundle_hash,a.cache_epoch,a.region_code classification_region_code,g.game_code,g.family_code,p.compatibility_floor,rel.snapshot_schema_version,rel.card_codec_version,rel.event_interpreter_version,rel.protocol_version,rel.source_hash FROM aoo_compiled_index_active a JOIN aoo_compiled_room_create_index i ON i.game_id=a.game_id AND i.region_code=a.region_code AND i.play_version=a.play_version AND i.index_generation=a.index_generation JOIN aoo_play_version p ON p.game_id=i.game_id AND p.play_version=i.play_version JOIN aoo_game_catalog g ON g.game_id=i.game_id JOIN aoo_game_release rel ON rel.release_id=i.release_id AND rel.game_id=i.game_id AND rel.play_version=i.play_version AND rel.status='ACTIVE' WHERE a.game_id=? AND a.region_code=? AND a.play_version=? AND i.lifecycle_state='ACTIVE'";
        try(Connection c=dataSource.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,gameId);s.setString(2,region);s.setString(3,playVersion);try(ResultSet r=s.executeQuery()){if(!r.next())throw HallError.notFound("active game configuration not found");requireCompatible(clientVersion,r.getString("compatibility_floor"));RuntimeMetadataProjection.Entry bound=requireCanonicalProjectionRow(projection,gameId,r.getString("game_code"),r.getString("family_code"),playVersion,r.getString("classification_region_code"),r.getLong("release_id"),r.getLong("index_generation"),r.getString("source_hash"),r.getString("snapshot_schema_version"),r.getString("card_codec_version"),r.getString("event_interpreter_version"),r.getString("protocol_version"));Map<String,Object> ui=settlementUi(r.getString("family_code"),document(r.getString("ui_schema")));if(!bound.sourceHash().equals(String.valueOf(ui.get("roomRuleSourceHash"))))throw new HallError(503,"HALL_RUNTIME_METADATA_PROJECTION_CONFLICT","published configuration source hash is not canonical");Map<String,Object> out=row(r,"release_id","index_generation","bundle_hash","cache_epoch");out.put("gameId",gameId);out.put("classificationRegionCode",region);out.put("playVersion",playVersion);out.put("components",document(r.getString("component_chain")));out.put("rules",document(r.getString("rule_validator")));out.put("ui",ui);out.putAll(bound.fields());out.putAll(projection.manifestEnvelope());return out;}}catch(SQLException e){throw storage(e);}
    }
    public Map<String,Object> currentRuleRevision(long gameId,String playVersion,String clientVersion){Map<String,Object> configuration=configuration(gameId,playVersion,clientVersion);Object rawUi=configuration.get("ui");String sourceHash=rawUi instanceof Map<?,?> ui?String.valueOf(ui.get("roomRuleSourceHash")):"";if(sourceHash.isBlank())throw new HallError(503,"HALL_RULE_SCHEMA_UNAVAILABLE","published rule source hash is unavailable");return Map.of("classificationRegionCode",configuration.get("classificationRegionCode"),"releaseId",configuration.get("releaseId"),"indexGeneration",configuration.get("indexGeneration"),"sourceHash",sourceHash);}
public Map<String,Object> activeRoom(long account){return activeRoom(account,null);}
private static final int ACTIVE_ROOM_SCAN_LIMIT=256;
/** Temporary local-only diagnostic. The caller owns the enablement and redacted output. */
public Map<String,Object> activeRoom(long account,java.util.function.Consumer<String> diagnostic){
    String sql="SELECT r.room_id,r.game_id,r.classification_region_code,r.play_version,r.state,r.route_endpoint,r.scope_type,r.club_id,a.lifecycle_state,s.state_payload,(SELECT COUNT(*) FROM aoo_hall_room_member occupied WHERE occupied.room_id=r.room_id AND occupied.status='JOINED') occupied_count FROM aoo_hall_room r JOIN aoo_hall_room_member m ON m.room_id=r.room_id JOIN aoo_room_authority_route a ON a.room_id=r.room_id LEFT JOIN aoo_room_snapshot s ON s.room_id=r.room_id WHERE m.account_id=? AND m.status='JOINED' AND r.state IN('OPEN','PLAYING') ORDER BY r.updated_at DESC,r.room_id DESC";
    try(Connection c=dataSource.getConnection()){
        boolean consistent=false;
        if(diagnostic!=null){try{c.setReadOnly(true);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);c.setAutoCommit(false);consistent=true;}catch(Exception ignored){safeDiagnostic(diagnostic,"result=INCONCLUSIVE reason=SNAPSHOT_UNAVAILABLE");}}
        int candidates=0,rejected=0;String firstReject="NONE";Map<String,Object> found=null;boolean diagnosticScanFailed=false;
        try(PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,account);try(ResultSet r=s.executeQuery()){for(int scanned=0;scanned<=ACTIVE_ROOM_SCAN_LIMIT;scanned++){boolean next;try{next=r.next();}catch(SQLException scanFailure){if(found!=null&&consistent){diagnosticScanFailed=true;break;}throw scanFailure;}if(!next)break;if(scanned==ACTIVE_ROOM_SCAN_LIMIT)throw new IllegalStateException("ACTIVE_ROOM_SCAN_LIMIT_EXCEEDED");candidates++;if(found!=null)continue;String snapshotJson=r.getString("state_payload");Map<String,Object> snapshot=new LinkedHashMap<>();if(snapshotJson!=null){Object value=document(snapshotJson);if(value instanceof Map<?,?> raw){raw.forEach((key,item)->snapshot.put(String.valueOf(key),item));}}if(!LobbyActiveRoomRecoveryPolicy.isRecoverable(r.getString("state"),r.getString("lifecycle_state"),snapshot)){rejected++;if("NONE".equals(firstReject))firstReject=rejectCode(r.getString("state"),r.getString("lifecycle_state"),snapshot);continue;}long room=r.getLong("room_id");Map<String,Object> rules=roomRules(c,room);int playerNum=optionalInt(rules.get("playerCount"),optionalInt(rules.get("playerNum"),0));int occupiedCount=r.getInt("occupied_count");Map<String,Object> out=new LinkedHashMap<>();out.put("active",true);out.put("roomId",room);out.put("gameId",r.getLong("game_id"));out.put("classificationRegionCode",r.getString("classification_region_code"));out.put("playVersion",r.getString("play_version"));out.put("state",r.getString("state"));out.put("route",r.getString("route_endpoint"));out.put("scopeType",r.getString("scope_type"));out.put("rules",rules);out.put("configuration",roomConfiguration(c,room));out.put("playerNum",playerNum);out.put("occupiedCount",occupiedCount);out.put("waitingFull",playerNum>0&&occupiedCount>=playerNum);out.put("roundNo",optionalInt(snapshot.get("roundNo"),0));out.put("roundLimit",optionalInt(snapshot.get("roundLimit"),0));long club=r.getLong("club_id");if(club>0)out.put("clubId",club);out.putAll(resourceRoute(c,room));found=Map.copyOf(out);if(!consistent)break;}}}
        if(diagnostic!=null&&consistent){try{String stages=diagnosticStages(c,account);String result=diagnosticScanFailed||candidates>1?"INCONCLUSIVE":found!=null?"ACTIVE":candidates==1?"POLICY_REJECTED":"SQL_ZERO_CANDIDATE";safeDiagnostic(diagnostic,"result="+result+" "+stages+" candidates="+cap(candidates)+" rejected="+cap(rejected)+" firstReject="+firstReject);}catch(Exception ignored){safeDiagnostic(diagnostic,"result=INCONCLUSIVE reason=DIAGNOSTIC_FAILURE");}}
        if(consistent)try{c.rollback();}catch(SQLException ignored){}
        return found==null?Map.of("active",false):found;
    }catch(SQLException e){throw storage(e);}catch(RuntimeException e){throw e;}catch(Exception e){throw storage(e);}
}
private static void safeDiagnostic(java.util.function.Consumer<String> diagnostic,String line){try{diagnostic.accept(line);}catch(Exception ignored){}}
private static int cap(int count){return Math.min(count,2);}
public static String rejectCode(String state,String authority,Map<String,Object> snapshot){
    if(!"ACTIVE".equalsIgnoreCase(authority))return "AUTHORITY_NOT_ACTIVE";
    if(Boolean.TRUE.equals(snapshot.get("roomTerminal"))||Boolean.TRUE.equals(snapshot.get("dissolved")))return "SNAPSHOT_TERMINAL";
    if(present(snapshot.get("finalSettlement"))||present(snapshot.get("bigSettlement")))return "FINAL_SETTLEMENT_PRESENT";
    if("PLAYING".equalsIgnoreCase(state)&&snapshot.isEmpty())return "PLAYING_SNAPSHOT_MISSING";
    return "POLICY_OTHER";
}
private static boolean present(Object value){return value!=null&&!Boolean.FALSE.equals(value)&&!"false".equalsIgnoreCase(String.valueOf(value))&&!"0".equals(String.valueOf(value))&&!"".equals(String.valueOf(value));}
private static String diagnosticStages(Connection c,long account)throws SQLException{
    int joined=diagnosticCount(c,"SELECT COUNT(*) FROM aoo_hall_room_member WHERE account_id=? AND status='JOINED'",account);
    int open=diagnosticCount(c,"SELECT COUNT(*) FROM aoo_hall_room_member m JOIN aoo_hall_room r ON r.room_id=m.room_id WHERE m.account_id=? AND m.status='JOINED' AND r.state IN('OPEN','PLAYING')",account);
    int route=diagnosticCount(c,"SELECT COUNT(*) FROM aoo_hall_room_member m JOIN aoo_hall_room r ON r.room_id=m.room_id JOIN aoo_room_authority_route a ON a.room_id=r.room_id WHERE m.account_id=? AND m.status='JOINED' AND r.state IN('OPEN','PLAYING')",account);
    int snapshot=diagnosticCount(c,"SELECT COUNT(*) FROM aoo_hall_room_member m JOIN aoo_hall_room r ON r.room_id=m.room_id JOIN aoo_room_authority_route a ON a.room_id=r.room_id JOIN aoo_room_snapshot s ON s.room_id=r.room_id WHERE m.account_id=? AND m.status='JOINED' AND r.state IN('OPEN','PLAYING')",account);
    return "joined="+cap(joined)+" openPlaying="+cap(open)+" route="+cap(route)+" snapshot="+cap(snapshot);
}
private static int diagnosticCount(Connection c,String sql,long account)throws SQLException{try(PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,account);try(ResultSet r=s.executeQuery()){r.next();return r.getInt(1);}}}
public void registerRoom(LobbyRoomCreateSaga.Command command){
    transaction(c->{enforceUniqueRoomMembership(c,command.accountId(),command.roomId(),command.requestId(),command.requestId());return Boolean.TRUE;});
    transaction(c->{
        long game=command.gameId(),room=command.roomId();
        LobbyRoomCreateSaga.RuleRevision revision=command.ruleRevision();
        if(revision==null)throw HallError.bad("HALL_RULE_REVISION_REQUIRED","ruleRevision is required");
        authorizeCreateScope(c,command);
        Index index=lockIndex(c,game,revision.classificationRegionCode(),command.playVersion());
        requireCompatible(command.clientVersion(),index.compatibilityFloor());
        requireRevisionMatches(game,command.playVersion(),index,revision);
        // normalizeCreateCommand already performed the only client-input validation. Its
        // canonical projection may contain server-owned disabled defaults; validating that
        // projection as if it were the original request would incorrectly reject it.
        Map<String,Object> validatedRules=command.normalizedRules()
                ? Map.copyOf(command.rules())
                : validateRules(c,game,index.regionCode(),command.playVersion(),gameplayRules(command));
        if(command.creationOrigin()==LobbyRoomCreateSaga.CreationOrigin.CLUB_TEMPLATE_ENTRY)
            validatedRules=mergeClubTemplatePolicy(validatedRules,command.rules());
        if(existingRoomIsIdempotent(c,command,index,validatedRules))return Boolean.TRUE;
        String route=route(c,game,command.playVersion());
        try(PreparedStatement s=c.prepareStatement("INSERT INTO aoo_hall_room(room_id,game_id,classification_region_code,play_version,release_id,index_generation,owner_account_id,scope_type,club_id,club_template_code,state,route_endpoint,rules_json,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,'CREATING',?,?,CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))")){
            int i=1;s.setLong(i++,room);s.setLong(i++,game);s.setString(i++,index.regionCode());s.setString(i++,command.playVersion());
            s.setLong(i++,index.releaseId());s.setLong(i++,index.generation());s.setLong(i++,command.accountId());s.setString(i++,command.scope().type());
            if(command.scope().club())s.setLong(i++,command.scope().clubId());else s.setNull(i++,Types.BIGINT);
            if(command.scope().club())s.setString(i++,command.scope().templateCode());else s.setNull(i++,Types.VARCHAR);
            s.setString(i++,route);s.setString(i,json.writeValueAsString(validatedRules));s.executeUpdate();
        }
        try(PreparedStatement s=c.prepareStatement("INSERT INTO aoo_hall_room_member(room_id,account_id,seat_no,status,joined_at) VALUES(?,?,0,'JOINED',CURRENT_TIMESTAMP(3))")){
            s.setLong(1,room);s.setLong(2,command.accountId());s.executeUpdate();
        }
        insertRoomRuleLock(c,room,game,command.playVersion(),index,validatedRules);
        return Boolean.TRUE;
    });
}
/**
 * Resolves the published rule schema once for the create saga.  The returned
 * command is the only rule projection that downstream billing is allowed to
 * consume; no billing-side defaults or UI aliases are introduced here.
 */
public LobbyRoomCreateSaga.Command normalizeCreateCommand(LobbyRoomCreateSaga.Command command){
    Objects.requireNonNull(command,"command");
    LobbyRoomCreateSaga.RuleRevision revision=command.ruleRevision();
    if(revision==null)throw HallError.bad("HALL_RULE_REVISION_REQUIRED","ruleRevision is required");
    String sql="SELECT rule_validator FROM aoo_compiled_room_create_index i JOIN aoo_compiled_index_active a ON a.game_id=i.game_id AND a.region_code=i.region_code AND a.play_version=i.play_version AND a.index_generation=i.index_generation WHERE i.game_id=? AND i.region_code=? AND i.play_version=? AND i.lifecycle_state='ACTIVE'";
    try(Connection c=dataSource.getConnection();PreparedStatement s=c.prepareStatement(sql)){
        s.setLong(1,command.gameId());s.setString(2,revision.classificationRegionCode());s.setString(3,command.playVersion());
        try(ResultSet r=s.executeQuery()){
            if(!r.next())throw new HallError(503,"HALL_RULE_SCHEMA_UNAVAILABLE","published room rule schema is unavailable");
            Map<String,Object> normalized=LobbyRoomRuleSchemaValidator.validate(document(r.getString(1)),gameplayRules(command),command.gameId(),command.playVersion());
            if(command.creationOrigin()==LobbyRoomCreateSaga.CreationOrigin.CLUB_TEMPLATE_ENTRY)
                normalized=mergeClubTemplatePolicy(normalized,command.rules());
            return command.withValidatedRules(normalized);
        }
    }catch(HallError error){throw error;}catch(SQLException error){throw storage(error);}catch(RuntimeException error){throw error;}
}
public Map<String,Object> confirmRoom(long account,long room){return transaction(c->{Room value=lockRoom(c,room);if(value.state().equals("CREATING") == false)throw HallError.conflict("HALL_ROOM_STATE_CONFLICT","room is not awaiting authority confirmation");try(PreparedStatement s=c.prepareStatement("UPDATE aoo_hall_room SET state='OPEN',updated_at=CURRENT_TIMESTAMP(3) WHERE room_id=? AND state='CREATING'")){s.setLong(1,room);if(s.executeUpdate()!=1)throw HallError.conflict("HALL_ROOM_STATE_CONFLICT","room confirmation lost its state transition");}if(value.clubId()>0)projectClubRoom(c,value,room,"OPEN");member(c,room,account);Map<String,Object> out=new LinkedHashMap<>(Map.of("roomId",room,"gameId",value.gameId(),"gameCode",value.gameCode(),"playVersion",value.playVersion(),"classificationRegionCode",value.region(),"route",value.route(),"state","OPEN","scopeType",value.scopeType()));out.put("rules",roomRules(c,room));out.put("configuration",roomConfiguration(c,room));out.put("runtimeIdentity",runtimeIdentity(c,room));out.put("roomLockMetadata",roomLockMetadata(c,room));out.putAll(resourceRoute(c,room));if(value.clubId()>0)out.put("clubId",value.clubId());return Map.copyOf(out);});}
/** Trusted Saga completion; OPEN replay is accepted only with the original persisted creation binding. */
public LobbyRoomCreateSaga.RoomConfirmation confirmRoomCompletion(LobbyRoomCreateSaga.Command command,long nextVersion){
    Objects.requireNonNull(command,"command");
    try{return transaction(c->{
        try(PreparedStatement q=c.prepareStatement("SELECT request_hash,account_id,room_id,game_id,play_version,state_version,step FROM aoo_room_create_saga WHERE request_id=? FOR UPDATE")){
            q.setString(1,command.requestId());try(ResultSet r=q.executeQuery()){
                if(!r.next()||!command.requestHash().equals(r.getString(1))||r.getLong(2)!=command.accountId()||r.getLong(3)!=command.roomId()||r.getInt(4)!=command.gameId()||!command.playVersion().equals(r.getString(5))||r.getLong(6)+1!=nextVersion||!"AUTHORITY_CREATED".equals(r.getString(7)))throw new IllegalStateException("room confirmation request binding mismatch");
            }
        }
        LobbyRoomCreateSaga.RuleRevision revision=Objects.requireNonNull(command.ruleRevision(),"room rule revision");
        try(PreparedStatement q=c.prepareStatement("SELECT owner_account_id,game_id,play_version,classification_region_code,release_id,index_generation,scope_type,club_id,club_template_code,rules_json FROM aoo_hall_room WHERE room_id=? FOR UPDATE")){
            q.setLong(1,command.roomId());try(ResultSet r=q.executeQuery()){
                if(!r.next())throw new IllegalStateException("room confirmation room missing");long club=r.getLong(8);boolean clubNull=r.wasNull();
                if(r.getLong(1)!=command.accountId()||r.getInt(2)!=command.gameId()||!command.playVersion().equals(r.getString(3))||!revision.classificationRegionCode().equals(r.getString(4))||r.getLong(5)!=revision.releaseId()||r.getLong(6)!=revision.indexGeneration()||!command.scope().type().equals(r.getString(7))||(command.scope().club()?(clubNull||club!=command.scope().clubId()):!clubNull)||!Objects.equals(command.scope().club()?command.scope().templateCode():null,r.getString(9))||!sameJson(r.getString(10),command.rules()))throw new IllegalStateException("room confirmation durable room binding mismatch");
            }
        }
        try(PreparedStatement q=c.prepareStatement("SELECT game_id,region_code,play_version,release_id,index_generation,source_hash,immutable_rules FROM aoo_room_rule_lock WHERE room_id=? FOR UPDATE")){
            q.setLong(1,command.roomId());try(ResultSet r=q.executeQuery()){
                if(!r.next()||r.getLong(1)!=command.gameId()||!revision.classificationRegionCode().equals(r.getString(2))||!command.playVersion().equals(r.getString(3))||r.getLong(4)!=revision.releaseId()||r.getLong(5)!=revision.indexGeneration()||!revision.sourceHash().equals(r.getString(6))||!sameJson(r.getString(7),command.rules()))throw new IllegalStateException("room confirmation rule lock binding mismatch");
            }
        }
        try(PreparedStatement q=c.prepareStatement("SELECT seat_no,status FROM aoo_hall_room_member WHERE room_id=? AND account_id=? FOR UPDATE")){
            q.setLong(1,command.roomId());q.setLong(2,command.accountId());try(ResultSet r=q.executeQuery()){if(!r.next()||r.getInt(1)!=0||!"JOINED".equals(r.getString(2)))throw new IllegalStateException("room confirmation creator membership mismatch");}
        }
        Room value=lockRoom(c,command.roomId());boolean replay="OPEN".equals(value.state());
        if(!replay&&!"CREATING".equals(value.state()))throw new IllegalStateException("room is not awaiting or replaying confirmation");
        if(!replay){try(PreparedStatement q=c.prepareStatement("UPDATE aoo_hall_room SET state='OPEN',updated_at=CURRENT_TIMESTAMP(3) WHERE room_id=? AND state='CREATING'")){q.setLong(1,command.roomId());if(q.executeUpdate()!=1)throw new IllegalStateException("room confirmation transition lost");}if(value.clubId()>0)projectClubRoom(c,value,command.roomId(),"OPEN");}
        Map<String,Object> out=new LinkedHashMap<>(Map.of("roomId",command.roomId(),"gameId",value.gameId(),"gameCode",value.gameCode(),"playVersion",value.playVersion(),"classificationRegionCode",value.region(),"route",value.route(),"state","OPEN","scopeType",value.scopeType()));
        out.put("rules",roomRules(c,command.roomId()));out.put("configuration",roomConfiguration(c,command.roomId()));out.put("runtimeIdentity",runtimeIdentity(c,command.roomId()));out.put("roomLockMetadata",roomLockMetadata(c,command.roomId()));out.putAll(resourceRoute(c,command.roomId()));if(value.clubId()>0)out.put("clubId",value.clubId());
        return new LobbyRoomCreateSaga.RoomConfirmation(out,replay);
    });}catch(LobbyRoomCreateSaga.FinalConfirmationRecoveryException failure){throw failure;}
    catch(RuntimeException failure){throw new LobbyRoomCreateSaga.FinalConfirmationRecoveryException("room confirmation binding or durable recovery is uncertain",failure);}
}
public long allocateRoomId(long account,String requestId){
    requireKey(requestId);
    return transaction(c->{
        try(PreparedStatement existing=c.prepareStatement("SELECT account_id,room_id FROM aoo_room_idempotent_allocation WHERE request_id=? FOR UPDATE")){
            existing.setString(1,requestId);
            try(ResultSet r=existing.executeQuery()){
                if(r.next()){
                    if(r.getLong(1)!=account)throw HallError.conflict("HALL_REQUEST_ID_CONFLICT","requestId belongs to another account");
                    return r.getLong(2);
                }
            }
        }
        // 房间号属于对外展示标识，不能用账号/时间推导；数据库唯一键负责并发兜底，
        // 这里只对唯一冲突重试，避免把冲突误当成成功或覆盖已有房间。
        for(int attempt=0;attempt<16;attempt++){
            long candidate=100_000L+random.nextInt(900_000);
            try(PreparedStatement insert=c.prepareStatement("INSERT INTO aoo_room_idempotent_allocation(request_id,account_id,room_id,created_at) VALUES(?,?,?,CURRENT_TIMESTAMP(3))")){
                insert.setString(1,requestId);insert.setLong(2,account);insert.setLong(3,candidate);insert.executeUpdate();
                return candidate;
            }catch(SQLIntegrityConstraintViolationException conflict){
                // 并发请求只会在 room_id 或 request_id 唯一键上冲突；下一轮生成新候选。
            }
        }
        throw new IllegalStateException("六位房间号分配冲突次数超限");
    });
}
public void compensateCreate(long account,String key,long room){transaction(c->{Room value=room(c,room,false);try(PreparedStatement q=c.prepareStatement("SELECT owner_account_id FROM aoo_hall_room WHERE room_id=? FOR UPDATE")){q.setLong(1,room);try(ResultSet r=q.executeQuery()){if(!r.next())return Boolean.TRUE;if(r.getLong(1)!=account)throw new HallError(403,"HALL_FORBIDDEN","only the room owner can compensate creation");}}try(PreparedStatement t=c.prepareStatement("DELETE FROM aoo_hall_game_ticket WHERE room_id=?")){t.setLong(1,room);t.executeUpdate();}try(PreparedStatement m=c.prepareStatement("UPDATE aoo_hall_room_member SET status='LEFT',left_at=COALESCE(left_at,CURRENT_TIMESTAMP(3)) WHERE room_id=? AND status='JOINED'")){m.setLong(1,room);m.executeUpdate();}try(PreparedStatement r=c.prepareStatement("UPDATE aoo_hall_room SET state='DISSOLVED',updated_at=CURRENT_TIMESTAMP(3) WHERE room_id=? AND state<>'DISSOLVED'")){r.setLong(1,room);r.executeUpdate();}if(value.clubId()>0)projectClubRoom(c,value,room,"DISSOLVED");return Boolean.TRUE;});}
public Map<String,Object> closeRoom(long room,String key,String reason,String traceId){requireKey(key);return transaction(c->{Room value=lockRoom(c,room);if(!"DISSOLVED".equals(value.state())){try(PreparedStatement t=c.prepareStatement("DELETE FROM aoo_hall_game_ticket WHERE room_id=?")){t.setLong(1,room);t.executeUpdate();}try(PreparedStatement m=c.prepareStatement("UPDATE aoo_hall_room_member SET status='LEFT',left_at=COALESCE(left_at,CURRENT_TIMESTAMP(3)) WHERE room_id=? AND status IN('JOINING','JOINED')")){m.setLong(1,room);m.executeUpdate();}try(PreparedStatement r=c.prepareStatement("UPDATE aoo_hall_room SET state='DISSOLVED',updated_at=CURRENT_TIMESTAMP(3) WHERE room_id=? AND state<>'DISSOLVED'")){r.setLong(1,room);r.executeUpdate();}if(value.clubId()>0)projectClubRoom(c,value,room,"DISSOLVED");}return Map.of("roomId",room,"state","DISSOLVED","requestId",key,"reason",reason==null?"AUTHORITY_REMOVED":reason,"traceId",traceId);});}
    public Map<String,Object> join(long account,String key,long room,String traceId){return join(account,key,room,traceId,Map.of());}
    public Map<String,Object> join(long account,String key,long room,String traceId,Map<String,Object>admission){
        admission=Map.copyOf(Objects.requireNonNull(admission));
        requireKey(key);require(traceId,"traceId");
        Map<String,Object> prior=transaction(c->idempotent(c,account,key,"JOIN"));
        if(prior!=null)return prior;
        JoinReservation reservation=transaction(c->{
            Room value=lockRoom(c,room);
            if(!"OPEN".equals(value.state())){
                logJoinRejected("HALL_ROOM_ENDED",room,account,key,traceId,
                        "roomState="+value.state()+" constraint=ROOM_NOT_OPEN");
                throw HallError.conflict("HALL_ROOM_ENDED","room is no longer joinable");
            }
            authorizeJoinScope(c,value,account);
            enforceUniqueRoomMembership(c,account,room,key,traceId);
            // LEFT rows are terminal history for the previous admission, but the
            // room-seat unique key still reserves their seat number. 2.2.2 reuses a
            // vacated chair immediately, so retire those operational rows before
            // calculating the next seat instead of colliding on (room_id, seat_no).
            try(PreparedStatement retired=c.prepareStatement("DELETE FROM aoo_hall_room_member WHERE room_id=? AND status='LEFT'")){
                retired.setLong(1,room);retired.executeUpdate();
            }
            int seat;
            boolean alreadyJoined=false;
            try(PreparedStatement existing=c.prepareStatement("SELECT seat_no FROM aoo_hall_room_member WHERE room_id=? AND account_id=? AND status IN('JOINING','JOINED') FOR UPDATE")){
                existing.setLong(1,room);existing.setLong(2,account);
                try(ResultSet found=existing.executeQuery()){
                    if(found.next()){seat=found.getInt(1);alreadyJoined=membershipStatus(c,room,account).equals("JOINED");}
                    else{
                        try(PreparedStatement q=c.prepareStatement("SELECT seat_no FROM aoo_hall_room_member WHERE room_id=? AND status IN('JOINING','JOINED') ORDER BY seat_no FOR UPDATE")){
                            q.setLong(1,room);
                            try(ResultSet r=q.executeQuery()){
                                List<Integer> occupied=new ArrayList<>();
                                while(r.next())occupied.add(r.getInt(1));
                                seat=firstVacantSeat(occupied);
                            }
                        }
                    }
                }
            }
            try(PreparedStatement s=c.prepareStatement("INSERT INTO aoo_hall_room_member(room_id,account_id,seat_no,status,joined_at) VALUES(?,?,?,'JOINING',CURRENT_TIMESTAMP(3)) ON DUPLICATE KEY UPDATE seat_no=VALUES(seat_no),status=IF(status='JOINED','JOINED','JOINING'),joined_at=CURRENT_TIMESTAMP(3),left_at=NULL")){
                s.setLong(1,room);s.setLong(2,account);s.setInt(3,seat);s.executeUpdate();
            }
            return new JoinReservation(seat,value.gameId(),value.gameCode(),value.playVersion(),value.state(),value.route(),alreadyJoined);
        });
        Map<String,Object> authority;
        try{authority=membershipAuthority.join(room,account,reservation.seat(),reservation.playVersion(),key,traceId,admission);}
        catch(LobbyRoomAuthorityRejected rejected){logJoinRejected(rejected.code(),room,account,key,traceId,
                "seatNo="+reservation.seat()+" alreadyJoined="+reservation.alreadyJoined()+" constraint=AUTHORITY");releaseFailedJoin(account,room,reservation.alreadyJoined());if("ROOM_ENDED".equals(rejected.code())){closeStaleRoom(room);throw HallError.conflict("HALL_ROOM_ENDED","room is no longer joinable");}if("ROOM_FULL".equals(rejected.code()))throw HallError.conflict("HALL_ROOM_FULL","房间人数已满");if("ROOM_JOIN_REJECTED".equals(rejected.code()))throw HallError.conflict("HALL_ROOM_JOIN_REJECTED","不满足该房间的加入条件");throw new HallError(rejected.status(),"HALL_AUTHORITY_REJECTED","room authority rejected membership");}
        catch(RuntimeException failure){releaseFailedJoin(account,room,reservation.alreadyJoined());throw failure;}
        if(((Number)authority.getOrDefault("roomId",0)).longValue()!=room||((Number)authority.getOrDefault("accountId",0)).longValue()!=account||((Number)authority.getOrDefault("seatNo",-1)).intValue()!=reservation.seat())throw new IllegalStateException("room authority returned mismatched membership");
        return transaction(c->{
            Map<String,Object> replay=idempotent(c,account,key,"JOIN");
            if(replay!=null)return replay;
            lockRoom(c,room);
            try(PreparedStatement s=c.prepareStatement("UPDATE aoo_hall_room_member SET status='JOINED',joined_at=CURRENT_TIMESTAMP(3),left_at=NULL WHERE room_id=? AND account_id=? AND seat_no=? AND status IN('JOINING','JOINED')")){
                s.setLong(1,room);s.setLong(2,account);s.setInt(3,reservation.seat());
                if(s.executeUpdate()!=1)throw new IllegalStateException("reserved Hall membership disappeared before authority confirmation");
            }
            Map<String,Object> result=Map.of("roomId",room,"accountId",account,"seatNo",reservation.seat(),
                    "gameId",reservation.gameId(),"gameCode",reservation.gameCode(),"playVersion",reservation.playVersion(),
                    "state",reservation.state(),"route",reservation.route());
            saveIdempotent(c,account,key,"JOIN",result);
            return result;
        });
    }
    private void enforceUniqueRoomMembership(Connection c,long account,long requestedRoom,String operationId,String traceId)throws Exception{try(PreparedStatement s=c.prepareStatement("SELECT m.room_id,m.status,r.state,a.lifecycle_state,s.state_payload FROM aoo_hall_room_member m JOIN aoo_hall_room r ON r.room_id=m.room_id LEFT JOIN aoo_room_authority_route a ON a.room_id=r.room_id LEFT JOIN aoo_room_snapshot s ON s.room_id=r.room_id WHERE m.account_id=? AND m.room_id<>? AND m.status IN('JOINING','JOINED') FOR UPDATE")){s.setLong(1,account);s.setLong(2,requestedRoom);try(ResultSet r=s.executeQuery()){while(r.next()){long priorRoom=r.getLong(1);String memberState=r.getString(2),roomState=r.getString(3),authorityState=r.getString(4);Map<String,Object> snapshot=r.getString(5)==null?Map.of():json.readValue(r.getString(5),new TypeReference<>(){});if("JOINING".equals(memberState)||!LobbyActiveRoomRecoveryPolicy.isRecoverable(roomState,authorityState,snapshot))markMemberLeft(c,priorRoom,account);else{logJoinRejected("HALL_ALREADY_IN_ANOTHER_ROOM",requestedRoom,account,operationId,traceId,"priorRoomId="+priorRoom+" memberState="+memberState+" roomState="+roomState+" authorityState="+authorityState+" constraint=RECOVERABLE_PRIOR_ROOM");throw HallError.conflict("HALL_ALREADY_IN_ANOTHER_ROOM","account is already in room "+priorRoom);}}}}}
    private static void logJoinRejected(String code,long room,long account,String operationId,String traceId,String summary){System.err.println("[HallJoinRejected] errorCode="+code+" roomId="+room+" playerId="+account+" operationId="+operationId+" traceId="+traceId+" "+summary);}
    private String membershipStatus(Connection c,long room,long account)throws Exception{try(PreparedStatement s=c.prepareStatement("SELECT status FROM aoo_hall_room_member WHERE room_id=? AND account_id=?")){s.setLong(1,room);s.setLong(2,account);try(ResultSet r=s.executeQuery()){return r.next()?r.getString(1):"";}}}
    private void markMemberLeft(Connection c,long room,long account)throws Exception{try(PreparedStatement t=c.prepareStatement("DELETE FROM aoo_hall_game_ticket WHERE room_id=? AND account_id=?")){t.setLong(1,room);t.setLong(2,account);t.executeUpdate();}try(PreparedStatement s=c.prepareStatement("UPDATE aoo_hall_room_member SET status='LEFT',left_at=COALESCE(left_at,CURRENT_TIMESTAMP(3)) WHERE room_id=? AND account_id=? AND status IN('JOINING','JOINED')")){s.setLong(1,room);s.setLong(2,account);s.executeUpdate();}}
    private void releaseFailedJoin(long account,long room,boolean alreadyJoined){if(alreadyJoined)return;transaction(c->{markMemberLeft(c,room,account);return Boolean.TRUE;});}
    private void closeStaleRoom(long room){transaction(c->{try(PreparedStatement t=c.prepareStatement("DELETE FROM aoo_hall_game_ticket WHERE room_id=?")){t.setLong(1,room);t.executeUpdate();}try(PreparedStatement m=c.prepareStatement("UPDATE aoo_hall_room_member SET status='LEFT',left_at=COALESCE(left_at,CURRENT_TIMESTAMP(3)) WHERE room_id=? AND status IN('JOINING','JOINED')")){m.setLong(1,room);m.executeUpdate();}try(PreparedStatement r=c.prepareStatement("UPDATE aoo_hall_room SET state='DISSOLVED',updated_at=CURRENT_TIMESTAMP(3) WHERE room_id=? AND state IN('OPEN','PLAYING')")){r.setLong(1,room);r.executeUpdate();}return Boolean.TRUE;});}
public Map<String,Object> room(long account,long room){try(Connection c=dataSource.getConnection()){Room value=room(c,room,false);member(c,room,account);List<Map<String,Object>> members=new ArrayList<>();try(PreparedStatement s=c.prepareStatement("SELECT account_id,seat_no,status,joined_at,left_at FROM aoo_hall_room_member WHERE room_id=? ORDER BY seat_no")){s.setLong(1,room);try(ResultSet r=s.executeQuery()){while(r.next())members.add(row(r,"account_id","seat_no","status","joined_at","left_at"));}}Map<String,Object> out=new LinkedHashMap<>(Map.of("roomId",room,"gameId",value.gameId(),"gameCode",value.gameCode(),"playVersion",value.playVersion(),"classificationRegionCode",value.region(),"state",value.state(),"route",value.route(),"members",members,"scopeType",value.scopeType()));out.put("rules",roomRules(c,room));out.put("configuration",roomConfiguration(c,room));out.put("runtimeIdentity",runtimeIdentity(c,room));out.put("roomLockMetadata",roomLockMetadata(c,room));out.put("leaveAdmission",leaveAdmission(room,value,out));out.putAll(resourceRoute(c,room));if(value.clubId()>0)out.put("clubId",value.clubId());return out;}catch(RuntimeException e){throw e;}catch(Exception e){throw storage(e);}}
    public Map<String,Object> leave(long account,String key,long room){return leave(account,key,room,key,Map.of());}
    public Map<String,Object> leaveAuthenticated(LobbyRequestAuthenticator.Principal actor,String key,long room,
            String traceId,Map<String,Object> suppliedIdentity){
        Objects.requireNonNull(actor,"authenticatedPrincipal");
        requirePublicLeaveIdentity(suppliedIdentity);
        if(!Long.toString(room).equals(suppliedIdentity.get("roomId")))
            throw HallError.conflict("HALL_ROOM_IDENTITY_MISMATCH","leave identity belongs to another room");
        Map<String,Object> identity=new LinkedHashMap<>(suppliedIdentity);
        identity.put("actorType","ACCOUNT_HTTP");identity.put("accountSessionId",actor.accountSessionId());
        identity.put("accountId",actor.accountId());identity.put("permission","AUTHENTICATED_ROOM_MEMBER");
        return leave(actor.accountId(),key,room,traceId,identity);
    }
    static void requirePublicLeaveIdentity(Map<String,Object> identity){
        if(identity==null||!PUBLIC_LEAVE_FIELDS.equals(identity.keySet()))
            throw HallError.bad("HALL_AUTHORITATIVE_FIELD_FORBIDDEN","leave requires only the published identity fields");
        for(String key:PUBLIC_LEAVE_FIELDS){Object value=identity.get(key);
            if(!(value instanceof String text)||text.isBlank()||!text.equals(text.strip()))
                throw HallError.bad("HALL_INVALID_REQUEST","leave identity must be lossless text");}
        for(String key:List.of("roomId","gameId","lifecycleEpoch","fencingToken","stateVersion","releaseId","indexGeneration")){
            String value=(String)identity.get(key);
            if(!isLeaveIdentityNumber(value)||(!"stateVersion".equals(key)&&"0".equals(value)))
                throw HallError.bad("HALL_INVALID_REQUEST","leave identity integer is invalid");
        }
        for(String key:List.of("sourceHash","ruleContentHash","componentContentHash"))requireHash((String)identity.get(key),"HALL_INVALID_REQUEST");
        for(String key:List.of("playVersion","cardCodecVersion","eventInterpreterVersion","protocolVersion","snapshotSchemaVersion"))
            requireVersion((String)identity.get(key),"HALL_INVALID_REQUEST");
    }
    public Map<String,Object> leave(long account,String key,long room,String traceId,Map<String,Object> suppliedIdentity){
        requireKey(key);require(traceId,"traceId");requireLeaveIdentity(suppliedIdentity);
        Map<String,Object> identity=new LinkedHashMap<>(suppliedIdentity);
        identity.put("roomId",room);identity.put("accountId",account);identity.put("requestId",key);
        identity.put("idempotencyKey",key);identity.put("traceId",traceId);
        Map<String,Object>prior=transaction(c->idempotent(c,account,key,"LEAVE"));if(prior!=null)return matchingLeaveReceipt(prior,account,room);
        Room roomValue=transaction(c->{Room value=lockRoom(c,room);String status=membershipStatus(c,room,account);if(status.isBlank())throw new HallError(403,"HALL_ROOM_ACCESS_DENIED","room membership required");return value;});
        if("ACCOUNT_HTTP".equals(identity.get("actorType"))&&!roomValue.playVersion().equals(identity.get("playVersion")))
            throw HallError.conflict("HALL_ROOM_IDENTITY_MISMATCH","leave play version does not match the room");
        Map<String,Object> authority=membershipAuthority.leave(room,account,roomValue.playVersion(),key,traceId,Map.copyOf(identity));
            String status=String.valueOf(authority==null?null:authority.get("status"));
            if(!"LEFT".equals(status)&&!"ALREADY_LEFT".equals(status))throw new IllegalStateException("ROOM_ORCHESTRATOR_LEAVE_RESULT_REQUIRED");
        return transaction(c->{Map<String,Object> replay=idempotent(c,account,key,"LEAVE");if(replay!=null)return matchingLeaveReceipt(replay,account,room);Room value=lockRoom(c,room);markMemberLeft(c,room,account);dissolveEmptyClubRoom(c,value,room);Map<String,Object>result=Map.of("roomId",room,"accountId",account,"status","LEFT");saveIdempotent(c,account,key,"LEAVE",result);return result;});
    }
    /** An exception has no authoritative terminal receipt, regardless of its code. */
    public static boolean authorityConfirmsAlreadyLeft(LobbyRoomAuthorityRejected rejected){return false;}
    private static Map<String,Object> matchingLeaveReceipt(Map<String,Object> prior,long account,long room){
        if(!Long.toString(account).equals(String.valueOf(prior.get("accountId")))
                ||!Long.toString(room).equals(String.valueOf(prior.get("roomId")))||!"LEFT".equals(prior.get("status")))
            throw HallError.conflict("HALL_IDEMPOTENCY_CONFLICT","leave receipt does not match actor and room");
        return prior;
    }
    static void requireLeaveIdentity(Map<String,Object> identity){
        if(identity==null)throw new IllegalArgumentException("ROOM_ORCHESTRATOR_LEAVE_IDENTITY_REQUIRED");
        boolean http="ACCOUNT_HTTP".equals(identity.get("actorType"));
        for(String key:LEAVE_IDENTITY_FIELDS){if(http&&"sessionId".equals(key))continue;Object value=identity.get(key);if(value==null||(value instanceof String text&&text.isBlank()))throw new IllegalArgumentException("ROOM_ORCHESTRATOR_LEAVE_IDENTITY_REQUIRED:"+key);}
        if(!isLeaveIdentityNumber(identity.get("gameId"))||!isLeaveIdentityNumber(identity.get("releaseId"))||!isLeaveIdentityNumber(identity.get("indexGeneration"))||!isLeaveIdentityNumber(identity.get("lifecycleEpoch"))||!isLeaveIdentityNumber(identity.get("fencingToken"))||!isLeaveIdentityNumber(identity.get("stateVersion")))throw new IllegalArgumentException("ROOM_ORCHESTRATOR_LEAVE_IDENTITY_REQUIRED");
        if(http){
            if(!(identity.get("accountSessionId") instanceof String sid)||sid.isBlank()||identity.containsKey("sessionId")
                    ||!(identity.get("accountId") instanceof Number account)||account.longValue()<=0
                    ||!"AUTHENTICATED_ROOM_MEMBER".equals(identity.get("permission")))
                throw new SecurityException("ROOM_ORCHESTRATOR_LEAVE_HTTP_ACTOR_REQUIRED");
        }else if(!"SEATED_MEMBER".equals(String.valueOf(identity.get("permission"))))throw new SecurityException("ROOM_ORCHESTRATOR_LEAVE_PERMISSION_REQUIRED");
    }

    /** Preserve numeric compatibility while admitting only lossless wire integers for JS clients. */
    private static boolean isLeaveIdentityNumber(Object value){
        if(value instanceof Number)return true;
        if(!(value instanceof String text)||!text.matches(CANONICAL_DECIMAL))return false;
        try{return new BigInteger(text).compareTo(BigInteger.valueOf(Long.MAX_VALUE))<=0;}catch(NumberFormatException ignored){return false;}
    }

    private void dissolveEmptyClubRoom(Connection c,Room value,long room)throws Exception{
        if(value.clubId()<=0||!("OPEN".equals(value.state())||"PLAYING".equals(value.state())))return;
        try(PreparedStatement q=c.prepareStatement("SELECT COUNT(*) FROM aoo_hall_room_member WHERE room_id=? AND status IN('JOINING','JOINED')")){
            q.setLong(1,room);
            try(ResultSet r=q.executeQuery()){
                r.next();
                if(r.getLong(1)>0)return;
            }
        }
        try(PreparedStatement t=c.prepareStatement("DELETE FROM aoo_hall_game_ticket WHERE room_id=?")){t.setLong(1,room);t.executeUpdate();}
        try(PreparedStatement r=c.prepareStatement("UPDATE aoo_hall_room SET state='DISSOLVED',updated_at=CURRENT_TIMESTAMP(3) WHERE room_id=? AND state IN('OPEN','PLAYING')")){r.setLong(1,room);r.executeUpdate();}
        projectClubRoom(c,value,room,"DISSOLVED");
    }
    public Map<String,Object> ticket(long account,String key,long room,String device){requireKey(key);require(device,"deviceFingerprint");return transaction(c->{Map<String,Object> prior=idempotent(c,account,key,"TICKET");if(prior!=null)return prior;Room value=lockRoom(c,room);member(c,room,account);byte[] entropy=new byte[32];random.nextBytes(entropy);String ticket=Base64.getUrlEncoder().withoutPadding().encodeToString(entropy);try(PreparedStatement s=c.prepareStatement("INSERT INTO aoo_hall_game_ticket(ticket_hash,room_id,account_id,device_fingerprint,route_endpoint,expires_at,created_at) VALUES(SHA2(?,256),?,?,?,?,DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL 30 SECOND),CURRENT_TIMESTAMP(3))")){s.setString(1,ticket);s.setLong(2,room);s.setLong(3,account);s.setString(4,device);s.setString(5,value.route());s.executeUpdate();}Map<String,Object> result=Map.of("ticket",ticket,"roomId",room,"route",value.route(),"expiresInSeconds",30);saveIdempotent(c,account,key,"TICKET",result);return result;});}
    /** Game-service boundary: atomically consumes the durable ticket exactly once. */
    public Map<String,Object> consumeTicket(String ticket,String device){require(ticket,"ticket");require(device,"deviceFingerprint");return transaction(c->{try(PreparedStatement s=c.prepareStatement("SELECT room_id,account_id,route_endpoint,device_fingerprint,expires_at,consumed_at FROM aoo_hall_game_ticket WHERE ticket_hash=SHA2(?,256) FOR UPDATE")){s.setString(1,ticket);try(ResultSet r=s.executeQuery()){if(!r.next()||r.getTimestamp("consumed_at")!=null||!clock.instant().isBefore(r.getTimestamp("expires_at").toInstant())||!device.equals(r.getString("device_fingerprint")))throw new HallError(401,"HALL_TICKET_REJECTED","game ticket invalid, expired, or already consumed");long room=r.getLong("room_id"),account=r.getLong("account_id");String route=r.getString("route_endpoint");try(PreparedStatement u=c.prepareStatement("UPDATE aoo_hall_game_ticket SET consumed_at=CURRENT_TIMESTAMP(3) WHERE ticket_hash=SHA2(?,256) AND consumed_at IS NULL")){u.setString(1,ticket);if(u.executeUpdate()!=1)throw new HallError(401,"HALL_TICKET_REJECTED","game ticket already consumed");}return Map.of("roomId",room,"accountId",account,"route",route);}}});}

    private Map<String,Object> validateRules(Connection c,long game,String region,String play,Object rules)throws Exception{try(PreparedStatement s=c.prepareStatement("SELECT rule_validator FROM aoo_compiled_room_create_index i JOIN aoo_compiled_index_active a ON a.game_id=i.game_id AND a.region_code=i.region_code AND a.play_version=i.play_version AND a.index_generation=i.index_generation WHERE i.game_id=? AND i.region_code=? AND i.play_version=? AND i.lifecycle_state='ACTIVE'")){s.setLong(1,game);s.setString(2,region);s.setString(3,play);try(ResultSet r=s.executeQuery()){if(!r.next())throw new HallError(503,"HALL_RULE_SCHEMA_UNAVAILABLE","published room rule schema is unavailable");return LobbyRoomRuleSchemaValidator.validate(document(r.getString(1)),rules,game,play);}}}
    private static Map<String,Object> gameplayRules(LobbyRoomCreateSaga.Command command){
        if(command.creationOrigin()!=LobbyRoomCreateSaga.CreationOrigin.CLUB_TEMPLATE_ENTRY)return command.rules();
        Map<String,Object> gameplay=new LinkedHashMap<>(command.rules());
        CLUB_TEMPLATE_POLICY_FIELDS.forEach(gameplay::remove);
        return Map.copyOf(gameplay);
    }
    public static Map<String,Object> mergeClubTemplatePolicy(Map<String,Object> gameplay,Map<String,Object> submitted){
        Map<String,Object> merged=new LinkedHashMap<>(gameplay);
        CLUB_TEMPLATE_POLICY_FIELDS.forEach(key->{if(submitted.containsKey(key))merged.put(key,submitted.get(key));});
        return Map.copyOf(merged);
    }
    private Map<String,Object> resourceRoute(Connection c,long room)throws Exception{String sql="SELECT i.ui_schema FROM aoo_hall_room h JOIN aoo_compiled_room_create_index i ON i.game_id=h.game_id AND i.region_code=h.classification_region_code AND i.play_version=h.play_version AND i.index_generation=h.index_generation AND i.release_id=h.release_id WHERE h.room_id=?";try(PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,room);try(ResultSet r=s.executeQuery()){if(!r.next())throw new HallError(503,"HALL_RESOURCE_ROUTE_UNAVAILABLE","published game resource route unavailable");Object value=document(r.getString(1));if(!(value instanceof Map<?,?> raw))throw new HallError(503,"HALL_RESOURCE_ROUTE_INVALID","published game resource route is invalid");Object bundleValue=raw.get("bundle"),sceneValue=raw.get("scene");String bundle=bundleValue instanceof String bundleText?bundleText.strip():"",scene=sceneValue instanceof String sceneText?sceneText.strip():"";require(bundle,"bundleName");require(scene,"sceneName");return Map.of("bundleName",bundle,"sceneName",scene);}}}
    private Map<String,Object> runtimeIdentity(Connection c,long room)throws Exception{String sql="SELECT room_instance_id,lifecycle_epoch,fencing_token,state_version,lifecycle_state FROM aoo_room_runtime_identity_authority WHERE room_id=?";try(PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,room);try(ResultSet r=s.executeQuery()){if(!r.next())throw new HallError(503,"HALL_RUNTIME_IDENTITY_UNAVAILABLE","room runtime identity is unavailable");String instance=r.getString("room_instance_id"),state=r.getString("lifecycle_state");long epoch=r.getLong("lifecycle_epoch"),fence=r.getLong("fencing_token"),version=r.getLong("state_version");if(instance==null||instance.isBlank()||epoch<=0||fence<=0||version<0||!"ACTIVE".equals(state))throw new HallError(409,"HALL_RUNTIME_IDENTITY_CONFLICT","room runtime identity is not active");return Map.of("roomInstanceId",instance,"lifecycleEpoch",Long.toString(epoch),"fencingToken",Long.toString(fence),"stateVersion",Long.toString(version),"lifecycleState",state);}}}
    private Map<String,Object> roomLockMetadata(Connection c,long room)throws Exception{String sql="SELECT game_id,region_code,play_version,release_id,index_generation,source_hash,rule_hash,component_hash,card_codec_version,event_interpreter_version,protocol_version,snapshot_schema_version FROM aoo_room_rule_lock WHERE room_id=?";try(PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,room);try(ResultSet r=s.executeQuery()){if(!r.next())throw new HallError(503,"HALL_ROOM_RULE_LOCK_METADATA_UNAVAILABLE","room rule lock metadata is unavailable");String region=r.getString("region_code"),play=r.getString("play_version"),source=r.getString("source_hash"),rule=r.getString("rule_hash"),component=r.getString("component_hash"),card=r.getString("card_codec_version"),event=r.getString("event_interpreter_version"),protocol=r.getString("protocol_version"),snapshot=r.getString("snapshot_schema_version");long game=r.getLong("game_id"),release=r.getLong("release_id"),generation=r.getLong("index_generation");if(game<=0||release<=0||generation<=0||region==null||region.isBlank()||play==null||play.isBlank())throw new HallError(503,"HALL_ROOM_RULE_LOCK_METADATA_UNAVAILABLE","room rule lock static metadata is incomplete");requireHash(source,"HALL_ROOM_RULE_LOCK_METADATA_UNAVAILABLE");requireHash(rule,"HALL_ROOM_RULE_LOCK_METADATA_UNAVAILABLE");requireHash(component,"HALL_ROOM_RULE_LOCK_METADATA_UNAVAILABLE");requireVersion(card,"HALL_ROOM_RULE_LOCK_METADATA_UNAVAILABLE");requireVersion(event,"HALL_ROOM_RULE_LOCK_METADATA_UNAVAILABLE");requireVersion(protocol,"HALL_ROOM_RULE_LOCK_METADATA_UNAVAILABLE");requireVersion(snapshot,"HALL_ROOM_RULE_LOCK_METADATA_UNAVAILABLE");return losslessIdentityNumbers(attachRuntimeMetadataProjection(Map.ofEntries(Map.entry("gameId",game),Map.entry("regionCode",region),Map.entry("playVersion",play),Map.entry("releaseId",release),Map.entry("indexGeneration",generation),Map.entry("sourceHash",source),Map.entry("ruleContentHash",rule),Map.entry("componentContentHash",component),Map.entry("cardCodecVersion",card),Map.entry("eventInterpreterVersion",event),Map.entry("protocolVersion",protocol),Map.entry("snapshotSchemaVersion",snapshot))));}}}

    private static Map<String,Object> losslessIdentityNumbers(Map<String,Object> metadata){
        Map<String,Object> out=new LinkedHashMap<>(metadata);
        for(String key:List.of("gameId","releaseId","indexGeneration")){
            Object value=out.get(key);
            if(!(value instanceof Number number))throw new IllegalStateException("HALL_ROOM_RULE_LOCK_METADATA_UNAVAILABLE");
            out.put(key,Long.toString(number.longValue()));
        }
        return Map.copyOf(out);
    }
    private static Map<String,Object> leaveAdmission(long room,Room value,Map<String,Object> projection){
        Map<?,?> runtime=(Map<?,?>)projection.get("runtimeIdentity"), metadata=(Map<?,?>)projection.get("roomLockMetadata");
        Map<String,Object> out=new LinkedHashMap<>();
        out.put("roomId",Long.toString(room));out.put("gameCode",value.gameCode());
        for(String key:PUBLIC_LEAVE_FIELDS){
            if(out.containsKey(key))continue;
            out.put(key,runtime.containsKey(key)?runtime.get(key):metadata.get(key));
        }
        if(!value.playVersion().equals(out.get("playVersion"))||!Long.toString(value.gameId()).equals(out.get("gameId")))
            throw HallError.conflict("HALL_ROOM_IDENTITY_MISMATCH","leave metadata does not match the room");
        requirePublicLeaveIdentity(out);
        return Map.copyOf(out);
    }

    private RuntimeMetadataProjection.Entry requireCanonicalProjectionIdentity(
            RuntimeMetadataProjection projection, long gameId) {
        if (gameId <= 0 || gameId > Integer.MAX_VALUE) {
            throw new HallError(503, "HALL_RUNTIME_METADATA_PROJECTION_CONFLICT",
                    "requested game identity is not canonical");
        }
        try {
            return projection.require(Math.toIntExact(gameId));
        } catch (RuntimeException rejected) {
            throw new HallError(503, "HALL_RUNTIME_METADATA_PROJECTION_CONFLICT",
                    "requested game identity is not canonical");
        }
    }

    private RuntimeMetadataProjection.Entry requireCanonicalProjectionRow(
            RuntimeMetadataProjection projection, long gameId, String gameCode, String family,
            String playVersion, String regionCode, long releaseId, long indexGeneration,
            String sourceHash, String snapshotSchemaVersion, String cardCodecVersion,
            String eventInterpreterVersion, String protocolVersion) {
        if (gameId <= 0 || gameId > Integer.MAX_VALUE || gameCode == null || gameCode.isBlank()
                || family == null || family.isBlank() || playVersion == null || playVersion.isBlank()
                || regionCode == null || regionCode.isBlank()) {
            throw new HallError(503, "HALL_RUNTIME_METADATA_PROJECTION_UNAVAILABLE",
                    "published identity is incomplete");
        }
        RuntimeMetadataProjection.Entry row;
        try {
            row = projection.require(Math.toIntExact(gameId), gameCode, family, playVersion, regionCode);
        } catch (RuntimeException rejected) {
            throw new HallError(503, "HALL_RUNTIME_METADATA_PROJECTION_CONFLICT",
                    "published identity is not canonical");
        }
        if (row.releaseId() != releaseId || row.indexGeneration() != indexGeneration
                || !row.sourceHash().equals(sourceHash)
                || !row.snapshotSchemaVersion().equals(snapshotSchemaVersion)
                || !row.cardCodecVersion().equals(cardCodecVersion)
                || !row.eventInterpreterVersion().equals(eventInterpreterVersion)
                || !row.protocolVersion().equals(protocolVersion)) {
            throw new HallError(503, "HALL_RUNTIME_METADATA_PROJECTION_CONFLICT",
                    "published runtime metadata is not canonical");
        }
        return row;
    }

    private Map<String,Object> attachRuntimeMetadataProjection(Map<String,Object> metadata) {
        if (runtimeMetadataProjection == null) return metadata;
        Object gameValue=metadata.get("gameId"), playValue=metadata.get("playVersion");
        if (!(gameValue instanceof Number game) || !(playValue instanceof String play)) {
            throw new HallError(503,"HALL_RUNTIME_METADATA_PROJECTION_UNAVAILABLE","room identity is incomplete");
        }
        RuntimeMetadataProjection.Entry row=runtimeMetadataProjection.require(game.intValue());
        if (!row.playVersion().equals(play) || !row.regionCode().equals(String.valueOf(metadata.get("regionCode")))
                || !(metadata.get("releaseId") instanceof Number release) || row.releaseId()!=release.longValue()
                || !(metadata.get("indexGeneration") instanceof Number generation) || row.indexGeneration()!=generation.longValue()
                || !row.sourceHash().equals(String.valueOf(metadata.get("sourceHash")))) {
            throw new HallError(503,"HALL_RUNTIME_METADATA_PROJECTION_CONFLICT","room metadata is not bound to release projection");
        }
        Map<String,Object> out=new LinkedHashMap<>(metadata);
        out.putAll(row.fields());
        out.putAll(runtimeMetadataProjection.manifestEnvelope());
        return Map.copyOf(out);
    }
    private Map<String,Object> roomConfiguration(Connection c,long room)throws Exception{String sql="SELECT h.game_id,h.classification_region_code,h.play_version,h.release_id,h.index_generation,i.component_chain,i.rule_validator,i.ui_schema,i.bundle_hash,g.family_code FROM aoo_hall_room h JOIN aoo_compiled_room_create_index i ON i.game_id=h.game_id AND i.region_code=h.classification_region_code AND i.play_version=h.play_version AND i.index_generation=h.index_generation AND i.release_id=h.release_id JOIN aoo_game_catalog g ON g.game_id=h.game_id WHERE h.room_id=?";try(PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,room);try(ResultSet r=s.executeQuery()){if(!r.next())throw new HallError(503,"HALL_ROOM_RULE_SNAPSHOT_UNAVAILABLE","pinned room rule snapshot is unavailable");Map<String,Object> out=row(r,"game_id","classification_region_code","play_version","release_id","index_generation","bundle_hash");out.put("components",document(r.getString("component_chain")));out.put("rules",document(r.getString("rule_validator")));out.put("ui",settlementUi(r.getString("family_code"),document(r.getString("ui_schema"))));return Map.copyOf(out);}}}
    private Map<String,Object> roomRules(Connection c,long room)throws Exception{try(PreparedStatement q=c.prepareStatement("SELECT rules_json FROM aoo_hall_room WHERE room_id=?")){q.setLong(1,room);try(ResultSet r=q.executeQuery()){if(!r.next())throw HallError.notFound("room not found");Object value=document(r.getString(1));if(!(value instanceof Map<?,?> raw))return Map.of();Map<String,Object> out=new LinkedHashMap<>();raw.forEach((key,item)->out.put(String.valueOf(key),item));return Map.copyOf(out);}}}
    private String activeRegion(long game,String play){return activeRegion(game,play,null);}
    private String activeRegion(long game,String play,String requested){if(requested!=null&&!requested.isBlank()){String sql="SELECT 1 FROM aoo_compiled_index_active a WHERE a.game_id=? AND a.region_code=? AND a.play_version=? AND EXISTS(SELECT 1 FROM aoo_game_service_route r WHERE r.game_id=a.game_id AND r.play_version=a.play_version AND r.status='ACTIVE')";try(Connection c=dataSource.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,game);s.setString(2,requested);s.setString(3,play);try(ResultSet r=s.executeQuery()){if(!r.next())throw HallError.notFound("active game configuration not found");return requested;}}catch(SQLException e){throw storage(e);}}String sql="SELECT DISTINCT a.region_code FROM aoo_compiled_index_active a WHERE a.game_id=? AND a.play_version=? AND EXISTS(SELECT 1 FROM aoo_game_service_route r WHERE r.game_id=a.game_id AND r.play_version=a.play_version AND r.status='ACTIVE') ORDER BY a.region_code";try(Connection c=dataSource.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,game);s.setString(2,play);try(ResultSet r=s.executeQuery()){List<String> regions=new ArrayList<>();while(r.next())regions.add(r.getString(1));return resolveActiveRegion(regions);}}catch(SQLException e){throw storage(e);}}
    public static String resolveActiveRegion(List<String> regions){if(regions.isEmpty())throw new HallError(503,"HALL_GAME_ROUTE_UNAVAILABLE","published game route unavailable");if(regions.contains("GLOBAL"))return "GLOBAL";if(regions.size()==1)return regions.get(0);throw HallError.conflict("HALL_RULE_REGION_AMBIGUOUS","an explicit classification region is required");}
    private void requireRevisionMatches(long game,String play,Index active,LobbyRoomCreateSaga.RuleRevision requested){
        if(!active.regionCode().equals(requested.classificationRegionCode())
                ||active.releaseId()!=requested.releaseId()
                ||active.generation()!=requested.indexGeneration()
                ||!active.sourceHash().equals(requested.sourceHash()))
            throw HallError.conflict("HALL_RULE_REVISION_STALE","room rule configuration changed; refresh and retry");
    }
    private Index lockIndex(Connection c,long game,String region,String play)throws Exception{
        String sql="SELECT a.region_code,a.release_id,a.index_generation,p.compatibility_floor,g.game_code,"
                +"i.component_chain,i.rule_validator,i.ui_schema,i.bundle_hash index_bundle_hash,"
                +"r.catalog_snapshot,r.catalog_hash,r.ui_snapshot,r.ui_hash,r.rule_hash,r.component_hash,r.bundle_hash release_bundle_hash,r.snapshot_schema_version,"
                +"r.card_codec_version,r.event_interpreter_version,r.protocol_version,r.source_hash "
                +"FROM aoo_compiled_index_active a "
                +"JOIN aoo_compiled_room_create_index i ON i.game_id=a.game_id AND i.region_code=a.region_code "
                +"AND i.play_version=a.play_version AND i.index_generation=a.index_generation AND i.release_id=a.release_id "
                +"JOIN aoo_play_version p ON p.game_id=a.game_id AND p.play_version=a.play_version AND p.status='ACTIVE' "
                +"JOIN aoo_game_catalog g ON g.game_id=a.game_id AND g.status='ACTIVE' "
                +"JOIN aoo_game_release r ON r.release_id=a.release_id AND r.game_id=a.game_id AND r.play_version=a.play_version "
                +"JOIN aoo_game_release_region rr ON rr.release_id=r.release_id AND rr.region_code=a.region_code AND rr.status='ACTIVE' "
                +"WHERE a.game_id=? AND a.region_code=? AND a.play_version=? "
                +"AND i.lifecycle_state='ACTIVE' AND r.status='ACTIVE' FOR UPDATE";
        try(PreparedStatement s=c.prepareStatement(sql)){
            s.setLong(1,game);s.setString(2,region);s.setString(3,play);
            try(ResultSet r=s.executeQuery()){
                if(!r.next())throw HallError.notFound("active game index not found");
                String catalogSnapshot=r.getString("catalog_snapshot"),ruleValidator=r.getString("rule_validator"),componentChain=r.getString("component_chain");
                Index index=new Index(r.getString("region_code"),r.getLong("release_id"),r.getLong("index_generation"),
                        r.getString("compatibility_floor"),r.getString("game_code"),componentChain,
                        ruleValidator,r.getString("ui_schema"),r.getString("index_bundle_hash"),
                        catalogSnapshot,r.getString("catalog_hash"),sha256(catalogSnapshot),
                        r.getString("ui_snapshot"),r.getString("ui_hash"),r.getString("release_bundle_hash"),r.getString("rule_hash"),r.getString("component_hash"),
                        sha256(ruleValidator),sha256(componentChain),
                        r.getString("snapshot_schema_version"),r.getString("card_codec_version"),
                        r.getString("event_interpreter_version"),r.getString("protocol_version"),r.getString("source_hash"));
                requireCanonicalIndex(game,play,index);
                return index;
            }
        }
    }
    private void requireCanonicalIndex(long game,String play,Index index){
        if(index.releaseId()<=0||index.generation()<=0||index.regionCode()==null||index.regionCode().isBlank()
                ||index.gameCode()==null||index.gameCode().isBlank()||index.componentChain()==null||index.componentChain().isBlank()
                ||index.ruleValidator()==null||index.ruleValidator().isBlank()||index.uiSchema()==null||index.uiSchema().isBlank())
            throw new HallError(503,"HALL_ROOM_RULE_SNAPSHOT_UNAVAILABLE","canonical room rule publication is incomplete");
        requireHash(index.catalogHash(),"HALL_ROOM_RULE_SNAPSHOT_UNAVAILABLE");
        requireHash(index.catalogContentHash(),"HALL_ROOM_RULE_SNAPSHOT_UNAVAILABLE");
        requireHash(index.uiHash(),"HALL_ROOM_RULE_SNAPSHOT_UNAVAILABLE");
        requireHash(index.indexBundleHash(),"HALL_ROOM_RULE_SNAPSHOT_UNAVAILABLE");
        requireHash(index.releaseBundleHash(),"HALL_ROOM_RULE_SNAPSHOT_UNAVAILABLE");
        requireHash(index.ruleHash(),"HALL_ROOM_RULE_SNAPSHOT_UNAVAILABLE");
        requireHash(index.componentHash(),"HALL_ROOM_RULE_SNAPSHOT_UNAVAILABLE");
        requireHash(index.ruleContentHash(),"HALL_ROOM_RULE_SNAPSHOT_UNAVAILABLE");
        requireHash(index.componentContentHash(),"HALL_ROOM_RULE_SNAPSHOT_UNAVAILABLE");
        if(!index.catalogHash().equals(index.catalogContentHash()))
            throw new HallError(503,"HALL_ROOM_RULE_SNAPSHOT_UNAVAILABLE","release catalog snapshot hash is inconsistent");
        if(!index.ruleHash().equals(index.ruleContentHash()))
            throw new HallError(503,"HALL_ROOM_RULE_SNAPSHOT_UNAVAILABLE","release rule hash is inconsistent with the compiled validator");
        if(!index.componentHash().equals(index.componentContentHash()))
            throw new HallError(503,"HALL_ROOM_RULE_SNAPSHOT_UNAVAILABLE","release component hash is inconsistent with the compiled chain");
        if(!index.releaseBundleHash().equals(index.indexBundleHash()))
            throw new HallError(503,"HALL_ROOM_RULE_SNAPSHOT_UNAVAILABLE","compiled index and release bundle hashes disagree");
        requireHash(index.sourceHash(),"HALL_RULE_SCHEMA_UNAVAILABLE");
        Object publishedUi=document(index.uiSnapshot());
        Object ui=document(index.uiSchema());
        if(!index.uiHash().equals(sha256(canonicalUiJson(publishedUi))))
            throw new HallError(503,"HALL_RULE_SCHEMA_UNAVAILABLE","release UI content hash is inconsistent with its published snapshot");
        if(!Objects.equals(publishedUi,ui))
            throw new HallError(503,"HALL_RULE_SCHEMA_UNAVAILABLE","compiled and release UI snapshots disagree");
        requireVersion(index.snapshotSchemaVersion(),"HALL_ROOM_RULE_SNAPSHOT_UNAVAILABLE");
        requireVersion(index.cardCodecVersion(),"HALL_ROOM_RULE_SNAPSHOT_UNAVAILABLE");
        requireVersion(index.eventInterpreterVersion(),"HALL_ROOM_RULE_SNAPSHOT_UNAVAILABLE");
        requireVersion(index.protocolVersion(),"HALL_ROOM_RULE_SNAPSHOT_UNAVAILABLE");
        if(!(ui instanceof Map<?,?> uiMap)||!index.sourceHash().equals(String.valueOf(uiMap.get("roomRuleSourceHash"))))
            throw new HallError(503,"HALL_RULE_SCHEMA_UNAVAILABLE","published room rule source hash is inconsistent");
        Object catalog=document(index.catalogSnapshot());
        if(!(catalog instanceof Map<?,?> catalogMap)
                ||!sameIdentity(catalogMap.get("gameId"),game)
                ||!index.gameCode().equals(String.valueOf(catalogMap.get("gameCode"))))
            throw new HallError(503,"HALL_ROOM_RULE_SNAPSHOT_UNAVAILABLE","published catalog identity is inconsistent");
        if(!(publishedUi instanceof Map<?,?> publishedUiMap)
                ||!index.sourceHash().equals(String.valueOf(publishedUiMap.get("roomRuleSourceHash"))))
            throw new HallError(503,"HALL_RULE_SCHEMA_UNAVAILABLE","release UI snapshot is inconsistent");
        Object validator=document(index.ruleValidator());
        if(!(validator instanceof Map<?,?> validatorMap))
            throw new HallError(503,"HALL_RULE_SCHEMA_UNAVAILABLE","published room rule validator is invalid");
        if(!sameIdentity(validatorMap.get("gameId"),game)
                ||!index.gameCode().equals(String.valueOf(validatorMap.get("gameCode")))
                ||!play.equals(String.valueOf(validatorMap.get("playVersion")))
                ||!index.sourceHash().equals(String.valueOf(validatorMap.get("sourceHash"))))
            throw new HallError(503,"HALL_RULE_SCHEMA_UNAVAILABLE","published room rule identity is inconsistent");
    }
    private String canonicalUiJson(Object value){
        if(value instanceof Map<?,?> map){
            List<String> keys=map.keySet().stream().map(String::valueOf).sorted().toList();
            List<String> fields=new ArrayList<>();
            for(String key:keys)fields.add(canonicalUiJson(key)+":"+canonicalUiJson(map.get(key)));
            return "{"+String.join(",",fields)+"}";
        }
        if(value instanceof List<?> list){
            List<String> items=new ArrayList<>();
            for(Object item:list)items.add(canonicalUiJson(item));
            return "["+String.join(",",items)+"]";
        }
        try{return json.writeValueAsString(value);}
        catch(Exception error){throw new IllegalStateException("invalid UI snapshot scalar",error);}
    }
    private static boolean sameIdentity(Object raw,long expected){try{return Long.parseLong(String.valueOf(raw))==expected;}catch(Exception ignored){return false;}}
    private static String sha256(String value){
        if(value==null)throw new HallError(503,"HALL_ROOM_RULE_SNAPSHOT_UNAVAILABLE","canonical content is missing");
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(Exception error){throw new IllegalStateException("SHA-256 unavailable",error);}
    }
    private static void requireHash(String value,String code){if(value==null||!value.matches(SHA256))throw new HallError(503,code,"canonical SHA-256 metadata is missing or invalid");}
    private static void requireVersion(String value,String code){if(value==null||!value.matches(VERSION))throw new HallError(503,code,"canonical runtime version metadata is missing or invalid");}

    private boolean existingRoomIsIdempotent(Connection c,LobbyRoomCreateSaga.Command command,Index index,Map<String,Object> validatedRules)throws Exception{
        long room=command.roomId();
        try(PreparedStatement s=c.prepareStatement("SELECT game_id,classification_region_code,play_version,release_id,index_generation,owner_account_id,scope_type,club_id,club_template_code,state,rules_json FROM aoo_hall_room WHERE room_id=? FOR UPDATE")){
            s.setLong(1,room);
            try(ResultSet r=s.executeQuery()){
                if(!r.next()){
                    try(PreparedStatement orphan=c.prepareStatement("SELECT room_id FROM aoo_room_rule_lock WHERE room_id=? FOR UPDATE")){
                        orphan.setLong(1,room);try(ResultSet found=orphan.executeQuery()){if(found.next())throw new HallError(409,"HALL_ROOM_RULE_LOCK_ORPHANED","room rule lock exists without its Hall room");}
                    }
                    return false;
                }
                long clubId=r.getLong("club_id");boolean clubNull=r.wasNull();
                boolean identity=r.getLong("game_id")==command.gameId()
                        &&index.regionCode().equals(r.getString("classification_region_code"))
                        &&command.playVersion().equals(r.getString("play_version"))
                        &&r.getLong("release_id")==index.releaseId()&&r.getLong("index_generation")==index.generation()
                        &&r.getLong("owner_account_id")==command.accountId()
                        &&command.scope().type().equals(r.getString("scope_type"))
                        &&(command.scope().club()?(!clubNull&&clubId==command.scope().clubId()):clubNull)
                        &&Objects.equals(command.scope().club()?command.scope().templateCode():null,r.getString("club_template_code"));
                if(!identity||!"CREATING".equals(r.getString("state")))throw HallError.conflict("HALL_ROOM_IDENTITY_CONFLICT","room identity is already bound to another creation");
                if(!sameJson(r.getString("rules_json"),validatedRules))throw HallError.conflict("HALL_ROOM_IDENTITY_CONFLICT","room rules differ from the existing creation");
                try(PreparedStatement member=c.prepareStatement("SELECT account_id,seat_no,status FROM aoo_hall_room_member WHERE room_id=? ORDER BY seat_no,account_id LIMIT 1 FOR UPDATE")){
                    member.setLong(1,room);
                    try(ResultSet first=member.executeQuery()){
                        if(!first.next()||first.getLong("account_id")!=command.accountId()||first.getInt("seat_no")!=0||!"JOINED".equals(first.getString("status")))
                            throw new HallError(503,"HALL_ROOM_MEMBER_MISSING","existing room is missing its joined owner membership");
                    }
                }
                assertRoomRuleLockMatches(c,room,command.gameId(),command.playVersion(),index,validatedRules);
                return true;
            }
        }
    }
    private void assertRoomRuleLockMatches(Connection c,long room,long game,String play,Index index,Map<String,Object> rules)throws Exception{
        String sql="SELECT game_id,region_code,play_version,index_generation,release_id,component_chain,immutable_rules,bundle_hash,rule_hash,component_hash,card_codec_version,event_interpreter_version,protocol_version,snapshot_schema_version,source_hash FROM aoo_room_rule_lock WHERE room_id=? FOR UPDATE";
        try(PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,room);try(ResultSet r=s.executeQuery()){
            if(!r.next())throw new HallError(503,"HALL_ROOM_RULE_LOCK_MISSING","existing Hall room has no canonical rule lock");
            boolean same=r.getLong("game_id")==game&&index.regionCode().equals(r.getString("region_code"))&&play.equals(r.getString("play_version"))
                    &&index.generation()==r.getLong("index_generation")&&index.releaseId()==r.getLong("release_id")
                    &&Objects.equals(index.regionCode(),r.getString("region_code"))&&sameJson(r.getString("component_chain"),index.componentChain())
                    &&sameJson(r.getString("immutable_rules"),rules)&&Objects.equals(index.releaseBundleHash(),r.getString("bundle_hash"))
                    &&Objects.equals(index.ruleHash(),r.getString("rule_hash"))&&Objects.equals(index.componentHash(),r.getString("component_hash"))
                    &&Objects.equals(index.cardCodecVersion(),r.getString("card_codec_version"))
                    &&Objects.equals(index.eventInterpreterVersion(),r.getString("event_interpreter_version"))
                    &&Objects.equals(index.protocolVersion(),r.getString("protocol_version"))
                    &&Objects.equals(index.snapshotSchemaVersion(),r.getString("snapshot_schema_version"))
                    &&Objects.equals(index.sourceHash(),r.getString("source_hash"));
            if(!same)throw HallError.conflict("HALL_ROOM_IDENTITY_CONFLICT","existing room rule lock differs from canonical publication");
        }}
    }
    private boolean sameJson(String left,Object right){try{return Objects.equals(json.readValue(left,Object.class),right instanceof String text?json.readValue(text,Object.class):right);}catch(Exception error){/* exception-policy: malformed canonical JSON is non-equal; callers reject the idempotent replay without writes. */return false;}}
    private void insertRoomRuleLock(Connection c,long room,long game,String play,Index index,Map<String,Object> rules)throws Exception{
        String sql="INSERT INTO aoo_room_rule_lock(room_id,game_id,region_code,play_version,index_generation,release_id,component_chain,immutable_rules,bundle_hash,rule_hash,component_hash,card_codec_version,event_interpreter_version,protocol_version,source_hash,snapshot_schema_version) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try(PreparedStatement s=c.prepareStatement(sql)){int i=1;s.setLong(i++,room);s.setLong(i++,game);s.setString(i++,index.regionCode());s.setString(i++,play);s.setLong(i++,index.generation());s.setLong(i++,index.releaseId());s.setString(i++,index.componentChain());s.setString(i++,json.writeValueAsString(rules));s.setString(i++,index.releaseBundleHash());s.setString(i++,index.ruleHash());s.setString(i++,index.componentHash());s.setString(i++,index.cardCodecVersion());s.setString(i++,index.eventInterpreterVersion());s.setString(i++,index.protocolVersion());s.setString(i++,index.sourceHash());s.setString(i++,index.snapshotSchemaVersion());s.executeUpdate();}
    }
    private String route(Connection c,long game,String play)throws Exception{try(PreparedStatement s=c.prepareStatement("SELECT endpoint FROM aoo_game_service_route WHERE game_id=? AND play_version=? AND status='ACTIVE' ORDER BY priority,route_id LIMIT 1 FOR UPDATE")){s.setLong(1,game);s.setString(2,play);try(ResultSet r=s.executeQuery()){if(!r.next())throw new HallError(503,"HALL_GAME_ROUTE_UNAVAILABLE","no active game service route");return r.getString(1);}}}
public static void authorizeCreateScope(Connection c,LobbyRoomCreateSaga.Command command)throws Exception{if(!command.scope().club())return;try(PreparedStatement s=c.prepareStatement("SELECT member_role FROM aoo_club_member WHERE club_id=? AND player_id=? AND member_status='ACTIVE' FOR UPDATE")){s.setLong(1,command.scope().clubId());s.setLong(2,command.accountId());try(ResultSet r=s.executeQuery()){if(!r.next())throw new HallError(403,"HALL_CLUB_MEMBERSHIP_REQUIRED","active club membership required");String role=r.getString(1);if(command.creationOrigin()==LobbyRoomCreateSaga.CreationOrigin.CLUB_TEMPLATE_ENTRY)return;if(!Set.of("OWNER","MANAGER","ADMIN").contains(role))throw new HallError(403,"HALL_CLUB_MANAGER_REQUIRED","club manager role required to create rooms");}}}
private void authorizeJoinScope(Connection c,Room room,long account)throws Exception{if(room.clubId()<=0)return;try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM aoo_club_member WHERE club_id=? AND player_id=? AND member_status='ACTIVE'")){s.setLong(1,room.clubId());s.setLong(2,account);try(ResultSet r=s.executeQuery()){if(!r.next())throw new HallError(403,"HALL_CLUB_MEMBERSHIP_REQUIRED","active club membership required");}}}
private void projectClubRoom(Connection c,Room room,long roomId,String status)throws Exception{try(PreparedStatement s=c.prepareStatement("INSERT INTO aoo_club_room_projection(club_id,room_id,template_code,room_status,display_name,created_at) VALUES(?,?,?,?,?,CURRENT_TIMESTAMP(3)) ON DUPLICATE KEY UPDATE template_code=VALUES(template_code),room_status=VALUES(room_status),display_name=VALUES(display_name)")){s.setLong(1,room.clubId());s.setString(2,String.valueOf(roomId));s.setString(3,room.templateCode());s.setString(4,status);s.setString(5,room.displayName());s.executeUpdate();}}
private Room lockRoom(Connection c,long id)throws Exception{return room(c,id,true);}private Room room(Connection c,long id,boolean lock)throws Exception{try(PreparedStatement s=c.prepareStatement("SELECT h.game_id,g.game_code,h.classification_region_code,h.play_version,h.state,h.route_endpoint,h.scope_type,h.club_id,h.club_template_code,g.display_name FROM aoo_hall_room h JOIN aoo_game_catalog g ON g.game_id=h.game_id WHERE h.room_id=?"+(lock?" FOR UPDATE":""))){s.setLong(1,id);try(ResultSet r=s.executeQuery()){if(!r.next())throw HallError.notFound("room not found");return room(r);}}}
private static Room room(ResultSet r)throws SQLException{return new Room(r.getLong("game_id"),r.getString("game_code"),r.getString("classification_region_code"),r.getString("play_version"),r.getString("state"),r.getString("route_endpoint"),r.getString("scope_type"),r.getLong("club_id"),r.getString("club_template_code"),r.getString("display_name"));}
    private void member(Connection c,long room,long account)throws Exception{try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM aoo_hall_room_member WHERE room_id=? AND account_id=? AND status='JOINED'")){s.setLong(1,room);s.setLong(2,account);try(ResultSet r=s.executeQuery()){if(!r.next())throw new HallError(403,"HALL_ROOM_ACCESS_DENIED","active room membership required");}}}
    private Map<String,Object> idempotent(Connection c,long account,String key,String operation)throws Exception{try(PreparedStatement s=c.prepareStatement("SELECT operation,response_json FROM aoo_hall_idempotency WHERE account_id=? AND idempotency_key=? FOR UPDATE")){s.setLong(1,account);s.setString(2,key);try(ResultSet r=s.executeQuery()){if(!r.next())return null;if(!operation.equals(r.getString(1)))throw HallError.conflict("HALL_IDEMPOTENCY_CONFLICT","idempotency key was used for another operation");return json.readValue(r.getString(2),new TypeReference<>(){});}}}
    private void saveIdempotent(Connection c,long account,String key,String operation,Map<String,Object> result)throws Exception{try(PreparedStatement s=c.prepareStatement("INSERT INTO aoo_hall_idempotency(account_id,idempotency_key,operation,response_json,created_at) VALUES(?,?,?,?,CURRENT_TIMESTAMP(3))")){s.setLong(1,account);s.setString(2,key);s.setString(3,operation);s.setString(4,json.writeValueAsString(result));s.executeUpdate();}}
    private <T>T transaction(SqlWork<T> work){try(Connection c=dataSource.getConnection()){c.setAutoCommit(false);try{T out=work.run(c);c.commit();return out;}catch(Exception e){c.rollback();if(e instanceof RuntimeException r)throw r;throw new IllegalStateException(e);}}catch(SQLException e){throw storage(e);}}
    private Object document(String value){try{return json.readValue(value,Object.class);}catch(Exception e){throw new IllegalStateException("invalid catalog JSON",e);}}
    public static Map<String,Object> settlementUi(String familyCode,Object raw){
        if(!(raw instanceof Map<?,?> source))throw HallError.bad("HALL_INVALID_UI_SCHEMA","ui schema must be an object");
        Map<String,Object> out=new LinkedHashMap<>();source.forEach((key,value)->out.put(String.valueOf(key),value));
        String big=optionalText(out.get("bigSettleTemplate"));
        if(!big.isEmpty()&&!big.matches("^BigSettleTpl_[0-9]{2,}$"))throw HallError.bad("HALL_INVALID_SETTLEMENT_TEMPLATE","invalid big settlement template");
        String small=optionalText(out.get("smallSettleTemplate"));
        if(!small.isEmpty()){
            String family=canonicalFamily(familyCode),alias=switch(family){case "poker:pao-de-kuai"->"pdk";case "poker:compare-hand"->"zjh";case "poker:betting"->"nn";case "poker:landlord"->"ddz";case "poker:510k"->"510k";case "poker:climbing"->"climbing";case "poker:generic-card-round"->"generic_card_round";case "poker:trick-taking"->"trick_taking";default->"";};
            String pattern=alias.isEmpty()?"^SmallSettleTpl_[0-9]{2,}$":"^SmallSettleTpl_"+alias+"_[0-9]{2,}$";
            if(!small.matches(pattern))throw HallError.bad("HALL_INVALID_SETTLEMENT_TEMPLATE","small settlement template does not belong to play family");
        }
        return Collections.unmodifiableMap(out);
    }
    private static String optionalText(Object value){return value==null?"":String.valueOf(value).trim();}
    private static String canonicalFamily(String value){String family=optionalText(value).toLowerCase(Locale.ROOT).replace('_','-');if(family.contains(":"))return family;int split=family.indexOf('-');return split>0?family.substring(0,split)+":"+family.substring(split+1):family;}
    private static Map<String,Object> row(ResultSet r,String... names)throws SQLException{Map<String,Object> out=new LinkedHashMap<>();for(String n:names)out.put(camel(n),r.getObject(n));return out;}private static String camel(String n){StringBuilder b=new StringBuilder();boolean up=false;for(char c:n.toCharArray()){if(c=='_'){up=true;}else{b.append(up?Character.toUpperCase(c):c);up=false;}}return b.toString();}
    private static boolean compatible(String client,String floor){return floor==null||floor.isBlank()||compareVersions(client,floor)>=0;}private static void requireCompatible(String client,String floor){require(client,"clientVersion");if(!compatible(client,floor))throw new HallError(426,"HALL_CLIENT_UPGRADE_REQUIRED","client version is below compatibility floor "+floor);}
    public static int compareVersions(String a,String b){String[] x=a.split("[.-]"),y=b.split("[.-]");for(int i=0;i<Math.max(x.length,y.length);i++){String l=i<x.length?x[i]:"0",r=i<y.length?y[i]:"0";int c;if(l.matches("\\d+")&&r.matches("\\d+"))c=new java.math.BigInteger(l).compareTo(new java.math.BigInteger(r));else c=l.compareToIgnoreCase(r);if(c!=0)return c;}return 0;}
    public static int firstVacantSeat(Collection<Integer> occupied){int seat=0;for(int value:new TreeSet<>(occupied)){if(value<seat)continue;if(value>seat)break;seat++;}return seat;}
    private static int optionalInt(Object value,int fallback){try{return Integer.parseInt(String.valueOf(value));}catch(Exception ignored){return fallback;}}
    private static String text(Map<String,Object> m,String name){Object v=m.get(name);String s=v==null?null:String.valueOf(v);require(s,name);return s;}private static long number(Map<String,Object> m,String name){try{long v=Long.parseLong(String.valueOf(m.get(name)));if(v<=0)throw new Exception();return v;}catch(Exception e){throw HallError.bad("HALL_INVALID_REQUEST",name+" must be positive");}}private static void require(String s,String name){if(s==null||s.isBlank())throw HallError.bad("HALL_INVALID_REQUEST",name+" is required");}private static void requireKey(String s){require(s,"Idempotency-Key");if(s.length()>128)throw HallError.bad("HALL_INVALID_REQUEST","Idempotency-Key is too long");}private static IllegalStateException storage(Exception e){return new IllegalStateException("hall persistence failure",e);}
    private record Index(String regionCode,long releaseId,long generation,String compatibilityFloor,String gameCode,String componentChain,String ruleValidator,String uiSchema,String indexBundleHash,String catalogSnapshot,String catalogHash,String catalogContentHash,String uiSnapshot,String uiHash,String releaseBundleHash,String ruleHash,String componentHash,String ruleContentHash,String componentContentHash,String snapshotSchemaVersion,String cardCodecVersion,String eventInterpreterVersion,String protocolVersion,String sourceHash){}private record Room(long gameId,String gameCode,String region,String playVersion,String state,String route,String scopeType,long clubId,String templateCode,String displayName){}private record JoinReservation(int seat,long gameId,String gameCode,String playVersion,String state,String route,boolean alreadyJoined){}@FunctionalInterface private interface SqlWork<T>{T run(Connection c)throws Exception;}
}
