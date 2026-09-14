package jsproto.c2s.cclass.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 胡牌奖励分组信息
 *
 * @author Administrator
 */
public class HuRewardGroupInfo {
    private int groupType;
    private List<Long> groupList = new ArrayList<>();
    private List<HuRewardPrizeInfo> prizeInfo = new ArrayList<>();

    public int getGroupType() {
        return groupType;
    }

    public void setGroupType(int groupType) {
        this.groupType = groupType;
    }

    public List<Long> getGroupList() {
        return groupList;
    }

    public void setGroupList(List<Long> groupList) {
        this.groupList = groupList;
    }


    public List<HuRewardPrizeInfo> getPrizeInfo() {
        return prizeInfo;
    }

    public void setPrizeInfo(List<HuRewardPrizeInfo> prizeInfo) {
        this.prizeInfo = prizeInfo;
    }

    @Override
    public String toString() {
        return "HuRewardGroupInfo [groupType=" + groupType + ", groupList=" + groupList + ", prizeInfo=" + prizeInfo + "]";
    }


}
