package core.network.http.client;

import com.google.gson.Gson;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
class HttpProtocolIdempotency {
    private static final Gson GSON = new Gson();
    private static final String PROCESSING = "__PROCESSING__";

    private static volatile RedissonClient redis;

    HttpProtocolIdempotency(RedissonClient redis) {
        HttpProtocolIdempotency.redis = redis;
    }

    static HttpProtocolEnvelope previous(String requestId, String requestBody) {
        String value = bucket(requestId, requestBody).get();
        return value == null || value.isBlank() || PROCESSING.equals(value)
                ? null : GSON.fromJson(value, HttpProtocolEnvelope.class);
    }

    static boolean acquire(String requestId, String requestBody) {
        return bucket(requestId, requestBody).setIfAbsent(PROCESSING, Duration.ofSeconds(30));
    }

    static void complete(String requestId, String requestBody, HttpProtocolEnvelope response) {
        bucket(requestId, requestBody).set(GSON.toJson(response), Duration.ofMinutes(5));
    }

    static void release(String requestId, String requestBody) {
        bucket(requestId, requestBody).delete();
    }

    private static RBucket<String> bucket(String requestId, String requestBody) {
        RedissonClient client = redis;
        if (client == null) throw new IllegalStateException("Redisson client is not initialized");
        return client.getBucket(key(requestId, requestBody));
    }

    private static String key(String requestId, String requestBody) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(requestBody.getBytes(StandardCharsets.UTF_8));
            return "protocol:http:idempotency:" + requestId + ":" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
