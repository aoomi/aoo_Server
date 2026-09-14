package core.network.client2game.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;

import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Compatibility guard until every client write uses the unified Gateway frame. */
final class LegacyReplayGuard {
    private static final long ALLOWED_SKEW_MS = Duration.ofSeconds(30).toMillis();
    private static final int MAX_REQUESTS_PER_PLAYER = 2_048;
    private static final Map<Long, LinkedHashMap<String, Long>> REQUESTS = new LinkedHashMap<>();

    private LegacyReplayGuard() { }

    static void validate(String event, long playerId, String message) throws WSException {
        if (!isMutating(event)) return;
        final JSONObject body;
        try { body = JSON.parseObject(message); }
        catch (RuntimeException error) { throw new WSException(ErrorCode.NotAllow, "invalid request envelope"); }
        String requestId = body.getString("requestId");
        Long timestamp = body.getLong("timestamp");
        if (requestId == null || requestId.isBlank() || requestId.length() > 128 || timestamp == null) {
            throw new WSException(ErrorCode.NotAllow, "requestId and timestamp are required");
        }
        long now = Clock.systemUTC().millis();
        if (timestamp <= 0 || Math.abs(now - timestamp) > ALLOWED_SKEW_MS) {
            throw new WSException(ErrorCode.NotAllow, "request timestamp outside accepted window");
        }
        synchronized (REQUESTS) {
            LinkedHashMap<String, Long> playerRequests = REQUESTS.computeIfAbsent(playerId,
                    ignored -> new LinkedHashMap<>(64, 0.75f, true));
            playerRequests.entrySet().removeIf(entry -> now - entry.getValue() > ALLOWED_SKEW_MS);
            if (playerRequests.putIfAbsent(requestId, now) != null)
                throw new WSException(ErrorCode.NotAllow, "duplicate requestId");
            while (playerRequests.size() > MAX_REQUESTS_PER_PLAYER) {
                playerRequests.remove(playerRequests.keySet().iterator().next());
            }
        }
    }

    private static boolean isMutating(String event) {
        if (event == null) return false;
        return event.contains("OpCard") || event.contains("Trusteeship") || event.contains("Piao")
                || event.contains("OpenCard") || event.contains("AddDouble") || event.contains("QiangZhuang")
                || event.contains("KongPai") || event.contains("RobClose") || event.contains("FaPaiJieShu")
                || event.contains("AutoChoose") || event.contains("ChangePlayerNum") || event.contains("KickRoom")
                || event.contains("Give") || event.contains("Zhuang") || event.contains("PlayerOp");
    }
}
