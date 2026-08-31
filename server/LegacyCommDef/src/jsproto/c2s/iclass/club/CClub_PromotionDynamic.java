package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 推广员消息动态
 */
@Data
public class CClub_PromotionDynamic extends BaseSendMsg {
    /**
     * 亲友圈Id
     */
    private long clubId;
    /**
     * 赛事Id
     */
    private long unionId;
    /**
     * 玩家Pid
     */
    private long pid;
    /**
     * 第几页
     */
    private int pageNum;
    /**
     * 类型 0全部、1下属玩家变动、2亲友圈房间、3赛事房间、4异常扣除、5异常补偿
     */
    private int type;
    /**
     * 操作pid
     */
    private long query;
}
