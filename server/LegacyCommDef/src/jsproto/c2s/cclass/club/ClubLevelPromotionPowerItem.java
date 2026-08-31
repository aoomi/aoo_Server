package jsproto.c2s.cclass.club;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 亲友圈等级推广员权限
 */
@Data
public class ClubLevelPromotionPowerItem {


    /**
     * 等级权限
     */
    private Club_define.Club_PROMOTION_LEVEL_POWER levelPower;

    /**
     * 成员id列表
     */
    private List<Long> uidList = new ArrayList<>();
    /**
     * 操作成员Id
     */
    private long uid;

    public ClubLevelPromotionPowerItem(Club_define.Club_PROMOTION_LEVEL_POWER levelPower, List<Long> uidList, long uid) {
        this.levelPower = levelPower;
        this.uidList = uidList;
        this.uid = uid;
    }
}
