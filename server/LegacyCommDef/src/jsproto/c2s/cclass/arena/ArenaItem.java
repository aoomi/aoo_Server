package jsproto.c2s.cclass.arena;

/**
 * 比赛场列表项
 *
 * @author Huaxing
 */
public class ArenaItem {
    // 比赛场id
    private long aid = 0;
    // 名称
    private String name = "";
    // 图标
    private String icon = "";
    // 人数
    private int number = 0;
    // 报名状态
    private int state = 0;
    // 费用
    private int cost = 0;
    // 费用类型
    private int prizeType = 0;
    // 启动条件
    private int startCondition = 0;
    // 比赛开始类型
    private int arenaStartType = 0;
    private int createTime = 0;
    private boolean isShare;

    public long getAid() {
        return aid;
    }

    public void setAid(long aid) {
        this.aid = aid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public int getPrizeType() {
        return prizeType;
    }

    public void setPrizeType(int prizeType) {
        this.prizeType = prizeType;
    }

    public int getStartCondition() {
        return startCondition;
    }

    public void setStartCondition(int startCondition) {
        this.startCondition = startCondition;
    }

    public int getArenaStartType() {
        return arenaStartType;
    }

    public void setArenaStartType(int arenaStartType) {
        this.arenaStartType = arenaStartType;
    }

    public int getCreateTime() {
        return createTime;
    }

    public void setCreateTime(int createTime) {
        this.createTime = createTime;
    }

    public boolean isShare() {
        return isShare;
    }

    public void setShare(boolean isShare) {
        this.isShare = isShare;
    }


}
