package jsproto.c2s.cclass;

import lombok.Data;

import java.util.List;

@Data
public class FamilyItem {
    /**
     * vip
     */
    private int vip;

    /**
     * 城市列表
     */
    private List<Integer> cityIdList;

    /**
     * 权限
     */
    private int power;

    public FamilyItem(int vip, List<Integer> cityIdList, int power) {
        this.vip = vip;
        this.cityIdList = cityIdList;
        this.power = power;
    }
}
