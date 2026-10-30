package jsproto.c2s.cclass.playback;


/**
 * 存放回放时间和key等信息
 */
public class PlayBackDateTimeInfo {
    /**
     * 随机生成的6位数回放
     */
    private int code;
    /**
     * 周数 1-7
     */
    private int week;
    /**
     * 年月日和周
     */
    private String dayKey;
    /**
     * 最终回放码
     */
    private int playBackCode;
    /**
     * 标识Id
     */
    private int tabId;

    public PlayBackDateTimeInfo(int code, int week, String dayKey, int playBackCode, int tabId) {
        this.code = code;
        this.week = week;
        this.dayKey = dayKey;
        this.playBackCode = playBackCode;
        this.tabId = tabId;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getWeek() {
        return week;
    }

    public void setWeek(int week) {
        this.week = week;
    }

    public String getDayKey() {
        return dayKey;
    }

    public void setDayKey(String dayKey) {
        this.dayKey = dayKey;
    }

    public int getPlayBackCode() {
        return playBackCode;
    }

    public void setPlayBackCode(int playBackCode) {
        this.playBackCode = playBackCode;
    }

    public int getTabId() {
        return tabId;
    }
}
