package cenum;


/**
 * 消耗类型
 *
 * @author Administrator
 */
public enum PrizeType {
    None(0), // 0-
    Gold(1), // 1-金币
    RoomCard(2), // 2-房卡
    ClubCard(3),//3-圈卡
    RedEnvelope(8),// 8-红包
    Free(11),//免费
    ;
    private int value;

    private PrizeType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static PrizeType valueOf(int value) {
        for (PrizeType flow : PrizeType.values()) {
            if (flow.value == value) {
                return flow;
            }
        }
        return PrizeType.None;
    }
}
