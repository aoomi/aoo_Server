package jsproto.c2s.cclass.club;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 亲友圈推广员项
 */
@Data
public class ClubCompetitionRanked {

    private List<ClubPromotionLevelItem> clubPromotionLevelItemList = new ArrayList<>();
    private int dateType;
    private double scorePointTotal;
    private int totalPointShowStatus;//总积分显示状态

    public ClubCompetitionRanked(List<ClubPromotionLevelItem> clubPromotionLevelItemList, int dateType, double scorePointTotal, int totalPointShowStatus) {

        this.clubPromotionLevelItemList = clubPromotionLevelItemList;
        this.dateType = dateType;
        this.scorePointTotal = scorePointTotal;
        this.totalPointShowStatus = totalPointShowStatus;
    }
}
