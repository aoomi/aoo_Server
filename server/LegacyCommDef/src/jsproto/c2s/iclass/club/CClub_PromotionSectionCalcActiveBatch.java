package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.club.ClubPromotionSectionCalcActiveItem;
import lombok.Data;

import java.util.List;

@Data
public class CClub_PromotionSectionCalcActiveBatch extends BaseSendMsg {
    /**
     * 亲友圈Id
     */
    private long opClubId;
    /**
     * 玩家Pid
     */
    private long opPid;
    /**
     * 区间信息
     */
    List<ClubPromotionSectionCalcActiveItem> promotionSectionCalcActiveItems;
    /**
     * 预留值
     */
    private double value;

}
