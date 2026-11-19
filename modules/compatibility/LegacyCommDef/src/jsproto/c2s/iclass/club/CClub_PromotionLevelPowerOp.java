package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class CClub_PromotionLevelPowerOp extends BaseSendMsg {
    /**
     * 俱乐部ID
     */
    private long clubId;
    /**
     *
     */
    private long pid;
    /**
     * 踢人（0:不允许,1:允许）
     */
    private int kicking;
    /**
     * 从属修改（0:不允许,1:允许）
     */
    private int modifyValue;
    /**
     * 显示分成（0:不允许,1:允许）
     */
    private int showShare;
    /**
     * 邀请（0:不允许,1:允许）
     */
    private int invite;
}
