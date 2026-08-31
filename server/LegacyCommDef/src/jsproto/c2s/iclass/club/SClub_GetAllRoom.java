package jsproto.c2s.iclass.club;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.room.RoomInfoItem;

import java.util.List;

/**
 * 获取俱乐部房间信息
 *
 * @author zaf
 */
public class SClub_GetAllRoom extends BaseSendMsg {
    public long clubId;
    public long unionId;
    public List<RoomInfoItem> roomList;
    public int pageNum;

    public static SClub_GetAllRoom make(long clubId, List<RoomInfoItem> roomList, int pageNum) {
        SClub_GetAllRoom ret = new SClub_GetAllRoom();
        ret.clubId = clubId;
        ret.unionId = 0L;
        ret.roomList = roomList;
        ret.pageNum = pageNum;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}