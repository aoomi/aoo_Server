package cenum.redis;

/**
 * 分布式锁时间
 */
public enum RedissLockTimeEnum {
    /**
     *等待时间
     */
    WAIT_TIME(3),

    /**
     * 超时时间
     */
    OUT_TIME(30),

    ;

    private final int value;

    RedissLockTimeEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
