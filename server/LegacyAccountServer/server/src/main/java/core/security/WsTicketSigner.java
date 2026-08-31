package core.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public final class WsTicketSigner {
    private WsTicketSigner() {}

    public static String issue(long accountId) {
        if (accountId <= 0L) throw new IllegalArgumentException("accountId");
        long expires = System.currentTimeMillis() / 1000L + 30L;
        String payload = accountId + ":" + expires + ":" + UUID.randomUUID();
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encoded + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sign(encoded));
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
        catch (Exception e) { throw new IllegalStateException("Unable to sign wsTicket", e); }
    }
}
