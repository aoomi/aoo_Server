package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 亲友圈配置
 * @author Administrator
 *
 */
@Data
public class ClubAwardTimeAndNum extends BaseSendMsg {
    /**
     * 颁奖时间
     */
    private int awardTime;
    /**
     * 颁奖次数
     */
    private int awardNum;

    public ClubAwardTimeAndNum(int awardTime, int awardNum) {
        this.awardTime = awardTime;
        this.awardNum = awardNum;
    }
}
