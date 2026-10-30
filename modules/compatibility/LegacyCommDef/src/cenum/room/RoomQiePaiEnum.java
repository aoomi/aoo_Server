package cenum.room;

/**
 * 洗牌
 *
 * @author Administrator
 */
public enum RoomQiePaiEnum {

    RoomQiePaiEnum_Not(0),
    RoomQiePaiEnum_1(0.1),
    RoomQiePaiEnum_2(0.2),
    RoomQiePaiEnum_3(0.5),
    RoomQiePaiEnum_4(0.8),
    RoomQiePaiEnum_5(1.0),
    ;
    private double value;

    private RoomQiePaiEnum(double value) {
        this.value = value;
    }

    public double value() {
        return this.value;
    }

    public static RoomQiePaiEnum valueOf(int value) {
        for (RoomQiePaiEnum flow : RoomQiePaiEnum.values()) {
            if (flow.ordinal() == value) {
                return flow;
            }
        }
        return RoomQiePaiEnum.RoomQiePaiEnum_Not;
    }
}