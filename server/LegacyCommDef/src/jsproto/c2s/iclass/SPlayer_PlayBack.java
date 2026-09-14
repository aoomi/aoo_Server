package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

public class SPlayer_PlayBack extends BaseSendMsg {
    public long roomID;
    public int setID;
    public int endTime;
    public int dPos;
    public String playerList;
    public int setCount = 0;
    public String roomKey = "";
    public int gameType;
    public int tabId;

    public static SPlayer_PlayBack make(long roomID, int setID, int endTime, int dPos, String playerList, int setCount, String roomKey, int gameType, int tabId) {
        SPlayer_PlayBack ret = new SPlayer_PlayBack();
        ret.roomID = roomID;
        ret.setID = setID;
        ret.endTime = endTime;
        ret.dPos = dPos;
        ret.playerList = playerList;
        ret.setCount = setCount;
        ret.roomKey = roomKey;
        ret.gameType = gameType;
        ret.tabId = tabId;
        return ret;
    }


}
