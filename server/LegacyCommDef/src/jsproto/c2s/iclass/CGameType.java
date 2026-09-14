package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 游戏类型
 */
public class CGameType extends BaseSendMsg {
    // 游戏类型
    private int gameType;

    public static CGameType make(int gameType) {
        CGameType ret = new CGameType();
        ret.setGameType(gameType);
        return ret;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public int getGameType() {
        return gameType;
    }
}