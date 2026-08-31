package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class CClub_PromotionChange extends BaseSendMsg {
    /**
     * 亲友圈id
     */
    private long clubId;

    /**
     * 玩家Pid
     */
    private long pid;
    /**
     * 推广员Id
     */
    private long partnerPid;
}
