package com.aoo.bcg.bootstrap;

import com.aoo.bcg.gateway.ConnectionIdentity;
import com.aoo.bcg.gateway.WebSocketFrame;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class WaitingRoomFullProjectionTest {
    @Test void currentRoomReturnsAuthoritativeCapacityForEveryWaitingMember() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:waiting-room-full;MODE=MySQL;DB_CLOSE_DELAY=-1");
        try (Connection connection = source.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE aoo_game_catalog (game_id INT PRIMARY KEY, game_code VARCHAR(64) NOT NULL)");
            statement.execute("CREATE TABLE aoo_hall_room (room_id BIGINT PRIMARY KEY, game_id INT NOT NULL, play_version VARCHAR(64) NOT NULL, state VARCHAR(24) NOT NULL, route_endpoint VARCHAR(255), club_id BIGINT NOT NULL, rules_json CLOB NOT NULL, updated_at TIMESTAMP NOT NULL)");
            statement.execute("CREATE TABLE aoo_hall_room_member (room_id BIGINT NOT NULL, account_id BIGINT NOT NULL, status VARCHAR(16) NOT NULL)");
            statement.execute("INSERT INTO aoo_game_catalog VALUES (2003,'liangshan2003')");
            statement.execute("INSERT INTO aoo_hall_room VALUES (880001,2003,'liangshan2003-v1','OPEN','ws://room',77,'{\"playerCount\":3}',CURRENT_TIMESTAMP)");
            statement.execute("INSERT INTO aoo_hall_room_member VALUES (880001,901,'JOINED'),(880001,902,'JOINED'),(880001,903,'JOINED')");
        }

        JdbcHallWebSocketDispatcher dispatcher = new JdbcHallWebSocketDispatcher(
                source, new ObjectMapper(), Clock.fixed(Instant.parse("2026-10-06T13:25:00Z"), ZoneOffset.UTC));
        Object response = dispatcher.dispatch(
                new ConnectionIdentity(901, "device-901", "https://game.example", "page-901"),
                new WebSocketFrame("hall.dispatch", "room-projection", 1, "", 0, "", 1_791_291_900_000L,
                        Map.of("action", "game.C1101GetRoomID", "payload", Map.of())));

        @SuppressWarnings("unchecked")
        Map<String, Object> room = (Map<String, Object>) response;
        assertEquals(880001L, room.get("roomId"));
        assertEquals("liangshan2003", room.get("gameCode"));
        assertEquals(3, room.get("playerNum"));
        assertEquals(3, room.get("occupiedCount"));
        assertEquals(true, room.get("waitingFull"));
    }
}
