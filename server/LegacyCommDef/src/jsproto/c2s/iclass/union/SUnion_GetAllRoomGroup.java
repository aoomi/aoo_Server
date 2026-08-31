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
public class SUnion_GetAllRoomGroup extends BaseSendMsg {
    private long clubId;
    private long unionId;
    private List<RoomInfoItem> roomList;
    private int pageNum;
    public boolean finalPage;

    public static SUnion_GetAllRoomGroup make(long clubId, long unionId, List<RoomInfoItem> roomList, int pageNum, boolean finalPage) {
        SUnion_GetAllRoomGroup ret = new SUnion_GetAllRoomGroup();
        ret.setClubId(clubId);
        ret.setUnionId(unionId);
        ret.setRoomList(roomList);
        ret.setPageNum(pageNum);
        ret.setFinalPage(finalPage);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}