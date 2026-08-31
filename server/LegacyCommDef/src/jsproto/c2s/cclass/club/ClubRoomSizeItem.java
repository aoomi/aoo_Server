package jsproto.c2s.cclass.club;

import lombok.Data;

@Data
public class ClubRoomSizeItem {
    /**
     * 亲友圈Id
     */
    private long clubId;
    /**
     * 联盟Id
     */
    private long unionId;
    /**
     * 时间
     */
    private String dateTime;
    /**
     * 房间数
     */
    private int roomSize;

    public static String getItemsName() {
        return "sum(roomSize) as roomSize";
    }
}
