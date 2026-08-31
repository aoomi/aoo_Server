package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class CClub_PromotionActive extends BaseSendMsg {
    /**
     * 亲友圈Id
     */
    private long clubId;
    /**
     * 玩家Pid
     */
    private long pid;
    /**
     * 类型0加，1减
     */
    private int type;
    /**
     * 操作值
     */
    private double value;

}
