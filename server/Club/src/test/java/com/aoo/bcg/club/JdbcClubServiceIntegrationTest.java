package com.aoo.bcg.club;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.time.Clock;

class JdbcClubServiceIntegrationTest {
    @Test void commitsStateAndIdempotencyInTheSameDatabase() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:club_service;MODE=MySQL;DB_CLOSE_DELAY=-1");
        schema(source);var service=new JdbcClubService(source,new ObjectMapper().findAndRegisterModules(),Clock.systemUTC());
        var first=service.create("create-40",40,400,"Durable");var replay=service.create("create-40",999,999,"ignored");
        assertEquals(first,replay);service.saveTemplate("template-40",40,400,"t1","Classic","mj","{}");
        assertEquals(1,service.get(40).templates().size());
        try(Connection c=source.getConnection();var q=c.prepareStatement("SELECT row_version FROM aoo_club_state WHERE club_id=40");var rows=q.executeQuery()){assertTrue(rows.next());assertEquals(2,rows.getLong(1));}
    }
    static void schema(JdbcDataSource source)throws Exception{try(Connection c=source.getConnection();var s=c.createStatement()){s.execute("CREATE TABLE aoo_club_state(club_id BIGINT PRIMARY KEY,state_json CLOB NOT NULL,row_version BIGINT NOT NULL,updated_at TIMESTAMP NOT NULL)");s.execute("CREATE TABLE aoo_club_write_idempotency(scope_key VARCHAR(160) PRIMARY KEY,response_json CLOB NOT NULL,created_at TIMESTAMP NOT NULL)");}}
}
