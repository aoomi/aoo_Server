package jsproto.c2s.cclass.pos;

public class PlayerPosInfo {
    public long pid;
    public int point;
    public int posID;
    public Double sportsPoint;

    public PlayerPosInfo() {
        super();
    }


    public PlayerPosInfo(long pid, int point, int posID) {
        super();
        this.pid = pid;
        this.point = point;
        this.posID = posID;
    }

    public PlayerPosInfo(long pid, int point, int posID, Double sportsPoint) {
        this.pid = pid;
        this.point = point;
        this.posID = posID;
        this.sportsPoint = sportsPoint;
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
        this.point = point;
    }

    public int getPosID() {
        return posID;
    }

    public void setPosID(int posID) {
        this.posID = posID;
    }

    public Double getSportsPoint() {
        return sportsPoint;
    }

    public void setSportsPoint(Double sportsPoint) {
        this.sportsPoint = sportsPoint;
    }
}
