package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.room.RoomInfoItem;

/**
 * 获取赛事房间状态改变
 *
 * @author zaf
 */
public class SUnion_RoomStatusChange extends BaseSendMsg {

    public RoomInfoItem roomInfoItem;

    public static SUnion_RoomStatusChange make(RoomInfoItem roomInfoItem) {
        SUnion_RoomStatusChange ret = new SUnion_RoomStatusChange();
        ret.roomInfoItem = roomInfoItem;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}