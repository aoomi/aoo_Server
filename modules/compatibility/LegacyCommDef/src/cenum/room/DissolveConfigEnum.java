package cenum.room;

/**
 * 解散配置
 *
 * @author Administrator
 */
public enum DissolveConfigEnum {
    // 30秒,
    Sec30(30),
    // 1分钟
    Min1(60),
    // 3分钟
    Min3(180),
    // 5分钟
    Min5(300),
    // 不可解散
    NotDissolve(0),
    ;
    private int value;

    DissolveConfigEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public static DissolveConfigEnum valueOf(int value) {
        for (DissolveConfigEnum flow : DissolveConfigEnum.values()) {
            if (flow.ordinal() == value) {
                return flow;
            }
        }
        return DissolveConfigEnum.Min3;
    }

}
