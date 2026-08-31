package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.club.ClubPromotionCalcActiveItem;
import lombok.Data;

import java.util.List;

@Data
public class CClub_PromotionCalcActiveBatch extends BaseSendMsg {
    /**
     * 亲友圈Id
     */
    private long clubId;
    /**
     * 玩家Pid
     */
    private long pid;
    /**
     * 分成类型（0：百分比，1：固定值）
     */
    private int type;
    /**
     * configId , scorePercent
     */
    List<ClubPromotionCalcActiveItem> promotionCalcActiveItemList;
    /**
     * 预留值
     */
    private double value;

}
