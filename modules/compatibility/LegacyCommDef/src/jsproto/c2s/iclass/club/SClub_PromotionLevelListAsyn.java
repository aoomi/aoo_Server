package jsproto.c2s.iclass.club;
import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.club.ClubPromotionLevelItemList;

/**
 * 获取俱乐部玩家信息状态改变
 * @author zaf
 *
 */
public class SClub_PromotionLevelListAsyn extends BaseSendMsg {
     private ClubPromotionLevelItemList clubPromotionLevelItemList=new ClubPromotionLevelItemList();


    public static SClub_PromotionLevelListAsyn make(ClubPromotionLevelItemList clubPromotionLevelItemList) {
        SClub_PromotionLevelListAsyn ret = new SClub_PromotionLevelListAsyn();
        ret.setClubPromotionLevelItemList(clubPromotionLevelItemList);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }

    public ClubPromotionLevelItemList getClubPromotionLevelItemList() {
        return clubPromotionLevelItemList;
    }

    public void setClubPromotionLevelItemList(ClubPromotionLevelItemList clubPromotionLevelItemList) {
        this.clubPromotionLevelItemList = clubPromotionLevelItemList;
    }
}