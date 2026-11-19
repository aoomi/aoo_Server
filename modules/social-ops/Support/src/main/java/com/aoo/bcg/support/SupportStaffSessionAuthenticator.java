package com.aoo.bcg.support;

import com.sun.net.httpserver.HttpExchange;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Objects;

/** Reads existing staff session facts; never accepts a caller-selected staff identity. */
public final class SupportStaffSessionAuthenticator {
    private final DataSource source;
    private final Clock clock;
    public SupportStaffSessionAuthenticator(DataSource source, Clock clock) {
        this.source=Objects.requireNonNull(source); this.clock=Objects.requireNonNull(clock);
    }
    public long authenticate(HttpExchange exchange, String permission) {
        if (!permission.equals("support.case.read") && !permission.equals("support.case.handle"))
            throw new IllegalArgumentException("unsupported support permission");
        if (exchange.getRequestHeaders().containsKey("Authorization")
                || exchange.getRequestHeaders().containsKey("X-Support-Agent-Id"))
            throw new SecurityException("caller-selected staff credentials forbidden");
        String token=null;
        for (String header:exchange.getRequestHeaders().getOrDefault("Cookie", java.util.List.of())) {
            for (String part:header.split(";")) {
                String[] pair=part.strip().split("=",2);
                if (pair[0].equals("aoo_admin_session")) {
                    if (token!=null || pair.length!=2) throw new SecurityException("ambiguous staff session");
                    token=pair[1];
                }
            }
        }
        var csrfValues=exchange.getRequestHeaders().get("X-CSRF-Token");
        String csrf=csrfValues!=null && csrfValues.size()==1 ? csrfValues.getFirst() : null;
        if (!validToken(token) || !validToken(csrf)) throw new SecurityException("staff session and csrf required");
        String sessionSql="SELECT s.operator_id,s.csrf_hash FROM admin_browser_session s "
                +"JOIN admin_operator_account a ON a.operator_id=s.operator_id AND a.enabled=1 "
                +"WHERE s.session_hash=? AND s.revoked_at IS NULL AND s.expires_at>?";
        String permissionSql="SELECT 1 FROM admin_operator_permission WHERE operator_id=? AND permission_code=? AND enabled=1 "
                +"UNION ALL SELECT 1 FROM admin_operator_role ar JOIN admin_role r ON r.role_code=ar.role_code AND r.enabled=1 "
                +"JOIN admin_role_permission rp ON rp.role_code=ar.role_code "
                +"WHERE ar.operator_id=? AND ar.enabled=1 AND rp.permission_code=? LIMIT 1";
        try (var c=source.getConnection()) {
            c.setReadOnly(true);
            long actor;
            try (var q=c.prepareStatement(sessionSql)) {
                q.setString(1,hash(token)); q.setTimestamp(2,Timestamp.from(clock.instant()));
                try (var r=q.executeQuery()) {
                    if (!r.next() || !MessageDigest.isEqual(hash(csrf).getBytes(StandardCharsets.US_ASCII),
                            r.getString(2).getBytes(StandardCharsets.US_ASCII)))
                        throw new SecurityException("invalid staff session or csrf");
                    actor=r.getLong(1);
                    if (actor<=0 || r.next()) throw new SecurityException("invalid staff identity");
                }
            }
            try (var q=c.prepareStatement(permissionSql)) {
                q.setLong(1,actor); q.setString(2,permission); q.setLong(3,actor); q.setString(4,permission);
                try (var r=q.executeQuery()) { if (!r.next()) throw new SecurityException("support permission required"); }
            }
            return actor;
        } catch (SecurityException e) { throw e; }
        catch (Exception e) { throw new IllegalStateException("staff authority unavailable",e); }
    }
    private static boolean validToken(String value) { return value!=null && value.matches("[A-Za-z0-9_-]{43}"); }
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException("staff credential hashing unavailable",e); }
    }
}
