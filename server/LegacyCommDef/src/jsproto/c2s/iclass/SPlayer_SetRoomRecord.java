package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.PlayerSetRoomRecord;

import java.util.List;

/**
 * 返回玩家房间内每局的数据
 *
 * @author Huaxing
 */
public class SPlayer_SetRoomRecord extends BaseSendMsg {
    public List<PlayerSetRoomRecord> pSetRoomRecords;

    public static SPlayer_SetRoomRecord make(List<PlayerSetRoomRecord> pSetRoomRecords) {
        SPlayer_SetRoomRecord ret = new SPlayer_SetRoomRecord();
        ret.pSetRoomRecords = pSetRoomRecords;
        return ret;

    }
}
