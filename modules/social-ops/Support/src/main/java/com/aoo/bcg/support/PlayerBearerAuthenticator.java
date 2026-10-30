package com.aoo.bcg.support;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;

/** Verifies the same short-lived account assertion contract used by player-facing services. */
public final class PlayerBearerAuthenticator {
    private final byte[] secret; private final Clock clock;
    public PlayerBearerAuthenticator(String secret,Clock clock){if(secret==null||secret.length()<32)throw new IllegalArgumentException("support auth secret must contain at least 32 characters");this.secret=secret.getBytes(StandardCharsets.UTF_8);this.clock=clock;}
    public long authenticate(String authorization){try{if(authorization==null||!authorization.startsWith("Bearer "))throw new SecurityException("valid player bearer required");String[] p=authorization.substring(7).split("\\.",-1);if(p.length!=3)throw new SecurityException("valid player bearer required");long id=Long.parseLong(p[0]),expiry=Long.parseLong(p[1]),now=clock.instant().getEpochSecond();if(id<=0||expiry<now||expiry>now+300)throw new SecurityException("valid player bearer required");Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret,"HmacSHA256"));if(!MessageDigest.isEqual(mac.doFinal((p[0]+"."+p[1]).getBytes(StandardCharsets.UTF_8)),HexFormat.of().parseHex(p[2])))throw new SecurityException("valid player bearer required");return id;}catch(SecurityException e){throw e;}catch(Exception e){throw new SecurityException("valid player bearer required");}}
}
