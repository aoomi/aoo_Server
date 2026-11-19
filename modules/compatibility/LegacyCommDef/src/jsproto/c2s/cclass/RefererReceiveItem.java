package jsproto.c2s.cclass;

/**
 * 推广领取列表
 *
 * @author Huaxing
 */
public class RefererReceiveItem {
    private long pid;        //用户ID
    private String name;    //名称
    private int state;
    private int inviteTime;
    private int price;


    public RefererReceiveItem(long pid, String name, int state, int inviteTime, int price) {
        super();
        this.pid = pid;
        this.name = name;
        this.state = state;
        this.inviteTime = inviteTime;
        this.price = price;
    }

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

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getInviteTime() {
        return inviteTime;
    }

    public void setInviteTime(int inviteTime) {
        this.inviteTime = inviteTime;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }


}
