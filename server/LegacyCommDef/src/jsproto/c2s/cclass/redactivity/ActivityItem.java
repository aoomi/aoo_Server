package jsproto.c2s.cclass.redactivity;

import jsproto.c2s.cclass.GameType;

/**
 * 排行榜列表项
 *
 * @author Huaxing
 */
public class ActivityItem {
    private long pid;
    private GameType gameType;
    private long roomID;
    private long time = 0;


    public ActivityItem(long pid, GameType gameType, long roomID, long time) {
        this.pid = pid;
        this.gameType = gameType;
        this.roomID = roomID;
        this.time = time;
    }


    /**
     * @return pid
     */
    public long getPid() {
        return pid;
    }


    /**
     * @param pid 要设置的 pid
     */
    public void setPid(long pid) {
        this.pid = pid;
    }


    /**
     * @return gameType
     */
    public GameType getGameType() {
        return gameType;
    }


    /**
     * @param gameType 要设置的 gameType
     */
    public void setGameType(GameType gameType) {
        this.gameType = gameType;
    }


    /**
     * @return roomID
     */
    public long getRoomID() {
        return roomID;
    }


    /**
     * @param roomID 要设置的 roomID
     */
    public void setRoomID(long roomID) {
        this.roomID = roomID;
    }


    /**
     * @return time
     */
    public long getTime() {
        return time;
    }


    /**
     * @param time 要设置的 time
     */
    public void setTime(long time) {
        this.time = time;
    }
}
