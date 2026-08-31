package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.room.RoomInfoItemShortOne;
import lombok.Data;

import java.util.List;

/**
 * 获取赛事房间信息
 *
 * @author zaf
 */
@Data
public class SUnion_GetAllRoomMin extends BaseSendMsg {
    private long clubId;
    private long unionId;
    private List<RoomInfoItemShortOne> roomList;

    public static SUnion_GetAllRoomMin make(long clubId, long unionId, List<RoomInfoItemShortOne> roomList) {
        SUnion_GetAllRoomMin ret = new SUnion_GetAllRoomMin();
        ret.setClubId(clubId);
        ret.setUnionId(unionId);
        ret.setRoomList(roomList);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}