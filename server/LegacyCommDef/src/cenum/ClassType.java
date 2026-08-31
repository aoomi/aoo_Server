package cenum;

/**
 * 游戏种类
 *
 * @author Administrator
 */
public enum ClassType {
    /**
     * 空
     */
    NONE(0),
    /**
     * 麻将
     */
    MJ(1),
    /**
     * 扑克
     */
    PK(2),
    ;
    private int value;

    ClassType(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    public static ClassType valueOf(int value) {
        for (ClassType flow : ClassType.values()) {
            if (flow.value == value) {
                return flow;
            }
        }
        return ClassType.MJ;
    }
}