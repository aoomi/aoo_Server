package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.room.RoomInfoItem;
import lombok.Data;

import java.util.List;

/**
 * 获取赛事房间信息
 *
 * @author zaf
 */
@Data
public class SUnion_GetAllRoom extends BaseSendMsg {
    private long clubId;
    private long unionId;
    private List<RoomInfoItem> roomList;
    private int pageNum;

    public static SUnion_GetAllRoom make(long clubId, long unionId, List<RoomInfoItem> roomList, int pageNum) {
        SUnion_GetAllRoom ret = new SUnion_GetAllRoom();
        ret.setClubId(clubId);
        ret.setUnionId(unionId);
        ret.setRoomList(roomList);
        ret.setPageNum(pageNum);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}