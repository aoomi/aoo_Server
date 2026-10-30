package core.network.http;

import com.ddm.server.common.utils.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RequestSecurityGuard {
    private static final int MAX_BODY_BYTES = 1024 * 1024;
    private static final long MAX_CLOCK_SKEW_SECONDS = 300;
    private static final int CLIENT_REQUESTS_PER_SECOND = 300;
    private final ConcurrentHashMap<String, Long> nonces = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RateWindow> rates = new ConcurrentHashMap<>();
    private final byte[] serverSecret = System.getenv().getOrDefault("aoo_SERVER_PACK_HMAC_SECRET", "")
            .getBytes(StandardCharsets.UTF_8);

    public void verifyClient(HttpServletRequest request, String body) {
        verifyBody(body);
        String ip = IpUtil.getIpAddr(request);
        long second = Instant.now().getEpochSecond();
        RateWindow window = rates.compute(ip, (key, previous) ->
                previous == null || previous.second != second ? new RateWindow(second) : previous);
        if (window.requests.incrementAndGet() > CLIENT_REQUESTS_PER_SECOND) {
            throw new SecurityException("Client request rate exceeded");
        }
        if ((second & 63) == 0) {
            rates.entrySet().removeIf(entry -> entry.getValue().second < second - 2);
        }
    }

    public void verifyServer(HttpServletRequest request, String body) {
        verifyBody(body);
        String remoteAddress = request.getRemoteAddr();
        if (serverSecret.length == 0) {
            if (!request.isSecure() && !("127.0.0.1".equals(remoteAddress) || "::1".equals(remoteAddress))) {
                throw new SecurityException("aoo_SERVER_PACK_HMAC_SECRET is required for remote server requests");
            }
            return;
        }

        String timestamp = requiredHeader(request, "X-aoo-Timestamp");
        String nonce = requiredHeader(request, "X-aoo-Nonce");
        String signature = requiredHeader(request, "X-aoo-Signature");
        long requestSecond;
        try {
            requestSecond = Long.parseLong(timestamp);
        } catch (NumberFormatException exception) {
            throw new SecurityException("Invalid request timestamp", exception);
        }
        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - requestSecond) > MAX_CLOCK_SKEW_SECONDS) {
            throw new SecurityException("Expired server request");
        }
        nonces.entrySet().removeIf(entry -> entry.getValue() < now - MAX_CLOCK_SKEW_SECONDS);
        if (nonces.putIfAbsent(nonce, now) != null) {
            throw new SecurityException("Replayed server request");
        }
        byte[] expected = hmac(timestamp + "\n" + nonce + "\n" + body);
        byte[] supplied;
        try {
            supplied = HexFormat.of().parseHex(signature);
        } catch (IllegalArgumentException exception) {
            throw new SecurityException("Invalid server signature", exception);
        }
        if (!MessageDigest.isEqual(expected, supplied)) {
            nonces.remove(nonce);
            throw new SecurityException("Invalid server signature");
        }
    }

    private void verifyBody(String body) {
        if (body == null || body.getBytes(StandardCharsets.UTF_8).length > MAX_BODY_BYTES) {
            throw new SecurityException("Request body is empty or too large");
        }
    }

    private String requiredHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            throw new SecurityException("Missing " + name);
        }
        return value;
    }

    private byte[] hmac(String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(serverSecret, "HmacSHA256"));
            return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
        }
    }

    private static final class RateWindow {
        private final long second;
        private final AtomicInteger requests = new AtomicInteger();

        private RateWindow(long second) {
            this.second = second;
        }
    }
}
