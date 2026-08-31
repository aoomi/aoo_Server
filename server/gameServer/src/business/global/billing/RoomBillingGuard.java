package business.global.billing;

import com.ddm.server.common.redis.RedisUtil;

/**
 * Room billing idempotency boundary shared by personal and club rooms.
 */
public final class RoomBillingGuard {
    private static final int RETENTION_SECONDS = 90 * 24 * 60 * 60;
    private static final String PREFIX = "aoo:billing:room:";

    private RoomBillingGuard() {
    }

    public static boolean beginRefund(long roomId, long playerId) {
        if (roomId <= 0 || playerId <= 0) {
            throw new IllegalArgumentException("refund requires roomId and playerId");
        }
        RedisUtil.AtomicResult result = RedisUtil.setNxExResult(
                PREFIX + roomId + ":player:" + playerId + ":refund",
                "PROCESSING",
                RETENTION_SECONDS);
        if (result == RedisUtil.AtomicResult.UNAVAILABLE || result == RedisUtil.AtomicResult.UNKNOWN_APPLIED) {
            throw new IllegalStateException("billing idempotency outcome unknown");
        }
        return result == RedisUtil.AtomicResult.APPLIED;
    }

    public static void recordConsume(long roomId, long playerId, int amount) {
        if (roomId <= 0 || playerId <= 0 || amount < 0) {
            throw new IllegalArgumentException("invalid consume record");
        }
        RedisUtil.setex(
                PREFIX + roomId + ":player:" + playerId + ":consume",
                RETENTION_SECONDS,
                "amount=" + amount);
    }
}
