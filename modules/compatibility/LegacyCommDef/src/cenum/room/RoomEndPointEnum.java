package cenum.room;

/**
 * 	大局分数
 *
 * @author Administrator
 */
public enum RoomEndPointEnum {

    RoomEndPointEnum_normal(0), // 正常
    RoomEndPointEnum_Ten_Enough(1), // 不足10按10算
    RoomEndPointEnum_Ten_Doubble(2), // 不足10翻倍算
    ;
    private int value;

    private RoomEndPointEnum(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    public static RoomEndPointEnum valueOf(int value) {
        for (RoomEndPointEnum flow : RoomEndPointEnum.values()) {
            if (flow.value == value) {
                return flow;
            }
        }
        return RoomEndPointEnum.RoomEndPointEnum_normal;
    }
}