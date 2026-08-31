package cenum;

/**
 * 默认参数值
 *
 * @author Administrator
 */
public enum DefaultEnum {
    FAMILY_ID(10001),    //默认公会ID
    GAME_SERVER_SIGN_ID(500),
    HALL_SERVER_SIGN_ID(400),
    ;

    private int value;

    DefaultEnum(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }
};
