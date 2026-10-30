package jsproto.c2s.cclass.share;

/**
 * @author xsj
 * @date 2020/8/17 10:54
 * @description 共享消费类型
 */
public enum SharePrizeType {
    None(0), // 0-
    Gold(1), // 1-金币
    RoomCard(2), // 2-房卡
    ClubCard(3),//3-圈卡
    RedEnvelope(8),// 8-红包
    Free(11),//免费
    ;
    private int value;

    private SharePrizeType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static SharePrizeType valueOf(int value) {
        for (SharePrizeType flow : SharePrizeType.values()) {
            if (flow.value == value) {
                return flow;
            }
        }
        return SharePrizeType.None;
    }
}
