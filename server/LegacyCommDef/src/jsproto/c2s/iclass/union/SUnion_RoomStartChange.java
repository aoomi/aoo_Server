package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 获取赛事房间开始通知
 *
 * @author zaf
 */
@Data
public class SUnion_RoomStartChange extends BaseSendMsg {
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


    public static SUnion_RoomStartChange make(long id, String roomKey, int roomStateId) {
        SUnion_RoomStartChange ret = new SUnion_RoomStartChange();
        ret.setId(id);
        ret.setRoomKey(roomKey);
        ret.setRoomStateId(roomStateId);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }

}