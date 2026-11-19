package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 玩家圈卡改变
 *
 * @author Huaxing
 */
public class SPlayer_ChangeClubCard extends BaseSendMsg {
    public long pid;
    public int playerClubCard;
    public long agentsID;
    public int level;
    public long clubId;

    public static SPlayer_ChangeClubCard make(long pid, int playerClubCard, long agentsID, int level, long clubId) {
        SPlayer_ChangeClubCard ret = new SPlayer_ChangeClubCard();
        ret.pid = pid;
        ret.playerClubCard = playerClubCard;
        ret.agentsID = agentsID;
        ret.level = level;
        ret.clubId = clubId;
        return ret;
    }
}
