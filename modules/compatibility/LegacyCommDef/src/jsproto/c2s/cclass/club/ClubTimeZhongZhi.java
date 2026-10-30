package jsproto.c2s.cclass.club;

import lombok.Data;

/**
 * 中至时间段信息
 */
@Data
public class ClubTimeZhongZhi {

    private int beginTime;// 开始时间
    private int endTime;// 结束时间
    private int status = 0;//比赛状态 0 已结束 1进行中
    private int type = 0;//日期 0今天 1昨天 2前天 ....

    public ClubTimeZhongZhi() {
    }

    public ClubTimeZhongZhi(int beginTime, int endTime, int status, int type) {
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.status = status;
        this.type = type;
    }
}
