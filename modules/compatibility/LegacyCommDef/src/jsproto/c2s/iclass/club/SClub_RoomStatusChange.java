package jsproto.c2s.iclass.club;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.room.RoomInfoItem;

/**
 * 获取俱乐部房间状态改变
 *
 * @author zaf
 */
public class SClub_RoomStatusChange extends BaseSendMsg {

    public RoomInfoItem roomInfoItem;

    public static SClub_RoomStatusChange make(RoomInfoItem roomInfoItem) {
        SClub_RoomStatusChange ret = new SClub_RoomStatusChange();
        ret.roomInfoItem = roomInfoItem;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}