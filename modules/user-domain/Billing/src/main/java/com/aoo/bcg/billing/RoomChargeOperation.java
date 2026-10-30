package com.aoo.bcg.billing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;

/** Internal server command. Monetary values and asset identity come from the published policy. */
public record RoomChargeOperation(long roomId, String roomAttempt, String unitId, long actorId,
        long payerId, long gameId, String playVersion, String regionCode, Kind kind,
        Context context, Payer payerMode, String measureKind, int measureValue,
        int playerCount, long carryMinor) {
    public enum Kind { FIXED, LOOP, CARRY }
    public enum Context { HALL, CLUB, UNION }
    public enum Payer { OWNER, AA, CLUB, UNION }
    public RoomChargeOperation {
        if (roomId<=0 || actorId<=0 || payerId<=0 || gameId<=0 || measureValue<=0 || playerCount<=0)
            throw new IllegalArgumentException("invalid billing identity or dimension");
        requireToken(roomAttempt,48); requireToken(unitId,48);
        requireToken(playVersion,64); requireToken(regionCode,32);
        Objects.requireNonNull(kind); Objects.requireNonNull(context); Objects.requireNonNull(payerMode);
        if (context==Context.HALL && payerMode!=Payer.OWNER && payerMode!=Payer.AA
                || context==Context.CLUB && payerMode!=Payer.CLUB
                || context==Context.UNION && payerMode!=Payer.UNION)
            throw new IllegalArgumentException("payer context mismatch");
        if (context==Context.HALL && payerId!=actorId)
            throw new IllegalArgumentException("hall payer must be the charging participant");
        if (kind==Kind.CARRY) {
            if (payerMode==Payer.AA || carryMinor<=0 || !"CARRY_MINOR_PER_TIER".equals(measureKind))
                throw new IllegalArgumentException("invalid carry policy dimension");
        } else if (carryMinor!=0 || !("ROUND_COUNT".equals(measureKind)||"DURATION_MINUTES".equals(measureKind))) {
            throw new IllegalArgumentException("invalid fixed or loop dimension");
        }
    }
    public String operationKey() { return hash(roomId,roomAttempt,unitId,kind,payerId); }
    public String requestId() { return "rc:"+operationKey(); }
    public String commandHash() { return hash(roomId,roomAttempt,unitId,actorId,payerId,gameId,
            playVersion,regionCode,kind,context,payerMode,measureKind,measureValue,playerCount,carryMinor); }
    static void requireToken(String value,int maximum) {
        if(value==null || !value.matches("[A-Za-z0-9_.:-]{1,"+maximum+"}"))
            throw new IllegalArgumentException("invalid stable billing token");
    }
    static String hash(Object... values) {
        try {
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            for(Object value:values) {
                byte[] bytes=Objects.requireNonNull(value).toString().getBytes(StandardCharsets.UTF_8);
                digest.update((bytes.length+":").getBytes(StandardCharsets.US_ASCII)); digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch(java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    static String json(String value) {
        if(value==null)return "null";
        StringBuilder out=new StringBuilder("\"");
        for(char c:value.toCharArray()) {
            if(c=='"'||c=='\\')out.append('\\').append(c);
            else if(c<32)out.append(String.format(java.util.Locale.ROOT,"\\u%04x",(int)c));
            else out.append(c);
        }
        return out.append('"').toString();
    }
}
