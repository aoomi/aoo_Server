package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class CClub_PromotionCalcActive extends BaseSendMsg {
    /**
     * 亲友圈Id
     */
    private long clubId;
    /**
     * 玩家Pid
     */
    private long pid;
    /**
     * 操作值
     */
    private double value;

}
