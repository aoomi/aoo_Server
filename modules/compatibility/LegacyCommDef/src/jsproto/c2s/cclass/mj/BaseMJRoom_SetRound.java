package jsproto.c2s.cclass.mj;

import java.util.ArrayList;
import java.util.List;

public class BaseMJRoom_SetRound {
    // 本次等待
    private int waitID = 0; // 当前第几次等待操作
    private int startWaitSec = 0; //开始等待时间
    private int runWaitSec = 0; //跑了多少时间
    private boolean isShow = true; //是否显示
    private final List<BaseMJRoom_RoundPos> opPosList = new ArrayList<>();
    private int gameStatus = 0;//游戏状态


    public int getWaitID() {
        return waitID;
    }

    public void setWaitID(int waitID) {
        this.waitID = waitID;
    }

    public int getStartWaitSec() {
        return startWaitSec;
    }

    public void setStartWaitSec(int startWaitSec) {
        this.startWaitSec = startWaitSec;
    }

    public int getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(int gameStatus) {
        this.gameStatus = gameStatus;
    }

    public void setRunWaitSec(int runWaitSec) {
        this.runWaitSec = runWaitSec;
    }

    public List<BaseMJRoom_RoundPos> getOpPosList() {
        return opPosList;
    }

    public void addOpPosList(BaseMJRoom_RoundPos bRoundPos) {
        if (null == bRoundPos) {
            return;
        }
        this.opPosList.add(bRoundPos);
    }

    public void setShow(boolean show) {
        isShow = show;
    }

    @Override
    public String toString() {
        return "BaseMJRoom_SetRound [waitID=" + waitID + ", startWaitSec="
                + startWaitSec + ", opPosList=" + opPosList + "]";
    }


}