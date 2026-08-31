package jsproto.c2s.cclass.union;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 亲友圈配置
 *
 * @author Administrator
 */
@Data
public class UnionLastRoundTimeInfo extends BaseSendMsg {
    private int startRoundTime;//本回合开始时间
    private int endRoundTime;//本回合结束时间
    private int lastStartRoundTime;//上回合开始时间
    private int lastEndRoundTime;//上回合结束时间

    public UnionLastRoundTimeInfo(int startRoundTime, int endRoundTime, int lastStartRoundTime, int lastEndRoundTime) {
        this.startRoundTime = startRoundTime;
        this.endRoundTime = endRoundTime;
        this.lastStartRoundTime = lastStartRoundTime;
        this.lastEndRoundTime = lastEndRoundTime;
    }

    public UnionLastRoundTimeInfo() {
    }
}
