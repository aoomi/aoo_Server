package jsproto.c2s.iclass.club;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 俱乐部房卡变了通知
 *
 * @author zaf
 */
public class SClub_DiamondsNotEnough extends BaseSendMsg {

    public long clubId;//亲友圈id
    public String name;//亲友圈名称
    public int diamondsValue;//钻石值

    public static SClub_DiamondsNotEnough make(long clubId, String name, int diamondsValue) {
        SClub_DiamondsNotEnough ret = new SClub_DiamondsNotEnough();
        ret.clubId = clubId;
        ret.name = name;
        ret.diamondsValue = diamondsValue;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}