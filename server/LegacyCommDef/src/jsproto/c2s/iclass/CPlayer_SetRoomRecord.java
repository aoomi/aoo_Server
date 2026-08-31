package jsproto.c2s.iclass;


/**
 * 客户端请求，获取房间每局记录
 *
 * @author Huaxing
 */
public class CPlayer_SetRoomRecord {
    public long roomID;//房间ID

    public static CPlayer_SetRoomRecord make(long roomID) {
        CPlayer_SetRoomRecord ret = new CPlayer_SetRoomRecord();
        ret.roomID = roomID;
        return ret;
    }
}
