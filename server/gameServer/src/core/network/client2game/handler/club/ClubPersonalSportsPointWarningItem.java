package core.network.client2game.handler.club;

import lombok.Data;

/**
 * 亲友圈推广员项
 */
@Data
public class ClubPersonalSportsPointWarningItem {
    /**
     * 玩家Pid
     */
    private long pid;
    /**
     * 玩家昵称
     */
    private String name;
    /**
     * 个人预警状态（0:不预警,1:预警）
     */
    private int personalWarnStatus;
    /**
     * 个人预警值
     */
    private double personalSportsPointWarning;

    public ClubPersonalSportsPointWarningItem(long pid, String name, int personalWarnStatus, double personalSportsPointWarning) {
        this.pid = pid;
        this.name = name;
        this.personalWarnStatus = personalWarnStatus;
        this.personalSportsPointWarning = personalSportsPointWarning;
    }
}
