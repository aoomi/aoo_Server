package com.aoo.bcg.bootstrap;

import com.aoo.bcg.billing.*;
import com.aoo.bcg.gamespi.*;
import com.aoo.bcg.gateway.*;
import com.aoo.bcg.common.idempotency.*;
import com.aoo.bcg.common.settlement.*;
import com.aoo.bcg.config.RoomRuntimeIdentityStore;
import javax.sql.DataSource;
import java.lang.reflect.*;
import java.net.URI;
import com.aoo.bcg.lobby.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;
import java.io.StringReader;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

/** Listener-free use of the exact factory consumed by the two production assemblies. */
public final class JdbcCarryParticipationPortTest {
    static final Path ROOT=Path.of("/Users/aoo/Code/Game/BCG/Aoo");
    static final String PLAY="cx-v1.0.0",SOURCE="b".repeat(64);
    static final Clock CLOCK=Clock.fixed(Instant.parse("2026-10-17T00:00:00Z"),ZoneOffset.UTC);
    static final ObjectMapper JSON=new ObjectMapper().findAndRegisterModules();
    static void ddl(Connection c,String name)throws Exception {
        RunScript.execute(c,new StringReader(Files.readString(ROOT.resolve("server/database/migrations/"+name))));
    }
    static void sql(JdbcDataSource d,String text)throws Exception {try(var c=d.getConnection();var s=c.createStatement()){s.execute(text);}}
    static long count(JdbcDataSource d,String table)throws Exception {try(var c=d.getConnection();var q=c.createStatement();var row=q.executeQuery("SELECT COUNT(*) FROM "+table)){assertTrue(row.next());return row.getLong(1);}}
    static long balance(JdbcDataSource d,long actor)throws Exception {try(var c=d.getConnection();var q=c.prepareStatement("SELECT balance FROM aoo_currency_balance WHERE player_id=? AND currency='ROOM_CARD' AND currency_scope_id=0")){q.setLong(1,actor);try(var row=q.executeQuery()){assertTrue(row.next());return row.getLong(1);}}}
    static JdbcDataSource fixture(String templateScope)throws Exception {
        var d=new JdbcDataSource();d.setURL("jdbc:h2:mem:carry_frame_"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=1000");
        try(var c=d.getConnection();var s=c.createStatement()) {
            // The old application resource omits columns consumed by the current SagaStore.
            String schema=Files.readString(ROOT.resolve("server/database/bootstrap/current-schema.sql"));
            var match=java.util.regex.Pattern.compile("CREATE TABLE `aoo_room_create_saga` \\([\\s\\S]*?;").matcher(schema);assertTrue(match.find());
            String block=match.group();RunScript.execute(c,new StringReader(block.substring(0,block.indexOf(" ENGINE="))+";"));
            for(String migration:List.of("V20261017_01__room_card_billing_intent_journal.sql","V20261017_02__room_billing_subject_source_binding.sql","V20261017_03__room_billing_business_outcome_receipt.sql"))ddl(c,migration);
            s.execute("CREATE TABLE aoo_currency_catalog(currency_code VARCHAR PRIMARY KEY,scope_type VARCHAR,status VARCHAR)");
            s.execute("INSERT INTO aoo_currency_catalog VALUES('ROOM_CARD','GLOBAL','ACTIVE')");
            s.execute("CREATE TABLE aoo_currency_balance(player_id BIGINT,currency VARCHAR,currency_scope_id BIGINT,balance BIGINT,version BIGINT,updated_at TIMESTAMP,PRIMARY KEY(player_id,currency,currency_scope_id))");
            for(long actor:new long[]{107,108,117,118})s.execute("INSERT INTO aoo_currency_balance VALUES("+actor+",'ROOM_CARD',0,500,0,CURRENT_TIMESTAMP)");
            s.execute("CREATE TABLE aoo_ledger(ledger_id BIGINT PRIMARY KEY,business_id VARCHAR UNIQUE,player_id BIGINT,currency VARCHAR,currency_scope_id BIGINT,delta BIGINT,balance_after BIGINT,reason_code VARCHAR,entry_status VARCHAR,channel_code VARCHAR,created_at TIMESTAMP)");
            s.execute("CREATE TABLE aoo_room_cost_publication(publication_id BIGINT PRIMARY KEY,environment VARCHAR)");s.execute("INSERT INTO aoo_room_cost_publication VALUES(1,'PRODUCTION')");
            ddl(c,"V20261016_91__room_card_pricing_publication_provenance.sql");
            s.execute("CREATE TABLE aoo_room_cost_active(pointer_name VARCHAR PRIMARY KEY,publication_id BIGINT)");s.execute("INSERT INTO aoo_room_cost_active VALUES('roomcard',1)");
            s.execute("CREATE TABLE aoo_room_cost_policy(publication_id BIGINT,policy_version BIGINT,content_hash VARCHAR,cost_minor BIGINT,currency_code VARCHAR,free_policy_ref VARCHAR,refund_policy_ref VARCHAR,tier_minor BIGINT,round_count BIGINT,game_id BIGINT,play_version VARCHAR,region_code VARCHAR,measure_kind VARCHAR,measure_value INT,player_count INT,payer_mode VARCHAR,billing_kind VARCHAR,billing_context VARCHAR,status VARCHAR,confirmation_status VARCHAR)");
            s.execute("CREATE TABLE aoo_room_billing_reservation(request_id VARCHAR PRIMARY KEY,account_id BIGINT,room_id BIGINT,currency_code VARCHAR,amount BIGINT,state VARCHAR,created_at TIMESTAMP,updated_at TIMESTAMP,billing_operation_key VARCHAR UNIQUE,command_hash VARCHAR,actor_id BIGINT,room_attempt VARCHAR,billing_kind VARCHAR,billing_unit VARCHAR,policy_publication_id BIGINT,policy_snapshot VARCHAR,debit_business_id VARCHAR,refunded_minor BIGINT DEFAULT 0,confirmation_id VARCHAR,confirmation_hash VARCHAR)");
            s.execute("CREATE TABLE aoo_room_billing_refund(refund_id VARCHAR PRIMARY KEY,request_id VARCHAR,command_hash VARCHAR,amount BIGINT,credit_business_id VARCHAR,evidence_json VARCHAR,created_at TIMESTAMP)");
            s.execute("CREATE TABLE aoo_game_release(release_id BIGINT PRIMARY KEY,game_id BIGINT,play_version VARCHAR,source_hash VARCHAR,status VARCHAR)");
            s.execute("CREATE TABLE aoo_compiled_index_active(game_id BIGINT,region_code VARCHAR,play_version VARCHAR,index_generation BIGINT,release_id BIGINT)");
            s.execute("CREATE TABLE aoo_compiled_room_create_index(game_id BIGINT,region_code VARCHAR,play_version VARCHAR,index_generation BIGINT,release_id BIGINT,rule_validator CLOB,lifecycle_state VARCHAR)");
            s.execute("INSERT INTO aoo_game_release VALUES(11,2999,'"+PLAY+"','"+SOURCE+"','ACTIVE')");s.execute("INSERT INTO aoo_compiled_index_active VALUES(2999,'CN-51-01','"+PLAY+"',12,11)");
            String validator=JSON.writeValueAsString(Map.of("roomMeasureKind","ROUND_COUNT","fields",List.of(Map.of("key","roundCount","defaultValue",10),Map.of("key","maxPlayers","defaultValue",4),Map.of("key","payerMode","defaultValue","OWNER"))));
            try(var q=c.prepareStatement("INSERT INTO aoo_compiled_room_create_index VALUES(2999,'CN-51-01',?,12,11,?,'ACTIVE')")){q.setString(1,PLAY);q.setString(2,validator);q.executeUpdate();}
            s.execute("CREATE TABLE aoo_club_member(club_id BIGINT,player_id BIGINT,member_role VARCHAR,member_status VARCHAR)");
            s.execute("INSERT INTO aoo_club_member VALUES(1,117,'MEMBER','ACTIVE'),(1,107,'OWNER','ACTIVE'),(2,108,'OWNER','ACTIVE')");
            s.execute("CREATE TABLE aoo_club_state(club_id BIGINT PRIMARY KEY,state_json CLOB)");
            Map<String,Object> template=Map.of("id","test-template-1","name","synthetic","game","chengdu2999","rules","{}","index",1);
            Map<String,Object> club=Map.of("id",1,"name","synthetic source","status","ACTIVE","members",Map.of("107","OWNER","117","MEMBER"),"templates",List.of(template),"settings",Map.of("templateScope.1",templateScope,"unionId",5,"unionOwnerClubId",2,"unionOwnerId",108));
            Map<String,Object> unionOwner=Map.of("id",2,"name","synthetic owner","status","ACTIVE","members",Map.of("108","OWNER"),"settings",Map.of("unionId",5,"unionOwnerClubId",2,"unionOwnerId",108));
            try(var q=c.prepareStatement("INSERT INTO aoo_club_state VALUES(?,?)")){q.setLong(1,1);q.setString(2,JSON.writeValueAsString(club));q.executeUpdate();q.setLong(1,2);q.setString(2,JSON.writeValueAsString(unionOwner));q.executeUpdate();}
            for(String context:List.of("HALL","CLUB","UNION")) {
                String payer=context.equals("HALL")?"OWNER":context;
                s.execute("INSERT INTO aoo_room_cost_policy VALUES(1,1,'"+"a".repeat(64)+"',100,'ROOM_CARD',NULL,'refund-proof',0,0,2999,'"+PLAY+"','CN-51-01','ROUND_COUNT',10,4,'"+payer+"','FIXED','"+context+"','ACTIVE','CONFIRMED')");
                var identity=new RoomCardPricingImportValidator.IdentityKey(2999,"chengdu2999",PLAY,"CN-51-01");
                var priceKey=new RoomCardPricingImportValidator.PriceKey(identity,"FIXED",context,4,"ROUND_COUNT",10,payer,0);
                c.setAutoCommit(false);
                new JdbcRoomCardPricingProvenanceRepository().save(c,new RoomCardPricingProvenance(1,RoomCardPricingProvenance.policyKey(priceKey),"fixture-v1","a".repeat(64),"c".repeat(64),new RoomCardPricingImportValidator.SourceBinding(RoomCardPricingImportValidator.SourceClass.CURRENT_PUBLISHED,11,12,SOURCE),null,null,null,false));
                c.commit();c.setAutoCommit(true);
            }
        }
        for(String t:List.of("aoo_ledger","aoo_room_billing_reservation","aoo_room_billing_intent_journal","aoo_room_billing_source_binding","aoo_room_billing_business_receipt"))assertEquals(0,count(d,t));
        return d;
    }
    static LobbyRoomCreateSaga.Command command(JdbcDataSource d,long authenticated,boolean club,boolean trusted,String payer)throws Exception {
        Map<String,Object> body=new LinkedHashMap<>();body.put("roomId",9001);body.put("gameId",2999);body.put("playVersion",PLAY);body.put("clientVersion","3.8.8");body.put("stateVersion",1);
        body.put("ruleRevision",Map.of("classificationRegionCode","CN-51-01","releaseId",11L,"indexGeneration",12L,"sourceHash",SOURCE));
        body.put("rules",Map.of("roundCount",10,"maxPlayers",4,"payerMode",payer));body.put("scope",club?Map.of("type","CLUB","clubId",1,"templateCode","test-template-1"):Map.of("type","PERSONAL"));
        body.put("ownerId",9999);body.put("payerId",9999); // untrusted fields cannot select the charging identity
        var parsed=LobbyRoomCreateSaga.Command.parse(authenticated,"entry-authenticated-request-0001",body,trusted?LobbyRoomCreateSaga.CreationOrigin.CLUB_TEMPLATE_ENTRY:LobbyRoomCreateSaga.CreationOrigin.PUBLIC);
        LobbyMembershipAuthorityPort noNetwork=new LobbyMembershipAuthorityPort(){public Map<String,Object>join(long r,long a,int s,String p,String q,String t,Map<String,Object>x){throw new AssertionError("no listener");}public Map<String,Object>leave(long r,long a,String p,String q,String t,Map<String,Object>x){throw new AssertionError("no listener");}};
        var normalized=new JdbcLobbyRepository(d,JSON,CLOCK,noNetwork).normalizeCreateCommand(parsed);
        new LobbyRoomCreateSagaStore(d,JSON).begin(normalized);return normalized;
    }
    static JdbcRoomBillingLifecyclePort factory(JdbcDataSource d){return BootstrapAPP.roomBillingLifecycle(d,new AtomicLong(1000)::incrementAndGet,CLOCK);}

    @org.junit.jupiter.api.BeforeAll
    static void verifyWholeFixtureSqlClosure() throws Exception {
        try (var setup=new Setup("none",false); var c=setup.db.getConnection(); var q=c.createStatement()) {
            String[] queries={
                "SELECT authority_endpoint,updated_at,last_request_id,trace_id,lease_expires_at,first_round_started_at,last_business_activity_at FROM aoo_room_authority_route WHERE 1=0",
                "SELECT room_instance_id,lifecycle_epoch,fencing_token,state_version,lifecycle_state FROM aoo_room_runtime_identity_authority WHERE 1=0",
                "SELECT game_id,play_version,classification_region_code,release_id,index_generation,owner_account_id,rules_json,state FROM aoo_hall_room WHERE 1=0",
                "SELECT index_generation,release_id,rule_hash,component_hash,card_codec_version,event_interpreter_version,protocol_version,source_hash,snapshot_schema_version FROM aoo_room_rule_lock WHERE 1=0",
                "SELECT state_payload,state_version,fencing_token,last_event_sequence FROM aoo_room_snapshot WHERE 1=0",
                "SELECT request_id,user_id,operation,room_id,round_no,client_request_id,status,response_code,response_version,response_schema_version,response_payload,expires_at FROM aoo_business_idempotency WHERE 1=0",
                "SELECT attempt_json,original_json,money_mode FROM aoo_room_carry_attempt WHERE 1=0",
                "SELECT batch_json,status,proof_json FROM aoo_room_carry_batch WHERE 1=0"
            };
            for(String sql:queries)try(var rows=q.executeQuery(sql)){assertFalse(rows.next());}
        }
    }

    static final String ATOMIC_FIXTURE = """

CREATE TABLE `aoo_room_runtime_identity_authority` (
    room_id BIGINT  NOT NULL,
    room_instance_id VARCHAR(128) NOT NULL,
    lifecycle_epoch BIGINT  NOT NULL,
    fencing_token BIGINT  NOT NULL,
    state_version BIGINT  NOT NULL,
    lifecycle_state VARCHAR(16) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (room_id),
    UNIQUE KEY uk_room_runtime_identity_generation (room_instance_id, lifecycle_epoch, fencing_token),
    CONSTRAINT chk_room_runtime_identity_room CHECK (room_id > 0),
    CONSTRAINT chk_room_runtime_identity_instance CHECK (CHAR_LENGTH(TRIM(room_instance_id)) > 0),
    CONSTRAINT chk_room_runtime_identity_epoch CHECK (lifecycle_epoch > 0),
    CONSTRAINT chk_room_runtime_identity_fence CHECK (fencing_token > 0),
    CONSTRAINT chk_room_runtime_identity_state CHECK (lifecycle_state IN ('ACTIVE', 'DESTROYED'))
);
CREATE TABLE `aoo_room_authority_route` (
    room_id BIGINT  NOT NULL,
    game_id BIGINT  NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    node_id VARCHAR(128) NOT NULL,
    authority_endpoint VARCHAR(512) NOT NULL,
    fencing_token BIGINT  NOT NULL,
    lifecycle_state VARCHAR(24) NOT NULL,
    last_request_id VARCHAR(128) NOT NULL,
    trace_id VARCHAR(64) NOT NULL,
    lease_expires_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    first_round_started_at TIMESTAMP(3) NULL,
    last_business_activity_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY(room_id),
    UNIQUE KEY uk_authority_create_request(last_request_id),
    CONSTRAINT chk_authority_fencing CHECK(fencing_token > 0),
    CONSTRAINT chk_authority_state CHECK(lifecycle_state IN('CREATING','ACTIVE','RECOVERING','REMOVING','REMOVED','FAILED'))
);
CREATE TABLE `aoo_hall_room` (
    room_id BIGINT  NOT NULL,
    game_id BIGINT  NOT NULL,
    classification_region_code VARCHAR(32) NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    release_id BIGINT  NOT NULL,
    index_generation BIGINT  NOT NULL,
    owner_account_id BIGINT  NOT NULL,
    scope_type VARCHAR(16) NOT NULL DEFAULT 'PERSONAL',
    club_id BIGINT  NULL,
    club_template_code VARCHAR(64) NULL,
    state VARCHAR(16) NOT NULL,
    route_endpoint VARCHAR(512) NOT NULL,
    rules_json CLOB NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY(room_id),
    CONSTRAINT chk_hall_room_scope CHECK (
        (scope_type = 'PERSONAL' AND club_id IS NULL AND club_template_code IS NULL)
        OR (scope_type = 'CLUB' AND club_id IS NOT NULL AND club_id > 0 AND club_template_code IS NOT NULL)
    ),
    CONSTRAINT chk_hall_room_state
        CHECK (state IN ('CREATING', 'OPEN', 'PLAYING', 'FINISHED', 'DISSOLVED'))
);
CREATE TABLE aoo_game_catalog(game_id INT,game_code VARCHAR(100));
CREATE TABLE `aoo_room_rule_lock` (
    room_id BIGINT  NOT NULL,
    game_id BIGINT  NOT NULL,
    region_code VARCHAR(32) NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    index_generation BIGINT  NOT NULL,
    release_id BIGINT  NOT NULL,
    component_chain CLOB NOT NULL,
    immutable_rules CLOB NOT NULL,
    bundle_hash CHAR(64) NOT NULL,
    rule_hash CHAR(64) NOT NULL,
    component_hash CHAR(64) NOT NULL,
    card_codec_version VARCHAR(64) NOT NULL,
    event_interpreter_version VARCHAR(64) NOT NULL,
    protocol_version VARCHAR(64) NOT NULL,
    source_hash CHAR(64) NULL,
    snapshot_schema_version VARCHAR(64) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (room_id),
    CONSTRAINT chk_room_rule_lock_hashes CHECK (
        bundle_hash REGEXP '^[0-9a-f]{64}$'
        AND rule_hash REGEXP '^[0-9a-f]{64}$'
        AND component_hash REGEXP '^[0-9a-f]{64}$'
    ),
    CONSTRAINT chk_room_rule_lock_runtime_metadata CHECK (
        (source_hash IS NULL AND snapshot_schema_version IS NULL)
        OR (
            source_hash REGEXP '^[0-9a-f]{64}$'
            AND snapshot_schema_version REGEXP '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
        )
    )
);
CREATE TABLE `aoo_room_snapshot` (
    room_id BIGINT  NOT NULL,
    game_id INT NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    release_id BIGINT  NULL,
    component_version VARCHAR(64) NOT NULL,
    rule_content_hash CHAR(64) NULL,
    component_content_hash CHAR(64) NULL,
    card_codec_version VARCHAR(64) NULL,
    event_interpreter_version VARCHAR(64) NULL,
    protocol_version VARCHAR(64) NULL,
    fencing_token BIGINT NOT NULL,
    state_version BIGINT NOT NULL DEFAULT 0,
    last_event_sequence BIGINT NOT NULL,
    captured_at DATETIME(3) NOT NULL,
    state_payload CLOB NOT NULL,
    PRIMARY KEY (room_id),
    CONSTRAINT chk_snapshot_identity CHECK (room_id > 0 AND fencing_token > 0 AND state_version >= 0 AND last_event_sequence = state_version)
);
CREATE TABLE aoo_business_idempotency(request_id VARCHAR(1024) PRIMARY KEY,user_id VARCHAR(100),operation VARCHAR(100),room_id BIGINT,round_no INT,client_request_id VARCHAR(100),status VARCHAR(100),response_code INT,response_version VARCHAR(100),response_schema_version INT,response_payload CLOB,expires_at TIMESTAMP,request_hash VARCHAR(64),created_at TIMESTAMP);
CREATE ALIAS SHA2 FOR "com.aoo.bcg.bootstrap.JdbcCarryParticipationPortTest.sha2";

""";
    public static String sha2(String value,int bits)throws Exception {return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
    static RoomRuntimeIdentity identity(long version){return new RoomRuntimeIdentity("carry-synthetic-instance",7,11,version);}
    static GameProvider provider(){return new com.aoo.bcg.poker.cx.CxGameProvider(new GameDescriptor(2999,"chengdu2999","cx-synthetic-fixture",GameCategory.POKER,"poker:cx",RegionScope.CITY,"CN","CN-51-01",PLAY));}
    static final class Setup implements AutoCloseable {
        final JdbcDataSource db;final DataSource persistence;final GameProvider provider;final GameRegistry games;
        final RuntimeGameRoomRegistry rooms=new RuntimeGameRoomRegistry();final JdbcGatewayGameCommandCommitter committer;
        final JdbcGatewayRoomAuthority authority;final java.util.concurrent.atomic.AtomicBoolean armed=new java.util.concurrent.atomic.AtomicBoolean();
        final java.util.concurrent.atomic.AtomicInteger faults=new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicInteger restores=new java.util.concurrent.atomic.AtomicInteger();
        CommittedCommandBinding seatedBinding;
        Setup(String failure,boolean restoreFailure)throws Exception {
            db=fixture("CLUB");var command=command(db,117,false,false,"OWNER");factory(db).reserve(command);
            try(var c=db.getConnection()){RunScript.execute(c,new StringReader(ATOMIC_FIXTURE));ddl(c,"V20261017_06__room_card_carry_effect_receipt.sql");}
            sql(db,"CREATE TABLE perspective_replay_event(room_id BIGINT,set_id INT,event_sequence BIGINT,visibility VARCHAR,owner_player_id BIGINT,message_id VARCHAR,schema_version INT,play_version VARCHAR,release_id BIGINT,card_codec_version VARCHAR,event_interpreter_version VARCHAR,protocol_version VARCHAR,payload BLOB,content_hash VARCHAR DEFAULT '"+"a".repeat(64)+"',PRIMARY KEY(room_id,set_id,event_sequence,visibility,owner_player_id))");
            sql(db,"CREATE TABLE replay_participant(room_id BIGINT,set_id INT,player_id BIGINT,seat_id INT,granted_at TIMESTAMP,PRIMARY KEY(room_id,set_id,player_id))");
            sql(db,"CREATE TABLE replay_set_manifest(room_id BIGINT,set_id INT,play_version VARCHAR,schema_version INT,event_count BIGINT,content_hash VARCHAR,closed_at TIMESTAMP,archive_after TIMESTAMP,delete_after TIMESTAMP,legal_hold BOOLEAN,PRIMARY KEY(room_id,set_id))");
            sql(db,"INSERT INTO aoo_game_catalog VALUES(2999,'chengdu2999')");
            sql(db,"INSERT INTO aoo_hall_room(room_id,game_id,play_version,classification_region_code,release_id,index_generation,owner_account_id,rules_json,state,route_endpoint,created_at,updated_at) VALUES(9001,2999,'"+PLAY+"','CN-51-01',11,12,117,'{}','CREATING','unit-test://no-network',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))");
            sql(db,"INSERT INTO aoo_room_authority_route(room_id,game_id,play_version,node_id,authority_endpoint,last_request_id,trace_id,lifecycle_state,lease_expires_at,fencing_token) VALUES(9001,2999,'"+PLAY+"','synthetic-node','unit-test://no-network','synthetic-route-create','synthetic-route-trace','ACTIVE',DATEADD('HOUR',24,CURRENT_TIMESTAMP),11)");
            sql(db,"INSERT INTO aoo_room_rule_lock(room_id,game_id,region_code,play_version,index_generation,release_id,rule_hash,component_hash,card_codec_version,event_interpreter_version,protocol_version,snapshot_schema_version,source_hash,immutable_rules,component_chain,bundle_hash) VALUES(9001,2999,'CN-51-01','"+PLAY+"',12,11,'"+"a".repeat(64)+"','"+"b".repeat(64)+"','codec-v1','events-v1','protocol-v1','snapshot-v1','"+SOURCE+"','{}','[]','"+"c".repeat(64)+"')");
            new RoomRuntimeIdentityStore(db).create(9001,identity(0));
            GameProvider original=provider();
            provider=(GameProvider)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{GameProvider.class},(p,m,a)->{
                if(m.getName().equals("restoreAuthoritativeSession")){if(armed.get())restores.incrementAndGet();if(restoreFailure)return Optional.empty();}
                try{return m.invoke(original,a);}catch(InvocationTargetException ex){throw ex.getCause();}
            });
            games=GameRegistryFactory.production();games.register(provider);
            persistence=(DataSource)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{DataSource.class},(p,m,a)->{
                try{Object out=m.invoke(db,a);if(m.getName().equals("getConnection")){Connection actual=(Connection)out;return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(x,cm,ca)->{
                    if(armed.get()&&cm.getName().equals("commit")&&!failure.equals("none")&&faults.compareAndSet(0,1)){if(failure.equals("ackLost"))actual.commit();throw new SQLException("synthetic commit boundary");}
                    try{return cm.invoke(actual,ca);}catch(InvocationTargetException ex){throw ex.getCause();}
                });}return out;}catch(InvocationTargetException ex){throw ex.getCause();}
            });
            var settle=new SettlementExecutor(new SettlementRepository(){public SettlementResult save(String key,SettlementResult value){throw new AssertionError("no settlement");}public Optional<SettlementResult>find(String key){return Optional.empty();}});
            committer=new JdbcGatewayGameCommandCommitter(persistence,JSON,CLOCK,Duration.ofHours(24),new DurableGameSettlementService(games,settle));
            committer.carryPort().configureCharges(new AtomicLong(5000)::incrementAndGet,CLOCK);
            var session=provider.createAuthoritativeSession(new RoomCreationContext(9001,117,Map.of("startPlayers",4,"roundCount",10))).orElseThrow();
            var metadata=new RoomSnapshotMetadataSource(11L,"a".repeat(64),"b".repeat(64),"codec-v1","events-v1","protocol-v1","snapshot-v1",12L,"CN-51-01",SOURCE);
            var room=new GameRoomHandle(9001,2999,PLAY,session,null,metadata,identity(0));rooms.bindRuntime(room);
            committer.saveCreateSnapshot(room,11,command.requestId(),117);
            authority=new JdbcGatewayRoomAuthority(persistence,games,rooms,CLOCK,"synthetic-node","unit-test://no-network",JSON,committer,
                    new GatewayRoomBroadcastHub((roomId,actor)->{throw new AssertionError("no external broadcast");},JSON,CLOCK),
                    new HallRoomLifecycleClient(URI.create("unit-test://no-network"),"synthetic-only-not-live-token-xxxxxxxx",JSON));authority.close();
        }
        GameCommandRequest request(String msg,String id,long actor,int seat,Map<String,Object> payload){var room=rooms.requireRuntime(9001);return new GameCommandRequest(msg,id,room.requireAuthoritativeSession().stateVersion()+1,9001,0,PLAY,Long.toString(actor),seat,payload);}
        GameCommandResult directSit(String requestId,long actor)throws Exception {
            var room=rooms.requireRuntime(9001);var session=room.requireAuthoritativeSession();var request=request("poker.chengdu2999.sit_req",requestId,actor,-1,Map.of("carryScore",100L));
            ((CarryBillingEffectSource)session).proposeCarry(request);
            new JdbcIdempotencyStore<GameCommandResult>(persistence,JSON,GameCommandResult.class,CLOCK).acquire(new IdempotencyKey(request.authenticatedUserId(),request.msgId(),9001,0,requestId),Duration.ofHours(24));
            var checkpoint=committer.carryPort().begin(room,committer.latest(9001).orElseThrow());
            var handled=session.execute(request);var next=new RoomRuntimeIdentity(room.runtimeIdentity().roomInstanceId(),7,11,session.stateVersion());
            var advanced=new GameRoomHandle(9001,2999,PLAY,session,null,room.snapshotMetadataSource(),next);
            try{var out=committer.commitResult(advanced,request,handled.withAuthorityMetadata(session.stateVersion(),request.sequence()),room.runtimeIdentity());rooms.updateRuntime(advanced);committer.carryPort().finish(checkpoint,true);
                seatedBinding=out.committedBinding();assertNotNull(seatedBinding);
                seatedBinding.requireScope(Long.toString(actor),request.msgId(),9001,0,requestId);
                assertEquals(next.roomInstanceId(),seatedBinding.roomInstanceId());assertEquals(next.lifecycleEpoch(),seatedBinding.lifecycleEpoch());assertEquals(next.fencingToken(),seatedBinding.fencingToken());assertEquals(next.stateVersion(),seatedBinding.stateVersion());
                requireStoredBinding(request,seatedBinding);actualSeat(actor);return out;}
            catch(RuntimeException ex){committer.carryPort().finish(checkpoint,false);throw ex;}
        }
        void requireStoredBinding(GameCommandRequest request,CommittedCommandBinding binding)throws Exception {
            var key=new IdempotencyKey(request.authenticatedUserId(),request.msgId(),request.roomId(),request.roundNo(),request.requestId());
            try(var c=db.getConnection();var q=c.prepareStatement("SELECT response_schema_version,response_payload FROM aoo_business_idempotency WHERE request_id=? AND status='COMPLETED'")){
                q.setString(1,key.storageKey());try(var row=q.executeQuery()){
                    assertTrue(row.next(),"stored command receipt exists");assertEquals(2,row.getInt(1),"stored schema");
                    var document=JSON.readTree(row.getString(2));var stored=document.get("committedBinding");assertTrue(stored!=null&&stored.isObject(),"stored hasBinding");
                    @SuppressWarnings("unchecked") Map<String,Object> expectedBinding=JSON.convertValue(binding,Map.class);
                    @SuppressWarnings("unchecked") Map<String,Object> actualBinding=JSON.convertValue(stored,Map.class);
                    assertEquals(stateDigest(expectedBinding),stateDigest(actualBinding),"stored typed binding digest");
                }
            }
        }
        int actualSeat(long actor){
            var view=rooms.requireRuntime(9001).requireAuthoritativeSession().viewFor(actor);
            assertEquals("SEATED",view.get("viewerRole"));assertTrue(view.get("viewerSeat") instanceof Number);
            int seat=((Number)view.get("viewerSeat")).intValue();assertTrue(seat>=0);
            assertTrue(view.get("players") instanceof Map);var players=(Map<?,?>)view.get("players");Object occupant=players.get(seat);if(occupant==null)occupant=players.get(Integer.toString(seat));
            assertTrue(occupant instanceof Number);assertEquals(actor,((Number)occupant).longValue());return seat;
        }
        RoomOrchestrator.ExecuteCommand stand(){var room=rooms.requireRuntime(9001);assertNotNull(seatedBinding);var id=room.runtimeIdentity();
            assertEquals(seatedBinding.roomInstanceId(),id.roomInstanceId());assertEquals(seatedBinding.lifecycleEpoch(),id.lifecycleEpoch());assertEquals(seatedBinding.fencingToken(),id.fencingToken());assertEquals(seatedBinding.stateVersion(),id.stateVersion());
            var request=request("poker.chengdu2999.stand_req","carry-authenticated-stand-0001",117,actualSeat(117),Map.of());
            return new RoomOrchestrator.ExecuteCommand(new RoomOrchestrator.RoomIdentity(9001,id.roomInstanceId(),2999,"chengdu2999",PLAY,7,11,id.stateVersion()),"synthetic-session",request);}
        void requireInjected(RuntimeException failure){
            assertFalse(failure instanceof SecurityException);boolean matched=false;Throwable cause=failure;
            for(int depth=0;cause!=null&&depth<16;depth++,cause=cause.getCause()){
                assertFalse(cause instanceof SecurityException);
                if(cause instanceof SQLException&&"synthetic commit boundary".equals(cause.getMessage()))matched=true;
            }
            assertTrue(matched,"expected exact injected commit cause");assertEquals(1,faults.get());assertEquals(1,restores.get());
        }
        @Override public void close()throws Exception{authority.close();sql(db,"SHUTDOWN");}
    }
    @Test void realNativeRequestAndActualCommitterStoreFinalAckBytesWithoutNewCarryMoney()throws Exception {
        try(var s=new Setup("none",false)){
            long before=count(s.db,"aoo_ledger");s.directSit("carry-original-request-0001",117);
            assertEquals(before,count(s.db,"aoo_ledger"));assertEquals(2,count(s.db,"aoo_room_carry_effect"));
            try(var c=s.db.getConnection();var q=c.createStatement();var rows=q.executeQuery("SELECT status,proof_json FROM aoo_room_carry_batch")){assertTrue(rows.next());assertEquals("FINALIZED",rows.getString(1));assertEquals("COMMAND",JSON.readTree(rows.getString(2)).get("writerKind").asText());}
            var state=committedState(s);var carry=(Map<?,?>)state.get("carryBilling");assertEquals(2,((Number)carry.get("ackThroughOrdinal")).intValue());assertTrue(((List<?>)carry.get("effects")).isEmpty());
            assertEquals(0,((CarryBillingEffectSource)s.rooms.requireRuntime(9001).requireAuthoritativeSession()).pendingCarryEffects().effects().size());
        }
    }
    static String stateDigest(Map<String,Object> state)throws Exception {
        byte[] bytes=JSON.copy().configure(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,true).writeValueAsBytes(state);
        return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    static Map<String,Object> committedState(Setup s){return s.committer.latest(9001).orElseThrow().authoritativeState();}
    @Test void realPublishedAuthorityStandPersistsUnusedTerminationInSameTransaction()throws Exception {
        try(var s=new Setup("none",false)){s.directSit("carry-original-request-0002",117);long ledger=count(s.db,"aoo_ledger");var out=s.authority.execute(s.stand());assertNotNull(out.commandResult().committedBinding());assertEquals(ledger,count(s.db,"aoo_ledger"));assertEquals(3,count(s.db,"aoo_room_carry_effect"));s.committer.carryPort().read(9001,()->s.rooms.requireRuntime(9001).requireAuthoritativeSession().viewFor(117));}
    }
    @Test void failedCommitRestoresActualProviderSnapshotBeforeReadGateOpens()throws Exception {
        try(var s=new Setup("before",false)){s.directSit("carry-original-request-0003",117);var before=stateDigest(committedState(s));var command=s.stand();s.armed.set(true);var failure=assertThrows(RuntimeException.class,()->s.authority.execute(command));s.requireInjected(failure);assertEquals(before,stateDigest(s.rooms.requireRuntime(9001).requireAuthoritativeSession().authoritativeState()));s.committer.carryPort().requireAvailable(9001);assertEquals(2,count(s.db,"aoo_room_carry_effect"));}
    }
    @Test void acknowledgementLostUsesExactDurableResultAndDoesNotExecuteAgain()throws Exception {
        try(var s=new Setup("ackLost",false)){s.directSit("carry-original-request-0004",117);long ledgerBefore=count(s.db,"aoo_ledger");var command=s.stand();s.armed.set(true);var result=s.authority.execute(command);assertNotNull(result.commandResult().committedBinding());
            s.requireStoredBinding(command.request(),result.commandResult().committedBinding());
            assertEquals(ledgerBefore,count(s.db,"aoo_ledger"),"acklost does not debit again");
            var session=s.rooms.requireRuntime(9001).requireAuthoritativeSession();var operations=(java.util.List<?>)session.authoritativeState().get("operations");
            assertEquals(1L,operations.stream().filter(command.request().requestId()::equals).count(),"original handler request occurs once");
            assertEquals(command.expectedIdentity().stateVersion()+1,session.stateVersion(),"one committed native mutation");
            assertEquals(3,count(s.db,"aoo_room_carry_effect"));s.committer.carryPort().requireAvailable(9001);assertEquals(1,s.faults.get());assertEquals(1,s.restores.get());}
    }
    @Test void failedNativeRestoreKeepsReadMutationAndPerspectiveUnavailable()throws Exception {
        try(var s=new Setup("before",true)){s.directSit("carry-original-request-0005",117);var command=s.stand();s.armed.set(true);var failure=assertThrows(RuntimeException.class,()->s.authority.execute(command));s.requireInjected(failure);assertThrows(IllegalStateException.class,()->s.committer.carryPort().requireAvailable(9001));var called=new java.util.concurrent.atomic.AtomicInteger();assertThrows(IllegalStateException.class,()->s.committer.carryPort().read(9001,()->{called.incrementAndGet();return s.rooms.requireRuntime(9001).requireAuthoritativeSession().viewFor(117);}));assertEquals(0,called.get());assertThrows(IllegalStateException.class,()->s.authority.execute(command));}
    }
    static GameRoomHandle restoredCopy(Setup s,GameRoomHandle expected,RoomRuntimeIdentity identity) {
        var session=s.provider.restoreAuthoritativeSession(s.committer.latest(9001).orElseThrow().authoritativeState()).orElseThrow();
        return new GameRoomHandle(expected.roomId(),expected.gameId(),expected.playVersion(),session,expected.compatibilityOrigin(),expected.snapshotMetadataSource(),identity);
    }
    @Test void durableCasRejectsIdentityDriftAndKeepsOrdinaryUpdateGuard()throws Exception {
        try(var s=new Setup("none",false)) {
            s.directSit("carry-cas-negative-original",117);var expected=s.rooms.requireRuntime(9001);var id=expected.runtimeIdentity();
            var replacement=restoredCopy(s,expected,id);
            assertThrows(IllegalStateException.class,()->s.rooms.updateRuntime(replacement));
            assertThrows(IllegalStateException.class,()->new RuntimeGameRoomRegistry().restoreRuntimeFromDurable(expected,replacement));
            assertThrows(IllegalStateException.class,()->s.rooms.restoreRuntimeFromDurable(replacement,replacement));
            assertThrows(IllegalStateException.class,()->s.rooms.restoreRuntimeFromDurable(expected,restoredCopy(s,expected,new RoomRuntimeIdentity("other-instance",id.lifecycleEpoch(),id.fencingToken(),id.stateVersion()))));
            assertThrows(IllegalStateException.class,()->s.rooms.restoreRuntimeFromDurable(expected,restoredCopy(s,expected,new RoomRuntimeIdentity(id.roomInstanceId(),id.lifecycleEpoch()+1,id.fencingToken(),id.stateVersion()))));
            assertThrows(IllegalStateException.class,()->s.rooms.restoreRuntimeFromDurable(expected,restoredCopy(s,expected,new RoomRuntimeIdentity(id.roomInstanceId(),id.lifecycleEpoch(),id.fencingToken()+1,id.stateVersion()))));
            assertThrows(IllegalStateException.class,()->s.rooms.restoreRuntimeFromDurable(expected,restoredCopy(s,expected,new RoomRuntimeIdentity(id.roomInstanceId(),id.lifecycleEpoch(),id.fencingToken(),id.stateVersion()-1))));
            assertThrows(IllegalStateException.class,()->s.rooms.restoreRuntimeFromDurable(expected,restoredCopy(s,expected,new RoomRuntimeIdentity(id.roomInstanceId(),id.lifecycleEpoch(),id.fencingToken(),id.stateVersion()+1))));
            assertThrows(IllegalStateException.class,()->s.rooms.restoreRuntimeFromDurable(expected,new GameRoomHandle(9002,expected.gameId(),expected.playVersion(),replacement.legacyRoom(),expected.compatibilityOrigin(),expected.snapshotMetadataSource(),id)));
            assertSame(expected,s.rooms.requireRuntime(9001));assertSame(replacement,s.rooms.restoreRuntimeFromDurable(expected,replacement));
            assertThrows(IllegalStateException.class,()->s.rooms.updateRuntime(restoredCopy(s,replacement,id)));
        }
    }
    @Test void durableCasConcurrentSameExpectedHasExactlyOneWinner()throws Exception {
        try(var s=new Setup("none",false)) {
            s.directSit("carry-cas-concurrent-original",117);var expected=s.rooms.requireRuntime(9001);
            var first=restoredCopy(s,expected,expected.runtimeIdentity());var second=restoredCopy(s,expected,expected.runtimeIdentity());
            var pool=java.util.concurrent.Executors.newFixedThreadPool(2);var ready=new java.util.concurrent.CountDownLatch(2);var go=new java.util.concurrent.CountDownLatch(1);
            try {
                java.util.function.Function<GameRoomHandle,java.util.concurrent.Callable<Boolean>> attempt=handle->()->{ready.countDown();if(!go.await(2,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("CAS barrier deadline");try{s.rooms.restoreRuntimeFromDurable(expected,handle);return true;}catch(IllegalStateException rejected){return false;}};
                var one=pool.submit(attempt.apply(first));var two=pool.submit(attempt.apply(second));assertTrue(ready.await(2,java.util.concurrent.TimeUnit.SECONDS));go.countDown();
                assertEquals(1,(one.get(2,java.util.concurrent.TimeUnit.SECONDS)?1:0)+(two.get(2,java.util.concurrent.TimeUnit.SECONDS)?1:0));
                assertTrue(s.rooms.requireRuntime(9001)==first||s.rooms.requireRuntime(9001)==second);
            } finally {go.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(2,java.util.concurrent.TimeUnit.SECONDS));}
        }
    }
}
