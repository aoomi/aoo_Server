package com.aoo.bcg.common.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="AOO_DB_IT_URL", matches=".+")
class BusinessKeyConcurrencyIntegrationTest {
    @Test void databaseAllowsExactlyOneConcurrentInsertPerPlayMemberAndTemplateBusinessKey() throws Exception {
        var dataSource=new DriverManagerDataSource(System.getenv("AOO_DB_IT_URL"),System.getenv("AOO_DB_IT_USER"),System.getenv("AOO_DB_IT_PASSWORD"));
        long suffix=System.currentTimeMillis();
        try {
            assertEquals(1,race(16,()->insert(dataSource,"INSERT INTO aoo_play_variant(game_id,play_version,region_code,component_version,rule_schema_version,status,created_at) VALUES(?,?,?,?,1,'ACTIVE',?)",900000+suffix%10000,"it-"+suffix,"IT","component-"+suffix,Timestamp.from(Instant.now()))));
            assertEquals(1,race(16,()->insert(dataSource,"INSERT INTO aoo_club_member(club_id,player_id,member_status,member_role,online,profile_payload,joined_at,updated_at) VALUES(?,?,'ACTIVE','MEMBER',0,'{}',?,?)",suffix,suffix,Timestamp.from(Instant.now()),Timestamp.from(Instant.now()))));
            assertEquals(1,race(16,()->insert(dataSource,"INSERT INTO aoo_room_template(club_id,template_code,template_version,game_id,play_version,display_name,rule_payload,status,created_by,created_at,updated_at) VALUES(?,?,1,62,'v1',?,'{}','ACTIVE',1,?,?)",suffix,"it-"+suffix,"IT "+suffix,Timestamp.from(Instant.now()),Timestamp.from(Instant.now()))));
        } finally {
            try(var connection=dataSource.getConnection()) {
                for(String sql:List.of("DELETE FROM aoo_room_template WHERE club_id="+suffix,"DELETE FROM aoo_club_member WHERE club_id="+suffix,"DELETE FROM aoo_play_variant WHERE play_version='it-"+suffix+"'")) try(var statement=connection.createStatement()){statement.executeUpdate(sql);}
            }
        }
    }
    private static int race(int workers,ThrowingRunnable insert)throws Exception{var ready=new CountDownLatch(workers);var go=new CountDownLatch(1);var success=new AtomicInteger();try(var pool=Executors.newFixedThreadPool(workers)){List<Future<?>>tasks=new ArrayList<>();for(int i=0;i<workers;i++)tasks.add(pool.submit(()->{ready.countDown();go.await();try{insert.run();success.incrementAndGet();}catch(java.sql.SQLException conflict){if(conflict.getErrorCode()!=1062&&conflict.getErrorCode()!=1213)throw conflict;}return null;}));ready.await();go.countDown();for(Future<?>task:tasks)task.get();}return success.get();}
    private static void insert(javax.sql.DataSource source,String sql,Object...values)throws Exception{try(var connection=source.getConnection()){if(sql.contains("aoo_room_template"))try(var constraint=connection.createStatement()){constraint.execute("SET FOREIGN_KEY_CHECKS=0");}try(var statement=connection.prepareStatement(sql)){for(int i=0;i<values.length;i++)statement.setObject(i+1,values[i]);statement.executeUpdate();}}}
    @FunctionalInterface private interface ThrowingRunnable{void run()throws Exception;}
}
