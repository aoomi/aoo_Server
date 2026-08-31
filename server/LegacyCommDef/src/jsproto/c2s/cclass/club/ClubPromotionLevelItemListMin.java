package jsproto.c2s.cclass.club;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 亲友圈推广员项
 */
@Data
public class ClubPromotionLevelItemListMin {
    private List<Integer> showList = new ArrayList<>();
    private List<Integer> showListSecond = new ArrayList<>();
    private List<ClubPromotionLevelItemMin> clubPromotionLevelItemList = new ArrayList<>();
    private int dateType;

    public ClubPromotionLevelItemListMin(List<Integer> showList, List<Integer> showListSecond, List<ClubPromotionLevelItemMin> clubPromotionLevelItemList, int dateType) {
        this.showList = showList;
        this.showListSecond = showListSecond;
        this.clubPromotionLevelItemList = clubPromotionLevelItemList;
        this.dateType = dateType;
    }
}
