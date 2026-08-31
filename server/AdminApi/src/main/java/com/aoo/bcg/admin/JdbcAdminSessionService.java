package com.aoo.bcg.admin;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

/** Database-backed browser authentication. Raw session and CSRF tokens are never persisted. */
public final class JdbcAdminSessionService {
    public record Login(long operatorId, String userName, String sessionToken, String csrfToken,
            List<String> roles, List<String> permissions) { }
    public record Session(long operatorId, String csrfHash, Instant expiresAt) { }

    private static final Duration TTL = Duration.ofHours(8);
    private final JdbcAdminResourceRepository.ConnectionFactory connections;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public JdbcAdminSessionService(JdbcAdminResourceRepository.ConnectionFactory connections, Clock clock) {
        this.connections = Objects.requireNonNull(connections);
        this.clock = Objects.requireNonNull(clock);
    }

    public Login login(String username, char[] password) {
        if (username == null || !username.matches("[A-Za-z0-9_.@-]{3,64}") || password == null) return null;
        String sql = "SELECT operator_id,username,credential_verifier FROM admin_operator_account WHERE username=? AND enabled=1";
        try (Connection connection = connections.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next() || !verify(password, row.getString("credential_verifier"))) return null;
                long operatorId = row.getLong("operator_id");
                String token = randomToken(), csrf = randomToken();
                try (PreparedStatement insert = connection.prepareStatement("INSERT INTO admin_browser_session(session_hash,operator_id,csrf_hash,created_at,expires_at,last_seen_at) VALUES(?,?,?,?,?,?)")) {
                    Instant now = clock.instant();
                    insert.setString(1, sha256(token)); insert.setLong(2, operatorId);
                    insert.setString(3, sha256(csrf)); insert.setObject(4, now); insert.setObject(5, now.plus(TTL)); insert.setObject(6, now);
                    insert.executeUpdate();
                }
                return new Login(operatorId, username, token, csrf, roles(connection, operatorId), permissions(connection, operatorId));
            }
        } catch (Exception error) { throw new IllegalStateException("cannot authenticate administrator", error); }
        finally { java.util.Arrays.fill(password, '\0'); }
    }

    public Session authenticate(String token) {
        if (token == null || token.length() < 32) return null;
        String sql = "SELECT operator_id,csrf_hash,expires_at FROM admin_browser_session WHERE session_hash=? AND revoked_at IS NULL AND expires_at>?";
        try (Connection connection = connections.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sha256(token)); statement.setObject(2, clock.instant());
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) return null;
                return new Session(row.getLong(1), row.getString(2), row.getTimestamp(3).toInstant());
            }
        } catch (Exception error) { throw new IllegalStateException("cannot validate admin session", error); }
    }

    public void logout(String token) {
        if (token == null) return;
        try (Connection connection = connections.open(); PreparedStatement statement = connection.prepareStatement("UPDATE admin_browser_session SET revoked_at=? WHERE session_hash=? AND revoked_at IS NULL")) {
            statement.setObject(1, clock.instant()); statement.setString(2, sha256(token)); statement.executeUpdate();
        } catch (Exception error) { throw new IllegalStateException("cannot revoke admin session", error); }
    }

    public boolean csrfMatches(Session session, String csrf) {
        return csrf != null && MessageDigest.isEqual(session.csrfHash().getBytes(StandardCharsets.US_ASCII), sha256(csrf).getBytes(StandardCharsets.US_ASCII));
    }

    private List<String> roles(Connection connection, long id) throws Exception {
        try (PreparedStatement s=connection.prepareStatement("SELECT role_code FROM admin_operator_role WHERE operator_id=? AND enabled=1 ORDER BY role_code")){s.setLong(1,id);try(ResultSet r=s.executeQuery()){var out=new java.util.ArrayList<String>();while(r.next())out.add(r.getString(1));return List.copyOf(out);}}
    }
    private List<String> permissions(Connection connection, long id) throws Exception {
        String sql="SELECT permission_code FROM admin_operator_permission WHERE operator_id=? AND enabled=1 UNION SELECT rp.permission_code FROM admin_operator_role ar JOIN admin_role r ON r.role_code=ar.role_code AND r.enabled=1 JOIN admin_role_permission rp ON rp.role_code=ar.role_code WHERE ar.operator_id=? AND ar.enabled=1 ORDER BY permission_code";
        try (PreparedStatement s=connection.prepareStatement(sql)){s.setLong(1,id);s.setLong(2,id);try(ResultSet r=s.executeQuery()){var out=new java.util.ArrayList<String>();while(r.next())out.add(r.getString(1));return List.copyOf(out);}}
    }
    private boolean verify(char[] password, String encoded) {
        try {
            String[] p=encoded.split(":",3); int iterations=Integer.parseInt(p[0]);
            if(iterations<210_000||iterations>2_000_000)return false;
            byte[] salt=Base64.getDecoder().decode(p[1]), expected=Base64.getDecoder().decode(p[2]);
            PBEKeySpec spec=new PBEKeySpec(password,salt,iterations,expected.length*8);
            byte[] actual=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            spec.clearPassword(); return MessageDigest.isEqual(expected,actual);
        } catch(Exception ignored){return false;}
    }
    private String randomToken(){byte[] bytes=new byte[32];random.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    private String sha256(String value){try{return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
