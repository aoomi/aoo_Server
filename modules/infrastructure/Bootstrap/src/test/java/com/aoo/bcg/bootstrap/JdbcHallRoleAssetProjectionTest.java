package com.aoo.bcg.bootstrap;

import com.aoo.bcg.gateway.ConnectionIdentity;
import com.aoo.bcg.gateway.WebSocketFrame;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

final class JdbcHallRoleAssetProjectionTest {
    @Test void roleProjectsIndependentRoomCardAndGoldWithoutLegacyCurrency() throws Exception {
        JdbcDataSource source = source(true);
        balance(source, "ROOM_CARD", 7);
        balance(source, "GOLD", 11);
        balance(source, "CRYSTAL", 99); // Deliberate legacy negative fixture.
        Map<?, ?> role = profile(source);
        assertEquals(7L, role.get("roomCard"));
        assertEquals(11L, role.get("gold"));
        assertFalse(role.containsKey("diamond"));
        assertFalse(role.containsKey("crystal"));
    }
    @Test void missingRoomCardRemainsZeroWhenGoldExists() throws Exception {
        JdbcDataSource source = source(true);
        balance(source, "GOLD", 11);
        Map<?, ?> role = profile(source);
        assertEquals(0L, role.get("roomCard"));
        assertEquals(11L, role.get("gold"));
        assertFalse(role.containsKey("diamond"));
    }
    @Test void missingAccountStillRejectsAuthenticatedProfile() throws Exception {
        assertThrows(SecurityException.class, () -> profile(source(false)));
    }
    private static JdbcDataSource source(boolean account) throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:role-assets-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        try (Connection connection = source.getConnection(); var s = connection.createStatement()) {
            s.execute("CREATE TABLE aoo_account(account_id BIGINT PRIMARY KEY, guest BOOLEAN NOT NULL)");
            s.execute("CREATE TABLE player_profile(player_id BIGINT PRIMARY KEY,nickname VARCHAR(64),gender_code VARCHAR(16))");
            s.execute("CREATE TABLE aoo_account_identity(account_id BIGINT,identity_type VARCHAR(32),status VARCHAR(16),normalized_value VARCHAR(128))");
            s.execute("CREATE TABLE aoo_currency_balance(player_id BIGINT,currency VARCHAR(32),currency_scope_id BIGINT,balance BIGINT)");
            if (account) {
                s.execute("INSERT INTO aoo_account VALUES(901,FALSE)");
                s.execute("INSERT INTO player_profile VALUES(901,'fixture','MALE')");
                s.execute("INSERT INTO aoo_account_identity VALUES(901,'DISPLAY_ID','ACTIVE','901')");
            }
        }
        return source;
    }
    private static void balance(JdbcDataSource source,String currency,long value) throws Exception {
        try (Connection c=source.getConnection();var q=c.prepareStatement("INSERT INTO aoo_currency_balance VALUES(901,?,0,?)")) {
            q.setString(1,currency);q.setLong(2,value);q.executeUpdate();
        }
    }
    private static Map<?, ?> profile(JdbcDataSource source) {
        JdbcHallWebSocketDispatcher dispatcher = new JdbcHallWebSocketDispatcher(source,new ObjectMapper(),Clock.systemUTC());
        return (Map<?, ?>)dispatcher.dispatch(new ConnectionIdentity(901,"fixture","https://fixture.invalid","fixture"),
            new WebSocketFrame("account.session_dispatch","fixture",1,"",0,"",1L,
                Map.of("action","base.C1006RoleLogin","payload",Map.of())));
    }
}
