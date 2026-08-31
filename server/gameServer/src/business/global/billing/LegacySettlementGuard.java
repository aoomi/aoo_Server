package business.global.billing;

import com.ddm.server.common.redis.RedisUtil;

public final class LegacySettlementGuard {
    private static final int RETENTION_SECONDS = 180 * 24 * 60 * 60;
    private LegacySettlementGuard() {}

    public static boolean beginRound(long roomId, int roundNo, String settlementVersion) {
        if (roomId <= 0 || roundNo <= 0 || settlementVersion == null || settlementVersion.isBlank())
            throw new IllegalArgumentException("invalid settlement identity");
        return applied(RedisUtil.setNxExResult("aoo:settlement:round:" + roomId + ":" + roundNo + ":" + settlementVersion,
                "PROCESSING", RETENTION_SECONDS));
    }

    public static boolean beginFinal(long roomId, String settlementVersion) {
        if (roomId <= 0 || settlementVersion == null || settlementVersion.isBlank())
            throw new IllegalArgumentException("invalid final settlement identity");
        return applied(RedisUtil.setNxExResult("aoo:settlement:final:" + roomId + ":" + settlementVersion,
                "PROCESSING", RETENTION_SECONDS));
    }

    /** Allows the post-round dissolution adjustment to run once without reopening round settlement. */
    public static boolean beginDissolutionAdjustment(long roomId, int roundNo, String settlementVersion) {
        if (roomId <= 0 || roundNo <= 0 || settlementVersion == null || settlementVersion.isBlank())
            throw new IllegalArgumentException("invalid dissolution settlement identity");
        return applied(RedisUtil.setNxExResult("aoo:settlement:dissolution:" + roomId + ":" + roundNo + ":" + settlementVersion,
                "1", 30 * 24 * 60 * 60));
    }
    private static boolean applied(RedisUtil.AtomicResult result){if(result==RedisUtil.AtomicResult.UNAVAILABLE||result==RedisUtil.AtomicResult.UNKNOWN_APPLIED)throw new IllegalStateException("settlement idempotency outcome unknown");return result==RedisUtil.AtomicResult.APPLIED;}
}
