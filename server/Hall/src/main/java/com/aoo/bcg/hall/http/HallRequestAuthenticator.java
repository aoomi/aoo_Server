package com.aoo.bcg.hall.http;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import javax.sql.DataSource;
import java.sql.PreparedStatement;

/** Verifies the account service's short-lived HMAC bearer assertion. */
public final class HallRequestAuthenticator {
    private final byte[] secret; private final Clock clock; private final DataSource sessions;
    public HallRequestAuthenticator(String secret,Clock clock){
        if(secret==null||secret.length()<32)throw new IllegalArgumentException("hall auth secret must contain at least 32 characters");
        this.secret=secret.getBytes(StandardCharsets.UTF_8);this.clock=clock;this.sessions=null;
    }
    public HallRequestAuthenticator(DataSource sessions,Clock clock){this.secret=null;this.sessions=sessions;this.clock=clock;}
    public long authenticate(String authorization){try{
        if(authorization==null||!authorization.startsWith("Bearer "))throw rejected();
        if(sessions!=null)return authenticateSession(authorization.substring(7));
        String[] p=authorization.substring(7).split("\\.",-1);if(p.length!=3)throw rejected();
        long account=Long.parseLong(p[0]),expires=Long.parseLong(p[1]);
        if(account<=0||expires<clock.instant().getEpochSecond()||expires>clock.instant().plusSeconds(300).getEpochSecond())throw rejected();
        byte[] supplied=HexFormat.of().parseHex(p[2]);byte[] expected=sign(p[0]+"."+p[1]);
        if(!MessageDigest.isEqual(expected,supplied))throw rejected();return account;
    }catch(HallError e){throw e;}catch(Exception e){throw rejected();}}
    private long authenticateSession(String token)throws Exception{String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));try(var c=sessions.getConnection();PreparedStatement q=c.prepareStatement("SELECT s.account_id FROM aoo_account_session s JOIN aoo_account a ON a.account_id=s.account_id WHERE s.access_hash=? AND s.revoked_at IS NULL AND s.access_expires_at>CURRENT_TIMESTAMP(3) AND s.auth_generation=a.auth_generation AND (a.banned_until IS NULL OR a.banned_until<=CURRENT_TIMESTAMP(3))")){q.setString(1,hash);try(var r=q.executeQuery()){if(!r.next())throw rejected();return r.getLong(1);}}}
    byte[] sign(String value)throws Exception{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret,"HmacSHA256"));return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));}
    private static HallError rejected(){return new HallError(401,"HALL_UNAUTHORIZED","valid account bearer assertion required");}
}
