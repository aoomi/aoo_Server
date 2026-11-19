package cenum;

/**
 * 默认参数值
 *
 * @author Administrator
 */
public enum DefaultEnum {
    /**
     * 存在
     */
    EXIST(1),

    /**
     * 不存在
     */
    NOT_EXIST(0),


    /**
     * tagDBDataKey keyID
     */
    DATA_KEY_ID(1),
    ;

    private int value;

    DefaultEnum(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

};