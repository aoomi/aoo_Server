package jsproto.c2s.cclass.luckdraw;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 抽奖次数信息
 */
@Data
@NoArgsConstructor
public class LuckDrawValueInfo {
    /**
     * 0：免费，1：房卡消耗，2：局数，3：大赢家
     */
    private int type;

    /**
     * 抽奖条件值（如：房卡消耗 X 张可抽奖）
     */
    private int conditionValue;

    /**
     * 到达条件后可抽奖次数
     */
    private int luckDrawValue;

    /**
     * 当前可抽奖的次数（能不能抽奖用这个判断）
     */
    private int value;

    /**
     * 日期类型：0:每日、1:每周、2:具体日期
     */
    private int dateType;
    /**
     * 时间段 0：全天、1:具体时间
     */
    private int timeSlot;
    /**
     * 开始时间
     */
    private int startTime;
    /**
     * 结束时间
     */
    private int endTime;

    public LuckDrawValueInfo(int type, int conditionValue, int luckDrawValue, int value, int dateType, int timeSlot, int startTime, int endTime) {
        this.type = type;
        this.conditionValue = conditionValue;
        this.luckDrawValue = luckDrawValue;
        this.value = value;
        this.dateType = dateType;
        this.timeSlot = timeSlot;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
