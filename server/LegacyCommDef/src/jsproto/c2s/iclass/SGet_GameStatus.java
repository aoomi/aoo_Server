package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

public class SGet_GameStatus<T> extends BaseSendMsg {
    public boolean isPlayingGame; //0：大厅， 1：游戏中
    public int gameType;
    public long roomID;

    public static SGet_GameStatus make(Boolean isPlayingGame, int gameType, long roomID) {
        SGet_GameStatus ret = new SGet_GameStatus();
        ret.isPlayingGame = isPlayingGame;
        ret.gameType = gameType;
        ret.roomID = roomID;
        return ret;
    }
}
