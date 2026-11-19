package com.aoo.bcg.bootstrap;

import com.aoo.bcg.billing.*;
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
final class JdbcRoomSagaBillingPortTest {
    static final Path ROOT=JdbcRoomBillingLifecyclePortTest.ROOT;
    static final String PLAY="liangshan2003-v1.0.0",SOURCE="b".repeat(64);
    static final Clock CLOCK=Clock.fixed(Instant.parse("2026-10-17T00:00:00Z"),ZoneOffset.UTC);
    static final ObjectMapper JSON=new ObjectMapper().findAndRegisterModules();
    static void ddl(Connection c,String name)throws Exception {
        RunScript.execute(c,new StringReader(Files.readString(ROOT.resolve("server/database/migrations/"+name))));
    }
    static void sql(JdbcDataSource d,String text)throws Exception {try(var c=d.getConnection();var s=c.createStatement()){s.execute(text);}}
    static long count(JdbcDataSource d,String table)throws Exception {try(var c=d.getConnection();var q=c.createStatement();var row=q.executeQuery("SELECT COUNT(*) FROM "+table)){assertTrue(row.next());return row.getLong(1);}}
    static long balance(JdbcDataSource d,long actor)throws Exception {try(var c=d.getConnection();var q=c.prepareStatement("SELECT balance FROM aoo_currency_balance WHERE player_id=? AND currency='ROOM_CARD' AND currency_scope_id=0")){q.setLong(1,actor);try(var row=q.executeQuery()){assertTrue(row.next());return row.getLong(1);}}}
    static JdbcDataSource fixture(String templateScope)throws Exception {
        var d=new JdbcDataSource();d.setURL("jdbc:h2:mem:entry_f2_"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=1000");
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
            s.execute("INSERT INTO aoo_game_release VALUES(11,2003,'"+PLAY+"','"+SOURCE+"','ACTIVE')");s.execute("INSERT INTO aoo_compiled_index_active VALUES(2003,'LS','"+PLAY+"',12,11)");
            String validator=JSON.writeValueAsString(Map.of("roomMeasureKind","ROUND_COUNT","fields",List.of(Map.of("key","roundCount","defaultValue",8),Map.of("key","maxPlayers","defaultValue",3),Map.of("key","payerMode","defaultValue","OWNER"))));
            try(var q=c.prepareStatement("INSERT INTO aoo_compiled_room_create_index VALUES(2003,'LS',?,12,11,?,'ACTIVE')")){q.setString(1,PLAY);q.setString(2,validator);q.executeUpdate();}
            s.execute("CREATE TABLE aoo_club_member(club_id BIGINT,player_id BIGINT,member_role VARCHAR,member_status VARCHAR)");
            s.execute("INSERT INTO aoo_club_member VALUES(1,117,'MEMBER','ACTIVE'),(1,107,'OWNER','ACTIVE'),(2,108,'OWNER','ACTIVE')");
            s.execute("CREATE TABLE aoo_club_state(club_id BIGINT PRIMARY KEY,state_json CLOB)");
            Map<String,Object> template=Map.of("id","test-template-1","name","synthetic","game","liangshan2003","rules","{}","index",1);
            Map<String,Object> club=Map.of("id",1,"name","synthetic source","status","ACTIVE","members",Map.of("107","OWNER","117","MEMBER"),"templates",List.of(template),"settings",Map.of("templateScope.1",templateScope,"unionId",5,"unionOwnerClubId",2,"unionOwnerId",108));
            Map<String,Object> unionOwner=Map.of("id",2,"name","synthetic owner","status","ACTIVE","members",Map.of("108","OWNER"),"settings",Map.of("unionId",5,"unionOwnerClubId",2,"unionOwnerId",108));
            try(var q=c.prepareStatement("INSERT INTO aoo_club_state VALUES(?,?)")){q.setLong(1,1);q.setString(2,JSON.writeValueAsString(club));q.executeUpdate();q.setLong(1,2);q.setString(2,JSON.writeValueAsString(unionOwner));q.executeUpdate();}
            for(String context:List.of("HALL","CLUB","UNION")) {
                String payer=context.equals("HALL")?"OWNER":context;
                s.execute("INSERT INTO aoo_room_cost_policy VALUES(1,1,'"+"a".repeat(64)+"',100,'ROOM_CARD',NULL,'refund-proof',0,0,2003,'"+PLAY+"','LS','ROUND_COUNT',8,3,'"+payer+"','FIXED','"+context+"','ACTIVE','CONFIRMED')");
                var identity=new RoomCardPricingImportValidator.IdentityKey(2003,"liangshan2003",PLAY,"LS");
                var priceKey=new RoomCardPricingImportValidator.PriceKey(identity,"FIXED",context,3,"ROUND_COUNT",8,payer,0);
                c.setAutoCommit(false);
                new JdbcRoomCardPricingProvenanceRepository().save(c,new RoomCardPricingProvenance(1,RoomCardPricingProvenance.policyKey(priceKey),"fixture-v1","a".repeat(64),"c".repeat(64),new RoomCardPricingImportValidator.SourceBinding(RoomCardPricingImportValidator.SourceClass.CURRENT_PUBLISHED,11,12,SOURCE),null,null,null,false));
                c.commit();c.setAutoCommit(true);
            }
        }
        for(String t:List.of("aoo_ledger","aoo_room_billing_reservation","aoo_room_billing_intent_journal","aoo_room_billing_source_binding","aoo_room_billing_business_receipt"))assertEquals(0,count(d,t));
        return d;
    }
    static LobbyRoomCreateSaga.Command command(JdbcDataSource d,long authenticated,boolean club,boolean trusted,String payer)throws Exception {
        Map<String,Object> body=new LinkedHashMap<>();body.put("roomId",9001);body.put("gameId",2003);body.put("playVersion",PLAY);body.put("clientVersion","3.8.8");body.put("stateVersion",1);
        body.put("ruleRevision",Map.of("classificationRegionCode","LS","releaseId",11L,"indexGeneration",12L,"sourceHash",SOURCE));
        body.put("rules",Map.of("roundCount",8,"maxPlayers",3,"payerMode",payer));body.put("scope",club?Map.of("type","CLUB","clubId",1,"templateCode","test-template-1"):Map.of("type","PERSONAL"));
        body.put("ownerId",9999);body.put("payerId",9999); // untrusted fields cannot select the charging identity
        var parsed=LobbyRoomCreateSaga.Command.parse(authenticated,"entry-authenticated-request-0001",body,trusted?LobbyRoomCreateSaga.CreationOrigin.CLUB_TEMPLATE_ENTRY:LobbyRoomCreateSaga.CreationOrigin.PUBLIC);
        LobbyMembershipAuthorityPort noNetwork=new LobbyMembershipAuthorityPort(){public Map<String,Object>join(long r,long a,int s,String p,String q,String t,Map<String,Object>x){throw new AssertionError("no listener");}public Map<String,Object>leave(long r,long a,String p,String q,String t,Map<String,Object>x){throw new AssertionError("no listener");}};
        var normalized=new JdbcLobbyRepository(d,JSON,CLOCK,noNetwork).normalizeCreateCommand(parsed);
        new LobbyRoomCreateSagaStore(d,JSON).begin(normalized);return normalized;
    }
    static JdbcRoomBillingLifecyclePort factory(JdbcDataSource d){return BootstrapAPP.roomBillingLifecycle(d,new AtomicLong(1000)::incrementAndGet,CLOCK);}
    @Test void realFactoryNormalizesAuthenticatedHallAndDebitsOnlyOnce()throws Exception {
        var d=fixture("CLUB");var command=command(d,117,false,false,"OWNER");var port=factory(d);port.reserve(command);
        assertEquals(400,balance(d,117));assertEquals(1,count(d,"aoo_ledger"));assertEquals(1,count(d,"aoo_room_billing_reservation"));assertEquals(1,count(d,"aoo_room_billing_source_binding"));assertEquals(0,count(d,"aoo_room_billing_business_receipt"));
        try(var c=d.getConnection();var q=c.createStatement();var row=q.executeQuery("SELECT authenticated_actor_id,creator_id FROM aoo_room_billing_source_binding")){assertTrue(row.next());assertEquals(117L,row.getLong(1));assertEquals(117L,row.getLong(2));}
        sql(d,"UPDATE aoo_room_cost_policy SET cost_minor=300");port.reserve(command);assertEquals(400,balance(d,117));assertEquals(1,count(d,"aoo_ledger"));
    }
    @Test void clubMemberChargesTrustedClubOwnerDespiteUnionMembership()throws Exception {
        var d=fixture("CLUB");factory(d).reserve(command(d,117,true,true,"OWNER"));assertEquals(400,balance(d,107));assertEquals(500,balance(d,108));assertEquals(500,balance(d,117));assertEquals(1,count(d,"aoo_ledger"));
    }
    @Test void explicitUnionTemplateChargesValidatedUnionOwner()throws Exception {
        var d=fixture("UNION:5");factory(d).reserve(command(d,117,true,true,"OWNER"));assertEquals(400,balance(d,108));assertEquals(500,balance(d,107));assertEquals(500,balance(d,117));assertEquals(1,count(d,"aoo_ledger"));
    }
    @Test void unauthorizedScopeAndMismatchedProvenanceNeverDebit()throws Exception {
        var publicMember=fixture("CLUB");var a=command(publicMember,117,true,false,"OWNER");assertThrows(RuntimeException.class,()->factory(publicMember).reserve(a));assertEquals(0,count(publicMember,"aoo_ledger"));assertEquals(0,count(publicMember,"aoo_room_billing_source_binding"));
        var absent=fixture("CLUB");var b=command(absent,118,true,true,"OWNER");assertThrows(RuntimeException.class,()->factory(absent).reserve(b));assertEquals(0,count(absent,"aoo_ledger"));
        for(String column:List.of("release_id","index_generation","source_hash")){
            var d=fixture("CLUB");sql(d,"UPDATE aoo_room_cost_policy_provenance SET "+column+"="+(column.equals("source_hash")?"'"+"d".repeat(64)+"'":"99"));var c=command(d,117,false,false,"OWNER");assertThrows(IllegalArgumentException.class,()->factory(d).reserve(c));assertEquals(500,balance(d,117));assertEquals(0,count(d,"aoo_ledger"));assertEquals(0,count(d,"aoo_room_billing_reservation"));
        }
    }
    @Test void aaCreateNeverChargesAllParticipants()throws Exception {
        var d=fixture("CLUB");var c=command(d,117,false,false,"AA");assertThrows(IllegalArgumentException.class,()->factory(d).reserve(c));assertEquals(500,balance(d,117));assertEquals(0,count(d,"aoo_ledger"));assertEquals(0,count(d,"aoo_room_billing_source_binding"));
    }
}
