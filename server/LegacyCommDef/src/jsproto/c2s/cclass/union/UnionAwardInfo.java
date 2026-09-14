package jsproto.c2s.cclass.union;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class UnionAwardInfo {
    /**
     * 颁奖次数
     */
    private int awardNum;
    /**
     * 颁奖时间
     */
    private int awardTime;

    public int getAwardNum() {
        return awardNum;
    }

    public void setAwardNum(int awardNum) {
        this.awardNum = awardNum;
    }

    public int getAwardTime() {
        return awardTime;
    }

    public void setAwardTime(int awardTime) {
        this.awardTime = awardTime;
    }
}
