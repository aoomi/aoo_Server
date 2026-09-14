package cenum.room;

/**
 * 房间托管状态
 *
 * @author Huaxing
 */
public enum TrusteeshipState {
    // 正常托管
    Normal(0),
    // 发起解散
    Dissolve(1),
    // 继续游戏
    Continue(2),
    // 等待
    Wait(3),
    // 结束定时器
    End(4),
    ;
    private int value;

    private TrusteeshipState(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    public static TrusteeshipState valueOf(int value) {
        for (TrusteeshipState flow : TrusteeshipState.values()) {
            if (flow.value == value) {
                return flow;
            }
        }
        return TrusteeshipState.Normal;
    }
}