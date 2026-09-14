package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;

public class CBase_GameDiscount extends BaseSendMsg {
    private int gameId;
    private long clubId;
    private long unionId;

    public static CBase_GameDiscount make(int gameId, long clubId, long unionId) {
        CBase_GameDiscount ret = new CBase_GameDiscount();
        ret.setGameId(gameId);
        ret.setClubId(clubId);
        ret.setUnionId(unionId);
        return ret;
    }

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public long getClubId() {
        return clubId;
    }

    public void setClubId(long clubId) {
        this.clubId = clubId;
    }

    public long getUnionId() {
        return unionId;
    }

    public void setUnionId(long unionId) {
        this.unionId = unionId;
    }
}
