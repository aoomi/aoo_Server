package jsproto.c2s.cclass.club;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 亲友圈推广员项
 */
@Data
public class ClubPromotionLevelItemList {
    private List<Integer> showList = new ArrayList<>();
    private List<Integer> showListSecond = new ArrayList<>();
    private List<ClubPromotionLevelItem> clubPromotionLevelItemList = new ArrayList<>();
    private int dateType;

    public ClubPromotionLevelItemList() {
    }

    public ClubPromotionLevelItemList(List<Integer> showList, List<Integer> showListSecond, List<ClubPromotionLevelItem> clubPromotionLevelItemList, int dateType) {
        this.showList = showList;
        this.showListSecond = showListSecond;
        this.clubPromotionLevelItemList = clubPromotionLevelItemList;
        this.dateType = dateType;
    }
}
