package jsproto.c2s.iclass.club;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 获取俱乐部显示状态信息
 *
 * @author zaf
 */
public class SClub_TotalPointShowStatus extends BaseSendMsg {

    public long clubId;//俱乐部ID
    public int type;//0 关闭  1 开启

    public static SClub_TotalPointShowStatus make(long clubId, int type) {
        SClub_TotalPointShowStatus ret = new SClub_TotalPointShowStatus();
        ret.clubId = clubId;
        ret.type = type;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}