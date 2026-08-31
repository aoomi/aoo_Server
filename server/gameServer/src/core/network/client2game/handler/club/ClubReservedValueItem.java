package core.network.client2game.handler.club;

import lombok.Data;

/**
 * 亲友圈推广员项
 */
@Data
public class ClubReservedValueItem {
    /**
     * 玩家Pid
     */
    private long pid;
    /**
     * 玩家昵称
     */
    private String name;

    /**
     * 预留值
     */
    private double reservedValue;

    public ClubReservedValueItem(long pid, String name, double reservedValue) {
        this.pid = pid;
        this.name = name;
        this.reservedValue = reservedValue;
    }
}
