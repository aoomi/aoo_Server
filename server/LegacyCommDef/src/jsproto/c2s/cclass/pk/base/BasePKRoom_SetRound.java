package jsproto.c2s.cclass.pk.base;

import java.util.ArrayList;
import java.util.List;

public class BasePKRoom_SetRound {
    // 本次等待
    private int waitID = 0; // 当前第几次等待操作
    private int startWaitSec = 0; //开始等待时间
    private final List<BasePKRoom_RoundPos> opPosList = new ArrayList<>();
    private int runWaitSec = 0; //跑了多少时间

    private int waitOpPos = -1;


    public int getRunWaitSec() {
        return runWaitSec;
    }

    public void setRunWaitSec(int runWaitSec) {
        this.runWaitSec = runWaitSec;
    }

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

    public List<BasePKRoom_RoundPos> getOpPosList() {
        return opPosList;
    }

    public int getWaitOpPos() {
        return waitOpPos;
    }

    public void setWaitOpPos(int waitOpPos) {
        this.waitOpPos = waitOpPos;
    }

    public void addOpPosList(BasePKRoom_RoundPos bRoundPos) {
        if (null == bRoundPos) {
            return;
        }
        this.opPosList.add(bRoundPos);
    }

    @Override
    public String toString() {
        return "BasePKRoom_SetRound [waitID=" + waitID + ", startWaitSec="
                + startWaitSec + ", opPosList=" + opPosList + "]";
    }


}