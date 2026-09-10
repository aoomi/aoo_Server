package com.aoo.bcg.club;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

class JdbcClubServiceIntegrationTest {
    @Test void commitsStateAndIdempotencyInTheSameDatabase() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:club_service;MODE=MySQL;DB_CLOSE_DELAY=-1");
        schema(source);var service=new JdbcClubService(source,new ObjectMapper().findAndRegisterModules(),Clock.systemUTC());
        var first=service.create("create-40",40,400,"Durable");var replay=service.create("create-40",999,999,"ignored");
        assertEquals(first,replay);service.saveTemplate("template-40",40,400,"t1","Classic","mj","{}");
        assertEquals(1,service.get(40).templates().size());
        try(Connection c=source.getConnection();var q=c.prepareStatement("SELECT row_version FROM aoo_club_state WHERE club_id=40");var rows=q.executeQuery()){assertTrue(rows.next());assertEquals(2,rows.getLong(1));}
        try(Connection c=source.getConnection();var q=c.prepareStatement("SELECT member_status,member_role FROM aoo_club_member WHERE club_id=40 AND player_id=400");var rows=q.executeQuery()){assertTrue(rows.next());assertEquals("ACTIVE",rows.getString(1));assertEquals("OWNER",rows.getString(2));}
    }
    @Test void memberProjectionTracksRoleAndRemoval() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:club_member_projection;MODE=MySQL;DB_CLOSE_DELAY=-1");
        schema(source);var service=new JdbcClubService(source,new ObjectMapper().findAndRegisterModules(),Clock.systemUTC());
        service.create("create-41",41,410,"Members");
        service.update("join-41",41,state->JdbcClubService.copy(state,state.name(),state.status(),java.util.Map.of(410L,"OWNER",411L,"MEMBER"),state.templates(),state.tables(),state.invites(),state.records(),state.ledger(),state.settings(),state.applications(),state.memberExtras(),state.groupings(),state.roomBans(),state.viewedRooms()));
        service.setRole("role-41",41,410,411,"ADMIN");
        try(Connection c=source.getConnection();var q=c.prepareStatement("SELECT member_role FROM aoo_club_member WHERE club_id=41 AND player_id=411");var rows=q.executeQuery()){assertTrue(rows.next());assertEquals("MANAGER",rows.getString(1));}
        service.kick("kick-41",41,410,411);
        try(Connection c=source.getConnection();var q=c.prepareStatement("SELECT member_status FROM aoo_club_member WHERE club_id=41 AND player_id=411");var rows=q.executeQuery()){assertTrue(rows.next());assertEquals("LEFT",rows.getString(1));}
    }
    @Test void unionTemplateFlowPersistsAndReturnsManagerAndLobbyShapes() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:union_template_flow;MODE=MySQL;DB_CLOSE_DELAY=-1");
        schema(source);var mapper=new ObjectMapper().findAndRegisterModules();var clock=Clock.systemUTC();
        var service=new JdbcClubService(source,mapper,clock);service.create("create-42",42,420,"Union");
        service.update("union-42",42,state->{var settings=new LinkedHashMap<>(state.settings());settings.put("unionId",84);
            return JdbcClubService.copy(state,state.name(),state.status(),state.members(),state.templates(),state.tables(),state.invites(),state.records(),state.ledger(),settings,state.applications(),state.memberExtras(),state.groupings(),state.roomBans(),state.viewedRooms());});
        var dispatch=new ClubDispatchService(source,mapper,clock,()->9001L);
        Map<String,Object> payload=new LinkedHashMap<>(Map.of("clubId",42,"unionId",84,"gameId",201,
                "gameCode","CD201","roomName","验收模板","roomSportsThreshold",100,"JoinGamePoint",10));
        var created=(Map<?,?>)dispatch.dispatch(420,"create-template-42","union.CUnionCreateRoom",payload);
        assertEquals("验收模板",created.get("roomName"));assertEquals(201,((Number)created.get("gameId")).intValue());
        var rows=(java.util.List<?>)dispatch.dispatch(420,"list-template-42","union.CUnionRoomCfgList",Map.of("clubId",42,"unionId",84,"pageNum",1,"classType",2));
        assertEquals(1,rows.size());assertEquals(10,((Number)((Map<?,?>)((Map<?,?>)rows.get(0)).get("bRoomConfigure")).get("JoinGamePoint")).intValue());
        var count=(Map<?,?>)dispatch.dispatch(420,"count-template-42","union.CUnionRoomCfgCount",Map.of("clubId",42,"unionId",84,"classType",2));
        assertEquals(0L,((Number)count.get("roomCount")).longValue());
        var lobby=(java.util.List<?>)dispatch.dispatch(420,"lobby-template-42","union.CUnionGetAllRoomMin",Map.of("clubId",42,"unionId",84));
        assertNotNull(lobby);
    }
    static void schema(JdbcDataSource source)throws Exception{try(Connection c=source.getConnection();var s=c.createStatement()){s.execute("CREATE TABLE aoo_club_state(club_id BIGINT PRIMARY KEY,state_json CLOB NOT NULL,row_version BIGINT NOT NULL,updated_at TIMESTAMP NOT NULL)");s.execute("CREATE TABLE aoo_club_write_idempotency(scope_key VARCHAR(160) PRIMARY KEY,response_json CLOB NOT NULL,created_at TIMESTAMP NOT NULL)");s.execute("CREATE TABLE aoo_club_member(club_id BIGINT NOT NULL,player_id BIGINT NOT NULL,member_status VARCHAR(16) NOT NULL,member_role VARCHAR(16) NOT NULL,online TINYINT NOT NULL DEFAULT 0,profile_payload CLOB NOT NULL,joined_at TIMESTAMP NOT NULL,updated_at TIMESTAMP NOT NULL,PRIMARY KEY(club_id,player_id))");}}
}
