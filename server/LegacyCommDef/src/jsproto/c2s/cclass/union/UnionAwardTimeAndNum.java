package jsproto.c2s.cclass.union;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

import java.util.List;

/**
 * 亲友圈配置
 * @author Administrator
 *
 */
@Data
public class UnionAwardTimeAndNum extends BaseSendMsg {
    /**
     * 颁奖时间
     */
    private int awardTime;
    /**
     * 颁奖次数
     */
    private int awardNum;

    public UnionAwardTimeAndNum(int awardTime, int awardNum) {
        this.awardTime = awardTime;
        this.awardNum = awardNum;
    }
}
