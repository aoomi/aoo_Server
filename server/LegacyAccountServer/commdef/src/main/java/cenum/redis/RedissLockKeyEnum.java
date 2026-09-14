package cenum.redis;

/**
 * 分布式锁
 */
public enum RedissLockKeyEnum {
    /**
     * 玩家账号Id
     */
    PLAYER_AID_LOCK("PLAYER:AID:%d:LOCK"),
    ;

    private final String key;

    RedissLockKeyEnum(String key) {
        this.key = key;
    }

    public String getKey(Object... objects) {
        return String.format(key, objects);
    }
}
