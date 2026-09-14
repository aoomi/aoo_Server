package jsproto.c2s.iclass.club;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.room.RoomPosInfo;

/**
 * 获取俱乐部房间里面玩家信息改变
 *
 * @author zaf
 */
public class SClub_RoomPlayerChange extends BaseSendMsg {


    public long roomID;                //房间ID
    public long clubId;            //俱乐部ID
    public String roomKey;            //房间roomkey
    public RoomPosInfo pos;                //玩家信息
    public int sort;

    public static SClub_RoomPlayerChange make(long clubId, long roomID, String roomKey, RoomPosInfo pos, int sort) {
        SClub_RoomPlayerChange ret = new SClub_RoomPlayerChange();
        ret.roomID = roomID;
        ret.clubId = clubId;
        ret.roomKey = roomKey;
        ret.pos = pos;
        ret.sort = sort;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}