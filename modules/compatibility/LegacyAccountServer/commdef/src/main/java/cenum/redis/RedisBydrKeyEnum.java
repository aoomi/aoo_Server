package cenum.redis;

/**
 * redis数据key枚举
 */
public enum RedisBydrKeyEnum {
    /**
     * 玩家基本信息
     */
    PLAYER_MAP("PLAYER:PID:%d:MAP"),
    /**
     * 账号服ID 转 玩家ID
     */
    AID_2_PID_MAP("AID_2_PID_MAP"),
    /**
     * 玩家活动记录
     */
    PLAYER_ACTIVITY_MAP("PLAYER:ACTIVITY:PID:%d:MAP"),

    /**
     * 自增的账号Id
     */
    ACCOUNT_ID("ACCOUNT_ID:STR"),

    /**
     * 密钥
     */
    SECRET("SECRET:MAP"),

    ;

    private final String key;

    RedisBydrKeyEnum(String key) {
        this.key = key;
    }

    public String getKey(Object... objects) {
        return String.format(key, objects);
    }

    public String getKey() {
        return key;
    }
}
