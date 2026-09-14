package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 俱乐部房卡变了通知
 *
 * @author zaf
 */
public class SUnion_DiamondsNotEnough extends BaseSendMsg {

    public long unionId;//联盟ID
    public String name;//联盟名称
    public int diamondsValue;//钻石值

    public static SUnion_DiamondsNotEnough make(long unionId, String name, int diamondsValue) {
        SUnion_DiamondsNotEnough ret = new SUnion_DiamondsNotEnough();
        ret.unionId = unionId;
        ret.diamondsValue = diamondsValue;
        ret.name = name;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}