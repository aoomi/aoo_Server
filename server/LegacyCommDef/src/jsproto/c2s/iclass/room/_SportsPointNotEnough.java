package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class _SportsPointNotEnough extends BaseSendMsg {
    //房间id
    public long roomID;

    public static _SportsPointNotEnough make(long roomID, String gameName) {
        _SportsPointNotEnough ret = new _SportsPointNotEnough();
        ret.roomID = roomID;
        ret.setGameNameStr(gameName);
        return ret;
    }
}
