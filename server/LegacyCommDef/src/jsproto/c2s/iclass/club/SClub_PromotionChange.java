package jsproto.c2s.iclass.club;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 亲友圈推广员变更
 */
@Data
public class SClub_PromotionChange extends BaseSendMsg {
    /**
     * 玩家id
     */
    private long pid;
    /**
     * 亲友圈Id
     */
    private long clubId;
    /**
     * 推广员状态(0不是推广员,1任命,2卸任)
     */
    private int promotion;


    public static SClub_PromotionChange make(long pid, long clubId, int promotion) {
        SClub_PromotionChange ret = new SClub_PromotionChange();
        ret.setClubId(clubId);
        ret.setPid(pid);
        ret.setPromotion(promotion);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}
