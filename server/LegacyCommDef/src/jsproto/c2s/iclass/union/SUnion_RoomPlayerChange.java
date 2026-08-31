package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.room.RoomPosInfoShort;

/**
 * 获取赛事房间里面玩家信息改变
 *
 * @author zaf
 */
public class SUnion_RoomPlayerChange extends BaseSendMsg {


    public long id;            //俱乐部ID
    public String roomKey;            //房间roomkey
    public RoomPosInfoShort pos;                //玩家信息d
    public int sort;

    public static SUnion_RoomPlayerChange make(long id, String roomKey, RoomPosInfoShort pos, int sort) {
        SUnion_RoomPlayerChange ret = new SUnion_RoomPlayerChange();
        ret.id = id;
        ret.roomKey = roomKey;
        ret.pos = pos;
        ret.sort = sort;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}