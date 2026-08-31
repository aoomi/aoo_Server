package jsproto.c2s.iclass.club;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 获取俱乐部玩家信息状态改变
 *
 * @author zaf
 */
public class SClub_ShowOnlinePlayerNum extends BaseSendMsg {

    public long clubId;//俱乐部ID
    /**
     * 查看在线人数(0:全部可见,1:推广员不可见)
     */
    private int showOnlinePlayerNum = 0;

    public static SClub_ShowOnlinePlayerNum make(long clubId, int showOnlinePlayerNum) {
        SClub_ShowOnlinePlayerNum ret = new SClub_ShowOnlinePlayerNum();
        ret.clubId = clubId;
        ret.showOnlinePlayerNum = showOnlinePlayerNum;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);

        return ret;
    }
}