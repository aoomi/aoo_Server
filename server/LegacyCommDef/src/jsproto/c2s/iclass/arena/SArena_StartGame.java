package jsproto.c2s.iclass.arena;

import cenum.ArenaEnum.StartGameEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.GameType;

/**
 * 开始游戏通知
 *
 * @author Huaxing
 */
public class SArena_StartGame extends BaseSendMsg {
    public long roomID = 0L;
    public StartGameEnum startGame;
    public GameType gameType;

    public static SArena_StartGame make(long roomID, StartGameEnum startGame, GameType gameType) {
        SArena_StartGame ret = new SArena_StartGame();
        ret.roomID = roomID;
        ret.startGame = startGame;
        ret.gameType = gameType;
        return ret;
    }
}
