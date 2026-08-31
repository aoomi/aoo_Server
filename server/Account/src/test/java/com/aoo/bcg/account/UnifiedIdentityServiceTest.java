package com.aoo.bcg.account;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class UnifiedIdentityServiceTest {
    JdbcDataSource ds; UnifiedIdentityService service;
    @BeforeEach void setup()throws Exception{
        ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:identity"+System.nanoTime()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
        try(Connection c=ds.getConnection();Statement s=c.createStatement()){
            s.execute("CREATE TABLE aoo_account(account_id BIGINT PRIMARY KEY,password_hash VARCHAR(255),guest BOOLEAN DEFAULT FALSE,auth_generation BIGINT DEFAULT 0,updated_at TIMESTAMP,created_at TIMESTAMP)");
            s.execute("CREATE TABLE aoo_account_session(session_id VARCHAR(36) PRIMARY KEY,account_id BIGINT,revoked_at TIMESTAMP,revoke_reason VARCHAR(64))");
            s.execute("CREATE TABLE aoo_account_audit(audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,account_id BIGINT,action_name VARCHAR(64),detail_value VARCHAR(500),source_ip VARCHAR(64),occurred_at TIMESTAMP)");
            s.execute("CREATE TABLE aoo_outbox(event_id VARCHAR(64) PRIMARY KEY,aggregate_type VARCHAR(64),aggregate_id BIGINT,event_type VARCHAR(128),payload JSON,created_at TIMESTAMP)");
            s.execute("CREATE TABLE aoo_display_id_segment(digit_length INT PRIMARY KEY,range_start DECIMAL(65),range_end DECIMAL(65),capacity BIGINT,occupied_count BIGINT,allocation_threshold_bps INT,status VARCHAR(16),updated_at TIMESTAMP)");
            s.execute("INSERT INTO aoo_display_id_segment VALUES(6,100000,999999,900000,0,8500,'ACTIVE',CURRENT_TIMESTAMP)");
            s.execute("CREATE TABLE aoo_account_identity(identity_id BIGINT AUTO_INCREMENT PRIMARY KEY,identity_type VARCHAR(24),normalized_value VARCHAR(255),value_hash CHAR(64),account_id BIGINT,verified BOOLEAN,status VARCHAR(16),created_at TIMESTAMP,updated_at TIMESTAMP,UNIQUE(identity_type,value_hash,status))");
            s.execute("CREATE TABLE aoo_display_id_history(history_id BIGINT AUTO_INCREMENT PRIMARY KEY,account_id BIGINT,old_display_id VARCHAR(65),new_display_id VARCHAR(65),valid_from TIMESTAMP,valid_until TIMESTAMP,operator_id BIGINT,reason VARCHAR(255),source_ip VARCHAR(64),trace_id VARCHAR(128))");
            s.execute("CREATE TABLE aoo_identity_admin_request(request_id VARCHAR(128) PRIMARY KEY,result_json VARCHAR(255),completed_at TIMESTAMP)");
            s.execute("CREATE TABLE aoo_identity_migration(migration_id VARCHAR(36),source_account_id BIGINT,target_account_id BIGINT,identity_type VARCHAR(24),value_hash CHAR(64),source_proof_hash CHAR(64),target_proof_hash CHAR(64),operator_id BIGINT,reason VARCHAR(255),trace_id VARCHAR(128),created_at TIMESTAMP)");
            for(long i=1;i<=40;i++)s.execute("INSERT INTO aoo_account(account_id,updated_at,created_at) VALUES("+i+",CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        }service=new UnifiedIdentityService(ds,Clock.fixed(Instant.parse("2026-08-25T00:00:00Z"),ZoneOffset.UTC));
    }
    @Test void concurrentAllocationIsUnique()throws Exception{ExecutorService pool=Executors.newFixedThreadPool(8);try{List<Future<String>> ids=new ArrayList<>();for(long i=1;i<=32;i++){long id=i;ids.add(pool.submit(()->service.allocate(id,"concurrent-"+id)));}Set<String> unique=new HashSet<>();for(Future<String> id:ids){String v=id.get(10,TimeUnit.SECONDS);assertTrue(v.matches("[1-9][0-9]{5}"));unique.add(v);}assertEquals(32,unique.size());}finally{pool.shutdownNow();}}
    @Test void thresholdExpandsToSevenDigits()throws Exception{try(Connection c=ds.getConnection();Statement s=c.createStatement()){s.execute("UPDATE aoo_display_id_segment SET occupied_count=765000 WHERE digit_length=6");}assertTrue(service.allocate(1,"expand").matches("[1-9][0-9]{6}"));}
    @Test void adminChangeReleasesOldIdPreservesHistoryAndRevokesSessions()throws Exception{String old=service.allocate(1,"initial");service.allocate(2,"second");try(Connection c=ds.getConnection();Statement s=c.createStatement()){s.execute("INSERT INTO aoo_account_session VALUES('session-1',1,NULL,NULL)");}service.changeDisplayId(1,"888999",admin("change-1"));assertEquals(1,service.resolve("888999"));assertThrows(UnifiedIdentityService.NotFound.class,()->service.resolve(old));service.changeDisplayId(2,old,admin("reuse-2"));assertEquals(2,service.resolve(old));List<UnifiedIdentityService.DisplayOwner> owners=service.owners(old);assertEquals(2,owners.size());assertTrue(owners.stream().anyMatch(v->v.accountId()==1&&!v.current()));assertTrue(owners.stream().anyMatch(v->v.accountId()==2&&v.current()));try(Connection c=ds.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT revoked_at FROM aoo_account_session WHERE session_id='session-1'")){assertTrue(r.next());assertNotNull(r.getTimestamp(1));}}
    @Test void identityConflictNeverSilentlyMergesAndMigrationRequiresPermissionAndTwoProofs(){service.bind(1,UnifiedIdentityService.Type.PHONE,"+8613812345678",true,admin("bind-1"));assertEquals(1,service.resolve("+8613812345678"));assertThrows(UnifiedIdentityService.Conflict.class,()->service.bind(2,UnifiedIdentityService.Type.PHONE,"+8613812345678",true,admin("bind-2")));assertThrows(IllegalArgumentException.class,()->service.migrateIdentity(1,2,UnifiedIdentityService.Type.PHONE,"+8613812345678","same","same",adminMigrate("migrate-1")));service.migrateIdentity(1,2,UnifiedIdentityService.Type.PHONE,"+8613812345678","source-proof","target-proof",adminMigrate("migrate-2"));assertEquals(2,service.resolve("+8613812345678"));}
    @Test void highRiskMutationRequiresSecondFactorAndIsIdempotent(){var denied=new UnifiedIdentityService.AdminContext(7,"ticket","127.0.0.1","t","denied",false,Set.of("account.identity.mutate"));assertThrows(UnifiedIdentityService.Forbidden.class,()->service.changeDisplayId(1,"777777",denied));service.allocate(1,"first");service.changeDisplayId(1,"777777",admin("same-request"));assertThrows(UnifiedIdentityService.DuplicateRequest.class,()->service.changeDisplayId(1,"666666",admin("same-request")));}
    private UnifiedIdentityService.AdminContext admin(String request){return new UnifiedIdentityService.AdminContext(7,"support ticket","127.0.0.1","trace-"+request,request,true,Set.of("account.identity.mutate"));}
    private UnifiedIdentityService.AdminContext adminMigrate(String request){return new UnifiedIdentityService.AdminContext(7,"approved migration","127.0.0.1","trace-"+request,request,true,Set.of("account.identity.migrate"));}
}
