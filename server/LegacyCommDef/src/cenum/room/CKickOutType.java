package cenum.room;

/**
 * 游戏踢出房间类型
 *
 * @author Administrator
 */
public enum CKickOutType {
    None(0), // 默认值
    SYSTEM(1), // 系统提出
    SPECIAL(2),// 亲友圈或者联赛
    TIMEOUT(3),// 超时未准备

    ;
    private int value;

    private CKickOutType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static CKickOutType valueOf(int value) {
        for (CKickOutType flow : CKickOutType.values()) {
            if (flow.value == value) {
                return flow;
            }
        }
        return CKickOutType.None;
    }
}