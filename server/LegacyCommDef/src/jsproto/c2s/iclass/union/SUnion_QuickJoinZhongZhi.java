package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.room.RoomInfoItemShortOne;
import jsproto.c2s.cclass.room.RoomInfoItemShortOneQuickJoinZhongZhi;
import lombok.Data;

import java.util.List;

/**
 * 获取赛事房间信息
 *
 * @author zaf
 */
@Data
public class SUnion_QuickJoinZhongZhi extends BaseSendMsg {
    private long clubId;
    private long unionId;
    private List<RoomInfoItemShortOneQuickJoinZhongZhi> roomList;
    public static SUnion_QuickJoinZhongZhi make(long clubId, long unionId, List<RoomInfoItemShortOneQuickJoinZhongZhi> roomList) {
        SUnion_QuickJoinZhongZhi ret = new SUnion_QuickJoinZhongZhi();
        ret.setClubId(clubId);
        ret.setUnionId(unionId);
        ret.setRoomList(roomList);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}