package com.ddm.server.protocol.v2;

import com.ddm.server.common.redis.RedisUtil;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public final class WsTicketVerifier {
    private WsTicketVerifier() {}

    public static long verifyAndConsume(String ticket) {
        if (ticket == null || ticket.isBlank()) throw new IllegalArgumentException("wsTicket is required");
        try {
            String[] parts = ticket.split("\\.", 2);
            if (parts.length != 2 || !MessageDigest.isEqual(sign(parts[0]), decode(parts[1]))) {
                throw new IllegalArgumentException("Invalid wsTicket");
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String[] values = payload.split(":", 3);
            if (values.length != 3 || values[2].isBlank()) throw new IllegalArgumentException("Invalid wsTicket");
            long accountId = Long.parseLong(values[0]);
            long expires = Long.parseLong(values[1]);
            long now = System.currentTimeMillis() / 1000L;
            if (accountId <= 0L || expires < now || expires - now > 30L) throw new IllegalArgumentException("Expired wsTicket");
            if (!RedisUtil.setNxEx("protocol:ws-ticket:used:" + values[2], Long.toString(accountId), 35)) {
                throw new IllegalArgumentException("wsTicket already used");
            }
            return accountId;
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid wsTicket", e);
        }
    }

    private static byte[] sign(String value) {
        try {
            String secret = System.getProperty("WsTicketSecret");
            if (secret == null || secret.isBlank()) secret = System.getenv("WS_TICKET_SECRET");
            if (secret == null || secret.length() < 32) throw new IllegalStateException("WsTicketSecret must contain at least 32 characters");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (RuntimeException e) { throw e; }
        catch (Exception e) { throw new IllegalStateException("Unable to verify wsTicket", e); }
    }

    private static byte[] decode(String signature) {
        try { return Base64.getUrlDecoder().decode(signature); }
        catch (IllegalArgumentException e) { return new byte[0]; }
    }
}
