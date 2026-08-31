package jsproto.c2s.cclass.arena;

import jsproto.c2s.cclass.RewardInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 局数奖励
 *
 * @author Administrator
 */
public class SetRecordItem {
    private int setCount;
    private List<RewardInfo> eMailRewards = new ArrayList<RewardInfo>();

    public SetRecordItem(int setCount) {
        this.setCount = setCount;
        this.eMailRewards.add(new RewardInfo());
        this.eMailRewards.add(new RewardInfo());
        this.eMailRewards.add(new RewardInfo());
    }


    public int getSetCount() {
        return setCount;
    }

    public void setSetCount(int setCount) {
        this.setCount = setCount;
    }

    public List<RewardInfo> geteMailRewards() {
        return eMailRewards;
    }

    public void seteMailRewards(List<RewardInfo> eMailRewards) {
        this.eMailRewards = eMailRewards;
    }


    @Override
    public String toString() {
        return "SetRecordItem [setCount=" + setCount + ", eMailRewards=" + eMailRewards + "]";
    }


}
