package cenum.room;

/**
 * 显示操作配置
 * 不限制,3分钟出牌,5分钟出牌
 *
 * @author Administrator
 */
public enum XianShiConfigEnum {
    // 不限制
    NOT_LIMIT(0),
    // 3分钟
    Min3(181000),
    // 5分钟
    Min5(301000),
    // 1分钟
    Min1(61000),
    // 30秒
    Sec3(31000),
    // 15秒
    Sec15(16000),
    // 10分钟
    Min10(601000),
    ;
    private int value;

    XianShiConfigEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public static XianShiConfigEnum valueOf(int value) {
        for (XianShiConfigEnum flow : XianShiConfigEnum.values()) {
            if (flow.ordinal() == value) {
                return flow;
            }
        }
        return XianShiConfigEnum.NOT_LIMIT;
    }

}
