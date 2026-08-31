package jsproto.c2s.iclass.club;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 获取俱乐部玩家信息状态改变
 *
 * @author zaf
 */
public class SClub_BecomePromotionManage extends BaseSendMsg {

    public long clubId;//俱乐部ID
    public String clubName;//俱乐部名称
    public int promotionManage;//

    public static SClub_BecomePromotionManage make(long clubId, String clubName, int promotionManage) {
        SClub_BecomePromotionManage ret = new SClub_BecomePromotionManage();
        ret.clubId = clubId;
        ret.promotionManage = promotionManage;
        ret.clubName = clubName;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}