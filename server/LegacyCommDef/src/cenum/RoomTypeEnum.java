package cenum;

/**
 * 房间类型
 *
 * @author Administrator
 */
public enum RoomTypeEnum {
    /**
     * 正常游戏
     */
    NORMAL,
    /**
     * 亲友圈
     */
    CLUB,
    /**
     * 练习场
     */
    ROBOT,
    /**
     * 竞技场
     */
    ARENA,
    /**
     * 赛事
     */
    UNION,
    ;

    public static boolean checkUnionOrClub(RoomTypeEnum typeEnum) {
        if (RoomTypeEnum.UNION.equals(typeEnum) || RoomTypeEnum.CLUB.equals(typeEnum)) {
            return true;
        }
        return false;
    }

    /**
     * 检查存在抽奖的房间类型
     *
     * @param typeEnum 房间类型
     * @return
     */
    public static boolean checkExistLuckDrawRoomType(RoomTypeEnum typeEnum) {
        return RoomTypeEnum.UNION.equals(typeEnum) || RoomTypeEnum.CLUB.equals(typeEnum) || RoomTypeEnum.NORMAL.equals(typeEnum);
    }
}
