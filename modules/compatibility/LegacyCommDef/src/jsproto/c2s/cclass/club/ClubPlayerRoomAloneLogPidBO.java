package jsproto.c2s.cclass.club;

import lombok.Data;

/**
 * 玩家房间单独记录表
 *
 * @author Administrator
 */
@Data
public class ClubPlayerRoomAloneLogPidBO {
    /**
     * 房间ID
     */
    private long roomID;

    public ClubPlayerRoomAloneLogPidBO() {
        roomID = 0L;
    }

    public static String getItemsName() {
        return "roomID";
    }
}
