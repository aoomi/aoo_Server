package jsproto.c2s.iclass.mj;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.pos.PlayerPosInfo;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class _Promptly extends BaseSendMsg {
    public long roomID;
    public List<? extends PlayerPosInfo> playerPosInfoList = new ArrayList<>();

    public static _Promptly make(long roomID, List<? extends PlayerPosInfo> playerPosInfoList, String gameNameStr) {
        _Promptly ret = new _Promptly();
        ret.roomID = roomID;
        ret.playerPosInfoList = playerPosInfoList;
        ret.setGameNameStr(gameNameStr);
        return ret;

    }
}		
