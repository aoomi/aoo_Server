package cenum;

/**
 * 访问标记
 */
public enum VisitSignEnum {
    /**
     * 空标记
     */
    NONE,
    /**
     * 房间内
     */
    ROOM,
    /**
     * 亲友圈房间主界面
     */
    CLUN_ROOM_MAIN,
    ;

    public static VisitSignEnum valueOf(int value) {
        for (VisitSignEnum flow : VisitSignEnum.values()) {
            if (flow.ordinal() == value) {
                return flow;
            }
        }
        return VisitSignEnum.NONE;
    }
}
