package com.aoo.bcg.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Synthetic staff session rows exercise verification logic, not real Admin login or DB grants. */
class SupportWorkflowHttpTest {
    static final String SECRET="01234567890123456789012345678901";
    static final String SESSION="A".repeat(43), SECOND="B".repeat(43), CSRF="C".repeat(43);
    final ObjectMapper json=new ObjectMapper().findAndRegisterModules();
    final Clock clock=Clock.fixed(Instant.parse("2026-10-14T11:00:00Z"),ZoneOffset.UTC);
    JdbcDataSource ds; JdbcSupportRepository repo; HttpServer server;
    final HttpClient client=HttpClient.newHttpClient();
    @BeforeEach void setup()throws Exception {
        ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:workflow"+System.nanoTime()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        sql("CREATE TABLE support_case(id BIGINT AUTO_INCREMENT PRIMARY KEY,player_id BIGINT NOT NULL,case_kind VARCHAR(20),subject VARCHAR(120),description VARCHAR(4000),status VARCHAR(20),resolution_summary VARCHAR(1000),created_at TIMESTAMP,updated_at TIMESTAMP,row_version BIGINT)",
            "CREATE TABLE support_case_evidence(id BIGINT AUTO_INCREMENT PRIMARY KEY,case_id BIGINT,evidence_type VARCHAR(20),reference_id VARCHAR(256),label VARCHAR(80))",
            "CREATE TABLE support_case_event(id BIGINT AUTO_INCREMENT PRIMARY KEY,case_id BIGINT,actor_type VARCHAR(20),actor_id BIGINT,event_type VARCHAR(40),case_status VARCHAR(20),created_at TIMESTAMP)",
            "CREATE TABLE replay_participant(room_id BIGINT,set_id INT,player_id BIGINT)",
            "CREATE TABLE replay_participant_archive(room_id BIGINT,set_id INT,player_id BIGINT)",
            "CREATE TABLE admin_operator_account(operator_id BIGINT PRIMARY KEY,enabled INT)",
            "CREATE TABLE admin_browser_session(session_hash VARCHAR(64) PRIMARY KEY,operator_id BIGINT,csrf_hash VARCHAR(64),expires_at TIMESTAMP,revoked_at TIMESTAMP)",
            "CREATE TABLE admin_operator_permission(operator_id BIGINT,permission_code VARCHAR(96),enabled INT)",
            "CREATE TABLE admin_operator_role(operator_id BIGINT,role_code VARCHAR(64),enabled INT)",
            "CREATE TABLE admin_role(role_code VARCHAR(64) PRIMARY KEY,enabled INT)",
            "CREATE TABLE admin_role_permission(role_code VARCHAR(64),permission_code VARCHAR(96))",
            "INSERT INTO admin_operator_account VALUES(99,1),(98,1)",
            "INSERT INTO admin_operator_permission VALUES(99,'support.case.read',1),(99,'support.case.handle',1)",
            "INSERT INTO admin_role VALUES('support',1)",
            "INSERT INTO admin_operator_role VALUES(98,'support',1)",
            "INSERT INTO admin_role_permission VALUES('support','support.case.read'),('support','support.case.handle')");
        try(var c=ds.getConnection();var q=c.prepareStatement("INSERT INTO admin_browser_session VALUES(?,?,?,?,NULL)")){
            for(var entry:Map.of(SESSION,99L,SECOND,98L).entrySet()){
                q.setString(1,hash(entry.getKey()));q.setLong(2,entry.getValue());q.setString(3,hash(CSRF));q.setTimestamp(4,java.sql.Timestamp.from(clock.instant().plusSeconds(3600)));q.executeUpdate();
            }
        }
        repo=new JdbcSupportRepository(ds,json,clock);
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        new SupportHttpRoutes(repo,new PlayerBearerAuthenticator(SECRET,clock),json,"abcdefghijklmnopqrstuvwxyz0123456789").mount(server);server.start();
    }
    @AfterEach void stop(){if(server!=null)server.stop(0);}
    @Test void authenticatedCaseLifecyclePersistsAndIsAssigned()throws Exception {
        var created=player(7,"/v1/support/cases","POST","{\"kind\":\"TICKET\",\"subject\":\"incident\",\"description\":\"review\"}");assertEquals(201,created.statusCode());
        long id=data(created).get("id").asLong();String base="/internal/v1/support/cases/"+id;
        assertEquals("SUBMITTED",data(player(7,"/v1/support/cases/"+id+"/workflow","GET",null)).get("phase").asText());
        assertEquals(401,player(8,"/v1/support/cases/"+id+"/workflow","GET",null).statusCode());
        assertEquals(200,staff(SESSION,base+"/accept","POST","{\"version\":1}").statusCode());
        assertEquals(401,staff(SECOND,base+"/process","POST","{\"version\":2}").statusCode());
        assertEquals(409,staff(SESSION,base+"/process","POST","{\"version\":1}").statusCode());
        assertEquals(409,staff(SESSION,base+"/close","POST","{\"version\":2}").statusCode());
        assertEquals(200,staff(SESSION,base+"/process","POST","{\"version\":2}").statusCode());
        assertEquals(200,staff(SESSION,base+"/reply","POST","{\"version\":3,\"reply\":\"Reviewed; contact person@example.invalid\"}").statusCode());
        var playerView=data(player(7,"/v1/support/cases/"+id+"/workflow","GET",null));assertTrue(playerView.get("reply").asText().contains("Reviewed"));assertEquals("REPLIED",playerView.get("phase").asText());
        var staffView=data(staff(SESSION,base+"/workflow","GET",null));assertEquals(99,staffView.get("assignedStaffId").asLong());assertFalse(staffView.has("playerId"));assertFalse(staffView.has("description"));assertFalse(staffView.toString().contains("person@example.invalid"));
        assertEquals(200,staff(SESSION,base+"/close","POST","{\"version\":4}").statusCode());
        assertEquals("CLOSED",new JdbcSupportRepository(ds,json,clock).playerWorkflow(7,id).get("phase"));
        assertEquals(401,player(8,"/v1/support/cases/"+id+"/reopen","POST","{\"version\":5}").statusCode());
        assertEquals(200,player(7,"/v1/support/cases/"+id+"/reopen","POST","{\"version\":5}").statusCode());
        var reopened=repo.playerWorkflow(7,id);assertEquals("REOPENED",reopened.get("phase"));assertNull(reopened.get("assignedStaffId"));
        assertEquals(List.of("CREATED","ACCEPTED","PROCESSING","REPLIED","CLOSED","REOPENED"),repo.events(7,0).stream().map(e->e.get("type")).toList());
        assertEquals(200,staff(SECOND,base+"/accept","POST","{\"version\":6}").statusCode());
        assertEquals(401,staff(SESSION,base+"/process","POST","{\"version\":7}").statusCode());
    }
    @Test void staffAuthorityIsFreshAndCallerIdentityCannotOverrideIt()throws Exception {
        long id=repo.create(7,SupportCase.Kind.TICKET,"incident","review",List.of()).id();String path="/internal/v1/support/cases/"+id+"/workflow";
        assertEquals(200,staff(SESSION,path,"GET",null).statusCode());
        assertEquals(401,request(path,"GET",null,Map.of("Cookie","aoo_admin_session="+SESSION,"X-CSRF-Token",CSRF,"X-Support-Agent-Id","98")).statusCode());
        assertEquals(401,request(path,"GET",null,Map.of("Authorization","Bearer abcdefghijklmnopqrstuvwxyz0123456789","X-Support-Agent-Id","99")).statusCode());
        assertEquals(401,request(path,"GET",null,Map.of("Cookie","aoo_admin_session="+SESSION,"X-CSRF-Token","D".repeat(43))).statusCode());
        assertEquals(401,request(path,"GET",null,Map.of("Cookie","aoo_admin_session="+SESSION+"; aoo_admin_session="+SECOND,"X-CSRF-Token",CSRF)).statusCode());
        sql("UPDATE admin_browser_session SET revoked_at=CURRENT_TIMESTAMP WHERE operator_id=99");assertEquals(401,staff(SESSION,path,"GET",null).statusCode());
        expiry(clock.instant().minusSeconds(60));assertEquals(401,staff(SESSION,path,"GET",null).statusCode());
        expiry(clock.instant().plusSeconds(3600));sql("UPDATE admin_operator_account SET enabled=0 WHERE operator_id=99");assertEquals(401,staff(SESSION,path,"GET",null).statusCode());
        sql("UPDATE admin_operator_account SET enabled=1 WHERE operator_id=99","UPDATE admin_operator_permission SET enabled=0 WHERE operator_id=99");assertEquals(401,staff(SESSION,path,"GET",null).statusCode());
        assertEquals(200,staff(SECOND,path,"GET",null).statusCode());sql("DELETE FROM admin_role_permission WHERE role_code='support'");assertEquals(401,staff(SECOND,path,"GET",null).statusCode());
        assertEquals("SUBMITTED",repo.playerWorkflow(7,id).get("phase"));
    }
    @Test void exactFieldsStaleDuplicatesAndConcurrentClaimCannotMutateAnotherScope()throws Exception {
        long id=repo.create(7,SupportCase.Kind.TICKET,"incident","review",List.of()).id();String base="/internal/v1/support/cases/"+id;
        assertEquals(400,staff(SESSION,base+"/accept","POST","{\"version\":1,\"balance\":100,\"role\":\"admin\",\"hiddenCards\":[],\"export\":true}").statusCode());
        assertEquals(400,staff(SESSION,base+"/accept","POST","{\"version\":1.5}").statusCode());
        assertEquals("SUBMITTED",repo.playerWorkflow(7,id).get("phase"));
        try(var pool=Executors.newFixedThreadPool(2)){
            var start=new CountDownLatch(1);Callable<Boolean> claim=()->{start.await();try{repo.handleCase(99,id,1,"accept",null);return true;}catch(IllegalStateException conflict){return false;}};
            var a=pool.submit(claim);var b=pool.submit(claim);start.countDown();assertNotEquals(a.get(5,TimeUnit.SECONDS),b.get(5,TimeUnit.SECONDS));
        }
        assertEquals(409,staff(SESSION,base+"/accept","POST","{\"version\":1}").statusCode());assertEquals(2,repo.events(7,0).size());
        assertEquals(200,player(7,"/v1/support/cases/"+id+"/withdraw","POST","{\"version\":2}").statusCode());
        assertThrows(SecurityException.class,()->repo.handleCase(99,id,3,"process",null));assertEquals("WITHDRAWN",repo.playerWorkflow(7,id).get("phase"));
    }
    @Test void explicitStaffDatasourceIsIndependentAndMissingAuthorityNeverFallsBack()throws Exception {
        var staffDs=new JdbcDataSource();staffDs.setURL("jdbc:h2:mem:staff"+System.nanoTime()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        try(var c=staffDs.getConnection();var s=c.createStatement()){
            s.execute("CREATE TABLE admin_operator_account(operator_id BIGINT PRIMARY KEY,enabled INT)");
            s.execute("CREATE TABLE admin_browser_session(session_hash VARCHAR(64) PRIMARY KEY,operator_id BIGINT,csrf_hash VARCHAR(64),expires_at TIMESTAMP,revoked_at TIMESTAMP)");
            s.execute("CREATE TABLE admin_operator_permission(operator_id BIGINT,permission_code VARCHAR(96),enabled INT)");
            s.execute("CREATE TABLE admin_operator_role(operator_id BIGINT,role_code VARCHAR(64),enabled INT)");
            s.execute("CREATE TABLE admin_role(role_code VARCHAR(64) PRIMARY KEY,enabled INT)");
            s.execute("CREATE TABLE admin_role_permission(role_code VARCHAR(64),permission_code VARCHAR(96))");
            s.execute("INSERT INTO admin_operator_account VALUES(99,1)");
            s.execute("INSERT INTO admin_operator_permission VALUES(99,'support.case.read',1),(99,'support.case.handle',1)");
            try(var q=c.prepareStatement("INSERT INTO admin_browser_session VALUES(?,?,?,?,NULL)")){
                q.setString(1,hash(SESSION));q.setLong(2,99);q.setString(3,hash(CSRF));q.setTimestamp(4,java.sql.Timestamp.from(clock.instant().plusSeconds(3600)));q.executeUpdate();
            }
        }
        sql("DROP TABLE admin_browser_session","DROP TABLE admin_operator_account","DROP TABLE admin_operator_permission","DROP TABLE admin_operator_role","DROP TABLE admin_role_permission","DROP TABLE admin_role");
        try(var c=ds.getConnection();var q=c.prepareStatement("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='public' AND TABLE_NAME LIKE 'admin_%'");var r=q.executeQuery()){
            assertTrue(r.next());assertEquals(0,r.getInt(1));
        }
        var players=new PlayerBearerAuthenticator(SECRET,clock);
        assertThrows(NullPointerException.class,()->new SupportHttpRoutes(repo,players,json,"abcdefghijklmnopqrstuvwxyz0123456789",null));
        long id=repo.create(7,SupportCase.Kind.TICKET,"incident","review",List.of()).id();String base="/internal/v1/support/cases/"+id;
        server.stop(0);server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        new SupportHttpRoutes(repo,players,json,"abcdefghijklmnopqrstuvwxyz0123456789",new SupportStaffSessionAuthenticator(staffDs,clock)).mount(server);server.start();
        assertEquals(200,staff(SESSION,base+"/workflow","GET",null).statusCode());
        assertEquals(401,staff(SECOND,base+"/accept","POST","{\"version\":1}").statusCode());
        assertEquals("SUBMITTED",repo.playerWorkflow(7,id).get("phase"));
        assertEquals(200,staff(SESSION,base+"/accept","POST","{\"version\":1}").statusCode());
        assertEquals(99L,repo.playerWorkflow(7,id).get("assignedStaffId"));
        long untouched=repo.create(7,SupportCase.Kind.TICKET,"other incident","review",List.of()).id();
        server.stop(0);server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        new SupportHttpRoutes(repo,players,json,"abcdefghijklmnopqrstuvwxyz0123456789",new SupportStaffSessionAuthenticator(ds,clock)).mount(server);server.start();
        var rejected=staff(SESSION,"/internal/v1/support/cases/"+untouched+"/accept","POST","{\"version\":1}");
        assertEquals(409,rejected.statusCode());assertEquals("SUBMITTED",repo.playerWorkflow(7,untouched).get("phase"));
        assertEquals(1,repo.find(7,untouched).orElseThrow().version());
    }
    private void expiry(Instant value)throws Exception{try(var c=ds.getConnection();var q=c.prepareStatement("UPDATE admin_browser_session SET revoked_at=NULL,expires_at=? WHERE operator_id=99")){q.setTimestamp(1,java.sql.Timestamp.from(value));q.executeUpdate();}}
    private void sql(String...commands)throws Exception{try(var c=ds.getConnection();var s=c.createStatement()){for(String sql:commands)s.execute(sql);}}
    private com.fasterxml.jackson.databind.JsonNode data(HttpResponse<String> response)throws Exception{return json.readTree(response.body()).get("data");}
    private HttpResponse<String> staff(String session,String path,String method,String body)throws Exception{return request(path,method,body,Map.of("Cookie","aoo_admin_session="+session,"X-CSRF-Token",CSRF));}
    private HttpResponse<String> player(long id,String path,String method,String body)throws Exception {
        String value=id+"."+clock.instant().plusSeconds(60).getEpochSecond();Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
        return request(path,method,body,Map.of("Authorization","Bearer "+value+"."+HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)))));
    }
    private HttpResponse<String> request(String path,String method,String body,Map<String,String> headers)throws Exception{
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+server.getAddress().getPort()+path)).timeout(Duration.ofSeconds(5)).header("X-API-Version","1");headers.forEach(builder::header);
        return client.send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    private static String hash(String value)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
}
