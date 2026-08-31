package jsproto.c2s.iclass.pk;

import jsproto.c2s.cclass.BaseSendMsg;

import java.util.List;

public class SPK_RoomEndResult<T> extends BaseSendMsg {
    public long roomId;
    public String key;
    public int setId;
    public int endTime;
    public long ownerID;
    public List<T> countRecords;

    public static <T> SPK_RoomEndResult make(long roomId, long ownerID, String key, int setId, int endTime, List<T> countRecords) {
        SPK_RoomEndResult ret = new SPK_RoomEndResult();
        ret.roomId = roomId;
        ret.ownerID = ownerID;
        ret.key = key;
        ret.setId = setId;
        ret.endTime = endTime;
        ret.countRecords = countRecords;
        return ret;
    }
}