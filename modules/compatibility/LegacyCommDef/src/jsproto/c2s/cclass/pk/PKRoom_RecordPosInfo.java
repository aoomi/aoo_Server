package jsproto.c2s.cclass.pk;

import jsproto.c2s.cclass.room.BaseResults;

public class PKRoom_RecordPosInfo extends BaseResults {
    public int pos; // 位置
    public int winCount = 0;// 赢场数
    public int loseCount = 0;// 输场数
    public int flatCount = 0;// 平场数
    public int outBombSize = 0;
    public int maxBomb = 0;
    public boolean isLandowner = false; //是不是地主
    public int setMaxPoint = 0;// 当局最高
    public int setPoint = 0;// 牌局输赢分
    public int bombPoint = 0;// 炸弹分
    public Double reduceSportPoint;// 总的竞技点
    /**
     * 进园子原来的分数
     */
    public int jinYuanPoint = 0;

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }


    public int getWinCount() {
        return winCount;
    }

    public void setWinCount(int winCount) {
        this.winCount = winCount;
    }

    public int getLoseCount() {
        return loseCount;
    }

    public void setLoseCount(int loseCount) {
        this.loseCount = loseCount;
    }

    public int getFlatCount() {
        return flatCount;
    }

    public void setFlatCount(int flatCount) {
        this.flatCount = flatCount;
    }

    public int getOutBombSize() {
        return outBombSize;
    }

    public void setOutBombSize(int outBombSize) {
        this.outBombSize = outBombSize;
    }

    public int getMaxBomb() {
        return maxBomb;
    }

    public void setMaxBomb(int maxBomb) {
        this.maxBomb = maxBomb;
    }

    public boolean isLandowner() {
        return isLandowner;
    }

    public void setLandowner(boolean landowner) {
        isLandowner = landowner;
    }


}
