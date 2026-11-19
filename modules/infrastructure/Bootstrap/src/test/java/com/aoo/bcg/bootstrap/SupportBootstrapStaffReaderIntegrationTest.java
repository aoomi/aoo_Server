package com.aoo.bcg.bootstrap;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import static org.junit.jupiter.api.Assertions.*;

/** Synthetic composition tests; listener case requires a separate explicit resource contract. */
class SupportBootstrapStaffReaderIntegrationTest {
    private static final String[] KEYS = {"support.database.url", "support.database.user", "support.database.password",
            "support.auth.hmac", "support.handoff.token", "support.http.port", "support.staff.database.url",
            "support.staff.database.user", "support.staff.database.password"};

    @Test void missingStaffConfigurationFailsBeforeCaseDatabaseConnection() throws Exception {
        try (PropertiesScope properties = new PropertiesScope()) {
            properties.base("jdbc:missing-driver:must-not-connect");
            for (String missing : List.of("support.staff.database.url", "support.staff.database.user", "support.staff.database.password")) {
                properties.staff("jdbc:missing-driver:staff-must-not-connect");
                System.clearProperty(missing);
                IllegalStateException failure = assertThrows(IllegalStateException.class, BootstrapAPP::startSupport);
                String expectedEnvironmentKey = missing.toUpperCase(Locale.ROOT).replace('.', '_');
                assertTrue(failure.getMessage().contains(expectedEnvironmentKey),
                        () -> "missing explicit staff setting must win before any JDBC connection; diagnostic=" + failure.getMessage());
            }
        }
    }

    @Test void actualBootstrapUsesStaffAuthoritySeparateFromTheCaseDatabase() throws Exception {
        assertTrue(Boolean.getBoolean("aoo.test.allow.support.wildcard.listener"),
                "explicit resource permission required for existing Bootstrap wildcard port0 listener");
        String caseUrl = "jdbc:h2:mem:support-case-" + UUID.randomUUID() + ";MODE=MySQL";
        String staffUrl = "jdbc:h2:mem:support-staff-" + UUID.randomUUID() + ";MODE=MySQL";
        var caseDS = new JdbcDataSource(); caseDS.setURL(caseUrl); caseDS.setUser("sa");
        var staffDS = new JdbcDataSource(); staffDS.setURL(staffUrl); staffDS.setUser("sa"); staffDS.setPassword("fixture-staff-password");
        String session = "s".repeat(43), csrf = "c".repeat(43);
        try (Connection caseKeeper = caseDS.getConnection(); Connection staffKeeper = staffDS.getConnection();
             PropertiesScope properties = new PropertiesScope()) {
            try (var q = caseKeeper.createStatement()) { q.execute("CREATE TABLE support_case(case_id BIGINT PRIMARY KEY)"); }
            try (var q = staffKeeper.createStatement()) {
                q.execute("CREATE TABLE admin_operator_account(operator_id BIGINT PRIMARY KEY,enabled INT)");
                q.execute("CREATE TABLE admin_browser_session(session_hash VARCHAR(64),operator_id BIGINT,csrf_hash VARCHAR(64),revoked_at TIMESTAMP NULL,expires_at TIMESTAMP)");
                q.execute("CREATE TABLE admin_operator_permission(operator_id BIGINT,permission_code VARCHAR(128),enabled INT)");
                q.execute("CREATE TABLE admin_operator_role(operator_id BIGINT,role_code VARCHAR(64),enabled INT)");
                q.execute("CREATE TABLE admin_role(role_code VARCHAR(64),enabled INT)");
                q.execute("CREATE TABLE admin_role_permission(role_code VARCHAR(64),permission_code VARCHAR(128))");
                q.execute("INSERT INTO admin_operator_account VALUES(71,1)");
                q.execute("INSERT INTO admin_operator_permission VALUES(71,'support.case.read',1)");
            }
            try (var q = staffKeeper.prepareStatement("INSERT INTO admin_browser_session VALUES(?,71,?,NULL,DATEADD('SECOND',60,CURRENT_TIMESTAMP))")) {
                q.setString(1, hash(session)); q.setString(2, hash(csrf)); q.executeUpdate();
            }
            properties.base(caseUrl); properties.staff(staffUrl);
            var server = BootstrapAPP.startSupport();
            try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()) {
                assertTrue(server.getAddress().getAddress().isAnyLocalAddress(), "current production binding is wildcard, not loopback-only");
                URI path = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/internal/v1/support/cases/1/not-an-action");
                assertEquals(403, request(client, path, session, "w".repeat(43)).statusCode());
                // Auth succeeds against staff-only tables, then the invalid action returns 404 before case repository work.
                assertEquals(404, request(client, path, session, csrf).statusCode());
                try (var q = staffKeeper.createStatement()) { q.executeUpdate("UPDATE admin_operator_permission SET enabled=0"); }
                assertEquals(403, request(client, path, session, csrf).statusCode());
                try (var q = caseKeeper.createStatement(); var rows = q.executeQuery("SELECT COUNT(*) FROM support_case")) {
                    assertTrue(rows.next()); assertEquals(0, rows.getInt(1));
                }
                try (var rows = caseKeeper.getMetaData().getTables(null, null, "ADMIN_BROWSER_SESSION", null)) {
                    assertFalse(rows.next(), "case database has no staff authority table");
                }
            } finally {
                server.stop(0);
                if (server.getExecutor() instanceof ExecutorService executor) executor.close();
            }
        }
    }

    private static HttpResponse<String> request(HttpClient client, URI uri, String session, String csrf) throws Exception {
        return client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(3))
                .header("Cookie", "aoo_admin_session=" + session).header("X-CSRF-Token", csrf).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }
    private static String hash(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }
    private static final class PropertiesScope implements AutoCloseable {
        private final Map<String,String> before = new HashMap<>();
        PropertiesScope() { for (String key : KEYS) before.put(key, System.getProperty(key)); }
        void base(String url) {
            System.setProperty("support.database.url", url); System.setProperty("support.database.user", "sa");
            System.setProperty("support.database.password", ""); System.setProperty("support.auth.hmac", "a".repeat(64));
            System.setProperty("support.handoff.token", "h".repeat(64)); System.setProperty("support.http.port", "0");
        }
        void staff(String url) {
            System.setProperty("support.staff.database.url", url); System.setProperty("support.staff.database.user", "sa");
            System.setProperty("support.staff.database.password", "fixture-staff-password");
        }
        @Override public void close() {
            before.forEach((key,value) -> { if (value == null) System.clearProperty(key); else System.setProperty(key,value); });
        }
    }
}
