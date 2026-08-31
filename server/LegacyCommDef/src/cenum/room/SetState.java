package cenum.room;

/**
 * 游戏阶段管理
 *
 * @author Administrator
 */
public enum SetState {
    Init(0), // 开始发牌中
    // Waiting(1), // 等待操作中
    Playing(1), // 执行操作中
    End(2), // 游戏已结束
    Waiting(3),
    WaitingEx(4),
    Init2(5),// nn扑克 存在二次发牌
    State6(6),//
    State7(7),//
    BiPaiFirst(8),//可能存在比牌 按顺序的
    State8(9),//独
    State9(10),//保
    ;

    private int value;

    private SetState(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    public static SetState valueOf(int value) {
        for (SetState flow : SetState.values()) {
            if (flow.value == value) {
                return flow;
            }
        }
        return SetState.Init;
    }
}