package cenum.room;

/**
 * 房间阶段管理
 *
 * @author Administrator
 */
public enum RoomState {
    Init(0), // 准备阶段
    Playing(1), // 游戏阶段
    // Compare(2),//游戏比较阶段
    End(2), // 游戏结算，点赞阶段
    Waiting(3),
    // Prepare(3), // 每局准备阶段

    ;
    private int value;

    private RoomState(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    public static RoomState valueOf(int value) {
        for (RoomState flow : RoomState.values()) {
            if (flow.value == value) {
                return flow;
            }
        }
        return RoomState.Init;
    }
}