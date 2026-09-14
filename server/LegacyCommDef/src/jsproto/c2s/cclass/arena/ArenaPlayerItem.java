package jsproto.c2s.cclass.arena;

public class ArenaPlayerItem {
    // 玩家ID
    private long pid;
    // 玩家分数
    private int point = 0;
    // 炸弹数
    private int bomb = 0;
    // 玩家排名
    private int ranking = 0;

    public ArenaPlayerItem() {
        super();
    }

    public ArenaPlayerItem(long pid, int point, int bomb) {
        super();
        this.pid = pid;
        this.point = point;
        this.bomb = bomb;
    }

    public ArenaPlayerItem(long pid, int point, int bomb, int ranking) {
        super();
        this.pid = pid;
        this.point = point;
        this.bomb = bomb;
        this.ranking = ranking;
    }

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public int getPoint() {
        return point;
    }

    public void setPoint(int point) {
        this.point += point;
    }

    public int getRanking() {
        return ranking;
    }

    public void setRanking(int ranking) {
        this.ranking = ranking;
    }


    public int getBomb() {
        return bomb;
    }

    public void setBomb(int bomb) {
        this.bomb += bomb;
    }

    @Override
    public String toString() {
        return "ArenaPlayerItem [pid=" + pid + ", point=" + point + ", ranking=" + ranking + "]";
    }

}
