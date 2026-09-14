package jsproto.c2s.cclass.club;

import lombok.Data;

@Data
public class ClubPromotionLevelPowerInfo {
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

    public ClubPromotionLevelPowerInfo(int kicking, int modifyValue, int showShare, int invite) {
        this.kicking = kicking;
        this.modifyValue = modifyValue;
        this.showShare = showShare;
        this.invite = invite;
    }
}
