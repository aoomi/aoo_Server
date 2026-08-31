package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 获取赛事房间局数变化
 *
 * @author zaf
 */
@Data
public class SUnion_RoomSetChange extends BaseSendMsg {
    /**
     * 赛事id
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
    public int sort;

    public static SUnion_RoomSetChange make(long id, String roomKey, int setId, int sort) {
        SUnion_RoomSetChange ret = new SUnion_RoomSetChange();
        ret.setId(id);
        ret.setRoomKey(roomKey);
        ret.setSetId(setId);
        ret.setSort(sort);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}