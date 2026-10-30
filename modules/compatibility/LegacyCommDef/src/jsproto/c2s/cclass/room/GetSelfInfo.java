package jsproto.c2s.cclass.room;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 房间公共信息
 *
 * @author Administrator
 */
public class GetSelfInfo extends BaseSendMsg {

    private long pid;
    private long accountID;//accountID
    private String name;
    private String iconUrl;
    private int icon = 0;
    private int sex = 0;

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public long getAccountID() {
        return accountID;
    }

    public void setAccountID(long accountID) {
        this.accountID = accountID;
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

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public int getSex() {
        return sex;
    }

    public void setSex(int sex) {
        this.sex = sex;
    }
}