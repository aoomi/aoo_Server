package jsproto.c2s.cclass.club;

/**
 * 下属项
 */
public class ClubSubordinateItem {
    /**
     * 玩家Pid
     */
    private long pid;
    /**
     * 玩家昵称
     */
    private String name;
    /**
     * 玩家头像
     */
    private String iconUrl;
    /**
     * 当前活跃
     */
    private double curActiveValue;

    public ClubSubordinateItem(long pid, String name, String iconUrl, double curActiveValue) {
        this.pid = pid;
        this.name = name;
        this.iconUrl = iconUrl;
        this.curActiveValue = curActiveValue;
    }
}
