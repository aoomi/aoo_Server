package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

public class S1101_GetRoomID extends BaseSendMsg {

    public long roomID;
    public long practiceId;
    public String gameType;
    public long roomKey;

    public static S1101_GetRoomID make(long roomID, String gameType, long practiceId) {
        S1101_GetRoomID ret = new S1101_GetRoomID();
        ret.roomID = roomID;
        ret.gameType = gameType;
        ret.practiceId = practiceId;
        return ret;

    }

    public static S1101_GetRoomID make(long roomID, String gameType, long practiceId, long roomKey) {
        S1101_GetRoomID ret = new S1101_GetRoomID();
        ret.roomID = roomID;
        ret.gameType = gameType;
        ret.practiceId = practiceId;
        ret.roomKey = roomKey;
        return ret;

    }
}