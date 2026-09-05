package com.aoo.bcg.bootstrap;

import static org.junit.jupiter.api.Assertions.*;

import com.aoo.bcg.gateway.ConnectionIdentity;
import com.aoo.bcg.gateway.WebSocketFrame;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

class ClubDispatchIntegrationTest {
    @Test void clubDispatchCommitsCreateJoinAuditAndMemberProjection() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:club_dispatch;MODE=MySQL;DB_CLOSE_DELAY=-1");
        source.setUser("sa");
        schema(source);
        Clock clock = Clock.fixed(Instant.parse("2026-09-01T00:00:00Z"), ZoneOffset.UTC);
        JdbcHallWebSocketDispatcher dispatcher = new JdbcHallWebSocketDispatcher(
                source, new ObjectMapper().findAndRegisterModules(), clock);

        ConnectionIdentity owner = new ConnectionIdentity(700, "owner-device", "https://game.example");
        try (Connection c = source.getConnection(); var s = c.createStatement()) {
            s.execute("INSERT INTO aoo_currency_balance(player_id,currency,currency_scope_id,balance,version,updated_at) VALUES(700,'CRYSTAL',0,10000,0,CURRENT_TIMESTAMP(3))");
        }
        Object created = dispatcher.dispatch(owner, frame("create-1", 1, "club.CClubCreate",
                Map.of("clubName", "Dispatch Circle")));
        assertInstanceOf(Map.class, created);
        long clubId = ((Number) ((Map<?, ?>) created).get("id")).longValue();
        assertEquals(9900L, ((Number) ((Map<?, ?>) created).get("diamond")).longValue());
        assertEquals(9900L, ((Number) ((Map<?, ?>) created).get("roomCard")).longValue());
        Object replay = dispatcher.dispatch(owner, frame("create-1", 1, "club.CClubCreate",
                Map.of("clubName", "Dispatch Circle")));
        assertEquals(clubId, ((Number) ((Map<?, ?>) replay).get("id")).longValue());
        assertEquals(9900L, ((Number) ((Map<?, ?>) replay).get("diamond")).longValue());
        try (Connection c = source.getConnection();
             var q = c.prepareStatement("SELECT balance FROM aoo_currency_balance WHERE player_id=700 AND currency='CRYSTAL' AND currency_scope_id=0")) {
            try (var rows = q.executeQuery()) {
                assertTrue(rows.next());
                assertEquals(9900L, rows.getLong(1));
            }
        }
        try (Connection c = source.getConnection();
             var q = c.prepareStatement("SELECT delta,balance_after,reason_code FROM aoo_ledger WHERE business_id='legacy-wss:club.CClubCreate.billing:create-1'")) {
            try (var rows = q.executeQuery()) {
                assertTrue(rows.next());
                assertEquals(-100L, rows.getLong(1));
                assertEquals(9900L, rows.getLong(2));
                assertEquals("CLUB_CREATE", rows.getString(3));
                assertFalse(rows.next());
            }
        }

        Object ownerList = dispatcher.dispatch(owner, frame("list-1", 2, "club.CGetClubListMin", Map.of()));
        assertInstanceOf(List.class, ownerList);
        assertEquals(clubId, ((Number) ((Map<?, ?>) ((List<?>) ownerList).get(0)).get("id")).longValue());
        try (Connection c = source.getConnection();
             var q = c.prepareStatement("SELECT member_status,member_role FROM aoo_club_member WHERE club_id=? AND player_id=?")) {
            q.setLong(1, clubId);
            q.setLong(2, 700);
            try (var rows = q.executeQuery()) {
                assertTrue(rows.next());
                assertEquals("ACTIVE", rows.getString(1));
                assertEquals("OWNER", rows.getString(2));
            }
        }

        ConnectionIdentity applicant = new ConnectionIdentity(701, "applicant-device", "https://game.example");
        Object join = dispatcher.dispatch(applicant, frame("join-1", 1, "club.CClubJoin",
                Map.of("clubSign", clubId)));
        assertEquals(16, ((Number) ((Map<?, ?>) join).get("joinStatus")).intValue());
        Object pending = dispatcher.dispatch(owner, frame("pending-1", 3, "club.CClubGetMemberManage",
                Map.of("clubId", clubId, "pageType", 1, "pageNum", 1)));
        assertInstanceOf(List.class, pending);
        assertEquals(701L, ((Number) ((Map<?, ?>) ((List<?>) pending).get(0)).get("pid")).longValue());

        dispatcher.dispatch(owner, frame("approve-1", 4, "club.CClubChangePlayerStatus",
                Map.of("clubId", clubId, "pid", 701, "status", 4)));
        Object applicantList = dispatcher.dispatch(applicant, frame("list-2", 2, "club.CGetClubListMin", Map.of()));
        assertInstanceOf(List.class, applicantList);
        assertFalse(((List<?>) applicantList).isEmpty());
        try (Connection c = source.getConnection();
             var q = c.prepareStatement("SELECT member_status,member_role FROM aoo_club_member WHERE club_id=? AND player_id=?")) {
            q.setLong(1, clubId);
            q.setLong(2, 701);
            try (var rows = q.executeQuery()) {
                assertTrue(rows.next());
                assertEquals("ACTIVE", rows.getString(1));
                assertEquals("MEMBER", rows.getString(2));
            }
        }
    }

    @Test void unionCreateAndJoinPersistOnClubState() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:union_dispatch;MODE=MySQL;DB_CLOSE_DELAY=-1");
        source.setUser("sa");
        schema(source);
        Clock clock = Clock.fixed(Instant.parse("2026-09-01T01:00:00Z"), ZoneOffset.UTC);
        JdbcHallWebSocketDispatcher dispatcher = new JdbcHallWebSocketDispatcher(
                source, new ObjectMapper().findAndRegisterModules(), clock);

        ConnectionIdentity owner = new ConnectionIdentity(800, "owner-device", "https://game.example");
        ConnectionIdentity applicant = new ConnectionIdentity(801, "applicant-device", "https://game.example");
        try (Connection c = source.getConnection(); var s = c.createStatement()) {
            s.execute("INSERT INTO aoo_currency_balance(player_id,currency,currency_scope_id,balance,version,updated_at) VALUES"
                    + "(800,'CRYSTAL',0,10000,0,CURRENT_TIMESTAMP(3)),"
                    + "(801,'CRYSTAL',0,10000,0,CURRENT_TIMESTAMP(3))");
        }

        Object ownerClub = dispatcher.dispatch(owner, frame("owner-club", 1, "club.CClubCreate",
                Map.of("clubName", "Owner Circle")));
        Object applicantClub = dispatcher.dispatch(applicant, frame("applicant-club", 2, "club.CClubCreate",
                Map.of("clubName", "Applicant Circle")));
        long ownerClubId = ((Number) ((Map<?, ?>) ownerClub).get("id")).longValue();
        long applicantClubId = ((Number) ((Map<?, ?>) applicantClub).get("id")).longValue();

        Object createdUnion = dispatcher.dispatch(owner, frame("union-create", 3, "union.CUnionCreate",
                Map.of("clubId", ownerClubId, "unionName", "Entry Union", "join", 1, "quit", 0,
                        "initSports", 2000, "matchRate", 0, "outSports", 0, "prizeType", 2,
                        "ranking", 0, "value", 0)));
        assertInstanceOf(Map.class, createdUnion);
        long unionId = ((Number) ((Map<?, ?>) createdUnion).get("unionId")).longValue();
        long unionSign = ((Number) ((Map<?, ?>) createdUnion).get("unionSign")).longValue();
        assertTrue(unionId > 0);
        assertEquals(unionId, unionSign);

        Object ownerDetail = dispatcher.dispatch(owner, frame("owner-detail", 4, "club.CGetClubListById",
                Map.of("clubId", ownerClubId)));
        assertEquals(unionId, ((Number) ((Map<?, ?>) ownerDetail).get("unionId")).longValue());
        assertEquals(3, ((Number) ((Map<?, ?>) ownerDetail).get("unionPostType")).intValue());

        Object join = dispatcher.dispatch(applicant, frame("union-join", 5, "union.CUnionJoin",
                Map.of("clubId", applicantClubId, "unionSign", unionSign)));
        assertEquals(0, ((Number) join).intValue());
        Object applicantDetail = dispatcher.dispatch(applicant, frame("applicant-detail", 6, "club.CGetClubListById",
                Map.of("clubId", applicantClubId)));
        assertEquals(unionId, ((Number) ((Map<?, ?>) applicantDetail).get("unionId")).longValue());
        assertEquals(1, ((Number) ((Map<?, ?>) applicantDetail).get("unionPostType")).intValue());

        try (Connection c = source.getConnection();
             var q = c.prepareStatement("SELECT state_json FROM aoo_club_state WHERE club_id=?")) {
            q.setLong(1, ownerClubId);
            try (var rows = q.executeQuery()) {
                assertTrue(rows.next());
                String stateJson = rows.getString(1);
                assertTrue(stateJson.contains("\"unionMembers\""));
                assertTrue(stateJson.contains(Long.toString(applicantClubId)));
            }
        }
    }

    private static WebSocketFrame frame(String requestId, long seq, String action, Map<String, Object> payload) {
        return new WebSocketFrame("club.dispatch", requestId, seq, "", 0, "",
                1_787_936_400_000L + seq, Map.of("action", action, "payload", payload));
    }

    private static void schema(JdbcDataSource source) throws Exception {
        try (Connection c = source.getConnection(); var s = c.createStatement()) {
            s.execute("CREATE TABLE aoo_club_state(club_id BIGINT PRIMARY KEY,state_json CLOB NOT NULL,row_version BIGINT NOT NULL,updated_at TIMESTAMP NOT NULL)");
            s.execute("CREATE TABLE aoo_club_write_idempotency(scope_key VARCHAR(160) PRIMARY KEY,response_json CLOB NOT NULL,created_at TIMESTAMP NOT NULL)");
            s.execute("CREATE TABLE aoo_club_member(club_id BIGINT NOT NULL,player_id BIGINT NOT NULL,member_status VARCHAR(16) NOT NULL,member_role VARCHAR(16) NOT NULL,online TINYINT NOT NULL DEFAULT 0,profile_payload CLOB NOT NULL,joined_at TIMESTAMP NOT NULL,updated_at TIMESTAMP NOT NULL,PRIMARY KEY(club_id,player_id))");
            s.execute("CREATE TABLE aoo_currency_catalog(currency_code VARCHAR(32) PRIMARY KEY,scope_type VARCHAR(16) NOT NULL,status VARCHAR(16) NOT NULL)");
            s.execute("INSERT INTO aoo_currency_catalog(currency_code,scope_type,status) VALUES('CRYSTAL','GLOBAL','ACTIVE')");
            s.execute("CREATE TABLE aoo_currency_balance(player_id BIGINT NOT NULL,currency VARCHAR(32) NOT NULL,currency_scope_id BIGINT NOT NULL DEFAULT 0,balance BIGINT NOT NULL DEFAULT 0,version BIGINT NOT NULL DEFAULT 0,updated_at TIMESTAMP NOT NULL,PRIMARY KEY(player_id,currency,currency_scope_id))");
            s.execute("CREATE TABLE aoo_ledger(ledger_id BIGINT PRIMARY KEY,business_id VARCHAR(128) UNIQUE,player_id BIGINT NOT NULL,currency VARCHAR(32) NOT NULL,currency_scope_id BIGINT NOT NULL DEFAULT 0,delta BIGINT NOT NULL,balance_after BIGINT NOT NULL,reason_code VARCHAR(64) NOT NULL,entry_status VARCHAR(16) NOT NULL,channel_code VARCHAR(32) NOT NULL,created_at TIMESTAMP NOT NULL)");
        }
    }
}
