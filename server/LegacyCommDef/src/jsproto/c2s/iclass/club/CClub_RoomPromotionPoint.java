package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 执行竞技点更新
 *
 * @author zaf
 */
@Data
public class CClub_RoomPromotionPoint extends BaseSendMsg {
    /**
     * 俱乐部ID
     */
    private long clubId;
    /**
     * 房间id
     */
    private long roomId;
    /**
     * 操作日期
     * 0 今天 1昨天 2前天
     */
    private int getType;
    /**
     * 预留值
     */
    private double value;

    /**
     * 要查看的人Pid
     */
    private long pid;

    public CClub_RoomPromotionPoint(long clubId, long roomId, int getType, double value) {
        this.clubId = clubId;
        this.roomId = roomId;
        this.getType = getType;
        this.value = value;
    }

    public CClub_RoomPromotionPoint(int getType, long pid) {
        this.getType = getType;
        this.pid = pid;
    }

    public CClub_RoomPromotionPoint(long roomId, int getType, long pid) {
        this.roomId = roomId;
        this.getType = getType;
        this.pid = pid;
    }

    @Override
    public String toString() {
        return "CClub_SportsPointUpdate{" +
                "clubId=" + clubId +
                ", roomId=" + roomId +
                ", getType=" + getType +
                ", value=" + value +
                '}';
    }
}