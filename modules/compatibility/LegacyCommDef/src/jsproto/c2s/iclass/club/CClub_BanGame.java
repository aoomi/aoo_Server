package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 禁止游戏操作
 *
 * @author zaf
 */
public class CClub_BanGame extends BaseSendMsg {

    public long clubId;//俱乐部ID
    public long banPid;//指定禁止Pid
    public boolean isBan;//是否静止。
    /**
     * 查询id1
     */
    public String pidOne;
    /**
     * 查询id12
     */
    public String pidTwo;

    public static CClub_BanGame make(long clubId, long banPid, boolean isBan) {
        CClub_BanGame ret = new CClub_BanGame();
        ret.clubId = clubId;
        ret.banPid = banPid;
        ret.isBan = isBan;
        return ret;
    }

    public String getPidOne() {
        return pidOne;
    }

    public void setPidOne(String pidOne) {
        this.pidOne = pidOne;
    }

    public String getPidTwo() {
        return pidTwo;
    }

    public void setPidTwo(String pidTwo) {
        this.pidTwo = pidTwo;
    }
}