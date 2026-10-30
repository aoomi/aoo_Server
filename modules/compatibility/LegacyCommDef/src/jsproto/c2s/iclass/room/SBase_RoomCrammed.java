package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 通知房间人满了接口
 */
public class SBase_RoomCrammed extends BaseSendMsg {
    // 房间ID
    private long roomID;
    private String roomKey;
    private String roomName;
    private int gameId;


    public static SBase_RoomCrammed make(long roomID, String roomKey, String roomName, int gameId) {
        SBase_RoomCrammed ret = new SBase_RoomCrammed();
        ret.roomID = roomID;
        ret.roomKey = roomKey;
        ret.roomName = roomName;
        ret.gameId = gameId;
        return ret;
    }
}
