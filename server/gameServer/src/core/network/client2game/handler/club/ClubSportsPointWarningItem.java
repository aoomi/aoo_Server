package core.network.client2game.handler.club;

import lombok.Data;

/**
 * 亲友圈推广员项
 */
@Data
public class ClubSportsPointWarningItem {
    /**
     * 玩家Pid
     */
    private long pid;
    /**
     * 玩家昵称
     */
    private String name;
    /**
     * 预警状态（0:不预警,1:预警）
     */
    private int warnStatus;
    /**
     * 预警值
     */
    private double sportsPointWarning;

    public ClubSportsPointWarningItem(long pid, String name, int warnStatus, double sportsPointWarning) {
        this.pid = pid;
        this.name = name;
        this.warnStatus = warnStatus;
        this.sportsPointWarning = sportsPointWarning;
    }
}
