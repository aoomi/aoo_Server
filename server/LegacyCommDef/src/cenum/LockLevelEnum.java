package cenum;

/**
 * 锁等级
 */
public enum LockLevelEnum {
    /**
     * 一级
     */
    LEVLE_1(1),
    /**
     * 二级
     */
    LEVLE_2(2),
    /**
     * 十级
     */
    LEVLE_10(10),
    ;

    private int value;

    LockLevelEnum(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }
}
