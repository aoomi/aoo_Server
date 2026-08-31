package business.py.c2s.cclass;

import java.io.Serializable;

/**
 * 刨幺扑克大厅回放
 * 回放玩家信息
 */
public class PYPlayBackPlayerInFo implements Serializable {

    private long pid;
    private int pos;
    private String name;
    private String iconUrl;
    private int sex = 0;
    private int point = 0;

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
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

    public int getSex() {
        return sex;
    }

    public void setSex(int sex) {
        this.sex = sex;
    }

    public int getPoint() {
        return point;
    }

    public void setPoint(int point) {
        this.point = point;
    }

}
