package jsproto.c2s.cclass.rank;

/**
 * 排行榜列表项
 *
 * @author Huaxing
 */
public class RankItem {
    private long pid;
    private String name;
    private String iconUrl;
    private int setCount = 0;
    private int winCount = 0;
    private int refererCount = 0;

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public int getSetCount() {
        return setCount;
    }

    public void setSetCount(int setCount) {
        this.setCount += setCount;
    }

    public int getWinCount() {
        return winCount;
    }

    public void setWinCount(int winCount) {
        this.winCount += winCount;
    }

    public int getRefererCount() {
        return refererCount;
    }

    public void setRefererCount(int refererCount) {
        this.refererCount += refererCount;
    }


    public void cleanAll() {
        this.setCount = 0;
        this.winCount = 0;
        this.refererCount = 0;
    }

    @Override
    public String toString() {
        return "RankItem [pid=" + pid + ", name=" + name + ", iconUrl="
                + iconUrl + ", setCount=" + setCount + ", winCount=" + winCount
                + ", refererCount=" + refererCount + "]";
    }


}
