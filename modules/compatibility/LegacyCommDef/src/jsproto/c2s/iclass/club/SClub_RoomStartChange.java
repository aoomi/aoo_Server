package jsproto.c2s.iclass.club;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 获取房间开始通知
 *
 * @author zaf
 */
@Data
public class SClub_RoomStartChange extends BaseSendMsg {
    /**
     * id
     */
    public long id;
    /**
     * 房间key
     */
    public String roomKey;
    /**
     * 房间状态类型
     */
    private int roomStateId;


    public static SClub_RoomStartChange make(long id, String roomKey, int roomStateId) {
        SClub_RoomStartChange ret = new SClub_RoomStartChange();
        ret.setId(id);
        ret.setRoomKey(roomKey);
        ret.setRoomStateId(roomStateId);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }

}