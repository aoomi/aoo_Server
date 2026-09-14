package jsproto.c2s.cclass.pk.base;

import java.util.ArrayList;
import java.util.List;

/**
 * 吊蟹 配置
 *
 * @author Clark
 */
// 一局中各位置的信息
public class BasePKSet_Pos {
    private int posID = -1;
    private long pid = -1;
    private List<Integer> shouCard = new ArrayList<>(); //手牌，如果不是自己，填0， 如果个数是3n+2,则独立显示手牌
    private Boolean isLostConnect;// T掉线：F连接
    public Boolean isPlaying = false;
    private Double beginSportPoint;//开始时房间竞技点数
    private boolean isTrusteeship = false;

    public Double getBeginSportPoint() {
        return beginSportPoint;
    }

    public void setBeginSportPoint(Double beginSportPoint) {
        this.beginSportPoint = beginSportPoint;
    }

    public int getPosID() {
        return posID;
    }

    public void setPosID(int posID) {
        this.posID = posID;
    }

    public List<Integer> getShouCard() {
        return shouCard;
    }

    public void setShouCard(List<Integer> shouCard) {
        this.shouCard.addAll(shouCard);
    }

    public Boolean getIsLostConnect() {
        return isLostConnect;
    }

    public void setIsLostConnect(Boolean isLostConnect) {
        this.isLostConnect = isLostConnect;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public long getPid() {
        return pid;
    }

    public boolean isTrusteeship() {
        return isTrusteeship;
    }

    public void setTrusteeship(boolean trusteeship) {
        isTrusteeship = trusteeship;
    }
}
