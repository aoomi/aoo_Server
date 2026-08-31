package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.room.RoomInfoItemShortTwo;
import lombok.Data;

import java.util.List;

/**
 * 获取赛事房间信息
 *
 * @author zaf
 */
@Data
public class SUnion_GetAllRoomData extends BaseSendMsg {
    private List<RoomInfoItemShortTwo> roomList;

    public static SUnion_GetAllRoomData make(List<RoomInfoItemShortTwo> roomList) {
        SUnion_GetAllRoomData ret = new SUnion_GetAllRoomData();
        ret.setRoomList(roomList);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}