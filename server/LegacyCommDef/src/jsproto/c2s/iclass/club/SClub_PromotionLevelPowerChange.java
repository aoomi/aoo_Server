package jsproto.c2s.iclass.club;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 通知
 */
public class SClub_PromotionLevelPowerChange extends BaseSendMsg {
    /**
     * 玩家Pid
     */
    private long pid;
    /**
     * 亲友圈Id
     */
    private long clubId;
    /**
     * 推广员状态
     */
    private int levelPromotion;
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

    public static SClub_PromotionLevelPowerChange make(long pid, long clubId, int levelPromotion, int kicking, int modifyValue, int showShare, int invite) {
        SClub_PromotionLevelPowerChange ret = new SClub_PromotionLevelPowerChange();
        ret.pid = pid;
        ret.clubId = clubId;
        ret.levelPromotion = levelPromotion;
        ret.kicking = kicking;
        ret.modifyValue = modifyValue;
        ret.showShare = showShare;
        ret.invite = invite;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}
