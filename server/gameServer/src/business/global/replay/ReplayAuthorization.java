package business.global.replay;

import com.ddm.server.common.redis.RedisUtil;

public final class ReplayAuthorization {
    private ReplayAuthorization() {}

    public static boolean mayRead(long playerId, int playBackCode) {
        if (playerId <= 0 || playBackCode <= 0) return false;
        String ownerKey = "aoo:replay:" + playBackCode + ":viewer:" + playerId;
        RedisUtil.AvailabilityResult result=RedisUtil.existsResult(ownerKey);
        if(!result.available()) throw new IllegalStateException("replay authorization unavailable");
        return result.value();
    }

    public static void grant(long playerId, int playBackCode) {
        if (playerId <= 0 || playBackCode <= 0) throw new IllegalArgumentException("invalid replay grant");
        RedisUtil.setex("aoo:replay:" + playBackCode + ":viewer:" + playerId,
                180 * 24 * 60 * 60, "PLAYER_VIEW");
    }
}
