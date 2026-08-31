package business.njpdk.c2s.iclass;

import cenum.PrizeType;
import cenum.room.RoomState;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.mj.MJRoomSetInfo;
import jsproto.c2s.cclass.room.RoomPosInfo;
import jsproto.c2s.cclass.room.Room_Dissolve;

import java.util.List;

/**
 * 安岳跑的快房间信息
 */
public class SNJPDK_GetRoomInfo<T> extends BaseSendMsg {
    public long roomID;
    public String key;
    public int createSec;
    public PrizeType prizeType;
    public RoomState state;
    public int setID;
    public long ownerID;
    public T cfg;
    public MJRoomSetInfo set;
    public List<RoomPosInfo> posList;
    public Room_Dissolve dissolve;
    public long createID;

    public static <T> SNJPDK_GetRoomInfo make(long roomID, String key, int createSec, PrizeType prizeType,
                                              RoomState state, int setID, long ownerID, T cfg, MJRoomSetInfo set, List<RoomPosInfo> posList,
                                              Room_Dissolve dissolve, long createID) {
        SNJPDK_GetRoomInfo ret = new SNJPDK_GetRoomInfo();
        ret.roomID = roomID;
        ret.key = key;
        ret.createSec = createSec;
        ret.prizeType = prizeType;
        ret.state = state;
        ret.setID = setID;
        ret.ownerID = ownerID;
        ret.cfg = cfg;
        ret.set = set;
        ret.posList = posList;
        ret.dissolve = dissolve;
        ret.createID = createID;
        return ret;
    }
}
