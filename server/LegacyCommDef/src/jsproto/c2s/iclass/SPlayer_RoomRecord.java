package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.PlayerRoomRecord;

import java.util.List;


/**
 * 返回房间战绩
 *
 * @author Huaxing
 */
public class SPlayer_RoomRecord extends BaseSendMsg {
    public List<PlayerRoomRecord> pRoomRecords;

    public static SPlayer_RoomRecord make(List<PlayerRoomRecord> pRoomRecords) {
        SPlayer_RoomRecord ret = new SPlayer_RoomRecord();
        ret.pRoomRecords = pRoomRecords;
        return ret;

    }

}
