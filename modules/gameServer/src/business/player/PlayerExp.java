package business.player;

import cenum.PlayerEnum;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.clarkGame.PlayerBO;
import lombok.Data;

@Data
public class PlayerExp {
    private final PlayerBO playerBO;

    public PlayerExp(PlayerBO playerBO) {
        this.playerBO = playerBO;
    }

    /**
     * 检查活跃积分
     */
    public void checkVipExp() {
        // 如果活跃记录时间 <= 0,初始化积分
        if (this.playerBO.getBannedChatExpiredTime() <= 0) {
            // 初始活跃积分
            this.initVipExp(0);
            return;
        }
        // 初始玩家最近一次游戏时间
        if (this.playerBO.getLastLogout() <= 0) {
            this.playerBO.saveLastLogout(CommTime.nowSecond());
        }
        // 检查最近一次游戏时间和当前时间，计算相距天数。
        int disDay = CommTime.daysBetween(this.playerBO.getLastLogout(), CommTime.nowSecond());
        // 如果 >= 3天没玩游戏,
        if (disDay >= 3) {
            // 活跃积分为0
            this.playerBO.saveVipExp(PlayerEnum.ACTIVE_INT.EXP_0.value());
            // 设置本次的初始时间
            this.playerBO.saveBannedChatExpiredTime(CommTime.nowSecond());
            return;
        }
        // 两个时间间隔天数
        int dayInt = CommTime.daysBetween(this.playerBO.getBannedChatExpiredTime(), CommTime.nowSecond());
        // 如果时间间隔天数 >= 7
        if (dayInt >= 7) {
            // 初始活跃积分
            this.initVipExp(dayInt);
        }

    }

    /**
     * 初始活跃积分
     */
    public void initVipExp(int day) {
        PlayerEnum.TRY_TO_PLAY_USERS tryToPlayUser = PlayerEnum.TRY_TO_PLAY_USERS.valueOf(this.playerBO.getVipLevel());
        switch (tryToPlayUser) {
            case NEW_USER:// 新用户
                this.playerBO.saveVipExp(PlayerEnum.ACTIVE_INT.EXP_0.value());
                break;
            case NOT_TRY_TO_PLAY:// 不是试玩用户
                this.playerBO.saveVipExp(initExp(day));
                break;
            default:
                break;
        }
        // 设置本次的初始时间
        this.playerBO.saveBannedChatExpiredTime(CommTime.nowSecond());
    }

    /**
     * 初始分数
     *
     * @return
     */
    private int initExp(int day) {
        // 单用户 7 天内容，活跃积分<= 20 ，设为 0
        if (this.playerBO.getVipExp() <= PlayerEnum.ACTIVE_INT.EXP_20.value()) {
            return PlayerEnum.ACTIVE_INT.EXP_0.value();
        }
        // 检查最近一次游戏时间和当前时间，计算相距天数。
        int disDay = CommTime.daysBetween(this.playerBO.getLastLogout(), CommTime.nowSecond());
        // 如果 >= 3天没玩游戏,
        if (disDay >= 3) {
            return PlayerEnum.ACTIVE_INT.EXP_0.value();
        }

        // 计算初始的活动分值
        int initExp = (int) ((PlayerEnum.ACTIVE_INT.EXP_10.value()) + (this.playerBO.getVipExp() * 0.02));
        // 天数间隔 >= 9
        if (day >= 9) {
            initExp = PlayerEnum.ACTIVE_INT.EXP_0.value();
        } else if (initExp >= PlayerEnum.ACTIVE_INT.EXP_20.value()) {
            // 计算初始的活动分值 >= 50
            // 则初始值设为20
            initExp = PlayerEnum.ACTIVE_INT.EXP_20.value();
        }
        return initExp;
    }

    /**
     * 添加玩家游戏房间次数
     */
    public void addVipExp() {
        this.playerBO.saveVipExp(this.playerBO.getVipExp() + 1);
        this.playerBO.saveLastLogout(CommTime.nowSecond());
    }

}
