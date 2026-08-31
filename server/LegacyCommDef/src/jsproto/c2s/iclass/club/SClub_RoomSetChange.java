package jsproto.c2s.iclass.club;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 获取俱乐部房间局数变化
 *
 * @author zaf
 */
@Data
public class SClub_RoomSetChange extends BaseSendMsg {

    /**
     * 亲友圈id
     */
    private long id;
    /**
     * 房间key
     */
    private String roomKey;
    /**
     * 第几局
     */
    private int setId;

    private int sort;

    public static SClub_RoomSetChange make(long id, long roomID, String roomKey, int setId, int sort) {
        SClub_RoomSetChange ret = new SClub_RoomSetChange();
        ret.id = id;
        ret.setId = setId;
        ret.roomKey = roomKey;
        ret.sort = sort;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}