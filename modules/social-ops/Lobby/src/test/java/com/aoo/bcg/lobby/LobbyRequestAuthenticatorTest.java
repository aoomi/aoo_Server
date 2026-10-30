package com.aoo.bcg.lobby;

import com.aoo.bcg.hall.http.HallError;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.h2.jdbcx.JdbcDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LobbyRequestAuthenticatorTest {
    static JdbcDataSource sessionDatabase() throws Exception {
        JdbcDataSource db = new JdbcDataSource();
        db.setURL("jdbc:h2:mem:lobby_session_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        try (var c = db.getConnection(); var s = c.createStatement()) {
            s.execute("CREATE TABLE aoo_account(account_id BIGINT,auth_generation BIGINT,banned_until TIMESTAMP)");
            s.execute("CREATE TABLE aoo_account_session(session_id VARCHAR(128),account_id BIGINT,device_id VARCHAR(128),access_hash VARCHAR(64),revoked_at TIMESTAMP,access_expires_at TIMESTAMP,auth_generation BIGINT)");
            s.execute("INSERT INTO aoo_account VALUES(42,1,NULL)");
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest("unit-access".getBytes(StandardCharsets.UTF_8)));
            s.execute("INSERT INTO aoo_account_session VALUES('account-session-42',42,'device-42','" + hash + "',NULL,DATEADD('DAY',1,CURRENT_TIMESTAMP),1)");
        }
        return db;
    }
    @Test void realPrincipalBindsSameBearerDeviceAndKeepsLegacyAccountApi() throws Exception {
        var db = sessionDatabase(); var auth = new LobbyRequestAuthenticator(db, fixedClock());
        assertEquals(new LobbyRequestAuthenticator.Principal(42,"account-session-42"),auth.authenticatePrincipal("Bearer unit-access","device-42"));
        assertEquals(42, auth.authenticate("Bearer unit-access"));
        assertEquals(401, assertThrows(HallError.class,()->auth.authenticatePrincipal("Bearer unit-access","wrong")).status());
        assertThrows(HallError.class,()->auth.authenticatePrincipal("Bearer unit-access",null));
        assertThrows(HallError.class,()->new LobbyRequestAuthenticator(SECRET,fixedClock()).authenticatePrincipal("Bearer unit-access","device-42"));
    }
    @Test void principalRejectsRevocationExpiryGenerationBanAndMissingSession() throws Exception {
        for(String update : new String[]{"UPDATE aoo_account_session SET revoked_at=CURRENT_TIMESTAMP",
                "UPDATE aoo_account_session SET access_expires_at=DATEADD('DAY',-1,CURRENT_TIMESTAMP)",
                "UPDATE aoo_account_session SET auth_generation=2",
                "UPDATE aoo_account SET banned_until=DATEADD('DAY',1,CURRENT_TIMESTAMP)",
                "UPDATE aoo_account_session SET session_id=NULL"}) {
            var db=sessionDatabase();try(var c=db.getConnection();var s=c.createStatement()){s.execute(update);}
            assertEquals(401,assertThrows(HallError.class,()->new LobbyRequestAuthenticator(db,fixedClock()).authenticatePrincipal("Bearer unit-access","device-42")).status());
        }
    }
    private static final String SECRET = "01234567890123456789012345678901";
    private static final Instant NOW = Instant.parse("2026-08-24T00:00:00Z");

    @Test
    void canonicalAuthenticatorAcceptsSignedShortLivedAssertion() throws Exception {
        var authenticator = new LobbyRequestAuthenticator(SECRET, fixedClock());
        String payload = "42." + NOW.plusSeconds(30).getEpochSecond();
        String token = payload + "." + HexFormat.of().formatHex(authenticator.sign(payload));

        assertEquals(42L, authenticator.authenticate("Bearer " + token));
    }

    @Test
    void canonicalAuthenticatorRejectsExpiredOrMalformedAssertion() {
        var authenticator = new LobbyRequestAuthenticator(SECRET, fixedClock());

        assertThrows(HallError.class, () -> authenticator.authenticate(
                "Bearer 42." + NOW.minusSeconds(1).getEpochSecond() + ".00"));
        assertThrows(HallError.class, () -> authenticator.authenticate("Bearer malformed"));
    }

    @Test
    void HallCompatibilityFacadeDelegatesToCanonicalAuthenticator() throws Exception {
        var authenticator = new com.aoo.bcg.hall.http.HallRequestAuthenticator(SECRET, fixedClock());
        String payload = "84." + NOW.plusSeconds(30).getEpochSecond();
        String token = payload + "." + HexFormat.of().formatHex(authenticator.sign(payload));

        assertEquals(84L, authenticator.authenticate("Bearer " + token));
    }

    private static Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }
}
