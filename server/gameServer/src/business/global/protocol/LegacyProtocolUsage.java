package business.global.protocol;

import com.ddm.server.common.redis.RedisUtil;
import com.ddm.server.common.CommLogD;
import java.time.LocalDate;
import java.time.ZoneOffset;

/** Aggregated counters used to prove that a legacy endpoint has reached zero traffic. */
public final class LegacyProtocolUsage {
    private static final int RETENTION_SECONDS = 40 * 24 * 60 * 60;
    private LegacyProtocolUsage() {}

    public static void record(String endpoint) {
        if (endpoint == null || !endpoint.matches("[a-z0-9._-]{1,80}"))
            throw new IllegalArgumentException("invalid legacy endpoint name");
        try {
            String day = LocalDate.now(ZoneOffset.UTC).toString();
            String key = "aoo:protocol:legacy-usage:" + day + ":" + endpoint;
            Long count = RedisUtil.incrLong(key);
            if (count != null && count == 1L) RedisUtil.expire(key, RETENTION_SECONDS);
        } catch (RuntimeException error) {
            // Observability must never make a compatible player request fail.
            CommLogD.error("legacy protocol usage counter failed endpoint:{} error:{}",
                    endpoint, error.getMessage());
        }
    }
}
