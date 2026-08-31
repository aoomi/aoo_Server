package jsproto.c2s.iclass.arena;

import cenum.ArenaEnum.StartGameEnum;
import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 加入游戏房间通知
 *
 * @author Huaxing
 */
public class SArena_JoinGameRoom extends BaseSendMsg {
    public String name;
    public StartGameEnum startGame;

    public static SArena_JoinGameRoom make(String name, StartGameEnum startGame) {
        SArena_JoinGameRoom ret = new SArena_JoinGameRoom();
        ret.name = name;
        ret.startGame = startGame;
        return ret;
    }
}
